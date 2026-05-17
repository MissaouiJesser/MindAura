<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use App\Entity\Categorie;
use Doctrine\Common\Collections\Collection;
use Doctrine\Common\Collections\ArrayCollection;
use App\Entity\Reponse;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity]
#[ORM\Table(name: 'reclamation')]
class Reclamation
{
    #[ORM\Id]
    #[ORM\GeneratedValue(strategy: 'IDENTITY')]
    #[ORM\Column(name: 'id_reclamation', type: 'integer')]
    private int $id_reclamation = 0;

    // FIX: name='categorie_id' (FK column in reclamation table)
    //      referencedColumnName='id_categorie' (PK column in categorie table)
    #[ORM\ManyToOne(targetEntity: Categorie::class, inversedBy: 'reclamations')]
    #[ORM\JoinColumn(name: 'categorie_id', referencedColumnName: 'id_categorie', nullable: false, onDelete: 'CASCADE')]
    #[Assert\NotNull(message: 'La catégorie est obligatoire')]
    private ?Categorie $categorie = null;

    #[ORM\Column(name: 'sujet_reclamation', type: 'string', length: 255)]
    #[Assert\NotBlank(message: 'Le sujet de la réclamation ne peut pas être vide')]
    #[Assert\Length(
        min: 3,
        max: 255,
        minMessage: 'Le sujet doit contenir au moins {{ limit }} caractères',
        maxMessage: 'Le sujet ne doit pas dépasser {{ limit }} caractères'
    )]
    private string $sujet_reclamation = '';

    #[ORM\Column(name: 'description_reclamation', type: 'text')]
    #[Assert\NotBlank(message: 'La description ne peut pas être vide')]
    #[Assert\Length(
        min: 10,
        max: 5000,
        minMessage: 'La description doit contenir au moins {{ limit }} caractères',
        maxMessage: 'La description ne doit pas dépasser {{ limit }} caractères'
    )]
    private string $description_reclamation = '';

    #[ORM\Column(name: 'date_creation_reclamation', type: 'datetime')]
    #[Assert\NotNull(message: 'La date de création est obligatoire')]
    private \DateTimeInterface $dateCreation_reclamation;

    #[ORM\Column(name: 'statut_reclamation', type: 'string')]
    #[Assert\NotBlank(message: 'Le statut est obligatoire')]
    #[Assert\Choice(
        choices: ['EN_ATTENTE', 'EN_COURS', 'TRAITEE', 'REJETEE'],
        message: 'Le statut "{{ value }}" n\'est pas valide.'
    )]
    private string $statut_reclamation;

    #[ORM\Column(name: 'utilisateur_id', type: 'integer')]
    #[Assert\NotNull(message: 'L\'ID utilisateur est obligatoire')]
    #[Assert\GreaterThan(0, message: 'L\'ID utilisateur doit être positif')]
    private int $id_utilisateur;

    #[ORM\Column(name: 'rate_reclamation', type: 'float')]
    #[Assert\Range(
        min: 0,
        max: 5,
        notInRangeMessage: 'La note doit être entre {{ min }} et {{ max }}'
    )]
    private float $rate_Reclamation = 0;

    #[ORM\Column(name: 'rate_sum', type: 'integer')]
    #[Assert\GreaterThanOrEqual(0, message: 'La somme des notes doit être positive ou zéro')]
    private int $rate_sum = 0;

    #[ORM\Column(name: 'rate_count', type: 'integer')]
    #[Assert\GreaterThanOrEqual(0, message: 'Le nombre de notes doit être positif ou zéro')]
    private int $rate_count = 0;

    /** @var Collection<int, Reponse> */
    #[ORM\OneToMany(mappedBy: 'reclamation', targetEntity: Reponse::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $reponses;

    public function __construct()
    {
        $this->reponses = new ArrayCollection();
        $this->dateCreation_reclamation = new \DateTime();
        $this->statut_reclamation = 'EN_ATTENTE';
        $this->id_utilisateur = 0;
    }

    /** @return Collection<int, Reponse> */
    public function getReponses(): Collection { return $this->reponses; }

    public function addReponse(Reponse $reponse): self
    {
        if (!$this->reponses->contains($reponse)) {
            $this->reponses[] = $reponse;
            $reponse->setReclamation($this);
        }
        return $this;
    }

    public function removeReponse(Reponse $reponse): self
    {
        if ($this->reponses->removeElement($reponse)) {
            if ($reponse->getReclamation() === $this) {
                $reponse->setReclamation(null);
            }
        }
        return $this;
    }

    public function getId_reclamation(): int { return $this->id_reclamation; }
    public function getIdReclamation(): int { return $this->id_reclamation; }

    public function getCategorie(): ?Categorie { return $this->categorie; }
    public function setCategorie(?Categorie $categorie): self { $this->categorie = $categorie; return $this; }

    public function getSujet_reclamation(): string { return $this->sujet_reclamation; }
    public function setSujet_reclamation(string $value): self { $this->sujet_reclamation = $value; return $this; }
    public function getSujetReclamation(): string { return $this->sujet_reclamation; }
    public function setSujetReclamation(string $value): self { return $this->setSujet_reclamation($value); }

    public function getDescription_reclamation(): string { return $this->description_reclamation; }
    public function setDescription_reclamation(string $value): self { $this->description_reclamation = $value; return $this; }
    public function getDescriptionReclamation(): string { return $this->description_reclamation; }
    public function setDescriptionReclamation(string $value): self { return $this->setDescription_reclamation($value); }

    public function getDateCreation_reclamation(): \DateTimeInterface { return $this->dateCreation_reclamation; }
    protected function setDateCreation_reclamation(\DateTimeInterface $value): self { $this->dateCreation_reclamation = $value; return $this; }
    public function getDateCreationReclamation(): \DateTimeInterface { return $this->dateCreation_reclamation; }
    protected function setDateCreationReclamation(\DateTimeInterface $value): self { return $this->setDateCreation_reclamation($value); }

    public function getStatut_reclamation(): string { return self::displayStatut($this->statut_reclamation); }
    public function setStatut_reclamation(?string $value): self { $this->statut_reclamation = self::normalizeStatutCode($value); return $this; }
    public function getStatutCode(): string { return self::normalizeStatutCode($this->statut_reclamation); }
    public function getStatutReclamation(): string { return $this->getStatut_reclamation(); }
    public function setStatutReclamation(?string $value): self { return $this->setStatut_reclamation($value); }

    public function getId_utilisateur(): int { return $this->id_utilisateur; }
    public function setId_utilisateur(int $value): self { $this->id_utilisateur = $value; return $this; }
    public function getIdUtilisateur(): int { return $this->id_utilisateur; }
    public function setIdUtilisateur(int $value): self { return $this->setId_utilisateur($value); }

    public function getRate_Reclamation(): float { return $this->rate_Reclamation; }
    public function setRate_Reclamation(float $value): self { $this->rate_Reclamation = $value; return $this; }
    public function getRateReclamation(): float { return $this->rate_Reclamation; }
    public function setRateReclamation(float $value): self { return $this->setRate_Reclamation($value); }

    public function getRate_sum(): int { return $this->rate_sum; }
    public function setRate_sum(int $value): self { $this->rate_sum = $value; return $this; }
    public function getRateSum(): int { return $this->rate_sum; }
    public function setRateSum(int $value): self { return $this->setRate_sum($value); }

    public function getRate_count(): int { return $this->rate_count; }
    public function setRate_count(int $value): self { $this->rate_count = $value; return $this; }
    public function getRateCount(): int { return $this->rate_count; }
    public function setRateCount(int $value): self { return $this->setRate_count($value); }

    private static function normalizeStatutCode(?string $value): string
    {
        $status = trim((string) $value);
        return match ($status) {
            'EN_ATTENTE', 'En attente', '' => 'EN_ATTENTE',
            'EN_COURS', 'En cours', 'En cours de traitement' => 'EN_COURS',
            'TRAITEE', 'Traitée', 'Résolu' => 'TRAITEE',
            'REJETEE', 'Rejetée', 'Rejeté' => 'REJETEE',
            default => 'EN_ATTENTE',
        };
    }

    private static function displayStatut(?string $value): string
    {
        $status = trim((string) $value);
        return match ($status) {
            'EN_ATTENTE', '' => 'En attente',
            'EN_COURS' => 'En cours de traitement',
            'TRAITEE' => 'Résolu',
            'REJETEE' => 'Rejeté',
            'En attente' => 'En attente',
            'En cours', 'En cours de traitement' => 'En cours de traitement',
            'Traitée', 'Résolu' => 'Résolu',
            'Rejetée', 'Rejeté' => 'Rejeté',
            default => 'En attente',
        };
    }
}