package tn.esprit.enums;

public enum Objectif {
    GESTION_DU_STRESS("Gestion Du Stress"),
    ANXIETE("Anxiété");

    private final String libelle;

    Objectif(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static Objectif fromString(String text) {
        if (text == null) return GESTION_DU_STRESS;

        text = text.trim().toUpperCase();

        text = text.replace(" ", "_")
                .replace("É", "E")
                .replace("È", "E")
                .replace("Ê", "E");

        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return GESTION_DU_STRESS;
        }
    }
}
