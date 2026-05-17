<?php

namespace App\Entity\Trait;

use App\Entity\Utilisateurs;
use Doctrine\ORM\Mapping as ORM;

/**
 * Ajoute le champ d'audit updatedBy sur une entité.
 * createdBy est intentionnellement absent du trait : les entités qui
 * l'exigent NOT NULL (ex. ActivityLog) le déclarent directement dans
 * leur classe pour éviter le conflit de redéclaration PHP.
 */
trait BlameableTrait
{
    #[ORM\ManyToOne(targetEntity: Utilisateurs::class)]
    #[ORM\JoinColumn(name: 'updated_by_id', referencedColumnName: 'id_utilisateur', nullable: true, onDelete: 'SET NULL')]
    private ?Utilisateurs $updatedBy = null;

    public function getUpdatedBy(): ?Utilisateurs { return $this->updatedBy; }
    public function setUpdatedBy(?Utilisateurs $user): static { $this->updatedBy = $user; return $this; }
}