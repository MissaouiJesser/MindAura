package tn.esprit.services;

import tn.esprit.entities.salle;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class salle_SERVICE implements ICRUD<salle> {

    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;

    public salle_SERVICE() {
        conx = MyDataBase.getInstance().getConx();
    }

    // ── CREATE ─────────────────────────────────────────────────────────────────
    @Override
    public void add(salle s) throws SQLException {
        String req = "INSERT INTO salle (nom_salle, type_salle, capacite_salle, equipements, "
                + "disponibilite_salle, etage, image_url, statut_salle, local_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        pstm = conx.prepareStatement(req);
        pstm.setString(1, s.getNom_salle());
        pstm.setString(2, s.getType_salle());
        pstm.setString(3, s.getCapacite_salle());
        pstm.setString(4, s.getEquipements());
        pstm.setString(5, s.getDisponibilite_salle());
        pstm.setString(6, s.getEtage());
        pstm.setString(7, s.getImage_url());
        pstm.setString(8, s.getStatut_salle());
        pstm.setInt   (9, s.getId_local());
        pstm.executeUpdate();
        System.out.println("Salle ajoutee avec succes");
    }

    @Override
    public void addMeth2(salle s) throws SQLException {
        add(s);
    }

    // ── UPDATE ─────────────────────────────────────────────────────────────────
    @Override
    public void modifier(salle s) throws SQLException {
        String sql = "UPDATE salle SET nom_salle=?, type_salle=?, capacite_salle=?, " +
                "equipements=?, disponibilite_salle=?, etage=?, image_url=?, " +
                "statut_salle=?, local_id=? WHERE id_salle=?";
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setString(1,  s.getNom_salle());
            ps.setString(2,  s.getType_salle());
            ps.setString(3,  s.getCapacite_salle());
            ps.setString(4,  s.getEquipements());
            ps.setString(5,  s.getDisponibilite_salle());
            ps.setString(6,  s.getEtage());
            ps.setString(7,  s.getImage_url());
            ps.setString(8,  s.getStatut_salle());
            ps.setInt(9,     s.getId_local());
            ps.setInt(10,    s.getId_salle());
            ps.executeUpdate();
        }
    }

    // ── DELETE (par objet) ─────────────────────────────────────────────────────
    @Override
    public void delete(salle s) throws SQLException {
        delete(s.getId_salle());
    }

    // ── DELETE (par ID) ────────────────────────────────────────────────────────
    public void delete(int id) throws SQLException {
        pstm = conx.prepareStatement("DELETE FROM salle WHERE id_salle = ?");
        pstm.setInt(1, id);
        int rows = pstm.executeUpdate();
        System.out.println(rows > 0 ? "Salle supprimee" : "Aucune salle avec ID " + id);
    }

    // ── READ ALL ───────────────────────────────────────────────────────────────
    @Override
    public List<salle> afficherList() throws SQLException {
        List<salle> liste = new ArrayList<>();
        stm = conx.createStatement();
        ResultSet rs = stm.executeQuery("SELECT * FROM salle");
        while (rs.next()) {
            liste.add(mapRow(rs));
        }
        return liste;
    }

    // ── READ BY LOCAL ──────────────────────────────────────────────────────────
    public List<salle> afficherParLocal(int idLocal) throws SQLException {
        List<salle> salles = new ArrayList<>();
        String sql = "SELECT * FROM salle WHERE local_id = ?";   // ✅ local_id
        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, idLocal);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) salles.add(mapRow(rs));        // ✅ mapRow réutilisé
            }
        }
        return salles;
    }

    /** Alias pour compatibilité avec AfficherSalleController */
    public List<salle> getSallesByLocal(int idLocal) throws SQLException {
        return afficherParLocal(idLocal);
    }

    // ── READ ONE ───────────────────────────────────────────────────────────────
    public salle getById(int id) throws SQLException {
        pstm = conx.prepareStatement("SELECT * FROM salle WHERE id_salle = ?");
        pstm.setInt(1, id);
        ResultSet rs = pstm.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    // ── MAPPER — etage lu comme String ────────────────────────────────────────
    private salle mapRow(ResultSet rs) throws SQLException {
        return new salle(
                rs.getInt("id_salle"),
                rs.getString("nom_salle"),
                rs.getString("type_salle"),
                rs.getString("capacite_salle"),
                rs.getString("equipements"),
                rs.getString("disponibilite_salle"),
                rs.getString("etage"),
                rs.getString("image_url"),   // ✅ était "imag_url"
                rs.getString("statut_salle"),
                rs.getInt("local_id")
        );
    }
}