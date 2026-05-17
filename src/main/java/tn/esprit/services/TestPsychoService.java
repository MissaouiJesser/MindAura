package tn.esprit.services;

import tn.esprit.entities.TestPsycho;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestPsychoService implements ICRUD<TestPsycho> {

    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;

    public TestPsychoService() {
        conx = MyDataBase.getInstance().getConx();
    }

    @Override
    public void add(TestPsycho test) throws SQLException {
        String req = "INSERT INTO `test_psychologique`(`titre_test`, `description_test`, `type_test`, " +
                "`duree_estimee`, `instructions_test`, `est_actif`) " +
                "VALUES ('" + test.getTitreTest() + "','" + test.getDescriptionTest() + "','" +
                test.getTypeTest() + "'," + test.getDureeEstimee() + ",'" +  // ✅ getDureeEstimee()
                test.getInstructionsTest() + "'," + test.isEstActif() + ")";

        stm = conx.createStatement();
        stm.executeUpdate(req);
        System.out.println("Test psychologique ajouté");
    }

    @Override
    public void addMeth2(TestPsycho test) throws SQLException {
        String req = "INSERT INTO `test_psychologique`(`titre_test`, `description_test`, `type_test`, " +
                "`duree_estimee`, `instructions_test`, `est_actif`) " +
                "VALUES (?,?,?,?,?,?)";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, test.getTitreTest());
        pstm.setString(2, test.getDescriptionTest());
        pstm.setString(3, test.getTypeTest());
        pstm.setInt(4, test.getDureeEstimee());         // ✅ getDureeEstimee()
        pstm.setString(5, test.getInstructionsTest());
        pstm.setBoolean(6, test.isEstActif());

        pstm.executeUpdate();
        System.out.println("Test psychologique ajouté (méthode 2)");
    }

    @Override
    public void modifier(TestPsycho test) throws SQLException {
        String req = "UPDATE `test_psychologique` SET " +
                "`titre_test`=?, `description_test`=?, `type_test`=?, " +
                "`duree_estimee`=?, `instructions_test`=?, `est_actif`=? " +
                "WHERE `id_test`=?";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, test.getTitreTest());
        pstm.setString(2, test.getDescriptionTest());
        pstm.setString(3, test.getTypeTest());
        pstm.setInt(4, test.getDureeEstimee());         // ✅ getDureeEstimee()
        pstm.setString(5, test.getInstructionsTest());
        pstm.setBoolean(6, test.isEstActif());
        pstm.setInt(7, test.getIdTest());

        pstm.executeUpdate();
        System.out.println("Test modifié avec ID = " + test.getIdTest());
    }

    @Override
    public void delete(TestPsycho test) throws SQLException {
        String req = "DELETE FROM `test_psychologique` WHERE `id_test`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, test.getIdTest());
        pstm.executeUpdate();
        System.out.println("Test supprimé avec ID = " + test.getIdTest());
    }

    @Override
    public List<TestPsycho> afficherList() throws SQLException {
        String req = "SELECT * FROM `test_psychologique`";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);

        List<TestPsycho> tests = new ArrayList<>();
        while (res.next()) {
            tests.add(mapResultSet(res));   // ✅ méthode commune pour éviter la duplication
        }
        return tests;
    }

    public TestPsycho getTestById(int idTest) throws SQLException {
        String req = "SELECT * FROM `test_psychologique` WHERE `id_test`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        ResultSet res = pstm.executeQuery();

        if (res.next()) return mapResultSet(res);
        return null;
    }

    public List<TestPsycho> afficherTestsActifs() throws SQLException {
        String req = "SELECT * FROM `test_psychologique` WHERE `est_actif`=1";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);

        List<TestPsycho> tests = new ArrayList<>();
        while (res.next()) {
            tests.add(mapResultSet(res));
        }
        return tests;
    }

    public List<TestPsycho> afficherTestsParType(String typeTest) throws SQLException {
        String req = "SELECT * FROM `test_psychologique` WHERE `type_test`=? AND `est_actif`=1";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, typeTest);
        ResultSet res = pstm.executeQuery();

        List<TestPsycho> tests = new ArrayList<>();
        while (res.next()) {
            tests.add(mapResultSet(res));
        }
        return tests;
    }

    public void toggleActif(int idTest) throws SQLException {
        String req = "UPDATE `test_psychologique` SET `est_actif` = NOT `est_actif` WHERE `id_test`=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idTest);
        pstm.executeUpdate();
        System.out.println("Statut du test modifié (activé/désactivé)");
    }

    // ✅ Méthode utilitaire pour mapper un ResultSet → TestPsycho (évite la duplication)
    private TestPsycho mapResultSet(ResultSet res) throws SQLException {
        return new TestPsycho(
                res.getInt("id_test"),
                res.getString("titre_test"),        // ✅ colonnes DB corrigées
                res.getString("description_test"),  // ✅
                res.getString("type_test"),
                res.getInt("duree_estimee"),
                res.getString("instructions_test"), // ✅
                res.getTimestamp("date_creation"),
                res.getTimestamp("date_modification"),
                res.getBoolean("est_actif")
        );
    }
}