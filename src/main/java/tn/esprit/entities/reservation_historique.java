package tn.esprit.entities;

import java.util.Date;

/**
 * Entité représentant une entrée dans l'historique des modifications d'une réservation.
 * Correspond à la table `reservation_historique` en base de données.
 */
public class reservation_historique {

    private int    id_historique;
    private int    id_reservation;
    private String champ_modifie;
    private String ancienne_valeur;
    private String nouvelle_valeur;
    private String modifie_par;
    private Date   date_modification;
    private String commentaire;

    // ─── Constructeurs ─────────────────────────────────────────────────────────

    public reservation_historique() {}

    /** Constructeur principal utilisé lors de l'enregistrement d'une modification. */
    public reservation_historique(int idReservation, String champModifie,
                                  String ancienneValeur, String nouvelleValeur,
                                  String modifiePar) {
        this.id_reservation  = idReservation;
        this.champ_modifie   = champModifie;
        this.ancienne_valeur = ancienneValeur;
        this.nouvelle_valeur = nouvelleValeur;
        this.modifie_par     = modifiePar;
        this.date_modification = new Date();
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────────

    public int getId_historique()               { return id_historique; }
    public void setId_historique(int v)         { this.id_historique = v; }

    public int getId_reservation()              { return id_reservation; }
    public void setId_reservation(int v)        { this.id_reservation = v; }

    public String getChamp_modifie()            { return champ_modifie; }
    public void setChamp_modifie(String v)      { this.champ_modifie = v; }

    public String getAncienne_valeur()          { return ancienne_valeur; }
    public void setAncienne_valeur(String v)    { this.ancienne_valeur = v; }

    public String getNouvelle_valeur()          { return nouvelle_valeur; }
    public void setNouvelle_valeur(String v)    { this.nouvelle_valeur = v; }

    public String getModifie_par()              { return modifie_par; }
    public void setModifie_par(String v)        { this.modifie_par = v; }

    public Date getDate_modification()          { return date_modification; }
    public void setDate_modification(Date v)    { this.date_modification = v; }

    public String getCommentaire()              { return commentaire; }
    public void setCommentaire(String v)        { this.commentaire = v; }

    @Override
    public String toString() {
        return "Historique{id=" + id_historique +
                ", reservation=" + id_reservation +
                ", champ='" + champ_modifie + "'" +
                ", " + ancienne_valeur + " → " + nouvelle_valeur +
                ", par='" + modifie_par + "'" +
                ", date=" + date_modification + "}";
    }
}