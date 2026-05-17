<?php

namespace App\Controller;

use App\Entity\Utilisateurs;
use App\Repository\UtilisateursRepository;
use Doctrine\ORM\EntityManagerInterface;
use KnpU\OAuth2ClientBundle\Client\ClientRegistry;
use League\OAuth2\Client\Provider\Exception\IdentityProviderException;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\RedirectResponse;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;
use Symfony\Component\Security\Http\Authentication\UserAuthenticatorInterface;
use Symfony\Component\Security\Http\Authenticator\FormLoginAuthenticator;

/**
 * Gère les redirections OAuth et les callbacks pour Google et GitHub.
 *
 * Flux :
 *   1. L'utilisateur clique sur "Connexion avec Google/GitHub" → app_oauth_connect_{provider}
 *   2. L'utilisateur est redirigé vers le fournisseur OAuth.
 *   3. Le fournisseur rappelle notre app → app_oauth_check_{provider}
 *   4. On récupère le profil, on crée/met à jour l'utilisateur, on connecte.
 */
class OAuthController extends AbstractController
{
    // ── Providers supportés ──────────────────────────────────────────
    /** @phpstan-ignore classConstant.unused */
    private const PROVIDERS = ['google', 'github'];

    // ════════════════════════════════════════════════════════════════
    //  REDIRECT → Fournisseur OAuth
    // ════════════════════════════════════════════════════════════════

    #[Route('/oauth/connect/google', name: 'app_oauth_connect_google')]
    public function connectGoogle(ClientRegistry $registry): RedirectResponse
    {
        return $registry->getClient('google')->redirect([
            'openid', 'profile', 'email',
        ], []);
    }

    #[Route('/oauth/connect/github', name: 'app_oauth_connect_github')]
    public function connectGithub(ClientRegistry $registry): RedirectResponse
    {
        return $registry->getClient('github')->redirect([
            'read:user', 'user:email',
        ], []);
    }

    // ════════════════════════════════════════════════════════════════
    //  CALLBACK ← Fournisseur OAuth
    // ════════════════════════════════════════════════════════════════

    /**
     * Point d'entrée unique pour les deux providers.
     * knpu/oauth2-client-bundle appelle cette route via security.yaml (check_path).
     * En pratique c'est l'authenticator qui fait le travail ; cette méthode
     * ne sera atteinte qu'en cas d'erreur non gérée.
     */
    #[Route('/oauth/check/google', name: 'app_oauth_check_google')]
    public function checkGoogle(): Response
    {
        // L'authenticator (GoogleAuthenticator) intercepte cette route avant qu'on arrive ici.
        throw new \LogicException('Ce contrôleur ne devrait pas être atteint directement.');
    }

    #[Route('/oauth/check/github', name: 'app_oauth_check_github')]
    public function checkGithub(): Response
    {
        throw new \LogicException('Ce contrôleur ne devrait pas être atteint directement.');
    }
}