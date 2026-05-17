import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.enums.typeL;
import tn.esprit.utils.MyDataBase;
import java.sql.SQLException;
import java.util.List;

public class main {
    public static void main(String[] args) {
        // Connexion à la base de données
        MyDataBase db = MyDataBase.getInstance();
        System.out.println("\n========== GESTION DES LOCAUX PSYCHIATRIQUES ==========\n");

        local_psychiatrie_SERVICE service = new local_psychiatrie_SERVICE();

        try {
            // 1. LISTE INITIALE
            System.out.println("--- Liste initiale des locaux ---");
            List<local_psychiatrie> locauxInitial = service.afficherList();
            System.out.println("Total : " + locauxInitial.size() + " local(aux)");
            for (local_psychiatrie local : locauxInitial) {
                System.out.println(local);
            }

            // 2. AJOUT DE 3 LOCAUX
            System.out.println("\n--- Ajout de 3 nouveaux locaux ---");

            // LOCAL 1 - Centre Serenity
            local_psychiatrie local1 = new local_psychiatrie();
            local1.setId_local(0);
            local1.setNom_local("Centre Serenity");
            local1.setAdresse_local("123 Rue de la Paix");
            local1.setVille_local("Tunis");
            local1.setDescription_local("Centre moderne pour soins psychiatriques");
            local1.setCapacite_local("50");
            local1.setType_local(typeL.HOPITAL);
            local1.setTelephone_local(71234567);
            local1.setEmail_local("contact@serenity.tn");
            local1.setDisponibilite_local("Disponible");
            local1.setImageURL("/images/serenity.jpg");
            service.add(local1);

            // LOCAL 2 - Clinique Espoir
            local_psychiatrie local2 = new local_psychiatrie();
            local2.setId_local(0);
            local2.setNom_local("Clinique Espoir");
            local2.setAdresse_local("456 Avenue de la Liberté");
            local2.setVille_local("Sfax");
            local2.setDescription_local("Clinique spécialisée en thérapie comportementale");
            local2.setCapacite_local("30");
            local2.setType_local(typeL.CLINIQUE_PSYCHIATRIQUE);
            local2.setTelephone_local(74567890);
            local2.setEmail_local("info@espoir.tn");
            local2.setDisponibilite_local("Disponible");
            local2.setImageURL("/images/espoir.jpg");
            service.add(local2);

            // LOCAL 3 - Cabinet Psy Harmonie
            local_psychiatrie local3 = new local_psychiatrie();
            local3.setId_local(0);
            local3.setNom_local("Cabinet Psy Harmonie");
            local3.setAdresse_local("789 Boulevard Habib Bourguiba");
            local3.setVille_local("Sousse");
            local3.setDescription_local("Cabinet privé spécialisé en psychothérapie");
            local3.setCapacite_local("15");
            local3.setType_local(typeL.CABINET_PRIVE);
            local3.setTelephone_local(73456789);
            local3.setEmail_local("contact@harmonie.tn");
            local3.setDisponibilite_local("Disponible");
            local3.setImageURL("/images/harmonie.jpg");
            service.add(local3);

            // 3. LISTE APRÈS AJOUT
            System.out.println("\n--- Liste après ajout ---");
            List<local_psychiatrie> locauxApresAjout = service.afficherList();
            System.out.println("Total : " + locauxApresAjout.size() + " local(aux)");
            for (local_psychiatrie local : locauxApresAjout) {
                System.out.println(local);
            }

            // 4. MODIFICATION D'UN LOCAL
            if (!locauxApresAjout.isEmpty()) {
                System.out.println("\n--- Modification du premier local ---");
                local_psychiatrie localAModifier = locauxApresAjout.get(0);

                System.out.println("=============Avant modification============== :");
                System.out.println(localAModifier);

                localAModifier.setNom_local("Centre Serenity Plus");
                localAModifier.setCapacite_local("60");
                localAModifier.setDisponibilite_local("Complet");

                service.modifier(localAModifier);

                System.out.println("==============Après modification===============:");
                System.out.println(localAModifier);

                // 5. LISTE APRÈS MODIFICATION
                System.out.println("\n--- Liste après modification ---");
                List<local_psychiatrie> locauxApresModif = service.afficherList();
                System.out.println("Total : " + locauxApresModif.size() + " local(aux)");
                for (local_psychiatrie local : locauxApresModif) {
                    System.out.println(local);
                }

                // 6. SUPPRESSION D'UN LOCAL
                if (!locauxApresModif.isEmpty()) {
                    System.out.println("\n--- Suppression d'un local ---");
                    local_psychiatrie localASupprimer = locauxApresModif.get(0);
                    System.out.println("Local à supprimer :");
                    System.out.println(localASupprimer);

                    service.delete(localASupprimer);

                    // 7. LISTE APRÈS SUPPRESSION
                    System.out.println("\n--- Liste après suppression ---");
                    List<local_psychiatrie> locauxApresSuppression = service.afficherList();
                    System.out.println("Total : " + locauxApresSuppression.size() + " local(aux)");
                    for (local_psychiatrie local : locauxApresSuppression) {
                        System.out.println(local);
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("ERREUR : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n========== FIN DU PROGRAMME ==========\n");
    }
}