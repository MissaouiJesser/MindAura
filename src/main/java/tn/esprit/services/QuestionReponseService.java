package tn.esprit.services;

import tn.esprit.entities.QuestionReponse;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionReponseService implements ICRUD<QuestionReponse> {

    private Connection     conx;
    private Statement      stm;
    private PreparedStatement pstm;

    public QuestionReponseService() {
        conx = MyDataBase.getInstance().getConx();
    }

    // =========================================================================
    //  ADD — concaténation (méthode 1, non sécurisée — préférer addMeth2)
    // =========================================================================
    @Override
    public void add(QuestionReponse qr) throws SQLException {
        // ⚠️ Colonnes réelles : id_test_id, option1..option5, score1..score5
        String req = "INSERT INTO `question_reponse` " +
                "(`id_test_id`, `texte_question`, `type_question`, `ordre_question`, " +
                " `option1`, `score1`, `option2`, `score2`, " +
                " `option3`, `score3`, `option4`, `score4`, " +
                " `option5`, `score5`, `est_obligatoire`) " +
                "VALUES (" +
                qr.getIdTestId() + ",'" +
                qr.getTexteQuestion() + "','" +
                qr.getTypeQuestion() + "'," +
                qr.getOrdreQuestion() + ",'" +
                qr.getOption1() + "'," + qr.getScore1() + ",'" +
                qr.getOption2() + "'," + qr.getScore2() + ",'" +
                qr.getOption3() + "'," + qr.getScore3() + ",'" +
                qr.getOption4() + "'," + qr.getScore4() + ",'" +
                qr.getOption5() + "'," + qr.getScore5() + "," +
                qr.isEstObligatoire() + ")";

        stm = conx.createStatement();
        stm.executeUpdate(req);
        System.out.println("Question-Réponse ajoutée");
    }

    // =========================================================================
    //  ADD — PreparedStatement (méthode 2, sécurisée ✅)
    // =========================================================================
    @Override
    public void addMeth2(QuestionReponse qr) throws SQLException {
        String req = "INSERT INTO `question_reponse` " +
                "(`id_test_id`, `texte_question`, `type_question`, `ordre_question`, " +
                " `option1`, `score1`, `option2`, `score2`, " +
                " `option3`, `score3`, `option4`, `score4`, " +
                " `option5`, `score5`, `est_obligatoire`) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        pstm = conx.prepareStatement(req);
        pstm.setInt    (1,  qr.getIdTestId());
        pstm.setString (2,  qr.getTexteQuestion());
        pstm.setString (3,  qr.getTypeQuestion());
        pstm.setInt    (4,  qr.getOrdreQuestion());
        pstm.setString (5,  qr.getOption1());
        pstm.setInt    (6,  qr.getScore1());
        pstm.setString (7,  qr.getOption2());
        pstm.setInt    (8,  qr.getScore2());
        pstm.setString (9,  qr.getOption3());
        pstm.setInt    (10, qr.getScore3());
        pstm.setString (11, qr.getOption4());
        pstm.setInt    (12, qr.getScore4());
        pstm.setString (13, qr.getOption5());
        pstm.setInt    (14, qr.getScore5());
        pstm.setBoolean(15, qr.isEstObligatoire());

        pstm.executeUpdate();
        System.out.println("Question-Réponse ajoutée (méthode 2)");
    }

    // =========================================================================
    //  MODIFIER
    // =========================================================================
    @Override
    public void modifier(QuestionReponse qr) throws SQLException {
        String req = "UPDATE `question_reponse` SET " +
                "`id_test_id`=?, `texte_question`=?, `type_question`=?, `ordre_question`=?, " +
                "`option1`=?, `score1`=?, `option2`=?, `score2`=?, " +
                "`option3`=?, `score3`=?, `option4`=?, `score4`=?, " +
                "`option5`=?, `score5`=?, `est_obligatoire`=? " +
                "WHERE `id_question_reponse`=?";

        pstm = conx.prepareStatement(req);
        pstm.setInt    (1,  qr.getIdTestId());
        pstm.setString (2,  qr.getTexteQuestion());
        pstm.setString (3,  qr.getTypeQuestion());
        pstm.setInt    (4,  qr.getOrdreQuestion());
        pstm.setString (5,  qr.getOption1());
        pstm.setInt    (6,  qr.getScore1());
        pstm.setString (7,  qr.getOption2());
        pstm.setInt    (8,  qr.getScore2());
        pstm.setString (9,  qr.getOption3());
        pstm.setInt    (10, qr.getScore3());
        pstm.setString (11, qr.getOption4());
        pstm.setInt    (12, qr.getScore4());
        pstm.setString (13, qr.getOption5());
        pstm.setInt    (14, qr.getScore5());
        pstm.setBoolean(15, qr.isEstObligatoire());
        pstm.setInt    (16, qr.getIdQuestionReponse());

        pstm.executeUpdate();
        System.out.println("Question-Réponse modifiée avec ID = " + qr.getIdQuestionReponse());
    }

    // =========================================================================
    //  DELETE
    // =========================================================================
    @Override
    public void delete(QuestionReponse qr) throws SQLException {
        String req = "DELETE FROM `question_reponse` WHERE `id_question_reponse`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, qr.getIdQuestionReponse());
        pstm.executeUpdate();
        System.out.println("Question-Réponse supprimée avec ID = " + qr.getIdQuestionReponse());
    }

    // =========================================================================
    //  AFFICHER LISTE — toutes les questions
    // =========================================================================
    @Override
    public List<QuestionReponse> afficherList() throws SQLException {
        String req = "SELECT * FROM `question_reponse` ORDER BY `ordre_question`";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);
        return mapResultSet(res);
    }

    // =========================================================================
    //  QUESTIONS PAR TEST
    // =========================================================================
    public List<QuestionReponse> getQuestionsByTestId(int idTest) throws SQLException {
        String req = "SELECT * FROM `question_reponse` WHERE `id_test_id`=? ORDER BY `ordre_question`";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        return mapResultSet(pstm.executeQuery());
    }

    // =========================================================================
    //  QUESTION PAR ID
    // =========================================================================
    public QuestionReponse getQuestionById(int idQuestionReponse) throws SQLException {
        String req = "SELECT * FROM `question_reponse` WHERE `id_question_reponse`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idQuestionReponse);
        ResultSet res = pstm.executeQuery();
        List<QuestionReponse> list = mapResultSet(res);
        return list.isEmpty() ? null : list.get(0);
    }

    // =========================================================================
    //  COMPTER LES QUESTIONS D'UN TEST
    // =========================================================================
    public int countQuestionsByTest(int idTest) throws SQLException {
        String req = "SELECT COUNT(*) AS total FROM `question_reponse` WHERE `id_test_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        ResultSet res = pstm.executeQuery();
        return res.next() ? res.getInt("total") : 0;
    }

    // =========================================================================
    //  SUPPRIMER TOUTES LES QUESTIONS D'UN TEST
    // =========================================================================
    public void deleteQuestionsByTestId(int idTest) throws SQLException {
        String req = "DELETE FROM `question_reponse` WHERE `id_test_id`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        pstm.executeUpdate();
        System.out.println("Toutes les questions du test " + idTest + " ont été supprimées");
    }

    // =========================================================================
    //  HELPER PRIVÉ — mappage ResultSet → QuestionReponse
    //  Colonnes DB réelles : option1..option5, score1..score5, id_test_id
    // =========================================================================
    private List<QuestionReponse> mapResultSet(ResultSet res) throws SQLException {
        List<QuestionReponse> questions = new ArrayList<>();
        while (res.next()) {
            QuestionReponse qr = new QuestionReponse(
                    res.getInt    ("id_question_reponse"),
                    res.getInt    ("id_test_id"),          // ✅ FK réelle
                    res.getString ("texte_question"),
                    res.getString ("type_question"),
                    res.getInt    ("ordre_question"),
                    res.getString ("option1"),             // ✅ sans underscore
                    res.getInt    ("score1"),
                    res.getString ("option2"),
                    res.getInt    ("score2"),
                    res.getString ("option3"),
                    res.getInt    ("score3"),
                    res.getString ("option4"),
                    res.getInt    ("score4"),
                    res.getString ("option5"),
                    res.getInt    ("score5"),
                    res.getBoolean("est_obligatoire")
            );
            questions.add(qr);
        }
        return questions;
    }
}