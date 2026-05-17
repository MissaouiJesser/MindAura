package tn.esprit.entities;

import java.util.Date;

public class Reclamation {

    private int idReclamation;             // id_reclamation          AUTO_INCREMENT
    private String sujetReclamation;       // sujet_reclamation       varchar(255)
    private String descriptionReclamation; // description_reclamation longtext
    private Date dateCreationReclamation;  // date_creation_reclamation datetime
    private String statutReclamation;      // statut_reclamation      varchar(255)
    private int utilisateurId;             // utilisateur_id          int(11)  ← ⚠️
    private double rateReclamation;        // rate_reclamation        double   ← ⚠️
    private int rateSum;                   // rate_sum                int(11)
    private int rateCount;                 // rate_count              int(11)
    private int categorieId;              // categorie_id            int(11)  ← ⚠️

    // ─── Constructeur complet (avec ID + rateSum/rateCount) ───────────────────
    public Reclamation(int idReclamation, String sujetReclamation,
                       String descriptionReclamation, Date dateCreationReclamation,
                       String statutReclamation, int utilisateurId,
                       double rateReclamation, int rateSum, int rateCount,
                       int categorieId) {
        this.idReclamation           = idReclamation;
        this.sujetReclamation        = sujetReclamation;
        this.descriptionReclamation  = descriptionReclamation;
        this.dateCreationReclamation = dateCreationReclamation;
        this.statutReclamation       = statutReclamation;
        this.utilisateurId           = utilisateurId;
        this.rateReclamation         = rateReclamation;
        this.rateSum                 = rateSum;
        this.rateCount               = rateCount;
        this.categorieId             = categorieId;
    }

    // ─── Constructeur complet sans rateSum/rateCount ──────────────────────────
    public Reclamation(int idReclamation, String sujetReclamation,
                       String descriptionReclamation, Date dateCreationReclamation,
                       String statutReclamation, int utilisateurId,
                       double rateReclamation, int categorieId) {
        this(idReclamation, sujetReclamation, descriptionReclamation,
                dateCreationReclamation, statutReclamation,
                utilisateurId, rateReclamation, 0, 0, categorieId);
    }

    // ─── Constructeur sans ID (pour insertion) ────────────────────────────────
    public Reclamation(String sujetReclamation, String descriptionReclamation,
                       Date dateCreationReclamation, String statutReclamation,
                       int utilisateurId, double rateReclamation, int categorieId) {
        this.sujetReclamation        = sujetReclamation;
        this.descriptionReclamation  = descriptionReclamation;
        this.dateCreationReclamation = dateCreationReclamation;
        this.statutReclamation       = statutReclamation;
        this.utilisateurId           = utilisateurId;
        this.rateReclamation         = rateReclamation;
        this.categorieId             = categorieId;
    }

    // ─── Constructeur simple ──────────────────────────────────────────────────
    public Reclamation(String sujetReclamation, int categorieId) {
        this.sujetReclamation        = sujetReclamation;
        this.categorieId             = categorieId;
        this.statutReclamation       = "EN_ATTENTE";
        this.dateCreationReclamation = new Date();
    }

    // ─── Constructeur vide ────────────────────────────────────────────────────
    public Reclamation() {}

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int getIdReclamation() { return idReclamation; }
    public void setIdReclamation(int idReclamation) { this.idReclamation = idReclamation; }

    public String getSujetReclamation() { return sujetReclamation; }
    public void setSujetReclamation(String sujetReclamation) { this.sujetReclamation = sujetReclamation; }

    public String getDescriptionReclamation() { return descriptionReclamation; }
    public void setDescriptionReclamation(String d) { this.descriptionReclamation = d; }

    public Date getDateCreationReclamation() { return dateCreationReclamation; }
    public void setDateCreationReclamation(Date d) { this.dateCreationReclamation = d; }

    public String getStatutReclamation() { return statutReclamation; }
    public void setStatutReclamation(String statutReclamation) { this.statutReclamation = statutReclamation; }

    /** Colonne {@code utilisateur_id} en base. */
    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    /** Retourne la moyenne calculée si des votes existent, sinon la valeur stockée. */
    public double getRateReclamation() {
        if (rateCount > 0) return (double) rateSum / rateCount;
        return rateReclamation;
    }
    public void setRateReclamation(double rateReclamation) { this.rateReclamation = rateReclamation; }

    public int getRateSum() { return rateSum; }
    public void setRateSum(int rateSum) { this.rateSum = rateSum; }

    public int getRateCount() { return rateCount; }
    public void setRateCount(int rateCount) { this.rateCount = rateCount; }

    /** Colonne {@code categorie_id} en base. */
    public int getCategorieId() { return categorieId; }
    public void setCategorieId(int categorieId) { this.categorieId = categorieId; }

    @Override
    public String toString() {
        return "Reclamation{" +
                "idReclamation=" + idReclamation +
                ", sujetReclamation='" + sujetReclamation + '\'' +
                ", statutReclamation='" + statutReclamation + '\'' +
                ", categorieId=" + categorieId +
                ", utilisateurId=" + utilisateurId +
                ", rateReclamation=" + getRateReclamation() +
                '}';
    }
}