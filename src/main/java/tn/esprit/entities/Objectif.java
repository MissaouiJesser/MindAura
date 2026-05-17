package tn.esprit.entities;

import java.sql.Date;
import java.sql.Timestamp;

public class Objectif {

    // ============================
    // Champs (correspondent à la table objectif)
    // ============================
    private int       idObjectif;          // id_objectif       - PK, AUTO_INCREMENT, NOT NULL
    private String    source;              // source            - varchar(20), NOT NULL, default 'admin'
    private int       idUtilisateur;       // id_utilisateur    - int(11), NULL
    private String    titre;              // titre             - varchar(255), NOT NULL
    private String    description;        // description       - longtext, NOT NULL
    private String    typeObjectif;       // type_objectif     - varchar(20), NOT NULL, default 'admin'
    private String    typeTest;           // type_test         - varchar(255), NOT NULL
    private String    niveauRecommande;   // niveau_recommande - varchar(255), NOT NULL
    private int       scoreMin;           // score_min         - int(11), NOT NULL
    private int       scoreMax;           // score_max         - int(11), NOT NULL
    private String    categorie;          // categorie         - varchar(255), NOT NULL
    private int       dureeEstimee;       // duree_estimee     - int(11), NOT NULL
    private String    difficulte;         // difficule         - varchar(255), NOT NULL
    private Timestamp dateCreation;       // date_creation     - datetime, NULL
    private Date      dateEcheance;       // date_echeance     - date, NOT NULL
    private String    statut;             // statut            - varchar(255), NOT NULL
    private boolean   estPublic;          // est_public        - tinyint(4), NOT NULL

    // ============================
    // Constructeur COMPLET (avec ID) — pour récupérer de la BD
    // ============================
    public Objectif(int idObjectif, String source, int idUtilisateur,
                    String titre, String description, String typeObjectif,
                    String typeTest, String niveauRecommande, int scoreMin, int scoreMax,
                    String categorie, int dureeEstimee, String difficulte,
                    Timestamp dateCreation, Date dateEcheance,
                    String statut, boolean estPublic) {
        this.idObjectif       = idObjectif;
        this.source           = source;
        this.idUtilisateur    = idUtilisateur;
        this.titre            = titre;
        this.description      = description;
        this.typeObjectif     = typeObjectif;
        this.typeTest         = typeTest;
        this.niveauRecommande = niveauRecommande;
        this.scoreMin         = scoreMin;
        this.scoreMax         = scoreMax;
        this.categorie        = categorie;
        this.dureeEstimee     = dureeEstimee;
        this.difficulte       = difficulte;
        this.dateCreation     = dateCreation;
        this.dateEcheance     = dateEcheance;
        this.statut           = statut;
        this.estPublic        = estPublic;
    }

    // ============================
    // Constructeur ADMIN (sans ID) — pour créer un objectif admin
    // ============================
    public Objectif(String titre, String description, String typeTest,
                    String niveauRecommande, int scoreMin, int scoreMax,
                    String categorie, int dureeEstimee, String difficulte,
                    Date dateEcheance) {
        this.source           = "admin";
        this.titre            = titre;
        this.description      = description;
        this.typeObjectif     = "admin";
        this.typeTest         = typeTest;
        this.niveauRecommande = niveauRecommande;
        this.scoreMin         = scoreMin;
        this.scoreMax         = scoreMax;
        this.categorie        = categorie;
        this.dureeEstimee     = dureeEstimee;
        this.difficulte       = difficulte;
        this.dateEcheance     = dateEcheance;
        this.statut           = "actif";
        this.estPublic        = true;
    }

    // ============================
    // Constructeur PATIENT (sans ID) — pour créer un objectif patient
    // ============================
    public Objectif(int idUtilisateur, String titre, String description,
                    String categorie, String difficulte, Date dateEcheance) {
        this.source        = "patient";
        this.idUtilisateur = idUtilisateur;
        this.titre         = titre;
        this.description   = description;
        this.typeObjectif  = "patient";
        this.categorie     = categorie;
        this.difficulte    = difficulte;
        this.dateEcheance  = dateEcheance;
        this.statut        = "actif";
        this.estPublic     = false;
        this.scoreMin      = 0;
        this.scoreMax      = 0;
        this.dureeEstimee  = 0;
    }

    // ============================
    // Constructeur vide
    // ============================
    public Objectif() {
        this.source       = "admin";
        this.typeObjectif = "admin";
        this.statut       = "actif";
        this.estPublic    = true;
    }

    // ============================
    // Getters & Setters
    // ============================
    public int getIdObjectif() { return idObjectif; }
    public void setIdObjectif(int idObjectif) { this.idObjectif = idObjectif; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTypeObjectif() { return typeObjectif; }
    public void setTypeObjectif(String typeObjectif) { this.typeObjectif = typeObjectif; }

    public String getTypeTest() { return typeTest; }
    public void setTypeTest(String typeTest) { this.typeTest = typeTest; }

    public String getNiveauRecommande() { return niveauRecommande; }
    public void setNiveauRecommande(String niveauRecommande) { this.niveauRecommande = niveauRecommande; }

    public int getScoreMin() { return scoreMin; }
    public void setScoreMin(int scoreMin) { this.scoreMin = scoreMin; }

    public int getScoreMax() { return scoreMax; }
    public void setScoreMax(int scoreMax) { this.scoreMax = scoreMax; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public int getDureeEstimee() { return dureeEstimee; }
    public void setDureeEstimee(int dureeEstimee) { this.dureeEstimee = dureeEstimee; }

    public String getDifficulte() { return difficulte; }
    public void setDifficulte(String difficulte) { this.difficulte = difficulte; }

    public Timestamp getDateCreation() { return dateCreation; }
    public void setDateCreation(Timestamp dateCreation) { this.dateCreation = dateCreation; }

    public Date getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(Date dateEcheance) { this.dateEcheance = dateEcheance; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public boolean isEstPublic() { return estPublic; }
    public void setEstPublic(boolean estPublic) { this.estPublic = estPublic; }

    // ============================
    // toString
    // ============================
    @Override
    public String toString() {
        return "Objectif{" +
                "idObjectif=" + idObjectif +
                ", source='" + source + '\'' +
                ", idUtilisateur=" + idUtilisateur +
                ", titre='" + titre + '\'' +
                ", typeObjectif='" + typeObjectif + '\'' +
                ", categorie='" + categorie + '\'' +
                ", statut='" + statut + '\'' +
                ", estPublic=" + estPublic +
                '}';
    }
}