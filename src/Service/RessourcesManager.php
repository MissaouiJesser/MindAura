<?php

namespace App\Service;

use App\Entity\Ressources;

class RessourcesManager
{
    /**
     * Valide les règles métier d'une ressource.
     *
     * Règles :
     * 1. Le titre est obligatoire
     * 2. Le titre ne dépasse pas 255 caractères
     * 3. Le résumé est obligatoire
     * 4. Le type de contenu doit être valide (Article, Video, Podcast, PDF)
     * 5. La catégorie doit être valide
     * 6. Le niveau doit être valide (DEBUTANT, INTERMEDIAIRE, AVANCE)
     * 7. La durée de lecture doit être >= 0
     * 8. L'URL est obligatoire pour les types Article et Podcast
     * 9. L'email auteur doit être valide s'il est fourni
     *
     * @throws \InvalidArgumentException si une règle est violée
     */
    public function validate(Ressources $ressource): bool
    {
        // Règle 1 : titre obligatoire
        if (empty(trim($ressource->getTitre()))) {
            throw new \InvalidArgumentException('Le titre est obligatoire.');
        }

        // Règle 2 : titre max 255 caractères
        if (strlen($ressource->getTitre()) > 255) {
            throw new \InvalidArgumentException('Le titre ne doit pas dépasser 255 caractères.');
        }

        // Règle 3 : résumé obligatoire
       if (empty(trim($ressource->getResume()))) {
            throw new \InvalidArgumentException('Le résumé est obligatoire.');
        }

        // Règle 4 : type de contenu valide
        $contenusValides = ['Article', 'Video', 'Podcast', 'PDF'];
        if (!in_array($ressource->getContenu(), $contenusValides, true)) {
            throw new \InvalidArgumentException('Type de contenu invalide.');
        }

        // Règle 5 : catégorie valide
        $categoriesValides = [
            'gestion_stress', 'confiance_en_soi', 'motivation',
            'communication', 'bien_etre', 'protectivite', 'intelligence_emotionnelle',
        ];
        if (!in_array($ressource->getCategorie(), $categoriesValides, true)) {
            throw new \InvalidArgumentException('Catégorie invalide.');
        }

        // Règle 6 : niveau valide
        $niveauxValides = ['DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'];
        if (!in_array($ressource->getNiveau(), $niveauxValides, true)) {
            throw new \InvalidArgumentException('Niveau invalide.');
        }

        // Règle 7 : durée de lecture >= 0
        if ($ressource->getDureeLecture() < 0) {
            throw new \InvalidArgumentException('La durée de lecture doit être positive ou nulle.');
        }

        // Règle 8 : URL obligatoire pour Article et Podcast
        if (in_array($ressource->getContenu(), ['Article', 'Podcast'], true)) {
            if (empty(trim($ressource->getUrl() ?? ''))) {
                throw new \InvalidArgumentException('Le lien est obligatoire pour un article ou podcast.');
            }
            if (!filter_var($ressource->getUrl(), FILTER_VALIDATE_URL)) {
                throw new \InvalidArgumentException("Le lien n'est pas une URL valide.");
            }
        }

        // Règle 9 : email auteur valide si fourni
        if (!empty($ressource->getEmailAuteur())) {
            if (!filter_var($ressource->getEmailAuteur(), FILTER_VALIDATE_EMAIL)) {
                throw new \InvalidArgumentException("L'email auteur n'est pas valide.");
            }
        }

        return true;
    }

    /**
     * Incrémente les vues et retourne le nouveau total.
     */
    public function incrementerVues(Ressources $ressource): int
    {
        $ressource->incrementVues();
        return $ressource->getNbrVues();
    }

    /**
     * Incrémente les likes et retourne le nouveau total.
     */
    public function incrementerLikes(Ressources $ressource): int
    {
        $ressource->incrementLikes();
        return $ressource->getLikes();
    }
}