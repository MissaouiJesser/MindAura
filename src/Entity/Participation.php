<?php

namespace App\Entity;

use App\Repository\ParticipationRepository;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ParticipationRepository::class)]
class Participation
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    #[ORM\Column(length: 100)]
    private string $nom = '';

    #[ORM\Column(length: 100)]
    private string $prenom = '';

    #[ORM\Embedded(class: Email::class, columnPrefix: false)]
    private Email $email;

    #[ORM\Column(length: 20, nullable: true)]
    private ?string $telephone = null;

    #[ORM\Column(type: 'datetime_immutable')]
    private \DateTimeImmutable $dateInscription;

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $codeQr = null;

    #[ORM\Column(length: 30)]
    private string $statut = '';

    #[ORM\Column(nullable: true)]
    private ?int $positionAttente = null;

    #[ORM\ManyToOne(inversedBy: 'participations')]
    #[ORM\JoinColumn(nullable: false, onDelete: 'CASCADE')]
    private ?Evenement $evenement = null;

    public function __construct()
    {
        $this->email = $this->createEmptyEmail();
        $this->dateInscription = new \DateTimeImmutable();
    }

    private function createEmptyEmail(): Email
    {
        $reflection = new \ReflectionClass(Email::class);
        /** @var Email $email */
        $email = $reflection->newInstanceWithoutConstructor();
        $prop  = $reflection->getProperty('value');
        $prop->setAccessible(true);
        $prop->setValue($email, '');
        return $email;
    }

    public function getId(): ?int { return $this->id; }

    public function getNom(): string { return $this->nom; }
    public function setNom(string $nom): static { $this->nom = $nom; return $this; }

    public function getPrenom(): string { return $this->prenom; }
    public function setPrenom(string $prenom): static { $this->prenom = $prenom; return $this; }

    public function getEmail(): Email { return $this->email; }
    public function setEmail(Email $email): static { $this->email = $email; return $this; }

    public function getTelephone(): ?string { return $this->telephone; }
    public function setTelephone(?string $telephone): static { $this->telephone = $telephone; return $this; }

    public function getDateInscription(): \DateTimeImmutable { return $this->dateInscription; }
    protected function setDateInscription(\DateTimeImmutable $d): static { $this->dateInscription = $d; return $this; }

    public function getCodeQr(): ?string { return $this->codeQr; }
    public function setCodeQr(?string $codeQr): static { $this->codeQr = $codeQr; return $this; }

    public function getStatut(): string { return $this->statut; }
    public function setStatut(string $statut): static { $this->statut = $statut; return $this; }

    public function getPositionAttente(): ?int { return $this->positionAttente; }
    public function setPositionAttente(?int $p): static { $this->positionAttente = $p; return $this; }

    public function isEnAttente(): bool { return $this->statut === 'en_attente'; }

    public function getEvenement(): ?Evenement { return $this->evenement; }
    public function setEvenement(?Evenement $evenement): static { $this->evenement = $evenement; return $this; }
}