<?php

namespace App\Entity;

use App\Repository\ReservationLocalRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;
use App\Entity\Salle;
use App\Entity\LocalsPsychiatrie;
use App\Entity\ReservationHistorique;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;

#[ORM\Entity(repositoryClass: ReservationLocalRepository::class)]
#[ORM\Table(name: 'reservation_local')]
class ReservationLocal
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_reservation', type: Types::INTEGER)]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Salle::class)]
    #[ORM\JoinColumn(name: 'salle_id', referencedColumnName: 'id_salle', nullable: false, onDelete: "CASCADE")]
    private ?Salle $salle = null;

    #[ORM\Column(name: 'id_utilisateur', type: Types::INTEGER, nullable: true)]
    private ?int $idUtilisateur = null;

    #[ORM\ManyToOne(targetEntity: LocalsPsychiatrie::class)]
    #[ORM\JoinColumn(name: 'local_id', referencedColumnName: 'id_local', nullable: true)]
    private ?LocalsPsychiatrie $local = null;

    #[ORM\Column(name: 'date_reservation', type: Types::DATE_MUTABLE, nullable: true)]
    #[Assert\NotBlank(message: 'La date de réservation est obligatoire.')]
    private ?\DateTimeInterface $dateReservation = null;

    #[ORM\Column(name: 'heure_debut_reservation', type: Types::TIME_MUTABLE, nullable: true)]
    #[Assert\NotBlank(message: "L'heure de début est obligatoire.")]
    private ?\DateTimeInterface $heureDebutReservation = null;

    #[ORM\Column(name: 'heure_fin_reservation', type: Types::TIME_MUTABLE, nullable: true)]
    #[Assert\NotBlank(message: "L'heure de fin est obligatoire.")]
    private ?\DateTimeInterface $heureFinReservation = null;

    #[ORM\Column(name: 'status_reservation', type: Types::STRING, length: 50, nullable: true)]
    private ?string $statusReservation = 'En attente';

    #[ORM\Column(name: 'motif_reservation', type: Types::TEXT, nullable: true)]
    private ?string $motifReservation = null;

    #[ORM\Column(name: 'prix_reservation', type: Types::DECIMAL, precision: 10, scale: 2, nullable: true)]
    private ?string $prixReservation = null;

    #[ORM\Column(name: 'nom_cl', type: Types::STRING, length: 100, nullable: true)]
    #[Assert\NotBlank(message: 'Le nom du client est obligatoire.')]
    private ?string $nomCl = null;

    #[ORM\Column(name: 'prenom_cl', type: Types::STRING, length: 100, nullable: true)]
    #[Assert\NotBlank(message: 'Le prénom du client est obligatoire.')]
    private ?string $prenomCl = null;

    // 🔥 RELATION MANQUANTE AJOUTÉE
    /** @var Collection<int, ReservationHistorique> */
    #[ORM\OneToMany(mappedBy: 'reservation', targetEntity: ReservationHistorique::class)]
    private Collection $historiques;

    // ── Constructor ─────────────────────────────
    public function __construct()
    {
        $this->historiques = new ArrayCollection();
    }

    // ── Getters / Setters ───────────────────────

    public function getIdReservation(): ?int
    {
        return $this->id;
    }

    public function getSalle(): ?Salle
    {
        return $this->salle;
    }

    public function setSalle(?Salle $salle): static
    {
        $this->salle = $salle;
        return $this;
    }

    public function getIdUtilisateur(): ?int
    {
        return $this->idUtilisateur;
    }

    public function setIdUtilisateur(?int $idUtilisateur): static
    {
        $this->idUtilisateur = $idUtilisateur;
        return $this;
    }

    public function getLocal(): ?LocalsPsychiatrie
    {
        return $this->local;
    }

    public function setLocal(?LocalsPsychiatrie $local): static
    {
        $this->local = $local;
        return $this;
    }

    public function getDateReservation(): ?\DateTimeInterface
    {
        return $this->dateReservation;
    }

    public function setDateReservation(?\DateTimeInterface $dateReservation): static
    {
        $this->dateReservation = $dateReservation;
        return $this;
    }

    public function getHeureDebutReservation(): ?\DateTimeInterface
    {
        return $this->heureDebutReservation;
    }

    public function setHeureDebutReservation(?\DateTimeInterface $heureDebutReservation): static
    {
        $this->heureDebutReservation = $heureDebutReservation;
        return $this;
    }

    public function getHeureFinReservation(): ?\DateTimeInterface
    {
        return $this->heureFinReservation;
    }

    public function setHeureFinReservation(?\DateTimeInterface $heureFinReservation): static
    {
        $this->heureFinReservation = $heureFinReservation;
        return $this;
    }

    public function getStatusReservation(): ?string
    {
        return $this->statusReservation;
    }

    public function setStatusReservation(?string $statusReservation): static
    {
        $this->statusReservation = $statusReservation;
        return $this;
    }

    public function getMotifReservation(): ?string
    {
        return $this->motifReservation;
    }

    public function setMotifReservation(?string $motifReservation): static
    {
        $this->motifReservation = $motifReservation;
        return $this;
    }

    public function getPrixReservation(): ?string
    {
        return $this->prixReservation;
    }

    public function setPrixReservation(?string $prixReservation): static
    {
        $this->prixReservation = $prixReservation;
        return $this;
    }

    public function getNomCl(): ?string
    {
        return $this->nomCl;
    }

    public function setNomCl(?string $nomCl): static
    {
        $this->nomCl = $nomCl;
        return $this;
    }

    public function getPrenomCl(): ?string
    {
        return $this->prenomCl;
    }

    public function setPrenomCl(?string $prenomCl): static
    {
        $this->prenomCl = $prenomCl;
        return $this;
    }

    // ── Historiques ─────────────────────────────

    /** @return Collection<int, ReservationHistorique> */
    public function getHistoriques(): Collection
    {
        return $this->historiques;
    }
}