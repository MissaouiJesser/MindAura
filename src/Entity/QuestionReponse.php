<?php

namespace App\Entity;

use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Validator\Constraints as Assert;

use App\Repository\QuestionReponseRepository;

#[ORM\Entity(repositoryClass: QuestionReponseRepository::class)]
#[ORM\Table(name: 'question_reponse')]
class QuestionReponse
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id_question_reponse = null;  // ← auto-generated: stays ?int

    public function getId_question_reponse(): ?int { return $this->id_question_reponse; }
    public function setId_question_reponse(int $id_question_reponse): self { $this->id_question_reponse = $id_question_reponse; return $this; }
    public function getIdQuestionReponse(): ?int { return $this->id_question_reponse; }

    // ─── Relation ManyToOne vers TestPsychologique ───────────────────────────
    // JoinColumn nullable:false but PHP stays ?T — object relations need null until assigned

    #[ORM\ManyToOne(targetEntity: TestPsychologique::class, inversedBy: 'questionReponses')]
    #[ORM\JoinColumn(name: 'id_test_id', referencedColumnName: 'id_test', nullable: false)]
    private ?TestPsychologique $testPsychologique = null;

    public function getTestPsychologique(): ?TestPsychologique { return $this->testPsychologique; }
    public function setTestPsychologique(?TestPsychologique $testPsychologique): static { $this->testPsychologique = $testPsychologique; return $this; }

    /** @deprecated Utiliser getTestPsychologique() à la place. */
    public function getId_test(): ?int { return $this->testPsychologique?->getIdTest(); }
    public function getIdTest(): ?int  { return $this->testPsychologique?->getIdTest(); }

    // ─── Texte de la question ────────────────────────────────────────────────

    #[ORM\Column(type: 'text', nullable: false)]
    #[Assert\NotBlank(message: "Le texte de la question est obligatoire.")]
    #[Assert\Length(min: 3, max: 2000, minMessage: "Le texte doit contenir au moins {{ limit }} caractères.", maxMessage: "Le texte ne peut pas dépasser {{ limit }} caractères.")]
    private string $texte_question = '';  // ← was ?string

    public function getTexte_question(): string { return $this->texte_question; }
    public function setTexte_question(string $texte_question): self { $this->texte_question = $texte_question; return $this; }
    public function getTexteQuestion(): string { return $this->texte_question; }
    public function setTexteQuestion(string $texte_question): static { $this->texte_question = $texte_question; return $this; }

    // ─── Type de question ────────────────────────────────────────────────────

    #[ORM\Column(type: 'string', nullable: false)]
    #[Assert\NotBlank(message: "Le type de question est obligatoire.")]
    #[Assert\Choice(choices: ['texte_libre', 'choix_unique', 'choix_multiple', 'vrai_faux', 'echelle'], message: "Le type de question doit être valide.")]
    #[Assert\Length(max: 255)]
    private string $type_question = '';  // ← was ?string

    public function getType_question(): string { return $this->type_question; }
    public function setType_question(string $type_question): self { $this->type_question = $type_question; return $this; }  // ← param no longer nullable
    public function getTypeQuestion(): string { return $this->type_question; }
    public function setTypeQuestion(string $type_question): static { $this->type_question = $type_question; return $this; }  // ← param no longer nullable

    // ─── Ordre de la question ────────────────────────────────────────────────

    #[ORM\Column(type: 'integer', nullable: false)]
    #[Assert\NotNull(message: "L'ordre de la question est obligatoire.")]
    #[Assert\Positive(message: "L'ordre de la question doit être supérieur à zéro.")]
    private int $ordre_question = 0;  // ← was ?int

    public function getOrdre_question(): int { return $this->ordre_question; }
    public function setOrdre_question(int $ordre_question): self { $this->ordre_question = $ordre_question; return $this; }  // ← param no longer nullable
    public function getOrdreQuestion(): int { return $this->ordre_question; }
    public function setOrdreQuestion(int $ordre_question): static { $this->ordre_question = $ordre_question; return $this; }  // ← param no longer nullable

    // ─── Options et scores (nullable: true → stays as-is) ────────────────────

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(max: 255)]
    private ?string $option1 = null;

    public function getOption1(): ?string { return $this->option1; }
    public function setOption1(?string $option1): self { $this->option1 = $option1; return $this; }

    #[ORM\Column(type: 'integer', nullable: true)]
    #[Assert\PositiveOrZero]
    private ?int $score1 = null;

    public function getScore1(): ?int { return $this->score1; }
    public function setScore1(?int $score1): self { $this->score1 = $score1; return $this; }

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(max: 255)]
    private ?string $option2 = null;

    public function getOption2(): ?string { return $this->option2; }
    public function setOption2(?string $option2): self { $this->option2 = $option2; return $this; }

    #[ORM\Column(type: 'integer', nullable: true)]
    #[Assert\PositiveOrZero]
    private ?int $score2 = null;

    public function getScore2(): ?int { return $this->score2; }
    public function setScore2(?int $score2): self { $this->score2 = $score2; return $this; }

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(max: 255)]
    private ?string $option3 = null;

    public function getOption3(): ?string { return $this->option3; }
    public function setOption3(?string $option3): self { $this->option3 = $option3; return $this; }

    #[ORM\Column(type: 'integer', nullable: true)]
    #[Assert\PositiveOrZero]
    private ?int $score3 = null;

    public function getScore3(): ?int { return $this->score3; }
    public function setScore3(?int $score3): self { $this->score3 = $score3; return $this; }

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(max: 255)]
    private ?string $option4 = null;

    public function getOption4(): ?string { return $this->option4; }
    public function setOption4(?string $option4): self { $this->option4 = $option4; return $this; }

    #[ORM\Column(type: 'integer', nullable: true)]
    #[Assert\PositiveOrZero]
    private ?int $score4 = null;

    public function getScore4(): ?int { return $this->score4; }
    public function setScore4(?int $score4): self { $this->score4 = $score4; return $this; }

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(max: 255)]
    private ?string $option5 = null;

    public function getOption5(): ?string { return $this->option5; }
    public function setOption5(?string $option5): self { $this->option5 = $option5; return $this; }

    #[ORM\Column(type: 'integer', nullable: true)]
    #[Assert\PositiveOrZero]
    private ?int $score5 = null;

    public function getScore5(): ?int { return $this->score5; }
    public function setScore5(?int $score5): self { $this->score5 = $score5; return $this; }

    // ─── Méthodes agrégées ───────────────────────────────────────────────────

    /**
     * @return array<int, string>
     */
    public function getOptions(): array
    {
        $out = [];
        foreach ([$this->option1, $this->option2, $this->option3, $this->option4, $this->option5] as $v) {
            if ($v === null || trim($v) === '') {
                continue;
            }
            $out[] = $v;
        }
        return $out;
    }

    /**
     * @return int[]
     */
    public function getScores(): array
    {
        $pairs = [
            [$this->option1, $this->score1],
            [$this->option2, $this->score2],
            [$this->option3, $this->score3],
            [$this->option4, $this->score4],
            [$this->option5, $this->score5],
        ];
        $scores = [];
        foreach ($pairs as [$opt, $sc]) {
            if ($opt === null || trim($opt) === '') {
                continue;
            }
            $scores[] = $sc ?? 0;
        }
        return $scores;
    }

    // ─── Est obligatoire (nullable: true → stays as-is) ──────────────────────

    #[ORM\Column(type: 'boolean', nullable: true)]
    private ?bool $est_obligatoire = null;

    public function isEst_obligatoire(): ?bool { return $this->est_obligatoire; }
    public function setEst_obligatoire(?bool $est_obligatoire): self { $this->est_obligatoire = $est_obligatoire; return $this; }
    public function isEstObligatoire(): ?bool { return $this->est_obligatoire; }
    public function setEstObligatoire(?bool $est_obligatoire): static { $this->est_obligatoire = $est_obligatoire; return $this; }

    // ─── Relation OneToMany vers ReponseClient ────────────────────────────────

    /** @var Collection<int, ReponseClient> */
    #[ORM\OneToMany(targetEntity: ReponseClient::class, mappedBy: 'questionReponse')]
    private Collection $reponseClients;

    public function __construct()
    {
        $this->reponseClients = new ArrayCollection();
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
}