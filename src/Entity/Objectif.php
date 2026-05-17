<?php

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Component\Validator\Context\ExecutionContextInterface;

use App\Repository\ObjectifRepository;

#[ORM\Entity(repositoryClass: ObjectifRepository::class)]
#[ORM\Table(name: 'objectif')]
#[ORM\HasLifecycleCallbacks]
class Objectif
{
    // ─── Clé primaire ────────────────────────────────────────────────────────

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id_objectif = null;

    public function getId_objectif(): ?int { return $this->id_objectif; }
    public function setId_objectif(int $v): self { $this->id_objectif = $v; return $this; }
    public function getIdObjectif(): ?int  { return $this->id_objectif; }

    // ─── Source ──────────────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', length: 20, nullable: false, options: ['default' => 'admin'])]
    #[Assert\Choice(choices: ['admin', 'patient'], message: "La source doit être 'admin' ou 'patient'.")]
    private string $source = 'admin';

    public function getSource(): string        { return $this->source; }
    public function setSource(string $v): static { $this->source = $v; return $this; }

    #[ORM\Column(type: 'integer', nullable: true)]
    private ?int $id_utilisateur = null;

    public function getId_utilisateur(): ?int { return $this->id_utilisateur; }
    public function setId_utilisateur(?int $v): self { $this->id_utilisateur = $v; return $this; }
    public function getIdUtilisateur(): ?int { return $this->id_utilisateur; }
    public function setIdUtilisateur(?int $v): static { $this->id_utilisateur = $v; return $this; }

    // ─── Titre ───────────────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "Le titre de l'objectif est obligatoire.")]
    #[Assert\Length(min: 3, max: 255, minMessage: "Le titre doit contenir au moins {{ limit }} caractères.", maxMessage: "Le titre ne peut pas dépasser {{ limit }} caractères.")]
    private string $titre = '';  // ← was ?string

    public function getTitre(): string { return $this->titre; }
    public function setTitre(string $v): self { $this->titre = $v; return $this; }

    // ─── Description ─────────────────────────────────────────────────────────

    #[ORM\Column(type: 'text', nullable: false)]
    #[Assert\NotBlank(message: "La description est obligatoire.")]
    #[Assert\Length(max: 1000, maxMessage: "La description ne peut pas dépasser {{ limit }} caractères.")]
    private string $description = '';  // ← was ?string

    public function getDescription(): string { return $this->description; }
    public function setDescription(string $v): self { $this->description = $v; return $this; }

    // ─── Type objectif ────────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', length: 20, nullable: false, options: ['default' => 'admin'])]
    private string $type_objectif = 'admin';  // ← was ?string

    public function getType_objectif(): string { return $this->type_objectif; }
    public function setType_objectif(string $v): self { $this->type_objectif = $v; return $this; }
    public function getTypeObjectif(): string  { return $this->type_objectif; }
    public function setTypeObjectif(string $v): static { $this->type_objectif = $v; return $this; }

    // ─── Type test ────────────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "Le type de test est obligatoire.")]
    #[Assert\Choice(choices: ['personnalité', 'stress', 'logique', 'mémoire', 'autre'], message: "Le type de test doit être l'un des suivants : {{ choices }}.")]
    #[Assert\Length(max: 255)]
    private string $type_test = '';  // ← was ?string

    public function getType_test(): string { return $this->type_test; }
    public function setType_test(string $v): self { $this->type_test = $v; return $this; }
    public function getTypeTest(): string  { return $this->type_test; }
    public function setTypeTest(string $v): static { $this->type_test = $v; return $this; }

    // ─── Niveau recommandé ────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "Le niveau recommandé est obligatoire.")]
    #[Assert\Choice(choices: ['débutant', 'intermédiaire', 'avancé', 'expert'], message: "Le niveau recommandé doit être l'un des suivants : {{ choices }}.")]
    #[Assert\Length(max: 255)]
    private string $niveau_recommande = '';  // ← was ?string

    public function getNiveau_recommande(): string { return $this->niveau_recommande; }
    public function setNiveau_recommande(string $v): self { $this->niveau_recommande = $v; return $this; }
    public function getNiveauRecommande(): string  { return $this->niveau_recommande; }
    public function setNiveauRecommande(string $v): static { $this->niveau_recommande = $v; return $this; }

    // ─── Score min / max ──────────────────────────────────────────────────────

    #[ORM\Column(type: 'integer', nullable: false)]
    #[Assert\NotNull(message: "Le score minimum est obligatoire.")]
    #[Assert\PositiveOrZero(message: "Le score minimum doit être positif ou nul.")]
    private int $score_min = 0;  // ← was ?int

    public function getScore_min(): int { return $this->score_min; }
    public function setScore_min(int $v): self { $this->score_min = $v; return $this; }
    public function getScoreMin(): int  { return $this->score_min; }
    public function setScoreMin(int $v): static { $this->score_min = $v; return $this; }

    #[ORM\Column(type: 'integer', nullable: false)]
    #[Assert\NotNull(message: "Le score maximum est obligatoire.")]
    #[Assert\PositiveOrZero(message: "Le score maximum doit être positif ou nul.")]
    private int $score_max = 0;  // ← was ?int

    public function getScore_max(): int { return $this->score_max; }
    public function setScore_max(int $v): self { $this->score_max = $v; return $this; }
    public function getScoreMax(): int  { return $this->score_max; }
    public function setScoreMax(int $v): static { $this->score_max = $v; return $this; }

    // ─── Catégorie ────────────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "La catégorie est obligatoire.")]
    #[Assert\Choice(choices: ['développement personnel', 'carrière', 'éducation', 'santé mentale', 'autre'], message: "La catégorie doit être l'une des suivantes : {{ choices }}.")]
    #[Assert\Length(max: 255)]
    private string $categorie = '';  // ← was ?string

    public function getCategorie(): string { return $this->categorie; }
    public function setCategorie(string $v): self { $this->categorie = $v; return $this; }

    // ─── Durée estimée ────────────────────────────────────────────────────────

    #[ORM\Column(type: 'integer', nullable: false)]
    #[Assert\NotNull(message: "La durée estimée est obligatoire.")]
    #[Assert\Positive(message: "La durée estimée doit être positive.")]
    private int $duree_estimee = 0;  // ← was ?int

    public function getDuree_estimee(): int { return $this->duree_estimee; }
    public function setDuree_estimee(int $v): self { $this->duree_estimee = $v; return $this; }
    public function getDureeEstimee(): int  { return $this->duree_estimee; }
    public function setDureeEstimee(int $v): static { $this->duree_estimee = $v; return $this; }

    // ─── Difficulté ───────────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "La difficulté est obligatoire.")]
    #[Assert\Choice(choices: ['facile', 'moyen', 'difficile', 'très difficile'], message: "La difficulté doit être l'une des suivantes : {{ choices }}.")]
    #[Assert\Length(max: 255)]
    private string $difficule = '';  // ← was ?string

    public function getDifficule(): string  { return $this->difficule; }
    public function setDifficule(string $v): self { $this->difficule = $v; return $this; }
    public function getDifficulte(): string { return $this->difficule; }

    // ─── Dates ────────────────────────────────────────────────────────────────

    #[ORM\Column(type: 'datetime_immutable', nullable: true)]
    private ?\DateTimeImmutable $date_creation = null;

    #[ORM\PrePersist]
    public function initDateCreation(): void
    {
        if ($this->date_creation === null) {
            $this->date_creation = new \DateTimeImmutable();
        }
    }

    public function getDate_creation(): ?\DateTimeImmutable { return $this->date_creation; }
    protected function setDate_creation(?\DateTimeImmutable $v): self { $this->date_creation = $v; return $this; }
    public function getDateCreation(): ?\DateTimeImmutable { return $this->date_creation; }
    protected function setDateCreation(?\DateTimeImmutable $v): static { $this->date_creation = $v; return $this; }

    #[ORM\Column(type: 'date_immutable', nullable: false)]
    #[Assert\NotNull(message: "La date d'échéance est obligatoire.")]
    private \DateTimeImmutable $date_echeance;  // ← was ?\DateTimeImmutable, removed = null

    public function getDate_echeance(): \DateTimeImmutable { return $this->date_echeance; }
    public function setDate_echeance(\DateTimeImmutable $v): self { $this->date_echeance = $v; return $this; }
    public function getDateEcheance(): \DateTimeImmutable { return $this->date_echeance; }
    public function setDateEcheance(\DateTimeImmutable $v): static { $this->date_echeance = $v; return $this; }

    // ─── Statut ───────────────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "Le statut est obligatoire.")]
    #[Assert\Choice(choices: ['actif', 'inactif'], message: "Le statut doit être 'actif' ou 'inactif'.")]
    private string $statut = '';  // ← was ?string

    public function getStatut(): string { return $this->statut; }
    public function setStatut(string $v): self { $this->statut = $v; return $this; }

    // ─── Est public ───────────────────────────────────────────────────────────

    #[ORM\Column(type: 'boolean', nullable: false)]
    #[Assert\NotNull(message: "Le champ public est obligatoire.")]
    private bool $est_public = false;  // ← was ?bool

    public function isEst_public(): bool { return $this->est_public; }
    public function setEst_public(bool $v): self { $this->est_public = $v; return $this; }
    public function isEstPublic(): bool  { return $this->est_public; }
    public function setEstPublic(bool $v): static { $this->est_public = $v; return $this; }

    // ─── Validation ──────────────────────────────────────────────────────────

    #[Assert\Callback]
    public function validate(ExecutionContextInterface $context): void
    {
        if ($this->score_max < $this->score_min) {
            $context->buildViolation('Le score maximum doit être supérieur ou égal au score minimum.')
                ->atPath('score_max')
                ->addViolation();
        }
    }
}