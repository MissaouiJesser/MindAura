<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity]
#[ORM\Table(name: 'reservation_historique')]
#[ORM\HasLifecycleCallbacks]
class ReservationHistorique
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_historique', type: 'integer')]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: ReservationLocal::class, inversedBy: 'historiques')]
    #[ORM\JoinColumn(name: 'reservation_id', referencedColumnName: 'id_reservation', nullable: false)]
    private ReservationLocal $reservation;

    #[ORM\Column(name: 'champ_modifie', type: 'string', length: 255)]
    private string $champModifie;

    #[ORM\Column(name: 'ancienne_valeur', type: 'text', nullable: true)]
    private ?string $ancienneValeur = null;

    #[ORM\Column(name: 'nouvelle_valeur', type: 'text', nullable: true)]
    private ?string $nouvelleValeur = null;

    #[ORM\Column(name: 'modifie_par', type: 'string', length: 255, nullable: true)]
    private ?string $modifiePar = null;

    #[ORM\Column(name: 'date_modification', type: 'datetime')]
    private \DateTimeInterface $dateModification;

    #[ORM\Column(type: 'string', length: 255, nullable: true)]
    private ?string $commentaire = null;

    public function getIdHistorique(): ?int { return $this->id; }
    public function getReservation(): ReservationLocal { return $this->reservation; }
    public function setReservation(ReservationLocal $v): static { $this->reservation = $v; return $this; }
    public function getChampModifie(): string { return $this->champModifie; }
    public function setChampModifie(string $v): static { $this->champModifie = $v; return $this; }
    public function getAncienneValeur(): ?string { return $this->ancienneValeur; }
    public function setAncienneValeur(?string $v): static { $this->ancienneValeur = $v; return $this; }
    public function getNouvelleValeur(): ?string { return $this->nouvelleValeur; }
    public function setNouvelleValeur(?string $v): static { $this->nouvelleValeur = $v; return $this; }
    public function getModifiePar(): ?string { return $this->modifiePar; }
    public function setModifiePar(?string $v): static { $this->modifiePar = $v; return $this; }
    public function getDateModification(): \DateTimeInterface { return $this->dateModification; }
    protected function setDateModification(\DateTimeInterface $v): static { $this->dateModification = $v; return $this; }

    #[ORM\PrePersist]
    public function initDateModification(): void
    {
        $this->dateModification = new \DateTime();
    }
    public function getCommentaire(): ?string { return $this->commentaire; }
    public function setCommentaire(?string $v): static { $this->commentaire = $v; return $this; }
}