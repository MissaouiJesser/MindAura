package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.components.LanguageSelectorComponent;
import tn.esprit.services.TranslationService;
import tn.esprit.utils.LanguageManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour l'écran de choix entre Back Office et Front Office.
 * VERSION MISE À JOUR : avec sélecteur de langue.
 */
public class ChoixController extends TranslatableController {

    @FXML private Button btnFrontOffice;
    @FXML private Button btnBackOffice;

    // ── NOUVEAUX éléments (à ajouter dans Choix.fxml) ───────────────────────────
    @FXML private ComboBox<String> comboLangue;   // sélecteur de langue
    @FXML private Label lblBienvenue;              // "Bienvenue ! Choisissez votre espace"
    @FXML private Label lblSousTitre;              // "Gestion des Locaux Psychiatriques"
    @FXML private Label lblEspacePublic;           // "Espace Public"
    @FXML private Label lblEspaceAdmin;            // "Espace Admin"
    @FXML private Label lblFooter;                 // "© 2026 MindAura..."

    private final TranslationService translationService = TranslationService.getInstance();

    @Override
    protected void initComponents(URL url, ResourceBundle resourceBundle) {

        // ── 1. Sélecteur de langue ───────────────────────────────────────────────
        if (comboLangue != null) {
            LanguageSelectorComponent.setup(comboLangue);
            // Style adapté au fond clair de la page Choix
            comboLangue.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: #D1D5DB; " +
                            "-fx-border-width: 1.5px; " +
                            "-fx-border-radius: 10px; " +
                            "-fx-background-radius: 10px; " +
                            "-fx-font-size: 13px; " +
                            "-fx-cursor: hand; " +
                            "-fx-pref-width: 160px;"
            );
        }

        // ── 2. Écouter les changements de langue ─────────────────────────────────
        LanguageManager.getInstance().addListener(this::onLanguageChanged);

        // ── 3. Animations ────────────────────────────────────────────────────────
        applyFadeInAnimation();
        addHoverEffects();

        // ── 4. Traduire si la langue n'est pas le français ───────────────────────
        String lang = LanguageManager.getInstance().getCurrentLanguage();
        if (!TranslationService.LANG_FR.equals(lang)) {
            onLanguageChanged(lang);
        }

        System.out.println("✅ Écran de choix chargé avec succès");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  TRADUCTION
    // ─────────────────────────────────────────────────────────────────────────────

    private void onLanguageChanged(String langCode) {
        System.out.println("🌐 Choix: traduction vers " + TranslationService.getNomLangue(langCode));

        // Boutons principaux
        traduireButton(btnFrontOffice, "Accéder", langCode);
        traduireButton(btnBackOffice,  "Accéder", langCode);

        // Labels (si fx:id ajoutés dans le FXML)
        traduireLabel(lblBienvenue,   "Bienvenue ! Choisissez votre espace", langCode);
        traduireLabel(lblSousTitre,   "Gestion des Locaux Psychiatriques",   langCode);
        traduireLabel(lblEspacePublic,"Espace Public",                        langCode);
        traduireLabel(lblEspaceAdmin, "Espace Admin",                         langCode);
        traduireLabel(lblFooter,
                "© 2026 MindAura - Votre bien-être mental, notre priorité", langCode);
    }

    private void traduireButton(Button btn, String texteFr, String langCode) {
        if (btn == null) return;
        if (TranslationService.LANG_FR.equals(langCode)) { btn.setText(texteFr); return; }
        translationService.traduireAsync(texteFr, btn::setText);
    }

    private void traduireLabel(Label lbl, String texteFr, String langCode) {
        if (lbl == null) return;
        if (TranslationService.LANG_FR.equals(langCode)) { lbl.setText(texteFr); return; }
        translationService.traduireAsync(texteFr, lbl::setText);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  NAVIGATION (inchangé)
    // ─────────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleFrontOffice() {
        applyClickAnimation(btnFrontOffice);
        new Thread(() -> {
            try {
                Thread.sleep(300);
                javafx.application.Platform.runLater(this::navigateToFrontOffice);
            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleBackOffice() {
        applyClickAnimation(btnBackOffice);
        new Thread(() -> {
            try {
                Thread.sleep(300);
                javafx.application.Platform.runLater(this::navigateToBackOffice);
            } catch (InterruptedException e) { e.printStackTrace(); }
        }).start();
    }

    private void navigateToFrontOffice() {
        naviguerVers("/FrontOfficeAccueil.fxml", "MindAura - Réservation de Locaux",
                1400, 850, btnFrontOffice);
    }

    private void navigateToBackOffice() {
        naviguerVers("/AfficherLocal.fxml", "MindAura - Administration",
                1400, 850, btnBackOffice);
    }

    private void naviguerVers(String fxml, String titre, double w, double h, Button source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();
            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            } catch (Exception ignored) {}
            stage.setTitle(titre);
            stage.setScene(scene);
            stage.setWidth(w); stage.setHeight(h);
            stage.centerOnScreen();
        } catch (IOException e) {
            System.err.println("❌ Erreur navigation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  ANIMATIONS (inchangé)
    // ─────────────────────────────────────────────────────────────────────────────

    private void applyFadeInAnimation() {
        if (btnFrontOffice != null && btnFrontOffice.getParent() != null
                && btnFrontOffice.getParent().getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(800),
                    btnFrontOffice.getParent().getParent());
            fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        }
    }

    private void applyClickAnimation(Button button) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), button);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(0.95);  scale.setToY(0.95);
        scale.setCycleCount(2); scale.setAutoReverse(true); scale.play();
    }

    private void addHoverEffects() {
        addHoverEffect(btnFrontOffice);
        addHoverEffect(btnBackOffice);
    }

    private void addHoverEffect(Button button) {
        if (button == null) return;
        button.setOnMouseEntered(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(200), button);
            s.setToX(1.05); s.setToY(1.05); s.play();
        });
        button.setOnMouseExited(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(200), button);
            s.setToX(1.0); s.setToY(1.0); s.play();
        });
    }

    @Override
    protected void traduireUI(String langCode) {
        tr(btnFrontOffice, "Accéder");
        tr(btnBackOffice,  "Accéder");
        if (lblBienvenue    != null) tr(lblBienvenue,    "Bienvenue ! Choisissez votre espace");
        if (lblSousTitre    != null) tr(lblSousTitre,    "Gestion des Locaux Psychiatriques");
        if (lblEspacePublic != null) tr(lblEspacePublic, "Espace Public");
        if (lblEspaceAdmin  != null) tr(lblEspaceAdmin,  "Espace Admin");
        if (lblFooter       != null) tr(lblFooter,       "© 2026 MindAura - Votre bien-être mental, notre priorité");
    }

}