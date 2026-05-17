package tn.esprit.services;

import tn.esprit.entities.ReponseClient;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseClientService implements ICRUD<ReponseClient> {

    private Connection        conx;
    private Statement         stm;
    private PreparedStatement pstm;

    public ReponseClientService() {
        conx = MyDataBase.getInstance().getConx();
    }

    // =========================================================================
    //  ADD — concaténation (méthode 1, non sécurisée — préférer addMeth2)
    // =========================================================================
    @Override
    public void add(ReponseClient rc) throws SQLException {
        // ✅ Colonnes réelles : id_test_id, id_question_reponse_id, utilisateur_id
        String req = "INSERT INTO `reponse_client` " +
                "(`utilisateur_id`, `option_choisie`, `score_obtenu`, " +
                " `reponse_texte_libre`, `date_reponse`, `id_test_id`, `id_question_reponse_id`) " +
                "VALUES (" +
                rc.getUtilisateurId() + ",'" +
                rc.getOptionChoisie() + "'," +
                rc.getScoreObtenu() + ",'" +
                rc.getReponseTexteLibre() + "','" +
                rc.getDateReponse() + "'," +
                rc.getIdTestId() + "," +
                rc.getIdQuestionReponseId() + ")";

        stm = conx.createStatement();
        stm.executeUpdate(req);
        System.out.println("Réponse client ajoutée");
    }

    // =========================================================================
    //  ADD — PreparedStatement (méthode 2, sécurisée ✅)
    // =========================================================================
    @Override
    public void addMeth2(ReponseClient rc) throws SQLException {
        // ✅ Colonnes réelles : utilisateur_id (int), id_test_id (FK), id_question_reponse_id (FK)
        String req = "INSERT INTO `reponse_client` " +
                "(`utilisateur_id`, `option_choisie`, `score_obtenu`, " +
                " `reponse_texte_libre`, `date_reponse`, `id_test_id`, `id_question_reponse_id`) " +
                "VALUES (?,?,?,?,?,?,?)";

        pstm = conx.prepareStatement(req);
        // ✅ utilisateur_id est int(11) — setInt ou setNull
        if (rc.getUtilisateurId() != 0) {
            pstm.setInt(1, rc.getUtilisateurId());
        } else {
            pstm.setNull(1, Types.INTEGER);
        }
        pstm.setString   (2, rc.getOptionChoisie());
        pstm.setInt      (3, rc.getScoreObtenu());
        pstm.setString   (4, rc.getReponseTexteLibre());
        pstm.setTimestamp(5, rc.getDateReponse() != null
                ? rc.getDateReponse()
                : new Timestamp(System.currentTimeMillis()));
        pstm.setInt      (6, rc.getIdTestId());
        pstm.setInt      (7, rc.getIdQuestionReponseId());

        pstm.executeUpdate();
        System.out.println("Réponse client ajoutée (méthode 2)");
    }

    // =========================================================================
    //  MODIFIER
    // =========================================================================
    @Override
    public void modifier(ReponseClient rc) throws SQLException {
        String req = "UPDATE `reponse_client` SET " +
                "`utilisateur_id`=?, `option_choisie`=?, `score_obtenu`=?, " +
                "`reponse_texte_libre`=?, `id_test_id`=?, `id_question_reponse_id`=? " +
                "WHERE `id_reponse_client`=?";

        pstm = conx.prepareStatement(req);
        pstm.setInt   (1, rc.getUtilisateurId());
        pstm.setString(2, rc.getOptionChoisie());
        pstm.setInt   (3, rc.getScoreObtenu());
        pstm.setString(4, rc.getReponseTexteLibre());
        pstm.setInt   (5, rc.getIdTestId());
        pstm.setInt   (6, rc.getIdQuestionReponseId());
        pstm.setInt   (7, rc.getIdReponseClient());

        pstm.executeUpdate();
        System.out.println("Réponse client modifiée avec ID = " + rc.getIdReponseClient());
    }

    // =========================================================================
    //  DELETE
    // =========================================================================
    @Override
    public void delete(ReponseClient rc) throws SQLException {
        String req = "DELETE FROM `reponse_client` WHERE `id_reponse_client`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, rc.getIdReponseClient());
        pstm.executeUpdate();
        System.out.println("Réponse client supprimée avec ID = " + rc.getIdReponseClient());
    }

    // =========================================================================
    //  AFFICHER LISTE
    // =========================================================================
    @Override
    public List<ReponseClient> afficherList() throws SQLException {
        String req = "SELECT * FROM `reponse_client` ORDER BY `date_reponse` DESC";
        stm = conx.createStatement();
        return mapResultSet(stm.executeQuery(req));
    }

    // =========================================================================
    //  RÉPONSES PAR TEST
    // =========================================================================
    public List<ReponseClient> getReponsesByTestId(int idTest) throws SQLException {
        // ✅ colonne FK réelle : id_test_id
        String req = "SELECT * FROM `reponse_client` WHERE `id_test_id`=? ORDER BY `date_reponse` DESC";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        return mapResultSet(pstm.executeQuery());
    }

    // =========================================================================
    //  SCORE TOTAL PAR TEST + UTILISATEUR
    // =========================================================================
    public int calculerScoreTotal(int idTest, int idUtilisateur) throws SQLException {
        // ✅ utilisateur_id est int, id_test_id est la FK réelle
        String req = "SELECT SUM(`score_obtenu`) AS total FROM `reponse_client` " +
                "WHERE `id_test_id`=? AND `utilisateur_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        pstm.setInt(2, idUtilisateur);
        ResultSet res = pstm.executeQuery();
        return res.next() ? res.getInt("total") : 0;
    }

    // =========================================================================
    //  SCORE TOTAL PAR TEST (sans filtre utilisateur)
    // =========================================================================
    public int calculerScoreTotalTest(int idTest) throws SQLException {
        String req = "SELECT SUM(`score_obtenu`) AS total FROM `reponse_client` WHERE `id_test_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        ResultSet res = pstm.executeQuery();
        return res.next() ? res.getInt("total") : 0;
    }

    // =========================================================================
    //  SUPPRIMER TOUTES LES RÉPONSES D'UN TEST
    // =========================================================================
    public void deleteReponsesByTestId(int idTest) throws SQLException {
        String req = "DELETE FROM `reponse_client` WHERE `id_test_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        pstm.executeUpdate();
        System.out.println("Toutes les réponses du test " + idTest + " ont été supprimées");
    }

    // =========================================================================
    //  VÉRIFIER SI UN UTILISATEUR A DÉJÀ RÉPONDU À UN TEST
    // =========================================================================
    public boolean hasUserAnswered(int idTest, int idUtilisateur) throws SQLException {
        String req = "SELECT COUNT(*) AS total FROM `reponse_client` " +
                "WHERE `id_test_id`=? AND `utilisateur_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        pstm.setInt(2, idUtilisateur);
        ResultSet res = pstm.executeQuery();
        return res.next() && res.getInt("total") > 0;
    }

    // =========================================================================
    //  RÉPONSE D'UNE QUESTION SPÉCIFIQUE POUR UN UTILISATEUR
    // =========================================================================
    public ReponseClient getReponseByQuestionId(int idQuestionReponse, int idUtilisateur) throws SQLException {
        // ✅ colonne FK réelle : id_question_reponse_id, utilisateur_id est int
        String req = "SELECT * FROM `reponse_client` " +
                "WHERE `id_question_reponse_id`=? AND `utilisateur_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idQuestionReponse);
        pstm.setInt(2, idUtilisateur);
        List<ReponseClient> list = mapResultSet(pstm.executeQuery());
        return list.isEmpty() ? null : list.get(0);
    }

    // =========================================================================
    //  HELPER PRIVÉ — mappage ResultSet → ReponseClient
    //  Colonnes DB réelles : utilisateur_id (int), id_test_id (FK), id_question_reponse_id (FK)
    // =========================================================================
    private List<ReponseClient> mapResultSet(ResultSet res) throws SQLException {
        List<ReponseClient> reponses = new ArrayList<>();
        while (res.next()) {
            ReponseClient rc = new ReponseClient(
                    res.getInt      ("id_reponse_client"),
                    res.getInt      ("utilisateur_id"),          // ✅ int, pas String
                    res.getString   ("option_choisie"),
                    res.getInt      ("score_obtenu"),
                    res.getString   ("reponse_texte_libre"),
                    res.getTimestamp("date_reponse"),
                    res.getInt      ("id_test_id"),              // ✅ FK réelle
                    res.getInt      ("id_question_reponse_id")  // ✅ FK réelle
            );
            reponses.add(rc);
        }
        return reponses;
    }
}