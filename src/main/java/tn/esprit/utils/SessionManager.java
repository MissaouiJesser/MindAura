package tn.esprit.utils;

import tn.esprit.entities.utilisateurs;

/**
 * Gestionnaire de session – conserve l'utilisateur connecté.
 * Singleton statique, accessible de partout dans l'application.
 */
public class SessionManager {

    private static utilisateurs currentUser = null;

    private SessionManager() {}

    /** Définit l'utilisateur connecté */
    public static void setCurrentUser(utilisateurs user) {
        currentUser = user;
    }

    /** Retourne l'utilisateur connecté (null si aucun) */
    public static utilisateurs getCurrentUser() {
        return currentUser;
    }

    /** Vérifie si un utilisateur est connecté */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /** Vérifie si l'utilisateur connecté est admin */
    public static boolean isAdmin() {
        if (currentUser == null) return false;
        return "ROLE_ADMIN".equals(currentUser.getRole_utilisateur().name());
    }

    /** Déconnexion – vide la session */
    public static void logout() {
        currentUser = null;
    }

    /** Retourne le nom complet de l'utilisateur connecté */
    public static String getNomComplet() {
        if (currentUser == null) return "";
        return currentUser.getPrenom_utilisateur() + " " + currentUser.getNom_utilisateur();
    }

    /** Retourne les initiales de l'utilisateur connecté */
    public static String getInitiales() {
        if (currentUser == null) return "?";
        String p = currentUser.getPrenom_utilisateur();
        String n = currentUser.getNom_utilisateur();
        String pi = (p != null && !p.isEmpty()) ? String.valueOf(p.charAt(0)).toUpperCase() : "";
        String ni = (n != null && !n.isEmpty()) ? String.valueOf(n.charAt(0)).toUpperCase() : "";
        return pi + ni;
    }

    /** Retourne le libellé du rôle pour affichage */
    public static String getRoleLibelle() {
        if (currentUser == null) return "";
        return currentUser.getRole_utilisateur().getLibelle();
    }

    /** Retourne le chemin de la photo de profil de l'utilisateur connecté */
    public static String getPhotoPath() {
        if (currentUser == null) return null;
        return currentUser.getPhoto_profil_utilisateur();
    }
}