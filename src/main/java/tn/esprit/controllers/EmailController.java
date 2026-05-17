package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.services.EmailService;
import tn.esprit.services.EmailService.EmailResult;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

/**
 * Contrôleur interface emails MindAura
 * Onglet 1 : Confirmation de réservation
 * Onglet 2 : Rappel de séance
 */
public class EmailController implements Initializable {

    // ── Onglet 1 : Confirmation ───────────────────────
    @FXML private TextField            txtConfNomPatient;
    @FXML private TextField            txtConfNomPsy;
    @FXML private DatePicker           dpConfDate;
    @FXML private TextField            txtConfHeure;
    @FXML private ComboBox<String>     comboConfType;
    @FXML private TextField            txtConfLieu;
    // CORRECTION #3 : champ email destinataire (n'était pas hardcodé)
    @FXML private TextField            txtConfEmail;
    @FXML private Button               btnEnvoyerConf;
    @FXML private Label                lblStatutConf;

    // ── Onglet 2 : Rappel ─────────────────────────────
    @FXML private TextField            txtRappNomPatient;
    @FXML private TextField            txtRappNomPsy;
    @FXML private DatePicker           dpRappDate;
    @FXML private TextField            txtRappHeure;
    @FXML private ComboBox<String>     comboRappDelai;
    @FXML private TextField            txtRappLienVisio;
    // CORRECTION #3 : champ email destinataire (n'était pas hardcodé)
    @FXML private TextField            txtRappEmail;
    @FXML private Button               btnEnvoyerRapp;
    @FXML private Label                lblStatutRapp;

    // ── Commun ────────────────────────────────────────
    @FXML private ProgressIndicator    progressIndicator;
    @FXML private Button               btnFermer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Types de séances
        comboConfType.getItems().addAll(
                "Séance individuelle",
                "Thérapie de couple",
                "Thérapie familiale",
                "Consultation psychiatrique",
                "Suivi psychologique",
                "Séance de groupe"
        );
        comboConfType.getSelectionModel().selectFirst();

        // Délais de rappel
        comboRappDelai.getItems().addAll(
                "24h avant (J-1)",
                "12h avant",
                "6h avant",
                "2h avant",
                "1h avant"
        );
        comboRappDelai.getSelectionModel().selectFirst();

        progressIndicator.setVisible(false);
    }

    // =====================================================
    //  CONFIRMATION
    // =====================================================

    @FXML
    private void handleEnvoyerConfirmation() {
        // CORRECTION #3 : validation du champ email au lieu du hardcodage
        if (!valider(txtConfNomPatient, txtConfNomPsy, txtConfLieu, txtConfEmail)) {
            statut(lblStatutConf, "Veuillez remplir tous les champs obligatoires.", false);
            return;
        }
        if (dpConfDate.getValue() == null) {
            statut(lblStatutConf, "Veuillez sélectionner une date.", false);
            return;
        }

        // CORRECTION #4 : setLoading avec le texte propre au bouton
        setLoading(true, btnEnvoyerConf, "Envoyer la confirmation");

        LocalDateTime dt = parseDateTime(dpConfDate.getValue().toString(),
                txtConfHeure.getText());

        new Thread(() -> {
            EmailResult r = EmailService.envoyerConfirmation(
                    txtConfEmail.getText().trim(),        // CORRECTION #3 : email depuis le champ
                    txtConfNomPatient.getText().trim(),
                    txtConfNomPsy.getText().trim(),
                    dt,
                    comboConfType.getValue(),
                    txtConfLieu.getText().trim()
            );
            Platform.runLater(() -> {
                // CORRECTION #4 : texte correct restauré
                setLoading(false, btnEnvoyerConf, "Envoyer la confirmation");
                statut(lblStatutConf, r.toString(), r.succes);  // r.succes fonctionne désormais
            });
        }).start();
    }

    // =====================================================
    //  RAPPEL
    // =====================================================

    @FXML
    private void handleEnvoyerRappel() {
        // CORRECTION #3 : validation du champ email au lieu du hardcodage
        if (!valider(txtRappNomPatient, txtRappNomPsy, txtRappEmail)) {
            statut(lblStatutRapp, "Veuillez remplir tous les champs obligatoires.", false);
            return;
        }
        if (dpRappDate.getValue() == null) {
            statut(lblStatutRapp, "Veuillez sélectionner une date.", false);
            return;
        }

        // CORRECTION #4 : setLoading avec le texte propre au bouton
        setLoading(true, btnEnvoyerRapp, "Envoyer le rappel");

        LocalDateTime dt  = parseDateTime(dpRappDate.getValue().toString(),
                txtRappHeure.getText());
        int heures        = parseDelai(comboRappDelai.getValue());
        String lien       = txtRappLienVisio.getText().trim();

        new Thread(() -> {
            EmailResult r = EmailService.envoyerRappel(
                    txtRappEmail.getText().trim(),        // CORRECTION #3 : email depuis le champ
                    txtRappNomPatient.getText().trim(),
                    txtRappNomPsy.getText().trim(),
                    dt,
                    heures,
                    lien
            );
            Platform.runLater(() -> {
                // CORRECTION #4 : texte correct restauré
                setLoading(false, btnEnvoyerRapp, "Envoyer le rappel");
                statut(lblStatutRapp, r.toString(), r.succes);  // r.succes fonctionne désormais
            });
        }).start();
    }

    // =====================================================
    //  UTILITAIRES
    // =====================================================

    private boolean valider(TextField... champs) {
        boolean ok = true;
        for (TextField f : champs) {
            if (f == null) continue;
            boolean vide = f.getText() == null || f.getText().trim().isEmpty();
            f.setStyle(vide
                    ? "-fx-border-color:#EF4444;-fx-border-width:1.5;-fx-border-radius:8;"
                    : "-fx-border-color:#D1D5DB;-fx-border-width:1;-fx-border-radius:8;");
            if (vide) ok = false;
        }
        return ok;
    }

    private void statut(Label lbl, String msg, boolean succes) {
        lbl.setText(succes ? "✔  " + msg : "✖  " + msg);
        lbl.setStyle(succes
                ? "-fx-text-fill:#2D6A4F;-fx-font-weight:bold;"
                : "-fx-text-fill:#EF4444;-fx-font-weight:bold;");
    }

    // CORRECTION #4 : signature étendue avec texteOriginal pour distinguer les deux boutons
    private void setLoading(boolean on, Button btn, String texteOriginal) {
        progressIndicator.setVisible(on);
        btn.setDisable(on);
        btn.setText(on ? "Envoi en cours..." : texteOriginal);
    }

    private LocalDateTime parseDateTime(String date, String heure) {
        try {
            String h = (heure == null || heure.isBlank()) ? "09:00" : heure;
            String[] p = h.split(":");
            return LocalDateTime.parse(date + "T"
                    + String.format("%02d", Integer.parseInt(p[0])) + ":"
                    + String.format("%02d", p.length > 1 ? Integer.parseInt(p[1]) : 0)
                    + ":00");
        } catch (Exception e) {
            return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        }
    }

    private int parseDelai(String delai) {
        if (delai == null) return 24;
        if (delai.startsWith("24")) return 24;
        if (delai.startsWith("12")) return 12;
        if (delai.startsWith("6"))  return 6;
        if (delai.startsWith("2"))  return 2;
        if (delai.startsWith("1"))  return 1;
        return 24;
    }

    @FXML
    private void handleFermer() {
        ((Stage) btnFermer.getScene().getWindow()).close();
    }
}