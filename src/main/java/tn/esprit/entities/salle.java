package tn.esprit.entities;

public class salle {
    private int    id_salle;
    private String nom_salle;
    private String type_salle;
    private String capacite_salle;
    private String equipements;
    private String disponibilite_salle;
    private String etage;           // String car la BD contient "1er etage", "RDC"...
    private String image_url;
    private String statut_salle;
    private int    id_local;

    public salle() {}

    // Constructeur complet (avec id)
    public salle(int id_salle, String nom_salle, String type_salle, String capacite_salle,
                 String equipements, String disponibilite_salle, String etage,
                 String image_url, String statut_salle, int id_local) {
        this.id_salle            = id_salle;
        this.nom_salle           = nom_salle;
        this.type_salle          = type_salle;
        this.capacite_salle      = capacite_salle;
        this.equipements         = equipements;
        this.disponibilite_salle = disponibilite_salle;
        this.etage               = etage;
        this.image_url            = image_url;
        this.statut_salle        = statut_salle;
        this.id_local            = id_local;
    }

    // Constructeur sans id (pour insertion)
    public salle(String nom_salle, String type_salle, String capacite_salle,
                 String equipements, String disponibilite_salle, String etage,
                 String image_url, String statut_salle, int id_local) {
        this.nom_salle           = nom_salle;
        this.type_salle          = type_salle;
        this.capacite_salle      = capacite_salle;
        this.equipements         = equipements;
        this.disponibilite_salle = disponibilite_salle;
        this.etage               = etage;
        this.image_url            = image_url;
        this.statut_salle        = statut_salle;
        this.id_local            = id_local;
    }

    public int getId_salle() { return id_salle; }
    public void setId_salle(int id_salle) { this.id_salle = id_salle; }

    public String getNom_salle() { return nom_salle; }
    public void setNom_salle(String nom_salle) { this.nom_salle = nom_salle; }

    public String getType_salle() { return type_salle; }
    public void setType_salle(String type_salle) { this.type_salle = type_salle; }

    public String getCapacite_salle() { return capacite_salle; }
    public void setCapacite_salle(String capacite_salle) { this.capacite_salle = capacite_salle; }

    public String getEquipements() { return equipements; }
    public void setEquipements(String equipements) { this.equipements = equipements; }

    public String getDisponibilite_salle() { return disponibilite_salle; }
    public void setDisponibilite_salle(String d) { this.disponibilite_salle = d; }

    public String getEtage() { return etage; }
    public void setEtage(String etage) { this.etage = etage; }

    public String getImage_url() { return image_url; }
    public void setImageURL(String image_url) { this.image_url = image_url; }

    public String getStatut_salle() { return statut_salle; }
    public void setStatut_salle(String statut_salle) { this.statut_salle = statut_salle; }

    public int getId_local() { return id_local; }
    public void setId_local(int id_local) { this.id_local = id_local; }

    @Override
    public String toString() {
        return "salle{id=" + id_salle + ", nom='" + nom_salle + "', etage='" + etage + "'}";
    }
}