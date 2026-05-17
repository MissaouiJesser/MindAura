package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.entities.Ressources;
import tn.esprit.services.FileUploadRessourceService;
import tn.esprit.services.RessourcesService;
import tn.esprit.utils.MyDataBase;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class ModifierRessourceController implements DashboardController.DashboardAware {

    // ─── Champs existants ────────────────────────────────────
    @FXML private TextField titreField;
    @FXML private TextArea  resumeField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private TextField tagsField;
    @FXML private DatePicker datePublicationPicker;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private ComboBox<String> typeContenuCombo;
    @FXML private TextField dureeField;
    @FXML private Label fichierLabel;

    // Image
    @FXML private Button    choisirImageBtn;
    @FXML private Label     imageLabel;
    @FXML private ImageView imagePreview;
    @FXML private VBox      emptyImageState;

    // Boutons
    @FXML private Button choisirFichierBtn;
    @FXML private Button modifierBtn;
    @FXML private Button supprimerBtn;
    @FXML private Button annulerBtn;

    // ─── Éléments dynamiques Section 4 ───────────────────────
    @FXML private VBox fichierBox;
    @FXML private VBox urlPodcastBox;
    @FXML private VBox urlArticleBox;
    @FXML private VBox noTypeBox;
    @FXML private TextField urlPodcastField;
    @FXML private TextField urlArticleField;
    @FXML private Label lblSectionFichierTitre;
    @FXML private Label lblSectionFichierSous;

    // ─── État ────────────────────────────────────────────────
    private Ressources ressourceActuelle;
    private Stage      currentStage;
    private File       fichierSelectionne;
    private File       imageSelectionnee;
    private RessourcesService service;
    private DashboardController dashboardController;

    // ✅ Service d'upload vers Symfony
    private final FileUploadRessourceService uploadService = new FileUploadRessourceService();

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // ─── INITIALIZE ──────────────────────────────────────────
    @FXML
    public void initialize() {
        service = new RessourcesService(MyDataBase.getInstance().getConx());

        categorieCombo.setItems(FXCollections.observableArrayList(
                "Gestion_stress",
                "Confiance_en_soi",
                "Motivation",
                "Communication",
                "Bien_etre",
                "Protectivite",
                "Intelligence_emotionnelle"
        ));

        niveauCombo.setItems(FXCollections.observableArrayList(
                "DEBUTANT",
                "INTERMEDIAIRE",
                "AVANCE"
        ));

        typeContenuCombo.setItems(FXCollections.observableArrayList(
                "Video",
                "PDF",
                "Article",
                "Podcast"
        ));

        setModeAucunType();
    }

    // ─── SWITCH DYNAMIQUE selon le type ──────────────────────
    @FXML
    private void onTypeContenuChange(ActionEvent event) {
        String type = typeContenuCombo.getValue();
        if (type == null) { setModeAucunType(); return; }

        switch (type) {
            case "Video", "PDF" -> setModeFichier(type);
            case "Article"      -> setModeArticle();
            case "Podcast"      -> setModePodcast();
            default             -> setModeAucunType();
        }
    }

    private void setModeFichier(String type) {
        setVisible(fichierBox,     true);
        setVisible(urlPodcastBox,  false);
        setVisible(urlArticleBox,  false);
        setVisible(noTypeBox,      false);

        lblSectionFichierTitre.setText("Fichier de la ressource");
        lblSectionFichierSous.setText("Sélectionnez le fichier " + type + " à associer");
        choisirFichierBtn.setOnAction(e -> choisirFichierPour(type));
    }

    private void setModeArticle() {
        setVisible(fichierBox,     false);
        setVisible(urlPodcastBox,  false);
        setVisible(urlArticleBox,  true);
        setVisible(noTypeBox,      false);

        lblSectionFichierTitre.setText("URL de l'article");
        lblSectionFichierSous.setText("Collez le lien vers l'article en ligne");
    }

    private void setModePodcast() {
        setVisible(fichierBox,     false);
        setVisible(urlPodcastBox,  true);
        setVisible(urlArticleBox,  false);
        setVisible(noTypeBox,      false);

        lblSectionFichierTitre.setText("URL du Podcast");
        lblSectionFichierSous.setText("Collez le lien de votre épisode ou flux podcast");
    }

    private void setModeAucunType() {
        setVisible(fichierBox,     false);
        setVisible(urlPodcastBox,  false);
        setVisible(urlArticleBox,  false);
        setVisible(noTypeBox,      true);

        lblSectionFichierTitre.setText("Fichier de la ressource");
        lblSectionFichierSous.setText("Mettez à jour le fichier ou le lien");
    }

    // ─── INIT DATA ───────────────────────────────────────────
    public void initData(Ressources r, Stage stage) {
        this.ressourceActuelle = r;
        this.currentStage = stage;
        if (r == null) return;

        titreField.setText(r.getTitre());
        resumeField.setText(r.getResume());

        if (r.getCategorie() != null && !r.getCategorie().isEmpty())
            categorieCombo.setValue(r.getCategorie());

        datePublicationPicker.setValue(LocalDate.now());

        // Afficher l'URL/fichier actuel dans le label
        if (r.getUrl() != null && !r.getUrl().isEmpty()) {
            fichierLabel.setText("Actuel : " + r.getUrl());
            fichierLabel.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 11px;");
        } else {
            fichierLabel.setText("Aucun fichier sélectionné");
            fichierLabel.setStyle("-fx-text-fill: #9AA89F;");
        }

        if (r.getContenu() != null && !r.getContenu().isEmpty()) {
            String contenu = r.getContenu();
            typeContenuCombo.setValue(contenu);
            switch (contenu) {
                case "Video", "PDF" -> setModeFichier(contenu);
                case "Article" -> {
                    setModeArticle();
                    if (urlArticleField != null && r.getUrl() != null && !r.getUrl().isEmpty())
                        urlArticleField.setText(r.getUrl());
                }
                case "Podcast" -> {
                    setModePodcast();
                    if (urlPodcastField != null && r.getUrl() != null && !r.getUrl().isEmpty())
                        urlPodcastField.setText(r.getUrl());
                }
                default -> setModeAucunType();
            }
        }

        // Pré-charger l'image si c'est une URL Symfony (commence par /uploads)
        if (r.getImageUrl() != null && !r.getImageUrl().isEmpty()) {
            String imgUrl = r.getImageUrl();
            try {
                String fullUrl = imgUrl.startsWith("http")
                        ? imgUrl
                        : "http://localhost:8000" + imgUrl;
                imagePreview.setImage(new Image(fullUrl, true));
                imagePreview.setVisible(true);
                setVisible(emptyImageState, false);
                imageLabel.setText("✓ Image actuelle chargée");
                imageLabel.setStyle("-fx-text-fill: #2D6A4F; -fx-font-weight: bold;");
            } catch (Exception ignored) {}
        }
    }

    // ─── CHOISIR FICHIER (Video / PDF) ───────────────────────
    private void choisirFichierPour(String type) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner un fichier " + type);

        switch (type) {
            case "Video" -> fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Vidéos", "*.mp4", "*.avi", "*.mkv", "*.mov"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
            case "PDF"   -> fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
            default      -> fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        }

        fichierSelectionne = fc.showOpenDialog(choisirFichierBtn.getScene().getWindow());
        if (fichierSelectionne != null) {
            fichierLabel.setText("✓ " + fichierSelectionne.getName() + " (sera uploadé)");
            fichierLabel.setStyle("-fx-text-fill: #2D6A4F; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void choisirFichier(ActionEvent event) {
        String type = typeContenuCombo.getValue();
        if (type == null) type = "TOUS";
        choisirFichierPour(type);
    }

    // ─── IMAGE ───────────────────────────────────────────────
    @FXML
    private void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image de couverture");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));

        imageSelectionnee = fc.showOpenDialog(choisirImageBtn.getScene().getWindow());
        if (imageSelectionnee == null) return;

        if (imageSelectionnee.length() > 5L * 1024 * 1024) {
            showAlert(Alert.AlertType.WARNING, "Image trop volumineuse",
                    "Veuillez choisir une image de moins de 5 MB.");
            imageSelectionnee = null;
            return;
        }

        imageLabel.setText("✓ " + imageSelectionnee.getName() + " (sera uploadée)");
        imageLabel.setStyle("-fx-text-fill: #2D6A4F; -fx-font-weight: bold;");
        try {
            imagePreview.setImage(new Image(imageSelectionnee.toURI().toString()));
            imagePreview.setVisible(true);
            setVisible(emptyImageState, false);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'image.");
        }
    }

    // ─── MODIFIER ────────────────────────────────────────────
    @FXML
    private void modifierRessource(ActionEvent event) {

        // ── Validations ──
        if (titreField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le titre est obligatoire !");
            return;
        }
        if (categorieCombo.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez sélectionner une catégorie !");
            return;
        }
        if (resumeField.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le résumé est obligatoire !");
            return;
        }

        String type = typeContenuCombo.getValue();

        try {
            // ── Gestion URL / Fichier ──
            if ("Podcast".equals(type)) {
                String url = urlPodcastField != null ? urlPodcastField.getText().trim() : "";
                if (url.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "URL manquante", "L'URL du podcast est obligatoire !");
                    return;
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    showAlert(Alert.AlertType.WARNING, "URL invalide",
                            "L'URL doit commencer par http:// ou https://");
                    return;
                }
                ressourceActuelle.setUrl(url);

            } else if ("Article".equals(type)) {
                String url = urlArticleField != null ? urlArticleField.getText().trim() : "";
                if (url.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "URL manquante", "L'URL de l'article est obligatoire !");
                    if (urlArticleField != null) urlArticleField.requestFocus();
                    return;
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    showAlert(Alert.AlertType.WARNING, "URL invalide",
                            "L'URL doit commencer par http:// ou https://");
                    return;
                }
                ressourceActuelle.setUrl(url);

            } else {
                // ✅ CORRIGÉ : Video / PDF → upload vers Symfony si nouveau fichier sélectionné
                if (fichierSelectionne != null) {
                    showAlert(Alert.AlertType.INFORMATION, "Upload en cours",
                            "Upload du fichier en cours, veuillez patienter...");
                    String urlSymfony = uploadService.uploadRessource(fichierSelectionne);
                    ressourceActuelle.setUrl(urlSymfony);
                    // Si aucun nouveau fichier → on garde l'URL existante en base (pas de setUrl)
                }
            }

            // ✅ CORRIGÉ : Image → upload vers Symfony si nouvelle image sélectionnée
            if (imageSelectionnee != null) {
                String urlImage = uploadService.uploadImage(imageSelectionnee);
                ressourceActuelle.setImageUrl(urlImage);
                // Si pas de nouvelle image → on garde l'imageUrl existante en base
            }

            // ── Champs texte ──
            ressourceActuelle.setTitre(titreField.getText().trim());
            ressourceActuelle.setResume(resumeField.getText().trim());
            ressourceActuelle.setCategorie(categorieCombo.getValue());

            if (niveauCombo.getValue() != null)
                ressourceActuelle.setNiveau(niveauCombo.getValue());
            if (tagsField != null && !tagsField.getText().trim().isEmpty())
                ressourceActuelle.setTags(tagsField.getText().trim());

            // ── Sauvegarde en base ──
            service.modifier(ressourceActuelle);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "La ressource '" + ressourceActuelle.getTitre() + "' a été modifiée !");
            retournerAffichage();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de la modification : " + e.getMessage());
        }
    }

    // ─── SUPPRIMER ───────────────────────────────────────────
    @FXML
    private void supprimerRessource(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer la ressource");
        confirmation.setContentText("Êtes-vous sûr ? Cette action est irréversible.");
        confirmation.getDialogPane().setStyle("-fx-background-color: white;");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    service.delete(ressourceActuelle);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Ressource supprimée !");
                    retournerAffichage();
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur : " + e.getMessage());
                }
            }
        });
    }

    // ─── ANNULER ─────────────────────────────────────────────
    @FXML
    private void annuler(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Annuler les modifications");
        confirmation.setContentText("Les modifications non enregistrées seront perdues.");
        confirmation.getDialogPane().setStyle("-fx-background-color: white;");
        confirmation.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) retournerAffichage();
        });
    }

    @FXML
    private void retournerAffichage() {
        if (dashboardController != null) {
            dashboardController.navigateTo("AfficherRe.fxml", "Gestion des ressources", "Gestion de Rania");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRe.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) modifierBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de retourner à l'affichage : " + e.getMessage());
        }
    }

    // ─── UTILITAIRES ─────────────────────────────────────────
    private void setVisible(javafx.scene.Node n, boolean v) {
        if (n != null) { n.setVisible(v); n.setManaged(v); }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: white;");
        alert.showAndWait();
    }
}