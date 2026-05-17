package tn.esprit.entities;

import java.sql.Timestamp;

public class ReponseClient {

    // ============================
    // Champs (correspondent à la table reponse_client)
    // ============================
    private int       idReponseClient;        // id_reponse_client       - PK, AUTO_INCREMENT, NOT NULL
    private int       utilisateurId;          // utilisateur_id          - int(11), NULL
    private String    optionChoisie;          // option_choisie          - varchar(255), NULL
    private int       scoreObtenu;            // score_obtenu            - int(11), NULL
    private String    reponseTexteLibre;      // reponse_texte_libre     - longtext, NULL
    private Timestamp dateReponse;            // date_reponse            - datetime, NOT NULL
    private int       idTestId;               // id_test_id              - FK, int(11), NULL
    private int       idQuestionReponseId;    // id_question_reponse_id  - FK, int(11), NULL

    // ============================
    // Constructeur complet (avec ID) — pour afficher / modifier
    // ============================
    public ReponseClient(int idReponseClient, int utilisateurId,
                         String optionChoisie, int scoreObtenu,
                         String reponseTexteLibre, Timestamp dateReponse,
                         int idTestId, int idQuestionReponseId) {
        this.idReponseClient       = idReponseClient;
        this.utilisateurId         = utilisateurId;
        this.optionChoisie         = optionChoisie;
        this.scoreObtenu           = scoreObtenu;
        this.reponseTexteLibre     = reponseTexteLibre;
        this.dateReponse           = dateReponse;
        this.idTestId              = idTestId;
        this.idQuestionReponseId   = idQuestionReponseId;
    }

    // ============================
    // Constructeur sans ID — pour l'insertion (choix_multiple / oui_non)
    // ============================
    public ReponseClient(int utilisateurId, int idTestId, int idQuestionReponseId,
                         String optionChoisie, int scoreObtenu) {
        this.utilisateurId       = utilisateurId;
        this.idTestId            = idTestId;
        this.idQuestionReponseId = idQuestionReponseId;
        this.optionChoisie       = optionChoisie;
        this.scoreObtenu         = scoreObtenu;
        this.dateReponse         = new Timestamp(System.currentTimeMillis());
    }

    // ============================
    // Constructeur pour réponse texte libre
    // ============================
    public ReponseClient(int utilisateurId, int idTestId, int idQuestionReponseId,
                         String reponseTexteLibre) {
        this.utilisateurId       = utilisateurId;
        this.idTestId            = idTestId;
        this.idQuestionReponseId = idQuestionReponseId;
        this.reponseTexteLibre   = reponseTexteLibre;
        this.scoreObtenu         = 0;
        this.dateReponse         = new Timestamp(System.currentTimeMillis());
    }

    // ============================
    // Constructeur vide
    // ============================
    public ReponseClient() {
        this.scoreObtenu = 0;
        this.dateReponse = new Timestamp(System.currentTimeMillis());
    }

    // ============================
    // Getters & Setters
    // ============================
    public int getIdReponseClient() { return idReponseClient; }
    public void setIdReponseClient(int idReponseClient) { this.idReponseClient = idReponseClient; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getOptionChoisie() { return optionChoisie; }
    public void setOptionChoisie(String optionChoisie) { this.optionChoisie = optionChoisie; }

    public int getScoreObtenu() { return scoreObtenu; }
    public void setScoreObtenu(int scoreObtenu) { this.scoreObtenu = scoreObtenu; }

    public String getReponseTexteLibre() { return reponseTexteLibre; }
    public void setReponseTexteLibre(String reponseTexteLibre) { this.reponseTexteLibre = reponseTexteLibre; }

    public Timestamp getDateReponse() { return dateReponse; }
    public void setDateReponse(Timestamp dateReponse) { this.dateReponse = dateReponse; }

    public int getIdTestId() { return idTestId; }
    public void setIdTestId(int idTestId) { this.idTestId = idTestId; }

    public int getIdQuestionReponseId() { return idQuestionReponseId; }
    public void setIdQuestionReponseId(int idQuestionReponseId) { this.idQuestionReponseId = idQuestionReponseId; }

    // ============================
    // toString
    // ============================
    @Override
    public String toString() {
        return "ReponseClient{" +
                "idReponseClient=" + idReponseClient +
                ", utilisateurId=" + utilisateurId +
                ", idTestId=" + idTestId +
                ", idQuestionReponseId=" + idQuestionReponseId +
                ", optionChoisie='" + optionChoisie + '\'' +
                ", scoreObtenu=" + scoreObtenu +
                ", reponseTexteLibre='" + reponseTexteLibre + '\'' +
                ", dateReponse=" + dateReponse +
                '}';
    }
}