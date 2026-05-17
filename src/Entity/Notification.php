<?php
// src/Entity/Notification.php

namespace App\Entity;

use App\Repository\NotificationRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: NotificationRepository::class)]
#[ORM\Table(name: 'notification')]
#[ORM\HasLifecycleCallbacks]
class Notification
{
    // Types de notifications disponibles
    const TYPE_CREATION    = 'creation';
    const TYPE_MODIFICATION = 'modification';
    const TYPE_SUPPRESSION  = 'suppression';
    const TYPE_RAPPEL       = 'rappel';

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id', type: Types::INTEGER)]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    /**
     * Type : 'creation' | 'modification' | 'suppression' | 'rappel'
     */
    #[ORM\Column(name: 'type', type: Types::STRING, length: 30)]
    private string $type = self::TYPE_CREATION;

    /**
     * Message lisible affiché dans la cloche
     */
    #[ORM\Column(name: 'message', type: Types::TEXT)]
    private string $message = '';

    /**
     * Lien optionnel vers la ressource concernée
     */
    #[ORM\Column(name: 'lien', type: Types::STRING, length: 255, nullable: true)]
    private ?string $lien = null;

    /**
     * Est-elle déjà lue ?
     */
    #[ORM\Column(name: 'est_lue', type: Types::BOOLEAN, options: ['default' => false])]
    private bool $estLue = false;

    /**
     * Lien avec la réservation concernée (id_reservation)
     */
    #[ORM\Column(name: 'id_reservation', type: Types::INTEGER, nullable: true)]
    private ?int $idReservation = null;

    /**
     * Date de création de la notification
     */
    #[ORM\Column(name: 'cree_le', type: Types::DATETIME_MUTABLE)]
    private \DateTimeInterface $creeLe;

    /**
     * notif_rappel_envoye : 0 = non envoyé, 1 = envoyé (utilisé pour les rappels J-1)
     */
    #[ORM\Column(name: 'notif_rappel_envoye', type: Types::SMALLINT, options: ['default' => 0])]
    private int $notifRappelEnvoye = 0;

    public function __construct()
    {
        $this->creeLe = new \DateTime();
    }

    #[ORM\PrePersist]
    public function initCreeLe(): void
    {
        $this->creeLe = new \DateTime();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────

    public function getId(): ?int { return $this->id; }

    public function getType(): string { return $this->type; }
    public function setType(string $type): static { $this->type = $type; return $this; }

    public function getMessage(): string { return $this->message; }
    public function setMessage(string $message): static { $this->message = $message; return $this; }

    public function getLien(): ?string { return $this->lien; }
    public function setLien(?string $lien): static { $this->lien = $lien; return $this; }

    public function isEstLue(): bool { return $this->estLue; }
    public function setEstLue(bool $estLue): static { $this->estLue = $estLue; return $this; }

    public function getIdReservation(): ?int { return $this->idReservation; }
    public function setIdReservation(?int $idReservation): static { $this->idReservation = $idReservation; return $this; }

    public function getCreeLe(): \DateTimeInterface { return $this->creeLe; }
    protected function setCreeLe(\DateTimeInterface $creeLe): static { $this->creeLe = $creeLe; return $this; }

    public function getNotifRappelEnvoye(): int { return $this->notifRappelEnvoye; }
    public function setNotifRappelEnvoye(int $v): static { $this->notifRappelEnvoye = $v; return $this; }

    /**
     * Retourne le libellé humain du type
     */
    public function getTypeLabel(): string
    {
        return match($this->type) {
            self::TYPE_CREATION    => 'Nouvelle réservation',
            self::TYPE_MODIFICATION => 'Réservation modifiée',
            self::TYPE_SUPPRESSION  => 'Réservation supprimée',
            self::TYPE_RAPPEL       => 'Rappel de réservation',
            default                 => 'Notification',
        };
    }

    /**
     * Retourne une couleur CSS associée au type
     */
    public function getTypeColor(): string
    {
        return match($this->type) {
            self::TYPE_CREATION    => '#52B788',   // vert
            self::TYPE_MODIFICATION => '#F59E0B',  // orange
            self::TYPE_SUPPRESSION  => '#EF4444',  // rouge
            self::TYPE_RAPPEL       => '#7B5EA7',  // violet
            default                 => '#9CA3AF',
        };
    }

    /**
     * Retourne le nom de l'icône (clé utilisée dans le twig)
     */
    public function getTypeIcon(): string
    {
        return match($this->type) {
            self::TYPE_CREATION    => 'plus',
            self::TYPE_MODIFICATION => 'edit',
            self::TYPE_SUPPRESSION  => 'trash',
            self::TYPE_RAPPEL       => 'bell',
            default                 => 'info',
        };
    }

    /**
     * Retourne une description relative du temps écoulé
     */
    public function getTempsRelatif(): string
    {
        $now  = new \DateTime();
        $diff = $now->getTimestamp() - $this->creeLe->getTimestamp();

        if ($diff < 60)        return 'À l\'instant';
        if ($diff < 3600)      return 'Il y a ' . floor($diff / 60) . ' min';
        if ($diff < 86400)     return 'Il y a ' . floor($diff / 3600) . ' h';
        if ($diff < 2592000)   return 'Il y a ' . floor($diff / 86400) . ' j';
        return $this->creeLe->format('d/m/Y');
    }
}