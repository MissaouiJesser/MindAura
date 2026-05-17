<?php
// src/Entity/ChatMessageevent.php

namespace App\Entity;

use App\Repository\ChatMessageeventRepository;
use Doctrine\DBAL\Types\Types;
use Doctrine\ORM\Mapping as ORM;

#[ORM\Entity(repositoryClass: ChatMessageeventRepository::class)]
#[ORM\HasLifecycleCallbacks]
class ChatMessageevent
{
    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column]
    /** @phpstan-ignore-next-line */
    private ?int $id = null;

    #[ORM\Column(length: 150)]
    private string $senderName = '';

    #[ORM\Column(length: 255)]
    private string $senderEmail = '';

    #[ORM\Column(type: Types::TEXT)]
    private string $content = '';

    #[ORM\Column(type: Types::DATETIME_MUTABLE)]
    private \DateTimeInterface $sentAt;

    #[ORM\ManyToOne(targetEntity: Evenement::class, inversedBy: 'chatMessages')]
    #[ORM\JoinColumn(nullable: false, onDelete: 'CASCADE')]
    private ?Evenement $evenement = null;

    public function __construct()
    {
        $this->sentAt = new \DateTime();
    }

    #[ORM\PrePersist]
    public function onPrePersist(): void
    {
        $this->sentAt = new \DateTime();
    }

    public function getId(): ?int { return $this->id; }
    public function getSenderName(): string { return $this->senderName; }
    public function setSenderName(string $v): static { $this->senderName = $v; return $this; }
    public function getSenderEmail(): string { return $this->senderEmail; }
    public function setSenderEmail(string $v): static { $this->senderEmail = $v; return $this; }
    public function getContent(): string { return $this->content; }
    public function setContent(string $v): static { $this->content = $v; return $this; }
    public function getSentAt(): \DateTimeInterface { return $this->sentAt; }
    protected function setSentAt(\DateTimeInterface $v): static { $this->sentAt = $v; return $this; }
    public function getEvenement(): ?Evenement { return $this->evenement; }
    public function setEvenement(?Evenement $v): static { $this->evenement = $v; return $this; }

    public function getInitials(): string
    {
        $parts = explode(' ', trim($this->senderName));
        $initials = '';
        foreach (array_slice($parts, 0, 2) as $p) {
            $initials .= mb_strtoupper(mb_substr($p, 0, 1));
        }
        return $initials ?: '?';
    }

    public function getAvatarColor(): string
    {
        $colors = ['#c0392b','#2980b9','#27ae60','#8e44ad','#e67e22','#1abc9c','#e91e63','#ff5722'];
        $idx = abs(crc32($this->senderEmail)) % count($colors);
        return $colors[$idx];
    }
}