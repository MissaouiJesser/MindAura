package tn.esprit.services;

import tn.esprit.entities.Participation;
import tn.esprit.entities.Evenements;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService implements ICRUD<Participation> {

    private final Connection conx;
    private final EvenementService evenementService;

    public ParticipationService() {
        conx = MyDataBase.getInstance().getConx();
        evenementService = new EvenementService();
    }

    // ── ICRUD ─────────────────────────────────────────────────────────────────

    @Override
    public void add(Participation p) throws SQLException {
        // Nettoyage nom/prénom
        if ((p.getPrenom() == null || p.getPrenom().isEmpty()) && p.getNom() != null) {
            String full = p.getNom().trim();
            int space = full.indexOf(' ');
            if (space > 0) {
                p.setNom(full.substring(0, space).trim());
                p.setPrenom(full.substring(space + 1).trim());
            } else {
                p.setPrenom("-");
            }
        }
        if (p.getPrenom() == null || p.getPrenom().isEmpty()) p.setPrenom("-");
        if (p.getNom() == null || p.getNom().isEmpty()) p.setNom("-");

        // Récupérer l'événement et vérifier sa capacité
        Evenements evenement = evenementService.findById(p.getEvenementId());
        if (evenement == null) {
            throw new SQLException("L'événement ID=" + p.getEvenementId() + " n'existe pas !");
        }
        int capaciteActuelle = evenement.getCapaciteEvenement();
        if (capaciteActuelle <= 0) {
            throw new SQLException("Capacité insuffisante pour cet événement !");
        }

        // Vérifier doublon email + événement
        if (p.getEmail() != null && !p.getEmail().isEmpty()) {
            String checkDouble = "SELECT COUNT(*) FROM participation WHERE evenement_id = ? AND email = ?";
            try (PreparedStatement ds = conx.prepareStatement(checkDouble)) {
                ds.setInt(1, p.getEvenementId());
                ds.setString(2, p.getEmail());
                ResultSet rd = ds.executeQuery();
                if (rd.next() && rd.getInt(1) > 0) {
                    throw new SQLException("Cet email est déjà inscrit à cet événement !");
                }
            }
        }

        // Transaction : insertion + mise à jour capacité
        boolean autoCommit = conx.getAutoCommit();
        conx.setAutoCommit(false);
        try {
            // 1. Insérer la participation
            String req = "INSERT INTO participation " +
                    "(nom, prenom, email, telephone, date_inscription, code_qr, statut, position_attente, evenement_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement pstm = conx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
                pstm.setString(1, p.getNom());
                pstm.setString(2, p.getPrenom());
                pstm.setString(3, p.getEmail() != null ? p.getEmail() : "");
                pstm.setString(4, p.getTelephone());
                pstm.setTimestamp(5, p.getDateInscription() != null
                        ? new Timestamp(p.getDateInscription().getTime())
                        : new Timestamp(System.currentTimeMillis()));
                pstm.setString(6, p.getCodeQr());
                pstm.setString(7, p.getStatut() != null && !p.getStatut().isEmpty()
                        ? p.getStatut() : "confirmee");
                if (p.getPositionAttente() != null) {
                    pstm.setInt(8, p.getPositionAttente());
                } else {
                    pstm.setNull(8, Types.INTEGER);
                }
                pstm.setInt(9, p.getEvenementId());
                pstm.executeUpdate();

                ResultSet keys = pstm.getGeneratedKeys();
                if (keys.next()) p.setId(keys.getInt(1));
            }

            // 2. Diminuer la capacité de l'événement
            evenement.setCapaciteEvenement(capaciteActuelle - 1);
            evenementService.modifierEvenement(evenement);

            conx.commit();
        } catch (SQLException e) {
            conx.rollback();
            throw e;
        } finally {
            conx.setAutoCommit(autoCommit);
        }
    }

    @Override
    public void addMeth2(Participation p) throws SQLException {
        add(p);
    }

    @Override
    public void modifier(Participation p) throws SQLException {
        if ((p.getPrenom() == null || p.getPrenom().isEmpty()) && p.getNom() != null) {
            String full = p.getNom().trim();
            int space = full.indexOf(' ');
            if (space > 0) {
                p.setNom(full.substring(0, space).trim());
                p.setPrenom(full.substring(space + 1).trim());
            } else {
                p.setPrenom("-");
            }
        }
        if (p.getPrenom() == null || p.getPrenom().isEmpty()) p.setPrenom("-");

        String req = "UPDATE participation SET " +
                "nom=?, prenom=?, email=?, telephone=?, " +
                "statut=?, position_attente=?, evenement_id=? " +
                "WHERE id=?";
        try (PreparedStatement pstm = conx.prepareStatement(req)) {
            pstm.setString(1, p.getNom());
            pstm.setString(2, p.getPrenom());
            pstm.setString(3, p.getEmail());
            pstm.setString(4, p.getTelephone());
            pstm.setString(5, p.getStatut());
            if (p.getPositionAttente() != null) {
                pstm.setInt(6, p.getPositionAttente());
            } else {
                pstm.setNull(6, Types.INTEGER);
            }
            pstm.setInt(7, p.getEvenementId());
            pstm.setInt(8, p.getId());
            pstm.executeUpdate();
        }
    }

    @Override
    public void delete(Participation p) throws SQLException {
        // Récupérer l'événement associé avant suppression
        Evenements evenement = evenementService.findById(p.getEvenementId());
        if (evenement == null) {
            throw new SQLException("Événement introuvable pour la participation ID=" + p.getId());
        }

        // Transaction : suppression + augmentation capacité
        boolean autoCommit = conx.getAutoCommit();
        conx.setAutoCommit(false);
        try {
            // 1. Supprimer la participation
            String req = "DELETE FROM participation WHERE id=?";
            try (PreparedStatement pstm = conx.prepareStatement(req)) {
                pstm.setInt(1, p.getId());
                pstm.executeUpdate();
            }

            // 2. Augmenter la capacité
            evenement.setCapaciteEvenement(evenement.getCapaciteEvenement() + 1);
            evenementService.modifierEvenement(evenement);

            conx.commit();
        } catch (SQLException e) {
            conx.rollback();
            throw e;
        } finally {
            conx.setAutoCommit(autoCommit);
        }
    }

    @Override
    public List<Participation> afficherList() throws SQLException {
        List<Participation> list = new ArrayList<>();
        String req = "SELECT p.*, e.titre_evenement " +
                "FROM participation p " +
                "JOIN evenement e ON p.evenement_id = e.id " +
                "ORDER BY p.date_inscription DESC";
        try (PreparedStatement pstm = conx.prepareStatement(req);
             ResultSet rs = pstm.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    // ── Méthodes utilitaires ──────────────────────────────────────────────────

    public List<Participation> afficherListByEvenement(int idEvenement) throws SQLException {
        List<Participation> list = new ArrayList<>();
        String req = "SELECT p.*, e.titre_evenement " +
                "FROM participation p " +
                "JOIN evenement e ON p.evenement_id = e.id " +
                "WHERE p.evenement_id = ?";
        try (PreparedStatement pstm = conx.prepareStatement(req)) {
            pstm.setInt(1, idEvenement);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public int countTotal() throws SQLException {
        try (PreparedStatement ps = conx.prepareStatement("SELECT COUNT(*) FROM participation");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countConfirmees() throws SQLException {
        try (PreparedStatement ps = conx.prepareStatement(
                "SELECT COUNT(*) FROM participation WHERE statut = 'confirmee'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countByEvent(int eventId) throws SQLException {
        try (PreparedStatement ps = conx.prepareStatement(
                "SELECT COUNT(*) FROM participation WHERE evenement_id = ?")) {
            ps.setInt(1, eventId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public boolean isParticipantByEmail(String email, int eventId) throws SQLException {
        try (PreparedStatement ps = conx.prepareStatement(
                "SELECT COUNT(*) FROM participation WHERE email = ? AND evenement_id = ?")) {
            ps.setString(1, email);
            ps.setInt(2, eventId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public boolean isParticipantByName(String nom, int eventId) throws SQLException {
        try (PreparedStatement ps = conx.prepareStatement(
                "SELECT COUNT(*) FROM participation WHERE nom = ? AND evenement_id = ?")) {
            ps.setString(1, nom);
            ps.setInt(2, eventId);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // Alias pour compatibilité avec l'ancien code
    public List<Participation> recuperer() throws SQLException {
        return afficherList();
    }

    public void supprimer(Participation p) throws SQLException {
        delete(p);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Participation mapRow(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getInt("id"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setEmail(rs.getString("email"));
        p.setTelephone(rs.getString("telephone"));
        p.setDateInscription(rs.getTimestamp("date_inscription"));
        p.setCodeQr(rs.getString("code_qr"));
        p.setStatut(rs.getString("statut"));
        p.setPositionAttente((Integer) rs.getObject("position_attente"));
        p.setEvenementId(rs.getInt("evenement_id"));
        try {
            p.setNomEvenement(rs.getString("titre_evenement"));
        } catch (SQLException ignored) {
        }
        return p;
    }
}