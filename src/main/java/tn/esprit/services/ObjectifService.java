package tn.esprit.services;

import tn.esprit.entities.Objectif;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectifService implements ICRUD<Objectif> {

    private Connection        conx;
    private Statement         stm;
    private PreparedStatement pstm;

    public ObjectifService() {
        conx = MyDataBase.getInstance().getConx();
    }

    // =========================================================================
    //  ADD
    //  Colonnes réelles : source, id_utilisateur (int), titre, description,
    //  type_objectif, type_test, niveau_recommande, score_min, score_max,
    //  categorie, duree_estimee, difficule, date_creation, date_echeance,
    //  statut, est_public
    // =========================================================================
    @Override
    public void add(Objectif obj) throws SQLException {
        String req = "INSERT INTO `objectif` " +
                "(`source`, `id_utilisateur`, `titre`, `description`, `type_objectif`, " +
                " `type_test`, `niveau_recommande`, `score_min`, `score_max`, " +
                " `categorie`, `duree_estimee`, `difficule`, " +
                " `date_echeance`, `statut`, `est_public`) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        pstm = conx.prepareStatement(req);
        pstm.setString (1,  obj.getSource());
        // ✅ id_utilisateur est int(11), NULL autorisé
        if (obj.getIdUtilisateur() != 0) {
            pstm.setInt(2, obj.getIdUtilisateur());
        } else {
            pstm.setNull(2, Types.INTEGER);
        }
        pstm.setString (3,  obj.getTitre());
        pstm.setString (4,  obj.getDescription());
        pstm.setString (5,  obj.getTypeObjectif());
        pstm.setString (6,  obj.getTypeTest());
        pstm.setString (7,  obj.getNiveauRecommande());
        pstm.setInt    (8,  obj.getScoreMin());
        pstm.setInt    (9,  obj.getScoreMax());
        pstm.setString (10, obj.getCategorie());
        pstm.setInt    (11, obj.getDureeEstimee());
        pstm.setString (12, obj.getDifficulte());         // ✅ colonne "difficule" (sans accent)
        // ✅ date_echeance est NOT NULL en DB
        pstm.setDate   (13, obj.getDateEcheance() != null
                ? obj.getDateEcheance()
                : new Date(System.currentTimeMillis()));
        pstm.setString (14, obj.getStatut());
        pstm.setBoolean(15, obj.isEstPublic());

        pstm.executeUpdate();
        System.out.println("Objectif ajouté");
    }

    @Override
    public void addMeth2(Objectif obj) throws SQLException {
        add(obj);
    }

    // =========================================================================
    //  MODIFIER
    // =========================================================================
    @Override
    public void modifier(Objectif obj) throws SQLException {
        String req = "UPDATE `objectif` SET " +
                "`source`=?, `id_utilisateur`=?, `titre`=?, `description`=?, " +
                "`type_objectif`=?, `type_test`=?, `niveau_recommande`=?, " +
                "`score_min`=?, `score_max`=?, `categorie`=?, `duree_estimee`=?, " +
                "`difficule`=?, `date_echeance`=?, `statut`=?, `est_public`=? " +
                "WHERE `id_objectif`=?";

        pstm = conx.prepareStatement(req);
        pstm.setString (1,  obj.getSource());
        if (obj.getIdUtilisateur() != 0) {
            pstm.setInt(2, obj.getIdUtilisateur());
        } else {
            pstm.setNull(2, Types.INTEGER);
        }
        pstm.setString (3,  obj.getTitre());
        pstm.setString (4,  obj.getDescription());
        pstm.setString (5,  obj.getTypeObjectif());
        pstm.setString (6,  obj.getTypeTest());
        pstm.setString (7,  obj.getNiveauRecommande());
        pstm.setInt    (8,  obj.getScoreMin());
        pstm.setInt    (9,  obj.getScoreMax());
        pstm.setString (10, obj.getCategorie());
        pstm.setInt    (11, obj.getDureeEstimee());
        pstm.setString (12, obj.getDifficulte());
        pstm.setDate   (13, obj.getDateEcheance() != null
                ? obj.getDateEcheance()
                : new Date(System.currentTimeMillis()));
        pstm.setString (14, obj.getStatut());
        pstm.setBoolean(15, obj.isEstPublic());
        pstm.setInt    (16, obj.getIdObjectif());

        pstm.executeUpdate();
        System.out.println("Objectif modifié avec ID = " + obj.getIdObjectif());
    }

    // =========================================================================
    //  DELETE
    // =========================================================================
    @Override
    public void delete(Objectif obj) throws SQLException {
        String req = "DELETE FROM `objectif` WHERE `id_objectif`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, obj.getIdObjectif());
        pstm.executeUpdate();
        System.out.println("Objectif supprimé avec ID = " + obj.getIdObjectif());
    }

    // =========================================================================
    //  AFFICHER LISTE
    // =========================================================================
    @Override
    public List<Objectif> afficherList() throws SQLException {
        String req = "SELECT * FROM `objectif` ORDER BY `date_creation` DESC";
        stm = conx.createStatement();
        return mapResultSet(stm.executeQuery(req));
    }

    // =========================================================================
    //  OBJECTIFS ADMIN (publics)
    // =========================================================================
    public List<Objectif> getObjectifsAdmin() throws SQLException {
        String req = "SELECT * FROM `objectif` WHERE `type_objectif`='admin' AND `est_public`=1";
        stm = conx.createStatement();
        return mapResultSet(stm.executeQuery(req));
    }

    // =========================================================================
    //  OBJECTIFS PATIENT — ✅ idUtilisateur est int
    // =========================================================================
    public List<Objectif> getObjectifsPatient(int idUtilisateur) throws SQLException {
        String req = "SELECT * FROM `objectif` WHERE `type_objectif`='patient' AND `id_utilisateur`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idUtilisateur);
        return mapResultSet(pstm.executeQuery());
    }

    // =========================================================================
    //  RECOMMANDER SELON TYPE TEST + SCORE
    // =========================================================================
    public List<Objectif> recommanderObjectifs(String typeTest, int score) throws SQLException {
        String req = "SELECT * FROM `objectif` WHERE `type_objectif`='admin' " +
                "AND `type_test`=? AND `score_min`<=? AND `score_max`>=? AND `est_public`=1";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, typeTest);
        pstm.setInt   (2, score);
        pstm.setInt   (3, score);
        return mapResultSet(pstm.executeQuery());
    }

    // =========================================================================
    //  HELPER PRIVÉ — mappage ResultSet → Objectif
    //  Colonnes réelles : source, id_utilisateur (int), difficule (sans accent),
    //  date_creation (datetime/NULL), date_echeance (date/NOT NULL), est_public
    // =========================================================================
    private List<Objectif> mapResultSet(ResultSet res) throws SQLException {
        List<Objectif> objectifs = new ArrayList<>();
        while (res.next()) {
            Objectif obj = new Objectif(
                    res.getInt       ("id_objectif"),
                    res.getString    ("source"),              // ✅ nouveau champ
                    res.getInt       ("id_utilisateur"),      // ✅ int, pas String
                    res.getString    ("titre"),
                    res.getString    ("description"),
                    res.getString    ("type_objectif"),
                    res.getString    ("type_test"),
                    res.getString    ("niveau_recommande"),
                    res.getInt       ("score_min"),
                    res.getInt       ("score_max"),
                    res.getString    ("categorie"),
                    res.getInt       ("duree_estimee"),
                    res.getString    ("difficule"),           // ✅ sans accent en DB
                    res.getTimestamp ("date_creation"),       // ✅ Timestamp (datetime, NULL)
                    res.getDate      ("date_echeance"),       // ✅ java.sql.Date (date, NOT NULL)
                    res.getString    ("statut"),
                    res.getBoolean   ("est_public")
            );
            objectifs.add(obj);
        }
        return objectifs;
    }
}