package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.utilisateurs;
import tn.esprit.services.utilisateurs_service;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Contrôleur — Étape 3 du flux "Mot de passe oublié".
 * L'utilisateur saisit et confirme son nouveau mot de passe.
 * Après validation, le mot de passe est mis à jour en BDD
 * et l'utilisateur est redirigé vers le formulaire de connexion.
 */
public class NouveauMotDePasseController {

    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private PasswordField confirmField;
    @FXML private TextField     confirmVisible;
    @FXML private Label         errorLabel;

    private boolean pwdVisible     = false;
    private boolean confirmVisible2 = false;

    private LoginController loginController;
    private utilisateurs    userCible;
    private final utilisateurs_service userService = new utilisateurs_service();

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{6,}$");

    /** Injecté par LoginController */
    public void setLoginController(LoginController lc) {
        this.loginController = lc;
    }

    /** Utilisateur dont on va modifier le mot de passe */
    public void setUser(utilisateurs user) {
        this.userCible = user;

        // Synchronisation PasswordField ↔ TextField (même logique que LoginController)
        bindToggle(passwordField, passwordVisible);
        bindToggle(confirmField, confirmVisible);
    }

    // =========================================================================
    //  ACTIONS
    // =========================================================================

    @FXML
    private void handleTogglePassword() {
        pwdVisible = !pwdVisible;
        if (pwdVisible) {
            passwordVisible.setVisible(true);  passwordVisible.setManaged(true);
            passwordField.setVisible(false);   passwordField.setManaged(false);
        } else {
            passwordField.setVisible(true);    passwordField.setManaged(true);
            passwordVisible.setVisible(false); passwordVisible.setManaged(false);
        }
    }

    @FXML
    private void handleToggleConfirm() {
        confirmVisible2 = !confirmVisible2;
        if (confirmVisible2) {
            confirmVisible.setVisible(true);  confirmVisible.setManaged(true);
            confirmField.setVisible(false);   confirmField.setManaged(false);
        } else {
            confirmField.setVisible(true);    confirmField.setManaged(true);
            confirmVisible.setVisible(false); confirmVisible.setManaged(false);
        }
    }

    @FXML
    private void handleEnregistrer() {
        String mdp     = passwordField.getText();
        String confirm = confirmField.getText();
        errorLabel.setText("");

        // Validation
        if (!PASSWORD_PATTERN.matcher(mdp).matches()) {
            showError("Mot de passe : minimum 6 caractères, 1 majuscule, 1 chiffre.");
            return;
        }
        if (!mdp.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        // Mise à jour en BDD
        try {
            userCible.setMdp_utilisateur(mdp);
            userService.modifier(userCible);

            // Succès → retour à la connexion avec message
            if (loginController != null) {
                loginController.showLoginPanelAvecSucces(
                        "✓ Mot de passe modifié ! Vous pouvez maintenant vous connecter."
                );
            }

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#E74C3C; -fx-font-size:11px; -fx-font-weight:bold;");
        errorLabel.setText(msg);
    }

    private void bindToggle(PasswordField pf, TextField tf) {
        if (pf == null || tf == null) return;
        pf.textProperty().addListener((o, ov, nv) -> { if (!tf.getText().equals(nv)) tf.setText(nv); });
        tf.textProperty().addListener((o, ov, nv) -> { if (!pf.getText().equals(nv)) pf.setText(nv); });
    }
}
