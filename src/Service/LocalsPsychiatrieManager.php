<?php

namespace App\Service;

use App\Entity\LocalsPsychiatrie;

/**
 * Service métier pour la validation des règles de LocalsPsychiatrie.
 * Attributs validés : nom_local, adresse_local, disponibilite_local, telephone_local.
 */
class LocalsPsychiatrieManager
{
    /**
     * Règle 1 : nom_local       — obligatoire, min 3 caractères.
     * Règle 2 : adresse_local   — obligatoire, min 5 caractères.
     * Règle 3 : disponibilite_local — 'Disponible' ou 'Indisponible'.
     * Règle 4 : telephone_local — obligatoire, exactement 8 chiffres.
     *
     * Les autres champs (email, capacité, type...) sont aussi validés
     * pour que le helper makeValidLocal() ne bloque pas les tests.
     *
     * @throws \InvalidArgumentException si une règle n'est pas respectée.
     */
    public function validate(LocalsPsychiatrie $local): bool
    {
        // ── Règle 1 : nom_local ───────────────────────────────────────
        if (empty(trim((string) $local->getNomLocal()))) {
            throw new \InvalidArgumentException('Le nom du local est obligatoire.');
        }
        if (mb_strlen(trim((string) $local->getNomLocal())) < 3) {
            throw new \InvalidArgumentException('Le nom doit contenir au moins 3 caractères.');
        }

        // ── Règle 2 : adresse_local ───────────────────────────────────
        if (empty(trim((string) $local->getAdresseLocal()))) {
            throw new \InvalidArgumentException("L'adresse est obligatoire.");
        }
        if (mb_strlen(trim((string) $local->getAdresseLocal())) < 5) {
            throw new \InvalidArgumentException("L'adresse doit contenir au moins 5 caractères.");
        }

        // ── Règle 3 : disponibilite_local ────────────────────────────
        $validDispo = ['Disponible', 'Indisponible'];
        if (!in_array($local->getDisponibiliteLocal(), $validDispo, true)) {
            throw new \InvalidArgumentException(
                'La disponibilité doit être "Disponible" ou "Indisponible".'
            );
        }

        // ── Règle 4 : telephone_local (exactement 8 chiffres) ─────────
        $tel = $local->getTelephoneLocal();
        if ($tel === null) {
            throw new \InvalidArgumentException('Le numéro de téléphone est obligatoire.');
        }
        if ($tel < 10000000 || $tel > 99999999) {
            throw new \InvalidArgumentException(
                'Le numéro de téléphone doit contenir 8 chiffres (ex: 20123456).'
            );
        }

        // ── Autres champs (nécessaires pour ne pas bloquer les tests) ──
        if (empty($local->getEmailLocal()) || !filter_var($local->getEmailLocal(), FILTER_VALIDATE_EMAIL)) {
            throw new \InvalidArgumentException("L'email est obligatoire et doit être valide.");
        }

        $cap = $local->getCapaciteLocal();
        if ($cap === null || $cap === '' || !preg_match('/^\d+$/', $cap) || (int)$cap <= 0 || (int)$cap > 9999) {
            throw new \InvalidArgumentException('La capacité est invalide.');
        }

        $validTypes = ['Cabinet', 'Clinique', 'Hôpital', 'Centre de bien-être', 'Salle de thérapie', 'Autre'];
        if (!in_array($local->getTypeLocal(), $validTypes, true)) {
            throw new \InvalidArgumentException('Veuillez choisir un type de local valide.');
        }

        return true;
    }
}