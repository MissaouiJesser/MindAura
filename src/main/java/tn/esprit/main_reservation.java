package tn.esprit;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.entities.reservation_local;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.enums.StatutReservation;
import tn.esprit.enums.MotifReservation;

public class main_reservation {
    public static void main(String[] args) {
        reservation_local_SERVICE rs = new reservation_local_SERVICE();
        local_psychiatrie_SERVICE ls = new local_psychiatrie_SERVICE();

        try {
            // Vérifier qu'il y a des locaux disponibles
            List<local_psychiatrie> locaux = ls.afficherList();

            if (locaux.isEmpty()) {
                System.out.println("ATTENTION: Aucun local dans la base de données!");
                System.out.println("Veuillez d'abord ajouter des locaux psychiatriques.");
                return;
            }

            System.out.println("Nombre de locaux trouvés : " + locaux.size());
            for (local_psychiatrie local : locaux) {
                System.out.println("  - ID: " + local.getId_local() +
                        " | " + local.getNom_local() +
                        " | Type: " + local.getType_local().getLibelle() +
                        " | Ville: " + local.getVille_local());
            }

            // Récupérer les IDs des locaux
            int idLocal1 = locaux.get(0).getId_local();
            int idLocal2 = locaux.size() > 1 ? locaux.get(1).getId_local() : idLocal1;
            int idLocal3 = locaux.size() > 2 ? locaux.get(2).getId_local() : idLocal1;

            System.out.println("\n========================================");
            System.out.println("    TEST 1: AJOUT DE RÉSERVATIONS");
            System.out.println("========================================");

            // Réservation 1: Consultation individuelle
            reservation_local res1 = new reservation_local();
            res1.setId_utilisateur(1);
            res1.setId_local(idLocal1);
            res1.setDate_reservation(new Date());
            res1.setHeure_debut_reservation(new Date(System.currentTimeMillis() + 2L * 60 * 60 * 1000)); // Dans 2 heures
            res1.setHeure_fin_reservation(new Date(System.currentTimeMillis() + 3L * 60 * 60 * 1000)); // Dans 3 heures
            res1.setStatus_reservation(StatutReservation.EN_ATTENTE);
            res1.setMotif_reservation(MotifReservation.CONSULTATION_INDIVIDUELLE);
            res1.setPrix_reservation(150);
            res1.setNom_cl("Ben Ahmed");
            res1.setPrenom_cl("Fatma");

            rs.add(res1);

            // Réservation 2: Thérapie de groupe
            reservation_local res2 = new reservation_local();
            res2.setId_utilisateur(2);
            res2.setId_local(idLocal2);
            res2.setDate_reservation(new Date(System.currentTimeMillis() + 1L * 24 * 60 * 60 * 1000)); // Demain
            res2.setHeure_debut_reservation(new Date(System.currentTimeMillis() + 1L * 24 * 60 * 60 * 1000 + 9L * 60 * 60 * 1000)); // Demain à 9h
            res2.setHeure_fin_reservation(new Date(System.currentTimeMillis() + 1L * 24 * 60 * 60 * 1000 + 11L * 60 * 60 * 1000)); // Demain à 11h
            res2.setStatus_reservation(StatutReservation.CONFIRMEE);
            res2.setMotif_reservation(MotifReservation.THERAPIE_DE_GROUPE);
            res2.setPrix_reservation(200);
            res2.setNom_cl("Trabelsi");
            res2.setPrenom_cl("Mohamed");

            rs.add(res2);

            // Réservation 3: Consultation d'urgence
            reservation_local res3 = new reservation_local();
            res3.setId_utilisateur(3);
            res3.setId_local(idLocal3);
            res3.setDate_reservation(new Date());
            res3.setHeure_debut_reservation(new Date(System.currentTimeMillis() + 30L * 60 * 1000)); // Dans 30 minutes
            res3.setHeure_fin_reservation(new Date(System.currentTimeMillis() + 90L * 60 * 1000)); // Dans 1h30
            res3.setStatus_reservation(StatutReservation.CONFIRMEE);
            res3.setMotif_reservation(MotifReservation.CONSULTATION_URGENCE);
            res3.setPrix_reservation(250);
            res3.setNom_cl("Gharbi");
            res3.setPrenom_cl("Leila");

            rs.add(res3);

            System.out.println("\n========================================");
            System.out.println("    TEST 2: AFFICHAGE DES RÉSERVATIONS");
            System.out.println("========================================");

            List<reservation_local> reservations = rs.afficherList();

            if (reservations.isEmpty()) {
                System.out.println("Aucune réservation trouvée dans la base de données.");
            } else {
                System.out.println("Nombre de réservations : " + reservations.size());
                for (reservation_local res : reservations) {
                    System.out.println("ID: " + res.getId_reservation() +
                            " | Client: " + res.getNom_cl() + " " + res.getPrenom_cl() +
                            " | Motif: " + res.getMotif_reservation().getLibelle() +
                            " | Statut: " + res.getStatus_reservation().getLibelle() +
                            " | Prix: " + res.getPrix_reservation() + " DT" +
                            " | Local ID: " + res.getId_local() +
                            " | Utilisateur ID: " + res.getId_utilisateur());
                }
            }

            System.out.println("\n========================================");
            System.out.println("    TEST 3: MODIFICATION D'UNE RÉSERVATION");
            System.out.println("========================================");

            if (!reservations.isEmpty()) {
                reservation_local resToModify = reservations.get(0);

                System.out.println("Modification de la réservation ID: " + resToModify.getId_reservation());

                resToModify.setStatus_reservation(StatutReservation.CONFIRMEE);
                resToModify.setPrix_reservation(180);
                resToModify.setMotif_reservation(MotifReservation.SUIVI_PSYCHOLOGIQUE);
                resToModify.setHeure_fin_reservation(new Date(System.currentTimeMillis() + 4L * 60 * 60 * 1000)); // Dans 4 heures

                rs.modifier(resToModify);

                System.out.println("\n--- Après modification ---");
                reservations = rs.afficherList();
                for (reservation_local res : reservations) {
                    System.out.println("ID: " + res.getId_reservation() +
                            " | Client: " + res.getNom_cl() + " " + res.getPrenom_cl() +
                            " | Motif: " + res.getMotif_reservation().getLibelle() +
                            " | Statut: " + res.getStatus_reservation().getLibelle() +
                            " | Prix: " + res.getPrix_reservation() + " DT");
                }
            }

            System.out.println("\n========================================");
            System.out.println("    TEST 4: SUPPRESSION D'UNE RÉSERVATION");
            System.out.println("========================================");

            reservations = rs.afficherList();

            if (reservations.size() >= 2) {
                reservation_local resToDelete = reservations.get(1);
                System.out.println("Suppression de la réservation ID: " + resToDelete.getId_reservation() +
                        " - Client: " + resToDelete.getNom_cl() + " " + resToDelete.getPrenom_cl());

                rs.delete(resToDelete);

                System.out.println("\n--- Après suppression ---");
                reservations = rs.afficherList();
                System.out.println("Nombre de réservations restantes : " + reservations.size());
                for (reservation_local res : reservations) {
                    System.out.println("ID: " + res.getId_reservation() +
                            " | Client: " + res.getNom_cl() + " " + res.getPrenom_cl() +
                            " | Motif: " + res.getMotif_reservation().getLibelle() +
                            " | Statut: " + res.getStatus_reservation().getLibelle() +
                            " | Prix: " + res.getPrix_reservation() + " DT");
                }
            } else {
                System.out.println("Pas assez de réservations pour effectuer une suppression.");
            }

            System.out.println("\n========================================");
            System.out.println("    TESTS TERMINÉS AVEC SUCCÈS!");
            System.out.println("========================================");

        } catch (SQLException e) {
            System.out.println("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur générale: " + e.getMessage());
            e.printStackTrace();
        }
    }
}