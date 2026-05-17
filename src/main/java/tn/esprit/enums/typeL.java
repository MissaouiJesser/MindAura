package tn.esprit.enums;

public enum typeL {
    CABINET_PRIVE("Cabinet Prive"),
    CLINIQUE_PSYCHIATRIQUE("Clinique Psychiatrique"),
    CENTRE_DE_SANTE_MENTALE("Centre De Sante Mentale"),
    HOPITAL("Hopital"),
    CENTRE_DE_BIEN_ETRE("Centre De Bien Etre"),
    ESPACE_DE_THERAPIE("Espace De Therapie");

    private final String libelle;

    typeL(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static typeL fromString(String text) {
        if (text == null) return HOPITAL;

        text = text.trim().toUpperCase();

        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return HOPITAL; // Valeur par défaut
        }
    }

    @Override
    public String toString() {
        return libelle;
    }
}