package tn.esprit.services;

import tn.esprit.controllers.ICRUD;
import tn.esprit.entities.Categorie;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategorieService implements ICRUD<Categorie> {

    private Connection conx;
    private Statement stm;
    private PreparedStatement pstm;

    public CategorieService() {
        conx = MyDataBase.getInstance().getConx();
    }

    // ===================== ADD (méthode simple) =====================
    @Override
    public void add(Categorie categorie) throws SQLException {

        String req = "INSERT INTO categorie (nom_categorie, description) " +
                "VALUES ('" + categorie.getNomCategorie() + "','" +
                categorie.getDescription() + "')";

        stm = conx.createStatement();
        stm.executeUpdate(req);
        System.out.println("Catégorie ajoutée");
    }

    // ===================== ADD (PreparedStatement) =====================
    @Override
    public void addMeth2(Categorie categorie) throws SQLException {

        String req = "INSERT INTO categorie (nom_categorie, description) VALUES (?,?)";

        pstm = conx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pstm.setString(1, categorie.getNomCategorie());
        pstm.setString(2, categorie.getDescription());
        pstm.executeUpdate();

        // Récupère l'ID généré automatiquement
        ResultSet keys = pstm.getGeneratedKeys();
        if (keys.next()) {
            categorie.setIdCategorie(keys.getInt(1));
        }

        System.out.println("Catégorie ajoutée (meth2) avec ID = " + categorie.getIdCategorie());
    }

    // ===================== UPDATE =====================
    @Override
    public void modifier(Categorie categorie) throws SQLException {

        String req = "UPDATE categorie SET nom_categorie=?, description=? WHERE id_categorie=?";

        pstm = conx.prepareStatement(req);
        pstm.setString(1, categorie.getNomCategorie());
        pstm.setString(2, categorie.getDescription());
        pstm.setInt(3, categorie.getIdCategorie());

        pstm.executeUpdate();
        System.out.println("Catégorie modifiée avec ID = " + categorie.getIdCategorie());
    }

    // ===================== DELETE =====================
    @Override
    public void delete(Categorie categorie) throws SQLException {

        String req = "DELETE FROM categorie WHERE id_categorie=?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, categorie.getIdCategorie());
        pstm.executeUpdate();

        System.out.println("Catégorie supprimée avec ID = " + categorie.getIdCategorie());
    }

    // ===================== SELECT ALL =====================
    @Override
    public List<Categorie> afficherList() throws SQLException {

        String req = "SELECT * FROM categorie ORDER BY nom_categorie";
        stm = conx.createStatement();
        ResultSet res = stm.executeQuery(req);

        List<Categorie> categories = new ArrayList<>();

        while (res.next()) {
            Categorie c = new Categorie(
                    res.getInt("id_categorie"),
                    res.getString("nom_categorie"),
                    res.getString("description"),
                    res.getTimestamp("date_creation")
            );
            categories.add(c);
        }

        return categories;
    }

    // ===================== SELECT PAR ID =====================
    public Categorie getById(int idCategorie) throws SQLException {

        String req = "SELECT * FROM categorie WHERE id_categorie = ?";
        pstm = conx.prepareStatement(req);
        pstm.setInt(1, idCategorie);
        ResultSet res = pstm.executeQuery();

        if (res.next()) {
            return new Categorie(
                    res.getInt("id_categorie"),
                    res.getString("nom_categorie"),
                    res.getString("description"),
                    res.getTimestamp("date_creation")
            );
        }
        return null;
    }
}