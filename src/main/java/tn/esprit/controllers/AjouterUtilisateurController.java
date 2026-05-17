package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Role;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.utils.EmailService;

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
 * Contrôleur dédié à l'AJOUT d'un utilisateur (côté administrateur).
 *
 * ✉️  NOUVEAU — JavaMail API :
 *   Après la création réussie d'un compte, un email de confirmation HTML
 *   est automatiquement envoyé à l'adresse du nouvel utilisateur.
 *   L'envoi est asynchrone → l'UI ne se bloque pas.
 */
public class AjouterUtilisateurController implements Initializable, DashboardController.DashboardAware {

    // ─── Champs formulaire ─────────────────────────────────────────────────────────
    @FXML private TextField     fieldPrenom;
    @FXML private TextField     fieldNom;
    @FXML private TextField     fieldEmail;
    @FXML private TextField     fieldTelephone;
    @FXML private DatePicker    fieldDateNaissance;
    @FXML private ComboBox<String> fieldRole;
    @FXML private PasswordField fieldPassword;
    @FXML private PasswordField fieldPasswordConfirm;
    @FXML private CheckBox      fieldActif;
    @FXML private TextArea      fieldBio;

    // ─── Photo ──────────────────────────────────────────────────────────────────────
    @FXML private StackPane  photoPreviewPane;
    @FXML private StackPane  photoPlaceholder;
    @FXML private ImageView  photoImageView;
    @FXML private Label      photoInitiales;
    @FXML private Label      photoNameLabel;
    @FXML private Button     btnRemovePhoto;

    // ─── Labels d'erreur ────────────────────────────────────────────────────────────
    @FXML private Label errPrenom;
    @FXML private Label errNom;
    @FXML private Label errEmail;
    @FXML private Label errDate;
    @FXML private Label errRole;
    @FXML private Label errPassword;
    @FXML private Label errGlobal;

    private File   selectedPhotoFile = null;
    private DashboardController dashboardController;
    private final utilisateurs_service service = new utilisateurs_service();

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

        // Aperçu dynamique des initiales
        fieldPrenom.textProperty().addListener((o, ov, nv) -> updateInitiales());
        fieldNom.textProperty().addListener((o, ov, nv) -> updateInitiales());

        // Limiter le téléphone à 8 chiffres numériques
        fieldTelephone.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && nv.length() > 8)
                fieldTelephone.setText(nv.substring(0, 8));
            if (nv != null && !nv.matches("\\d*"))
                fieldTelephone.setText(nv.replaceAll("[^\\d]", ""));
        });
    }

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // =========================================================================
    //  PHOTO
    // =========================================================================

    @FXML
    private void handleChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg","*.jpeg","*.png","*.gif","*.bmp")
        );
        Stage stage = (Stage) photoPreviewPane.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        if (file.length() > 5 * 1024 * 1024) {
            errGlobal.setText("La photo ne doit pas dépasser 5 MB."); return;
        }
        selectedPhotoFile = file;
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
        photoNameLabel.setText("Aucune photo sélectionnée");
        photoImageView.setVisible(false);
        photoPlaceholder.setVisible(true);
        btnRemovePhoto.setVisible(false);
        btnRemovePhoto.setManaged(false);
        updateInitiales();
    }

    private void updateInitiales() {
        String p = fieldPrenom.getText().trim();
        String n = fieldNom.getText().trim();
        String pi = p.isEmpty() ? "?" : String.valueOf(p.charAt(0)).toUpperCase();
        String ni = n.isEmpty() ? "" : String.valueOf(n.charAt(0)).toUpperCase();
        if (photoInitiales != null) photoInitiales.setText(pi + ni);
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
            e.printStackTrace(); return null;
        }
    }

    private String getExt(String fname) {
        int d = fname.lastIndexOf('.');
        return d >= 0 ? fname.substring(d+1).toLowerCase() : "jpg";
    }

    // =========================================================================
    //  VALIDATIONS
    // =========================================================================

    private boolean validateForm() {
        clearErrors();
        boolean valid = true;

        String prenom = fieldPrenom.getText().trim();
        String nom    = fieldNom.getText().trim();
        String email  = fieldEmail.getText().trim();
        String tel    = fieldTelephone.getText().trim();
        String bio    = fieldBio.getText().trim();
        String password        = fieldPassword.getText();
        String passwordConfirm = fieldPasswordConfirm.getText();

        if (prenom.isEmpty() || prenom.length() < 2) {
            errPrenom.setText("Prénom : minimum 2 caractères."); valid = false;
        }
        if (nom.isEmpty() || nom.length() < 3) {
            errNom.setText("Nom : minimum 3 caractères."); valid = false;
        }
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            errEmail.setText("Email invalide (ex: user@example.com)."); valid = false;
        }
        if (!tel.isEmpty() && !PHONE_PATTERN.matcher(tel).matches()) {
            errGlobal.setText("Téléphone : doit contenir exactement 8 chiffres."); valid = false;
        }
        if (fieldDateNaissance.getValue() == null) {
            errDate.setText("Date de naissance obligatoire."); valid = false;
        } else if (fieldDateNaissance.getValue().isAfter(LocalDate.now())) {
            errDate.setText("La date de naissance ne peut pas être dans le futur."); valid = false;
        }
        if (fieldRole.getValue() == null) {
            errRole.setText("Rôle obligatoire."); valid = false;
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            errPassword.setText("Mot de passe : min 6 caractères, 1 majuscule, 1 chiffre."); valid = false;
        } else if (!password.equals(passwordConfirm)) {
            errPassword.setText("Les mots de passe ne correspondent pas."); valid = false;
        }
        if (bio.isEmpty() || bio.length() < 10) {
            errGlobal.setText("Bio : minimum 10 caractères."); valid = false;
        }

        return valid;
    }

    // =========================================================================
    //  SAVE  ✉️  + Email de confirmation
    // =========================================================================

    @FXML
    private void handleSave() {
        if (!validateForm()) return;

        try {
            // Vérification unicité email
            List<utilisateurs> list = service.afficherList();
            for (utilisateurs u : list) {
                if (u.getEmail_utilisateur().equalsIgnoreCase(fieldEmail.getText().trim())) {
                    errEmail.setText("Cet email est déjà utilisé.");
                    return;
                }
            }

            // Photo
            String photoPath = null;
            if (selectedPhotoFile != null) {
                photoPath = copyPhoto(selectedPhotoFile, fieldEmail.getText().trim());
            }

            // Construction de l'objet utilisateur
            utilisateurs u = new utilisateurs();
            u.setPrenom_utilisateur(fieldPrenom.getText().trim());
            u.setNom_utilisateur(fieldNom.getText().trim());
            u.setEmail_utilisateur(fieldEmail.getText().trim());
            u.setMdp_utilisateur(fieldPassword.getText());
            u.setTelephone_utilisateur(fieldTelephone.getText().trim().isEmpty()
                    ? null : fieldTelephone.getText().trim());
            u.setDate_naissance_utilisateur(
                    Date.from(fieldDateNaissance.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            u.setEst_actif_utilisateur(fieldActif.isSelected());
            u.setBio_utilisateur(fieldBio.getText().trim());
            u.setDate_inscription_utilisateur(new Date());
            u.setPhoto_profil_utilisateur(photoPath);

            // Résolution du rôle
            String roleLibelle = fieldRole.getValue();
            for (Role r : Role.values())
                if (r.getLibelle().equals(roleLibelle)) { u.setRole_utilisateur(r); break; }

            // Sauvegarde BDD
            service.addMeth2(u);

            // ✉️  Envoi asynchrone de l'email de confirmation
            // → Ne bloque pas l'UI, l'email part en arrière-plan
            EmailService.envoyerConfirmationInscription(
                    u.getEmail_utilisateur(),
                    u.getPrenom_utilisateur(),
                    u.getNom_utilisateur(),
                    roleLibelle,
                    // onSuccess
                    () -> System.out.println("[Email] Confirmation envoyée → " + u.getEmail_utilisateur()),
                    // onError (silencieux — ne pas bloquer l'admin)
                    err -> System.err.println("[Email] Non bloquant : " + err)
            );

            showSuccess("Utilisateur \"" + u.getPrenom_utilisateur() + " " + u.getNom_utilisateur()
                    + "\" ajouté ! ✉️ Email de confirmation envoyé.");

            // Retour à la liste
            if (dashboardController != null) {
                dashboardController.navigateTo("ListeUtilisateurs.fxml",
                        "Gestion des Utilisateurs", "Liste et gestion des comptes");
            }

        } catch (SQLException e) {
            errGlobal.setText("Erreur BDD : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAnnuler() {
        if (dashboardController != null) {
            dashboardController.navigateTo("ListeUtilisateurs.fxml",
                    "Gestion des Utilisateurs", "Liste et gestion des comptes");
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