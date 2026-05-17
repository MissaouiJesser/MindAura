package tn.esprit.entities;

import tn.esprit.enums.Etat;
import tn.esprit.enums.Objectif;
import tn.esprit.enums.TypeTraitement;

import java.util.Date;

public class traitement {
    private String id_traitement;
    private Date date_debut_traitement;
    private Date date_fin_traitement;
    private Objectif objectif_traitement;
    private String description_traitement;
    private Etat etat_traitement;
    private TypeTraitement type_traitement;
    private String id_utilisateur;
    private String id_coach;

    public traitement() {
    }

    public traitement(String id_traitement, Date date_debut_traitement, Date date_fin_traitement, Objectif objectif_traitement, String description_traitement, Etat etat_traitement, TypeTraitement type_traitement, String id_utilisateur, String id_coach) {
        this.id_traitement = id_traitement;
        this.date_debut_traitement = date_debut_traitement;
        this.date_fin_traitement = date_fin_traitement;
        this.objectif_traitement = objectif_traitement;
        this.description_traitement = description_traitement;
        this.etat_traitement = etat_traitement;
        this.type_traitement = type_traitement;
        this.id_utilisateur = id_utilisateur;
        this.id_coach = id_coach;
    }

    public String getId_traitement() {
        return id_traitement;
    }

    public void setId_traitement(String id_traitement) {
        this.id_traitement = id_traitement;
    }

    public Date getDate_debut_traitement() {
        return date_debut_traitement;
    }

    public void setDate_debut_traitement(Date date_debut_traitement) {
        this.date_debut_traitement = date_debut_traitement;
    }

    public Date getDate_fin_traitement() {
        return date_fin_traitement;
    }

    public void setDate_fin_traitement(Date date_fin_traitement) {
        this.date_fin_traitement = date_fin_traitement;
    }

    public Objectif getObjectif_traitement() {
        return objectif_traitement;
    }

    public void setObjectif_traitement(Objectif objectif_traitement) {
        this.objectif_traitement = objectif_traitement;
    }

    public void setObjectif_traitement(String objectifString) {
        this.objectif_traitement = Objectif.fromString(objectifString);
    }

    public String getDescription_traitement() {
        return description_traitement;
    }

    public void setDescription_traitement(String description_traitement) {
        this.description_traitement = description_traitement;
    }

    public Etat getEtat_traitement() {
        return etat_traitement;
    }

    public void setEtat_traitement(Etat etat_traitement) {
        this.etat_traitement = etat_traitement;
    }

    public void setEtat_traitement(String etatString) {
        this.etat_traitement = Etat.fromString(etatString);
    }

    public TypeTraitement getType_traitement() {
        return type_traitement;
    }

    public void setType_traitement(TypeTraitement type_traitement) {
        this.type_traitement = type_traitement;
    }

    public void setType_traitement(String typeString) {
        this.type_traitement = TypeTraitement.fromString(typeString);
    }

    public String getId_utilisateur() {
        return id_utilisateur;
    }

    public void setId_utilisateur(String id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    public String getId_coach() {
        return id_coach;
    }

    public void setId_coach(String id_coach) {
        this.id_coach = id_coach;
    }

    @Override
    public String toString() {
        return "traitement{" +
                "id_traitement='" + id_traitement + '\'' +
                ", date_debut_traitement=" + date_debut_traitement +
                ", date_fin_traitement=" + date_fin_traitement +
                ", objectif_traitement=" + objectif_traitement +
                ", description_traitement='" + description_traitement + '\'' +
                ", etat_traitement=" + etat_traitement +
                ", type_traitement=" + type_traitement +
                ", id_utilisateur='" + id_utilisateur + '\'' +
                ", id_coach='" + id_coach + '\'' +
                '}';
    }
}
