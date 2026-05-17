<?php

namespace App\Entity;

use App\Repository\FavoriRepository;
use Doctrine\ORM\Mapping as ORM;

/**
 * Entité Favori : stocke les événements mis en favoris par un utilisateur
 * identifié par son email (sans compte requis).
 */
#[ORM\Entity(repositoryClass: FavoriRepository::class)]
#[ORM\UniqueConstraint(name: 'unique_favori', columns: ['email_utilisateur', 'evenement_id'])]
class Favori
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    /**
     * Email de l'utilisateur qui a mis l'événement en favori.
     * Stocké pour identifier l'utilisateur sans système de login.
     */
    #[ORM\Column(length: 180)]
    private string $emailUtilisateur = '';

    #[ORM\ManyToOne(targetEntity: Evenement::class)]
    #[ORM\JoinColumn(nullable: false, onDelete: 'CASCADE')]
    private ?Evenement $evenement = null;

    #[ORM\Column(type: 'datetime')]
    private \DateTimeInterface $dateAjout;

    public function __construct()
    {
        $this->dateAjout = new \DateTime();
    }

    public function getId(): ?int { return $this->id; }

    public function getEmailUtilisateur(): string { return $this->emailUtilisateur; }
    public function setEmailUtilisateur(string $email): static { $this->emailUtilisateur = $email; return $this; }

    public function getEvenement(): ?Evenement { return $this->evenement; }
    public function setEvenement(?Evenement $evenement): static { $this->evenement = $evenement; return $this; }

    public function getDateAjout(): \DateTimeInterface { return $this->dateAjout; }

    // Protected: timestamp initialisé dans le constructeur, pas modifiable publiquement
    protected function setDateAjout(\DateTimeInterface $date): static { $this->dateAjout = $date; return $this; }
}