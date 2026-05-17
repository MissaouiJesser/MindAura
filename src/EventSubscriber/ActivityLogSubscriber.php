<?php

namespace App\EventSubscriber;

use App\Entity\Utilisateurs;
use App\Service\ActivityLoggerService;
use Symfony\Component\EventDispatcher\EventSubscriberInterface;
use Symfony\Component\Security\Http\Event\InteractiveLoginEvent;
use Symfony\Component\Security\Http\Event\LogoutEvent;

/**
 * Écoute les événements Symfony Security pour logger automatiquement :
 *   - Connexion réussie (form_login + OAuth)
 *   - Déconnexion
 *
 * Les actions métier (inscription, 2FA, reset mdp, CRUD admin…)
 * sont loggées manuellement depuis leurs contrôleurs respectifs.
 *
 * Enregistrement : services.yaml (auto-discovery via autoconfigure: true).
 */
class ActivityLogSubscriber implements EventSubscriberInterface
{
    public function __construct(
        private readonly ActivityLoggerService $logger,
    ) {}

    public static function getSubscribedEvents(): array
    {
        return [
            // Déclenché après toute authentification interactive (form + OAuth)
            InteractiveLoginEvent::class => 'onInteractiveLogin',
            // Priorité 64 : s'exécute AVANT que Symfony invalide la session
            LogoutEvent::class => ['onLogout', 64],
        ];
    }

    // ── Connexion réussie ─────────────────────────────────────────────────────

    public function onInteractiveLogin(InteractiveLoginEvent $event): void
    {
        $user = $event->getAuthenticationToken()->getUser();
        if (!$user instanceof Utilisateurs) {
            return;
        }

        $request = $event->getRequest();
        $route   = $request->attributes->get('_route', '');
        $isOAuth = str_contains($route, 'oauth');

        if ($isOAuth) {
            $provider = str_contains($route, 'google') ? 'Google' : 'GitHub';
            $this->logger->logOAuthLogin($user, $provider);
        } else {
            $this->logger->logLogin($user, 'form');
        }
    }

    // ── Déconnexion ───────────────────────────────────────────────────────────

    public function onLogout(LogoutEvent $event): void
    {
        $token = $event->getToken();
        if ($token === null) {
            return;
        }

        $user = $token->getUser();
        if (!$user instanceof Utilisateurs) {
            return;
        }

        try {
            $this->logger->logLogout($user);
        } catch (\Throwable) {
            // Ne jamais bloquer le logout si le log échoue
        }
    }
}