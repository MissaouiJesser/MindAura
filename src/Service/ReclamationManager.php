<?php

namespace App\Service;

use App\Entity\Reclamation;

class ReclamationManager
{
   
    public function validate(Reclamation $reclamation): bool
    {
        // Règle 1 : Le sujet est obligatoire et doit contenir au moins 3 caractères
        if (empty(trim($reclamation->getSujetReclamation()))) {
            throw new \InvalidArgumentException('Le sujet de la réclamation est obligatoire.');
        }
        if (strlen(trim($reclamation->getSujetReclamation())) < 3) {
            throw new \InvalidArgumentException('Le sujet doit contenir au moins 3 caractères.');
        }

        // Règle 2 : La description est obligatoire et doit contenir au moins 10 caractères
        if (empty(trim($reclamation->getDescriptionReclamation()))) {
            throw new \InvalidArgumentException('La description de la réclamation est obligatoire.');
        }
        if (strlen(trim($reclamation->getDescriptionReclamation())) < 10) {
            throw new \InvalidArgumentException('La description doit contenir au moins 10 caractères.');
        }

        // Règle 3 : Le statut doit être une valeur valide
        $statutsValides = ['EN_ATTENTE', 'EN_COURS', 'TRAITEE', 'REJETEE'];
        if (!in_array($reclamation->getStatutCode(), $statutsValides, true)) {
            throw new \InvalidArgumentException(
                sprintf('Le statut "%s" n\'est pas valide.', $reclamation->getStatutCode())
            );
        }

        // Règle 5 : La note doit être comprise entre 0 et 5
        if ($reclamation->getRateReclamation() < 0 || $reclamation->getRateReclamation() > 5) {
            throw new \InvalidArgumentException('La note doit être comprise entre 0 et 5.');
        }

        return true;
    }
}
