package tn.esprit.services;

import tn.esprit.entities.reservation_local;
import tn.esprit.utils.MyDataBase;
import tn.esprit.enums.StatutReservation;
import tn.esprit.enums.MotifReservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class reservation_local_SERVICE implements ICRUD<reservation_local>{
    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;

    public reservation_local_SERVICE() {
        conx = MyDataBase.getInstance().getConx();
    }
    @Override
    public void add(reservation_local reservationLocal) throws SQLException {
        String req = "INSERT INTO reservation_local"
                + "(id_utilisateur, local_id, salle_id, date_reservation, heure_debut_reservation, "
                + "heure_fin_reservation, status_reservation, motif_reservation, prix_reservation, "
                + "nom_cl, prenom_cl) VALUES ("
                + reservationLocal.getId_utilisateur() + ", "
                + reservationLocal.getId_local() + ", "
                + reservationLocal.getSalle_id() + ", '"
                + new java.sql.Date(reservationLocal.getDate_reservation().getTime()) + "', '"
                + new java.sql.Timestamp(reservationLocal.getHeure_debut_reservation().getTime()) + "', '"
                + new java.sql.Timestamp(reservationLocal.getHeure_fin_reservation().getTime()) + "', '"
                + reservationLocal.getStatus_reservation().name() + "', '"
                + reservationLocal.getMotif_reservation().name() + "', "
                + reservationLocal.getPrix_reservation() + ", '"
                + (reservationLocal.getNom_cl() != null ? reservationLocal.getNom_cl() : "") + "', '"
                + (reservationLocal.getPrenom_cl() != null ? reservationLocal.getPrenom_cl() : "") + "')";

        stm = conx.createStatement();
        stm.executeUpdate(req);
        System.out.println("Réservation ajoutée avec succès");

    }

    @Override
    public void addMeth2(reservation_local var1) throws SQLException {

    }

    @Override
    public void delete(reservation_local reservationLocal) throws SQLException {
        String req = "DELETE FROM reservation_local WHERE id_reservation = ?";

        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reservationLocal.getId_reservation());

        int rowsAffected = pstm.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Réservation supprimée avec succès");
        } else {
            System.out.println("Aucune réservation trouvée avec l'ID: " + reservationLocal.getId_reservation());
        }

    }

    @Override
    public void modifier(reservation_local reservationLocal) throws SQLException {
        System.out.println("DEBUG - ID à modifier: " + reservationLocal.getId_reservation());

        String req = "UPDATE reservation_local SET "
                + "id_utilisateur = ?, "
                + "local_id= ?, "
                + "salle_id = ?, "
                + "date_reservation = ?, "
                + "heure_debut_reservation = ?, "
                + "heure_fin_reservation = ?, "
                + "status_reservation = ?, "
                + "motif_reservation = ?, "
                + "prix_reservation = ?, "
                + "nom_cl = ?, "
                + "prenom_cl = ? "
                + "WHERE id_reservation = ?";

        pstm = conx.prepareStatement(req);
        pstm.setInt(1, reservationLocal.getId_utilisateur());
        pstm.setInt(2, reservationLocal.getId_local());
        pstm.setInt(3, reservationLocal.getSalle_id());
        pstm.setDate(4, new java.sql.Date(reservationLocal.getDate_reservation().getTime()));
        pstm.setTimestamp(5, new java.sql.Timestamp(reservationLocal.getHeure_debut_reservation().getTime()));
        pstm.setTimestamp(6, new java.sql.Timestamp(reservationLocal.getHeure_fin_reservation().getTime()));
        pstm.setString(7, reservationLocal.getStatus_reservation().name());
        pstm.setString(8, reservationLocal.getMotif_reservation().name());
        pstm.setInt(9, reservationLocal.getPrix_reservation());

        if (reservationLocal.getNom_cl() != null) {
            pstm.setString(10, reservationLocal.getNom_cl());
        } else {
            pstm.setNull(10, Types.VARCHAR);
        }

        if (reservationLocal.getPrenom_cl() != null) {
            pstm.setString(11, reservationLocal.getPrenom_cl());
        } else {
            pstm.setNull(11, Types.VARCHAR);
        }

        pstm.setInt(12, reservationLocal.getId_reservation());

        int rowsAffected = pstm.executeUpdate();

        System.out.println("DEBUG - Lignes affectées: " + rowsAffected);

        if (rowsAffected > 0) {
            System.out.println("Réservation modifiée avec succès");
        } else {
            System.out.println("Aucune réservation trouvée avec l'ID: " + reservationLocal.getId_reservation());
        }

    }

    @Override
    public List<reservation_local> afficherList() throws SQLException {
        List<reservation_local> liste = new ArrayList<>();
        String req = "SELECT id_reservation, id_utilisateur, local_id, salle_id, date_reservation, "
                + "heure_debut_reservation, heure_fin_reservation, status_reservation, "
                + "motif_reservation, prix_reservation, nom_cl, prenom_cl "
                + "FROM reservation_local";
        stm = conx.createStatement();
        ResultSet rs = stm.executeQuery(req);

        while (rs.next()) {
            liste.add(new reservation_local(
                    rs.getInt("id_reservation"),
                    rs.getInt("id_utilisateur"),
                    rs.getInt("local_id"),
                    rs.getInt("salle_id"),
                    rs.getDate("date_reservation"),
                    rs.getTimestamp("heure_debut_reservation"),
                    rs.getTimestamp("heure_fin_reservation"),
                    StatutReservation.fromString(rs.getString("status_reservation")),
                    MotifReservation.fromString(rs.getString("motif_reservation")),
                    rs.getInt("prix_reservation"),
                    rs.getString("nom_cl"),
                    rs.getString("prenom_cl")
            ));
        }
        return liste;
    }
    // Méthode supplémentaire pour vérifier la disponibilité d'un local
    /*public boolean verifierDisponibilite(int id_local, Date heure_debut, Date heure_fin) throws SQLException {
        String req = "SELECT COUNT(*) as count FROM reservation WHERE id_local = ? " +
                "AND status_reservation != 'ANNULEE' " +
                "AND ((heure_debut_reservation < ? AND heure_fin_reservation > ?) " +
                "OR (heure_debut_reservation < ? AND heure_fin_reservation > ?) " +
                "OR (heure_debut_reservation >= ? AND heure_fin_reservation <= ?))";

        pstm = conx.prepareStatement(req);
        pstm.setInt(1, id_local);
        pstm.setTimestamp(2, new java.sql.Timestamp(heure_fin.getTime()));
        pstm.setTimestamp(3, new java.sql.Timestamp(heure_debut.getTime()));
        pstm.setTimestamp(4, new java.sql.Timestamp(heure_fin.getTime()));
        pstm.setTimestamp(5, new java.sql.Timestamp(heure_fin.getTime()));
        pstm.setTimestamp(6, new java.sql.Timestamp(heure_debut.getTime()));
        pstm.setTimestamp(7, new java.sql.Timestamp(heure_fin.getTime()));

        ResultSet rs = pstm.executeQuery();
        if (rs.next()) {
            return rs.getInt("count") == 0; // Retourne true si aucune réservation conflictuelle
        }
        return true;
    }

    // Méthode pour annuler une réservation
    public void annulerReservation(reservation_local res) throws SQLException {
        res.setStatus_reservation(StatutReservation.ANNULEE);
        modifier_local(res);
        System.out.println("Réservation annulée avec succès");
    }

    // Méthode pour confirmer une réservation
    public void confirmerReservation(reservation_local res) throws SQLException {
        res.setStatus_reservation(StatutReservation.CONFIRMEE);
        modifier_local(res);
        System.out.println("Réservation confirmée avec succès");
    }

    // Méthode pour terminer une réservation
    public void terminerReservation(reservation_local res) throws SQLException {
        res.setStatus_reservation(StatutReservation.TERMINEE);
        modifier_local(res);
        System.out.println("Réservation terminée avec succès");
    }*/
}