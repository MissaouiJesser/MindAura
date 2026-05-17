package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Role;
import tn.esprit.services.utilisateurs_service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * Contrôleur dédié à la MODIFICATION d'un utilisateur.
 * Utilisé dans deux contextes :
 *  - Dashboard admin  → dashboardController est injecté, onDoneCallback est null
 *  - UserHome (profil) → dashboardController est null, onDoneCallback est défini
 *
 * FIX : handleSave() et handleAnnuler() appellent onDoneCallback quand
 * dashboardController est null, ce qui permet le retour vers UserHome.
 */
public class ModifierUtilisateurController implements Initializable, DashboardController.DashboardAware {

    // Champs du formulaire
    @FXML private Label headerNomComplet;
    @FXML private Label headerInitiales;
    @FXML private TextField fieldPrenom;
    @FXML private TextField fieldNom;
    @FXML private TextField fieldEmail;
    @FXML private TextField fieldTelephone;
    @FXML private DatePicker fieldDateNaissance;
    @FXML private ComboBox<String> fieldRole;
    @FXML private PasswordField fieldPassword;
    @FXML private CheckBox fieldActif;
    @FXML private TextArea fieldBio;

    // Photo
    @FXML private StackPane photoPreviewPane;
    @FXML private StackPane photoPlaceholder;
    @FXML private ImageView photoImageView;
    @FXML private Label photoInitiales;
    @FXML private Label photoNameLabel;
    @FXML private Button btnRemovePhoto;

    // Labels d'erreur
    @FXML private Label errPrenom, errNom, errEmail, errDate, errRole, errPassword, errGlobal;

    private utilisateurs userToEdit;
    private File selectedPhotoFile = null;
    private boolean photoChanged = false;
    private DashboardController dashboardController;
    private final utilisateurs_service service = new utilisateurs_service();
    private Runnable onDoneCallback;

    public void setOnDone(Runnable callback) {
        this.onDoneCallback = callback;
    }

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*\\d).{6,}$"
    );

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fieldRole.setItems(FXCollections.observableArrayList(
                Role.ROLE_ADMIN.getLibelle(),
                Role.ROLE_PSYCHOLOGUE.getLibelle(),
                Role.ROLE_PATIENT.getLibelle(),
                Role.ROLE_COACH.getLibelle(),
                Role.ROLE_USER.getLibelle()
        ));

        // Bloquer les dates futures
        fieldDateNaissance.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(LocalDate.now()));
            }
        });

        // Mise à jour du badge quand le rôle change
        fieldRole.valueProperty().addListener((o, ov, nv) -> updateBadge(nv));

        // Limiter le téléphone à 8 chiffres
        fieldTelephone.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && nv.length() > 8) {
                fieldTelephone.setText(nv.substring(0, 8));
            }
            if (nv != null && !nv.matches("\\d*")) {
                fieldTelephone.setText(nv.replaceAll("[^\\d]", ""));
            }
        });

        // Initiales dynamiques
        fieldPrenom.textProperty().addListener((o, ov, nv) -> updateInitiales());
        fieldNom.textProperty().addListener((o, ov, nv) -> updateInitiales());
    }

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    public void setUserToEdit(utilisateurs user) {
        this.userToEdit = user;

        // En-tête
        if (headerNomComplet != null)
            headerNomComplet.setText(user.getPrenom_utilisateur() + " " + user.getNom_utilisateur());

        // Pré-remplissage
        if (fieldPrenom != null) fieldPrenom.setText(user.getPrenom_utilisateur());
        if (fieldNom != null) fieldNom.setText(user.getNom_utilisateur());
        if (fieldEmail != null) fieldEmail.setText(user.getEmail_utilisateur());
        if (fieldTelephone != null)
            fieldTelephone.setText(user.getTelephone_utilisateur() != null ? user.getTelephone_utilisateur() : "");

        if (fieldDateNaissance != null && user.getDate_naissance_utilisateur() != null) {
            Date dateNaissance = user.getDate_naissance_utilisateur();
            LocalDate ld;
            if (dateNaissance instanceof java.sql.Date) {
                ld = ((java.sql.Date) dateNaissance).toLocalDate();
            } else {
                ld = dateNaissance.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            fieldDateNaissance.setValue(ld);
        }

        if (fieldRole != null && user.getRole_utilisateur() != null) {
            fieldRole.setValue(user.getRole_utilisateur().getLibelle());
            updateBadge(user.getRole_utilisateur().getLibelle());
        }

        if (fieldActif != null) fieldActif.setSelected(user.isEst_actif_utilisateur());
        if (fieldBio != null) fieldBio.setText(user.getBio_utilisateur() != null ? user.getBio_utilisateur() : "");

        // Initiales en-tête
        updateInitiales();

        // Afficher la photo existante
        loadExistingPhoto(user.getPhoto_profil_utilisateur());
    }

    private void loadExistingPhoto(String photoPath) {
        if (photoPath != null && !photoPath.isBlank()) {
            try {
                java.nio.file.Path p = Paths.get("C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/avatars/" + photoPath);
                if (Files.exists(p)) {
                    Image img = new Image(p.toUri().toString(), 90, 90, false, true);
                    photoImageView.setImage(img);
                    photoImageView.setVisible(true);
                    photoPlaceholder.setVisible(false);
                    photoNameLabel.setText(p.getFileName().toString());
                    btnRemovePhoto.setVisible(true);
                    btnRemovePhoto.setManaged(true);
                    return;
                }
            } catch (Exception ignored) {}
        }
        updateInitiales();
    }

    private void updateInitiales() {
        if (fieldPrenom == null || fieldNom == null) return;
        String p = fieldPrenom.getText().trim();
        String n = fieldNom.getText().trim();
        String pi = p.isEmpty() ? "?" : String.valueOf(p.charAt(0)).toUpperCase();
        String ni = n.isEmpty() ? "" : String.valueOf(n.charAt(0)).toUpperCase();
        if (photoInitiales != null) photoInitiales.setText(pi + ni);
        if (headerInitiales != null) headerInitiales.setText(pi + ni);
    }

    private void updateBadge(String roleLib) {
        // badgeRole n'existe pas dans le FXML — réservé pour usage futur
    }

    @FXML
    private void handleChoosePhoto() {
        handleChangePhoto();
    }

    @FXML
    private void handleChangePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg","*.jpeg","*.png","*.gif","*.bmp")
        );
        Stage stage = (Stage) photoPreviewPane.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        if (file.length() > 5 * 1024 * 1024) {
            errGlobal.setText("La photo ne doit pas dépasser 5 MB.");
            return;
        }

        selectedPhotoFile = file;
        photoChanged = true;
        photoNameLabel.setText(file.getName());

        try {
            Image img = new Image(file.toURI().toString(), 90, 90, false, true);
            photoImageView.setImage(img);
            photoImageView.setVisible(true);
            photoPlaceholder.setVisible(false);
        } catch (Exception e) {
            photoImageView.setVisible(false);
            photoPlaceholder.setVisible(true);
        }

        btnRemovePhoto.setVisible(true);
        btnRemovePhoto.setManaged(true);
    }

    @FXML
    private void handleRemovePhoto() {
        selectedPhotoFile = null;
        photoChanged = true;
        photoNameLabel.setText("Aucune photo");
        photoImageView.setVisible(false);
        photoPlaceholder.setVisible(true);
        btnRemovePhoto.setVisible(false);
        btnRemovePhoto.setManaged(false);
        updateInitiales();
    }

    private String copyPhoto(File photo, String email) {
        try {
            String dir = "C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/avatars/";
            Path destDir = Paths.get(dir);
            if (!Files.exists(destDir)) Files.createDirectories(destDir);

            String ext = getExt(photo.getName());
            String name = email.replaceAll("[^a-zA-Z0-9]","_") + "_" + System.currentTimeMillis() + "." + ext;
            Files.copy(photo.toPath(), destDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return name;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getExt(String fname) {
        int d = fname.lastIndexOf('.');
        return d >= 0 ? fname.substring(d+1).toLowerCase() : "jpg";
    }

    private boolean validateForm() {
        clearErrors();
        boolean valid = true;

        String prenom = fieldPrenom.getText().trim();
        String nom = fieldNom.getText().trim();
        String email = fieldEmail.getText().trim();
        String tel = fieldTelephone.getText().trim();
        String bio = fieldBio.getText().trim();
        String password = fieldPassword.getText();

        // Prénom : minimum 2 caractères
        if (prenom.isEmpty() || prenom.length() < 2) {
            errPrenom.setText("Prénom : minimum 2 caractères.");
            valid = false;
        }

        // Nom : minimum 3 caractères
        if (nom.isEmpty() || nom.length() < 3) {
            errNom.setText("Nom : minimum 3 caractères.");
            valid = false;
        }

        // Email : validation avec regex
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            errEmail.setText("Email invalide (ex: user@example.com).");
            valid = false;
        }

        // Téléphone : optionnel mais doit être 8 chiffres si renseigné
        if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
            errGlobal.setText("Téléphone : doit contenir exactement 8 chiffres.");
            valid = false;
        }

        // Date de naissance
        if (fieldDateNaissance.getValue() == null) {
            errDate.setText("Date de naissance obligatoire.");
            valid = false;
        } else if (fieldDateNaissance.getValue().isAfter(LocalDate.now())) {
            errDate.setText("La date de naissance ne peut pas être dans le futur.");
            valid = false;
        }

        // Rôle
        if (fieldRole.getValue() == null) {
            errRole.setText("Rôle obligatoire.");
            valid = false;
        }

        // Mot de passe : si modifié, doit respecter le pattern
        if (!password.isEmpty() && !PASSWORD_PATTERN.matcher(password).matches()) {
            errPassword.setText("Mot de passe : min 6 caractères, 1 majuscule, 1 chiffre.");
            valid = false;
        }

        // Bio : minimum 10 caractères
        if (bio.isEmpty() || bio.length() < 10) {
            errGlobal.setText("Bio : minimum 10 caractères.");
            valid = false;
        }

        return valid;
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        try {
            // Vérifier email unique (sauf pour cet utilisateur)
            List<utilisateurs> list = service.afficherList();
            for (utilisateurs u : list) {
                if (!u.getId_utilisateur().equals(userToEdit.getId_utilisateur())
                        && u.getEmail_utilisateur().equalsIgnoreCase(fieldEmail.getText().trim())) {
                    errEmail.setText("Cet email est déjà utilisé.");
                    return;
                }
            }

            // Mise à jour
            userToEdit.setPrenom_utilisateur(fieldPrenom.getText().trim());
            userToEdit.setNom_utilisateur(fieldNom.getText().trim());
            userToEdit.setEmail_utilisateur(fieldEmail.getText().trim());
            userToEdit.setTelephone_utilisateur(fieldTelephone.getText().trim().isEmpty() ? null : fieldTelephone.getText().trim());
            userToEdit.setDate_naissance_utilisateur(
                    Date.from(fieldDateNaissance.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            userToEdit.setEst_actif_utilisateur(fieldActif.isSelected());
            userToEdit.setBio_utilisateur(fieldBio.getText().trim());

            // Mot de passe si modifié
            if (!fieldPassword.getText().isEmpty()) {
                userToEdit.setMdp_utilisateur(fieldPassword.getText());
            }

            // Rôle
            for (Role r : Role.values())
                if (r.getLibelle().equals(fieldRole.getValue())) { userToEdit.setRole_utilisateur(r); break; }

            // Photo
            if (photoChanged) {
                if (selectedPhotoFile != null) {
                    String photoPath = copyPhoto(selectedPhotoFile, fieldEmail.getText().trim());
                    userToEdit.setPhoto_profil_utilisateur(photoPath);
                } else {
                    userToEdit.setPhoto_profil_utilisateur(null);
                }
            }

            service.modifier(userToEdit);

            showSuccess("Utilisateur modifié avec succès !");

            // ✅ FIX : utiliser onDoneCallback si pas de dashboardController (contexte UserHome)
            if (dashboardController != null) {
                dashboardController.navigateTo("ListeUtilisateurs.fxml",
                        "Gestion des Utilisateurs", "Liste et gestion des comptes");
            } else if (onDoneCallback != null) {
                onDoneCallback.run();
            }

        } catch (SQLException e) {
            errGlobal.setText("Erreur BDD : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnuler() {
        // ✅ FIX : utiliser onDoneCallback si pas de dashboardController (contexte UserHome)
        if (dashboardController != null) {
            dashboardController.navigateTo("ListeUtilisateurs.fxml",
                    "Gestion des Utilisateurs", "Liste et gestion des comptes");
        } else if (onDoneCallback != null) {
            onDoneCallback.run();
        }
    }

    private void clearErrors() {
        errPrenom.setText(""); errNom.setText(""); errEmail.setText("");
        errDate.setText(""); errRole.setText(""); errPassword.setText(""); errGlobal.setText("");
    }

    private void showSuccess(String msg) {
        errGlobal.setStyle("-fx-text-fill:#2E8B57; -fx-font-weight:bold;");
        errGlobal.setText(msg);
    }
}