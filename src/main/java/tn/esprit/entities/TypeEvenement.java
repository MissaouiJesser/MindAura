package tn.esprit.entities;

/**
 * Entité alignée sur la table `type_evenement`.
 *
 * Colonnes : id, libelle, modalite, categorie, est_gratuit
 */
public class TypeEvenement {

    private int     id;
    private String  libelle;
    private String  modalite;
    private String  categorie;
    private boolean estGratuit;

    // ── Constructeurs ──────────────────────────────────────────────────────────

    public TypeEvenement() {}

    public TypeEvenement(int id, String libelle, String modalite,
                         String categorie, boolean estGratuit) {
        this.id         = id;
        this.libelle    = libelle;
        this.modalite   = modalite;
        this.categorie  = categorie;
        this.estGratuit = estGratuit;
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getModalite() { return modalite; }
    public void setModalite(String modalite) { this.modalite = modalite; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public boolean isEstGratuit() { return estGratuit; }
    public void setEstGratuit(boolean estGratuit) { this.estGratuit = estGratuit; }

    @Override
    public String toString() {
        return "TypeEvenement{id=" + id + ", libelle='" + libelle
                + "', modalite='" + modalite + "', categorie='" + categorie
                + "', estGratuit=" + estGratuit + "}";
    }
}