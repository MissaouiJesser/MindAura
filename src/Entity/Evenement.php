<?php

namespace App\Entity;

use App\Repository\EvenementRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: EvenementRepository::class)]
class Evenement
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore-next-line */
    private ?int $id = null;

    #[ORM\Column(length: 100)]
    #[Assert\NotBlank(message: "L'identifiant est obligatoire.")]
    private string $identifiantEvenemnt = '';

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: "Le titre est obligatoire.")]
    #[Assert\Length(min: 5, minMessage: "Le titre doit avoir au moins {{ limit }} caractères.")]
    private string $titreEvenement = '';

    #[ORM\Column(type: Types::TEXT, nullable: true)]
    private ?string $descriptionEvenement = null;

    #[ORM\Column(type: Types::DATE_MUTABLE, nullable: true)]
    #[Assert\NotBlank(message: "La date de début est obligatoire.")]
    #[Assert\GreaterThan(value: 'today', message: "La date de début doit être supérieure à la date d'aujourd'hui.")]
    private ?\DateTimeInterface $datedebutEvenemnt = null;

    #[ORM\Column(type: Types::DATE_MUTABLE, nullable: true)]
    #[Assert\NotBlank(message: "La date de fin est obligatoire.")]
    #[Assert\GreaterThan(propertyPath: "datedebutEvenemnt", message: "La date de fin doit être strictement après la date de début.")]
    private ?\DateTimeInterface $datefinEvenemnt = null;

    #[ORM\Column(length: 255)]
    #[Assert\NotBlank(message: "Le lieu est obligatoire.")]
    private string $lieuEvenement = '';

    #[ORM\Column(nullable: true)]
    #[Assert\NotNull(message: "La capacité est obligatoire.")]
    #[Assert\Positive(message: "La capacité doit être un nombre positif.")]
    private ?int $capaciteEvenement = null;

    #[ORM\Column(nullable: true)]
    private ?int $maxListeAttente = null;

    #[ORM\Column(length: 50, nullable: true)]
    private ?string $statutEvenemnt = null;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $image = null;

    // FIX: onDelete: 'CASCADE' aligns the DB-level FK with ORM cascade="remove" on TypeEvenement,
    // preventing FK violations when TypeEvenement rows are deleted via direct SQL.
    #[ORM\ManyToOne(inversedBy: 'evenements')]
    #[ORM\JoinColumn(nullable: false, onDelete: 'CASCADE')]
    #[Assert\NotNull(message: "Le type d'événement est obligatoire.")]
    private ?TypeEvenement $typeEvenement = null;

    /**
     * @var Collection<int, Participation>
     */
    #[ORM\OneToMany(targetEntity: Participation::class, mappedBy: 'evenement', orphanRemoval: true, cascade: ['persist', 'remove'])]
    private Collection $participations;

    /**
     * @var Collection<int, ChatMessageevent>
     */
    #[ORM\OneToMany(targetEntity: ChatMessageevent::class, mappedBy: 'evenement', orphanRemoval: true, cascade: ['persist'])]
    private Collection $chatMessages;

    public function __construct()
    {
        $this->participations = new ArrayCollection();
        $this->chatMessages = new ArrayCollection();
    }

    public function getId(): ?int { return $this->id; }

    public function getIdentifiantEvenemnt(): string { return $this->identifiantEvenemnt; }
    public function setIdentifiantEvenemnt(string $v): static { $this->identifiantEvenemnt = $v; return $this; }

    public function getTitreEvenement(): string { return $this->titreEvenement; }
    public function setTitreEvenement(string $v): static { $this->titreEvenement = $v; return $this; }

    public function getDescriptionEvenement(): ?string { return $this->descriptionEvenement; }
    public function setDescriptionEvenement(?string $v): static { $this->descriptionEvenement = $v; return $this; }

    public function getDatedebutEvenemnt(): ?\DateTimeInterface { return $this->datedebutEvenemnt; }
    public function setDatedebutEvenemnt(?\DateTimeInterface $v): static { $this->datedebutEvenemnt = $v; return $this; }

    public function getDatefinEvenemnt(): ?\DateTimeInterface { return $this->datefinEvenemnt; }
    public function setDatefinEvenemnt(?\DateTimeInterface $v): static { $this->datefinEvenemnt = $v; return $this; }

    public function getLieuEvenement(): string { return $this->lieuEvenement; }
    public function setLieuEvenement(string $v): static { $this->lieuEvenement = $v; return $this; }

    public function getCapaciteEvenement(): ?int { return $this->capaciteEvenement; }
    public function setCapaciteEvenement(?int $v): static { $this->capaciteEvenement = $v; return $this; }

    public function getStatutEvenemnt(): ?string { return $this->statutEvenemnt; }
    public function setStatutEvenemnt(?string $v): static { $this->statutEvenemnt = $v; return $this; }

    public function getImage(): ?string { return $this->image; }
    public function setImage(?string $v): static { $this->image = $v; return $this; }

    public function getTypeEvenement(): ?TypeEvenement { return $this->typeEvenement; }
    public function setTypeEvenement(?TypeEvenement $v): static { $this->typeEvenement = $v; return $this; }

    /** @return Collection<int, Participation> */
    public function getParticipations(): Collection { return $this->participations; }

    public function addParticipation(Participation $participation): static {
        if (!$this->participations->contains($participation)) {
            $this->participations->add($participation);
            $participation->setEvenement($this);
        }
        return $this;
    }

    public function removeParticipation(Participation $participation): static {
        if ($this->participations->removeElement($participation) && $participation->getEvenement() === $this) {
            $participation->setEvenement(null);
        }
        return $this;
    }

    public function getMaxListeAttente(): ?int { return $this->maxListeAttente; }
    public function setMaxListeAttente(?int $v): static { $this->maxListeAttente = $v; return $this; }

    public function getNbParticipants(): int {
        return $this->participations->filter(fn($p) => $p->getStatut() === 'confirmee')->count();
    }

    public function hasPlacesDisponibles(): bool {
        return $this->capaciteEvenement === null || $this->getNbParticipants() < $this->capaciteEvenement;
    }

    public function getNbEnAttente(): int {
        return $this->participations->filter(fn($p) => $p->getStatut() === 'en_attente')->count();
    }

    public function hasPlaceEnAttente(): bool {
        if ($this->maxListeAttente === 0) return false;
        if ($this->maxListeAttente === null) return true;
        return $this->getNbEnAttente() < $this->maxListeAttente;
    }

    public function getNextPositionAttente(): int {
        return $this->getNbEnAttente() + 1;
    }

    /** @return Collection<int, ChatMessageevent> */
    public function getChatMessages(): Collection { return $this->chatMessages; }

    public function addChatMessage(ChatMessageevent $chatMessage): static {
        if (!$this->chatMessages->contains($chatMessage)) {
            $this->chatMessages->add($chatMessage);
            $chatMessage->setEvenement($this);
        }
        return $this;
    }

    public function removeChatMessage(ChatMessageevent $chatMessage): static {
        if ($this->chatMessages->removeElement($chatMessage) && $chatMessage->getEvenement() === $this) {
            $chatMessage->setEvenement(null);
        }
        return $this;
    }
}