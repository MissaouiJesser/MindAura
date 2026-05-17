package tn.esprit.services;

import tn.esprit.entities.Commentaires;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommentairesServices implements ICRUD<Commentaires> {

    private final Connection conx;

    public CommentairesServices(Connection conn) {
        this.conx = conn;
    }

    // ══════════════════════════════════════════════════════
    // AJOUTER
    // ══════════════════════════════════════════════════════
    @Override
    public void add(Commentaires c) throws SQLException {
        String req =
                "INSERT INTO commentaire " +
                        "(ressource_id, id_user, user_name, contenu, date_publication, likes, reponse, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pst = conx.prepareStatement(req)) {
            pst.setInt   (1, c.getRessource_id());
            pst.setInt   (2, c.getId_user());
            pst.setString(3, c.getUser_name());
            pst.setString(4, c.getContenu());
            pst.setDate  (5, new java.sql.Date(c.getDate().getTime()));
            pst.setInt   (6, c.getLikes());
            pst.setInt   (7, c.getReponse());
            pst.setString(8, c.getStatus() != null
                    ? c.getStatus().name()
                    : Commentaires.StatusCommentaires.ACTIF.name());
            pst.executeUpdate();
            System.out.println("Commentaire ajouté !");
        }
    }

    // Méthode 2 conservée pour compatibilité (non recommandée — pas de PreparedStatement)
    @Override
    public void addMeth2(Commentaires c) throws SQLException {
        String req =
                "INSERT INTO commentaire(ressource_id, id_user, user_name, contenu, date_publication, likes, reponse, status) " +
                        "VALUES ('" + c.getRessource_id() + "','" +
                        c.getId_user()   + "','" +
                        c.getUser_name() + "','" +
                        c.getContenu()   + "','" +
                        new java.sql.Date(c.getDate().getTime()) + "','" +
                        c.getLikes()     + "','" +
                        c.getReponse()   + "','" +
                        (c.getStatus() != null ? c.getStatus().name() : "ACTIF") + "')";
        conx.createStatement().executeUpdate(req);
        System.out.println("Commentaire ajouté (méthode 2)");
    }

    // ══════════════════════════════════════════════════════
    // MODIFIER
    // ══════════════════════════════════════════════════════
    @Override
    public void modifier(Commentaires c) throws SQLException {
        String req =
                "UPDATE commentaire SET " +
                        "ressource_id = ?, id_user = ?, user_name = ?, contenu = ?, " +
                        "date_publication = ?, likes = ?, reponse = ?, status = ? " +
                        "WHERE id_commentaires = ?";

        try (PreparedStatement pst = conx.prepareStatement(req)) {
            pst.setInt   (1, c.getRessource_id());
            pst.setInt   (2, c.getId_user());
            pst.setString(3, c.getUser_name());
            pst.setString(4, c.getContenu());
            pst.setDate  (5, new java.sql.Date(c.getDate().getTime()));
            pst.setInt   (6, c.getLikes());
            pst.setInt   (7, c.getReponse());
            pst.setString(8, c.getStatus() != null
                    ? c.getStatus().name()
                    : Commentaires.StatusCommentaires.ACTIF.name());
            pst.setInt   (9, c.getId_commentaires());

            int rows = pst.executeUpdate();
            System.out.println(rows > 0
                    ? "Commentaire modifié avec succès !"
                    : "Aucun commentaire trouvé avec cet ID !");
        }
    }

    // ══════════════════════════════════════════════════════
    // SUPPRIMER
    // ══════════════════════════════════════════════════════
    @Override
    public void delete(Commentaires c) throws SQLException {
        String req = "DELETE FROM commentaire WHERE id_commentaires = ?";
        try (PreparedStatement pst = conx.prepareStatement(req)) {
            pst.setInt(1, c.getId_commentaires());
            int rows = pst.executeUpdate();
            System.out.println(rows > 0
                    ? "Commentaire supprimé avec succès !"
                    : "Aucun commentaire trouvé avec cet ID !");
        }
    }

    // ══════════════════════════════════════════════════════
    // AFFICHER TOUT
    // ══════════════════════════════════════════════════════
    @Override
    public List<Commentaires> afficherList() throws SQLException {
        List<Commentaires> list = new ArrayList<>();
        String req = "SELECT * FROM commentaire";

        try (Statement stm = conx.createStatement();
             ResultSet rs  = stm.executeQuery(req)) {

            while (rs.next()) list.add(mapper(rs));
        }
        return list;
    }

    // ══════════════════════════════════════════════════════
    // PAR RESSOURCE (ex-idArticle)
    // ══════════════════════════════════════════════════════
    public List<Commentaires> getCommentairesByArticle(int ressourceId) {
        List<Commentaires> list = new ArrayList<>();
        String req =
                "SELECT * FROM commentaire WHERE ressource_id = ? ORDER BY date_publication DESC";

        try (PreparedStatement pst = conx.prepareStatement(req)) {
            pst.setInt(1, ressourceId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) list.add(mapper(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ══════════════════════════════════════════════════════
    // LIKER
    // ══════════════════════════════════════════════════════
    public void likeCommentaire(int idCommentaire) {
        String req = "UPDATE commentaire SET likes = likes + 1 WHERE id_commentaires = ?";
        try (PreparedStatement pst = conx.prepareStatement(req)) {
            pst.setInt(1, idCommentaire);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    // SIGNALER  (status → SIGNALE)
    // ══════════════════════════════════════════════════════
    public void signalerCommentaire(int idCommentaire) {
        String req = "UPDATE commentaire SET status = 'SIGNALE' WHERE id_commentaires = ?";
        try (PreparedStatement pst = conx.prepareStatement(req)) {
            pst.setInt(1, idCommentaire);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    // MAPPER ResultSet → Commentaires
    // ══════════════════════════════════════════════════════
    private Commentaires mapper(ResultSet rs) throws SQLException {
        Commentaires c = new Commentaires();
        c.setId_commentaires(rs.getInt("id_commentaires"));
        c.setId_user        (rs.getInt("id_user"));
        c.setUser_name      (rs.getString("user_name"));
        c.setContenu        (rs.getString("contenu"));
        c.setDate           (rs.getDate("date_publication"));   // ← corrigé
        c.setLikes          (rs.getInt("likes"));
        c.setReponse        (rs.getInt("reponse"));
        c.setRessource_id   (rs.getInt("ressource_id"));
        c.setStatusFromString(rs.getString("status"));

        // Colonnes optionnelles (présentes dans l'entité, peuvent être NULL en BD)
        c.setSentiment(rs.getString("sentiment"));
        c.setThemes   (rs.getString("themes"));
        c.setAiSummary(rs.getString("ai_summary"));

        int parentId = rs.getInt("parent_id");
        c.setParent_id(rs.wasNull() ? null : parentId);

        return c;
    }
}