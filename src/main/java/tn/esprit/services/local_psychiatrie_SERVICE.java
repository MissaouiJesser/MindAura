package tn.esprit.services;
import tn.esprit.utils.MyDataBase;
import tn.esprit.enums.typeL;
import tn.esprit.entities.local_psychiatrie;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.*;
import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class local_psychiatrie_SERVICE implements ICRUD<local_psychiatrie> {

    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;
    public local_psychiatrie_SERVICE(){
        conx = MyDataBase.getInstance().getConx();
    }

    @Override
    public void add(local_psychiatrie localPsychiatrie) throws SQLException {

        String req = "INSERT INTO local_psychiatrie "
                + "(id_local, nom_local, adresse_local, ville_local, description_local, "
                + "capacite_local, type_local, telephone_local, email_local, "
                + "disponibilite_local, image_url) VALUES ("
                + localPsychiatrie.getId_local() + ", '"
                + localPsychiatrie.getNom_local() + "', '"
                + localPsychiatrie.getAdresse_local() + "', '"
                + localPsychiatrie.getVille_local() + "', '"
                + localPsychiatrie.getDescription_local() + "', '"
                + localPsychiatrie.getCapacite_local() + "', '"
                + localPsychiatrie.getType_local().name() + "', "
                + localPsychiatrie.getTelephone_local() + ", '"
                + localPsychiatrie.getEmail_local() + "', '"
                + localPsychiatrie.getDisponibilite_local() + "', '"
                + localPsychiatrie.getImageURL() + "')";


        stm = conx.createStatement();
        stm.executeUpdate(req);

        System.out.println("Local psychiatrie ajouté avec succès");

    }

    @Override
    public void addMeth2(local_psychiatrie var1) throws SQLException {

    }

    @Override
    public void delete(local_psychiatrie localPsychiatrie) throws SQLException {
        String req = "DELETE FROM local_psychiatrie WHERE id_local = ?";

        pstm = conx.prepareStatement(req);
        pstm.setInt(1, localPsychiatrie.getId_local());

        int rowsAffected = pstm.executeUpdate();

        if (rowsAffected > 0) {
            System.out.println("Local psychiatrie supprimé avec succès");
        } else {
            System.out.println("Aucun local trouvé avec l'ID: " + localPsychiatrie.getId_local());
        }

    }

    @Override
    public void modifier(local_psychiatrie localPsychiatrie) throws SQLException {
        System.out.println("DEBUG - ID à modifier: " + localPsychiatrie.getId_local());
        System.out.println("DEBUG - Nouveau nom: " + localPsychiatrie.getNom_local());

        String req = "UPDATE local_psychiatrie SET "
                + "nom_local = ?, "
                + "adresse_local = ?, "
                + "ville_local = ?, "
                + "description_local = ?, "
                + "capacite_local = ?, "
                + "type_local = ?, "
                + "telephone_local = ?, "
                + "email_local = ?, "
                + "disponibilite_local = ?, "
                + "image_url = ? "
                + "WHERE id_local = ?";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, localPsychiatrie.getNom_local());
        pstm.setString(2, localPsychiatrie.getAdresse_local());
        pstm.setString(3, localPsychiatrie.getVille_local());
        pstm.setString(4, localPsychiatrie.getDescription_local());
        pstm.setString(5, localPsychiatrie.getCapacite_local());
        pstm.setString(6, localPsychiatrie.getType_local().name());
        pstm.setInt(7, localPsychiatrie.getTelephone_local());
        pstm.setString(8, localPsychiatrie.getEmail_local());
        pstm.setString(9, localPsychiatrie.getDisponibilite_local());
        pstm.setString(10, localPsychiatrie.getImageURL());
        pstm.setInt(11, localPsychiatrie.getId_local());

        int rowsAffected = pstm.executeUpdate();

        System.out.println("DEBUG - Lignes affectées: " + rowsAffected);

        if (rowsAffected > 0) {
            System.out.println("Local psychiatrie modifié avec succès");
        } else {
            System.out.println("Aucun local trouvé avec l'ID: " + localPsychiatrie.getId_local());
        }

    }

    @Override
    public List<local_psychiatrie> afficherList() throws SQLException {

        List<local_psychiatrie> liste = new ArrayList<>();
        String req = "SELECT * FROM `local_psychiatrie`";
        stm = conx.createStatement();
        ResultSet rs = stm.executeQuery(req);


        while (rs.next()) {
            liste.add(new local_psychiatrie(
                    rs.getInt("id_local"),
                    rs.getString("nom_local"),
                    rs.getString("adresse_local"),
                    rs.getString("ville_local"),
                    rs.getString("description_local"),
                    rs.getString("capacite_local"),
                    typeL.fromString(rs.getString("type_local")),
                    rs.getInt("telephone_local"),
                    rs.getString("email_local"),
                    rs.getString("disponibilite_local"),
                    rs.getString("image_url") // ✅
            ));
        }

        return liste;
    }
}