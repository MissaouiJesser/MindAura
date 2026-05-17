package tn.esprit.entities;

import java.util.Date;

public class TestPsycho {
    private int idTest;
    private String titreTest;
    private String descriptionTest;
    private String typeTest;
    private int dureeEstimee;        // ✅ renommé pour correspondre à la colonne DB
    private String instructionsTest;
    private Date dateCreation;       // ✅ correspond à date_creation
    private Date dateModification;   // ✅ correspond à date_modification
    private boolean estActif;

    // ✅ Constructeur complet (avec ID)
    public TestPsycho(int idTest, String titreTest, String descriptionTest,
                      String typeTest, int dureeEstimee, String instructionsTest,
                      Date dateCreation, Date dateModification,
                      boolean estActif) {
        this.idTest = idTest;
        this.titreTest = titreTest;
        this.descriptionTest = descriptionTest;
        this.typeTest = typeTest;
        this.dureeEstimee = dureeEstimee;
        this.instructionsTest = instructionsTest;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.estActif = estActif;
    }

    // ✅ Constructeur sans ID (pour insertion)
    public TestPsycho(String titreTest, String descriptionTest,
                      String typeTest, int dureeEstimee, String instructionsTest) {
        this.titreTest = titreTest;
        this.descriptionTest = descriptionTest;
        this.typeTest = typeTest;
        this.dureeEstimee = dureeEstimee;
        this.instructionsTest = instructionsTest;
        this.estActif = true;
    }

    // ✅ Constructeur simplifié
    public TestPsycho(String titreTest, String descriptionTest, String typeTest) {
        this.titreTest = titreTest;
        this.descriptionTest = descriptionTest;
        this.typeTest = typeTest;
        this.estActif = true;
    }

    // ✅ Constructeur vide
    public TestPsycho() {
        this.estActif = true;
    }

    // Getters & Setters
    public int getIdTest() { return idTest; }
    public void setIdTest(int idTest) { this.idTest = idTest; }

    public String getTitreTest() { return titreTest; }
    public void setTitreTest(String titreTest) { this.titreTest = titreTest; }

    public String getDescriptionTest() { return descriptionTest; }
    public void setDescriptionTest(String descriptionTest) { this.descriptionTest = descriptionTest; }

    public String getTypeTest() { return typeTest; }
    public void setTypeTest(String typeTest) { this.typeTest = typeTest; }

    public int getDureeEstimee() { return dureeEstimee; }
    public void setDureeEstimee(int dureeEstimee) { this.dureeEstimee = dureeEstimee; }

    public String getInstructionsTest() { return instructionsTest; }
    public void setInstructionsTest(String instructionsTest) { this.instructionsTest = instructionsTest; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public Date getDateModification() { return dateModification; }
    public void setDateModification(Date dateModification) { this.dateModification = dateModification; }

    public boolean isEstActif() { return estActif; }
    public void setEstActif(boolean estActif) { this.estActif = estActif; }

    @Override
    public String toString() {
        return "TestPsycho{" +
                "idTest=" + idTest +
                ", titreTest='" + titreTest + '\'' +
                ", descriptionTest='" + descriptionTest + '\'' +
                ", typeTest='" + typeTest + '\'' +
                ", dureeEstimee=" + dureeEstimee +
                ", instructionsTest='" + instructionsTest + '\'' +
                ", estActif=" + estActif +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                '}';
    }
}