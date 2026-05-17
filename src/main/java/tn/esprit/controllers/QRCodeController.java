package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;
import tn.esprit.services.QRCodeService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class QRCodeController implements Initializable {

    @FXML private ImageView imgQRCode;
    @FXML private Label     lblUrl;
    @FXML private Label     lblStatut;
    @FXML private Label     lblInstructions;
    @FXML private Button    btnFermer;
    @FXML private Button    btnSauvegarder;
    @FXML private Button    btnArreterServeur;
    @FXML private Button    btnRelancer;

    private BufferedImage currentTicketImage;
    private Participation currentParticipation;
    private Evenements    currentEvenement;

    private final QRCodeService qrCodeService = new QRCodeService();

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Masquer les boutons inutiles en mode ticket
        if (btnArreterServeur != null) btnArreterServeur.setVisible(false);
        if (btnRelancer       != null) btnRelancer.setVisible(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DONNÉES DU TICKET  (appelé depuis ParticipationFormController)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reçoit les données de participation + événement,
     * génère le ticket QR et l'affiche dans la fenêtre.
     */
    public void setTicketData(Participation p, Evenements e) {
        this.currentParticipation = p;
        this.currentEvenement     = e;

        // Statut initial
        if (lblStatut != null) {
            lblStatut.setText("⏳ Génération du ticket...");
            lblStatut.setStyle("-fx-text-fill: #2D6A4F; -fx-font-weight: bold;");
        }

        // Sous-titre : nom du participant et de l'événement
        if (lblUrl != null) {
            lblUrl.setText(p.getNom() + " — " + e.getTitreEvenement());
        }

        // Génération dans un thread séparé pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                BufferedImage ticket = qrCodeService.genererTicket(p, e);
                currentTicketImage = ticket;

                javafx.scene.image.Image fxImage = SwingFXUtils.toFXImage(ticket, null);

                Platform.runLater(() -> {
                    if (imgQRCode != null) {
                        imgQRCode.setImage(fxImage);
                        imgQRCode.setFitWidth(320);
                        imgQRCode.setFitHeight(320);
                        imgQRCode.setPreserveRatio(true);
                        imgQRCode.setSmooth(true);
                        applyFadeIn();
                    }

                    if (lblStatut != null) {
                        lblStatut.setText("✅ Ticket généré avec succès !");
                        lblStatut.setStyle("-fx-text-fill: #1B4332; -fx-font-weight: bold;");
                    }

                    if (lblInstructions != null) {
                        lblInstructions.setText(
                                "🎫 Présentez ce QR code à l'entrée de l'événement.\n" +
                                        "Un email de confirmation vous a été envoyé."
                        );
                    }
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (lblStatut != null) {
                        lblStatut.setText("❌ Erreur : " + ex.getMessage());
                        lblStatut.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                    }
                });
                System.err.println("[QRCodeController] Erreur génération : " + ex.getMessage());
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACTIONS BOUTONS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleFermer() {
        Stage stage = (Stage) btnFermer.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSauvegarder() {
        if (currentTicketImage == null) {
            if (lblStatut != null) lblStatut.setText("⚠ Aucun ticket à sauvegarder.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le Ticket QR");
        chooser.setInitialFileName("ticket_" +
                (currentParticipation != null
                        ? currentParticipation.getNom().replace(" ", "_")
                        : "participant")
                + ".png");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images PNG", "*.png"));

        File file = chooser.showSaveDialog(btnSauvegarder.getScene().getWindow());
        if (file != null) {
            try {
                ImageIO.write(currentTicketImage, "PNG", file);
                if (lblStatut != null) {
                    lblStatut.setText("✅ Ticket sauvegardé → " + file.getName());
                    lblStatut.setStyle("-fx-text-fill: #1B4332; -fx-font-weight: bold;");
                }
            } catch (Exception ex) {
                if (lblStatut != null) {
                    lblStatut.setText("❌ Erreur de sauvegarde : " + ex.getMessage());
                    lblStatut.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                }
            }
        }
    }

    // Boutons désactivés en mode ticket (cachés dans initialize)
    @FXML private void handleArreterServeur() { /* non utilisé */ }
    @FXML private void handleRelancer()       { /* non utilisé */ }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANIMATION
    // ─────────────────────────────────────────────────────────────────────────

    private void applyFadeIn() {
        if (imgQRCode != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(600), imgQRCode);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }
}