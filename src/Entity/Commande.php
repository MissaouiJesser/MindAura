<?php

namespace App\Entity;

use App\Repository\CommandeRepository;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: CommandeRepository::class)]
class Commande
{
    const STATUT_EN_ATTENTE = 'en_attente';
    const STATUT_PAYE       = 'paye';
    const STATUT_ANNULE     = 'annule';

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    #[ORM\Column(length: 255)]
    private string $userEmail = '';

    #[ORM\Column(length: 30)]
    private string $statutPaiement = self::STATUT_EN_ATTENTE;

    // nullable: true en DB → on garde ?string pour correspondre au mapping
    #[ORM\Column(type: Types::DECIMAL, precision: 10, scale: 2, nullable: true)]
    private ?string $montantTotal = '0.00';

    #[ORM\Column(length: 255, nullable: true)]
    private ?string $stripeSession = null;

    #[ORM\Column(type: Types::DATETIME_MUTABLE)]
    private \DateTimeInterface $dateCommande;

    /**
     * @var Collection<int, CommandeItem>
     */
    #[ORM\OneToMany(mappedBy: 'commande', targetEntity: CommandeItem::class, cascade: ['persist', 'remove'], orphanRemoval: true)]
    private Collection $items;

    public function __construct()
    {
        $this->dateCommande   = new \DateTime();
        $this->statutPaiement = self::STATUT_EN_ATTENTE;
        $this->items          = new ArrayCollection();
    }

    public function getId(): ?int { return $this->id; }

    public function getUserEmail(): string { return $this->userEmail; }
    public function setUserEmail(string $userEmail): static { $this->userEmail = $userEmail; return $this; }

    public function getStatutPaiement(): string { return $this->statutPaiement; }
    public function setStatutPaiement(string $s): static { $this->statutPaiement = $s; return $this; }

    public function getMontantTotal(): ?string { return $this->montantTotal; }
    public function setMontantTotal(?string $m): static { $this->montantTotal = $m; return $this; }

    public function getStripeSession(): ?string { return $this->stripeSession; }
    public function setStripeSession(?string $s): static { $this->stripeSession = $s; return $this; }

    public function getDateCommande(): \DateTimeInterface { return $this->dateCommande; }

    // Protected: timestamp auto-géré dans le constructeur
    protected function setDateCommande(\DateTimeInterface $d): static { $this->dateCommande = $d; return $this; }

    /**
     * @return Collection<int, CommandeItem>
     */
    public function getItems(): Collection { return $this->items; }

    public function addItem(CommandeItem $item): static
    {
        if (!$this->items->contains($item)) {
            $this->items->add($item);
            $item->setCommande($this);
        }
        return $this;
    }

    public function removeItem(CommandeItem $item): static
    {
        if ($this->items->removeElement($item)) {
            if ($item->getCommande() === $this) {
                $item->setCommande(null);
            }
        }
        return $this;
    }

    public function recalculerTotal(): void
    {
        $total = 0;
        foreach ($this->items as $item) {
            $total += (float)$item->getPrixUnitaire() * $item->getQuantite();
        }
        $this->montantTotal = number_format($total, 2, '.', '');
    }
}