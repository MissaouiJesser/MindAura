package tn.esprit.entities;

import tn.esprit.enums.Role;

import java.util.Date;

public class utilisateurs {
    private String id_utilisateur;
    private String nom_utilisateur;
    private String prenom_utilisateur;
    private String email_utilisateur;
    private String mdp_utilisateur;
    private String telephone_utilisateur;
    private Date date_naissance_utilisateur;
    private Role role_utilisateur;
    private String photo_profil_utilisateur;
    private Date date_inscription_utilisateur;
    private boolean est_actif_utilisateur;
    private String bio_utilisateur;

    public utilisateurs() {
        this.role_utilisateur = Role.ROLE_USER;
        this.est_actif_utilisateur = true;
    }

    public utilisateurs(String id_utilisateur, String nom_utilisateur, String prenom_utilisateur, String email_utilisateur, String mdp_utilisateur, String telephone_utilisateur, Date date_naissance_utilisateur, Role role_utilisateur, String photo_profil_utilisateur, Date date_inscription_utilisateur, boolean est_actif_utilisateur, String bio_utilisateur) {
        this.id_utilisateur = id_utilisateur;
        this.nom_utilisateur = nom_utilisateur;
        this.prenom_utilisateur = prenom_utilisateur;
        this.email_utilisateur = email_utilisateur;
        this.mdp_utilisateur = mdp_utilisateur;
        this.telephone_utilisateur = telephone_utilisateur;
        this.date_naissance_utilisateur = date_naissance_utilisateur;
        this.role_utilisateur = role_utilisateur;
        this.photo_profil_utilisateur = photo_profil_utilisateur;
        this.date_inscription_utilisateur = date_inscription_utilisateur;
        this.est_actif_utilisateur = est_actif_utilisateur;
        this.bio_utilisateur = bio_utilisateur;
    }

    public String getId_utilisateur() {
        return id_utilisateur;
    }

    public void setId_utilisateur(String id_utilisateur) {
        this.id_utilisateur = id_utilisateur;
    }

    public String getNom_utilisateur() {
        return nom_utilisateur;
    }

    public void setNom_utilisateur(String nom_utilisateur) {
        this.nom_utilisateur = nom_utilisateur;
    }

    public String getPrenom_utilisateur() {
        return prenom_utilisateur;
    }

    public void setPrenom_utilisateur(String prenom_utilisateur) {
        this.prenom_utilisateur = prenom_utilisateur;
    }

    public String getEmail_utilisateur() {
        return email_utilisateur;
    }

    public void setEmail_utilisateur(String email_utilisateur) {
        this.email_utilisateur = email_utilisateur;
    }

    public String getMdp_utilisateur() {
        return mdp_utilisateur;
    }

    public void setMdp_utilisateur(String mdp_utilisateur) {
        this.mdp_utilisateur = mdp_utilisateur;
    }

    public String getTelephone_utilisateur() {
        return telephone_utilisateur;
    }

    public void setTelephone_utilisateur(String telephone_utilisateur) {
        this.telephone_utilisateur = telephone_utilisateur;
    }

    public Date getDate_naissance_utilisateur() {
        return date_naissance_utilisateur;
    }

    public void setDate_naissance_utilisateur(Date date_naissance_utilisateur) {
        this.date_naissance_utilisateur = date_naissance_utilisateur;
    }

    public void setRole_utilisateur(Role role_utilisateur) {
        this.role_utilisateur = role_utilisateur;
    }

    public void setRole_utilisateur(String roleString) {
        this.role_utilisateur = Role.fromString(roleString);
    }

    public Role getRole_utilisateur() {
        return role_utilisateur;
    }

    public Date getDate_inscription_utilisateur() {
        return date_inscription_utilisateur;
    }

    public void setDate_inscription_utilisateur(Date date_inscription_utilisateur) {
        this.date_inscription_utilisateur = date_inscription_utilisateur;
    }

    public String getPhoto_profil_utilisateur() {
        return photo_profil_utilisateur;
    }

    public void setPhoto_profil_utilisateur(String photo_profil_utilisateur) {
        this.photo_profil_utilisateur = photo_profil_utilisateur;
    }

    public boolean isEst_actif_utilisateur() {
        return est_actif_utilisateur;
    }

    public void setEst_actif_utilisateur(boolean est_actif_utilisateur) {
        this.est_actif_utilisateur = est_actif_utilisateur;
    }

    public String getBio_utilisateur() {
        return bio_utilisateur;
    }

    public void setBio_utilisateur(String bio_utilisateur) {
        this.bio_utilisateur = bio_utilisateur;
    }

    @Override
    public String toString() {
        return "utilisateurs{" +
                "id_utilisateur='" + id_utilisateur + '\'' +
                ", nom_utilisateur='" + nom_utilisateur + '\'' +
                ", prenom_utilisateur='" + prenom_utilisateur + '\'' +
                ", email_utilisateur='" + email_utilisateur + '\'' +
                ", mdp_utilisateur='" + mdp_utilisateur + '\'' +
                ", telephone_utilisateur='" + telephone_utilisateur + '\'' +
                ", date_naissance_utilisateur=" + date_naissance_utilisateur +
                ", role_utilisateur='" + role_utilisateur +
                ", photo_profil_utilisateur='" + photo_profil_utilisateur + '\'' +
                ", date_inscription_utilisateur=" + date_inscription_utilisateur +
                ", est_actif_utilisateur=" + est_actif_utilisateur +
                ", bio_utilisateur='" + bio_utilisateur + '\'' +
                '}';
    }
}
