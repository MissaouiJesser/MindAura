package tn.esprit.mains;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import tn.esprit.services.traitement_service;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.entities.traitement;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Objectif;
import tn.esprit.enums.Etat;
import tn.esprit.enums.TypeTraitement;

public class Main2 {
    public static void main(String[] args) {
        traitement_service ts = new traitement_service();
        utilisateurs_service us = new utilisateurs_service();

        try {
            List<utilisateurs> users = us.afficherList();

            if (users.isEmpty()) {
                System.out.println("ATTENTION: Aucun utilisateur dans la base de données!");
                System.out.println("Veuillez d'abord ajouter des utilisateurs.");
                return;
            }

            System.out.println("Nombre d'utilisateurs trouvés : " + users.size());
            for (utilisateurs user : users) {
                System.out.println("  - ID: " + user.getId_utilisateur() +
                        " | " + user.getNom_utilisateur() +
                        " " + user.getPrenom_utilisateur() +
                        " | Rôle: " + user.getRole_utilisateur());
            }

            String idUtilisateur1 = users.get(0).getId_utilisateur();
            String idCoach = users.size() > 1 ? users.get(1).getId_utilisateur() : idUtilisateur1;
            String idUtilisateur2 = users.size() > 2 ? users.get(2).getId_utilisateur() : idUtilisateur1;

            traitement tr1 = new traitement();
            tr1.setDate_debut_traitement(new Date());
            tr1.setDate_fin_traitement(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)); // Dans 30 jours
            tr1.setObjectif_traitement(Objectif.GESTION_DU_STRESS);
            tr1.setDescription_traitement("Traitement pour gérer le stress lié au travail");
            tr1.setEtat_traitement(Etat.EN_COURS);
            tr1.setType_traitement(TypeTraitement.PSYCHOLOGIQUE);
            tr1.setId_utilisateur(idUtilisateur1);
            tr1.setId_coach(idCoach);

            ts.add(tr1);

            traitement tr2 = new traitement();
            tr2.setDate_debut_traitement(new Date(System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000)); // Il y a 15 jours
            tr2.setDate_fin_traitement(new Date(System.currentTimeMillis() + 45L * 24 * 60 * 60 * 1000)); // Dans 45 jours
            tr2.setObjectif_traitement(Objectif.ANXIETE);
            tr2.setDescription_traitement("Programme pour réduire l'anxiété sociale");
            tr2.setEtat_traitement(Etat.EN_COURS);
            tr2.setType_traitement(TypeTraitement.COMPORTEMENTAL);
            tr2.setId_utilisateur(idUtilisateur2);
            tr2.setId_coach(idCoach);

            ts.addMeth2(tr2);

            traitement tr3 = new traitement();
            tr3.setDate_debut_traitement(new Date(System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000)); // Il y a 60 jours
            tr3.setDate_fin_traitement(new Date(System.currentTimeMillis() - 10L * 24 * 60 * 60 * 1000)); // Il y a 10 jours
            tr3.setObjectif_traitement(Objectif.GESTION_DU_STRESS);
            tr3.setDescription_traitement("Traitement combiné pour stress et anxiété");
            tr3.setEtat_traitement(Etat.TERMINE);
            tr3.setType_traitement(TypeTraitement.MIXTE);
            tr3.setId_utilisateur(idUtilisateur1);
            tr3.setId_coach(idCoach);

            ts.addMeth2(tr3);

            List<traitement> traitements = ts.afficherList();

            if (traitements.isEmpty()) {
                System.out.println("Aucun traitement trouvé dans la base de données.");
            } else {
                System.out.println("Nombre de traitements : " + traitements.size());
                for (traitement tr : traitements) {
                    System.out.println("ID: " + tr.getId_traitement() +
                            " | Objectif: " + tr.getObjectif_traitement().getLibelle() +
                            " | État: " + tr.getEtat_traitement().getLibelle() +
                            " | Type: " + tr.getType_traitement().getLibelle() +
                            " | Utilisateur: " + tr.getId_utilisateur() +
                            " | Coach: " + tr.getId_coach());
                }
            }

            if (!traitements.isEmpty()) {
                traitement trToModify = traitements.get(0);

                System.out.println("Modification du traitement ID: " + trToModify.getId_traitement());

                trToModify.setDescription_traitement("Description mise à jour - Traitement intensif");
                trToModify.setEtat_traitement(Etat.SUSPENDU);
                trToModify.setType_traitement(TypeTraitement.MIXTE);
                trToModify.setDate_fin_traitement(new Date(System.currentTimeMillis() + 60L * 24 * 60 * 60 * 1000)); // Dans 60 jours

                ts.modifier(trToModify);

                traitements = ts.afficherList();
                for (traitement tr : traitements) {
                    System.out.println("ID: " + tr.getId_traitement() +
                            " | Objectif: " + tr.getObjectif_traitement().getLibelle() +
                            " | État: " + tr.getEtat_traitement().getLibelle() +
                            " | Type: " + tr.getType_traitement().getLibelle() +
                            " | Description: " + tr.getDescription_traitement());
                }
            }

            traitements = ts.afficherList();

            if (traitements.size() >= 2) {
                traitement trToDelete = traitements.get(1);
                System.out.println("Suppression du traitement ID: " + trToDelete.getId_traitement() +
                        " - " + trToDelete.getObjectif_traitement().getLibelle());

                ts.delete(trToDelete);

                traitements = ts.afficherList();
                System.out.println("Nombre de traitements restants : " + traitements.size());
                for (traitement tr : traitements) {
                    System.out.println("ID: " + tr.getId_traitement() +
                            " | Objectif: " + tr.getObjectif_traitement().getLibelle() +
                            " | État: " + tr.getEtat_traitement().getLibelle() +
                            " | Type: " + tr.getType_traitement().getLibelle());
                }
            } else {
                System.out.println("Pas assez de traitements pour effectuer une suppression.");
            }

        } catch (SQLException e) {
            System.out.println("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur générale: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
