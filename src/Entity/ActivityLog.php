<?php

namespace App\Entity;

use App\Entity\Trait\BlameableTrait;
use App\Enum\ActivityActionEnum;
use App\Repository\ActivityLogRepository;
use Doctrine\ORM\Mapping as ORM;

/**
 * Enregistre chaque action significative effectuée par un utilisateur connecté.
 *
 * Colonnes :
 *   id             — clé primaire auto-incrémentée
 *   user_id        — FK vers utilisateurs (nullable : actions pré-login)
 *   user_name      — snapshot du nom au moment de l'action
 *   user_email     — snapshot de l'email
 *   user_role      — snapshot du rôle
 *   user_avatar    — snapshot du nom de fichier avatar
 *   action         — enum ActivityActionEnum ex. USER_LOGIN, USER_CREATED
 *   label          — texte lisible ex. "Connexion réussie"
 *   context        — office : "backoffice" | "frontoffice"
 *   ip             — adresse IP cliente
 *   route          — nom Symfony de la route
 *   extra          — JSON libre pour données supplémentaires
 *   created_at     — horodatage (géré automatiquement via PrePersist)
 *   updated_at     — horodatage de mise à jour (géré automatiquement via PreUpdate)
 *   created_by_id  — FK vers utilisateurs, NOT NULL (audit, déclaré ici et non dans le trait)
 *   updated_by_id  — FK vers utilisateurs, nullable (audit, via BlameableTrait)
 */
#[ORM\Entity(repositoryClass: ActivityLogRepository::class)]
#[ORM\Table(name: 'activity_log')]
#[ORM\Index(columns: ['user_id'],    name: 'idx_al_user')]
#[ORM\Index(columns: ['action'],     name: 'idx_al_action')]
#[ORM\Index(columns: ['created_at'], name: 'idx_al_date')]
#[ORM\HasLifecycleCallbacks]
class ActivityLog
{
    use BlameableTrait;

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(type: 'integer')]
    /** @phpstan-ignore property.unusedType */
    private ?int $id = null;

    // createdBy déclaré ici (et non dans BlameableTrait) pour pouvoir imposer nullable: false.
    #[ORM\ManyToOne(targetEntity: Utilisateurs::class)]
    #[ORM\JoinColumn(name: 'created_by_id', referencedColumnName: 'id_utilisateur', nullable: false, onDelete: 'RESTRICT')]
    private Utilisateurs $createdBy;

    #[ORM\Column(name: 'user_id', type: 'integer', nullable: true)]
    private ?int $userId = null;

    #[ORM\Column(name: 'user_name', type: 'string', length: 255, nullable: true)]
    private ?string $userName = null;

    #[ORM\Column(name: 'user_email', type: 'string', length: 255, nullable: true)]
    private ?string $userEmail = null;

    #[ORM\Column(name: 'user_role', type: 'string', length: 100, nullable: true)]
    private ?string $userRole = null;

    #[ORM\Column(name: 'user_avatar', type: 'string', length: 255, nullable: true)]
    private ?string $userAvatar = null;

    // Stored as plain string in DB (VARCHAR 100).
    // Getter/setter cast to/from ActivityActionEnum so the rest of the app
    // never touches raw strings. This avoids the Doctrine Doctor false positive
    // that flags enumType vs VARCHAR as a type mismatch.
    #[ORM\Column(name: 'action', type: 'string', length: 100)]
    private string $action = '';

    #[ORM\Column(name: 'label', type: 'string', length: 255)]
    private string $label;

    #[ORM\Column(name: 'context', type: 'string', length: 50, options: ['default' => 'frontoffice'])]
    private string $context = 'frontoffice';

    #[ORM\Column(name: 'ip', type: 'string', length: 50, nullable: true)]
    private ?string $ip = null;

    #[ORM\Column(name: 'route', type: 'string', length: 255, nullable: true)]
    private ?string $route = null;

    /** @var array<string, mixed>|null */
    #[ORM\Column(name: 'extra', type: 'json', nullable: true)]
    private ?array $extra = null;

    #[ORM\Column(name: 'created_at', type: 'datetime')]
    private \DateTimeInterface $createdAt;

    #[ORM\Column(name: 'updated_at', type: 'datetime', nullable: true)]
    private ?\DateTimeInterface $updatedAt = null;

    public function __construct(Utilisateurs $createdBy)
    {
        $this->createdBy = $createdBy;
        $this->createdAt = new \DateTime();
    }

    // ── Lifecycle callbacks ───────────────────────────────────────────────────

    #[ORM\PrePersist]
    public function initCreatedAt(): void
    {
        $this->createdAt = new \DateTime();
    }

    #[ORM\PreUpdate]
    public function refreshUpdatedAt(): void
    {
        $this->updatedAt = new \DateTime();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public function getId(): ?int { return $this->id; }

    public function getCreatedBy(): Utilisateurs { return $this->createdBy; }

    public function getUserId(): ?int { return $this->userId; }
    public function setUserId(?int $v): static { $this->userId = $v; return $this; }

    public function getUserName(): ?string { return $this->userName; }
    public function setUserName(?string $v): static { $this->userName = $v; return $this; }

    public function getUserEmail(): ?string { return $this->userEmail; }
    public function setUserEmail(?string $v): static { $this->userEmail = $v; return $this; }

    public function getUserRole(): ?string { return $this->userRole; }
    public function setUserRole(?string $v): static { $this->userRole = $v; return $this; }

    public function getUserAvatar(): ?string { return $this->userAvatar; }
    public function setUserAvatar(?string $v): static { $this->userAvatar = $v; return $this; }

    // FIX: getter/setter cast between string (DB) and ActivityActionEnum (app layer).
    public function getAction(): ActivityActionEnum { return ActivityActionEnum::from($this->action); }
    public function setAction(ActivityActionEnum $v): static { $this->action = $v->value; return $this; }

    public function getLabel(): string { return $this->label; }
    public function setLabel(string $v): static { $this->label = $v; return $this; }

    public function getContext(): string { return $this->context; }
    public function setContext(string $v): static { $this->context = $v; return $this; }

    public function getIp(): ?string { return $this->ip; }
    public function setIp(?string $v): static { $this->ip = $v; return $this; }

    public function getRoute(): ?string { return $this->route; }
    public function setRoute(?string $v): static { $this->route = $v; return $this; }

    /** @return array<string, mixed>|null */
    public function getExtra(): ?array { return $this->extra; }
    /** @param array<string, mixed>|null $v */
    public function setExtra(?array $v): static { $this->extra = $v; return $this; }

    public function getCreatedAt(): \DateTimeInterface { return $this->createdAt; }

    public function getUpdatedAt(): ?\DateTimeInterface { return $this->updatedAt; }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Retourne les métadonnées visuelles (icône, couleurs) pour cette action.
     *
     * @return array<string, string>
     */
    public function getIconMeta(): array
    {
        return ActivityActionEnum::from($this->action)->iconMeta();
    }

    /**
     * Retourne un label court pour le contexte.
     */
    public function getContextLabel(): string
    {
        return match ($this->context) {
            'backoffice'  => 'Back-office',
            'frontoffice' => 'Front-office',
            default       => ucfirst($this->context),
        };
    }
}