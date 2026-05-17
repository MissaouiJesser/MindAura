package tn.esprit.services;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Service pour calculer automatiquement le prix des réservations
 * Règle : 80 DT pour la première heure, puis 20 DT par heure supplémentaire
 */
public class PriceCalculatorService {

    private static final int PRIX_PREMIERE_HEURE = 80;  // 80 DT
    private static final int PRIX_HEURE_SUPPLEMENTAIRE = 20;  // 20 DT

    /**
     * Calcule le prix d'une réservation selon la durée
     * @param heureDebut Heure de début de la réservation
     * @param heureFin Heure de fin de la réservation
     * @return Le prix en DT
     */
    public static int calculerPrix(Date heureDebut, Date heureFin) {
        if (heureDebut == null || heureFin == null) {
            return 0;
        }

        // Vérifier que heureFin est après heureDebut
        if (!heureFin.after(heureDebut)) {
            return 0;
        }

        // Calculer la durée en millisecondes
        long dureeMs = heureFin.getTime() - heureDebut.getTime();

        // Convertir en minutes
        long dureeMinutes = TimeUnit.MILLISECONDS.toMinutes(dureeMs);

        // Calculer le nombre d'heures (arrondi au supérieur)
        long heures = (dureeMinutes + 59) / 60; // Arrondi au supérieur

        // Calculer le prix
        int prix;
        if (heures <= 1) {
            // Première heure : 80 DT
            prix = PRIX_PREMIERE_HEURE;
        } else {
            // Première heure + heures supplémentaires
            prix = PRIX_PREMIERE_HEURE + ((int)(heures - 1) * PRIX_HEURE_SUPPLEMENTAIRE);
        }

        return prix;
    }

    /**
     * Formatte la durée pour l'affichage
     * @param heureDebut Heure de début
     * @param heureFin Heure de fin
     * @return Texte formaté (ex: "2h 30min")
     */
    public static String formatterDuree(Date heureDebut, Date heureFin) {
        if (heureDebut == null || heureFin == null || !heureFin.after(heureDebut)) {
            return "0h 0min";
        }

        long dureeMs = heureFin.getTime() - heureDebut.getTime();
        long dureeMinutes = TimeUnit.MILLISECONDS.toMinutes(dureeMs);

        long heures = dureeMinutes / 60;
        long minutes = dureeMinutes % 60;

        return heures + "h " + minutes + "min";
    }

    /**
     * Calcule et formatte le détail du prix
     * @param heureDebut Heure de début
     * @param heureFin Heure de fin
     * @return Détail du calcul (ex: "2h → 80 DT + 20 DT = 100 DT")
     */
    public static String getDetailPrix(Date heureDebut, Date heureFin) {
        if (heureDebut == null || heureFin == null || !heureFin.after(heureDebut)) {
            return "Durée invalide";
        }

        long dureeMs = heureFin.getTime() - heureDebut.getTime();
        long dureeMinutes = TimeUnit.MILLISECONDS.toMinutes(dureeMs);
        long heures = (dureeMinutes + 59) / 60;

        if (heures <= 1) {
            return String.format("%s → %d DT (première heure)",
                    formatterDuree(heureDebut, heureFin),
                    PRIX_PREMIERE_HEURE);
        } else {
            int prixSupp = (int)(heures - 1) * PRIX_HEURE_SUPPLEMENTAIRE;
            int total = PRIX_PREMIERE_HEURE + prixSupp;
            return String.format("%s → %d DT + %d DT = %d DT",
                    formatterDuree(heureDebut, heureFin),
                    PRIX_PREMIERE_HEURE,
                    prixSupp,
                    total);
        }
    }
}