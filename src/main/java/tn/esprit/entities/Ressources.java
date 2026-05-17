package tn.esprit.entities;

import java.util.Date;
import java.util.List;

public class Ressources {

    // ── contenu VARCHAR(50) : Article, Video, Podcast, PDF ───────────────────
    public enum Contenu {
        Article, Video, Podcast, PDF
    }

    // ── niveau VARCHAR(20) : DEBUTANT, INTERMEDIAIRE, AVANCE ─────────────────
    public enum Niveau {
        DEBUTANT, INTERMEDIAIRE, AVANCE
    }

    // ── Champs — EXACTEMENT les colonnes de la table `ressource` ─────────────
    private int          id_ressources;   // PK AUTO_INCREMENT
    private String       titre;           // VARCHAR(1000)
    private String       resume;          // LONGTEXT
    private String       contenu;         // VARCHAR(50) → stocké en String brut
    private String       categorie;       // VARCHAR(100) → stocké en String brut
    private String       tags;            // LONGTEXT
    private Date         date_publication;// DATE
    private int          nbr_vues;        // INT
    private int          likes;           // INT
    private String       niveau;          // VARCHAR(20) → stocké en String brut
    private int          duree_lecture;   // INT
    private String       imageUrl;        // VARCHAR(255) → colonne image_url
    private String       url;             // VARCHAR(255)
    private String       emailAuteur;     // VARCHAR(255) → colonne email_auteur

    // ── Constructeur vide ─────────────────────────────────────────────────────
    public Ressources() {}

    // ── Constructeur complet ──────────────────────────────────────────────────
    public Ressources(int id_ressources, String imageUrl, String titre, String resume,
                      String contenu, String categorie, String tags,
                      Date date_publication, int nbr_vues, int likes,
                      String niveau, int duree_lecture,
                      String url, String emailAuteur) {
        this.id_ressources    = id_ressources;
        this.imageUrl         = imageUrl;
        this.titre            = titre;
        this.resume           = resume;
        this.contenu          = contenu;
        this.categorie        = categorie;
        this.tags             = tags;
        this.date_publication = date_publication;
        this.nbr_vues         = nbr_vues;
        this.likes            = likes;
        this.niveau           = niveau;
        this.duree_lecture    = duree_lecture;
        this.url              = url;
        this.emailAuteur      = emailAuteur;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public int    getId_ressources()           { return id_ressources; }
    public void   setId_ressources(int v)      { this.id_ressources = v; }

    public String getTitre()                   { return titre; }
    public void   setTitre(String v)           { this.titre = v; }

    public String getResume()                  { return resume; }
    public void   setResume(String v)          { this.resume = v; }

    public String getContenu()                 { return contenu; }
    public void   setContenu(String v)         { this.contenu = v; }

    /** Setter depuis Enum (rétrocompat) */
    public void   setContenu(Contenu v)        { this.contenu = (v == null) ? null : v.name(); }

    /** Getter en Enum */
    public Contenu getContenuEnum() {
        if (contenu == null) return null;
        try { return Contenu.valueOf(contenu); }
        catch (IllegalArgumentException e) { return null; }
    }

    public String getCategorie()               { return categorie; }
    public void   setCategorie(String v)       { this.categorie = v; }

    public String getTags()                    { return tags; }
    public void   setTags(String v)            { this.tags = v; }

    public Date   getDate_publication()        { return date_publication; }
    public void   setDate_publication(Date v)  { this.date_publication = v; }

    public int    getNbr_vues()                { return nbr_vues; }
    public void   setNbr_vues(int v)           { this.nbr_vues = v; }

    public int    getLikes()                   { return likes; }
    public void   setLikes(int v)              { this.likes = v; }

    public String getNiveau()                  { return niveau; }
    public void   setNiveau(String v)          { this.niveau = v; }

    /** Setter depuis Enum (rétrocompat) */
    public void   setNiveau(Niveau v)          { this.niveau = (v == null) ? null : v.name(); }

    /** Getter en Enum */
    public Niveau getNiveauEnum() {
        if (niveau == null) return null;
        try { return Niveau.valueOf(niveau); }
        catch (IllegalArgumentException e) { return null; }
    }

    public int    getDuree_lecture()           { return duree_lecture; }
    public void   setDuree_lecture(int v)      { this.duree_lecture = v; }

    public String getImageUrl()                { return imageUrl; }
    public void   setImageUrl(String v)        { this.imageUrl = v; }

    public String getUrl()                     { return url; }
    public void   setUrl(String v)             { this.url = v; }

    public String getEmailAuteur()             { return emailAuteur; }
    public void   setEmailAuteur(String v)     { this.emailAuteur = v; }

    @Override
    public String toString() {
        return "Ressources{" +
                "id=" + id_ressources +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", categorie='" + categorie + '\'' +
                ", niveau='" + niveau + '\'' +
                ", nbr_vues=" + nbr_vues +
                ", likes=" + likes +
                ", duree_lecture=" + duree_lecture +
                ", url='" + url + '\'' +
                ", emailAuteur='" + emailAuteur + '\'' +
                '}';
    }
}
