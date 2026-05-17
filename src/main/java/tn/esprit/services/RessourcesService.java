package tn.esprit.services;

import tn.esprit.entities.Ressources;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RessourcesService implements ICRUD<Ressources> {

    private Connection conx;

    public RessourcesService(Connection conn) {
        this.conx = conn;
    }

    @Override
    public void add(Ressources r) throws SQLException {
        String req = "INSERT INTO ressource " +
                "(image_url, titre, resume, contenu, categorie, tags, " +
                " date_publication, nbr_vues, likes, niveau, duree_lecture, url, email_auteur) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement pst = conx.prepareStatement(req);
        pst.setString(1,  r.getImageUrl());
        pst.setString(2,  r.getTitre());
        pst.setString(3,  r.getResume());
        pst.setString(4,  r.getContenu());
        pst.setString(5,  r.getCategorie());
        pst.setString(6,  r.getTags());
        pst.setDate(7,    new java.sql.Date(r.getDate_publication().getTime()));
        pst.setInt(8,     r.getNbr_vues());
        pst.setInt(9,     r.getLikes());
        pst.setString(10, r.getNiveau());
        pst.setInt(11,    r.getDuree_lecture());
        pst.setString(12, r.getUrl());
        pst.setString(13, r.getEmailAuteur());
        pst.executeUpdate();
        System.out.println("✅ Ressource ajoutée !");
    }

    @Override
    public void addMeth2(Ressources r) throws SQLException {
        add(r);
    }

    @Override
    public void modifier(Ressources r) throws SQLException {
        String req = "UPDATE ressource SET " +
                "image_url = ?, titre = ?, resume = ?, contenu = ?, " +
                "categorie = ?, tags = ?, date_publication = ?, " +
                "nbr_vues = ?, likes = ?, niveau = ?, " +
                "duree_lecture = ?, url = ?, email_auteur = ? " +
                "WHERE id_ressources = ?";

        PreparedStatement pst = conx.prepareStatement(req);
        pst.setString(1,  r.getImageUrl());
        pst.setString(2,  r.getTitre());
        pst.setString(3,  r.getResume());
        pst.setString(4,  r.getContenu());
        pst.setString(5,  r.getCategorie());
        pst.setString(6,  r.getTags());
        pst.setDate(7,    new java.sql.Date(r.getDate_publication().getTime()));
        pst.setInt(8,     r.getNbr_vues());
        pst.setInt(9,     r.getLikes());
        pst.setString(10, r.getNiveau());
        pst.setInt(11,    r.getDuree_lecture());
        pst.setString(12, r.getUrl());
        pst.setString(13, r.getEmailAuteur());
        pst.setInt(14,    r.getId_ressources());

        int rows = pst.executeUpdate();
        System.out.println(rows > 0 ? "✅ Ressource modifiée !" : "⚠️ Aucune ressource trouvée.");
    }

    @Override
    public void delete(Ressources r) throws SQLException {
        String req = "DELETE FROM ressource WHERE id_ressources = ?";
        PreparedStatement pst = conx.prepareStatement(req);
        pst.setInt(1, r.getId_ressources());
        int rows = pst.executeUpdate();
        System.out.println(rows > 0 ? "✅ Ressource supprimée !" : "⚠️ Aucune ressource trouvée.");
    }

    @Override
    public List<Ressources> afficherList() throws SQLException {
        String req = "SELECT * FROM ressource";
        Statement stm = conx.createStatement();
        ResultSet rs  = stm.executeQuery(req);
        List<Ressources> list = new ArrayList<>();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    private Ressources map(ResultSet rs) throws SQLException {
        return new Ressources(
                rs.getInt("id_ressources"),
                rs.getString("image_url"),
                rs.getString("titre"),
                rs.getString("resume"),
                rs.getString("contenu"),
                rs.getString("categorie"),
                rs.getString("tags"),
                rs.getDate("date_publication"),
                rs.getInt("nbr_vues"),
                rs.getInt("likes"),
                rs.getString("niveau"),
                rs.getInt("duree_lecture"),
                rs.getString("url"),
                rs.getString("email_auteur")
        );
    }

    public List<Ressources> getAllRessources() {
        List<Ressources> list = new ArrayList<>();
        String sql = "SELECT * FROM ressource ORDER BY date_publication DESC";
        try (Statement st = conx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Ressources> getRessourcesByType(String type) {
        List<Ressources> list = new ArrayList<>();
        String sql = "SELECT * FROM ressource WHERE contenu = ?";
        try (PreparedStatement pst = conx.prepareStatement(sql)) {
            pst.setString(1, type);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
            System.out.println("✅ " + list.size() + " ressource(s) de type " + type);
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public Ressources getRessourceById(int id) {
        String sql = "SELECT * FROM ressource WHERE id_ressources = ?";
        try (PreparedStatement pst = conx.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public void incrementerVues(int id) {
        String sql = "UPDATE ressource SET nbr_vues = nbr_vues + 1 WHERE id_ressources = ?";
        try (PreparedStatement pst = conx.prepareStatement(sql)) {
            pst.setInt(1, id);
            pst.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void incrementerLikes(int id) throws SQLException {
        String sql = "UPDATE ressource SET likes = likes + 1 WHERE id_ressources = ?";
        PreparedStatement pst = conx.prepareStatement(sql);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    public void decrementerLikes(int id) throws SQLException {
        String sql = "UPDATE ressource SET likes = GREATEST(likes - 1, 0) WHERE id_ressources = ?";
        PreparedStatement pst = conx.prepareStatement(sql);
        pst.setInt(1, id);
        pst.executeUpdate();
    }

    public List<Ressources> rechercherRessources(String motCle) {
        List<Ressources> list = new ArrayList<>();
        String sql = "SELECT * FROM ressource WHERE titre LIKE ? OR tags LIKE ?";
        try (PreparedStatement pst = conx.prepareStatement(sql)) {
            String p = "%" + motCle + "%";
            pst.setString(1, p);
            pst.setString(2, p);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<String> getAllEmailsAuteurs() {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT DISTINCT email_auteur FROM ressource WHERE email_auteur IS NOT NULL";
        try (PreparedStatement pst = conx.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) emails.add(rs.getString("email_auteur"));
        } catch (SQLException e) { e.printStackTrace(); }
        return emails;
    }

    public List<Ressources> getRessourcesPodcast() {
        return getRessourcesByType("Podcast");
    }

    public int getLikes(int id) {
        String sql = "SELECT likes FROM ressource WHERE id_ressources = ?";
        try (PreparedStatement pst = conx.prepareStatement(sql)) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("likes");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}