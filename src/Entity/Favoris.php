<?php

namespace App\Entity;

use App\Repository\FavorisRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: FavorisRepository::class)]
#[ORM\Table(name: 'favori_ressource')]
#[ORM\UniqueConstraint(name: 'unique_favori', columns: ['user_id', 'ressource_id'])]
class Favoris
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore-next-line */
    private ?int $id = null;

    #[ORM\ManyToOne(targetEntity: Utilisateurs::class)]
    #[ORM\JoinColumn(name: 'user_id', referencedColumnName: 'id_utilisateur', nullable: false, onDelete: 'CASCADE')]
    private ?Utilisateurs $user = null;

    #[ORM\ManyToOne(targetEntity: Ressources::class)]
    #[ORM\JoinColumn(name: 'ressource_id', referencedColumnName: 'id_ressources', nullable: false, onDelete: 'CASCADE')]
    private ?Ressources $ressource = null;

    #[ORM\Column(name: 'created_at')]
    private \DateTimeImmutable $createdAt;

    public function __construct(?Utilisateurs $user = null, ?Ressources $ressource = null)
    {
        $this->user      = $user;
        $this->ressource = $ressource;
        $this->createdAt = new \DateTimeImmutable();
    }

    public function getId(): ?int { return $this->id; }

    public function getUser(): ?Utilisateurs { return $this->user; }
    public function setUser(?Utilisateurs $user): static { $this->user = $user; return $this; }

    /**
     * Retourne l'ID de l'utilisateur (compatibilité).
     */
    public function getUserId(): ?int
    {
        return $this->user?->getIdUtilisateur();
    }

    public function getRessource(): ?Ressources { return $this->ressource; }
    public function setRessource(?Ressources $ressource): static { $this->ressource = $ressource; return $this; }

    public function getCreatedAt(): \DateTimeImmutable { return $this->createdAt; }

    // Protected: timestamp is set automatically in the constructor and must not be overwritten externally.
    protected function setCreatedAt(\DateTimeImmutable $createdAt): static
    {
        $this->createdAt = $createdAt;
        return $this;
    }
}