<?php

namespace App\Enum;

enum ActivityActionEnum: string
{
    case USER_LOGIN           = 'user.login';
    case USER_LOGOUT          = 'user.logout';
    case USER_REGISTER        = 'user.register';
    case USER_EMAIL_VERIFIED  = 'user.email_verified';
    case USER_2FA_SETUP       = 'user.2fa_setup';
    case USER_OAUTH_LOGIN     = 'user.oauth_login';
    case USER_PASSWORD_RESET  = 'user.password_reset';
    case USER_CREATED         = 'user.created';
    case USER_UPDATED         = 'user.updated';
    case USER_DELETED         = 'user.deleted';
    case USER_TOGGLED         = 'user.toggled';
    case PROFIL_UPDATED       = 'profil.updated';
    case PROFIL_AVATAR        = 'profil.avatar';
    case PROFIL_BIO           = 'profil.bio';
    case ADMIN_VIEW           = 'admin.view';
    case EXPORT_CSV           = 'export.csv';
    case EXPORT_PDF           = 'export.pdf';

    public function label(): string
    {
        return match($this) {
            self::USER_LOGIN          => 'Connexion réussie',
            self::USER_LOGOUT         => 'Déconnexion',
            self::USER_REGISTER       => 'Inscription',
            self::USER_EMAIL_VERIFIED => 'Email vérifié',
            self::USER_2FA_SETUP      => 'Double authentification configurée',
            self::USER_OAUTH_LOGIN    => 'Connexion OAuth',
            self::USER_PASSWORD_RESET => 'Réinitialisation du mot de passe',
            self::USER_CREATED        => 'Utilisateur créé',
            self::USER_UPDATED        => 'Utilisateur mis à jour',
            self::USER_DELETED        => 'Utilisateur supprimé',
            self::USER_TOGGLED        => 'Statut utilisateur modifié',
            self::PROFIL_UPDATED      => 'Profil mis à jour',
            self::PROFIL_AVATAR       => 'Avatar mis à jour',
            self::PROFIL_BIO          => 'Bio mise à jour',
            self::ADMIN_VIEW          => 'Vue administration',
            self::EXPORT_CSV          => 'Export CSV',
            self::EXPORT_PDF          => 'Export PDF',
        };
    }

    public function icon(): string
    {
        return match($this) {
            self::USER_LOGIN          => '🔑',
            self::USER_LOGOUT         => '👋',
            self::USER_REGISTER       => '✨',
            self::USER_EMAIL_VERIFIED => '✅',
            self::USER_2FA_SETUP      => '🔐',
            self::USER_OAUTH_LOGIN    => '🌐',
            self::USER_PASSWORD_RESET => '🔒',
            self::USER_CREATED        => '➕',
            self::USER_UPDATED        => '✏️',
            self::USER_DELETED        => '🗑️',
            self::USER_TOGGLED        => '🔄',
            self::PROFIL_UPDATED      => '👤',
            self::PROFIL_AVATAR       => '🖼️',
            self::PROFIL_BIO          => '📝',
            self::ADMIN_VIEW          => '👁️',
            self::EXPORT_CSV          => '📊',
            self::EXPORT_PDF          => '📄',
        };
    }

    /** @return array<string, string> */
    public function iconMeta(): array
    {
        return match($this) {
            self::USER_LOGIN          => ['icon' => '🔑', 'color' => '#52B788', 'bg' => '#D8F3DC'],
            self::USER_LOGOUT         => ['icon' => '👋', 'color' => '#5A6475', 'bg' => '#F0F2F5'],
            self::USER_REGISTER       => ['icon' => '✨', 'color' => '#7B5EA7', 'bg' => '#EDE9F6'],
            self::USER_EMAIL_VERIFIED => ['icon' => '✅', 'color' => '#00897B', 'bg' => '#E0F2F1'],
            self::USER_2FA_SETUP      => ['icon' => '🔐', 'color' => '#1565C0', 'bg' => '#E3F2FD'],
            self::USER_OAUTH_LOGIN    => ['icon' => '🌐', 'color' => '#E65100', 'bg' => '#FFF3E0'],
            self::USER_PASSWORD_RESET => ['icon' => '🔒', 'color' => '#B71C1C', 'bg' => '#FFEBEE'],
            self::USER_CREATED        => ['icon' => '➕', 'color' => '#1B4332', 'bg' => '#D8F3DC'],
            self::USER_UPDATED        => ['icon' => '✏️', 'color' => '#0277BD', 'bg' => '#E1F5FE'],
            self::USER_DELETED        => ['icon' => '🗑️', 'color' => '#B71C1C', 'bg' => '#FFEBEE'],
            self::USER_TOGGLED        => ['icon' => '🔄', 'color' => '#E65100', 'bg' => '#FFF3E0'],
            self::PROFIL_UPDATED      => ['icon' => '👤', 'color' => '#7B5EA7', 'bg' => '#EDE9F6'],
            self::PROFIL_AVATAR       => ['icon' => '🖼️', 'color' => '#7B5EA7', 'bg' => '#EDE9F6'],
            self::PROFIL_BIO          => ['icon' => '📝', 'color' => '#00897B', 'bg' => '#E0F2F1'],
            self::ADMIN_VIEW          => ['icon' => '👁️', 'color' => '#5A6475', 'bg' => '#F0F2F5'],
            self::EXPORT_CSV          => ['icon' => '📊', 'color' => '#1565C0', 'bg' => '#E3F2FD'],
            self::EXPORT_PDF          => ['icon' => '📄', 'color' => '#B71C1C', 'bg' => '#FFEBEE'],
        };
    }
}
