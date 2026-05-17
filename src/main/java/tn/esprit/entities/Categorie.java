package tn.esprit.entities;

import java.sql.Timestamp;

public class Categorie {

    private int idCategorie;
    private String nomCategorie;
    private String description;
    private Timestamp dateCreation;

    // 🔹 Constructeur complet (avec ID)
    public Categorie(int idCategorie, String nomCategorie, String description, Timestamp dateCreation) {
        this.idCategorie = idCategorie;
        this.nomCategorie = nomCategorie;
        this.description = description;
        this.dateCreation = dateCreation;
    }

    // 🔹 Constructeur sans ID (pour insertion)
    public Categorie(String nomCategorie, String description) {
        this.nomCategorie = nomCategorie;
        this.description = description;
    }

    // 🔹 Constructeur vide
    public Categorie() {}

    // 🔹 Getters & Setters

    public int getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(int idCategorie) {
        this.idCategorie = idCategorie;
    }

    public String getNomCategorie() {
        return nomCategorie;
    }

    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Timestamp dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Categorie{" +
                "idCategorie=" + idCategorie +
                ", nomCategorie='" + nomCategorie + '\'' +
                ", description='" + description + '\'' +
                ", dateCreation=" + dateCreation +
                '}';
    }
}