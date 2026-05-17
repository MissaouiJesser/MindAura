package tn.esprit.enums;

public enum TypeTraitement {
    PSYCHOLOGIQUE("Psychologique"),
    COMPORTEMENTAL("Comportemental"),
    MIXTE("Mixte");

    private final String libelle;

    TypeTraitement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static TypeTraitement fromString(String text) {
        if (text == null) return PSYCHOLOGIQUE;

        text = text.trim().toUpperCase();

        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return PSYCHOLOGIQUE;
        }
    }
}
