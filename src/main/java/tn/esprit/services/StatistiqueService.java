package tn.esprit.services;

import tn.esprit.utils.MyDataBase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service pour les statistiques des réclamations
 * Traité / En cours / Non traité + par catégorie
 */
public class StatistiqueService {

    private final Connection conx;

    public StatistiqueService() {
        conx = MyDataBase.getInstance().getConx();
    }

    /**
     * Nombre de réclamations par statut
     * Retourne: {"TRAITEE": 12, "EN_COURS": 5, "EN_ATTENTE": 8}
     */
    public Map<String, Integer> getNombreParStatut() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        stats.put("TRAITEE", 0);
        stats.put("EN_COURS", 0);
        stats.put("EN_ATTENTE", 0);

        String req = "SELECT statut_reclamation, COUNT(*) as total " +
                "FROM reclamation GROUP BY statut_reclamation";

        PreparedStatement pstm = conx.prepareStatement(req);
        ResultSet rs = pstm.executeQuery();

        while (rs.next()) {
            String statut = rs.getString("statut_reclamation");
            int total = rs.getInt("total");
            stats.put(statut, total);
        }
        return stats;
    }

    /**
     * Total de toutes les réclamations
     */
    public int getTotalReclamations() throws SQLException {
        String req = "SELECT COUNT(*) as total FROM reclamation";
        PreparedStatement pstm = conx.prepareStatement(req);
        ResultSet rs = pstm.executeQuery();
        if (rs.next()) return rs.getInt("total");
        return 0;
    }

    /**
     * Nombre de réclamations par catégorie
     * Retourne: {"COACH": 10, "PSYCHOLOGUE": 5, ...}
     */
    public Map<String, Integer> getNombreParCategorie() throws SQLException {
        Map<String, Integer> stats = new LinkedHashMap<>();
        String req = "SELECT categorie_reclamation, COUNT(*) as total " +
                "FROM reclamation GROUP BY categorie_reclamation ORDER BY total DESC";
        PreparedStatement pstm = conx.prepareStatement(req);
        ResultSet rs = pstm.executeQuery();
        while (rs.next()) {
            stats.put(rs.getString("categorie_reclamation"), rs.getInt("total"));
        }
        return stats;
    }

    /**
     * Taux de résolution (réclamations traitées / total * 100)
     */
    public double getTauxResolution() throws SQLException {
        int total = getTotalReclamations();
        if (total == 0) return 0;
        Map<String, Integer> parStatut = getNombreParStatut();
        int traitees = parStatut.getOrDefault("TRAITEE", 0);
        return Math.round((traitees * 100.0 / total) * 10.0) / 10.0;
    }

    /**
     * Note moyenne des réclamations
     */
    public double getNoteMoyenne() throws SQLException {
        String req = "SELECT AVG(rate_reclamation) as moyenne FROM reclamation WHERE rate_reclamation > 0";
        PreparedStatement pstm = conx.prepareStatement(req);
        ResultSet rs = pstm.executeQuery();
        if (rs.next()) return Math.round(rs.getDouble("moyenne") * 10.0) / 10.0;
        return 0;
    }
}