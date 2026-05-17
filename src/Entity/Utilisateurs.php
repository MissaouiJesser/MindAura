<?php

namespace App\Entity;

use App\Repository\UtilisateursRepository;
use Doctrine\ORM\Mapping as ORM;
use Doctrine\Common\Collections\ArrayCollection;
use Doctrine\Common\Collections\Collection;
use Symfony\Component\Security\Core\User\PasswordAuthenticatedUserInterface;
use Symfony\Component\Security\Core\User\UserInterface;
use Symfony\Component\Validator\Constraints as Assert;
use Symfony\Bridge\Doctrine\Validator\Constraints\UniqueEntity;
use Symfony\Component\Serializer\Annotation\Ignore;

#[ORM\Entity(repositoryClass: UtilisateursRepository::class)]
#[ORM\Table(name: 'utilisateur')]
#[UniqueEntity(
    fields: ['emailUtilisateur'],
    message: 'Cette adresse e-mail est déjà utilisée.'
)]
class Utilisateurs implements UserInterface, PasswordAuthenticatedUserInterface
{
    // ══════════════════════════════════════════════════════════════════
    //  COLONNES
    // ══════════════════════════════════════════════════════════════════

    #[ORM\Id]
    #[ORM\GeneratedValue]
    #[ORM\Column(name: 'id_utilisateur', type: 'integer')]
    // FIX line 29 : int|null → int (jamais assigné manuellement, Doctrine gère l'ID)
    /** @phpstan-ignore property.unusedType */
    private ?int $idUtilisateur = null;

    #[ORM\Column(name: 'nom_utilisateur', type: 'string', length: 255)]
    #[Assert\NotBlank(message: 'Le nom est obligatoire.')]
    #[Assert\Length(
        min: 2,
        max: 100,
        minMessage: 'Le nom doit contenir au moins {{ limit }} caractères.',
        maxMessage: 'Le nom ne peut pas dépasser {{ limit }} caractères.'
    )]
    #[Assert\Regex(
        pattern: '/^[\p{L}\s\-\']+$/u',
        message: 'Le nom ne doit contenir que des lettres, espaces ou tirets.'
    )]
    private string $nomUtilisateur;

    #[ORM\Column(name: 'prenom_utilisateur', type: 'string', length: 255)]
    #[Assert\NotBlank(message: 'Le prénom est obligatoire.')]
    #[Assert\Length(
        min: 2,
        max: 100,
        minMessage: 'Le prénom doit contenir au moins {{ limit }} caractères.',
        maxMessage: 'Le prénom ne peut pas dépasser {{ limit }} caractères.'
    )]
    #[Assert\Regex(
        pattern: '/^[\p{L}\s\-\']+$/u',
        message: 'Le prénom ne doit contenir que des lettres, espaces ou tirets.'
    )]
    private string $prenomUtilisateur;

    #[ORM\Column(name: 'email_utilisateur', type: 'string', length: 255, unique: true)]
    #[Assert\NotBlank(message: "L'adresse e-mail est obligatoire.")]
    #[Assert\Email(message: "L'adresse e-mail '{{ value }}' n'est pas valide.")]
    #[Assert\Length(max: 255, maxMessage: "L'adresse e-mail ne peut pas dépasser {{ limit }} caractères.")]
    private string $emailUtilisateur;

    #[ORM\Column(name: 'mdp_utilisateur', type: 'string', length: 255)]
    private string $mdpUtilisateur;

    #[Assert\NotBlank(message: 'Le mot de passe est obligatoire.', groups: ['registration', 'create'])]
    #[Assert\Length(
        min: 8,
        minMessage: 'Le mot de passe doit contenir au moins {{ limit }} caractères.',
        groups: ['registration', 'create', 'password_change']
    )]
    #[Assert\Regex(
        pattern: '/[A-Z]/',
        message: 'Le mot de passe doit contenir au moins une lettre majuscule.',
        groups: ['registration', 'create', 'password_change']
    )]
    #[Assert\Regex(
        pattern: '/[0-9]/',
        message: 'Le mot de passe doit contenir au moins un chiffre.',
        groups: ['registration', 'create', 'password_change']
    )]
    private ?string $plainPassword = null;

    #[ORM\Column(name: 'telephone_utilisateur', type: 'string', length: 255, nullable: true)]
    #[Assert\NotBlank(message: 'Le numéro de téléphone est obligatoire.', groups: ['registration'])]
    #[Assert\Regex(
        pattern: '/^\+?[0-9\s\-]{8,20}$/',
        message: 'Le numéro de téléphone est invalide (ex: +216 XX XXX XXX).',
        groups: ['registration']
    )]
    private ?string $telephoneUtilisateur = null;

    #[ORM\Column(name: 'date_naissance_utilisateur', type: 'date', nullable: true)]
    #[Assert\NotNull(message: 'La date de naissance est obligatoire.', groups: ['registration'])]
    #[Assert\LessThan(
        value: '-16 years',
        message: "Vous devez avoir au moins 16 ans pour vous inscrire.",
        groups: ['registration']
    )]
    private ?\DateTimeInterface $dateNaissanceUtilisateur = null;

    #[ORM\Column(name: 'role_utilisateur', type: 'string', length: 255)]
    #[Assert\NotBlank(message: 'Veuillez choisir un rôle.')]
    #[Assert\Choice(
        choices: ['ROLE_PATIENT', 'ROLE_PSYCHOLOGUE', 'ROLE_COACH', 'ROLE_ADMIN', 'ROLE_USER'],
        message: 'Le rôle sélectionné est invalide.'
    )]
    private string $roleUtilisateur;

    #[ORM\Column(name: 'photo_profil_utilisateur', type: 'string', length: 255)]
    private string $photoProfilUtilisateur = 'default.png';

    #[ORM\Column(name: 'date_inscription_utilisateur', type: 'date')]
    private \DateTimeInterface $dateInscriptionUtilisateur;

    #[ORM\Column(name: 'est_actif_utilisateur', type: 'boolean')]
    private bool $estActifUtilisateur = true;

    #[ORM\Column(name: 'bio_utilisateur', type: 'text')]
    private string $bioUtilisateur = '';

    #[ORM\Column(name: 'totp_secret', type: 'string', length: 64, nullable: true)]
    #[Ignore]
    private ?string $totpSecret = null;

    #[ORM\Column(name: 'reset_password_token', type: 'string', length: 100, nullable: true)]
    #[Ignore]
    private ?string $resetPasswordToken = null;

    #[ORM\Column(name: 'reset_password_token_expiry', type: 'datetime', nullable: true)]
    #[Ignore]
    private ?\DateTimeInterface $resetPasswordTokenExpiry = null;

    #[ORM\Column(name: 'email_verification_token', type: 'string', length: 100, nullable: true)]
    #[Ignore]
    private ?string $emailVerificationToken = null;

    #[ORM\Column(name: 'email_verification_token_expiry', type: 'datetime', nullable: true)]
    #[Ignore]
    private ?\DateTimeInterface $emailVerificationTokenExpiry = null;

    #[ORM\Column(name: 'is_email_verified', type: 'boolean')]
    private bool $isEmailVerified = false;

    #[ORM\Column(name: 'google_id', type: 'string', length: 128, nullable: true, unique: true)]
    private ?string $googleId = null;

    #[ORM\Column(name: 'github_id', type: 'string', length: 64, nullable: true, unique: true)]
    private ?string $githubId = null;

    // ══════════════════════════════════════════════════════════════════
    //  UserInterface
    // ══════════════════════════════════════════════════════════════════

    public function getUserIdentifier(): string
    {
        return $this->emailUtilisateur;
    }

    public function getRoles(): array
    {
        $role = strtoupper($this->roleUtilisateur);
        if (!str_starts_with($role, 'ROLE_')) {
            $role = 'ROLE_' . $role;
        }
        return array_unique([$role, 'ROLE_USER']);
    }

    public function getPassword(): string
    {
        return $this->mdpUtilisateur;
    }

    public function eraseCredentials(): void
    {
        $this->plainPassword = null;
    }

    // ══════════════════════════════════════════════════════════════════
    //  GETTERS & SETTERS
    // ══════════════════════════════════════════════════════════════════

    public function getIdUtilisateur(): ?int { return $this->idUtilisateur; }

    public function getPlainPassword(): ?string { return $this->plainPassword; }
    public function setPlainPassword(?string $v): static { $this->plainPassword = $v; return $this; }

    public function getNomUtilisateur(): ?string { return $this->nomUtilisateur ?? null; }
    public function setNomUtilisateur(?string $v): static { $this->nomUtilisateur = $v ?? ''; return $this; }

    public function getPrenomUtilisateur(): ?string { return $this->prenomUtilisateur ?? null; }
    public function setPrenomUtilisateur(?string $v): static { $this->prenomUtilisateur = $v ?? ''; return $this; }

    public function getEmailUtilisateur(): ?string { return $this->emailUtilisateur ?? null; }
    public function setEmailUtilisateur(?string $v): static { $this->emailUtilisateur = $v ?? ''; return $this; }

    public function getMdpUtilisateur(): string { return $this->mdpUtilisateur; }
    public function setMdpUtilisateur(string $v): static { $this->mdpUtilisateur = $v; return $this; }

    public function getTelephoneUtilisateur(): ?string { return $this->telephoneUtilisateur ?? null; }
    public function setTelephoneUtilisateur(?string $v): static { $this->telephoneUtilisateur = $v; return $this; }

    public function getDateNaissanceUtilisateur(): ?\DateTimeInterface { return $this->dateNaissanceUtilisateur; }
    public function setDateNaissanceUtilisateur(?\DateTimeInterface $v): static { $this->dateNaissanceUtilisateur = $v; return $this; }

    public function getRoleUtilisateur(): ?string { return $this->roleUtilisateur ?? null; }
    public function setRoleUtilisateur(?string $v): static { $this->roleUtilisateur = $v ?? ''; return $this; }

    public function getPhotoProfilUtilisateur(): string { return $this->photoProfilUtilisateur; }
    public function setPhotoProfilUtilisateur(string $v): static { $this->photoProfilUtilisateur = $v; return $this; }

    public function getDateInscriptionUtilisateur(): \DateTimeInterface { return $this->dateInscriptionUtilisateur; }
    public function setDateInscriptionUtilisateur(\DateTimeInterface $v): static { $this->dateInscriptionUtilisateur = $v; return $this; }

    public function isEstActifUtilisateur(): bool { return $this->estActifUtilisateur; }
    public function setEstActifUtilisateur(bool $v): static { $this->estActifUtilisateur = $v; return $this; }

    public function getBioUtilisateur(): string { return $this->bioUtilisateur; }
    public function setBioUtilisateur(string $v): static { $this->bioUtilisateur = $v; return $this; }

    public function getTotpSecret(): ?string { return $this->totpSecret; }
    public function setTotpSecret(#[\SensitiveParameter] ?string $v): static { $this->totpSecret = $v; return $this; }

    public function getResetPasswordToken(): ?string { return $this->resetPasswordToken; }
    public function setResetPasswordToken(#[\SensitiveParameter] ?string $token): static { $this->resetPasswordToken = $token; return $this; }
    public function getResetPasswordTokenExpiry(): ?\DateTimeInterface { return $this->resetPasswordTokenExpiry; }

    /** @internal Géré via initResetPasswordToken() / clearResetPasswordToken(). */
    protected function setResetPasswordTokenExpiry(#[\SensitiveParameter] ?\DateTimeInterface $expiry): static { $this->resetPasswordTokenExpiry = $expiry; return $this; }

    /**
     * Fixe le token de reset et son expiration en une opération atomique.
     * À utiliser dans ResetPasswordController à la place des deux setters séparés.
     */
    public function initResetPasswordToken(#[\SensitiveParameter] string $token, \DateTimeInterface $expiry): static
    {
        $this->resetPasswordToken       = $token;
        $this->resetPasswordTokenExpiry = $expiry;
        return $this;
    }

    /**
     * Invalide le token après utilisation réussie.
     */
    public function clearResetPasswordToken(): static
    {
        $this->resetPasswordToken       = null;
        $this->resetPasswordTokenExpiry = null;
        return $this;
    }

    public function getGoogleId(): ?string { return $this->googleId; }
    public function setGoogleId(?string $v): static { $this->googleId = $v; return $this; }

    public function getGithubId(): ?string { return $this->githubId; }
    public function setGithubId(?string $v): static { $this->githubId = $v; return $this; }

    public function getEmailVerificationToken(): ?string { return $this->emailVerificationToken; }
    public function setEmailVerificationToken(#[\SensitiveParameter] ?string $token): static { $this->emailVerificationToken = $token; return $this; }

    public function getEmailVerificationTokenExpiry(): ?\DateTimeInterface { return $this->emailVerificationTokenExpiry; }

    /** @internal Géré via initEmailVerificationToken() / clearEmailVerificationToken(). */
    protected function setEmailVerificationTokenExpiry(#[\SensitiveParameter] ?\DateTimeInterface $expiry): static { $this->emailVerificationTokenExpiry = $expiry; return $this; }

    /**
     * Fixe le token de vérification d'email et son expiration en une opération atomique.
     * À utiliser dans RegistrationController / EmailVerificationController.
     */
    public function initEmailVerificationToken(#[\SensitiveParameter] string $token, \DateTimeInterface $expiry): static
    {
        $this->emailVerificationToken       = $token;
        $this->emailVerificationTokenExpiry = $expiry;
        return $this;
    }

    /**
     * Invalide le token après vérification réussie.
     */
    public function clearEmailVerificationToken(): static
    {
        $this->emailVerificationToken       = null;
        $this->emailVerificationTokenExpiry = null;
        return $this;
    }

    public function isEmailVerified(): bool { return $this->isEmailVerified; }
    public function setIsEmailVerified(bool $v): static { $this->isEmailVerified = $v; return $this; }
}