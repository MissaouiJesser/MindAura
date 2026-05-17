<?php

namespace App\Service;

use App\Entity\Utilisateurs;

class UtilisateursManager
{
    public function validate(Utilisateurs $utilisateur): bool
    {
        if (empty($utilisateur->getNomUtilisateur())) {
            throw new \InvalidArgumentException('Le nom est obligatoire');
        }

        if (empty($utilisateur->getPrenomUtilisateur())) {
            throw new \InvalidArgumentException('Le prénom est obligatoire');
        }

        if (!filter_var($utilisateur->getEmailUtilisateur(), FILTER_VALIDATE_EMAIL)) {
            throw new \InvalidArgumentException('Email invalide');
        }

        if (empty($utilisateur->getPlainPassword()) || strlen($utilisateur->getPlainPassword()) < 8) {
            throw new \InvalidArgumentException('Le mot de passe doit contenir au moins 8 caractères');
        }
        
        $telephone = (string) $utilisateur->getTelephoneUtilisateur();
        if (!preg_match('/^[0-9]{8}$/', $telephone)) {
            throw new \InvalidArgumentException('Le numéro de téléphone est invalide');
        }

        if ($utilisateur->getDateNaissanceUtilisateur() === null) {
            throw new \InvalidArgumentException('La date de naissance est obligatoire');
        }

        $age = (new \DateTime())->diff($utilisateur->getDateNaissanceUtilisateur())->y;
        if ($age < 16) {
            throw new \InvalidArgumentException('Vous devez avoir au moins 16 ans');
        }

        return true;
    }
}