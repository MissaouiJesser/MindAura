package tn.esprit.entities;

import java.util.Date;

/**
 * Entité alignée sur la table `participation`.
 *
 * Colonnes DB :
 *   id, nom, prenom, email, telephone, date_inscription,
 *   code_qr, statut, position_attente, evenement_id
 */
public class Participation {

    private int     id;
    private String  nom;
    private String  prenom;
    private String  email;
    private String  telephone;
    private Date    dateInscription;
    private String  codeQr;
    private String  statut;
    private Integer positionAttente;
    private int     evenementId;

    /** Champ de jointure (non persisté) – titre affiché dans les tableaux */
    private String nomEvenement;

    // ── Constructeurs ──────────────────────────────────────────────────────────

    public Participation() {}

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public int getId()              { return id; }
    public void setId(int id)       { this.id = id; }

    /** Alias de compatibilité */
    public int getId_participation()            { return id; }
    public void setId_participation(int id)     { this.id = id; }

    public String getNom()              { return nom; }
    public void setNom(String nom)      { this.nom = nom; }

    public String getPrenom()               { return prenom; }
    public void setPrenom(String prenom)    { this.prenom = prenom; }

    /** Renvoie "nom prénom" – alias de compatibilité */
    public String getNom_participation() {
        return (nom != null ? nom : "") + " " + (prenom != null ? prenom : "");
    }
    public void setNom_participation(String v) { this.nom = v; }

    public String getEmail()                { return email; }
    public void setEmail(String email)      { this.email = email; }

    public String getTelephone()                    { return telephone; }
    public void setTelephone(String telephone)      { this.telephone = telephone; }

    public Date getDateInscription()                        { return dateInscription; }
    public void setDateInscription(Date dateInscription)    { this.dateInscription = dateInscription; }

    public String getCodeQr()               { return codeQr; }
    public void setCodeQr(String codeQr)    { this.codeQr = codeQr; }

    public String getStatut()               { return statut; }
    public void setStatut(String statut)    { this.statut = statut; }

    /** Alias de compatibilité */
    public String getStatutParticipation()          { return statut; }
    public void setStatutParticipation(String v)    { this.statut = v; }

    /** Colonne position_attente (nullable) */
    public Integer getPositionAttente()                         { return positionAttente; }
    public void setPositionAttente(Integer positionAttente)     { this.positionAttente = positionAttente; }

    public int getEvenementId()                 { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }

    /** Alias de compatibilité avec l'ancien code getId_evenemnt() */
    public int getId_evenemnt()             { return evenementId; }
    public void setId_evenemnt(int v)       { this.evenementId = v; }

    public String getNomEvenement()                     { return nomEvenement; }
    public void setNomEvenement(String nomEvenement)    { this.nomEvenement = nomEvenement; }

    /** Alias de compatibilité */
    public String getNom_evenement()        { return nomEvenement; }
    public void setNom_evenement(String v)  { this.nomEvenement = v; }

    public boolean isEnAttente() { return "en_attente".equals(statut); }

    // ── Alias de compatibilité – anciens champs supprimés ─────────────────────

    /** Ancien champ utilisateur_id supprimé – conservé pour éviter erreurs de compilation */
    public void setUtilisateur_id(int v)    { /* ignoré */ }
    public int  getUtilisateur_id()         { return 0; }

    /** Ancien champ feedback supprimé – conservé pour éviter erreurs de compilation */
    public void setFeedback(String v)       { /* ignoré */ }
    public String getFeedback()             { return ""; }

    @Override
    public String toString() {
        return "Participation{id=" + id + ", nom='" + nom + " " + prenom
                + "', statut='" + statut + "'}";
    }
}