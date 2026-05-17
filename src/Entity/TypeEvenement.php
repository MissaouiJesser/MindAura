<?php

namespace App\Entity;

use App\Repository\TypeEvenementRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\ORM\Mapping as ORM;
use Symfony\Component\Validator\Constraints as Assert;

#[ORM\Entity(repositoryClass: TypeEvenementRepository::class)]
class TypeEvenement
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    #[ORM\Column(length: 100)]
    #[Assert\NotBlank(message: "Le libellé est obligatoire.")]
    #[Assert\Length(min: 3, minMessage: "Le libellé doit contenir au moins {{ limit }} caractères.")]
    private string $libelle = '';

    #[ORM\Column(length: 50)]
    #[Assert\NotBlank(message: "La modalité est obligatoire.")]
    #[Assert\Choice(choices: ['en ligne', 'presentiel'], message: "Modalité invalide. Choisissez 'en ligne' ou 'presentiel'.")]
    private string $modalite = '';

    #[ORM\Column(length: 50)]
    #[Assert\NotBlank(message: "La catégorie est obligatoire.")]
    #[Assert\Choice(choices: ['atelier', 'webinaire', 'conference'], message: "Catégorie invalide. Choisissez parmi : atelier, webinaire, conference.")]
    private string $categorie = '';

    #[ORM\Column]
    private bool $estGratuit = true;

    /**
     * FIX: added cascade: ['remove'] so that when a TypeEvenement is deleted via ORM,
     * its Evenement children are also removed. The matching onDelete: 'CASCADE' must be
     * set on the JoinColumn in Evenement.php (ManyToOne side) to align the DB-level FK.
     *
     * @var Collection<int, Evenement>
     */
    #[ORM\OneToMany(targetEntity: Evenement::class, mappedBy: 'typeEvenement', cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $evenements;

    public function __construct()
    {
        $this->evenements = new ArrayCollection();
    }

    public function getId(): ?int { return $this->id; }
    public function getLibelle(): string { return $this->libelle; }
    public function setLibelle(string $libelle): static { $this->libelle = $libelle; return $this; }
    public function getModalite(): string { return $this->modalite; }
    public function setModalite(string $modalite): static { $this->modalite = $modalite; return $this; }
    public function getCategorie(): string { return $this->categorie; }
    public function setCategorie(string $categorie): static { $this->categorie = $categorie; return $this; }
    public function isEstGratuit(): bool { return $this->estGratuit; }
    public function setEstGratuit(bool $estGratuit): static { $this->estGratuit = $estGratuit; return $this; }

    /**
     * @return Collection<int, Evenement>
     */
    public function getEvenements(): Collection { return $this->evenements; }

    public function addEvenement(Evenement $evenement): static
    {
        if (!$this->evenements->contains($evenement)) {
            $this->evenements->add($evenement);
            $evenement->setTypeEvenement($this);
        }
        return $this;
    }

    public function removeEvenement(Evenement $evenement): static
    {
        if ($this->evenements->removeElement($evenement)) {
            if ($evenement->getTypeEvenement() === $this) {
                $evenement->setTypeEvenement(null);
            }
        }
        return $this;
    }

    public function __toString(): string { return $this->libelle; }
}