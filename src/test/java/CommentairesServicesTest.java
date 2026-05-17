import org.junit.jupiter.api.*;
import tn.esprit.entities.Commentaires;
import tn.esprit.services.CommentairesServices;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CommentairesServicesTest {

    static CommentairesServices service;
    static int idCommentaire;

    @BeforeAll
    static void setup() {
        service = new CommentairesServices(
                tn.esprit.utils.MyDataBase.getInstance().getConx()
        );
    }

    @Test
    @Order(1)
    void ajouterCommentaireTest() throws SQLException {
        Commentaires com = new Commentaires(
                idCommentaire,
                1,         // idArticle
                2,         // idUser
                "minyar Guesmi",
                "Ceci est un commentaire de test !",
                new Date(System.currentTimeMillis()),
                0,
                0,
                false,
                Commentaires.StatusCommentaires.PUBLIE
        );
        service.add(com);
        List<Commentaires> liste = service.afficherList();
        boolean exists = liste.stream()
                .anyMatch(c -> c.getContenu().equals("Ceci est un commentaire de test !"));
        System.out.println(exists);
        idCommentaire = liste.get(liste.size() - 1).getId_commentaires();
        assertTrue(exists);
        assertFalse(liste.isEmpty());
    }
    @Test
    @Order(2)
    void afficherListTest() throws SQLException {

        List<Commentaires> listes = service.afficherList();

        assertNotNull(listes, "La méthode afficherList doit retourner une liste et non null.");
        assertFalse(listes.isEmpty(), "La liste ne doit pas être vide après insertion d’un commentaire.");

        // Vérifier présence du commentaire ajouté dans le test 1
        boolean exists = listes.stream()
                .anyMatch(c -> c.getId_commentaires() == idCommentaire);

        assertTrue(exists, "Le commentaire précédemment ajouté doit apparaître dans afficherList().");
    }

    @Test
    @Order(3)
    void modifierCommentaireTest() throws SQLException {

        Commentaires updated = new Commentaires(
                idCommentaire,
                1,
                2,
                "ahlem",
                "Commentaire modifié !",
                new Date(System.currentTimeMillis()),
                10,
                0,
                false,
                Commentaires.StatusCommentaires.ARCHIVE
        );

        service.modifier(updated);
        List<Commentaires> listes = service.afficherList();
        boolean exists = listes.stream()
                .anyMatch(c -> c.getId_commentaires() == idCommentaire &&
                        c.getContenu().equals("Commentaire modifié !"));
        System.out.println(exists);
        assertTrue(exists);
    }
    @Test
    @Order(4)
    void deleteCommentaireTest() throws SQLException {

        // Seul l'ID est utile pour supprimer
        Commentaires toDelete = new Commentaires(
                idCommentaire,  // ✔ utiliser l'ID ajouté précédemment !
                0,
                0,
                null,
                null,
                null,
                0,
                0,
                false,
                null
        );

        service.delete(toDelete);

        List<Commentaires> listes = service.afficherList();

        boolean exists = listes.stream()
                .anyMatch(c -> c.getId_commentaires() == idCommentaire);

        System.out.println("Commentaire encore présent après delete ? " + exists);

        assertFalse(exists);
    }

}




