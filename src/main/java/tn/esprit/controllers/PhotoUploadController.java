package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import javafx.stage.*;
import tn.esprit.entities.utilisateurs;
import tn.esprit.services.utilisateurs_service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Contrôleur de la fenêtre d'upload de photo de profil.
 * Appelé depuis LoginController quand l'utilisateur n'a pas de photo.
 *
 * Après avoir sauvegardé la photo en BDD → onPhotoSaved.run()
 * Si l'utilisateur annule → onCancel.run()
 *
 * MODIFICATION : btnRemove est maintenant un StackPane (bouton stylisé custom)
 * pour correspondre au design unifié de l'application.
 */
public class PhotoUploadController implements Initializable {

    @FXML private Label     userNameLabel;
    @FXML private StackPane photoPlaceholder;
    @FXML private ImageView photoPreview;
    @FXML private Label     photoNameLabel;
    @FXML private StackPane btnRemove;   // StackPane au lieu de Button
    @FXML private Label     errorLabel;

    private utilisateurs        user;
    private File                selectedFile = null;
    private Runnable            onPhotoSaved;
    private Runnable            onCancel;
    private final utilisateurs_service service = new utilisateurs_service();

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setUser(utilisateurs u) {
        this.user = u;
        if (userNameLabel != null)
            userNameLabel.setText(
                    "Bonjour " + u.getPrenom_utilisateur() + " " + u.getNom_utilisateur() + " !"
            );
    }

    public void setOnPhotoSaved(Runnable r) { this.onPhotoSaved = r; }
    public void setOnCancel(Runnable r)     { this.onCancel = r; }

    @FXML
    private void handleChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir votre photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"));
        Stage stage = (Stage) photoNameLabel.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        if (file.length() > 5 * 1024 * 1024) {
            showError("La photo ne doit pas dépasser 5 MB."); return;
        }

        selectedFile = file;
        photoNameLabel.setText(file.getName());

        try {
            Image img = new Image(file.toURI().toString(), 120, 120, false, true);
            photoPreview.setImage(img);
            photoPreview.setVisible(true);
            photoPlaceholder.setVisible(false);
        } catch (Exception e) {
            photoPreview.setVisible(false);
            photoPlaceholder.setVisible(true);
        }

        if (btnRemove != null) {
            btnRemove.setVisible(true);
            btnRemove.setManaged(true);
        }
        if (errorLabel != null) errorLabel.setText("");
    }

    @FXML
    private void handleRemovePhoto() {
        selectedFile = null;
        photoNameLabel.setText("Aucune photo sélectionnée");
        photoPreview.setImage(null);
        photoPreview.setVisible(false);
        photoPlaceholder.setVisible(true);
        if (btnRemove != null) {
            btnRemove.setVisible(false);
            btnRemove.setManaged(false);
        }
    }

    @FXML
    private void handleSave() {
        if (selectedFile == null) {
            showError("Veuillez choisir une photo avant de continuer."); return;
        }

        try {
            // Copier la photo dans images_users/
            Path destDir = Paths.get("C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/avatars/");
            if (!Files.exists(destDir)) Files.createDirectories(destDir);

            String ext  = getExt(selectedFile.getName());
            String name = user.getEmail_utilisateur().replaceAll("[^a-zA-Z0-9]", "_")
                    + "_" + System.currentTimeMillis() + "." + ext;
            Files.copy(selectedFile.toPath(), destDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            String photoPath = name;

            // Mettre à jour l'utilisateur en BDD
            user.setPhoto_profil_utilisateur(photoPath);
            service.modifier(user);

            // Fermer la fenêtre et notifier
            Stage stage = (Stage) photoNameLabel.getScene().getWindow();
            stage.close();
            if (onPhotoSaved != null) onPhotoSaved.run();

        } catch (IOException | SQLException e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) photoNameLabel.getScene().getWindow();
        stage.close();
        if (onCancel != null) onCancel.run();
    }

    private void showError(String msg) {
        if (errorLabel == null) return;
        errorLabel.setStyle("-fx-text-fill:#C0392B; -fx-font-size:11px; -fx-font-weight:bold;");
        errorLabel.setText(msg);
    }

    private String getExt(String fname) {
        int d = fname.lastIndexOf('.');
        return d >= 0 ? fname.substring(d + 1).toLowerCase() : "jpg";
    }
}