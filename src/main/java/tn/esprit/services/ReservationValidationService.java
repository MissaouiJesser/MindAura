package tn.esprit.services;

import tn.esprit.entities.reservation_local;
import tn.esprit.enums.StatutReservation;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Service pour valider les réservations et éviter les conflits
 */
public class ReservationValidationService {

    private reservation_local_SERVICE reservationService;

    public ReservationValidationService() {
        this.reservationService = new reservation_local_SERVICE();
    }

    /**
     * Vérifie s'il y a un conflit d'horaires pour un local donné
     * @param idLocal ID du local à vérifier
     * @param dateReservation Date de la réservation
     * @param heureDebut Heure de début souhaitée
     * @param heureFin Heure de fin souhaitée
     * @param idReservationExclure ID de la réservation à exclure (pour modification)
     * @return true s'il y a un conflit, false sinon
     */
    public boolean hasConflict(int idLocal, Date dateReservation, Date heureDebut,
                               Date heureFin, Integer idReservationExclure) {
        try {
            List<reservation_local> reservations = reservationService.afficherList();

            for (reservation_local res : reservations) {
                // Exclure la réservation en cours de modification
                if (idReservationExclure != null &&
                        res.getId_reservation() == idReservationExclure) {
                    continue;
                }

                // Ignorer les réservations annulées ou terminées
                if (res.getStatus_reservation() == StatutReservation.ANNULEE ||
                        res.getStatus_reservation() == StatutReservation.TERMINEE) {
                    continue;
                }

                // Vérifier si c'est le même local
                if (res.getId_local() != idLocal) {
                    continue;
                }

                // Vérifier si c'est la même date (comparer juste jour/mois/année)
                if (!isSameDay(res.getDate_reservation(), dateReservation)) {
                    continue;
                }

                // Vérifier le chevauchement d'horaires
                boolean overlap = !(heureFin.before(res.getHeure_debut_reservation()) ||
                        heureDebut.after(res.getHeure_fin_reservation()) ||
                        heureFin.equals(res.getHeure_debut_reservation()) ||
                        heureDebut.equals(res.getHeure_fin_reservation()));

                if (overlap) {
                    return true; // Conflit détecté !
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false; // Pas de conflit
    }

    /**
     * Vérifie si deux dates sont le même jour
     */
    private boolean isSameDay(Date date1, Date date2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /**
     * Obtient le message d'erreur en cas de conflit
     */
    public String getConflictMessage(int idLocal, Date heureDebut, Date heureFin) {
        return "⚠️ CONFLIT D'HORAIRES DÉTECTÉ!\n\n" +
                "Ce local est déjà réservé dans cette plage horaire.\n" +
                "Veuillez choisir :\n" +
                "• Un autre créneau horaire\n" +
                "• Un autre local disponible";
    }
}