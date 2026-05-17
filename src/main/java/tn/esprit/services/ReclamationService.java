package tn.esprit.services;

import tn.esprit.controllers.ICRUD;
import tn.esprit.entities.Reclamation;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements ICRUD<Reclamation> {

    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;

    public ReclamationService() {
        conx = MyDataBase.getInstance().getConx();
    }

    // ===================== ADD (méthode simple) =====================
    @Override
    public void add(Reclamation reclamation) throws SQLException {

        String req = "INSERT INTO reclamation " +
                "(sujet_reclamation, description_reclamation, date_creation_reclamation, " +
                "statut_reclamation, utilisateur_id, rate_reclamation, categorie_id) " +
                "VALUES ('" + reclamation.getSujetReclamation() + "','" +
                reclamation.getDescriptionReclamation() + "',NOW(),'" +
                reclamation.getStatutReclamation() + "'," +
                reclamation.getUtilisateurId() + "," +
                reclamation.getRateReclamation() + "," +
                reclamation.getCategorieId() + ")";

        stm = conx.createStatement();
        stm.executeUpdate(req);
        System.out.println("Réclamation ajoutée");
    }

    // ===================== ADD (PreparedStatement) =====================
    @Override
    public void addMeth2(Reclamation reclamation) throws SQLException {

        String req = "INSERT INTO reclamation " +
                "(sujet_reclamation, description_reclamation, date_creation_reclamation, " +
                "statut_reclamation, utilisateur_id, rate_reclamation, categorie_id) " +
                "VALUES (?,?,?,?,?,?,?)";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, reclamation.getSujetReclamation());
        pstm.setString(2, reclamation.getDescriptionReclamation());

        if (reclamation.getDateCreationReclamation() != null) {
            pstm.setTimestamp(3, new Timestamp(reclamation.getDateCreationReclamation().getTime()));
        } else {
            pstm.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
        }

        pstm.setString(4, reclamation.getStatutReclamation());
        pstm.setInt(5, reclamation.getUtilisateurId());          // ← utilisateur_id
        pstm.setDouble(6, reclamation.getRateReclamation());     // ← double
        pstm.setInt(7, reclamation.getCategorieId());            // ← categorie_id

        pstm.executeUpdate();
        System.out.println("Réclamation ajoutée (meth2)");
    }

    // ===================== UPDATE =====================
    @Override
    public void modifier(Reclamation reclamation) throws SQLException {

        String req = "UPDATE reclamation SET " +
                "sujet_reclamation=?, description_reclamation=?, " +
                "statut_reclamation=?, categorie_id=?, rate_reclamation=? " +
                "WHERE id_reclamation=?";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, reclamation.getSujetReclamation());
        pstm.setString(2, reclamation.getDescriptionReclamation());
        pstm.setString(3, reclamation.getStatutReclamation());
        pstm.setInt(4, reclamation.getCategorieId());            // ← categorie_id (int)
        pstm.setDouble(5, reclamation.getRateReclamation());     // ← double
        pstm.setInt(6, reclamation.getIdReclamation());

        pstm.executeUpdate();
        System.out.println("Réclamation modifiée avec ID = " + reclamation.getIdReclamation());
    }

    // ===================== DELETE =====================
    @Override
    public void delete(Reclamation reclamation) throws SQLException {

        String req = "DELETE FROM reclamation WHERE id_reclamation=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reclamation.getIdReclamation());
        pstm.executeUpdate();

        System.out.println("Réclamation supprimée avec ID = " + reclamation.getIdReclamation());
    }

    // ===================== SELECT ALL =====================
    @Override
    public List<Reclamation> afficherList() throws SQLException {

        String req = "SELECT * FROM reclamation";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);

        List<Reclamation> reclamations = new ArrayList<>();

        while (res.next()) {
            Reclamation r = new Reclamation(
                    res.getInt("id_reclamation"),
                    res.getString("sujet_reclamation"),
                    res.getString("description_reclamation"),
                    res.getTimestamp("date_creation_reclamation"),  // ← date_creation_reclamation
                    res.getString("statut_reclamation"),
                    res.getInt("utilisateur_id"),                   // ← utilisateur_id
                    res.getDouble("rate_reclamation"),              // ← double
                    res.getInt("rate_sum"),
                    res.getInt("rate_count"),
                    res.getInt("categorie_id")                      // ← categorie_id
            );
            reclamations.add(r);
        }

        return reclamations;
    }

    // ===================== SELECT PAR UTILISATEUR =====================
    public List<Reclamation> afficherParUtilisateur(int utilisateurId) throws SQLException {

        String req = "SELECT * FROM reclamation WHERE utilisateur_id = ? ORDER BY date_creation_reclamation DESC";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, utilisateurId);
        ResultSet res = pstm.executeQuery();

        List<Reclamation> reclamations = new ArrayList<>();
        while (res.next()) {
            Reclamation r = new Reclamation(
                    res.getInt("id_reclamation"),
                    res.getString("sujet_reclamation"),
                    res.getString("description_reclamation"),
                    res.getTimestamp("date_creation_reclamation"),
                    res.getString("statut_reclamation"),
                    res.getInt("utilisateur_id"),
                    res.getDouble("rate_reclamation"),
                    res.getInt("rate_sum"),
                    res.getInt("rate_count"),
                    res.getInt("categorie_id")
            );
            reclamations.add(r);
        }
        return reclamations;
    }

    // ===================== VOTE (rate_sum / rate_count) =====================
    public void addVoteReclamation(Reclamation r, int stars) throws SQLException {
        if (r == null || stars < 1 || stars > 5) return;

        String req = "UPDATE reclamation SET " +
                "rate_sum = COALESCE(rate_sum,0) + ?, " +
                "rate_count = COALESCE(rate_count,0) + 1 " +
                "WHERE id_reclamation = ?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, stars);
        pstm.setInt(2, r.getIdReclamation());
        pstm.executeUpdate();

        r.setRateSum(r.getRateSum() + stars);
        r.setRateCount(r.getRateCount() + 1);
    }
}