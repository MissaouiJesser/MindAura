package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.utils.ImageManager;

import java.net.URL;
import java.util.ResourceBundle;

public class DetailsLocalController implements Initializable {

    @FXML private Label lblId;
    @FXML private Label lblNom;
    @FXML private Label lblAdresse;
    @FXML private Label lblVille;
    @FXML private Label lblType;
    @FXML private Label lblDescription;
    @FXML private Label lblCapacite;
    @FXML private Label lblTelephone;
    @FXML private Label lblEmail;
    @FXML private Label lblDisponibilite;
    @FXML private ImageView imgLocal;
    @FXML private Button btnFermer;

    private local_psychiatrie currentLocal;

    // ── Référence au contrôleur parent pour revenir à la liste ──────────────
    private AfficherLocalController parentController;

    public void setParentController(AfficherLocalController parentController) {
        this.parentController = parentController;
    }

    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        applyCSSStyles();
        applyFadeInAnimation();
    }

    private void applyCSSStyles() {
        if (btnFermer != null) {
            btnFermer.getStyleClass().add("btn-annuler");
        }
    }

    public void setLocal(local_psychiatrie local) {
        this.currentLocal = local;

        lblId.setText(String.valueOf(local.getId_local()));
        lblNom.setText(local.getNom_local());
        lblAdresse.setText(local.getAdresse_local());
        lblVille.setText(local.getVille_local());
        lblType.setText(local.getType_local().getLibelle());
        lblDescription.setText(local.getDescription_local());
        lblCapacite.setText(local.getCapacite_local() + " personnes");
        lblTelephone.setText(formatPhoneNumber(String.valueOf(local.getTelephone_local())));
        lblEmail.setText(local.getEmail_local());

        setupDisponibiliteBadge(local);
        loadImage(local.getImageURL());
    }

    private void setupDisponibiliteBadge(local_psychiatrie local) {
        lblDisponibilite.getStyleClass().clear();
        lblDisponibilite.getStyleClass().add("badge");

        if (local.getDisponibilite_local().equals("Disponible")) {
            lblDisponibilite.getStyleClass().add("badge-disponible");
            lblDisponibilite.setText("✓ Disponible");
        } else if (local.getDisponibilite_local().equals("Non disponible")) {
            lblDisponibilite.getStyleClass().add("badge-non-disponible");
            lblDisponibilite.setText("✗ Non disponible");
        } else {
            lblDisponibilite.getStyleClass().add("badge-reservation");
            lblDisponibilite.setText("📅 Sur réservation");
        }
    }

    private void loadImage(String imageURL) {
        try {
            if (imageURL != null && !imageURL.trim().isEmpty()) {
                Image image = ImageManager.loadImage(imageURL, 400, 250, true);
                if (image != null) {
                    imgLocal.setImage(image);
                    imgLocal.setPreserveRatio(true);
                    imgLocal.setSmooth(true);

                    FadeTransition fade = new FadeTransition(Duration.millis(500), imgLocal);
                    fade.setFromValue(0.0);
                    fade.setToValue(1.0);
                    fade.play();
                } else {
                    setPlaceholder();
                }
            } else {
                setPlaceholder();
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement image: " + e.getMessage());
            setPlaceholder();
        }
    }

    private void setPlaceholder() {
        imgLocal.setImage(null);
        imgLocal.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #D8F3DC, #F0F4F8); " +
                        "-fx-background-radius: 12px; " +
                        "-fx-border-color: #2D6A4F; " +
                        "-fx-border-width: 2px; " +
                        "-fx-border-radius: 12px; " +
                        "-fx-border-style: dashed;"
        );
    }

    private String formatPhoneNumber(String phone) {
        if (phone != null && phone.length() == 8) {
            return phone.substring(0, 2) + " " + phone.substring(2, 5) + " " + phone.substring(5);
        }
        return phone != null ? phone : "Non spécifié";
    }

    /**
     * ✅ MODIFIÉ : Au lieu de fermer un Stage, on revient à la liste via le parent.
     */
    @FXML
    private void handleFermer() {
        if (parentController != null) {
            parentController.showListView();
        }
    }

    private void applyFadeInAnimation() {
        if (lblId != null && lblId.getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(600), lblId.getParent());
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }
}