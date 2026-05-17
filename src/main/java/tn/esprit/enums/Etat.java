package tn.esprit.enums;

public enum Etat {
    EN_COURS("En Cours"),
    TERMINE("Terminé"),
    SUSPENDU("Suspendu");

    private final String libelle;

    Etat(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static Etat fromString(String text) {
        if (text == null) return EN_COURS;

        text = text.trim().toUpperCase();

        text = text.replace(" ", "_")
                .replace("É", "E")
                .replace("È", "E")
                .replace("Ê", "E");

        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return EN_COURS;
        }
    }
}
