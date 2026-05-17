<?php

namespace App\Service;

use App\Entity\ActivityLog;
use App\Entity\Utilisateurs;
use App\Enum\ActivityActionEnum;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Component\HttpFoundation\RequestStack;
use Symfony\Component\Security\Core\Authentication\Token\Storage\TokenStorageInterface;

/**
 * Service central de journalisation des actions utilisateur.
 *
 * Injection :
 *   App\Service\ActivityLoggerService  $logger
 *
 * Usage minimal :
 *   $logger->log('user.login', 'Connexion réussie');
 *
 * Usage complet :
 *   $logger->log('user.created', 'Utilisateur créé', [
 *       'context' => 'backoffice',
 *       'extra'   => ['target_id' => $newUser->getId()],
 *       'user'    => $adminUser,
 *   ]);
 */
class ActivityLoggerService
{
    public function __construct(
        private readonly EntityManagerInterface $em,
        private readonly RequestStack           $requestStack,
        private readonly TokenStorageInterface  $tokenStorage,
    ) {}

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Crée et persiste un ActivityLog.
     *
     * @param string               $action  Code court ex. "user.login"
     * @param string               $label   Texte lisible ex. "Connexion réussie"
     * @param array<string, mixed> $options { context?, user?, extra? }
     */
    public function log(string $action, string $label, array $options = []): void
    {
        try {
            $request = $this->requestStack->getCurrentRequest();

            // ── Résoudre l'utilisateur ────────────────────────────────────────
            $user = $options['user'] ?? $this->getCurrentUser();

            if (!$user instanceof Utilisateurs) {
                return;
            }

            // ── Détecter le contexte si non fourni ───────────────────────────
            $context = $options['context'] ?? $this->detectContext($request);

            // ── Résoudre l'enum action ────────────────────────────────────────
            $actionEnum = ActivityActionEnum::tryFrom($action);
            if ($actionEnum === null) {
                return;
            }

            // ── Construire le log ─────────────────────────────────────────────
            $log = new ActivityLog($user);
            // FIX: $action property is now typed ActivityActionEnum — pass the enum directly.
            $log->setAction($actionEnum);
            $log->setLabel($label);
            $log->setContext($context);
            $log->setExtra($options['extra'] ?? null);

            if ($request) {
                $log->setIp($request->getClientIp());
                $log->setRoute($request->attributes->get('_route'));
            }

            $log->setUserId($user->getIdUtilisateur());
            $log->setUserName($user->getPrenomUtilisateur() . ' ' . $user->getNomUtilisateur());
            $log->setUserEmail($user->getEmailUtilisateur());
            $log->setUserRole($user->getRoleUtilisateur());
            $log->setUserAvatar($user->getPhotoProfilUtilisateur());

            $this->em->persist($log);
            $this->em->flush();

        } catch (\Throwable) {
            // Ne jamais bloquer l'app pour un log raté
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS SÉMANTIQUES
    // ════════════════════════════════════════════════════════════════════════

    public function logLogin(Utilisateurs $user, string $method = 'form'): void
    {
        $this->log('user.login', 'Connexion réussie', [
            'context' => 'frontoffice',
            'user'    => $user,
            'extra'   => ['method' => $method],
        ]);
    }

    public function logOAuthLogin(Utilisateurs $user, string $provider): void
    {
        $this->log('user.oauth_login', 'Connexion via ' . ucfirst($provider), [
            'context' => 'frontoffice',
            'user'    => $user,
            'extra'   => ['provider' => $provider],
        ]);
    }

    public function logLogout(Utilisateurs $user): void
    {
        $this->log('user.logout', 'Déconnexion', [
            'context' => 'frontoffice',
            'user'    => $user,
        ]);
    }

    public function logRegister(Utilisateurs $user): void
    {
        $this->log('user.register', 'Inscription créée', [
            'context' => 'frontoffice',
            'user'    => $user,
        ]);
    }

    public function logEmailVerified(Utilisateurs $user): void
    {
        $this->log('user.email_verified', 'Adresse e-mail confirmée', [
            'context' => 'frontoffice',
            'user'    => $user,
        ]);
    }

    public function log2faSetup(Utilisateurs $user): void
    {
        $this->log('user.2fa_setup', 'Authentification 2FA activée', [
            'context' => 'frontoffice',
            'user'    => $user,
        ]);
    }

    public function logPasswordReset(Utilisateurs $user): void
    {
        $this->log('user.password_reset', 'Mot de passe réinitialisé', [
            'context' => 'frontoffice',
            'user'    => $user,
        ]);
    }

    public function logProfilUpdate(Utilisateurs $user, bool $avatarChanged = false): void
    {
        $action = $avatarChanged ? 'profil.avatar' : 'profil.updated';
        $label  = $avatarChanged ? 'Photo de profil mise à jour' : 'Profil mis à jour';
        $this->log($action, $label, [
            'context' => 'frontoffice',
            'user'    => $user,
        ]);
    }

    public function logBioSaved(Utilisateurs $user): void
    {
        $this->log('profil.bio', 'Bio IA sauvegardée', [
            'context' => 'frontoffice',
            'user'    => $user,
        ]);
    }

    public function logAdminUserCreated(Utilisateurs $admin, Utilisateurs $target): void
    {
        $this->log('user.created', 'Compte créé : ' . $target->getPrenomUtilisateur() . ' ' . $target->getNomUtilisateur(), [
            'context' => 'backoffice',
            'user'    => $admin,
            'extra'   => ['target_id' => $target->getIdUtilisateur(), 'target_email' => $target->getEmailUtilisateur()],
        ]);
    }

    public function logAdminUserUpdated(Utilisateurs $admin, Utilisateurs $target): void
    {
        $this->log('user.updated', 'Compte modifié : ' . $target->getPrenomUtilisateur() . ' ' . $target->getNomUtilisateur(), [
            'context' => 'backoffice',
            'user'    => $admin,
            'extra'   => ['target_id' => $target->getIdUtilisateur()],
        ]);
    }

    public function logAdminUserDeleted(Utilisateurs $admin, string $targetName, int $targetId): void
    {
        $this->log('user.deleted', 'Compte supprimé : ' . $targetName, [
            'context' => 'backoffice',
            'user'    => $admin,
            'extra'   => ['target_id' => $targetId],
        ]);
    }

    public function logAdminToggle(Utilisateurs $admin, Utilisateurs $target): void
    {
        $status = $target->isEstActifUtilisateur() ? 'activé' : 'désactivé';
        $this->log('user.toggled', 'Compte ' . $status . ' : ' . $target->getPrenomUtilisateur() . ' ' . $target->getNomUtilisateur(), [
            'context' => 'backoffice',
            'user'    => $admin,
            'extra'   => ['target_id' => $target->getIdUtilisateur(), 'new_status' => $target->isEstActifUtilisateur()],
        ]);
    }

    public function logExport(Utilisateurs $admin, string $format): void
    {
        $action = 'export.' . strtolower($format);
        $this->log($action, 'Export ' . strtoupper($format) . ' des utilisateurs', [
            'context' => 'backoffice',
            'user'    => $admin,
        ]);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRIVÉ
    // ════════════════════════════════════════════════════════════════════════

    private function getCurrentUser(): ?Utilisateurs
    {
        $token = $this->tokenStorage->getToken();
        if (!$token) {
            return null;
        }
        $user = $token->getUser();
        return $user instanceof Utilisateurs ? $user : null;
    }

    private function detectContext(?\Symfony\Component\HttpFoundation\Request $request): string
    {
        if (!$request) {
            return 'frontoffice';
        }
        $route = $request->attributes->get('_route', '');
        return str_starts_with($route, 'app_admin') || str_starts_with($route, 'app_dashboard')
            ? 'backoffice'
            : 'frontoffice';
    }
}