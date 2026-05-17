<?php

namespace App\Entity;

use App\Repository\ProduitRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: ProduitRepository::class)]
class Produit
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: 'Le nom est obligatoire.')]
    #[Assert\Length(min: 3, minMessage: 'Le nom doit avoir au moins {{ limit }} caractères.')]
    private string $nom = '';

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $description = null;

    #[ORM\Column(type: Types::DECIMAL, precision: 10, scale: 2)]
    #[Assert\NotNull(message: 'Le prix est obligatoire.')]
    #[Assert\Positive(message: 'Le prix doit être un nombre positif.')]
    private string $prix = '0.00';

    #[ORM\Column(nullable: true)]
    #[Assert\PositiveOrZero(message: 'Le stock ne peut pas être négatif.')]
    private ?int $stock = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $image = null;

    #[ORM\Column]
    private bool $estActif = true;

    #[ORM\Column(type: Types::DATETIME_MUTABLE)]
    private \DateTimeInterface $dateCreation;

    #[ORM\ManyToOne(targetEntity: Evenement::class)]
    #[ORM\JoinColumn(nullable: true, onDelete: 'SET NULL')]
    private ?Evenement $evenement = null;

    /**
     * @var list<string>|null
     */
    #[ORM\Column(type: Types::JSON, nullable: true)]
    private ?array $alertesEmail = null;

    public function __construct()
    {
        $this->dateCreation = new \DateTime();
    }

    // Tous les getters/setters identiques à la version précédente (inchangés)
    public function getId(): ?int { return $this->id; }
    public function getNom(): string { return $this->nom; }
    public function setNom(string $nom): static { $this->nom = $nom; return $this; }
    public function getDescription(): ?string { return $this->description; }
    public function setDescription(?string $description): static { $this->description = $description; return $this; }
    public function getPrix(): string { return $this->prix; }
    public function setPrix(string $prix): static { $this->prix = $prix; return $this; }
    public function getStock(): ?int { return $this->stock; }
    public function setStock(?int $stock): static { $this->stock = $stock; return $this; }
    public function getImage(): ?string { return $this->image; }
    public function setImage(?string $image): static { $this->image = $image; return $this; }
    public function isEstActif(): bool { return $this->estActif; }
    public function setEstActif(bool $estActif): static { $this->estActif = $estActif; return $this; }
    public function getDateCreation(): \DateTimeInterface { return $this->dateCreation; }
    protected function setDateCreation(\DateTimeInterface $date): static { $this->dateCreation = $date; return $this; }
    public function getEvenement(): ?Evenement { return $this->evenement; }
    public function setEvenement(?Evenement $evenement): static { $this->evenement = $evenement; return $this; }
    public function isDisponible(): bool
    {
        return $this->estActif && ($this->stock === null || $this->stock > 0);
    }
    /**
     * @return list<string>
     */
    public function getAlertesEmail(): array
    {
        return $this->alertesEmail ?? [];
    }
    public function addAlerteEmail(string $email): static
    {
        if ($this->alertesEmail === null) {
            $this->alertesEmail = [];
        }
        if (!in_array($email, $this->alertesEmail, true)) {
            $this->alertesEmail[] = $email;
        }
        return $this;
    }
    public function clearAlertesEmail(): static
    {
        $this->alertesEmail = null;
        return $this;
    }
}