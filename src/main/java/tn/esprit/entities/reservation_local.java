package tn.esprit.entities;

import tn.esprit.enums.StatutReservation;
import tn.esprit.enums.MotifReservation;
import java.util.Date;
import java.util.List;

public class reservation_local {
    private int id_reservation;
    private int id_utilisateur;
    private int id_local;
    private int salle_id;          // FK → salle (0 = aucune salle réservée)
    private Date date_reservation;
    private Date heure_debut_reservation;
    private Date heure_fin_reservation;
    private StatutReservation status_reservation;
    private MotifReservation motif_reservation;
    private int prix_reservation;
    private String nom_cl;
    private String prenom_cl;

    // Constructeur vide
    public reservation_local() {
    }

    // Constructeur avec tous les paramètres (sans salle)
    public reservation_local(int id_reservation, int id_utilisateur, int id_local, Date date_reservation,
                             Date heure_debut_reservation, Date heure_fin_reservation, StatutReservation status_reservation,
                             MotifReservation motif_reservation, int prix_reservation, String nom_cl, String prenom_cl) {
        this.id_reservation = id_reservation;
        this.id_utilisateur = id_utilisateur;
        this.id_local = id_local;
        this.salle_id = 0;
        this.date_reservation = date_reservation;
        this.heure_debut_reservation = heure_debut_reservation;
        this.heure_fin_reservation = heure_fin_reservation;
        this.status_reservation = status_reservation;
        this.motif_reservation = motif_reservation;
        this.prix_reservation = prix_reservation;
        this.nom_cl = nom_cl;
        this.prenom_cl = prenom_cl;
    }

    // Constructeur avec salle_id
    public reservation_local(int id_reservation, int id_utilisateur, int id_local, int salle_id,
                             Date date_reservation, Date heure_debut_reservation, Date heure_fin_reservation,
                             StatutReservation status_reservation, MotifReservation motif_reservation,
                             int prix_reservation, String nom_cl, String prenom_cl) {
        this.id_reservation = id_reservation;
        this.id_utilisateur = id_utilisateur;
        this.id_local = id_local;
        this.salle_id = salle_id;
        this.date_reservation = date_reservation;
        this.heure_debut_reservation = heure_debut_reservation;
        this.heure_fin_reservation = heure_fin_reservation;
        this.status_reservation = status_reservation;
        this.motif_reservation = motif_reservation;
        this.prix_reservation = prix_reservation;
        this.nom_cl = nom_cl;
        this.prenom_cl = prenom_cl;
    }



    // Getters et Setters
    public int getId_reservation() {
        return id_reservation;
    }

    public void setId_reservation(int id_reservation) {
        this.id_reservation = id_reservation;
    }

    public int getId_utilisateur() {
        return id_utilisateur;
    }

    public void setId_utilisateur(int id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    public int getId_local() {
        return id_local;
    }

    public void setId_local(int id_local) {
        this.id_local = id_local;
    }

    public int getSalle_id() {
        return salle_id;
    }

    public void setSalle_id(int salle_id) {
        this.salle_id = salle_id;
    }

    public Date getDate_reservation() {
        return date_reservation;
    }

    public void setDate_reservation(Date date_reservation) {
        this.date_reservation = date_reservation;
    }

    public Date getHeure_debut_reservation() {
        return heure_debut_reservation;
    }

    public void setHeure_debut_reservation(Date heure_debut_reservation) {
        this.heure_debut_reservation = heure_debut_reservation;
    }

    public Date getHeure_fin_reservation() {
        return heure_fin_reservation;
    }

    public void setHeure_fin_reservation(Date heure_fin_reservation) {
        this.heure_fin_reservation = heure_fin_reservation;
    }

    public StatutReservation getStatus_reservation() {
        return status_reservation;
    }

    public void setStatus_reservation(StatutReservation status_reservation) {
        this.status_reservation = status_reservation;
    }

    public MotifReservation getMotif_reservation() {
        return motif_reservation;
    }

    public void setMotif_reservation(MotifReservation motif_reservation) {
        this.motif_reservation = motif_reservation;
    }

    public int getPrix_reservation() {
        return prix_reservation;
    }

    public void setPrix_reservation(int prix_reservation) {
        this.prix_reservation = prix_reservation;
    }

    public String getNom_cl() {
        return nom_cl;
    }

    public void setNom_cl(String nom_cl) {
        this.nom_cl = nom_cl;
    }

    public String getPrenom_cl() {
        return prenom_cl;
    }

    public void setPrenom_cl(String prenom_cl) {
        this.prenom_cl = prenom_cl;
    }

    @Override
    public String toString() {
        return "reservation{" +
                "id_reservation=" + id_reservation +
                ", id_utilisateur=" + id_utilisateur +
                ", id_local=" + id_local +
                ", salle_id=" + salle_id +
                ", date_reservation=" + date_reservation +
                ", heure_debut_reservation=" + heure_debut_reservation +
                ", heure_fin_reservation=" + heure_fin_reservation +
                ", status_reservation=" + status_reservation +
                ", motif_reservation=" + motif_reservation +
                ", prix_reservation=" + prix_reservation +
                ", nom_cl='" + nom_cl + '\'' +
                ", prenom_cl='" + prenom_cl + '\'' +
                '}';
    }

}