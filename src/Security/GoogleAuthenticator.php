<?php

namespace App\Security;

use App\Entity\Utilisateurs;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use KnpU\OAuth2ClientBundle\Client\ClientRegistry;
use KnpU\OAuth2ClientBundle\Security\Authenticator\OAuth2Authenticator;
use League\OAuth2\Client\Provider\GoogleUser;
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
 * Authenticator OAuth2 pour Google.
 *
 * Étend OAuth2Authenticator (fourni par knpu/oauth2-client-bundle).
 * Crée l'utilisateur en base s'il n'existe pas encore.
 */
class GoogleAuthenticator extends OAuth2Authenticator implements AuthenticationEntryPointInterface
{
    public function __construct(
        private readonly ClientRegistry          $clientRegistry,
        private readonly EntityManagerInterface  $em,
        private readonly UtilisateursRepository  $repo,
        private readonly RouterInterface         $router,
    ) {}

    // ── Filtre : s'applique uniquement sur la route de callback Google ──

    public function supports(Request $request): ?bool
    {
        return $request->attributes->get('_route') === 'app_oauth_check_google';
    }

    // ── Authentification ────────────────────────────────────────────

    public function authenticate(Request $request): Passport
    {
        $client      = $this->clientRegistry->getClient('google');
        $accessToken = $this->fetchAccessToken($client);

        return new SelfValidatingPassport(
            new UserBadge($accessToken->getToken(), function () use ($accessToken, $client) {
                /** @var GoogleUser $googleUser */
                $googleUser = $client->fetchUserFromToken($accessToken);

                $email      = $googleUser->getEmail();
                $googleId   = $googleUser->getId();
                $firstName  = $googleUser->getFirstName() ?? 'Utilisateur';
                $lastName   = $googleUser->getLastName()  ?? 'Google';
                $avatar     = $googleUser->getAvatar();   // URL de l'avatar

                // 1. Chercher par Google ID
                $user = $this->repo->findOneBy(['googleId' => $googleId]);

                // 2. Chercher par email (compte existant)
                if (!$user && $email) {
                    $user = $this->repo->findOneBy(['emailUtilisateur' => $email]);
                    if ($user) {
                        // Lier le compte existant au Google ID
                        $user->setGoogleId($googleId);
                        $this->em->flush();
                    }
                }

                // 3. Créer un nouveau compte
                if (!$user) {
                    $user = new Utilisateurs();
                    $user->setPrenomUtilisateur($firstName);
                    $user->setNomUtilisateur($lastName);
                    $user->setEmailUtilisateur($email ?? ($googleId . '@google.oauth'));
                    $user->setMdpUtilisateur(bin2hex(random_bytes(32))); // mot de passe aléatoire
                    $user->setTelephoneUtilisateur(null);
                    $user->setRoleUtilisateur('ROLE_PATIENT');
                    $user->setDateInscriptionUtilisateur(new \DateTime());
                    $user->setEstActifUtilisateur(true);
                    $user->setBioUtilisateur('');
                    $user->setIsEmailVerified(true);  // email Google est déjà vérifié
                    $user->setGoogleId($googleId);

                    // Télécharger et stocker l'avatar Google (optionnel, fallback sur default.png)
                    if ($avatar) {
                        $filename = $this->downloadAvatar($avatar, 'google_' . $googleId);
                        $user->setPhotoProfilUtilisateur($filename);
                    } else {
                        $user->setPhotoProfilUtilisateur('default.png');
                    }

                    // Générer un secret TOTP minimal (pas de 2FA forcée pour OAuth)
                    $user->setTotpSecret(null);

                    $this->em->persist($user);
                    $this->em->flush();
                }

                return $user;
            }),
            [new RememberMeBadge()]
        );
    }

    // ── Succès ──────────────────────────────────────────────────────

    public function onAuthenticationSuccess(Request $request, TokenInterface $token, string $firewallName): ?Response
    {
        return new RedirectResponse($this->router->generate('app_home'));
    }

    // ── Échec ───────────────────────────────────────────────────────

    public function onAuthenticationFailure(Request $request, AuthenticationException $exception): ?Response
    {
        $request->getSession()->set('oauth_error', strtr($exception->getMessageKey(), $exception->getMessageData()));
        return new RedirectResponse($this->router->generate('app_login'));
    }

    // ── Entry point (redirection vers login si non authentifié) ─────

    public function start(Request $request, ?AuthenticationException $authException = null): Response
    {
        return new RedirectResponse($this->router->generate('app_login'));
    }

    // ── Helper : téléchargement de l'avatar OAuth ───────────────────

    private function downloadAvatar(string $url, string $prefix): string
    {
        try {
            $data = @file_get_contents($url);
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