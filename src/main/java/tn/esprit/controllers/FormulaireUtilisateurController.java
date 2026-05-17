package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Role;
import tn.esprit.services.utilisateurs_service;

import java.net.URL;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur du formulaire Ajouter / Modifier utilisateur.
 * Aucun ID n'est affiché à l'utilisateur.
 */
public class FormulaireUtilisateurController implements Initializable {

    @FXML private Label         formTitle;

    // Champs
    @FXML private TextField     fieldPrenom;
    @FXML private TextField     fieldNom;
    @FXML private TextField     fieldEmail;
    @FXML private TextField     fieldTelephone;
    @FXML private DatePicker    fieldDateNaissance;
    @FXML private ComboBox<String> fieldRole;
    @FXML private PasswordField fieldPassword;
    @FXML private PasswordField fieldPasswordConfirm;
    @FXML private CheckBox      fieldActif;
    @FXML private TextArea      fieldBio;
    @FXML private HBox          passwordRow;

    // Erreurs
    @FXML private Label errPrenom;
    @FXML private Label errNom;
    @FXML private Label errEmail;
    @FXML private Label errDate;
    @FXML private Label errRole;
    @FXML private Label errPassword;
    @FXML private Label errGlobal;

    private utilisateurs userToEdit = null; // null = mode ajout
    private final utilisateurs_service service = new utilisateurs_service();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Peupler le ComboBox des rôles avec libellés lisibles (pas de "ROLE_" affiché)
        fieldRole.setItems(FXCollections.observableArrayList(
            Role.ROLE_ADMIN.getLibelle(),
            Role.ROLE_PSYCHOLOGUE.getLibelle(),
            Role.ROLE_PATIENT.getLibelle(),
            Role.ROLE_COACH.getLibelle(),
            Role.ROLE_USER.getLibelle()
        ));
    }

    /**
     * Appelé depuis ListeUtilisateursController pour pré-remplir le formulaire en mode édition.
     * @param user l'utilisateur à modifier
     */
    public void setUserToEdit(utilisateurs user) {
        this.userToEdit = user;
        formTitle.setText("Modifier l'utilisateur");

        // Pré-remplissage des champs (sans afficher l'ID)
        fieldPrenom.setText(user.getPrenom_utilisateur());
        fieldNom.setText(user.getNom_utilisateur());
        fieldEmail.setText(user.getEmail_utilisateur());
        fieldTelephone.setText(user.getTelephone_utilisateur() != null
                ? user.getTelephone_utilisateur() : "");
        fieldActif.setSelected(user.isEst_actif_utilisateur());
        fieldBio.setText(user.getBio_utilisateur() != null
                ? user.getBio_utilisateur() : "");

        if (user.getDate_naissance_utilisateur() != null) {
            fieldDateNaissance.setValue(
                user.getDate_naissance_utilisateur()
                    .toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            );
        }

        // Rôle
        fieldRole.setValue(user.getRole_utilisateur().getLibelle());

        // En mode édition, masquer les champs mot de passe (optionnel)
        passwordRow.setVisible(false);
        passwordRow.setManaged(false);
    }

    /**
     * Valide et sauvegarde l'utilisateur.
     * @return true si succès, false sinon
     */
    public boolean save() {
        clearErrors();

        // ─── Validation ───────────────────────────────────────────────────────
        boolean valid = true;

        if (fieldPrenom.getText().trim().isEmpty()) {
            errPrenom.setText("Le prénom est obligatoire.");
            valid = false;
        }
        if (fieldNom.getText().trim().isEmpty()) {
            errNom.setText("Le nom est obligatoire.");
            valid = false;
        }
        if (fieldEmail.getText().trim().isEmpty()
                || !fieldEmail.getText().contains("@")) {
            errEmail.setText("Email invalide.");
            valid = false;
        }
        if (fieldDateNaissance.getValue() == null) {
            errDate.setText("Date obligatoire.");
            valid = false;
        }
        if (fieldRole.getValue() == null) {
            errRole.setText("Veuillez choisir un rôle.");
            valid = false;
        }

        // Mot de passe seulement en mode ajout
        if (userToEdit == null) {
            if (fieldPassword.getText().length() < 6) {
                errPassword.setText("Minimum 6 caractères.");
                valid = false;
            } else if (!fieldPassword.getText().equals(fieldPasswordConfirm.getText())) {
                errPassword.setText("Les mots de passe ne correspondent pas.");
                valid = false;
            }
        }

        if (!valid) return false;

        // ─── Vérification email unique ─────────────────────────────────────────
        try {
            List<utilisateurs> existing = service.afficherList();
            for (utilisateurs u : existing) {
                // Ignorer l'utilisateur qu'on est en train de modifier
                if (userToEdit != null && u.getId_utilisateur().equals(userToEdit.getId_utilisateur()))
                    continue;
                if (u.getEmail_utilisateur().equalsIgnoreCase(fieldEmail.getText().trim())) {
                    errEmail.setText("Cet email est déjà utilisé.");
                    return false;
                }
            }
        } catch (SQLException e) {
            errGlobal.setText("Erreur de vérification : " + e.getMessage());
            return false;
        }

        // ─── Construction de l'objet ───────────────────────────────────────────
        utilisateurs u = (userToEdit != null) ? userToEdit : new utilisateurs();

        u.setPrenom_utilisateur(fieldPrenom.getText().trim());
        u.setNom_utilisateur(fieldNom.getText().trim());
        u.setEmail_utilisateur(fieldEmail.getText().trim());
        u.setTelephone_utilisateur(fieldTelephone.getText().trim().isEmpty()
                ? null : fieldTelephone.getText().trim());
        u.setEst_actif_utilisateur(fieldActif.isSelected());
        u.setBio_utilisateur(fieldBio.getText().trim().isEmpty()
                ? null : fieldBio.getText().trim());
        u.setDate_naissance_utilisateur(
            Date.from(fieldDateNaissance.getValue()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant())
        );

        // Convertir libellé → enum Role
        String libelle = fieldRole.getValue();
        for (Role r : Role.values()) {
            if (r.getLibelle().equals(libelle)) {
                u.setRole_utilisateur(r);
                break;
            }
        }

        // Mot de passe seulement en mode ajout
        if (userToEdit == null) {
            u.setMdp_utilisateur(fieldPassword.getText());
            u.setDate_inscription_utilisateur(new Date());
        }

        // ─── Persistance ───────────────────────────────────────────────────────
        try {
            if (userToEdit == null) {
                service.addMeth2(u);
            } else {
                service.modifier(u);
            }
            return true;
        } catch (SQLException e) {
            errGlobal.setText("Erreur : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void clearErrors() {
        errPrenom.setText("");
        errNom.setText("");
        errEmail.setText("");
        errDate.setText("");
        errRole.setText("");
        errPassword.setText("");
        errGlobal.setText("");
    }
}
