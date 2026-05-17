import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Role;
import tn.esprit.services.utilisateurs_service;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceUtilisateursTest {

    static utilisateurs_service userService;
    static String idUtilisateur;

    @BeforeAll
    static void setup() {
        userService = new utilisateurs_service();
    }

    @Test
    @Order(1)
    @DisplayName("Test d'ajout d'un utilisateur")
    void ajouterUtilisateurTest() throws SQLException {
        utilisateurs user = new utilisateurs();
        user.setNom_utilisateur("Doe");
        user.setPrenom_utilisateur("John");
        user.setEmail_utilisateur("john.doe@test.com");
        user.setMdp_utilisateur("password123");
        user.setTelephone_utilisateur("12345678");
        user.setDate_naissance_utilisateur(new Date());
        user.setRole_utilisateur(Role.ROLE_PATIENT);
        user.setPhoto_profil_utilisateur("images/default.jpg");
        user.setEst_actif_utilisateur(true);
        user.setBio_utilisateur("Utilisateur de test");

        userService.addMeth2(user);
        
        List<utilisateurs> utilisateurs = userService.afficherList();
        boolean check = utilisateurs.stream()
                .anyMatch(u -> u.getEmail_utilisateur().equals("john.doe@test.com"));
        
        System.out.println("Utilisateur ajouté: " + check);
        idUtilisateur = utilisateurs.get(utilisateurs.size() - 1).getId_utilisateur();
        
        assertTrue(check, "L'utilisateur devrait être ajouté");
        assertFalse(utilisateurs.isEmpty(), "La liste ne devrait pas être vide");
    }

    @Test
    @Order(2)
    @DisplayName("Test de modification d'un utilisateur")
    void modifierUtilisateurTest() throws SQLException {
        utilisateurs user = new utilisateurs();
        user.setId_utilisateur(idUtilisateur);
        user.setNom_utilisateur("Ben Ahmed");
        user.setPrenom_utilisateur("Ahmed");
        user.setEmail_utilisateur("ahmed.benahmed@test.com");
        user.setMdp_utilisateur("newpassword456");
        user.setTelephone_utilisateur("87654321");
        user.setDate_naissance_utilisateur(new Date());
        user.setRole_utilisateur(Role.ROLE_COACH);
        user.setPhoto_profil_utilisateur("images/ahmed.jpg");
        user.setEst_actif_utilisateur(true);
        user.setBio_utilisateur("Coach sportif");

        userService.modifier(user);
        
        List<utilisateurs> utilisateurs = userService.afficherList();
        boolean check = utilisateurs.stream()
                .anyMatch(u -> u.getNom_utilisateur().equals("Ben Ahmed") 
                        && u.getId_utilisateur().equals(idUtilisateur));
        
        System.out.println("Utilisateur modifié: " + check);
        assertTrue(check, "L'utilisateur devrait être modifié");
    }

    @Test
    @Order(3)
    @DisplayName("Test de recherche d'utilisateurs")
    void rechercherUtilisateurTest() throws SQLException {
        List<utilisateurs> resultats = userService.rechercher("Ahmed");
        
        assertFalse(resultats.isEmpty(), "La recherche devrait retourner des résultats");
        boolean check = resultats.stream()
                .anyMatch(u -> u.getNom_utilisateur().contains("Ahmed") 
                        || u.getPrenom_utilisateur().contains("Ahmed"));
        
        System.out.println("Résultats de recherche trouvés: " + check);
        assertTrue(check, "Les résultats devraient contenir 'Ahmed'");
    }

    @Test
    @Order(4)
    @DisplayName("Test d'affichage de la liste des utilisateurs")
    void afficherListeUtilisateursTest() throws SQLException {
        List<utilisateurs> utilisateurs = userService.afficherList();
        
        assertNotNull(utilisateurs, "La liste ne devrait pas être null");
        assertFalse(utilisateurs.isEmpty(), "La liste ne devrait pas être vide");
        
        System.out.println("Nombre d'utilisateurs: " + utilisateurs.size());
        
        // Vérifier que chaque utilisateur a les champs requis
        for (utilisateurs user : utilisateurs) {
            assertNotNull(user.getId_utilisateur(), "L'ID ne devrait pas être null");
            assertNotNull(user.getEmail_utilisateur(), "L'email ne devrait pas être null");
            assertNotNull(user.getRole_utilisateur(), "Le rôle ne devrait pas être null");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test de vérification du rôle utilisateur")
    void verifierRoleUtilisateurTest() throws SQLException {
        List<utilisateurs> utilisateurs = userService.afficherList();
        utilisateurs user = utilisateurs.stream()
                .filter(u -> u.getId_utilisateur().equals(idUtilisateur))
                .findFirst()
                .orElse(null);
        
        assertNotNull(user, "L'utilisateur devrait exister");
        assertEquals(Role.ROLE_COACH, user.getRole_utilisateur(), 
                "Le rôle devrait être ROLE_COACH");
    }

    @Test
    @Order(6)
    @DisplayName("Test de suppression d'un utilisateur")
    void supprimerUtilisateurTest() throws SQLException {
        utilisateurs user = new utilisateurs();
        user.setId_utilisateur(idUtilisateur);
        
        userService.delete(user);
        
        List<utilisateurs> utilisateurs = userService.afficherList();
        boolean check = utilisateurs.stream()
                .noneMatch(u -> u.getId_utilisateur().equals(idUtilisateur));
        
        System.out.println("Utilisateur supprimé: " + check);
        assertTrue(check, "L'utilisateur devrait être supprimé");
    }
}
