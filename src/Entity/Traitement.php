<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity]
#[ORM\Table(name: 'traitement')]
class Traitement
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_traitement', type: 'integer')]
    /** @phpstan-ignore property.unusedType */
    private ?int $idTraitement = null;

    #[ORM\Column(name: 'date_debut_traitement', type: 'date')]
    private \DateTimeInterface $dateDebutTraitement;

    #[ORM\Column(name: 'date_fin_traitement', type: 'date')]
    private \DateTimeInterface $dateFinTraitement;

    #[ORM\Column(name: 'objectif_traitement', type: 'string', length: 255)]
    private string $objectifTraitement;

    #[ORM\Column(name: 'description_traitement', type: 'text')]
    private string $descriptionTraitement;

    #[ORM\Column(name: 'etat_traitement', type: 'string', length: 255)]
    private string $etatTraitement;

    #[ORM\Column(name: 'type_traitement', type: 'string', length: 255)]
    private string $typeTraitement;

    #[ORM\ManyToOne(targetEntity: Utilisateurs::class, inversedBy: 'traitements')]
    #[ORM\JoinColumn(name: 'id_utilisateur_id', referencedColumnName: 'id_utilisateur', nullable: false)]
    private Utilisateurs $utilisateur;

    #[ORM\Column(name: 'id_coach', type: 'integer')]
    private int $idCoach;

    public function getIdTraitement(): ?int { return $this->idTraitement; }
    public function getDateDebutTraitement(): \DateTimeInterface { return $this->dateDebutTraitement; }
    public function setDateDebutTraitement(\DateTimeInterface $v): static { $this->dateDebutTraitement = $v; return $this; }
    public function getDateFinTraitement(): \DateTimeInterface { return $this->dateFinTraitement; }
    public function setDateFinTraitement(\DateTimeInterface $v): static { $this->dateFinTraitement = $v; return $this; }
    public function getObjectifTraitement(): string { return $this->objectifTraitement; }
    public function setObjectifTraitement(string $v): static { $this->objectifTraitement = $v; return $this; }
    public function getDescriptionTraitement(): string { return $this->descriptionTraitement; }
    public function setDescriptionTraitement(string $v): static { $this->descriptionTraitement = $v; return $this; }
    public function getEtatTraitement(): string { return $this->etatTraitement; }
    public function setEtatTraitement(string $v): static { $this->etatTraitement = $v; return $this; }
    public function getTypeTraitement(): string { return $this->typeTraitement; }
    public function setTypeTraitement(string $v): static { $this->typeTraitement = $v; return $this; }
    public function getUtilisateur(): Utilisateurs { return $this->utilisateur; }
    public function setUtilisateur(Utilisateurs $v): static { $this->utilisateur = $v; return $this; }
    public function getIdCoach(): int { return $this->idCoach; }
    public function setIdCoach(int $v): static { $this->idCoach = $v; return $this; }
}
