<?php
namespace App\Entity;

use App\Repository\RessourcesRepository;
use App\Validator\NoBadWords;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: RessourcesRepository::class)]
#[ORM\Table(name: 'ressource')]
class Ressources
{
   #[ORM\Id]
#[ORM\GeneratedValue]
#[ORM\Column(name: 'id_ressources', type: 'integer')]
/** @phpstan-ignore-next-line */
private ?int $id = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: 'Le titre est obligatoire.')]
    #[Assert\Length(max: 255, maxMessage: 'Le titre ne doit pas dépasser 255 caractères.')]
    #[NoBadWords]
    private string $titre = '';

    #[ORM\Column(type: Types::TEXT)]
    #[Assert\NotBlank(message: 'Le résumé est obligatoire.')]
    #[NoBadWords]
    private string $resume = '';

    #[ORM\Column(length: 50)]
    #[Assert\NotBlank(message: 'Le type de contenu est obligatoire.')]
    #[Assert\Choice(choices: ['Article', 'Video', 'Podcast', 'PDF'], message: 'Type de contenu invalide.')]
    private string $contenu = '';

    #[ORM\Column(length: 100)]
    #[Assert\NotBlank(message: 'La catégorie est obligatoire.')]
    private string $categorie = '';

    #[ORM\Column(type: Types::TEXT)]
    private string $tags = '';

    #[ORM\Column(name: 'date_publication', type: Types::DATE_MUTABLE)]
    private \DateTimeInterface $date_publication;

    #[ORM\Column(name: 'nbr_vues')]
    private int $nbr_vues = 0;

    #[ORM\Column]
    private int $likes = 0;

    #[ORM\Column(length: 20)]
    #[Assert\Choice(choices: ['DEBUTANT', 'INTERMEDIAIRE', 'AVANCE'], message: 'Niveau invalide.')]
    private string $niveau = 'DEBUTANT';

    #[ORM\Column(name: 'duree_lecture')]
    #[Assert\PositiveOrZero(message: 'La durée de lecture doit être positive ou nulle.')]
    private int $duree_lecture = 0;

    #[ORM\Column(name: 'image_url', length: 255, nullable: true)]
    private ?string $image_url = null;

    #[ORM\Column(length: 255, nullable: true)]
    #[Assert\When(
        expression: "this.getContenu() in ['Article', 'Podcast']",
        constraints: [
            new Assert\NotBlank(message: 'Le lien est obligatoire pour un article ou podcast.'),
            new Assert\Url(message: "Le lien n'est pas une URL valide."),
        ]
    )]
    private ?string $url = null;

    #[ORM\Column(name: 'email_auteur', length: 255, nullable: true)]
    #[Assert\Email(message: "L'email auteur n'est pas valide.")]
    private ?string $email_auteur = null;

    /**
     * @var Collection<int, Commentaires>
     */
    #[ORM\OneToMany(mappedBy: 'ressource', targetEntity: Commentaires::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $commentaires;

    public function __construct()
    {
        $this->commentaires     = new ArrayCollection();
        $this->date_publication = new \DateTime();
    }

    /**
     * @return array<string, string>
     */
    public static function getContenuChoices(): array
    {
        return ['Article' => 'Article', 'Vidéo' => 'Video', 'Podcast' => 'Podcast', 'PDF' => 'PDF'];
    }

    /**
     * @return array<string, string>
     */
    public static function getNiveauChoices(): array
    {
        return ['Débutant' => 'DEBUTANT', 'Intermédiaire' => 'INTERMEDIAIRE', 'Avancé' => 'AVANCE'];
    }

    /**
     * @return array<string, string>
     */
    public static function getCategorieChoices(): array
    {
        return [
            'Gestion du stress'         => 'gestion_stress',
            'Confiance en soi'          => 'confiance_en_soi',
            'Motivation'                => 'motivation',
            'Communication'             => 'communication',
            'Bien-être'                 => 'bien_etre',
            'Protectivité'              => 'protectivite',
            'Intelligence émotionnelle' => 'intelligence_emotionnelle',
        ];
    }

    public function getId(): ?int { return $this->id; }

    public function getTitre(): string { return $this->titre; }
    public function setTitre(string $titre): static { $this->titre = $titre; return $this; }

    public function getResume(): string { return $this->resume; }
    public function setResume(string $resume): static { $this->resume = $resume; return $this; }

    public function getContenu(): string { return $this->contenu; }
    public function setContenu(string $contenu): static { $this->contenu = $contenu; return $this; }

    public function getCategorie(): string { return $this->categorie; }
    public function setCategorie(string $categorie): static { $this->categorie = $categorie; return $this; }

    public function getTags(): string { return $this->tags; }
    public function setTags(string $tags): static { $this->tags = $tags; return $this; }

    public function getDatePublication(): \DateTimeInterface { return $this->date_publication; }
    public function setDatePublication(\DateTimeInterface $date_publication): static { $this->date_publication = $date_publication; return $this; }

    public function getNbrVues(): int { return $this->nbr_vues; }
    public function setNbrVues(int $nbr_vues): static { $this->nbr_vues = $nbr_vues; return $this; }
    public function incrementVues(): static { $this->nbr_vues++; return $this; }

    public function getLikes(): int { return $this->likes; }
    public function setLikes(int $likes): static { $this->likes = $likes; return $this; }
    public function incrementLikes(): static { $this->likes++; return $this; }

    public function getNiveau(): string { return $this->niveau; }
    public function setNiveau(string $niveau): static { $this->niveau = $niveau; return $this; }

    public function getDureeLecture(): int { return $this->duree_lecture; }
    public function setDureeLecture(int $duree_lecture): static { $this->duree_lecture = $duree_lecture; return $this; }

    public function getImageUrl(): ?string { return $this->image_url; }
    public function setImageUrl(?string $image_url): static { $this->image_url = $image_url; return $this; }

    public function getUrl(): ?string { return $this->url; }
    public function setUrl(?string $url): static { $this->url = $url; return $this; }

    public function getEmailAuteur(): ?string { return $this->email_auteur; }
    public function setEmailAuteur(?string $email_auteur): static { $this->email_auteur = $email_auteur; return $this; }

    /**
     * @return Collection<int, Commentaires>
     */
    public function getCommentaires(): Collection
    {
        return $this->commentaires;
    }

    public function addCommentaire(Commentaires $commentaire): static
    {
        if (!$this->commentaires->contains($commentaire)) {
            $this->commentaires->add($commentaire);
            $commentaire->setRessource($this);
        }
        return $this;
    }

    public function removeCommentaire(Commentaires $commentaire): static
    {
        if ($this->commentaires->removeElement($commentaire)) {
            if ($commentaire->getRessource() === $this) {
                $commentaire->setRessource(null);
            }
        }
        return $this;
    }
}