<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Validator\Constraints as Assert;

use App\Repository\TestPsychologiqueRepository;

#[ORM\Entity(repositoryClass: TestPsychologiqueRepository::class)]
#[ORM\Table(name: 'test_psychologique')]
#[ORM\HasLifecycleCallbacks]
class TestPsychologique
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id_test = null;  // ← auto-generated: stays ?int

    public function getId_test(): ?int { return $this->id_test; }
    public function setId_test(int $id_test): self { $this->id_test = $id_test; return $this; }
    public function getIdTest(): ?int { return $this->id_test; }

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "Le titre du test est obligatoire.")]
    #[Assert\Length(min: 3, max: 255, minMessage: "Le titre doit contenir au moins {{ limit }} caractères.", maxMessage: "Le titre ne peut pas dépasser {{ limit }} caractères.")]
    private string $titre_test = '';  // ← was ?string

    public function getTitre_test(): string { return $this->titre_test; }
    public function setTitre_test(string $titre_test): self { $this->titre_test = $titre_test; return $this; }
    public function getTitreTest(): string { return $this->titre_test; }
    public function setTitreTest(string $titre_test): static { $this->titre_test = $titre_test; return $this; }

    #[ORM\Column(type: 'text', nullable: false)]
    #[Assert\NotBlank(message: "La description du test est obligatoire.")]
    #[Assert\Length(max: 1000, maxMessage: "La description ne peut pas dépasser {{ limit }} caractères.")]
    private string $description_test = '';  // ← was ?string

    public function getDescription_test(): string { return $this->description_test; }
    public function setDescription_test(string $description_test): self { $this->description_test = $description_test; return $this; }  // ← param no longer nullable
    public function getDescriptionTest(): string { return $this->description_test; }
    public function setDescriptionTest(string $description_test): static { $this->description_test = $description_test; return $this; }  // ← param no longer nullable

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "Le type de test est obligatoire.")]
    #[Assert\Choice(choices: ['personnalité', 'stress', 'logique', 'mémoire', 'autre'], message: "Le type de test doit être l'un des suivants : {{ choices }}.")]
    #[Assert\Length(max: 255)]
    private string $type_test = '';  // ← was ?string

    public function getType_test(): string { return $this->type_test; }
    public function setType_test(string $type_test): self { $this->type_test = $type_test; return $this; }  // ← param no longer nullable
    public function getTypeTest(): string { return $this->type_test; }
    public function setTypeTest(string $type_test): static { $this->type_test = $type_test; return $this; }  // ← param no longer nullable

    #[ORM\Column(type: 'integer', nullable: false)]
    #[Assert\NotNull(message: "La durée estimée est obligatoire.")]
    #[Assert\Positive(message: "La durée estimée doit être positive.")]
    #[Assert\Range(min: 1, max: 1440, notInRangeMessage: "La durée estimée doit être entre {{ min }} et {{ max }} minutes.")]
    private int $duree_estimee = 0;  // ← was ?int

    public function getDuree_estimee(): int { return $this->duree_estimee; }
    public function setDuree_estimee(int $duree_estimee): self { $this->duree_estimee = $duree_estimee; return $this; }  // ← param no longer nullable
    public function getDureeEstimee(): int { return $this->duree_estimee; }
    public function setDureeEstimee(int $duree_estimee): static { $this->duree_estimee = $duree_estimee; return $this; }  // ← param no longer nullable

    #[ORM\Column(type: 'text', nullable: false)]
    #[Assert\NotBlank(message: "Les instructions du test sont obligatoires.")]
    #[Assert\Length(max: 1000, maxMessage: "Les instructions ne peuvent pas dépasser {{ limit }} caractères.")]
    private string $instructions_test = '';  // ← was ?string

    public function getInstructions_test(): string { return $this->instructions_test; }
    public function setInstructions_test(string $instructions_test): self { $this->instructions_test = $instructions_test; return $this; }  // ← param no longer nullable
    public function getInstructionsTest(): string { return $this->instructions_test; }
    public function setInstructionsTest(string $instructions_test): static { $this->instructions_test = $instructions_test; return $this; }  // ← param no longer nullable

    // ─── Dates (nullable: true → stay as-is) ─────────────────────────────────

    #[ORM\Column(type: 'datetime_immutable', nullable: true)]
    private ?\DateTimeImmutable $date_creation = null;

    #[ORM\Column(type: 'datetime_immutable', nullable: true)]
    private ?\DateTimeImmutable $date_modification = null;

    #[ORM\PrePersist]
    public function initDates(): void
    {
        if ($this->date_creation === null) {
            $this->date_creation = new \DateTimeImmutable();
        }
        $this->date_modification = new \DateTimeImmutable();
    }

    #[ORM\PreUpdate]
    public function updateDateModification(): void
    {
        $this->date_modification = new \DateTimeImmutable();
    }

    public function getDate_creation(): ?\DateTimeImmutable { return $this->date_creation; }
    protected function setDate_creation(?\DateTimeImmutable $date_creation): self { $this->date_creation = $date_creation; return $this; }
    public function getDateCreation(): ?\DateTimeImmutable { return $this->date_creation; }
    protected function setDateCreation(?\DateTimeImmutable $date_creation): static { $this->date_creation = $date_creation; return $this; }

    public function getDate_modification(): ?\DateTimeImmutable { return $this->date_modification; }
    protected function setDate_modification(?\DateTimeImmutable $date_modification): self { $this->date_modification = $date_modification; return $this; }
    public function getDateModification(): ?\DateTimeImmutable { return $this->date_modification; }
    protected function setDateModification(?\DateTimeImmutable $date_modification): static { $this->date_modification = $date_modification; return $this; }

    // ─── Est actif ────────────────────────────────────────────────────────────

    #[ORM\Column(type: 'boolean', nullable: false)]
    #[Assert\NotNull(message: "Le champ actif est obligatoire.")]
    private bool $est_actif = false;  // ← was ?bool

    public function isEst_actif(): bool { return $this->est_actif; }
    public function setEst_actif(bool $est_actif): self { $this->est_actif = $est_actif; return $this; }  // ← param no longer nullable
    public function isEstActif(): bool { return $this->est_actif; }
    public function setEstActif(bool $est_actif): static { $this->est_actif = $est_actif; return $this; }  // ← param no longer nullable

    // ─── Relations ────────────────────────────────────────────────────────────

    /** @var Collection<int, ReponseClient> */
    #[ORM\OneToMany(targetEntity: ReponseClient::class, mappedBy: 'testPsychologique')]
    private Collection $reponseClients;

    /** @var Collection<int, QuestionReponse> */
    #[ORM\OneToMany(targetEntity: QuestionReponse::class, mappedBy: 'testPsychologique')]
    private Collection $questionReponses;

    public function __construct()
    {
        $this->reponseClients   = new ArrayCollection();
        $this->questionReponses = new ArrayCollection();
    }

    /** @return Collection<int, ReponseClient> */
    public function getReponseClients(): Collection { return $this->reponseClients; }

    public function addReponseClient(ReponseClient $reponseClient): self
    {
        if (!$this->reponseClients->contains($reponseClient)) {
            $this->reponseClients->add($reponseClient);
        }
        return $this;
    }

    public function removeReponseClient(ReponseClient $reponseClient): self
    {
        $this->reponseClients->removeElement($reponseClient);
        return $this;
    }

    /** @return Collection<int, QuestionReponse> */
    public function getQuestionReponses(): Collection { return $this->questionReponses; }

    public function addQuestionReponse(QuestionReponse $questionReponse): self
    {
        if (!$this->questionReponses->contains($questionReponse)) {
            $this->questionReponses->add($questionReponse);
            $questionReponse->setTestPsychologique($this);
        }
        return $this;
    }

    public function removeQuestionReponse(QuestionReponse $questionReponse): self
    {
        if ($this->questionReponses->removeElement($questionReponse)) {
            if ($questionReponse->getTestPsychologique() === $this) {
                $questionReponse->setTestPsychologique(null);
            }
        }
        return $this;
    }
}