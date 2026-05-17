<?php

namespace App\Entity;

use App\Repository\SalleRepository;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: SalleRepository::class)]
#[ORM\Table(name: 'salle')]
class Salle
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_salle', type: 'integer')]
    /** @phpstan-ignore property.unusedType */
    private ?int $idSalle = null;

    // ── Relation Local ────────────────────────────────────────────
    // ✅ onDelete="CASCADE" kept on the JoinColumn so the DB constraint is consistent.
    //    ORM-level cascade must be declared on the *owning* OneToMany in LocalsPsychiatrie:
    //      cascade: ['persist', 'remove'], orphanRemoval: true
    //    That aligns ORM removes ($em->remove($local)) with DB-level cascade deletes
    //    so both paths produce identical behaviour with no FK violations.
    #[ORM\ManyToOne(targetEntity: LocalsPsychiatrie::class, inversedBy: 'salles')]
    #[ORM\JoinColumn(name: 'local_id', referencedColumnName: 'id_local', nullable: false, onDelete: "CASCADE")]
    #[Assert\NotNull(message: 'Veuillez associer un local psychiatrique à cette salle.')]
    private ?LocalsPsychiatrie $local = null;

    // ── Nom ───────────────────────────────────────────────────────
    #[ORM\Column(name: 'nom_salle', type: 'string', length: 100, nullable: false)]
    #[Assert\NotBlank(message: 'Le nom de la salle est obligatoire.')]
    #[Assert\Length(
        min: 2,
        max: 100,
        minMessage: 'Le nom doit contenir au moins {{ limit }} caractères.',
        maxMessage: 'Le nom ne peut pas dépasser {{ limit }} caractères.'
    )]
    #[Assert\Regex(
        pattern: '/^[\p{L}0-9\s\-\'\.]+$/u',
        message: 'Le nom ne peut contenir que des lettres, chiffres, espaces ou tirets.'
    )]
    private string $nomSalle = '';

    // ── Type ──────────────────────────────────────────────────────
    #[ORM\Column(name: 'type_salle', type: 'string', length: 100, nullable: true)]
    #[Assert\NotBlank(message: 'Le type de salle est obligatoire.')]
    #[Assert\Choice(
        choices: [
            'Salle de thérapie',
            'Salle de consultation',
            'Salle de groupe',
            'Salle de relaxation',
            "Salle d'attente",
            'Autre',
        ],
        message: 'Veuillez choisir un type valide.'
    )]
    private ?string $typeSalle = null;

    // ── Capacité ──────────────────────────────────────────────────
    #[ORM\Column(name: 'capacite_salle', type: 'string', length: 50, nullable: true)]
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
        value: 999,
        message: "La capacité d'une salle ne peut pas dépasser 999 personnes."
    )]
    private ?string $capaciteSalle = null;

    // ── Équipements ───────────────────────────────────────────────
    // ✅ CORRECTION : ajout de @Assert\NotBlank → le champ devient obligatoire.
    //    Le champ reste nullable en BDD pour la rétrocompatibilité, mais la saisie
    //    est désormais exigée. Ajout d'une contrainte Regex pour rejeter les saisies
    //    contenant uniquement des caractères spéciaux non significatifs.
    #[ORM\Column(name: 'equipements', type: 'text', nullable: true)]
    #[Assert\NotBlank(message: 'Veuillez renseigner les équipements de la salle.')]
    #[Assert\Length(
        min: 3,
        max: 2000,
        minMessage: 'Les équipements doivent contenir au moins {{ limit }} caractères.',
        maxMessage: 'Les équipements ne peuvent pas dépasser {{ limit }} caractères.'
    )]
    #[Assert\Regex(
        pattern: '/^[\p{L}0-9\s\-\'\.\,\!\?\;\:\(\)\n\r\/]+$/u',
        message: 'Les équipements contiennent des caractères non autorisés.'
    )]
    private ?string $equipements = null;

    // ── Disponibilité ─────────────────────────────────────────────
    // ✅ CORRECTION : contraintes complètes — NotBlank + Choice déjà présentes,
    //    on s'assure qu'elles sont bien déclarées ici ET dans SalleType (mapped=true).
    #[ORM\Column(name: 'disponibilite_salle', type: 'string', length: 100, nullable: true)]
    #[Assert\NotBlank(message: 'La disponibilité est obligatoire.')]
    #[Assert\Choice(
        choices: ['Disponible', 'Indisponible', 'En maintenance'],
        message: 'Veuillez choisir une valeur valide.'
    )]
    private ?string $disponibiliteSalle = null;

    // ── Étage ─────────────────────────────────────────────────────
    #[ORM\Column(name: 'etage', type: 'string', length: 50, nullable: true)]
    #[Assert\NotBlank(message: "L'étage est obligatoire.")]
    #[Assert\Choice(
        choices: ['RDC', '1er étage', '2ème étage', '3ème étage', '4ème étage'],
        message: "Veuillez choisir un étage valide."
    )]
    private ?string $etage = null;

    // ── Image (BDD) ───────────────────────────────────────────────
    #[ORM\Column(name: 'image_url', type: 'string', length: 255, nullable: true)]
    private ?string $imageUrl = null;

    // ── Image (champ virtuel upload) ──────────────────────────────
    // ℹ️ Ce champ est mapped=false dans SalleType, donc les contraintes @Assert\File
    //    ci-dessous ne sont PAS déclenchées lors de la validation du formulaire.
    //    Les contraintes actives sont celles déclarées dans SalleType (->add('imageFile', ...)).
    //    Ces annotations restent ici à titre documentaire uniquement.
    #[Assert\File(
        maxSize: '2M',
        mimeTypes: ['image/jpeg', 'image/png', 'image/webp', 'image/gif'],
        maxSizeMessage: "L'image ne doit pas dépasser 2 Mo.",
        mimeTypesMessage: "Formats acceptés : JPG, PNG, WEBP, GIF."
    )]
    private ?\Symfony\Component\HttpFoundation\File\UploadedFile $imageFile = null;

    // ── Statut ────────────────────────────────────────────────────
    // ✅ CORRECTION : NotBlank + Choice déjà présentes sur l'entité.
    //    On s'assure que ces contraintes sont également déclarées dans SalleType
    //    (cf. fichier SalleType.php corrigé) pour garantir la validation côté form.
    #[ORM\Column(name: 'statut_salle', type: 'string', length: 100, nullable: true)]
    #[Assert\NotBlank(message: 'Le statut est obligatoire.')]
    #[Assert\Choice(
        choices: ['Active', 'Inactive', 'En rénovation'],
        message: 'Veuillez choisir un statut valide.'
    )]
    private ?string $statutSalle = null;

    // ===================== GETTERS & SETTERS =====================

    public function getIdSalle(): ?int { return $this->idSalle; }

    public function getLocal(): ?LocalsPsychiatrie { return $this->local; }
    public function setLocal(?LocalsPsychiatrie $local): static { $this->local = $local; return $this; }

    public function getNomSalle(): string { return $this->nomSalle; }
    public function setNomSalle(string $nomSalle): static
    {
        $this->nomSalle = trim($nomSalle);
        return $this;
    }

    public function getTypeSalle(): ?string { return $this->typeSalle; }
    public function setTypeSalle(?string $typeSalle): static { $this->typeSalle = $typeSalle; return $this; }

    public function getCapaciteSalle(): ?string { return $this->capaciteSalle; }
    public function setCapaciteSalle(?string $capaciteSalle): static
    {
        $this->capaciteSalle = $capaciteSalle;
        return $this;
    }

    public function getEquipements(): ?string { return $this->equipements; }
    public function setEquipements(?string $equipements): static
    {
        $this->equipements = $equipements !== null ? trim($equipements) : null;
        return $this;
    }

    public function getDisponibiliteSalle(): ?string { return $this->disponibiliteSalle; }
    public function setDisponibiliteSalle(?string $disponibiliteSalle): static { $this->disponibiliteSalle = $disponibiliteSalle; return $this; }

    public function getEtage(): ?string { return $this->etage; }
    public function setEtage(?string $etage): static { $this->etage = $etage; return $this; }

    public function getImageURL(): ?string { return $this->imageUrl; }
    public function setImageURL(?string $imageURL): static { $this->imageUrl = $imageURL; return $this; }

    public function getImageFile(): ?\Symfony\Component\HttpFoundation\File\UploadedFile { return $this->imageFile; }
    public function setImageFile(?\Symfony\Component\HttpFoundation\File\UploadedFile $imageFile): static { $this->imageFile = $imageFile; return $this; }

    public function getStatutSalle(): ?string { return $this->statutSalle; }
    public function setStatutSalle(?string $statutSalle): static { $this->statutSalle = $statutSalle; return $this; }
}