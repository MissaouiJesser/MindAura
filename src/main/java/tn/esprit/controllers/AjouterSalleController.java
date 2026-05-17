package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.salle;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.salle_SERVICE;
import tn.esprit.utils.ImageManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class AjouterSalleController implements Initializable {

    @FXML private TextField        txtNom;
    @FXML private TextField        txtType;
    @FXML private TextField        txtCapacite;
    @FXML private TextArea         txtEquipements;
    @FXML private ComboBox<String> comboDisponibilite;
    @FXML private TextField        txtEtage;          // TextField (pas Spinner) — etage est du texte
    @FXML private TextField        txtImage_url;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<String> comboLocal;
    @FXML private Button           btnParcourir;
    @FXML private Button           btnEnregistrer;
    @FXML private Button           btnAnnuler;
    @FXML private Label            lblMessage;
    @FXML private ImageView        imgPreview;
    @FXML private Label            lblTitre;

    private final salle_SERVICE             salleService = new salle_SERVICE();
    private final local_psychiatrie_SERVICE localService = new local_psychiatrie_SERVICE();
    private AfficherSalleController         parentController;
    private File                            selectedImageFile;
    private salle                           salleToEdit;
    private List<local_psychiatrie>         locauxList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ImageManager.initializeImageDirectory();

        comboDisponibilite.setItems(FXCollections.observableArrayList(
                "Disponible", "Non disponible", "Sur reservation"));
        comboDisponibilite.setValue("Disponible");

        comboStatut.setItems(FXCollections.observableArrayList(
                "Active", "En maintenance", "Hors service"));
        comboStatut.setValue("Active");

        chargerLocaux();
        applyCSSStyles();
        applyFadeIn();
    }

    private void chargerLocaux() {
        try {
            locauxList = localService.afficherList();
            for (local_psychiatrie l : locauxList) {
                comboLocal.getItems().add(l.getId_local() + " - " + l.getNom_local());
            }
            if (!comboLocal.getItems().isEmpty()) comboLocal.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            showMessage("Impossible de charger les locaux : " + e.getMessage(), "warning");
        }
    }

    public void setSalleToEdit(salle s) {
        this.salleToEdit = s;
        if (lblTitre != null) lblTitre.setText("Modifier la Salle");

        txtNom.setText(s.getNom_salle());
        txtType.setText(s.getType_salle());
        txtCapacite.setText(s.getCapacite_salle());
        txtEquipements.setText(s.getEquipements());
        comboDisponibilite.setValue(s.getDisponibilite_salle());
        txtEtage.setText(s.getEtage() != null ? s.getEtage() : "");
        txtImage_url.setText(s.getImage_url() != null ? s.getImage_url() : "");
        comboStatut.setValue(s.getStatut_salle());

        comboLocal.getItems().stream()
                .filter(item -> item.startsWith(s.getId_local() + " - "))
                .findFirst()
                .ifPresent(comboLocal::setValue);

        btnEnregistrer.setText("Modifier");
    }

    @FXML
    private void handleParcourir() {
        File file = ImageManager.selectImageFile(btnParcourir.getScene().getWindow());
        if (file != null) {
            if (ImageManager.isValidImageFile(file)) {
                selectedImageFile = file;
                txtImage_url.setText(file.getName());
                if (imgPreview != null)
                    imgPreview.setImage(ImageManager.loadImage(file.getAbsolutePath(), 200, 150, true));
                showMessage("Image selectionnee : " + file.getName(), "success");
            } else {
                showMessage("Fichier invalide (PNG, JPG, JPEG, GIF)", "error");
            }
        }
    }

    @FXML
    private void handleEnregistrer() {
        if (!validateForm()) return;

        try {
            String imagePath = null;
            if (selectedImageFile != null) {
                imagePath = ImageManager.saveImage(selectedImageFile);
            }

            String localSelection = comboLocal.getValue();
            int idLocal = Integer.parseInt(localSelection.split(" - ")[0].trim());

            if (salleToEdit == null) {
                salle s = new salle(
                        txtNom.getText().trim(),
                        txtType.getText().trim(),
                        txtCapacite.getText().trim(),
                        txtEquipements.getText().trim(),
                        comboDisponibilite.getValue(),
                        txtEtage.getText().trim(),
                        imagePath != null ? imagePath : txtImage_url.getText().trim(),
                        comboStatut.getValue(),
                        idLocal
                );
                salleService.add(s);
                showMessage("Salle ajoutee avec succes !", "success");
            } else {
                salleToEdit.setNom_salle(txtNom.getText().trim());
                salleToEdit.setType_salle(txtType.getText().trim());
                salleToEdit.setCapacite_salle(txtCapacite.getText().trim());
                salleToEdit.setEquipements(txtEquipements.getText().trim());
                salleToEdit.setDisponibilite_salle(comboDisponibilite.getValue());
                salleToEdit.setEtage(txtEtage.getText().trim());
                if (imagePath != null) salleToEdit.setImageURL(imagePath);
                salleToEdit.setStatut_salle(comboStatut.getValue());
                salleToEdit.setId_local(idLocal);
                salleService.modifier(salleToEdit);
                showMessage("Salle modifiee avec succes !", "success");
            }

            if (parentController != null) parentController.refreshTable();

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(this::handleAnnuler);
            }).start();

        } catch (SQLException e) {
            showMessage("Erreur BD : " + e.getMessage(), "error");
        } catch (NumberFormatException e) {
            showMessage("Format invalide pour le local", "error");
        } catch (java.io.IOException e) { showMessage("x Erreur image : " + e.getMessage(), "error");}
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (txtNom.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
            txtNom.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtNom.setStyle("");

        if (txtType.getText().trim().isEmpty()) {
            errors.append("- Le type est obligatoire\n");
            txtType.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtType.setStyle("");

        if (txtCapacite.getText().trim().isEmpty()) {
            errors.append("- La capacite est obligatoire\n");
            txtCapacite.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtCapacite.setStyle("");

        if (comboLocal.getValue() == null) {
            errors.append("- Veuillez selectionner un local\n");
        }

        if (errors.length() > 0) { showMessage("Erreurs :\n" + errors, "error"); return false; }
        return true;
    }

    @FXML
    private void handleAnnuler() {
        ((Stage) btnAnnuler.getScene().getWindow()).close();
    }

    private void applyCSSStyles() {
        btnEnregistrer.getStyleClass().add("btn-primary");
        btnAnnuler.getStyleClass().add("btn-annuler");
        btnParcourir.getStyleClass().add("btn-outline");
    }

    private void applyFadeIn() {
        if (txtNom != null && txtNom.getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), txtNom.getParent());
            fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        }
    }

    private void showMessage(String message, String type) {
        lblMessage.setText(message);
        String style;
        if ("success".equals(type))
            style = "-fx-text-fill: #1B4332; -fx-font-weight: bold; -fx-background-color: #D8F3DC; "
                    + "-fx-padding: 12px 20px; -fx-background-radius: 10px; "
                    + "-fx-border-color: #2D6A4F; -fx-border-width: 1px; -fx-border-radius: 10px;";
        else if ("warning".equals(type))
            style = "-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #FFF4E6; "
                    + "-fx-padding: 12px 20px; -fx-background-radius: 10px; "
                    + "-fx-border-color: #B794F4; -fx-border-width: 1px; -fx-border-radius: 10px;";
        else
            style = "-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #F3E8FF; "
                    + "-fx-padding: 12px 20px; -fx-background-radius: 10px; "
                    + "-fx-border-color: #9B7DC4; -fx-border-width: 1px; -fx-border-radius: 10px;";
        lblMessage.setStyle(style);
        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
    }

    public void setParentController(AfficherSalleController controller) {
        this.parentController = controller;
    }
}