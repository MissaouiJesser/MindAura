<?php

namespace App\Security;

use App\Entity\Utilisateurs;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use KnpU\OAuth2ClientBundle\Client\ClientRegistry;
use KnpU\OAuth2ClientBundle\Security\Authenticator\OAuth2Authenticator;
use League\OAuth2\Client\Provider\GithubResourceOwner;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\RouterInterface;
use Symfony\Component\Security\Core\Authentication\Token\TokenInterface;
use Symfony\Component\Security\Core\Exception\AuthenticationException;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\RememberMeBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Badge\UserBadge;
use Symfony\Component\Security\Http\Authenticator\Passport\Passport;
use Symfony\Component\Security\Http\Authenticator\Passport\SelfValidatingPassport;
use Symfony\Component\Security\Http\EntryPoint\AuthenticationEntryPointInterface;

/**
 * Authenticator OAuth2 pour GitHub.
 *
 * GitHub ne retourne pas forcément l'email dans le profil public ;
 * on utilise l'API /user/emails pour récupérer l'email principal vérifié.
 */
class GithubAuthenticator extends OAuth2Authenticator implements AuthenticationEntryPointInterface
{
    public function __construct(
        private readonly ClientRegistry         $clientRegistry,
        private readonly EntityManagerInterface $em,
        private readonly UtilisateursRepository $repo,
        private readonly RouterInterface        $router,
    ) {}

    public function supports(Request $request): ?bool
    {
        return $request->attributes->get('_route') === 'app_oauth_check_github';
    }

    public function authenticate(Request $request): Passport
    {
        $client      = $this->clientRegistry->getClient('github');
        $accessToken = $this->fetchAccessToken($client);

        return new SelfValidatingPassport(
            new UserBadge($accessToken->getToken(), function () use ($accessToken, $client) {
                /** @var GithubResourceOwner $githubUser */
                $githubUser = $client->fetchUserFromToken($accessToken);

                $githubId = (string) $githubUser->getId();
                $login    = $githubUser->getNickname() ?? 'github_' . $githubId;
                $avatar   = $githubUser->toArray()['avatar_url'] ?? null;

                // GitHub peut cacher l'email dans le profil public.
                // On le récupère via l'API emails (scope user:email requis).
                $email = $githubUser->getEmail();
                if (!$email) {
                    $email = $this->fetchGithubPrimaryEmail($accessToken->getToken());
                }

                // Décomposer le login GitHub en prénom / nom
                $parts     = explode(' ', $githubUser->getName() ?? $login, 2);
                $firstName = $parts[0];
                $lastName  = $parts[1] ?? $login;

                // 1. Chercher par GitHub ID
                $user = $this->repo->findOneBy(['githubId' => $githubId]);

                // 2. Chercher par email
                if (!$user && $email) {
                    $user = $this->repo->findOneBy(['emailUtilisateur' => $email]);
                    if ($user) {
                        $user->setGithubId($githubId);
                        $this->em->flush();
                    }
                }

                // 3. Créer un nouveau compte
                if (!$user) {
                    $user = new Utilisateurs();
                    $user->setPrenomUtilisateur($firstName);
                    $user->setNomUtilisateur($lastName);
                    // Fallback si pas d'email public GitHub
                    $user->setEmailUtilisateur($email ?? ($login . '@github.oauth'));
                    $user->setMdpUtilisateur(bin2hex(random_bytes(32)));
                    $user->setTelephoneUtilisateur(null);
                    $user->setRoleUtilisateur('ROLE_PATIENT');
                    $user->setDateInscriptionUtilisateur(new \DateTime());
                    $user->setEstActifUtilisateur(true);
                    $user->setBioUtilisateur('');
                    $user->setIsEmailVerified(true);
                    $user->setGithubId($githubId);

                    if ($avatar) {
                        $filename = $this->downloadAvatar($avatar, 'github_' . $githubId);
                        $user->setPhotoProfilUtilisateur($filename);
                    } else {
                        $user->setPhotoProfilUtilisateur('default.png');
                    }

                    $user->setTotpSecret(null);

                    $this->em->persist($user);
                    $this->em->flush();
                }

                return $user;
            }),
            [new RememberMeBadge()]
        );
    }

    public function onAuthenticationSuccess(Request $request, TokenInterface $token, string $firewallName): ?Response
    {
        return new RedirectResponse($this->router->generate('app_home'));
    }

    public function onAuthenticationFailure(Request $request, AuthenticationException $exception): ?Response
    {
        $request->getSession()->set('oauth_error', strtr($exception->getMessageKey(), $exception->getMessageData()));
        return new RedirectResponse($this->router->generate('app_login'));
    }

    public function start(Request $request, ?AuthenticationException $authException = null): Response
    {
        return new RedirectResponse($this->router->generate('app_login'));
    }

    // ── Récupération de l'email principal GitHub ────────────────────

    /**
     * Appelle l'API GitHub /user/emails pour récupérer l'adresse principale vérifiée.
     * Nécessite le scope user:email.
     */
    private function fetchGithubPrimaryEmail(string $accessToken): ?string
    {
        try {
            $context = stream_context_create([
                'http' => [
                    'method'  => 'GET',
                    'header'  => implode("\r\n", [
                        'Authorization: token ' . $accessToken,
                        'Accept: application/vnd.github+json',
                        'User-Agent: MindAura-OAuth',
                    ]),
                    'timeout' => 5,
                ],
            ]);
            $json = @file_get_contents('https://api.github.com/user/emails', false, $context);
            if ($json === false) {
                return null;
            }
            $emails = json_decode($json, true);
            foreach ($emails as $e) {
                if (!empty($e['primary']) && !empty($e['verified'])) {
                    return $e['email'];
                }
            }
        } catch (\Throwable) {
            // Silently fail
        }
        return null;
    }

    private function downloadAvatar(string $url, string $prefix): string
    {
        try {
            $context = stream_context_create([
                'http' => [
                    'method'  => 'GET',
                    'header'  => 'User-Agent: MindAura-OAuth',
                    'timeout' => 5,
                ],
            ]);
            $data = @file_get_contents($url, false, $context);
            if ($data === false) {
                return 'default.png';
            }
            $filename   = $prefix . '_' . uniqid() . '.jpg';
            $avatarsDir = dirname(__DIR__, 2) . '/public/avatars';
            if (!is_dir($avatarsDir)) {
                mkdir($avatarsDir, 0775, true);
            }
            file_put_contents($avatarsDir . '/' . $filename, $data);
            return $filename;
        } catch (\Throwable) {
            return 'default.png';
        }
    }
}