<?php

namespace App\Security;

use App\Entity\Utilisateurs;
use Symfony\Component\Security\Core\Exception\CustomUserMessageAccountStatusException;
use Symfony\Component\Security\Core\User\UserCheckerInterface;
use Symfony\Component\Security\Core\User\UserInterface;

/**
 * Vérifie deux conditions avant d'autoriser la connexion :
 *   1. Le compte doit être actif (estActifUtilisateur = true).
 *   2. L'adresse e-mail doit avoir été vérifiée (isEmailVerified = true).
 */
class UserChecker implements UserCheckerInterface
{
    public function checkPreAuth(UserInterface $user): void
    {
        if (!$user instanceof Utilisateurs) {
            return;
        }

        // ── Vérification 1 : compte actif ──
        if (!$user->isEstActifUtilisateur()) {
            throw new CustomUserMessageAccountStatusException(
                'Votre compte a été désactivé. Veuillez contacter l\'administrateur.'
            );
        }

        // ── Vérification 2 : email confirmé ──
        if (!$user->isEmailVerified()) {
            throw new CustomUserMessageAccountStatusException(
                'Vous devez confirmer votre adresse e-mail avant de vous connecter. Vérifiez votre boîte de réception.'
            );
        }
    }

    public function checkPostAuth(UserInterface $user): void
    {
        // Rien à vérifier après l'authentification
    }
}