package tn.esprit.enums;

public enum MotifReservation {
    CONSULTATION_INDIVIDUELLE("Consultation Individuelle"),
    THERAPIE_DE_GROUPE("Therapie De Groupe"),
    SUIVI_PSYCHOLOGIQUE("Suivi Psychologique"),
    EVALATION_PSYCHOLOGIQUE("Evalation Psychologique"),
    CONSULTATION_URGENCE("Consultation D'urgence"),
    ATELIER_DE_DEVELOPPEMENT_PERSONNEL("Atelier De Developpement Personnel");

    private final String libelle;

    MotifReservation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static MotifReservation fromString(String text) {
        if (text == null) return CONSULTATION_INDIVIDUELLE;

        text = text.trim().toUpperCase();

        try {
            return valueOf(text);
        } catch (IllegalArgumentException e) {
            return CONSULTATION_INDIVIDUELLE; // Valeur par défaut
        }
    }

    @Override
    public String toString() {
        return libelle;
    }
}
