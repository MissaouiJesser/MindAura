package tn.esprit.utils;

import tn.esprit.entities.utilisateurs;
import java.sql.*;
import java.util.*;

/**
 * Gère l'historique des connexions en BDD.
 * Crée la table automatiquement si elle n'existe pas.
 */
public class ConnexionHistorique {

    private ConnexionHistorique() {}

    /** Enregistre une connexion réussie */
    public static void enregistrer(utilisateurs user) {
        ensureTable();

        // Utiliser une nouvelle connexion pour éviter les conflits
        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = MyDataBase.getInstance().getConx();
            String sql = "INSERT INTO historique_connexion (id_utilisateur, email_utilisateur, nom_complet, statut) VALUES (?,?,?,?)";
            ps = con.prepareStatement(sql);

            ps.setString(1, user.getId_utilisateur());
            ps.setString(2, user.getEmail_utilisateur());
            ps.setString(3, user.getPrenom_utilisateur() + " " + user.getNom_utilisateur());
            ps.setString(4, "SUCCES");

            ps.executeUpdate();
            System.out.println("Connexion enregistrée dans l'historique");

        } catch (SQLException e) {
            System.out.println("Erreur lors de l'enregistrement de l'historique: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Fermer uniquement le PreparedStatement, pas la connexion
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** Retourne les N dernières connexions pour tous les utilisateurs */
    public static List<Map<String,String>> getHistorique(int limit) {
        ensureTable();
        List<Map<String,String>> list = new ArrayList<>();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = MyDataBase.getInstance().getConx();
            String sql = "SELECT h.*, u.role_utilisateur FROM historique_connexion h " +
                    "LEFT JOIN utilisateur u ON h.id_utilisateur = u.id_utilisateur " +
                    "ORDER BY date_connexion DESC LIMIT ?";
            ps = con.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();

            while (rs.next()) {
                Map<String,String> row = new LinkedHashMap<>();
                row.put("id", rs.getString("id"));
                row.put("nom_complet", rs.getString("nom_complet"));
                row.put("email", rs.getString("email_utilisateur"));
                row.put("date", rs.getTimestamp("date_connexion") != null ?
                        rs.getTimestamp("date_connexion").toString() : "");
                row.put("statut", rs.getString("statut"));
                row.put("role", rs.getString("role_utilisateur") != null ?
                        rs.getString("role_utilisateur") : "");
                list.add(row);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération de l'historique: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return list;
    }

    /** Retourne le nombre de connexions pour un utilisateur donné */
    public static int countConnexions(String idUtilisateur) {
        ensureTable();

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = MyDataBase.getInstance().getConx();
            String sql = "SELECT COUNT(*) FROM historique_connexion WHERE id_utilisateur = ?";
            ps = con.prepareStatement(sql);
            ps.setString(1, idUtilisateur);
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors du comptage des connexions: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    /** Retourne les stats : connexions par jour sur les 7 derniers jours */
    public static Map<String,Integer> statsParJour() {
        ensureTable();
        Map<String,Integer> stats = new LinkedHashMap<>();

        Connection con = null;
        Statement st = null;
        ResultSet rs = null;

        try {
            con = MyDataBase.getInstance().getConx();
            String sql = "SELECT DATE(date_connexion) as jour, COUNT(*) as nb " +
                    "FROM historique_connexion WHERE date_connexion >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                    "GROUP BY DATE(date_connexion) ORDER BY jour ASC";
            st = con.createStatement();
            rs = st.executeQuery(sql);

            while (rs.next()) {
                stats.put(rs.getString("jour"), rs.getInt("nb"));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération des stats: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (st != null) st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return stats;
    }

    private static void ensureTable() {
        Connection con = null;
        Statement st = null;

        try {
            con = MyDataBase.getInstance().getConx();
            String sql = "CREATE TABLE IF NOT EXISTS historique_connexion (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "id_utilisateur VARCHAR(50)," +
                    "email_utilisateur VARCHAR(150)," +
                    "nom_complet VARCHAR(200)," +
                    "date_connexion DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "statut VARCHAR(20) DEFAULT 'SUCCES'," +
                    "INDEX idx_user (id_utilisateur)," +
                    "INDEX idx_date (date_connexion))";
            st = con.createStatement();
            st.executeUpdate(sql);
        } catch (SQLException e) {
            // Table existe déjà ou autre erreur
            System.out.println("Info table historique: " + e.getMessage());
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}