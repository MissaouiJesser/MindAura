<?php
namespace App\Entity;

use App\Repository\CommentairesRepository;
use App\Validator\NoBadWords;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: CommentairesRepository::class)]
#[ORM\Table(name: 'commentaire')]
class Commentaires
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_commentaires')]
    /** @phpstan-ignore-next-line */
    private ?int $id = null;

   #[ORM\ManyToOne(inversedBy: 'commentaires')]
#[ORM\JoinColumn(name: 'ressource_id', referencedColumnName: 'id_ressources', onDelete: 'CASCADE', nullable: false)]
private ?Ressources $ressource = null;

  #[ORM\Column(name: 'id_user', type: 'integer')]
private int $idUser = 0;



    #[ORM\Column(name: 'user_name', length: 255)]
    #[Assert\NotBlank(message: "Le nom d'utilisateur est obligatoire.")]
    private string $user_name = '';

    #[ORM\Column(type: Types::TEXT)]
    #[Assert\NotBlank(message: 'Le contenu du commentaire est obligatoire.')]
    #[Assert\Length(max: 5000, maxMessage: 'Le commentaire ne doit pas dépasser 5000 caractères.')]
    #[NoBadWords]
    private string $contenu = '';

    #[ORM\Column(name: 'date_publication', type: Types::DATE_MUTABLE)]
    private \DateTimeInterface $datePublication;

    #[ORM\Column]
    private int $likes = 0;

    #[ORM\Column]
    private int $reponse = 0;

    #[ORM\Column(type: 'string', length: 20, options: ['default' => 'ACTIF'])]
    #[Assert\Choice(choices: ['ACTIF', 'SUPPRIME', 'SIGNALE'], message: 'Statut invalide.')]
    private string $status = 'ACTIF';

    #[ORM\Column(length: 20, nullable: true)]
    private ?string $sentiment = null;

    /**
     * @var array<int, string>|null
     */
    #[ORM\Column(type: 'json', nullable: true)]
    private ?array $themes = null;

    #[ORM\Column(name: 'ai_summary', type: Types::TEXT, nullable: true)]
    private ?string $aiSummary = null;

    #[ORM\ManyToOne(targetEntity: self::class, inversedBy: 'reponses')]
    #[ORM\JoinColumn(name: 'parent_id', referencedColumnName: 'id_commentaires', nullable: true, onDelete: 'CASCADE')]
    private ?self $parent = null;

    /**
     * @var Collection<int, Commentaires>
     */
    #[ORM\OneToMany(mappedBy: 'parent', targetEntity: self::class, cascade: ['remove'])]
    private Collection $reponses;

    public function __construct()
    {
        $this->reponses        = new ArrayCollection();
        $this->datePublication = new \DateTime();
    }

    public function getId(): ?int { return $this->id; }

    public function getRessource(): ?Ressources { return $this->ressource; }
    public function setRessource(?Ressources $ressource): static { $this->ressource = $ressource; return $this; }

    public function getIdUser(): int { return $this->idUser; }
    public function setIdUser(int $idUser): static { $this->idUser = $idUser; return $this; }

    public function getUserName(): string { return $this->user_name; }
    public function setUserName(string $user_name): static { $this->user_name = $user_name; return $this; }

    public function getContenu(): string { return $this->contenu; }
    public function setContenu(string $contenu): static { $this->contenu = $contenu; return $this; }

    public function getDatePublication(): \DateTimeInterface { return $this->datePublication; }
    public function setDatePublication(\DateTimeInterface $datePublication): static { $this->datePublication = $datePublication; return $this; }

    public function getLikes(): int { return $this->likes; }
    public function setLikes(int $likes): static { $this->likes = $likes; return $this; }
    public function incrementLikes(): static { $this->likes++; return $this; }

    public function getReponse(): int { return $this->reponse; }
    public function setReponse(int $reponse): static { $this->reponse = $reponse; return $this; }

    public function getStatus(): string { return $this->status; }
    public function setStatus(string $status): static { $this->status = $status; return $this; }

    public function getSentiment(): ?string { return $this->sentiment; }
    public function setSentiment(?string $sentiment): static { $this->sentiment = $sentiment; return $this; }

    /**
     * @return array<int, string>|null
     */
    public function getThemes(): ?array { return $this->themes; }

    /**
     * @param array<int, string>|null $themes
     */
    public function setThemes(?array $themes): static { $this->themes = $themes; return $this; }

    public function getAiSummary(): ?string { return $this->aiSummary; }
    public function setAiSummary(?string $aiSummary): static { $this->aiSummary = $aiSummary; return $this; }

    public function getParent(): ?self { return $this->parent; }
    public function setParent(?self $parent): static { $this->parent = $parent; return $this; }

    /**
     * @return Collection<int, Commentaires>
     */
    public function getReponses(): Collection { return $this->reponses; }

    public function addReponse(self $reponse): static
    {
        if (!$this->reponses->contains($reponse)) {
            $this->reponses->add($reponse);
            $reponse->setParent($this);
        }
        return $this;
    }

    public function removeReponse(self $reponse): static
    {
        if ($this->reponses->removeElement($reponse)) {
            if ($reponse->getParent() === $this) {
                $reponse->setParent(null);
            }
        }
        return $this;
    }
}