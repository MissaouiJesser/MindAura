<?php

namespace App\Entity;

use Doctrine\ORM\Mapping as ORM;

use Doctrine\Common\Collections\Collection;
use Doctrine\Common\Collections\ArrayCollection;
use App\Entity\Reclamation;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity]
#[ORM\Table(name: 'categorie')]
class Categorie
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_categorie', type: 'integer')]
    private int $id_categorie = 0;

    #[ORM\Column(name: 'nom_categorie', type: 'string', length: 100)]
    #[Assert\NotBlank(message: 'Le nom de la catégorie ne peut pas être vide')]
    #[Assert\Length(
        min: 2,
        max: 100,
        minMessage: 'Le nom doit contenir au moins {{ limit }} caractères',
        maxMessage: 'Le nom ne doit pas dépasser {{ limit }} caractères'
    )]
    private string $nom_categorie = '';

    #[ORM\Column(name: 'description', type: 'string', length: 255)]
    #[Assert\NotBlank(message: 'La description ne peut pas être vide')]
    #[Assert\Length(
        min: 5,
        max: 255,
        minMessage: 'La description doit contenir au moins {{ limit }} caractères',
        maxMessage: 'La description ne doit pas dépasser {{ limit }} caractères'
    )]
    private string $description = '';

    #[ORM\Column(name: 'date_creation', type: 'datetime')]
    #[Assert\NotNull(message: 'La date de création est obligatoire')]
    private \DateTimeInterface $date_creation;

    /** @var Collection<int, Reclamation> */
    #[ORM\OneToMany(mappedBy: 'categorie', targetEntity: Reclamation::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $reclamations;

    public function __construct()
    {
        $this->reclamations = new ArrayCollection();
        $this->date_creation = new \DateTime();
    }

    /** @return Collection<int, Reclamation> */
    public function getReclamations(): Collection
    {
        return $this->reclamations;
    }

    public function addReclamation(Reclamation $reclamation): self
    {
        if (!$this->reclamations->contains($reclamation)) {
            $this->reclamations[] = $reclamation;
            $reclamation->setCategorie($this);
        }
        return $this;
    }

    public function removeReclamation(Reclamation $reclamation): self
    {
        if ($this->reclamations->removeElement($reclamation)) {
            if ($reclamation->getCategorie() === $this) {
                $reclamation->setCategorie(null);
            }
        }
        return $this;
    }

    public function getId_categorie(): int { return $this->id_categorie; }
    public function getIdCategorie(): int { return $this->id_categorie; }

    public function getNom_categorie(): string { return $this->nom_categorie; }
    public function setNom_categorie(string $value): self { $this->nom_categorie = $value; return $this; }
    public function getNomCategorie(): string { return $this->nom_categorie; }
    public function setNomCategorie(string $value): self { $this->nom_categorie = $value; return $this; }

    public function getDescription(): string { return $this->description; }
    public function setDescription(string $value): self { $this->description = $value; return $this; }

    public function getDate_creation(): \DateTimeInterface { return $this->date_creation; }
    protected function setDate_creation(\DateTimeInterface $value): self { $this->date_creation = $value; return $this; }
    public function getDateCreation(): \DateTimeInterface { return $this->date_creation; }
    protected function setDateCreation(\DateTimeInterface $value): self { $this->date_creation = $value; return $this; }
}