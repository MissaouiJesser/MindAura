package tn.esprit.services;

import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_local;
import tn.esprit.enums.StatutReservation;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour gérer la capacité et la disponibilité des locaux
 * en fonction des réservations actives
 */
public class LocalCapacityService {

    private local_psychiatrie_SERVICE localService;
    private reservation_local_SERVICE reservationService;

    // Map pour stocker la capacité originale de chaque local
    private static Map<Integer, String> originalCapacities = new HashMap<>();

    public LocalCapacityService() {
        this.localService = new local_psychiatrie_SERVICE();
        this.reservationService = new reservation_local_SERVICE();
    }

    /**
     * Met à jour la capacité et la disponibilité d'un local lors de l'ajout d'une réservation
     * @param idLocal ID du local réservé
     */
    public void onReservationAdded(int idLocal) throws SQLException {
        local_psychiatrie local = getLocalById(idLocal);

        if (local != null) {
            // Sauvegarder la capacité originale si c'est la première réservation
            if (!originalCapacities.containsKey(idLocal)) {
                originalCapacities.put(idLocal, local.getCapacite_local());
                System.out.println("💾 Capacité originale sauvegardée: " + local.getCapacite_local());
            }

            // Diminuer la capacité de 1
            String capaciteActuelle = local.getCapacite_local();
            try {
                int capacite = Integer.parseInt(capaciteActuelle.replaceAll("[^0-9]", ""));
                if (capacite > 0) {
                    capacite--;
                    local.setCapacite_local(String.valueOf(capacite));
                }
            } catch (NumberFormatException e) {
                System.err.println("⚠️ Format de capacité non valide: " + capaciteActuelle);
            }

            // Mettre à jour la disponibilité
            local.setDisponibilite_local("Sur réservation");

            // Sauvegarder les modifications
            localService.modifier(local);

            System.out.println("✅ Local mis à jour:");
            System.out.println("   Nom: " + local.getNom_local());
            System.out.println("   Nouvelle capacité: " + local.getCapacite_local());
            System.out.println("   Disponibilité: " + local.getDisponibilite_local());
        }
    }

    /**
     * Restaure la capacité et la disponibilité d'un local lors de la suppression d'une réservation
     * @param idLocal ID du local libéré
     */
    public void onReservationRemoved(int idLocal) throws SQLException {
        local_psychiatrie local = getLocalById(idLocal);

        if (local != null) {
            // Compter les réservations actives restantes pour ce local
            int activeReservations = countActiveReservations(idLocal);

            System.out.println("📊 Réservations actives restantes: " + activeReservations);

            if (activeReservations == 0) {
                // Plus de réservations actives, restaurer la capacité originale
                if (originalCapacities.containsKey(idLocal)) {
                    local.setCapacite_local(originalCapacities.get(idLocal));
                    System.out.println("🔄 Capacité restaurée: " + originalCapacities.get(idLocal));
                } else {
                    // Augmenter la capacité de 1 si on n'a pas la valeur originale
                    String capaciteActuelle = local.getCapacite_local();
                    try {
                        int capacite = Integer.parseInt(capaciteActuelle.replaceAll("[^0-9]", ""));
                        capacite++;
                        local.setCapacite_local(String.valueOf(capacite));
                    } catch (NumberFormatException e) {
                        System.err.println("⚠️ Format de capacité non valide: " + capaciteActuelle);
                    }
                }

                // Remettre le local comme "Disponible"
                local.setDisponibilite_local("Disponible");
                System.out.println("✅ Local redevenu Disponible");

            } else {
                // Il reste des réservations, augmenter juste la capacité de 1
                String capaciteActuelle = local.getCapacite_local();
                try {
                    int capacite = Integer.parseInt(capaciteActuelle.replaceAll("[^0-9]", ""));
                    capacite++;
                    local.setCapacite_local(String.valueOf(capacite));
                } catch (NumberFormatException e) {
                    System.err.println("⚠️ Format de capacité non valide: " + capaciteActuelle);
                }

                // Garder "Sur réservation"
                System.out.println("📌 Local reste Sur réservation (réservations actives: " + activeReservations + ")");
            }

            // Sauvegarder les modifications
            localService.modifier(local);

            System.out.println("✅ Local mis à jour après suppression:");
            System.out.println("   Nom: " + local.getNom_local());
            System.out.println("   Nouvelle capacité: " + local.getCapacite_local());
            System.out.println("   Disponibilité: " + local.getDisponibilite_local());
        }
    }

    /**
     * Compte le nombre de réservations actives (non annulées, non terminées) pour un local
     */
    private int countActiveReservations(int idLocal) throws SQLException {
        List<reservation_local> allReservations = reservationService.afficherList();

        return (int) allReservations.stream()
                .filter(r -> r.getId_local() == idLocal)
                .filter(r -> r.getStatus_reservation() != StatutReservation.ANNULEE)
                .filter(r -> r.getStatus_reservation() != StatutReservation.TERMINEE)
                .count();
    }

    /**
     * Récupère un local par son ID
     */
    private local_psychiatrie getLocalById(int idLocal) throws SQLException {
        List<local_psychiatrie> locaux = localService.afficherList();

        return locaux.stream()
                .filter(l -> l.getId_local() == idLocal)
                .findFirst()
                .orElse(null);
    }

    /**
     * Synchronise tous les locaux avec leurs réservations actuelles
     * Utile pour une remise à niveau globale
     */
    public void synchronizeAllLocals() throws SQLException {
        System.out.println("🔄 Synchronisation de tous les locaux...");

        List<local_psychiatrie> locaux = localService.afficherList();

        for (local_psychiatrie local : locaux) {
            int activeReservations = countActiveReservations(local.getId_local());

            if (activeReservations > 0) {
                local.setDisponibilite_local("Sur réservation");
            } else {
                local.setDisponibilite_local("Disponible");
                // Restaurer la capacité originale si disponible
                if (originalCapacities.containsKey(local.getId_local())) {
                    local.setCapacite_local(originalCapacities.get(local.getId_local()));
                }
            }

            localService.modifier(local);
        }

        System.out.println("✅ Synchronisation terminée pour " + locaux.size() + " locaux");
    }
}