<?php

namespace App\Service;

use App\Entity\TestPsychologique;

/**
 * Service métier pour la validation des règles liées à TestPsychologique.
 *
 * Règles métier validées :
 *  1. Le titre est obligatoire (min 3, max 255 caractères).
 *  2. Le type doit appartenir à la liste autorisée.
 *  3. La durée estimée doit être un entier strictement positif (entre 1 et 1440 minutes).
 *  4. La description ne peut pas dépasser 1000 caractères.
 *  5. Les instructions ne peuvent pas dépasser 1000 caractères.
 */
class TestPsychologiqueManager
{
    private const TYPES_AUTORISES = ['personnalité', 'stress', 'logique', 'mémoire', 'autre'];

    /**
     * Valide toutes les règles métier d'un TestPsychologique.
     *
     * @throws \InvalidArgumentException si une règle est violée
     */
    public function validate(TestPsychologique $test): bool
    {
        $this->validateTitre($test->getTitre_test());
        $this->validateType($test->getType_test());
        $this->validateDuree($test->getDuree_estimee());
        $this->validateDescription($test->getDescription_test());
        $this->validateInstructions($test->getInstructions_test());

        return true;
    }

    // -------------------------------------------------------------------------
    // Validations individuelles (utiles aussi en test isolé)
    // -------------------------------------------------------------------------

    public function validateTitre(?string $titre): void
    {
        // ✅ ligne 46 : cast (string) appliqué avant trim() ET mb_strlen()
        $titre = (string) $titre;

        if (empty(trim($titre))) {
            throw new \InvalidArgumentException('Le titre du test est obligatoire.');
        }
        $len = mb_strlen(trim($titre));
        if ($len < 3) {
            throw new \InvalidArgumentException('Le titre doit contenir au moins 3 caractères.');
        }
        if ($len > 255) {
            throw new \InvalidArgumentException('Le titre ne peut pas dépasser 255 caractères.');
        }
    }

    public function validateType(?string $type): void
    {
        if (empty(trim((string) $type))) {
            throw new \InvalidArgumentException('Le type de test est obligatoire.');
        }
        if (!in_array($type, self::TYPES_AUTORISES, true)) {
            throw new \InvalidArgumentException(
                sprintf('Le type "%s" est invalide. Valeurs acceptées : %s.', $type, implode(', ', self::TYPES_AUTORISES))
            );
        }
    }

    public function validateDuree(?int $duree): void
    {
        if ($duree === null) {
            throw new \InvalidArgumentException('La durée estimée est obligatoire.');
        }
        if ($duree < 1 || $duree > 1440) {
            throw new \InvalidArgumentException('La durée estimée doit être comprise entre 1 et 1440 minutes.');
        }
    }

    public function validateDescription(?string $description): void
    {
        // ✅ lignes 82 : cast (string) appliqué avant mb_strlen()
        $description = (string) $description;

        if (empty(trim($description))) {
            throw new \InvalidArgumentException('La description du test est obligatoire.');
        }
        if (mb_strlen($description) > 1000) {
            throw new \InvalidArgumentException('La description ne peut pas dépasser 1000 caractères.');
        }
    }

    public function validateInstructions(?string $instructions): void
    {
        // ✅ ligne 92 : cast (string) appliqué avant mb_strlen()
        $instructions = (string) $instructions;

        if (empty(trim($instructions))) {
            throw new \InvalidArgumentException('Les instructions du test sont obligatoires.');
        }
        if (mb_strlen($instructions) > 1000) {
            throw new \InvalidArgumentException('Les instructions ne peuvent pas dépasser 1000 caractères.');
        }
    }
}