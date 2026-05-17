package tn.esprit.entities;

import java.util.Date;

/**
 * Entité alignée sur la table `evenement`.
 *
 * Colonnes DB :
 *   id, identifiant_evenemnt, titre_evenement, description_evenement,
 *   datedebut_evenemnt, datefin_evenemnt, lieu_evenement,
 *   capacite_evenement, max_liste_attente, statut_evenemnt,
 *   image, type_evenement_id (FK → type_evenement.id)
 */
public class Evenements {

    private int           id;
    private String        identifiantEvenemnt;
    private String        titreEvenement;
    private String        descriptionEvenement;
    private Date          datedebutEvenemnt;
    private Date          datefinEvenemnt;
    private String        lieuEvenement;
    private int           capaciteEvenement;
    private Integer       maxListeAttente;
    private String        statutEvenemnt;
    private String        image;
    private int           typeEvenementId;       // FK brut (colonne type_evenement_id)
    private String        typeEvenement;         // libellé String – compat. contrôleurs
    private TypeEvenement typeEvenementObj;       // objet complet après JOIN

    // ── Constructeurs ──────────────────────────────────────────────────────────

    public Evenements() {}

    public Evenements(int id, String identifiantEvenemnt, String titreEvenement,
                      String descriptionEvenement, Date datedebutEvenemnt,
                      Date datefinEvenemnt, String lieuEvenement,
                      int capaciteEvenement, Integer maxListeAttente,
                      String statutEvenemnt, String image, int typeEvenementId) {
        this.id                   = id;
        this.identifiantEvenemnt  = identifiantEvenemnt;
        this.titreEvenement       = titreEvenement;
        this.descriptionEvenement = descriptionEvenement;
        this.datedebutEvenemnt    = datedebutEvenemnt;
        this.datefinEvenemnt      = datefinEvenemnt;
        this.lieuEvenement        = lieuEvenement;
        this.capaciteEvenement    = capaciteEvenement;
        this.maxListeAttente      = maxListeAttente;
        this.statutEvenemnt       = statutEvenemnt;
        this.image                = image;
        this.typeEvenementId      = typeEvenementId;
    }

    // ── id ────────────────────────────────────────────────────────────────────
    public int getId()              { return id; }
    public void setId(int id)       { this.id = id; }
    public int getId_evenemnt()     { return id; }
    public void setId_evenemnt(int id) { this.id = id; }

    // ── identifiant ───────────────────────────────────────────────────────────
    public String getIdentifiantEvenemnt()          { return identifiantEvenemnt; }
    public void setIdentifiantEvenemnt(String v)    { this.identifiantEvenemnt = v; }
    public String getIdentifiant_evenemnt()         { return identifiantEvenemnt; }
    public void setIdentifiant_evenemnt(String v)   { this.identifiantEvenemnt = v; }

    // ── titre ─────────────────────────────────────────────────────────────────
    public String getTitreEvenement()               { return titreEvenement; }
    public void setTitreEvenement(String v)         { this.titreEvenement = v; }
    public String getTitre_evenement()              { return titreEvenement; }
    public void setTitre_evenement(String v)        { this.titreEvenement = v; }

    // ── description ───────────────────────────────────────────────────────────
    public String getDescriptionEvenement()         { return descriptionEvenement; }
    public void setDescriptionEvenement(String v)   { this.descriptionEvenement = v; }
    public String getDescription_evenement()        { return descriptionEvenement; }
    public void setDescription_evenement(String v)  { this.descriptionEvenement = v; }

    // ── dates ─────────────────────────────────────────────────────────────────
    public Date getDatedebutEvenemnt()              { return datedebutEvenemnt; }
    public void setDatedebutEvenemnt(Date v)        { this.datedebutEvenemnt = v; }
    public Date getDatedebut_evenemnt()             { return datedebutEvenemnt; }
    public void setDatedebut_evenemnt(Date v)       { this.datedebutEvenemnt = v; }

    public Date getDatefinEvenemnt()                { return datefinEvenemnt; }
    public void setDatefinEvenemnt(Date v)          { this.datefinEvenemnt = v; }
    public Date getDatefin_evenemnt()               { return datefinEvenemnt; }
    public void setDatefin_evenemnt(Date v)         { this.datefinEvenemnt = v; }

    // ── lieu ──────────────────────────────────────────────────────────────────
    public String getLieuEvenement()                { return lieuEvenement; }
    public void setLieuEvenement(String v)          { this.lieuEvenement = v; }
    public String getLieu_evenement()               { return lieuEvenement; }
    public void setLieu_evenement(String v)         { this.lieuEvenement = v; }

    // ── capacité ──────────────────────────────────────────────────────────────
    public int getCapaciteEvenement()               { return capaciteEvenement; }
    public void setCapaciteEvenement(int v)         { this.capaciteEvenement = v; }
    public int getCapacite_evenement()              { return capaciteEvenement; }
    public void setCapacite_evenement(int v)        { this.capaciteEvenement = v; }

    // ── liste d'attente ───────────────────────────────────────────────────────
    public Integer getMaxListeAttente()             { return maxListeAttente; }
    public void setMaxListeAttente(Integer v)       { this.maxListeAttente = v; }

    // ── statut ────────────────────────────────────────────────────────────────
    public String getStatutEvenemnt()               { return statutEvenemnt; }
    public void setStatutEvenemnt(String v)         { this.statutEvenemnt = v; }
    public String getStatut_evenemnt()              { return statutEvenemnt; }
    public void setStatut_evenemnt(String v)        { this.statutEvenemnt = v; }

    // ── image ─────────────────────────────────────────────────────────────────
    public String getImage()                        { return image; }
    public void setImage(String v)                  { this.image = v; }

    // ── type_evenement_id (FK brut) ───────────────────────────────────────────
    public int getTypeEvenementId()                 { return typeEvenementId; }
    public void setTypeEvenementId(int v)           { this.typeEvenementId = v; }

    // ── typeEvenement : libellé String (compat. tous les contrôleurs) ─────────
    public String getTypeEvenement() {
        return typeEvenement != null ? typeEvenement : "";
    }
    public void setTypeEvenement(String libelle) {
        this.typeEvenement = libelle;
        if (this.typeEvenementObj == null) this.typeEvenementObj = new TypeEvenement();
        this.typeEvenementObj.setLibelle(libelle);
    }

    // ── typeEvenementObj : objet complet ──────────────────────────────────────
    public TypeEvenement getTypeEvenementObj()      { return typeEvenementObj; }
    public void setTypeEvenementObj(TypeEvenement obj) {
        this.typeEvenementObj = obj;
        if (obj != null) {
            this.typeEvenementId = obj.getId();
            this.typeEvenement   = obj.getLibelle();
        }
    }

    @Override
    public String toString() {
        return "Evenements{id=" + id + ", titre='" + titreEvenement
                + "', type='" + typeEvenement + "', statut='" + statutEvenemnt + "'}";
    }
}