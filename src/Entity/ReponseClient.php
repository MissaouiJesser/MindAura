<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Validator\Constraints as Assert;

use App\Repository\ReponseClientRepository;

#[ORM\Entity(repositoryClass: ReponseClientRepository::class)]
#[ORM\Table(name: 'reponse_client')]
#[ORM\HasLifecycleCallbacks]
class ReponseClient
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id_reponse_client = null;

    #[ORM\ManyToOne(targetEntity: TestPsychologique::class, inversedBy: 'reponseClients')]
    #[ORM\JoinColumn(name: 'id_test_id', referencedColumnName: 'id_test')]
    private ?TestPsychologique $testPsychologique = null;

    #[ORM\ManyToOne(targetEntity: QuestionReponse::class, inversedBy: 'reponseClients')]
    #[ORM\JoinColumn(name: 'id_question_reponse_id', referencedColumnName: 'id_question_reponse')]
    #[Assert\NotNull]
    private ?QuestionReponse $questionReponse = null;

    // utilisateur_id : convention _id suffix
    #[ORM\Column(name: 'utilisateur_id', type: 'integer', nullable: true)]
    #[Assert\PositiveOrZero]
    private ?int $id_utilisateur = null;

    #[ORM\Column(type: 'string', nullable: true)]
    #[Assert\Length(max: 255)]
    private ?string $option_choisie = null;

    #[ORM\Column(type: 'integer', nullable: true)]
    #[Assert\PositiveOrZero]
    private ?int $score_obtenu = null;

    #[ORM\Column(type: 'text', nullable: true)]
    #[Assert\Length(max: 2000)]
    private ?string $reponse_texte_libre = null;

    #[ORM\Column(type: 'datetime', nullable: true)]
    #[Assert\NotNull]
    #[Assert\Type(\DateTimeInterface::class)]
    private ?\DateTimeInterface $date_reponse = null;

    #[ORM\PrePersist]
    public function initDateReponse(): void
    {
        if ($this->date_reponse === null) {
            $this->date_reponse = new \DateTime();
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public function getId_reponse_client(): ?int
    {
        return $this->id_reponse_client;
    }

    public function getIdReponseClient(): ?int
    {
        return $this->id_reponse_client;
    }

    public function setId_reponse_client(int $id_reponse_client): self
    {
        $this->id_reponse_client = $id_reponse_client;
        return $this;
    }

    public function getTestPsychologique(): ?TestPsychologique
    {
        return $this->testPsychologique;
    }

    public function setTestPsychologique(?TestPsychologique $testPsychologique): self
    {
        $this->testPsychologique = $testPsychologique;
        return $this;
    }

    public function getQuestionReponse(): ?QuestionReponse
    {
        return $this->questionReponse;
    }

    public function setQuestionReponse(?QuestionReponse $questionReponse): self
    {
        $this->questionReponse = $questionReponse;
        return $this;
    }

    public function getId_utilisateur(): ?int
    {
        return $this->id_utilisateur;
    }

    public function setId_utilisateur(?int $id_utilisateur): self
    {
        $this->id_utilisateur = $id_utilisateur;
        return $this;
    }

    public function getIdUtilisateur(): ?int
    {
        return $this->id_utilisateur;
    }

    public function setIdUtilisateur(?int $id_utilisateur): static
    {
        $this->id_utilisateur = $id_utilisateur;
        return $this;
    }

    public function getOption_choisie(): ?string
    {
        return $this->option_choisie;
    }

    public function setOption_choisie(?string $option_choisie): self
    {
        $this->option_choisie = $option_choisie;
        return $this;
    }

    public function getOptionChoisie(): ?string
    {
        return $this->option_choisie;
    }

    public function setOptionChoisie(?string $option_choisie): static
    {
        $this->option_choisie = $option_choisie;
        return $this;
    }

    public function getScore_obtenu(): ?int
    {
        return $this->score_obtenu;
    }

    public function setScore_obtenu(?int $score_obtenu): self
    {
        $this->score_obtenu = $score_obtenu;
        return $this;
    }

    public function getScoreObtenu(): ?int
    {
        return $this->score_obtenu;
    }

    public function setScoreObtenu(?int $score_obtenu): static
    {
        $this->score_obtenu = $score_obtenu;
        return $this;
    }

    public function getReponse_texte_libre(): ?string
    {
        return $this->reponse_texte_libre;
    }

    public function setReponse_texte_libre(?string $reponse_texte_libre): self
    {
        $this->reponse_texte_libre = $reponse_texte_libre;
        return $this;
    }

    public function getReponseTexteLibre(): ?string
    {
        return $this->reponse_texte_libre;
    }

    public function setReponseTexteLibre(?string $reponse_texte_libre): static
    {
        $this->reponse_texte_libre = $reponse_texte_libre;
        return $this;
    }

    public function getDate_reponse(): ?\DateTimeInterface
    {
        return $this->date_reponse;
    }

    protected function setDate_reponse(\DateTimeInterface $date_reponse): self
    {
        $this->date_reponse = $date_reponse;
        return $this;
    }

    public function getDateReponse(): ?\DateTimeInterface
    {
        return $this->date_reponse;
    }

    protected function setDateReponse(\DateTimeInterface $date_reponse): static
    {
        $this->date_reponse = $date_reponse;
        return $this;
    }
}