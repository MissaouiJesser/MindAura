package tn.esprit.services;

import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Service de gestion des favoris persistants en base de données.
 *
 * Table SQL requise :
 * ─────────────────────────────────────────────────────────────
 * CREATE TABLE IF NOT EXISTS favoris (
 *     id         INT AUTO_INCREMENT PRIMARY KEY,
 *     ressource_id INT NOT NULL,
 *     user_id    INT NOT NULL DEFAULT 1,
 *     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 *     UNIQUE KEY unique_favori (ressource_id, user_id),
 *     FOREIGN KEY (ressource_id) REFERENCES ressources(id) ON DELETE CASCADE
 * );
 * ─────────────────────────────────────────────────────────────
 */
public class FavoriService {

    private final Connection conn;
    private final int userId; // ID de l'utilisateur connecté (1 par défaut)

    public FavoriService(int userId) {
        this.conn   = MyDataBase.getInstance().getConx();
        this.userId = userId;
        creerTableSiAbsente();
    }

    public FavoriService() {
        this(1); // utilisateur par défaut
    }

    // ─── Création de la table si elle n'existe pas ───────────
    private void creerTableSiAbsente() {
        String sql = """
            CREATE TABLE IF NOT EXISTS favoris (
                id           INT AUTO_INCREMENT PRIMARY KEY,
                ressource_id INT NOT NULL,
                user_id      INT NOT NULL DEFAULT 1,
                created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_favori (ressource_id, user_id)
            )
            """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("[FavoriService] Impossible de créer la table favoris : " + e.getMessage());
        }
    }

    // ─── Charger tous les IDs favoris de l'utilisateur ───────
    public Set<Integer> chargerFavoris() {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT ressource_id FROM favoris WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt("ressource_id"));
        } catch (SQLException e) {
            System.err.println("[FavoriService] Erreur chargement favoris : " + e.getMessage());
        }
        return ids;
    }

    // ─── Ajouter un favori ───────────────────────────────────
    public boolean ajouterFavori(int ressourceId) {
        String sql = "INSERT IGNORE INTO favoris (ressource_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ressourceId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[FavoriService] Erreur ajout favori : " + e.getMessage());
            return false;
        }
    }

    // ─── Supprimer un favori ─────────────────────────────────
    public boolean supprimerFavori(int ressourceId) {
        String sql = "DELETE FROM favoris WHERE ressource_id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ressourceId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[FavoriService] Erreur suppression favori : " + e.getMessage());
            return false;
        }
    }

    // ─── Toggle (ajouter ou supprimer selon état actuel) ─────
    public boolean toggleFavori(int ressourceId, boolean estFavori) {
        return estFavori ? ajouterFavori(ressourceId) : supprimerFavori(ressourceId);
    }

    // ─── Vérifier si une ressource est en favori ─────────────
    public boolean estFavori(int ressourceId) {
        String sql = "SELECT COUNT(*) FROM favoris WHERE ressource_id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ressourceId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("[FavoriService] Erreur vérification favori : " + e.getMessage());
        }
        return false;
    }
}