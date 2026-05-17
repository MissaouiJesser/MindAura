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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import tn.esprit.services.CommentairesServices;
import tn.esprit.services.RessourcesService;
import tn.esprit.services.ModerationService;
import tn.esprit.services.EmailRessource;
import tn.esprit.services.FileUploadRessourceService;   // ✅ NOUVEAU : import du service upload
import tn.esprit.entities.Ressources;
import tn.esprit.utils.MyDataBase;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Date;

public class ajouter implements DashboardController.DashboardAware {

    // ========== CHAMPS FXML ==========
    @FXML private TextField        titreField;
    @FXML private TextArea         resumeField;
    @FXML private ComboBox<String> categorieCombo;
    @FXML private TextField        tagsField;
    @FXML private DatePicker       datePublicationPicker;
    @FXML private ComboBox<String> niveauCombo;
    @FXML private ComboBox<String> typeContenuCombo;
    @FXML private TextField        dureeField;

    @FXML private Button           choisirFichierBtn;
    @FXML private Label            fichierLabel;
    @FXML private HBox             fileBox;
    @FXML private Label            iconeFichier;
    @FXML private HBox             badgesFormats;

    @FXML private Button           choisirImageBtn;
    @FXML private VBox             imageDropZone;
    @FXML private HBox             imageBandeauBox;
    @FXML private Label            imageLabel;
    @FXML private Label            imageTailleLabel;
    @FXML private ImageView        imagePreview;
    @FXML private VBox             emptyImageState;

    @FXML private Button           ajouterBtn;
    @FXML private Button           annulerBtn;
    @FXML private TextField        Emailtx;

    @FXML private VBox             panneauAucun;
    @FXML private VBox             panneauPodcast;
    @FXML private VBox             panneauFichier;
    @FXML private TextField        urlPodcastField;
    @FXML private HBox             urlFeedbackBox;
    @FXML private Label            urlFeedbackIcon;
    @FXML private Label            urlFeedbackTitre;
    @FXML private Label            urlFeedbackDesc;
    @FXML private Label            sousTitreSection4;
    @FXML private Label            badgeTypeContenu;
    @FXML private TextField        urlArticleField;
    @FXML private HBox             urlArticleFeedbackBox;
    @FXML private Label            urlArticleFeedbackIcon;
    @FXML private Label            urlArticleFeedbackTitre;
    @FXML private Label            urlArticleFeedbackDesc;
    @FXML private VBox             panneauArticle;

    // ========== VARIABLES INTERNES ==========
    private File              fichierSelectionne;
    private File              imageSelectionnee;
    private RessourcesService ressourceService;
    private ModerationService moderationService;
    private final EmailRessource    emailService  = new EmailRessource();
    private final FileUploadRessourceService uploadService = new FileUploadRessourceService(); // ✅ NOUVEAU
    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // ========== INITIALISATION ==========
    @FXML
    public void initialize() {
        Connection connection = MyDataBase.getInstance().getConx();
        ressourceService  = new RessourcesService(connection);
        moderationService = new ModerationService();

        categorieCombo.setItems(FXCollections.observableArrayList(
                "Gestion_stress", "Confiance_en_soi", "Motivation",
                "Communication", "Bien_etre", "Protectivite", "Intelligence_emotionnelle"
        ));
        niveauCombo.setItems(FXCollections.observableArrayList(
                "DEBUTANT", "INTERMEDIAIRE", "AVANCE"
        ));
        typeContenuCombo.setItems(FXCollections.observableArrayList(
                "Article", "Video", "PDF", "Podcast"
        ));

        datePublicationPicker.setValue(LocalDate.now());

        if (Emailtx != null) {
            Emailtx.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.isBlank()) surligner(Emailtx, false);
                else if (estEmailValide(newVal.trim()))  surligner(Emailtx, false);
                else                                     surligner(Emailtx, true);
            });
        }

        if (imageDropZone != null) imageDropZone.setOnMouseClicked(e -> choisirImage());

        typeContenuCombo.valueProperty().addListener((obs, ancienne, nouvelle) ->
                adapterSectionFichier(nouvelle));

        if (urlPodcastField != null)
            urlPodcastField.textProperty().addListener((obs, o, n) -> setVisible(urlFeedbackBox, false));
        if (urlArticleField != null)
            urlArticleField.textProperty().addListener((obs, o, n) -> setVisible(urlArticleFeedbackBox, false));

        setupButtonHoverEffects();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MODÉRATION PAR CHAMP SÉPARÉ
    // ══════════════════════════════════════════════════════════════════════════

    private String verifierModeration() {
        String titre  = titreField.getText().trim();
        String resume = resumeField.getText().trim();
        String tags   = tagsField.getText().trim();

        if (!titre.isEmpty()) {
            try {
                ModerationService.ResultatModeration r =
                        moderationService.analyserContenu(titre);
                if (r.estInapproprie) {
                    return "Le TITRE contient du contenu inapproprie :\n"
                            + String.join(", ", r.categoriesDetectees);
                }
            } catch (Exception e) {
                System.err.println("Erreur moderation titre : " + e.getMessage());
            }
        }

        if (!resume.isEmpty()) {
            try {
                ModerationService.ResultatModeration r =
                        moderationService.analyserContenu(resume);
                if (r.estInapproprie) {
                    return "Le RESUME contient du contenu inapproprie :\n"
                            + String.join(", ", r.categoriesDetectees);
                }
            } catch (Exception e) {
                System.err.println("Erreur moderation resume : " + e.getMessage());
            }
        }

        if (!tags.isEmpty()) {
            try {
                ModerationService.ResultatModeration r =
                        moderationService.analyserContenu(tags);
                if (r.estInapproprie) {
                    return "Les TAGS contiennent du contenu inapproprie :\n"
                            + String.join(", ", r.categoriesDetectees);
                }
            } catch (Exception e) {
                System.err.println("Erreur moderation tags : " + e.getMessage());
            }
        }

        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // AJOUTER LA RESSOURCE  ✅ MODIFIÉ ICI
    // ══════════════════════════════════════════════════════════════════════════

    @FXML
    private void ajouterRessource(ActionEvent event) {
        if (!validerFormulaire()) return;

        String emailAuteur = Emailtx.getText().trim();

        String erreurModeration = verifierModeration();
        if (erreurModeration != null) {
            String[] categories = extraireCategoriesDepuisMessage(erreurModeration);
            boolean emailEnvoye = emailService.notifierContenuRefuse(
                    emailAuteur,
                    titreField.getText(),
                    categories
            );
            String msgEmail = emailEnvoye
                    ? "\nUn email vous a ete envoye a : " + emailAuteur
                    : "\n(L'email de notification n'a pas pu etre envoye)";
            showAlert(Alert.AlertType.ERROR,
                    "Contenu inapproprie",
                    erreurModeration + msgEmail);
            return;
        }

        try {
            Ressources nouvelleRessource = new Ressources();
            nouvelleRessource.setTitre(titreField.getText().trim());
            nouvelleRessource.setResume(resumeField.getText().trim());
            nouvelleRessource.setCategorie(categorieCombo.getValue());
            nouvelleRessource.setNiveau(niveauCombo.getValue());
            nouvelleRessource.setContenu(typeContenuCombo.getValue());

            String type = typeContenuCombo.getValue();

            // ✅ MODIFIÉ : Podcast et Article → URL externe (inchangé)
            if ("Podcast".equals(type)) {
                nouvelleRessource.setUrl(urlPodcastField.getText().trim());

            } else if ("Article".equals(type)) {
                nouvelleRessource.setUrl(urlArticleField.getText().trim());

            } else {
                // ✅ MODIFIÉ : Video et PDF → copier vers Symfony + stocker URL relative
                String urlRessource = uploadService.uploadRessource(fichierSelectionne);
                nouvelleRessource.setUrl(urlRessource);
                // urlRessource = "/uploads/flysystem-test/resources/mon-fichier.mp4"
            }

            // ✅ MODIFIÉ : Image → copier vers Symfony + stocker URL relative
            if (imageSelectionnee != null) {
                String urlImage = uploadService.uploadImage(imageSelectionnee);
                nouvelleRessource.setImageUrl(urlImage);
                // urlImage = "/uploads/flysystem-test/images/ma-photo.png"
            }

            String tagsRaw = tagsField.getText().trim();
            nouvelleRessource.setTags(tagsRaw);

            nouvelleRessource.setDate_publication(new Date());
            nouvelleRessource.setNbr_vues(0);
            nouvelleRessource.setLikes(0);
            nouvelleRessource.setEmailAuteur(emailAuteur);

            ressourceService.add(nouvelleRessource);

            new Thread(() -> emailService.notifierNouvelleRessource(
                    emailAuteur,
                    titreField.getText(),
                    categorieCombo.getValue(),
                    niveauCombo.getValue()
            )).start();

            showAlert(Alert.AlertType.INFORMATION, "Succes",
                    "La ressource a ete publiee avec succes !");
            retournerAffichage();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Erreur lors de la creation : " + e.getMessage());
        }
    }

    private String[] extraireCategoriesDepuisMessage(String message) {
        if (message == null) return new String[0];
        int idx = message.indexOf(":\n");
        if (idx == -1) return new String[]{message};
        String cats = message.substring(idx + 2).trim();
        return cats.split(", ");
    }

    // ========== ADAPTATION DYNAMIQUE SECTION 4 ==========

    private void adapterSectionFichier(String type) {
        setVisible(panneauAucun,   false);
        setVisible(panneauPodcast, false);
        setVisible(panneauFichier, false);
        setVisible(panneauArticle, false);

        if (type == null) {
            setVisible(panneauAucun, true);
            setText(sousTitreSection4, "Selectionnez le type de contenu pour voir les options");
            if (badgeTypeContenu != null) badgeTypeContenu.setText("");
            return;
        }

        switch (type) {
            case "Podcast":
                setVisible(panneauPodcast, true);
                setText(sousTitreSection4, "Collez l'URL de votre episode de podcast");
                setBadge(badgeTypeContenu, "PODCAST", "#1DB954", "white");
                break;
            case "Video":
                setVisible(panneauFichier, true);
                setText(sousTitreSection4, "Selectionnez votre fichier video depuis votre PC");
                setBadge(badgeTypeContenu, "VIDEO", "#7B5EA7", "white");
                mettreAJourBadgesFormats(
                        new String[]{"MP4","AVI","MKV","MOV"},
                        new String[]{"#7B5EA7","#9B59B6","#8E44AD","#6C3483"}, "V");
                break;
            case "PDF":
                setVisible(panneauFichier, true);
                setText(sousTitreSection4, "Selectionnez votre fichier PDF depuis votre PC");
                setBadge(badgeTypeContenu, "PDF", "#E53E3E", "white");
                mettreAJourBadgesFormats(new String[]{"PDF"}, new String[]{"#E53E3E"}, "P");
                break;
            case "Article":
                setVisible(panneauArticle, true);
                setText(sousTitreSection4, "Collez l'URL de votre article en ligne");
                setBadge(badgeTypeContenu, "ARTICLE", "#2980B9", "white");
                break;
            default:
                setVisible(panneauAucun, true);
        }
    }

    private void mettreAJourBadgesFormats(String[] formats, String[] couleurs, String icone) {
        if (badgesFormats != null) {
            while (badgesFormats.getChildren().size() > 1)
                badgesFormats.getChildren().remove(1);
            for (int i = 0; i < formats.length; i++) {
                Label badge = new Label(formats[i]);
                String c = (i < couleurs.length) ? couleurs[i] : "#555";
                badge.setStyle("-fx-background-color:" + c + ";-fx-text-fill:white;"
                        + "-fx-background-radius:6;-fx-padding:3 8;"
                        + "-fx-font-size:11px;-fx-font-weight:bold;");
                badgesFormats.getChildren().add(badge);
            }
        }
        if (iconeFichier != null) iconeFichier.setText(icone);
    }

    // ========== CHOISIR UN FICHIER ==========
    @FXML
    private void choisirFichier(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selectionner un fichier");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        String type = typeContenuCombo.getValue();
        if ("Video".equals(type)) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Video", "*.mp4","*.avi","*.mkv","*.mov"));
        } else if ("PDF".equals(type)) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        } else {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        }

        File file = fileChooser.showOpenDialog(choisirFichierBtn.getScene().getWindow());
        if (file != null) {
            fichierSelectionne = file;
            long sizeKB = file.length() / 1024;
            String taille = sizeKB > 1024
                    ? String.format("%.1f MB", sizeKB / 1024.0) : sizeKB + " KB";
            String nom = file.getName().toLowerCase();
            String couleur = nom.endsWith(".pdf") ? "#E53E3E"
                    : (nom.endsWith(".mp4")||nom.endsWith(".avi")
                    ||nom.endsWith(".mkv")||nom.endsWith(".mov"))
                    ? "#7B5EA7" : "#2D6A4F";
            fichierLabel.setText("OK  " + file.getName() + "  (" + taille + ")");
            fichierLabel.setStyle("-fx-text-fill:" + couleur
                    + ";-fx-font-weight:bold;-fx-font-size:13px;");
        }
    }

    // ========== VALIDER URL ==========
    @FXML private void validerUrlPodcast() {
        validerUrl(urlPodcastField, urlFeedbackBox,
                urlFeedbackIcon, urlFeedbackTitre, urlFeedbackDesc, "podcast");
    }

    @FXML private void validerUrlArticle() {
        validerUrl(urlArticleField, urlArticleFeedbackBox,
                urlArticleFeedbackIcon, urlArticleFeedbackTitre, urlArticleFeedbackDesc, "article");
    }

    private void validerUrl(TextField urlField, HBox feedbackBox,
                            Label feedbackIcon, Label feedbackTitre, Label feedbackDesc,
                            String typeLabel) {
        String saisie = urlField != null ? urlField.getText().trim() : "";
        if (saisie.isEmpty()) {
            afficherFeedback(feedbackBox, feedbackIcon, feedbackTitre, feedbackDesc,
                    false, "URL vide", "Veuillez coller l'URL du " + typeLabel + ".");
            return;
        }
        if (!saisie.startsWith("http://") && !saisie.startsWith("https://")) {
            afficherFeedback(feedbackBox, feedbackIcon, feedbackTitre, feedbackDesc,
                    false, "Format invalide", "L'URL doit commencer par https:// ou http://");
            return;
        }
        afficherFeedback(feedbackBox, feedbackIcon, feedbackTitre, feedbackDesc,
                null, "Verification en cours...", "Connexion a " + domaine(saisie));

        new Thread(() -> {
            boolean ok = false;
            String detail;
            try {
                HttpURLConnection conn =
                        (HttpURLConnection) new URL(saisie).openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                int code = conn.getResponseCode();
                ok     = (code >= 200 && code < 400);
                detail = ok ? "Lien accessible (code " + code + ")"
                        : "Non accessible (code " + code + ") — URL enregistree quand meme.";
                conn.disconnect();
            } catch (Exception e) {
                detail = "Impossible de verifier — URL enregistree quand meme.";
            }
            final boolean estOk = ok;
            final String  msg   = detail;
            javafx.application.Platform.runLater(() ->
                    afficherFeedback(feedbackBox, feedbackIcon, feedbackTitre, feedbackDesc,
                            estOk, estOk ? "URL valide" : "Non verifiee", msg));
        }).start();
    }

    private void afficherFeedback(HBox box, Label icon, Label titre, Label desc,
                                  Boolean ok, String titreText, String descText) {
        if (box == null) return;
        box.setVisible(true); box.setManaged(true);
        String fond, bordure, couleur;
        if (ok == null)  { fond="#F8FAFF"; bordure="#C7D2FE"; couleur="#4338CA"; }
        else if (ok)     { fond="#F0FDF4"; bordure="#86EFAC"; couleur="#15803D"; }
        else             { fond="#FFFBEB"; bordure="#FDE68A"; couleur="#92400E"; }

        box.setStyle("-fx-background-color:" + fond + ";-fx-background-radius:12;"
                + "-fx-border-color:" + bordure + ";-fx-border-radius:12;"
                + "-fx-border-width:1;-fx-padding:14 20;");
        if (icon  != null) icon.setText(ok == null ? "..." : (ok ? "✓" : "⚠"));
        if (titre != null) {
            titre.setText(titreText);
            titre.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + couleur + ";");
        }
        if (desc  != null) {
            desc.setText(descText);
            desc.setStyle("-fx-font-size:11.5px;-fx-text-fill:" + couleur + ";");
        }
    }

    private String domaine(String url) {
        try { return new URL(url).getHost(); } catch (Exception e) { return url; }
    }

    // ========== CHOISIR UNE IMAGE ==========
    @FXML
    private void choisirImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selectionner une image de couverture");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif"));

        Stage window = (Stage) choisirImageBtn.getScene().getWindow();
        imageSelectionnee = fileChooser.showOpenDialog(window);
        if (imageSelectionnee == null) return;

        if (imageSelectionnee.length() > 5L * 1024 * 1024) {
            showAlert(Alert.AlertType.WARNING, "Image trop volumineuse",
                    "Veuillez choisir une image de moins de 5 MB.");
            imageSelectionnee = null;
            return;
        }

        if (imageLabel      != null) imageLabel.setText(imageSelectionnee.getName());
        if (imageTailleLabel != null)
            imageTailleLabel.setText(formaterTailleImage(imageSelectionnee.length()));
        if (imageBandeauBox  != null) {
            imageBandeauBox.setVisible(true); imageBandeauBox.setManaged(true);
        }
        if (imagePreview != null) {
            try {
                imagePreview.setImage(new Image(imageSelectionnee.toURI().toString()));
                imagePreview.setVisible(true);
                if (emptyImageState != null) emptyImageState.setVisible(false);
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'image.");
            }
        }
    }

    @FXML
    private void supprimerImage() {
        imageSelectionnee = null;
        if (imagePreview     != null) { imagePreview.setImage(null); imagePreview.setVisible(false); }
        if (emptyImageState  != null) emptyImageState.setVisible(true);
        if (imageBandeauBox  != null) { imageBandeauBox.setVisible(false); imageBandeauBox.setManaged(false); }
        if (imageLabel       != null) imageLabel.setText("");
        if (imageTailleLabel != null) imageTailleLabel.setText("");
    }

    private String formaterTailleImage(long bytes) {
        if (bytes < 1024)    return bytes + " o";
        if (bytes < 1048576) return String.format("%.1f Ko", bytes / 1024.0);
        return String.format("%.2f Mo", bytes / 1048576.0);
    }

    // ========== VALIDATION ==========

    private boolean estEmailValide(String email) {
        if (email == null || email.isBlank()) return false;
        return email.matches("^[\\w.+\\-]+@[a-zA-Z0-9\\-]+\\.[a-zA-Z]{2,}$");
    }

    private boolean validerFormulaire() {
        if (estVide(titreField.getText())) {
            showAlert(Alert.AlertType.ERROR, "Champ requis", "Le titre est obligatoire !");
            titreField.requestFocus(); return false;
        }
        if (estVide(resumeField.getText())) {
            showAlert(Alert.AlertType.ERROR, "Champ requis", "Le resume est obligatoire !");
            resumeField.requestFocus(); return false;
        }
        if (categorieCombo.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Champ requis",
                    "Veuillez selectionner une categorie !"); return false;
        }
        if (niveauCombo.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Champ requis",
                    "Veuillez selectionner un niveau !"); return false;
        }
        if (typeContenuCombo.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Champ requis",
                    "Veuillez selectionner un type de contenu !"); return false;
        }

        String type = typeContenuCombo.getValue();
        if ("Podcast".equals(type)) {
            String url = urlPodcastField != null ? urlPodcastField.getText().trim() : "";
            if (url.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "URL manquante",
                        "Veuillez coller l'URL du podcast."); return false;
            }
        } else if ("Article".equals(type)) {
            String url = urlArticleField != null ? urlArticleField.getText().trim() : "";
            if (url.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "URL manquante",
                        "Veuillez coller l'URL de l'article."); return false;
            }
        } else {
            if (fichierSelectionne == null) {
                showAlert(Alert.AlertType.ERROR, "Fichier manquant",
                        "Veuillez selectionner un fichier."); return false;
            }
        }

        String email = Emailtx.getText() == null ? "" : Emailtx.getText().trim();
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Champ requis",
                    "L'adresse email de l'auteur est obligatoire !");
            surligner(Emailtx, true); Emailtx.requestFocus(); return false;
        }
        if (!estEmailValide(email)) {
            showAlert(Alert.AlertType.ERROR, "Email invalide",
                    "Veuillez saisir une adresse email valide.");
            surligner(Emailtx, true); Emailtx.requestFocus(); return false;
        }
        surligner(Emailtx, false);
        return true;
    }

    // ========== EFFETS HOVER ==========
    private void setupButtonHoverEffects() {
        if (ajouterBtn != null) {
            String s = ajouterBtn.getStyle();
            ajouterBtn.setOnMouseEntered(e ->
                    ajouterBtn.setStyle(s + "-fx-scale-x:1.05;-fx-scale-y:1.05;"));
            ajouterBtn.setOnMouseExited(e -> ajouterBtn.setStyle(s));
        }
        if (annulerBtn != null) {
            String s = annulerBtn.getStyle();
            annulerBtn.setOnMouseEntered(e ->
                    annulerBtn.setStyle(s + "-fx-background-color:#F7FAFC;"));
            annulerBtn.setOnMouseExited(e -> annulerBtn.setStyle(s));
        }
    }

    // ========== ANNULER ==========
    @FXML
    private void annuler() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Annuler la creation");
        confirmation.setContentText(
                "Voulez-vous vraiment annuler ?\nLes donnees saisies seront perdues.");
        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) retournerAffichage();
        });
    }

    // ========== RETOUR AFFICHAGE ==========
    private void retournerAffichage() {
        if (dashboardController != null) {
            dashboardController.navigateTo("AfficherRe.fxml", "Gestion des ressources", "Gestion de Rania");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AfficherRe.fxml"));
            if (loader.getLocation() == null) {
                ((Stage) ajouterBtn.getScene().getWindow()).close();
                return;
            }
            Parent root  = loader.load();
            Stage  stage = (Stage) ajouterBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            ((Stage) ajouterBtn.getScene().getWindow()).close();
        }
    }

    // ========== UTILITAIRES ==========
    private void surligner(TextField f, boolean erreur) {
        if (f == null) return;
        String base = f.getStyle()
                .replaceAll("-fx-border-color:[^;]+;", "")
                .replaceAll("-fx-border-width:[^;]+;", "");
        f.setStyle(base + (erreur
                ? "-fx-border-color:#EF4444;-fx-border-width:2;"
                : "-fx-border-color:#22C55E;-fx-border-width:2;"));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color:white;");
        alert.showAndWait();
    }

    private boolean estVide(String s) { return s == null || s.trim().isEmpty(); }

    private void setVisible(javafx.scene.Node n, boolean v) {
        if (n != null) { n.setVisible(v); n.setManaged(v); }
    }

    private void setText(Label l, String t) {
        if (l != null) l.setText(t != null ? t : "");
    }

    private void setBadge(Label badge, String text, String bg, String fg) {
        if (badge == null) return;
        badge.setText(text);
        badge.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";"
                + "-fx-background-radius:20;-fx-padding:5 14;"
                + "-fx-font-size:11px;-fx-font-weight:bold;");
    }
}