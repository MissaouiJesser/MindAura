import org.junit.jupiter.api.*;
import tn.esprit.entities.Ressources;
import tn.esprit.services.RessourcesService;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RessourcesServicesTest {

    static RessourcesService service;
    static int idRessource;

    @BeforeAll
    static void setup() {
        service = new RessourcesService(
                tn.esprit.utils.MyDataBase.getInstance().getConx()
        );
    }

    @Test
    @Order(1)
    void ajouterCommentaireTest() throws SQLException {
        Ressources ressources = new Ressources(idRessource, "intelligence emotionnelle1 .png","L'intelligence émotionnelle", " Écoutez chaque jour pour bien démarrer votre journée ! Définir une intenti", Ressources.Contenu.Video,Ressources.CategorieArticle.Intelligence_emotionnelle, Arrays.asList("motivation","motivation matinal","lifestyle"),
                new Date(System.currentTimeMillis()), 100, 0, Ressources.Niveau.INTERMEDIAIRE, Ressources.StatusArticle.Publie, 10, "Comment l'intelligence émotionnelle peut transformer votre vie Avec Christophe Haag.mp4", "minyarguesmi87@gmail.com");

        service.add(ressources);
        List<Ressources> liste = service.afficherList();
        boolean exists = liste.stream().anyMatch(p->p.getTitre().equals("L'intelligence émotionnelle"));
        System.out.println(exists);
        idRessource = liste.get(liste.size() - 1).getId_ressources();
        assertTrue(exists);
        assertFalse(liste.isEmpty());
    }
    @Test
    @Order(2)
    void afficherListRessourcesTest() throws SQLException {

        List<Ressources> listes = service.afficherList();

        // 1. La liste ne doit pas être null
        assertNotNull(listes, "La méthode afficherList() doit retourner une liste, pas null.");

        // 2. La liste ne doit pas être vide après ajout
        assertFalse(listes.isEmpty(), "La liste ne doit pas être vide après insertion d’une ressource.");

        // 3. Vérifier que la ressource ajoutée dans le test 1 est bien présente
        boolean exists = listes.stream()
                .anyMatch(r -> r.getId_ressources() == idRessource);

        assertTrue(exists, "La ressource précédemment ajoutée doit apparaître dans afficherList().");
    }

/*
    @Test
    @Order(3)
    void modifierCommentaireTest() throws SQLException {

        Ressources updated = new Ressources(idRessource, "\"C:\\Users\\Admin\\Downloads\\Capture d’écran 2026-02-14 113721.png\"",
                "How to make the BEST of your 20's ", " How to make the BEST of your 20's | simple life advice I wish I had sooner", Ressources.Contenu.Video,Ressources.CategorieArticle.Motivation, Arrays.asList("motivation","motivation matinal","lifestyle"),
                new Date(System.currentTimeMillis()), 1000,
        0, Ressources.Niveau.INTERMEDIAIRE, Ressources.StatusArticle.Publie,
                25, "https://www.youtube.com/watch?v=4bTl3XV55EU", "minyarguesmi87@gmail.com"
        );

        service.modifier(updated);
        List<Ressources> listes = service.afficherList();
        boolean exists = listes.stream().anyMatch(p->p.getTitre().equals("MOTIVATION MATINALE"));
        System.out.println(exists);
        assertTrue(exists);
    }

    @Test
    @Order(4)
    void deleteRessourceTest() throws SQLException {

        // Ressource à supprimer (seulement ID nécessaire)
        Ressources toDelete = new Ressources(
                idRessource, null,
                null, null, null, null,
                null, null,
                0, 0, null, null,
                0, null, null
        );

        service.delete(toDelete);

        List<Ressources> listes = service.afficherList();

        boolean exists = listes.stream()
                .anyMatch(r -> r.getId_ressources() == idRessource);

        System.out.println("Ressource encore présente après delete ? " + exists);

        assertFalse(exists);
    }
*/
}
