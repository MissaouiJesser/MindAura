package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import tn.esprit.entities.utilisateurs;
import tn.esprit.utils.EmailService;

/**
 * Contrôleur — Étape 2 du flux "Mot de passe oublié".
 * L'utilisateur saisit le code reçu par email.
 * Le code est comparé à celui généré à l'étape 1.
 * Expire après 10 minutes (timestamp).
 */
public class VerificationCodeController {

    @FXML private Label     sousTitreLabel;
    @FXML private TextField codeField;
    @FXML private Label     errorLabel;
    @FXML private Label     btnRenvoyer;

    private LoginController loginController;
    private String          codeAttendu;
    private String          emailUtilisateur;
    private utilisateurs    userCible;
    private long            codeTimestamp; // Pour vérifier expiration (10 min)

    /** Injecté par LoginController */
    public void setLoginController(LoginController lc) {
        this.loginController = lc;
    }

    /**
     * Données transmises depuis MotDePasseOublieController via LoginController.
     * @param email  email de l'utilisateur
     * @param code   code 6 chiffres généré
     * @param user   utilisateur trouvé en BDD
     */
    public void setData(String email, String code, utilisateurs user) {
        this.emailUtilisateur = email;
        this.codeAttendu      = code;
        this.userCible        = user;
        this.codeTimestamp    = System.currentTimeMillis();

        // Afficher email masqué pour sécurité (ex: u***@gmail.com)
        String emailMasque = masquerEmail(email);
        if (sousTitreLabel != null)
            sousTitreLabel.setText("Code envoyé à " + emailMasque);

        // Limiter le champ à 6 chiffres uniquement
        if (codeField != null) {
            codeField.textProperty().addListener((o, ov, nv) -> {
                if (nv != null && nv.length() > 6)
                    codeField.setText(nv.substring(0, 6));
                if (nv != null && !nv.matches("\\d*"))
                    codeField.setText(nv.replaceAll("[^\\d]", ""));
            });
        }
    }

    // =========================================================================
    //  ACTIONS
    // =========================================================================

    @FXML
    private void handleVerifier() {
        String codeSaisi = codeField.getText().trim();
        errorLabel.setText("");

        // Vérification expiration (10 minutes = 600 000 ms)
        long elapsed = System.currentTimeMillis() - codeTimestamp;
        if (elapsed > 600_000) {
            showError("Le code a expiré. Veuillez en demander un nouveau.");
            return;
        }

        if (codeSaisi.isEmpty() || codeSaisi.length() != 6) {
            showError("Veuillez saisir le code à 6 chiffres.");
            return;
        }

        if (codeSaisi.equals(codeAttendu)) {
            // Code correct → étape 3
            if (loginController != null) {
                loginController.showNouveauMotDePasse(userCible);
            }
        } else {
            showError("Code incorrect. Veuillez réessayer.");
            codeField.clear();
            // Animation shake
            javafx.animation.TranslateTransition shake =
                    new javafx.animation.TranslateTransition(
                            javafx.util.Duration.millis(60), codeField);
            shake.setCycleCount(6);
            shake.setAutoReverse(true);
            shake.setByX(8);
            shake.play();
        }
    }

    @FXML
    private void handleRenvoyer(MouseEvent e) {
        if (emailUtilisateur == null || userCible == null) return;

        // Générer un nouveau code
        String nouveauCode = String.format("%06d", (int)(Math.random() * 900000) + 100000);
        this.codeAttendu   = nouveauCode;
        this.codeTimestamp = System.currentTimeMillis();

        btnRenvoyer.setDisable(true);
        errorLabel.setStyle("-fx-text-fill:#2D6A4F; -fx-font-size:11px; -fx-font-weight:bold;");
        errorLabel.setText("Envoi en cours...");

        EmailService.envoyerCodeReinitialisation(
                emailUtilisateur,
                userCible.getPrenom_utilisateur(),
                nouveauCode,
                () -> {
                    errorLabel.setStyle("-fx-text-fill:#2D6A4F; -fx-font-size:11px; -fx-font-weight:bold;");
                    errorLabel.setText("Nouveau code envoyé !");
                    btnRenvoyer.setDisable(false);
                    codeField.clear();
                },
                err -> {
                    showError("Erreur d'envoi : " + err);
                    btnRenvoyer.setDisable(false);
                }
        );
    }

    @FXML
    private void handleRetour(MouseEvent e) {
        if (loginController != null) {
            loginController.showMotDePasseOublie();
        }
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private void showError(String msg) {
        errorLabel.setStyle("-fx-text-fill:#E74C3C; -fx-font-size:11px; -fx-font-weight:bold;");
        errorLabel.setText(msg);
    }

    /**
     * Masque l'email pour affichage sécurisé.
     * Ex: "user@gmail.com" → "u***@gmail.com"
     */
    private String masquerEmail(String email) {
        if (email == null || !email.contains("@")) return "votre email";
        int at = email.indexOf('@');
        String local  = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return local.charAt(0) + "***" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
