package tn.esprit.services;

import tn.esprit.entities.reservation_historique;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour :
 *  1. Enregistrer l'historique des modifications de réservations
 *  2. Calculer les statistiques d'occupation par local (pour le tableau de bord)
 */
public class ReservationHistoriqueService {

    private final Connection connection;

    public ReservationHistoriqueService() {
        this.connection= MyDataBase.getInstance().getConx();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  GESTION DE L'HISTORIQUE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Enregistre UNE ligne dans reservation_historique.
     *
     * @param historique l'objet à persister
     */
    public void enregistrerModification(reservation_historique historique) throws SQLException {
        String sql = "INSERT INTO reservation_historique " +
                "(id_reservation, champ_modifie, ancienne_valeur, nouvelle_valeur, " +
                " modifie_par, date_modification, commentaire) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, historique.getId_reservation());
            ps.setString(2, historique.getChamp_modifie());
            ps.setString(3, historique.getAncienne_valeur());
            ps.setString(4, historique.getNouvelle_valeur());
            ps.setString(5, historique.getModifie_par() != null ? historique.getModifie_par() : "Admin");
            ps.setString(6, historique.getCommentaire());
            ps.executeUpdate();
        }
    }

    /**
     * Enregistre PLUSIEURS modifications d'un coup (batch) — pratique pour la
     * méthode handleModifier() qui peut changer plusieurs champs à la fois.
     */
    public void enregistrerModifications(List<reservation_historique> liste) throws SQLException {
        for (reservation_historique h : liste) {
            enregistrerModification(h);
        }
    }

    /**
     * Retourne TOUT l'historique trié du plus récent au plus ancien.
     */
    public List<reservation_historique> getToutHistorique() throws SQLException {
        List<reservation_historique> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservation_historique ORDER BY date_modification DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                liste.add(mapRow(rs));
            }
        }
        return liste;
    }

    /**
     * Retourne l'historique d'UNE réservation précise.
     */
    public List<reservation_historique> getHistoriqueParReservation(int idReservation) throws SQLException {
        List<reservation_historique> liste = new ArrayList<>();
        String sql = "SELECT * FROM reservation_historique " +
                "WHERE id_reservation = ? ORDER BY date_modification DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idReservation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapRow(rs));
                }
            }
        }
        return liste;
    }

    /** Helper : transforme un ResultSet row en objet reservation_historique. */
    private reservation_historique mapRow(ResultSet rs) throws SQLException {
        reservation_historique h = new reservation_historique();
        h.setId_historique(rs.getInt("id_historique"));
        h.setId_reservation(rs.getInt("id_reservation"));
        h.setChamp_modifie(rs.getString("champ_modifie"));
        h.setAncienne_valeur(rs.getString("ancienne_valeur"));
        h.setNouvelle_valeur(rs.getString("nouvelle_valeur"));
        h.setModifie_par(rs.getString("modifie_par"));
        h.setDate_modification(rs.getTimestamp("date_modification"));
        h.setCommentaire(rs.getString("commentaire"));
        return h;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STATISTIQUES POUR LE TABLEAU DE BORD
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Taux d'occupation par local :
     *   nombre de réservations CONFIRMEES ou EN_ATTENTE pour chaque local.
     *
     * Retourne une Map<NomLocal, NbReservations> triée par ordre de création.
     */
    public Map<String, Integer> getTauxOccupationParLocal() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql =
                "SELECT lp.nom_local, COUNT(r.id_reservation) AS nb " +
                        "FROM local_psychiatrie lp " +
                        "LEFT JOIN reservation_local r " +
                        "  ON lp.id_local = r.id_local " +
                        "  AND r.status_reservation IN ('CONFIRMEE','EN_ATTENTE') " +
                        "GROUP BY lp.id_local, lp.nom_local " +
                        "ORDER BY nb DESC";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("nom_local"), rs.getInt("nb"));
            }
        }
        return map;
    }

    /**
     * Répartition des réservations par STATUT.
     * Retourne une Map<libelle_statut, count>.
     */
    public Map<String, Integer> getReservationsParStatut() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT status_reservation, COUNT(*) AS nb " +
                "FROM reservation_local GROUP BY status_reservation";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("status_reservation"), rs.getInt("nb"));
            }
        }
        return map;
    }

    /**
     * Nombre total de réservations dans la base.
     */
    public int getTotalReservations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation_local";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Nombre total de locaux dans la base.
     */
    public int getTotalLocaux() throws SQLException {
        String sql = "SELECT COUNT(*) FROM local_psychiatrie";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Nombre de locaux actuellement DISPONIBLES.
     */
    public int getLocauxDisponibles() throws SQLException {
        String sql = "SELECT COUNT(*) FROM local_psychiatrie WHERE disponibilite_local = 'Disponible'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Nombre de réservations CONFIRMÉES.
     */
    public int getReservationsConfirmees() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reservation_local WHERE status_reservation = 'CONFIRMEE'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  NOUVELLES STATISTIQUES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Répartition des réservations par MOTIF (camembert).
     * Retourne Map<LibelleMotif, NbReservations>.
     */
    public Map<String, Integer> getReservationsParMotif() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT motif_reservation, COUNT(*) AS nb " +
                "FROM reservation_local " +
                "GROUP BY motif_reservation " +
                "ORDER BY nb DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("motif_reservation"), rs.getInt("nb"));
            }
        }
        return map;
    }

    /**
     * Revenu total : somme des prix des réservations CONFIRMÉES uniquement.
     */
    public double getRevenuTotal() throws SQLException {
        String sql = "SELECT COALESCE(SUM(prix_reservation), 0) " +
                "FROM reservation_local WHERE status_reservation = 'CONFIRMEE'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    /**
     * Revenu moyen par réservation (toutes réservations confondues).
     */
    public double getRevenuMoyen() throws SQLException {
        String sql = "SELECT COALESCE(AVG(prix_reservation), 0) FROM reservation_local";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0;
        }
    }

    /**
     * Nombre de modifications par réservation — les plus retouchées en premier.
     * Retourne Map<"Réserv. #ID (Nom Prénom)", NbModifications>.
     */
    public Map<String, Integer> getModificationsParReservation() throws SQLException {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql =
                "SELECT h.id_reservation, " +
                        "       COALESCE(r.nom_cl, '?') AS nom, " +
                        "       COALESCE(r.prenom_cl, '') AS prenom, " +
                        "       COUNT(h.id_historique) AS nb " +
                        "FROM reservation_historique h " +
                        "LEFT JOIN reservation_local r ON h.id_reservation = r.id_reservation " +
                        "GROUP BY h.id_reservation, r.nom_cl, r.prenom_cl " +
                        "ORDER BY nb DESC " +
                        "LIMIT 10";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String label = "#" + rs.getInt("id_reservation") +
                        " — " + rs.getString("nom") + " " + rs.getString("prenom");
                map.put(label, rs.getInt("nb"));
            }
        }
        return map;
    }
}