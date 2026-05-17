package tn.esprit.entities;

import java.sql.Timestamp;

public class Reponse {

    private int idReponse;           // id_reponse        AUTO_INCREMENT
    private String contenuReponse;   // contenu_reponse   longtext
    private Timestamp dateReponse;   // date_reponse      datetime   ← ⚠️ (plus timestamp)
    private int idUtilisateur;       // id_utilisateur    int(11)    NULL
    private String nomUtilisateur;   // nom_utilisateur   varchar(255) NULL ← ⚠️ colonne 5 (avant rate)
    private double rateReponse;      // rate_reponse      double     ← ⚠️ double
    private int rateSum;             // rate_sum          int(11)
    private int rateCount;           // rate_count        int(11)
    private int reclamationId;       // reclamation_id    int(11)    ← ⚠️ renommé

    // ─── Constructeur complet (avec ID) ──────────────────────────────────────
    public Reponse(int idReponse, String contenuReponse, Timestamp dateReponse,
                   int idUtilisateur, String nomUtilisateur,
                   double rateReponse, int rateSum, int rateCount,
                   int reclamationId) {
        this.idReponse      = idReponse;
        this.contenuReponse = contenuReponse;
        this.dateReponse    = dateReponse;
        this.idUtilisateur  = idUtilisateur;
        this.nomUtilisateur = nomUtilisateur;
        this.rateReponse    = rateReponse;
        this.rateSum        = rateSum;
        this.rateCount      = rateCount;
        this.reclamationId  = reclamationId;
    }

    // ─── Constructeur sans ID (pour insertion) ────────────────────────────────
    public Reponse(String contenuReponse, Timestamp dateReponse,
                   int idUtilisateur, String nomUtilisateur,
                   double rateReponse, int reclamationId) {
        this.contenuReponse = contenuReponse;
        this.dateReponse    = dateReponse;
        this.idUtilisateur  = idUtilisateur;
        this.nomUtilisateur = nomUtilisateur;
        this.rateReponse    = rateReponse;
        this.reclamationId  = reclamationId;
    }

    // ─── Constructeur sans ID ni nomUtilisateur ───────────────────────────────
    public Reponse(String contenuReponse, Timestamp dateReponse,
                   int idUtilisateur, double rateReponse, int reclamationId) {
        this(contenuReponse, dateReponse, idUtilisateur, null, rateReponse, reclamationId);
    }

    // ─── Constructeur vide ────────────────────────────────────────────────────
    public Reponse() {}

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public int getIdReponse() { return idReponse; }
    public void setIdReponse(int idReponse) { this.idReponse = idReponse; }

    public String getContenuReponse() { return contenuReponse; }
    public void setContenuReponse(String contenuReponse) { this.contenuReponse = contenuReponse; }

    public Timestamp getDateReponse() { return dateReponse; }
    public void setDateReponse(Timestamp dateReponse) { this.dateReponse = dateReponse; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    /** Colonne {@code nom_utilisateur} en base. */
    public String getNomUtilisateur() { return nomUtilisateur; }
    public void setNomUtilisateur(String nomUtilisateur) { this.nomUtilisateur = nomUtilisateur; }

    /** Retourne la moyenne calculée si des votes existent, sinon la valeur stockée. */
    public double getRateReponse() {
        if (rateCount > 0) return (double) rateSum / rateCount;
        return rateReponse;
    }
    public void setRateReponse(double rateReponse) { this.rateReponse = rateReponse; }

    public int getRateSum() { return rateSum; }
    public void setRateSum(int rateSum) { this.rateSum = rateSum; }

    public int getRateCount() { return rateCount; }
    public void setRateCount(int rateCount) { this.rateCount = rateCount; }

    /** Colonne {@code reclamation_id} en base. */
    public int getReclamationId() { return reclamationId; }
    public void setReclamationId(int reclamationId) { this.reclamationId = reclamationId; }

    @Override
    public String toString() {
        return "Reponse{" +
                "idReponse=" + idReponse +
                ", contenuReponse='" + contenuReponse + '\'' +
                ", dateReponse=" + dateReponse +
                ", idUtilisateur=" + idUtilisateur +
                ", nomUtilisateur='" + nomUtilisateur + '\'' +
                ", reclamationId=" + reclamationId +
                ", rateReponse=" + getRateReponse() +
                '}';
    }
}