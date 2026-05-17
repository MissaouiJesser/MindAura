import tn.esprit.entities.traitement;
import tn.esprit.enums.Etat;
import tn.esprit.enums.Objectif;
import tn.esprit.enums.TypeTraitement;
import tn.esprit.services.traitement_service;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceTraitementTest {

    static traitement_service traitementService;
    static String idTraitement;

    @BeforeAll
    static void setup() {
        traitementService = new traitement_service();
    }

    @Test
    @Order(1)
    @DisplayName("Test d'ajout d'un traitement")
    void ajouterTraitementTest() throws SQLException {
        traitement tr = new traitement();
        tr.setDate_debut_traitement(new Date());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 3);
        tr.setDate_fin_traitement(cal.getTime());

        tr.setObjectif_traitement(Objectif.GESTION_DU_STRESS);
        tr.setDescription_traitement("Traitement pour gérer le stress quotidien");
        tr.setEtat_traitement(Etat.EN_COURS);
        tr.setType_traitement(TypeTraitement.PSYCHOLOGIQUE);
        tr.setId_utilisateur("5");
        tr.setId_coach("4");

        traitementService.addMeth2(tr);

        List<traitement> traitements = traitementService.afficherList();
        boolean check = traitements.stream()
                .anyMatch(t -> t.getDescription_traitement() != null
                        && t.getDescription_traitement().contains("stress quotidien"));

        System.out.println("Traitement ajouté: " + check);
        idTraitement = traitements.get(traitements.size() - 1).getId_traitement();

        assertTrue(check, "Le traitement devrait être ajouté");
        assertFalse(traitements.isEmpty(), "La liste ne devrait pas être vide");
    }

    @Test
    @Order(2)
    @DisplayName("Test de modification d'un traitement")
    void modifierTraitementTest() throws SQLException {
        traitement tr = new traitement();
        tr.setId_traitement(idTraitement);
        tr.setDate_debut_traitement(new Date());

        // Date de fin dans 6 mois
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 6);
        tr.setDate_fin_traitement(cal.getTime());

        tr.setObjectif_traitement(Objectif.ANXIETE);
        tr.setDescription_traitement("Traitement modifié pour anxiété");
        tr.setEtat_traitement(Etat.EN_COURS);
        tr.setType_traitement(TypeTraitement.MIXTE);
        tr.setId_utilisateur("5");
        tr.setId_coach("4");

        traitementService.modifier(tr);

        List<traitement> traitements = traitementService.afficherList();
        boolean check = traitements.stream()
                .anyMatch(t -> t.getId_traitement().equals(idTraitement)
                        && t.getObjectif_traitement() == Objectif.ANXIETE);

        System.out.println("Traitement modifié: " + check);
        assertTrue(check, "Le traitement devrait être modifié");
    }

    @Test
    @Order(3)
    @DisplayName("Test de vérification de l'état du traitement")
    void verifierEtatTraitementTest() throws SQLException {
        List<traitement> traitements = traitementService.afficherList();
        traitement tr = traitements.stream()
                .filter(t -> t.getId_traitement().equals(idTraitement))
                .findFirst()
                .orElse(null);

        assertNotNull(tr, "Le traitement devrait exister");
        assertEquals(Etat.EN_COURS, tr.getEtat_traitement(),
                "L'état devrait être EN_COURS");
        assertEquals(TypeTraitement.MIXTE, tr.getType_traitement(),
                "Le type devrait être MIXTE");
    }

    @Test
    @Order(4)
    @DisplayName("Test d'affichage de la liste des traitements")
    void afficherListeTraitementsTest() throws SQLException {
        List<traitement> traitements = traitementService.afficherList();

        assertNotNull(traitements, "La liste ne devrait pas être null");
        assertFalse(traitements.isEmpty(), "La liste ne devrait pas être vide");

        System.out.println("Nombre de traitements: " + traitements.size());

        // Vérifier que chaque traitement a les champs requis
        for (traitement tr : traitements) {
            assertNotNull(tr.getId_traitement(), "L'ID ne devrait pas être null");
            assertNotNull(tr.getDate_debut_traitement(), "La date de début ne devrait pas être null");
            assertNotNull(tr.getEtat_traitement(), "L'état ne devrait pas être null");
            assertNotNull(tr.getObjectif_traitement(), "L'objectif ne devrait pas être null");
            assertNotNull(tr.getType_traitement(), "Le type ne devrait pas être null");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test de changement d'état du traitement")
    void changerEtatTraitementTest() throws SQLException {
        traitement tr = new traitement();
        tr.setId_traitement(idTraitement);
        tr.setDate_debut_traitement(new Date());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 6);
        tr.setDate_fin_traitement(cal.getTime());

        tr.setObjectif_traitement(Objectif.ANXIETE);
        tr.setDescription_traitement("Traitement modifié pour anxiété");
        tr.setEtat_traitement(Etat.TERMINE);
        tr.setType_traitement(TypeTraitement.MIXTE);
        tr.setId_utilisateur("5");
        tr.setId_coach("4");

        traitementService.modifier(tr);

        List<traitement> traitements = traitementService.afficherList();
        traitement traitementModifie = traitements.stream()
                .filter(t -> t.getId_traitement().equals(idTraitement))
                .findFirst()
                .orElse(null);

        assertNotNull(traitementModifie, "Le traitement devrait exister");
        assertEquals(Etat.TERMINE, traitementModifie.getEtat_traitement(),
                "L'état devrait être TERMINE");

        System.out.println("État changé à TERMINE: " +
                (traitementModifie.getEtat_traitement() == Etat.TERMINE));
    }

    @Test
    @Order(6)
    @DisplayName("Test de vérification des dates du traitement")
    void verifierDatesTraitementTest() throws SQLException {
        List<traitement> traitements = traitementService.afficherList();
        traitement tr = traitements.stream()
                .filter(t -> t.getId_traitement().equals(idTraitement))
                .findFirst()
                .orElse(null);

        assertNotNull(tr, "Le traitement devrait exister");
        assertNotNull(tr.getDate_debut_traitement(), "La date de début ne devrait pas être null");
        assertNotNull(tr.getDate_fin_traitement(), "La date de fin ne devrait pas être null");

        // Vérifier que la date de fin est après la date de début
        assertTrue(tr.getDate_fin_traitement().after(tr.getDate_debut_traitement()),
                "La date de fin devrait être après la date de début");

        System.out.println("Dates valides: " +
                tr.getDate_fin_traitement().after(tr.getDate_debut_traitement()));
    }

    @Test
    @Order(7)
    @DisplayName("Test de suppression d'un traitement")
    void supprimerTraitementTest() throws SQLException {
        traitement tr = new traitement();
        tr.setId_traitement(idTraitement);

        traitementService.delete(tr);

        List<traitement> traitements = traitementService.afficherList();
        boolean check = traitements.stream()
                .noneMatch(t -> t.getId_traitement().equals(idTraitement));

        System.out.println("Traitement supprimé: " + check);
        assertTrue(check, "Le traitement devrait être supprimé");
    }
}