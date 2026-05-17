package tn.esprit.services;

import tn.esprit.entities.traitement;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class traitement_service implements ICRUD<traitement>{
    private Connection connection;
    private Statement stm;
    private PreparedStatement pstm;

    public traitement_service() {
        this.connection = MyDataBase.getInstance().getConx();
    }

    @Override
    public void add(traitement tr) throws SQLException {
        String req = "INSERT INTO traitement(date_debut_traitement, date_fin_traitement, objectif_traitement, " +
                "description_traitement, etat_traitement, type_traitement, id_utilisateur, id_coach) " +
                "VALUES ('" + new java.sql.Date(tr.getDate_debut_traitement().getTime()) + "','" +
                (tr.getDate_fin_traitement() != null ? new java.sql.Date(tr.getDate_fin_traitement().getTime()) : null) + "','" +
                tr.getObjectif_traitement().name() + "','" +
                (tr.getDescription_traitement() != null ? tr.getDescription_traitement() : "") + "','" +
                tr.getEtat_traitement().name() + "','" +
                tr.getType_traitement().name() + "','" +
                (tr.getId_utilisateur() != null ? tr.getId_utilisateur() : "") + "','" +
                (tr.getId_coach() != null ? tr.getId_coach() : "") + "')";

        this.connection = MyDataBase.getInstance().getConx();
        this.stm = this.connection.createStatement();
        this.stm.executeUpdate(req);
        System.out.println("Traitement ajouté avec succès (Méthode 1)");
    }

    @Override
    public void addMeth2(traitement tr) throws SQLException {
        String req = "INSERT INTO traitement(date_debut_traitement, date_fin_traitement, objectif_traitement, " +
                "description_traitement, etat_traitement, type_traitement, id_utilisateur, id_coach) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        this.connection = MyDataBase.getInstance().getConx();
        this.pstm = this.connection.prepareStatement(req);

        // date_debut_traitement
        this.pstm.setDate(1, new java.sql.Date(tr.getDate_debut_traitement().getTime()));

        // date_fin_traitement (nullable)
        if (tr.getDate_fin_traitement() != null) {
            this.pstm.setDate(2, new java.sql.Date(tr.getDate_fin_traitement().getTime()));
        } else {
            this.pstm.setNull(2, Types.DATE);
        }

        // objectif_traitement
        this.pstm.setString(3, tr.getObjectif_traitement().name());

        // description_traitement (nullable)
        if (tr.getDescription_traitement() != null) {
            this.pstm.setString(4, tr.getDescription_traitement());
        } else {
            this.pstm.setNull(4, Types.VARCHAR);
        }

        // etat_traitement
        this.pstm.setString(5, tr.getEtat_traitement().name());

        // type_traitement
        this.pstm.setString(6, tr.getType_traitement().name());

        // id_utilisateur (nullable)
        if (tr.getId_utilisateur() != null) {
            this.pstm.setString(7, tr.getId_utilisateur());
        } else {
            this.pstm.setNull(7, Types.VARCHAR);
        }

        // id_coach (nullable)
        if (tr.getId_coach() != null) {
            this.pstm.setString(8, tr.getId_coach());
        } else {
            this.pstm.setNull(8, Types.VARCHAR);
        }

        this.pstm.executeUpdate();
        System.out.println("Traitement ajouté avec succès (Méthode 2 - PreparedStatement)");
    }

    @Override
    public void modifier(traitement tr) throws SQLException {
        String req = "UPDATE traitement SET date_debut_traitement=?, date_fin_traitement=?, objectif_traitement=?, " +
                "description_traitement=?, etat_traitement=?, type_traitement=?, id_utilisateur=?, id_coach=? " +
                "WHERE id_traitement=?";

        this.connection = MyDataBase.getInstance().getConx();
        this.pstm = this.connection.prepareStatement(req);

        // date_debut_traitement
        this.pstm.setDate(1, new java.sql.Date(tr.getDate_debut_traitement().getTime()));

        // date_fin_traitement (nullable)
        if (tr.getDate_fin_traitement() != null) {
            this.pstm.setDate(2, new java.sql.Date(tr.getDate_fin_traitement().getTime()));
        } else {
            this.pstm.setNull(2, Types.DATE);
        }

        // objectif_traitement
        this.pstm.setString(3, tr.getObjectif_traitement().name());

        // description_traitement (nullable)
        if (tr.getDescription_traitement() != null) {
            this.pstm.setString(4, tr.getDescription_traitement());
        } else {
            this.pstm.setNull(4, Types.VARCHAR);
        }

        // etat_traitement
        this.pstm.setString(5, tr.getEtat_traitement().name());

        // type_traitement
        this.pstm.setString(6, tr.getType_traitement().name());

        // id_utilisateur (nullable)
        if (tr.getId_utilisateur() != null) {
            this.pstm.setString(7, tr.getId_utilisateur());
        } else {
            this.pstm.setNull(7, Types.VARCHAR);
        }

        // id_coach (nullable)
        if (tr.getId_coach() != null) {
            this.pstm.setString(8, tr.getId_coach());
        } else {
            this.pstm.setNull(8, Types.VARCHAR);
        }

        // id_traitement (WHERE clause)
        pstm.setString(9, tr.getId_traitement());

        int rowsAffected = this.pstm.executeUpdate();
        System.out.println(rowsAffected + " traitement(s) modifié(s) avec succès");
    }

    @Override
    public void delete(traitement tr) throws SQLException {
        String req = "DELETE FROM traitement WHERE id_traitement=?";

        this.connection = MyDataBase.getInstance().getConx();
        this.pstm = this.connection.prepareStatement(req);
        pstm.setString(1, tr.getId_traitement());

        int rowsAffected = this.pstm.executeUpdate();
        System.out.println(rowsAffected + " traitement(s) supprimé(s) avec succès");
    }

    @Override
    public List<traitement> afficherList() throws SQLException {
        List<traitement> traitements = new ArrayList<>();
        String req = "SELECT * FROM traitement";

        this.connection = MyDataBase.getInstance().getConx();
        this.stm = this.connection.createStatement();
        ResultSet res = this.stm.executeQuery(req);

        while (res.next()) {
            traitement tr = new traitement();
            tr.setId_traitement(String.valueOf(res.getLong("id_traitement")));
            tr.setDate_debut_traitement(res.getDate("date_debut_traitement"));
            tr.setDate_fin_traitement(res.getDate("date_fin_traitement"));
            tr.setObjectif_traitement(res.getString("objectif_traitement"));
            tr.setDescription_traitement(res.getString("description_traitement"));
            tr.setEtat_traitement(res.getString("etat_traitement"));
            tr.setType_traitement(res.getString("type_traitement"));
            tr.setId_utilisateur(res.getString("id_utilisateur"));
            tr.setId_coach(res.getString("id_coach"));

            traitements.add(tr);
        }

        return traitements;
    }
}