<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use App\Entity\Reclamation;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity]
#[ORM\Table(name: 'reponse')]
class Reponse
{
    #[ORM\Id]
    #[ORM\GeneratedValue(strategy: 'AUTO')]
    #[ORM\Column(name: 'id_reponse', type: 'integer')]
    private int $id_reponse = 0;

    #[ORM\ManyToOne(targetEntity: Reclamation::class, inversedBy: "reponses")]
   #[ORM\JoinColumn(name: 'reclamation_id', referencedColumnName: 'id_reclamation', nullable: false, onDelete: 'CASCADE')]
    #[Assert\NotNull(message: 'La réclamation associée est obligatoire')]
    private ?Reclamation $reclamation = null;

    #[ORM\Column(type: "text")]
    #[Assert\NotBlank(message: 'Le contenu de la réponse ne peut pas être vide')]
    #[Assert\Length(
        min: 5,
        max: 5000,
        minMessage: 'Le contenu doit contenir au moins {{ limit }} caractères',
        maxMessage: 'Le contenu ne doit pas dépasser {{ limit }} caractères'
    )]
    private string $contenu_reponse = '';

    #[ORM\Column(type: "datetime")]
    #[Assert\NotNull(message: 'La date de la réponse est obligatoire')]
    private \DateTimeInterface $date_reponse;

    #[ORM\Column(type: "integer", nullable: true)]
    #[Assert\NotNull(message: "L'ID utilisateur est obligatoire")]
    #[Assert\GreaterThan(0, message: "L'ID utilisateur doit être positif")]
    private ?int $id_utilisateur = null;

    #[ORM\Column(type: "string", length: 255, nullable: true)]
    private ?string $nom_utilisateur = null;

    #[ORM\Column(type: "float")]
    #[Assert\Range(
        min: 0,
        max: 5,
        notInRangeMessage: 'La note doit être entre {{ min }} et {{ max }}'
    )]
    private float $rate_reponse = 0;

    #[ORM\Column(type: "integer")]
    #[Assert\GreaterThanOrEqual(0, message: 'La somme des notes doit être positive ou zéro')]
    private int $rate_sum = 0;

    #[ORM\Column(type: "integer")]
    #[Assert\GreaterThanOrEqual(0, message: 'Le nombre de notes doit être positif ou zéro')]
    private int $rate_count = 0;

    public function __construct()
    {
        $this->date_reponse = new \DateTime();
        $this->id_utilisateur = null;
    }

    // ── Getters / Setters snake_case (compatibilité legacy) ───────────────────

    public function getId_reponse(): int
    {
        return $this->id_reponse;
    }

    public function getReclamation(): ?Reclamation
    {
        return $this->reclamation;
    }

    public function setReclamation(?Reclamation $reclamation): self
    {
        $this->reclamation = $reclamation;
        return $this;
    }

    public function getContenu_reponse(): string
    {
        return $this->contenu_reponse;
    }

    public function setContenu_reponse(string $value): self
    {
        $this->contenu_reponse = $value;
        return $this;
    }

    public function getDate_reponse(): \DateTimeInterface
    {
        return $this->date_reponse;
    }

    // protected — les timestamps ne doivent pas être manipulés manuellement
    protected function setDate_reponse(\DateTimeInterface $value): self
    {
        $this->date_reponse = $value;
        return $this;
    }

    public function getId_utilisateur(): ?int
    {
        return $this->id_utilisateur;
    }

    public function setId_utilisateur(?int $value): self
    {
        $this->id_utilisateur = $value;
        return $this;
    }

    public function getNom_utilisateur(): ?string
    {
        return $this->nom_utilisateur;
    }

    public function setNom_utilisateur(?string $value): self
    {
        $this->nom_utilisateur = $value;
        return $this;
    }

    public function getRate_reponse(): float
    {
        return $this->rate_reponse;
    }

    public function setRate_reponse(float $value): self
    {
        $this->rate_reponse = $value;
        return $this;
    }

    public function getRate_sum(): int
    {
        return $this->rate_sum;
    }

    public function setRate_sum(int $value): self
    {
        $this->rate_sum = $value;
        return $this;
    }

    public function getRate_count(): int
    {
        return $this->rate_count;
    }

    public function setRate_count(int $value): self
    {
        $this->rate_count = $value;
        return $this;
    }

    // ── Getters / Setters camelCase (PropertyAccess / EntityType / Symfony forms) ──

    public function getIdReponse(): int
    {
        return $this->id_reponse;
    }

    public function getContenuReponse(): string
    {
        return $this->contenu_reponse;
    }

    public function setContenuReponse(string $value): self
    {
        return $this->setContenu_reponse($value);
    }

    public function getDateReponse(): \DateTimeInterface
    {
        return $this->date_reponse;
    }

    // protected — timestamp géré automatiquement via le constructeur
    protected function setDateReponse(\DateTimeInterface $value): self
    {
        return $this->setDate_reponse($value);
    }

    public function getIdUtilisateur(): ?int
    {
        return $this->id_utilisateur;
    }

    public function setIdUtilisateur(?int $value): self
    {
        return $this->setId_utilisateur($value);
    }

    public function getNomUtilisateur(): ?string
    {
        return $this->nom_utilisateur;
    }

    public function setNomUtilisateur(?string $value): self
    {
        return $this->setNom_utilisateur($value);
    }

    public function getRateReponse(): float
    {
        return $this->rate_reponse;
    }

    public function setRateReponse(float $value): self
    {
        return $this->setRate_reponse($value);
    }

    public function getRateSum(): int
    {
        return $this->rate_sum;
    }

    public function setRateSum(int $value): self
    {
        return $this->setRate_sum($value);
    }

    public function getRateCount(): int
    {
        return $this->rate_count;
    }

    public function setRateCount(int $value): self
    {
        return $this->setRate_count($value);
    }
}