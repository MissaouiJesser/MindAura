<?php

namespace App\Entity;

use App\Repository\MessageRepository;
use Doctrine\ORM\Mapping as ORM;

/**
 * Représente un message de chat entre deux utilisateurs.
 *
 * Colonnes :
 *   id          — clé primaire auto-incrémentée
 *   sender_id   — FK vers utilisateurs (expéditeur)
 *   receiver_id — FK vers utilisateurs (destinataire)
 *   content     — contenu du message
 *   is_read     — lu ou non
 *   created_at  — horodatage d'envoi
 */
#[ORM\Entity(repositoryClass: MessageRepository::class)]
#[ORM\Table(name: 'message')]
#[ORM\Index(columns: ['sender_id'],   name: 'idx_msg_sender')]
#[ORM\Index(columns: ['receiver_id'], name: 'idx_msg_receiver')]
#[ORM\Index(columns: ['created_at'],  name: 'idx_msg_date')]
#[ORM\HasLifecycleCallbacks]
class Message
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    private ?int $id = null; // @phpstan-ignore-line

    #[ORM\ManyToOne(targetEntity: Utilisateurs::class)]
    #[ORM\JoinColumn(name: 'sender_id', referencedColumnName: 'id_utilisateur', nullable: false, onDelete: 'CASCADE')]
    private Utilisateurs $sender;

    #[ORM\ManyToOne(targetEntity: Utilisateurs::class)]
    #[ORM\JoinColumn(name: 'receiver_id', referencedColumnName: 'id_utilisateur', nullable: false, onDelete: 'CASCADE')]
    private Utilisateurs $receiver;

    #[ORM\Column(name: 'content', type: 'text')]
    private string $content;

    #[ORM\Column(name: 'is_read', type: 'boolean', options: ['default' => false])]
    private bool $isRead = false;

    /** @var array<string, mixed>|null */
    #[ORM\Column(name: 'reactions', type: 'json', nullable: true)]
    private ?array $reactions = null;

    #[ORM\Column(name: 'created_at', type: 'datetime')]
    private \DateTimeInterface $createdAt;

    public function __construct(Utilisateurs $sender, Utilisateurs $receiver, string $content)
    {
        $this->sender    = $sender;
        $this->receiver  = $receiver;
        $this->content   = $content;
        $this->createdAt = new \DateTime();
    }

    #[ORM\PrePersist]
    public function initCreatedAt(): void
    {
        $this->createdAt = new \DateTime();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public function getId(): ?int { return $this->id; }

    public function getSender(): Utilisateurs { return $this->sender; }
    public function setSender(Utilisateurs $v): static { $this->sender = $v; return $this; }

    public function getReceiver(): Utilisateurs { return $this->receiver; }
    public function setReceiver(Utilisateurs $v): static { $this->receiver = $v; return $this; }

    public function getContent(): string { return $this->content; }
    public function setContent(string $v): static { $this->content = $v; return $this; }

    public function isRead(): bool { return $this->isRead; }
    public function setIsRead(bool $v): static { $this->isRead = $v; return $this; }

    public function getCreatedAt(): \DateTimeInterface { return $this->createdAt; }

    /** @return array<string, mixed>|null */
    public function getReactions(): ?array { return $this->reactions; }
    /** @param array<string, mixed>|null $v */
    public function setReactions(?array $v): static { $this->reactions = $v; return $this; }
}