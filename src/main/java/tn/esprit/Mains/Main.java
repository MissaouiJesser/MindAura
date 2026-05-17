package tn.esprit.mains;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import tn.esprit.services.utilisateurs_service;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Role;

public class Main {
    public static void main(String[] args) {
        utilisateurs_service us = new utilisateurs_service();

        try {
            utilisateurs user1 = new utilisateurs();
            user1.setNom_utilisateur("Laifi");
            user1.setPrenom_utilisateur("Azer");
            user1.setEmail_utilisateur("azer.laifi@esprit.tn");
            user1.setMdp_utilisateur("azer123");
            user1.setTelephone_utilisateur("+21629269297");
            user1.setDate_naissance_utilisateur(new Date());
            user1.setRole_utilisateur(Role.ROLE_ADMIN);
            user1.setPhoto_profil_utilisateur("azer.jpg");
            user1.setEst_actif_utilisateur(true);
            user1.setBio_utilisateur("Développeur");

            us.add(user1);

            utilisateurs user2 = new utilisateurs();
            user2.setNom_utilisateur("Chaffai");
            user2.setPrenom_utilisateur("Ichrak");
            user2.setEmail_utilisateur("ichrak.chaffai@esprit.tn");
            user2.setMdp_utilisateur("ichrak123");
            user2.setTelephone_utilisateur("+21699807906");
            user2.setDate_naissance_utilisateur(new Date(System.currentTimeMillis() - 30L * 365 * 24 * 60 * 60 * 1000)); // Il y a 30 ans
            user2.setRole_utilisateur(Role.ROLE_COACH);
            user2.setPhoto_profil_utilisateur("ichrak.jpg");
            user2.setEst_actif_utilisateur(true);
            user2.setBio_utilisateur("Coach en développement personnel");

            us.addMeth2(user2);

            List<utilisateurs> users = us.afficherList();

            if (users.isEmpty()) {
                System.out.println("Aucun utilisateur trouvé dans la base de données.");
            } else {
                System.out.println("Nombre d'utilisateurs : " + users.size());
                for (utilisateurs user : users) {
                    System.out.println("ID: " + user.getId_utilisateur() +
                            " | Nom: " + user.getNom_utilisateur() +
                            " " + user.getPrenom_utilisateur() +
                            " | Email: " + user.getEmail_utilisateur() +
                            " | Rôle: " + user.getRole_utilisateur());
                }
            }

            if (!users.isEmpty()) {
                utilisateurs userToModify = users.get(0);

                // Modifier ses informations
                userToModify.setNom_utilisateur("Laifi Modifié");
                userToModify.setPrenom_utilisateur("Azer Nouveau");
                userToModify.setEmail_utilisateur("azer.nouveau@esprit.tn");
                userToModify.setTelephone_utilisateur("+21699999999");
                userToModify.setBio_utilisateur("Développeur Senior - Profil mis à jour");
                userToModify.setRole_utilisateur(Role.ROLE_PSYCHOLOGUE);

                us.modifier(userToModify);

                users = us.afficherList();
                for (utilisateurs user : users) {
                    System.out.println("ID: " + user.getId_utilisateur() +
                            " | Nom: " + user.getNom_utilisateur() +
                            " " + user.getPrenom_utilisateur() +
                            " | Email: " + user.getEmail_utilisateur() +
                            " | Rôle: " + user.getRole_utilisateur() +
                            " | Bio: " + user.getBio_utilisateur());
                }
            }

            users = us.afficherList();

            if (users.size() >= 2) {
                utilisateurs userToDelete = users.get(1);
                System.out.println("Suppression de l'utilisateur: " + userToDelete.getNom_utilisateur() +
                        " " + userToDelete.getPrenom_utilisateur() +
                        " (ID: " + userToDelete.getId_utilisateur() + ")");

                us.delete(userToDelete);

                users = us.afficherList();
                System.out.println("Nombre d'utilisateurs restants : " + users.size());
                for (utilisateurs user : users) {
                    System.out.println("ID: " + user.getId_utilisateur() +
                            " | Nom: " + user.getNom_utilisateur() +
                            " " + user.getPrenom_utilisateur() +
                            " | Email: " + user.getEmail_utilisateur());
                }
            } else {
                System.out.println("Pas assez d'utilisateurs pour effectuer une suppression.");
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
