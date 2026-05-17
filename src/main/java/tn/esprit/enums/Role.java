package tn.esprit.enums;

public enum Role {
    ROLE_USER("Utilisateur"),
    ROLE_ADMIN("Administrateur"),
    ROLE_PSYCHOLOGUE("Psychologue"),
    ROLE_PATIENT("Patient"),
    ROLE_COACH("Coach");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static Role fromString(String text) {
        if (text == null) return ROLE_USER;

        text = text.trim().toUpperCase();

        // Si le texte ne commence pas par "ROLE_", on l'ajoute
        if (!text.startsWith("ROLE_")) {
            text = "ROLE_" + text;
        }

        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return ROLE_USER; // Valeur par défaut
        }
    }
}
