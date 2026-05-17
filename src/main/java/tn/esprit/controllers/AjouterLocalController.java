package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.enums.typeL;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.utils.ImageManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterLocalController extends TranslatableController {

    @FXML private TextField    txtNom;
    @FXML private TextField    txtAdresse;
    @FXML private TextField    txtVille;
    @FXML private TextArea     txtDescription;
    @FXML private TextField    txtCapacite;
    @FXML private ComboBox<typeL> comboType;
    @FXML private TextField    txtTelephone;
    @FXML private TextField    txtEmail;
    @FXML private ComboBox<String> comboDisponibilite;
    @FXML private TextField    txtImageURL;
    @FXML private Button       btnParcourir;
    @FXML private Button       btnAjouter;
    @FXML private Button       btnAnnuler;
    @FXML private Label        lblMessage;
    @FXML private ImageView    imgPreview;

    // Labels du formulaire (ajouter fx:id dans AjouterLocal.fxml si absent)
    @FXML private Label lblTitreFormulaire;
    @FXML private Label lblNom;
    @FXML private Label lblAdresse;
    @FXML private Label lblVille;
    @FXML private Label lblDescription;
    @FXML private Label lblCapacite;
    @FXML private Label lblType;
    @FXML private Label lblTelephone;
    @FXML private Label lblEmail;
    @FXML private Label lblDisponibilite;
    @FXML private Label lblImage;

    private local_psychiatrie_SERVICE localService;
    private AfficherLocalController parentController;
    private File selectedImageFile;

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void initComponents(URL url, ResourceBundle resourceBundle) {
        localService = new local_psychiatrie_SERVICE();
        ImageManager.initializeImageDirectory();

        comboType.setItems(FXCollections.observableArrayList(typeL.values()));
        comboType.setValue(typeL.HOPITAL);

        comboDisponibilite.setItems(FXCollections.observableArrayList(
                "Disponible", "Non disponible", "Sur réservation"));
        comboDisponibilite.setValue("Disponible");

        applyCSSStyles();
        applyFadeInAnimation();
        setupValidation();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRADUCTION COMPLÈTE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void traduireUI(String langCode) {
        // Titre
        if (lblTitreFormulaire != null) tr(lblTitreFormulaire, "➕ Ajouter un Local");

        // Labels champs
        if (lblNom          != null) tr(lblNom,          "Nom du local");
        if (lblAdresse      != null) tr(lblAdresse,      "Adresse");
        if (lblVille        != null) tr(lblVille,        "Ville");
        if (lblDescription  != null) tr(lblDescription,  "Description");
        if (lblCapacite     != null) tr(lblCapacite,     "Capacité");
        if (lblType         != null) tr(lblType,         "Type de local");
        if (lblTelephone    != null) tr(lblTelephone,    "Téléphone");
        if (lblEmail        != null) tr(lblEmail,        "Email");
        if (lblDisponibilite!= null) tr(lblDisponibilite,"Disponibilité");
        if (lblImage        != null) tr(lblImage,        "Image");

        // Boutons
        tr(btnAjouter,   "✓ Ajouter");
        tr(btnAnnuler,   "Annuler");
        tr(btnParcourir, "📂 Parcourir");

        // Placeholders
        trPrompt(txtNom,         "Ex: Clinique Ibn Sina");
        trPrompt(txtAdresse,     "Ex: 12 Rue de la Santé");
        trPrompt(txtVille,       "Ex: Tunis");
        trPrompt(txtTelephone,   "8 chiffres");
        trPrompt(txtEmail,       "exemple@email.com");
        trPrompt(txtCapacite,    "Ex: 20");
        trPrompt(txtImageURL,    "Sélectionner une image...");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STYLES & VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private void applyCSSStyles() {
        btnAjouter.getStyleClass().add("btn-primary");
        btnAnnuler.getStyleClass().add("btn-annuler");
        btnParcourir.getStyleClass().add("btn-outline");
    }

    private void setupValidation() {
        txtTelephone.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) txtTelephone.setText(oldVal);
        });
        txtEmail.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateEmail();
        });
    }

    private boolean validateEmail() {
        String email = txtEmail.getText();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            txtEmail.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
            return false;
        }
        txtEmail.setStyle("-fx-border-color: #2D6A4F; -fx-border-width: 1px;");
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleParcourir() {
        File selectedFile = ImageManager.selectImageFile(btnParcourir.getScene().getWindow());
        if (selectedFile != null) {
            if (ImageManager.isValidImageFile(selectedFile)) {
                selectedImageFile = selectedFile;
                txtImageURL.setText(selectedFile.getName());
                if (imgPreview != null) {
                    imgPreview.setImage(
                            ImageManager.loadImage(selectedFile.getAbsolutePath(), 200, 150, true));
                }
                showMessage("✓ Image sélectionnée: " + selectedFile.getName(), "success");
            } else {
                showMessage("✗ Fichier image invalide (PNG, JPG, JPEG, GIF)", "error");
            }
        }
    }

    @FXML
    private void handleAjouter() {
        if (validateForm()) {
            try {
                String imagePath = null;
                if (selectedImageFile != null) {
                    imagePath = ImageManager.saveImage(selectedImageFile);
                    if (imagePath == null)
                        showMessage("⚠ L'image n'a pas pu être sauvegardée", "warning");
                }

                local_psychiatrie local = new local_psychiatrie(
                        txtNom.getText().trim(),
                        txtAdresse.getText().trim(),
                        txtVille.getText().trim(),
                        txtDescription.getText().trim(),
                        txtCapacite.getText().trim(),
                        comboType.getValue(),
                        Integer.parseInt(txtTelephone.getText().trim()),
                        comboDisponibilite.getValue(),
                        txtEmail.getText().trim(),
                        imagePath != null ? imagePath : ""
                );

                localService.add(local);
                showMessage("✓ Local ajouté avec succès!", "success");

                if (parentController != null) parentController.refreshTable();

                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(this::handleAnnuler);
                }).start();

            } catch (SQLException e) {
                showMessage("✗ Erreur lors de l'ajout: " + e.getMessage(), "error");
            } catch (NumberFormatException e) {
                showMessage("✗ Format du téléphone invalide", "error");
            } catch (java.io.IOException e) { showMessage("x Erreur image : " + e.getMessage(), "error");}
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (txtNom.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
            txtNom.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtNom.setStyle("");
        if (txtAdresse.getText().trim().isEmpty()) {
            errors.append("- L'adresse est obligatoire\n");
            txtAdresse.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtAdresse.setStyle("");
        if (txtVille.getText().trim().isEmpty()) {
            errors.append("- La ville est obligatoire\n");
            txtVille.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtVille.setStyle("");
        if (txtTelephone.getText().trim().isEmpty() || txtTelephone.getText().trim().length() != 8) {
            errors.append("- Le téléphone doit contenir 8 chiffres\n");
            txtTelephone.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtTelephone.setStyle("");
        if (txtEmail.getText().trim().isEmpty()) {
            errors.append("- L'email est obligatoire\n");
            txtEmail.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else if (!validateEmail()) {
            errors.append("- L'email n'est pas valide\n");
        }
        if (txtCapacite.getText().trim().isEmpty()) {
            errors.append("- La capacité est obligatoire\n");
            txtCapacite.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtCapacite.setStyle("");
        if (errors.length() > 0) { showMessage("Erreurs:\n" + errors, "error"); return false; }
        return true;
    }

    @FXML
    private void handleAnnuler() {
        ((Stage) btnAnnuler.getScene().getWindow()).close();
    }

    private void showMessage(String message, String type) {
        lblMessage.setText(message);
        if ("success".equals(type))
            lblMessage.setStyle("-fx-text-fill: #1B4332; -fx-font-weight: bold; " +
                    "-fx-background-color: #D8F3DC; -fx-padding: 12px 20px; " +
                    "-fx-background-radius: 10px; -fx-border-color: #2D6A4F; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;");
        else if ("warning".equals(type))
            lblMessage.setStyle("-fx-text-fill: #7B5EA7; -fx-font-weight: bold; " +
                    "-fx-background-color: #FFF4E6; -fx-padding: 12px 20px; " +
                    "-fx-background-radius: 10px; -fx-border-color: #B794F4; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;");
        else
            lblMessage.setStyle("-fx-text-fill: #7B5EA7; -fx-font-weight: bold; " +
                    "-fx-background-color: #F3E8FF; -fx-padding: 12px 20px; " +
                    "-fx-background-radius: 10px; -fx-border-color: #9B7DC4; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;");
        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
    }

    private void applyFadeInAnimation() {
        if (txtNom != null && txtNom.getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), txtNom.getParent());
            fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        }
    }

    public void setParentController(AfficherLocalController controller) {
        this.parentController = controller;
    }
}