package tn.esprit.services;

import tn.esprit.controllers.ICRUD;
import tn.esprit.entities.Reponse;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements ICRUD<Reponse> {

    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;

    public ReponseService() {
        conx = MyDataBase.getInstance().getConx();
    }

    // ===================== ADD (méthode simple) =====================
    @Override
    public void add(Reponse reponse) throws SQLException {

        String req = "INSERT INTO reponse " +
                "(contenu_reponse, date_reponse, id_utilisateur, " +   // ← date_reponse
                "nom_utilisateur, rate_reponse, reclamation_id) " +    // ← nom_utilisateur, reclamation_id
                "VALUES ('" + reponse.getContenuReponse() + "',NOW()," +
                reponse.getIdUtilisateur() + ",'" +
                reponse.getNomUtilisateur() + "'," +
                reponse.getRateReponse() + "," +
                reponse.getReclamationId() + ")";                       // ← getReclamationId()

        stm = conx.createStatement();
        stm.executeUpdate(req);
        System.out.println("Réponse ajoutée");
    }

    // ===================== ADD (PreparedStatement) =====================
    @Override
    public void addMeth2(Reponse reponse) throws SQLException {

        String req = "INSERT INTO reponse " +
                "(contenu_reponse, date_reponse, id_utilisateur, " +
                "nom_utilisateur, rate_reponse, reclamation_id) " +
                "VALUES (?,?,?,?,?,?)";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, reponse.getContenuReponse());

        if (reponse.getDateReponse() != null) {
            pstm.setTimestamp(2, reponse.getDateReponse());
        } else {
            pstm.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        }

        pstm.setInt(3, reponse.getIdUtilisateur());
        pstm.setString(4, reponse.getNomUtilisateur());            // ← nom_utilisateur
        pstm.setDouble(5, reponse.getRateReponse());               // ← double
        pstm.setInt(6, reponse.getReclamationId());                // ← reclamation_id

        pstm.executeUpdate();
        System.out.println("Réponse ajoutée (meth2)");
    }

    // ===================== UPDATE =====================
    @Override
    public void modifier(Reponse reponse) throws SQLException {

        String req = "UPDATE reponse SET " +
                "contenu_reponse=?, date_reponse=?, id_utilisateur=?, " +
                "nom_utilisateur=?, rate_reponse=?, reclamation_id=? " +
                "WHERE id_reponse=?";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, reponse.getContenuReponse());

        if (reponse.getDateReponse() != null) {
            pstm.setTimestamp(2, reponse.getDateReponse());
        } else {
            pstm.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        }

        pstm.setInt(3, reponse.getIdUtilisateur());
        pstm.setString(4, reponse.getNomUtilisateur());            // ← nom_utilisateur
        pstm.setDouble(5, reponse.getRateReponse());               // ← double
        pstm.setInt(6, reponse.getReclamationId());                // ← reclamation_id
        pstm.setInt(7, reponse.getIdReponse());

        pstm.executeUpdate();
        System.out.println("Réponse modifiée avec ID = " + reponse.getIdReponse());
    }

    // ===================== DELETE =====================
    @Override
    public void delete(Reponse reponse) throws SQLException {

        String req = "DELETE FROM reponse WHERE id_reponse=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reponse.getIdReponse());
        pstm.executeUpdate();

        System.out.println("Réponse supprimée avec ID = " + reponse.getIdReponse());
    }

    // ===================== SELECT ALL =====================
    @Override
    public List<Reponse> afficherList() throws SQLException {

        String req = "SELECT * FROM reponse";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);

        List<Reponse> reponses = new ArrayList<>();

        while (res.next()) {
            Reponse r = new Reponse(
                    res.getInt("id_reponse"),
                    res.getString("contenu_reponse"),
                    res.getTimestamp("date_reponse"),               // ← date_reponse
                    res.getInt("id_utilisateur"),
                    res.getString("nom_utilisateur"),               // ← nom_utilisateur
                    res.getDouble("rate_reponse"),                  // ← double
                    res.getInt("rate_sum"),
                    res.getInt("rate_count"),
                    res.getInt("reclamation_id")                    // ← reclamation_id
            );
            reponses.add(r);
        }

        return reponses;
    }

    // ===================== SELECT PAR RECLAMATION =====================
    public List<Reponse> afficherParReclamation(int reclamationId) throws SQLException {

        String req = "SELECT * FROM reponse WHERE reclamation_id = ? ORDER BY date_reponse";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reclamationId);                             // ← reclamation_id
        ResultSet res = pstm.executeQuery();

        List<Reponse> reponses = new ArrayList<>();
        while (res.next()) {
            Reponse r = new Reponse(
                    res.getInt("id_reponse"),
                    res.getString("contenu_reponse"),
                    res.getTimestamp("date_reponse"),
                    res.getInt("id_utilisateur"),
                    res.getString("nom_utilisateur"),
                    res.getDouble("rate_reponse"),
                    res.getInt("rate_sum"),
                    res.getInt("rate_count"),
                    res.getInt("reclamation_id")
            );
            reponses.add(r);
        }
        return reponses;
    }

    // ===================== COMPTER PAR RECLAMATION =====================
    public int compterParReclamation(int reclamationId) throws SQLException {

        String req = "SELECT COUNT(*) FROM reponse WHERE reclamation_id = ?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reclamationId);
        ResultSet res = pstm.executeQuery();
        return res.next() ? res.getInt(1) : 0;
    }

    // ===================== VOTE (rate_sum / rate_count) =====================
    public void addVoteReponse(Reponse r, int stars) throws SQLException {
        if (r == null || stars < 1 || stars > 5) return;

        String req = "UPDATE reponse SET " +
                "rate_sum = COALESCE(rate_sum,0) + ?, " +
                "rate_count = COALESCE(rate_count,0) + 1 " +
                "WHERE id_reponse = ?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, stars);
        pstm.setInt(2, r.getIdReponse());
        pstm.executeUpdate();

        r.setRateSum(r.getRateSum() + stars);
        r.setRateCount(r.getRateCount() + 1);
    }
}