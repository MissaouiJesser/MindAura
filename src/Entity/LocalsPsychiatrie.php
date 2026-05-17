<?php

namespace App\Entity;

use App\Repository\LocalsPsychiatrieRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: LocalsPsychiatrieRepository::class)]
#[ORM\Table(name: 'local_psychiatrie')]
class LocalsPsychiatrie
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_local', type: 'integer')]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    // ── Nom ──────────────────────────────────────────────────────
    #[ORM\Column(name: 'nom_local', type: 'string', length: 100, nullable: false)]
    #[Assert\NotBlank(message: 'Le nom du local est obligatoire.')]
    #[Assert\Length(
        min: 3,
        max: 100,
        minMessage: 'Le nom doit contenir au moins {{ limit }} caractères.',
        maxMessage: 'Le nom ne peut pas dépasser {{ limit }} caractères.'
    )]
    #[Assert\Regex(
        pattern: '/^[\p{L}0-9\s\-\'\.]+$/u',
        message: 'Le nom ne peut contenir que des lettres, chiffres, espaces ou tirets.'
    )]
    private string $nomLocal = '';

    // ── Adresse ──────────────────────────────────────────────────
    #[ORM\Column(name: 'adresse_local', type: 'string', length: 150, nullable: false)]
    #[Assert\NotBlank(message: "L'adresse est obligatoire.")]
    #[Assert\Length(
        min: 5,
        max: 150,
        minMessage: "L'adresse doit contenir au moins {{ limit }} caractères.",
        maxMessage: "L'adresse ne peut pas dépasser {{ limit }} caractères."
    )]
    private string $adresseLocal = '';

    // ── Ville ─────────────────────────────────────────────────────
    #[ORM\Column(name: 'ville_local', type: 'string', length: 100, nullable: false)]
    #[Assert\NotBlank(message: 'La ville est obligatoire.')]
    #[Assert\Length(
        min: 2,
        max: 100,
        minMessage: 'La ville doit contenir au moins {{ limit }} caractères.',
        maxMessage: 'La ville ne peut pas dépasser {{ limit }} caractères.'
    )]
    #[Assert\Regex(
        pattern: '/^[\p{L}\s\-]+$/u',
        message: 'La ville ne peut contenir que des lettres et des tirets.'
    )]
    private string $villeLocal = '';

    // ── Description ───────────────────────────────────────────────
    // ✅ CORRECTION : ajout de @Assert\NotBlank + @Assert\Regex pour interdire
    //    les descriptions vides ou composées uniquement d'espaces/caractères spéciaux.
    #[ORM\Column(name: 'description_local', type: 'text', nullable: true)]
    #[Assert\NotBlank(message: 'La description du local est obligatoire.')]
    #[Assert\Length(
        min: 10,
        max: 2000,
        minMessage: 'La description doit contenir au moins {{ limit }} caractères.',
        maxMessage: 'La description ne peut pas dépasser {{ limit }} caractères.'
    )]
    #[Assert\Regex(
        pattern: '/^[\p{L}0-9\s\-\'\.\,\!\?\;\:\(\)\n\r]+$/u',
        message: 'La description contient des caractères non autorisés.'
    )]
    private ?string $descriptionLocal = null;

    // ── Capacité ──────────────────────────────────────────────────
    #[ORM\Column(name: 'capacite_local', type: 'string', length: 50, nullable: true)]
    #[Assert\NotBlank(message: 'La capacité est obligatoire.')]
    #[Assert\Regex(
        pattern: '/^\d+$/',
        message: 'La capacité doit être un nombre entier positif.'
    )]
    #[Assert\GreaterThan(
        value: 0,
        message: 'La capacité doit être supérieure à 0.'
    )]
    #[Assert\LessThanOrEqual(
        value: 9999,
        message: 'La capacité ne peut pas dépasser 9999 personnes.'
    )]
    private ?string $capaciteLocal = null;

    // ── Type ──────────────────────────────────────────────────────
    #[ORM\Column(name: 'type_local', type: 'string', length: 50, nullable: true)]
    #[Assert\NotBlank(message: 'Le type de local est obligatoire.')]
    #[Assert\Choice(
        choices: ['Cabinet', 'Clinique', 'Hôpital', 'Centre de bien-être', 'Salle de thérapie', 'Autre'],
        message: 'Veuillez choisir un type valide.'
    )]
    private ?string $typeLocal = null;

    // ── Téléphone ─────────────────────────────────────────────────
    #[ORM\Column(name: 'telephone_local', type: 'integer', nullable: true)]
    #[Assert\NotBlank(message: 'Le numéro de téléphone est obligatoire.')]
    #[Assert\Positive(message: 'Le numéro de téléphone doit être un nombre positif.')]
    #[Assert\Range(
        min: 10000000,
        max: 99999999,
        notInRangeMessage: 'Le numéro de téléphone doit contenir 8 chiffres (ex: 20123456).'
    )]
    private ?int $telephoneLocal = null;

    // ── Email ─────────────────────────────────────────────────────
    #[ORM\Column(name: 'email_local', type: 'string', length: 150, nullable: true)]
    #[Assert\NotBlank(message: "L'email est obligatoire.")]
    #[Assert\Email(message: "L'adresse email '{{ value }}' n'est pas valide.")]
    #[Assert\Length(
        max: 150,
        maxMessage: "L'email ne peut pas dépasser {{ limit }} caractères."
    )]
    private ?string $emailLocal = null;

    // ── Disponibilité ─────────────────────────────────────────────
    #[ORM\Column(name: 'disponibilite_local', type: 'string', length: 100, nullable: true)]
    #[Assert\NotBlank(message: 'La disponibilité est obligatoire.')]
    #[Assert\Choice(
        choices: ['Disponible', 'Indisponible'],
        message: 'La valeur doit être Disponible ou Indisponible.'
    )]
    private ?string $disponibiliteLocal = null;

    // ── Image (BDD) ───────────────────────────────────────────────
    #[ORM\Column(name: 'image_url', type: 'string', length: 255, nullable: true)]
    private ?string $imageUrl = null;

    // ── Image (champ virtuel upload) ──────────────────────────────
    // ℹ️ Ce champ est mapped=false dans le formulaire, donc les contraintes
    //    @Assert\File ci-dessous ne sont PAS déclenchées par le validator Symfony
    //    lors de la soumission du form. Les contraintes actives sont celles
    //    déclarées directement dans LocalsPsychiatrieType (->add('imageFile', ...)).
    //    Ces annotations restent ici à titre documentaire uniquement.
    #[Assert\File(
        maxSize: '2M',
        mimeTypes: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
        maxSizeMessage: "L'image ne doit pas dépasser 2 Mo.",
        mimeTypesMessage: "Formats acceptés : JPG, PNG, WEBP, GIF."
    )]
    private ?\Symfony\Component\HttpFoundation\File\UploadedFile $imageFile = null;

    // ── Collection des salles liées ───────────────────────────────
    /** @var Collection<int, Salle> */
    #[ORM\OneToMany(mappedBy: 'local', targetEntity: Salle::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $salles;

    public function __construct()
    {
        $this->salles = new ArrayCollection();
    }

    // ===================== GETTERS & SETTERS =====================

    public function getIdLocal(): ?int { return $this->id; }

    public function getNomLocal(): string { return $this->nomLocal; }
    public function setNomLocal(string $nomLocal): static
    {
        $this->nomLocal = trim($nomLocal);
        return $this;
    }

    public function getAdresseLocal(): string { return $this->adresseLocal; }
    public function setAdresseLocal(string $adresseLocal): static
    {
        $this->adresseLocal = trim($adresseLocal);
        return $this;
    }

    public function getVilleLocal(): string { return $this->villeLocal; }
    public function setVilleLocal(string $villeLocal): static
    {
        $this->villeLocal = trim($villeLocal);
        return $this;
    }

    public function getDescriptionLocal(): ?string { return $this->descriptionLocal; }
    public function setDescriptionLocal(?string $descriptionLocal): static
    {
        $this->descriptionLocal = $descriptionLocal !== null ? trim($descriptionLocal) : null;
        return $this;
    }

    public function getCapaciteLocal(): ?string { return $this->capaciteLocal; }
    public function setCapaciteLocal(?string $capaciteLocal): static
    {
        $this->capaciteLocal = $capaciteLocal;
        return $this;
    }

    public function getTypeLocal(): ?string { return $this->typeLocal; }
    public function setTypeLocal(?string $typeLocal): static { $this->typeLocal = $typeLocal; return $this; }

    public function getTelephoneLocal(): ?int { return $this->telephoneLocal; }
    public function setTelephoneLocal(?int $telephoneLocal): static { $this->telephoneLocal = $telephoneLocal; return $this; }

    public function getEmailLocal(): ?string { return $this->emailLocal; }
    public function setEmailLocal(?string $emailLocal): static { $this->emailLocal = $emailLocal; return $this; }

    public function getDisponibiliteLocal(): ?string { return $this->disponibiliteLocal; }
    public function setDisponibiliteLocal(?string $disponibiliteLocal): static { $this->disponibiliteLocal = $disponibiliteLocal; return $this; }

    public function getImageURL(): ?string { return $this->imageUrl; }
    public function setImageURL(?string $imageURL): static { $this->imageUrl = $imageURL; return $this; }

    public function getImageFile(): ?\Symfony\Component\HttpFoundation\File\UploadedFile { return $this->imageFile; }
    public function setImageFile(?\Symfony\Component\HttpFoundation\File\UploadedFile $imageFile): static { $this->imageFile = $imageFile; return $this; }

    // ── Salles ────────────────────────────────────────────────────
    /** @return Collection<int, Salle> */
    public function getSalles(): Collection { return $this->salles; }

    public function addSalle(Salle $salle): static
    {
        if (!$this->salles->contains($salle)) {
            $this->salles->add($salle);
            $salle->setLocal($this);
        }
        return $this;
    }

    public function removeSalle(Salle $salle): static
    {
        if ($this->salles->removeElement($salle)) {
            if ($salle->getLocal() === $this) {
                $salle->setLocal(null);
            }
        }
        return $this;
    }

    /**
     * +1 sur la capacité du local lors de l'ajout d'une salle.
     */
    public function incrementCapacite(): void
    {
        $this->capaciteLocal = (string) ((int) $this->capaciteLocal + 1);
    }

    /**
     * -1 sur la capacité du local lors de la suppression d'une salle (min 0).
     */
    public function decrementCapacite(): void
    {
        $this->capaciteLocal = (string) max(0, (int) $this->capaciteLocal - 1);
    }

    /**
     * Si le local passe à "Indisponible", toutes ses salles
     * passent automatiquement à "Indisponible".
     */
    public function propagateDisponibilite(): void
    {
        if ($this->disponibiliteLocal === 'Indisponible') {
            foreach ($this->salles as $salle) {
                $salle->setDisponibiliteSalle('Indisponible');
            }
        }
    }
}