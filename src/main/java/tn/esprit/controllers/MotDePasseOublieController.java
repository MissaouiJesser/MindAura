package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import tn.esprit.entities.utilisateurs;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.utils.EmailService;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Contrôleur — Étape 1 du flux "Mot de passe oublié".
 * L'utilisateur saisit son email ; si le compte existe, un code 6 chiffres
 * est généré et envoyé via EmailService (Gmail SMTP).
 *
 * Cette vue est chargée DANS le LoginController (même fenêtre, même panneau droit).
 */
public class MotDePasseOublieController {

    @FXML private TextField emailField;
    @FXML private Label     errorLabel;
    @FXML private Button    btnEnvoyer;

    private LoginController loginController;
    private final utilisateurs_service userService = new utilisateurs_service();

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /** Appelé par LoginController pour passer la référence */
    public void setLoginController(LoginController lc) {
        this.loginController = lc;
    }

    // =========================================================================
    //  ACTIONS
    // =========================================================================

    @FXML
    private void handleEnvoyer() {
        String email = emailField.getText().trim();

        // Validation format email
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            showError("Veuillez saisir une adresse email valide.");
            return;
        }

        // Désactiver le bouton pour éviter les doubles clics
        btnEnvoyer.setDisable(true);
        btnEnvoyer.setText("Vérification...");
        errorLabel.setText("");

        // Vérification existence en base + envoi email en arrière-plan
        new Thread(() -> {
            try {
                List<utilisateurs> users = userService.afficherList();
                utilisateurs found = null;
                for (utilisateurs u : users) {
                    if (u.getEmail_utilisateur().equalsIgnoreCase(email)) {
                        found = u;
                        break;
                    }
                }

                final utilisateurs userFound = found;

                javafx.application.Platform.runLater(() -> {
                    if (userFound == null) {
                        showError("Aucun compte n'est associé à cette adresse email.");
                        btnEnvoyer.setDisable(false);
                        btnEnvoyer.setText("Envoyer le code de vérification");
                        return;
                    }

                    // Générer un code à 6 chiffres
                    String code = String.format("%06d", (int)(Math.random() * 900000) + 100000);

                    // Envoyer l'email avec le code (asynchrone)
                    EmailService.envoyerCodeReinitialisation(
                            userFound.getEmail_utilisateur(),
                            userFound.getPrenom_utilisateur(),
                            code,
                            () -> {
                                // Succès → naviguer vers étape 2
                                if (loginController != null) {
                                    loginController.showVerificationCode(email, code, userFound);
                                }
                            },
                            err -> {
                                showError("Erreur d'envoi email : " + err);
                                btnEnvoyer.setDisable(false);
                                btnEnvoyer.setText("Envoyer le code de vérification");
                            }
                    );

                    btnEnvoyer.setText("Envoi en cours...");
                });

            } catch (SQLException e) {
                javafx.application.Platform.runLater(() -> {
                    showError("Erreur base de données : " + e.getMessage());
                    btnEnvoyer.setDisable(false);
                    btnEnvoyer.setText("Envoyer le code de vérification");
                });
            }
        }).start();
    }

    @FXML
    private void handleRetour(MouseEvent e) {
        if (loginController != null) {
            loginController.showLoginPanel();
        }
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#E74C3C; -fx-font-size:11px; -fx-font-weight:bold;");
        errorLabel.setText(msg);
    }
}
