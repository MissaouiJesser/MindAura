import org.junit.jupiter.api.*;
import tn.esprit.enums.typeL;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.entities.local_psychiatrie;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceLocalTest {
    static local_psychiatrie_SERVICE ps;
    static int idLocal;

    @BeforeAll
    static void setup() {
        ps = new local_psychiatrie_SERVICE();
    }

    // ==================== TEST 1 : AJOUT ====================
    @Test
    @Order(1)
    @DisplayName("Test 1 : Ajout d'un local psychiatrique")
    void ajouterPersonneTest() throws SQLException {
        // CORRECTION: Utiliser le constructeur sans ID avec l'ordre correct des paramètres
        // Ordre: nom, adresse, ville, description, capacite, type, telephone, EMAIL, DISPONIBILITE, imageURL
        local_psychiatrie local = new local_psychiatrie(
                "Cabinet Psy Harmonie",
                "789 Boulevard Habib Bourguiba",
                "Sousse",
                "Cabinet privé spécialisé en psychothérapie",
                "15",
                typeL.CABINET_PRIVE,
                73456789,
                "contact@harmonie.tn",      // EMAIL en 8ème position
                "Disponible",                // DISPONIBILITE en 9ème position
                "/images/harmonie.jpg"
        );

        ps.add(local);

        // Assert - Vérifier que le local a été ajouté
        List<local_psychiatrie> locaux = ps.afficherList();

        assertFalse(locaux.isEmpty(), "La liste ne devrait pas être vide après l'ajout");

        boolean localAjoute = locaux.stream()
                .anyMatch(p -> p.getNom_local().equals("Cabinet Psy Harmonie"));

        assertTrue(localAjoute, "Le local 'Cabinet Psy Harmonie' devrait être dans la liste");

        // Récupérer l'ID du local ajouté
        idLocal = locaux.get(locaux.size() - 1).getId_local();

        System.out.println("✓ Local ajouté avec succès");
        System.out.println("  Nom : " + local.getNom_local());
        System.out.println("  Ville : " + local.getVille_local());
        System.out.println("  Total locaux en base : " + locaux.size());
    }

    // ==================== TEST 2 : AFFICHAGE ====================
    @Test
    @Order(2)
    @DisplayName("Test 2 : Affichage de la liste des locaux")
    void afficherListLocalTest() throws SQLException {
        System.out.println("\n--- Test 2 : Affichage de la liste ---");

        // Act - Récupérer la liste complète
        List<local_psychiatrie> locaux = ps.afficherList();

        // Assert - Vérifications
        assertNotNull(locaux, "La liste ne devrait pas être null");
        assertFalse(locaux.isEmpty(), "La liste ne devrait pas être vide");
        assertTrue(locaux.size() > 0, "Il devrait y avoir au moins 1 local");

        // Vérifier que le local de test est présent
        boolean localPresent = locaux.stream()
                .anyMatch(l -> l.getId_local() == idLocal);

        assertTrue(localPresent, "Le local de test devrait être dans la liste");

        // Afficher les informations
        System.out.println("✓ Liste affichée avec succès");
        System.out.println("  Total de locaux : " + locaux.size());
        System.out.println("  Liste des locaux :");

        for (local_psychiatrie local : locaux) {
            System.out.println("    - Nom: " + local.getNom_local() +
                    " | Ville: " + local.getVille_local() +
                    " | Type: " + local.getType_local().getLibelle());
        }
    }

    // ==================== TEST 3 : MODIFICATION ====================
    @Test
    @Order(3)
    @DisplayName("Test 3 : Modification d'un local psychiatrique")
    void modifierLocalTest() throws SQLException {
        System.out.println("\n--- Test 3 : Modification d'un local ---");

        // Arrange - Récupérer le local à modifier
        List<local_psychiatrie> locaux = ps.afficherList();
        local_psychiatrie localAModifier = locaux.stream()
                .filter(l -> l.getId_local() == idLocal)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le local à modifier n'a pas été trouvé"));

        System.out.println("Avant modification :");
        System.out.println("  Nom : " + localAModifier.getNom_local());
        System.out.println("  Capacité : " + localAModifier.getCapacite_local());
        System.out.println("  Disponibilité : " + localAModifier.getDisponibilite_local());
        System.out.println("  Email : " + localAModifier.getEmail_local());

        // Modifier les données
        localAModifier.setNom_local("Cabinet Psy Harmonie MODIFIÉ");
        localAModifier.setCapacite_local("25");
        localAModifier.setDisponibilite_local("Complet");
        localAModifier.setEmail_local("nouveau@harmonie.tn");
        localAModifier.setDescription_local("Description mise à jour après modification");

        // Act - Modifier le local dans la base de données
        ps.modifier(localAModifier);

        // Assert - Vérifier que les modifications ont été appliquées
        List<local_psychiatrie> locauxApres = ps.afficherList();
        local_psychiatrie localModifie = locauxApres.stream()
                .filter(l -> l.getId_local() == idLocal)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le local modifié n'a pas été trouvé"));

        assertEquals("Cabinet Psy Harmonie MODIFIÉ", localModifie.getNom_local(),
                "Le nom devrait être modifié");
        assertEquals("25", localModifie.getCapacite_local(),
                "La capacité devrait être modifiée");
        assertEquals("Complet", localModifie.getDisponibilite_local(),
                "La disponibilité devrait être modifiée");
        assertEquals("nouveau@harmonie.tn", localModifie.getEmail_local(),
                "L'email devrait être modifié");
        assertEquals("Description mise à jour après modification", localModifie.getDescription_local(),
                "La description devrait être modifiée");

        // Vérifier que les données non modifiées restent identiques
        assertEquals("Sousse", localModifie.getVille_local(),
                "La ville ne devrait pas changer");
        assertEquals(typeL.CABINET_PRIVE, localModifie.getType_local(),
                "Le type ne devrait pas changer");

        System.out.println("\n✓ Local modifié avec succès");
        System.out.println("Après modification :");
        System.out.println("  Nom : " + localModifie.getNom_local());
        System.out.println("  Capacité : " + localModifie.getCapacite_local());
        System.out.println("  Disponibilité : " + localModifie.getDisponibilite_local());
        System.out.println("  Email : " + localModifie.getEmail_local());
    }

    // ==================== TEST 4 : SUPPRESSION ====================
    @Test
    @Order(4)
    @DisplayName("Test 4 : Suppression d'un local psychiatrique")
    void supprimerLocalTest() throws SQLException {
        System.out.println("\n--- Test 4 : Suppression d'un local ---");

        // Arrange - Récupérer le nombre de locaux avant suppression
        List<local_psychiatrie> locauxAvant = ps.afficherList();
        int nombreAvant = locauxAvant.size();

        // Récupérer le local à supprimer
        local_psychiatrie localASupprimer = locauxAvant.stream()
                .filter(l -> l.getId_local() == idLocal)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Le local à supprimer n'a pas été trouvé"));

        System.out.println("Local à supprimer :");
        System.out.println("  Nom : " + localASupprimer.getNom_local());
        System.out.println("  Ville : " + localASupprimer.getVille_local());
        System.out.println("  Nombre de locaux avant suppression : " + nombreAvant);

        // Act - Supprimer le local
        ps.delete(localASupprimer);

        // Assert - Vérifier que le local a été supprimé
        List<local_psychiatrie> locauxApres = ps.afficherList();
        int nombreApres = locauxApres.size();

        // Vérifier que le nombre de locaux a diminué
        assertEquals(nombreAvant - 1, nombreApres,
                "Le nombre de locaux devrait diminuer de 1");

        // Vérifier que le local n'existe plus dans la liste
        boolean localExiste = locauxApres.stream()
                .anyMatch(l -> l.getId_local() == idLocal);

        assertFalse(localExiste, "Le local ne devrait plus être dans la liste");

        System.out.println("\n✓ Local supprimé avec succès");
        System.out.println("  Nombre de locaux après suppression : " + nombreApres);
        System.out.println("  Différence : " + (nombreAvant - nombreApres) + " local(aux) supprimé(s)");
    }
}