package tn.esprit.entities;

import java.util.Date;

public class Commentaires {

    // ── Enum status — valeurs réelles en BD : ACTIF, SUPPRIME, SIGNALE ───────
    public enum StatusCommentaires {
        ACTIF, SUPPRIME, SIGNALE, ARCHIVE
    }

    // ── Champs — EXACTEMENT les colonnes de la table `commentaire` ────────────
    private int                id_commentaires;  // PK AUTO_INCREMENT
    private int                id_user;          // INT  → colonne id_user
    private String             user_name;        // VARCHAR(255)
    private String             contenu;          // LONGTEXT
    private Date               datePublication;  // DATE → colonne date_publication
    private int                likes;            // INT DEFAULT 0
    private int                reponse;          // INT DEFAULT 0
    private StatusCommentaires status;           // VARCHAR(20) DEFAULT 'ACTIF'
    private String             sentiment;        // VARCHAR(20) nullable
    private String             themes;           // LONGTEXT JSON nullable
    private String             aiSummary;        // LONGTEXT → colonne ai_summary nullable
    private int                ressource_id;     // INT → colonne ressource_id (FK)
    private Integer            parent_id;        // INT nullable → réponse imbriquée

    // ── Constructeur vide ─────────────────────────────────────────────────────
    public Commentaires() {}

    // ── Constructeur complet (colonnes BD) ────────────────────────────────────
    public Commentaires(int id_commentaires, int id_user, String user_name,
                        String contenu, Date datePublication, int likes,
                        int reponse, StatusCommentaires status,
                        String sentiment, String themes, String aiSummary,
                        int ressource_id, Integer parent_id) {
        this.id_commentaires = id_commentaires;
        this.id_user         = id_user;
        this.user_name       = user_name;
        this.contenu         = contenu;
        this.datePublication = datePublication;
        this.likes           = likes;
        this.reponse         = reponse;
        this.status          = status;
        this.sentiment       = sentiment;
        this.themes          = themes;
        this.aiSummary       = aiSummary;
        this.ressource_id    = ressource_id;
        this.parent_id       = parent_id;
    }

    // ── Constructeur simplifié (ajout rapide) ─────────────────────────────────
    public Commentaires(int ressource_id, int id_user, String user_name, String contenu) {
        this.ressource_id    = ressource_id;
        this.id_user         = id_user;
        this.user_name       = user_name;
        this.contenu         = contenu;
        this.status          = StatusCommentaires.ACTIF;
        this.likes           = 0;
        this.reponse         = 0;
        this.datePublication = new java.util.Date();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int    getId_commentaires()            { return id_commentaires; }
    public void   setId_commentaires(int v)       { this.id_commentaires = v; }

    public int    getId_user()                    { return id_user; }
    public void   setId_user(int v)               { this.id_user = v; }

    /** Alias rétrocompat */
    public int    getId_User()                    { return id_user; }
    public void   setId_User(int v)               { this.id_user = v; }

    /** Alias rétrocompat : getId_Article() → ressource_id */
    public int    getId_Article()                 { return ressource_id; }
    public void   setId_Article(int v)            { this.ressource_id = v; }

    public int    getRessource_id()               { return ressource_id; }
    public void   setRessource_id(int v)          { this.ressource_id = v; }

    public String getUser_name()                  { return user_name; }
    public void   setUser_name(String v)          { this.user_name = v; }

    public String getContenu()                    { return contenu; }
    public void   setContenu(String v)            { this.contenu = v; }

    public Date   getDate()                       { return datePublication; }
    public void   setDate(Date v)                 { this.datePublication = v; }

    public Date   getDatePublication()            { return datePublication; }
    public void   setDatePublication(Date v)      { this.datePublication = v; }

    public int    getLikes()                      { return likes; }
    public void   setLikes(int v)                 { this.likes = v; }

    public int    getReponse()                    { return reponse; }
    public void   setReponse(int v)               { this.reponse = v; }

    public StatusCommentaires getStatus()         { return status; }
    public void   setStatus(StatusCommentaires v) { this.status = v; }

    public void setStatusFromString(String s) {
        if (s == null) { this.status = StatusCommentaires.ACTIF; return; }
        try { this.status = StatusCommentaires.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { this.status = StatusCommentaires.ACTIF; }
    }

    public boolean isSignale() { return status == StatusCommentaires.SIGNALE; }

    public String getSentiment()                  { return sentiment; }
    public void   setSentiment(String v)          { this.sentiment = v; }

    public String getThemes()                     { return themes; }
    public void   setThemes(String v)             { this.themes = v; }

    public String getAiSummary()                  { return aiSummary; }
    public void   setAiSummary(String v)          { this.aiSummary = v; }

    public Integer getParent_id()                 { return parent_id; }
    public void    setParent_id(Integer v)        { this.parent_id = v; }

    @Override
    public String toString() {
        return "Commentaires{" +
                "id=" + id_commentaires +
                ", id_user=" + id_user +
                ", user_name='" + user_name + '\'' +
                ", contenu='" + contenu + '\'' +
                ", date=" + datePublication +
                ", likes=" + likes +
                ", status=" + status +
                ", ressource_id=" + ressource_id +
                ", parent_id=" + parent_id +
                '}';
    }
}
