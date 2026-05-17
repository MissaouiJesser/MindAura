package tn.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.components.LanguageSelectorComponent;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.enums.typeL;
import tn.esprit.services.TranslationService;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.salle_SERVICE;
import tn.esprit.utils.ImageManager;
import tn.esprit.utils.LanguageManager;

public class AfficherLocalController extends TranslatableController {

    // ── TABLE ─────────────────────────────────────────────────────────────────
    @FXML private TableView<local_psychiatrie> tableLocal;
    @FXML private TableColumn<local_psychiatrie, String>  colNom;
    @FXML private TableColumn<local_psychiatrie, String>  colImageURL;
    @FXML private TableColumn<local_psychiatrie, String>  colAdresse;
    @FXML private TableColumn<local_psychiatrie, String>  colVille;
    @FXML private TableColumn<local_psychiatrie, String>  colType;
    @FXML private TableColumn<local_psychiatrie, Integer> colTelephone;
    @FXML private TableColumn<local_psychiatrie, String>  colEmail;
    @FXML private TableColumn<local_psychiatrie, String>  colCapacite;
    @FXML private TableColumn<local_psychiatrie, String>  colDisponibilite;
    @FXML private TableColumn<local_psychiatrie, Void>    colActions;

    // ── FILTRES & RECHERCHE ───────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterTypeComboBox;
    @FXML private ComboBox<String> filterVilleComboBox;

    // ── STATS ─────────────────────────────────────────────────────────────────
    @FXML private Label totalLocauxLabel;
    @FXML private Label totalLocauxBadge;
    @FXML private Label totalDisponiblesLabel;
    @FXML private Label totalReservationsLabel;
    @FXML private Label totalReservationsBadge;

    // ── BOUTONS LISTE ─────────────────────────────────────────────────────────
    @FXML private Button btnAjouter;
    @FXML private Button btnRafraichir;
    @FXML private Button btnFrontOffice;

    // ── PAGINATION ────────────────────────────────────────────────────────────
    @FXML private Button btnPremierePage;
    @FXML private Button btnPagePrecedente;
    @FXML private Button btnPageSuivante;
    @FXML private Button btnDernierePage;
    @FXML private Label  paginationInfoLabel;
    @FXML private Label  currentPageLabel;

    // ── LANGUE ────────────────────────────────────────────────────────────────
    @FXML private ComboBox<String> comboLangue;

    // ── SWAP DE VUES ─────────────────────────────────────────────────────────
    @FXML private VBox vueListe;
    @FXML private VBox vueFormulaire;

    // ✅ NOUVEAU : vue détails inline (à déclarer dans le FXML)
    @FXML private VBox vueDetails;

    // ── CHAMPS FORMULAIRE ─────────────────────────────────────────────────────
    @FXML private Label        lblTitreFormulaire;
    @FXML private Label        lblSousTitreFormulaire;
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
    @FXML private Button       btnSauvegarder;
    @FXML private Label        lblMessage;
    @FXML private ImageView    imgPreview;

    // ── ÉTAT INTERNE ──────────────────────────────────────────────────────────
    private local_psychiatrie_SERVICE localService;
    private final salle_SERVICE        salleService = new salle_SERVICE();
    private ObservableList<local_psychiatrie> localList;
    private ObservableList<local_psychiatrie> filteredList;
    private final ObservableList<local_psychiatrie> pageCourante = FXCollections.observableArrayList();
    private final TranslationService translationService = TranslationService.getInstance();

    /** null → mode AJOUTER  /  non-null → mode MODIFIER */
    private local_psychiatrie localEnCours = null;
    private File   selectedImageFile;
    private String originalImagePath;

    // ✅ Pour stocker la vue détails chargée dynamiquement
    private Parent detailsPane = null;
    private DetailsLocalController detailsController = null;

    private static final int ITEMS_PAR_PAGE = 5;
    private int currentPage = 1;
    private int totalPages  = 1;

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

        initializeTableColumns();
        loadData();
        initializeFilters();

        searchField.textProperty().addListener((obs, o, n) -> { currentPage = 1; filterData(); });
        filterTypeComboBox.valueProperty().addListener((obs, o, n) -> { currentPage = 1; filterData(); });
        filterVilleComboBox.valueProperty().addListener((obs, o, n) -> { currentPage = 1; filterData(); });

        setupFormValidation();
        applyFadeInAnimation();
        applyCSSStyles();

        if (comboLangue != null) LanguageSelectorComponent.setup(comboLangue);
        LanguageManager.getInstance().addListener(this::onLanguageChanged);
        String lang = LanguageManager.getInstance().getCurrentLanguage();
        if (!TranslationService.LANG_FR.equals(lang)) onLanguageChanged(lang);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SWAP DE VUES  — liste / formulaire / détails
    // ─────────────────────────────────────────────────────────────────────────

    /** Cache tout sauf la liste */
    public void showListView() {
        // Masquer formulaire
        if (vueFormulaire != null) {
            vueFormulaire.setVisible(false);
            vueFormulaire.setManaged(false);
        }
        // Masquer détails (si vue FXML dédiée)
        if (vueDetails != null) {
            vueDetails.setVisible(false);
            vueDetails.setManaged(false);
        }
        // Masquer détails chargés dynamiquement
        if (detailsPane != null) {
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);
        }
        // Afficher liste
        vueListe.setVisible(true);
        vueListe.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), vueListe);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
        localEnCours = null;
        selectedImageFile = null;
    }

    /** Affiche le formulaire (Ajouter / Modifier) plein écran */
    private void afficherFormulaire() {
        vueListe.setVisible(false);
        vueListe.setManaged(false);
        if (vueDetails != null) { vueDetails.setVisible(false); vueDetails.setManaged(false); }
        if (detailsPane != null) { detailsPane.setVisible(false); detailsPane.setManaged(false); }

        vueFormulaire.setVisible(true);
        vueFormulaire.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), vueFormulaire);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * ✅ MODIFIÉ : Affiche les détails INLINE (dans la même fenêtre, sans nouveau Stage).
     * La vue DetailsLocal.fxml est chargée et injectée dans le conteneur parent de vueListe.
     */
    private void afficherDetails(local_psychiatrie local) {
        try {
            // Charger le FXML des détails
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsLocal.fxml"));
            Parent detailsRoot = loader.load();
            detailsPane = detailsRoot;

            DetailsLocalController ctrl = loader.getController();
            ctrl.setLocal(local);
            ctrl.setParentController(this); // ← passer la référence pour le retour

            // Masquer la liste et le formulaire
            vueListe.setVisible(false);
            vueListe.setManaged(false);
            if (vueFormulaire != null) { vueFormulaire.setVisible(false); vueFormulaire.setManaged(false); }

            // Injecter la vue dans le même parent que vueListe
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) vueListe.getParent();
            if (!parent.getChildren().contains(detailsRoot)) {
                parent.getChildren().add(detailsRoot);
            }
            detailsRoot.setVisible(true);
            detailsRoot.setManaged(true);

            // Si c'est un VBox/HBox, s'assurer que la vue prend tout l'espace
            if (parent instanceof VBox) {
                VBox.setVgrow(detailsRoot, javafx.scene.layout.Priority.ALWAYS);
            } else if (parent instanceof HBox) {
                HBox.setHgrow(detailsRoot, javafx.scene.layout.Priority.ALWAYS);
            }

            FadeTransition ft = new FadeTransition(Duration.millis(300), detailsRoot);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'afficher les détails", e.getMessage());
        }
    }

    /** Retourne à la liste depuis le formulaire (bouton Annuler du formulaire) */
    @FXML
    private void handleRetourListe() {
        showListView();
        // Nettoyer les vues détails injectées dynamiquement
        if (detailsPane != null) {
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) vueListe.getParent();
            parent.getChildren().remove(detailsPane);
            detailsPane = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AJOUTER / MODIFIER
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleAjouterLocal() {
        localEnCours     = null;
        selectedImageFile = null;
        originalImagePath = null;
        viderFormulaire();
        lblTitreFormulaire.setText("➕ Ajouter un Nouveau Local");
        lblSousTitreFormulaire.setText("Remplissez les informations du local psychiatrique");
        btnSauvegarder.setText("✓ Ajouter le Local");
        lblMessage.setText("");
        afficherFormulaire();
    }

    private void ouvrirFormulaireModifier(local_psychiatrie local) {
        localEnCours      = local;
        selectedImageFile  = null;
        originalImagePath  = local.getImageURL();

        txtNom.setText(local.getNom_local());
        txtAdresse.setText(local.getAdresse_local());
        txtVille.setText(local.getVille_local());
        txtDescription.setText(local.getDescription_local());
        txtCapacite.setText(local.getCapacite_local());
        comboType.setValue(local.getType_local());
        txtTelephone.setText(String.valueOf(local.getTelephone_local()));
        txtEmail.setText(local.getEmail_local());
        comboDisponibilite.setValue(local.getDisponibilite_local());

        if (local.getImageURL() != null && !local.getImageURL().isEmpty()) {
            txtImageURL.setText(new File(local.getImageURL()).getName());
            if (imgPreview != null) {
                Image img = ImageManager.loadImage(local.getImageURL(), 200, 150, true);
                if (img != null) imgPreview.setImage(img);
            }
        } else {
            txtImageURL.setText("");
            if (imgPreview != null) imgPreview.setImage(null);
        }

        lblTitreFormulaire.setText("✏️ Modifier le Local");
        lblSousTitreFormulaire.setText("Modifiez les informations de : " + local.getNom_local());
        btnSauvegarder.setText("✓ Enregistrer les Modifications");
        lblMessage.setText("");
        afficherFormulaire();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAUVEGARDER (Ajouter OU Modifier selon le mode)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleSauvegarder() {
        if (!validateForm()) return;

        if (localEnCours == null) {
            // ── MODE AJOUTER ──
            try {
                String imagePath = null;
                if (selectedImageFile != null) {
                    imagePath = ImageManager.saveImage(selectedImageFile);
                    if (imagePath == null) showMessage("⚠ L'image n'a pas pu être sauvegardée", "warning");
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
                // ── CASCADE : si le local est indisponible, rendre ses salles indisponibles ──
                cascaderDisponibiliteVersSalles(local);
                showMessage("✓ Local ajouté avec succès !", "success");
                refreshTable();
                retournerApresDelai();
            } catch (SQLException e) {
                showMessage("✗ Erreur lors de l'ajout: " + e.getMessage(), "error");
            } catch (NumberFormatException e) {
                showMessage("✗ Format du téléphone invalide", "error");
            } catch (java.io.IOException e) { showMessage("x Erreur image : " + e.getMessage(), "error");}
        } else {
            // ── MODE MODIFIER ──
            try {
                String imagePath = originalImagePath;
                if (selectedImageFile != null) {
                    String newPath = ImageManager.saveImage(selectedImageFile);
                    if (newPath != null) {
                        if (originalImagePath != null && !originalImagePath.isEmpty())
                            ImageManager.deleteImage(originalImagePath);
                        imagePath = newPath;
                    } else {
                        showMessage("⚠ La nouvelle image n'a pas pu être sauvegardée", "warning");
                    }
                }
                localEnCours.setNom_local(txtNom.getText().trim());
                localEnCours.setAdresse_local(txtAdresse.getText().trim());
                localEnCours.setVille_local(txtVille.getText().trim());
                localEnCours.setDescription_local(txtDescription.getText().trim());
                localEnCours.setCapacite_local(txtCapacite.getText().trim());
                localEnCours.setType_local(comboType.getValue());
                localEnCours.setTelephone_local(Integer.parseInt(txtTelephone.getText().trim()));
                localEnCours.setEmail_local(txtEmail.getText().trim());
                localEnCours.setDisponibilite_local(comboDisponibilite.getValue());
                localEnCours.setImageURL(imagePath != null ? imagePath : "");

                localService.modifier(localEnCours);
                // ── CASCADE : synchroniser la disponibilité des salles du local ──
                cascaderDisponibiliteVersSalles(localEnCours);
                showMessage("✓ Local modifié avec succès !", "success");
                refreshTable();
                retournerApresDelai();
            } catch (SQLException e) {
                showMessage("✗ Erreur lors de la modification: " + e.getMessage(), "error");
            } catch (NumberFormatException e) {
                showMessage("✗ Format du téléphone invalide", "error");
            } catch (java.io.IOException e) { showMessage("x Erreur image : " + e.getMessage(), "error");}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CASCADE DISPONIBILITÉ LOCAL → SALLES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Si le local est "Non disponible", toutes ses salles passent à "Non disponible".
     * Si le local redevient "Disponible" ou "Sur réservation", les salles repassent
     * à "Disponible" (elles redeviennent actives).
     * Un message récapitulatif est affiché avec le nombre de salles mises à jour.
     */
    private void cascaderDisponibiliteVersSalles(local_psychiatrie local) {
        try {
            List<tn.esprit.entities.salle> sallesDuLocal =
                    salleService.getSallesByLocal(local.getId_local());
            if (sallesDuLocal == null || sallesDuLocal.isEmpty()) return;

            boolean localIndisponible = "Non disponible".equalsIgnoreCase(local.getDisponibilite_local());
            String nouvelleDisponibilite = localIndisponible ? "Non disponible" : "Disponible";

            int compteur = 0;
            for (tn.esprit.entities.salle s : sallesDuLocal) {
                if (!nouvelleDisponibilite.equalsIgnoreCase(s.getDisponibilite_salle())) {
                    s.setDisponibilite_salle(nouvelleDisponibilite);
                    salleService.modifier(s);
                    compteur++;
                }
            }

            if (compteur > 0) {
                String msg = localIndisponible
                        ? "⚠ " + compteur + " salle(s) du local marquée(s) « Non disponible »"
                        : "✓ " + compteur + " salle(s) du local remise(s) « Disponible »";
                showMessage(msg, localIndisponible ? "warning" : "success");
            }
        } catch (SQLException e) {
            showMessage("⚠ Disponibilité des salles non synchronisée : " + e.getMessage(), "warning");
        }
    }

    private void retournerApresDelai() {
        new Thread(() -> {
            try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(this::handleRetourListe);
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PARCOURIR IMAGE
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleParcourir() {
        File selectedFile = ImageManager.selectImageFile(vueFormulaire.getScene().getWindow());
        if (selectedFile != null) {
            if (ImageManager.isValidImageFile(selectedFile)) {
                selectedImageFile = selectedFile;
                txtImageURL.setText(selectedFile.getName());
                if (imgPreview != null)
                    imgPreview.setImage(ImageManager.loadImage(selectedFile.getAbsolutePath(), 200, 150, true));
                showMessage("✓ Image sélectionnée: " + selectedFile.getName(), "success");
            } else {
                showMessage("✗ Fichier image invalide (PNG, JPG, JPEG, GIF)", "error");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VALIDATION FORMULAIRE
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFormValidation() {
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
            txtEmail.setStyle("-fx-border-color: #B91C1C; -fx-border-width: 2px;");
            return false;
        }
        txtEmail.setStyle("-fx-border-color: #2D6A4F; -fx-border-width: 1px;");
        return true;
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (txtNom.getText().trim().isEmpty())      { errors.append("- Le nom est obligatoire\n");      txtNom.setStyle("-fx-border-color: #B91C1C; -fx-border-width: 2px;"); } else txtNom.setStyle("");
        if (txtAdresse.getText().trim().isEmpty())  { errors.append("- L'adresse est obligatoire\n");  txtAdresse.setStyle("-fx-border-color: #B91C1C; -fx-border-width: 2px;"); } else txtAdresse.setStyle("");
        if (txtVille.getText().trim().isEmpty())    { errors.append("- La ville est obligatoire\n");    txtVille.setStyle("-fx-border-color: #B91C1C; -fx-border-width: 2px;"); } else txtVille.setStyle("");
        if (txtTelephone.getText().trim().isEmpty() || txtTelephone.getText().trim().length() != 8)
        { errors.append("- Téléphone : 8 chiffres requis\n"); txtTelephone.setStyle("-fx-border-color: #B91C1C; -fx-border-width: 2px;"); } else txtTelephone.setStyle("");
        if (txtEmail.getText().trim().isEmpty())    { errors.append("- L'email est obligatoire\n");     txtEmail.setStyle("-fx-border-color: #B91C1C; -fx-border-width: 2px;"); }
        else if (!validateEmail())                  { errors.append("- L'email n'est pas valide\n"); }
        if (txtCapacite.getText().trim().isEmpty()) { errors.append("- La capacité est obligatoire\n"); txtCapacite.setStyle("-fx-border-color: #B91C1C; -fx-border-width: 2px;"); } else txtCapacite.setStyle("");
        if (errors.length() > 0) { showMessage("Erreurs:\n" + errors, "error"); return false; }
        return true;
    }

    private void viderFormulaire() {
        txtNom.clear(); txtAdresse.clear(); txtVille.clear(); txtDescription.clear();
        txtCapacite.clear(); txtTelephone.clear(); txtEmail.clear(); txtImageURL.clear();
        comboType.setValue(typeL.HOPITAL);
        comboDisponibilite.setValue("Disponible");
        if (imgPreview != null) imgPreview.setImage(null);
        txtNom.setStyle(""); txtAdresse.setStyle(""); txtVille.setStyle("");
        txtTelephone.setStyle(""); txtEmail.setStyle(""); txtCapacite.setStyle("");
    }

    private void showMessage(String message, String type) {
        lblMessage.setText(message);
        if ("success".equals(type))
            lblMessage.setStyle("-fx-text-fill: #1B4332; -fx-font-weight: bold; -fx-background-color: #D8F3DC; -fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #2D6A4F; -fx-border-width: 1px; -fx-border-radius: 10px;");
        else if ("warning".equals(type))
            lblMessage.setStyle("-fx-text-fill: #B45309; -fx-font-weight: bold; -fx-background-color: #FEF3C7; -fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #D97706; -fx-border-width: 1px; -fx-border-radius: 10px;");
        else
            lblMessage.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: bold; -fx-background-color: #FEE2E2; -fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #DC2626; -fx-border-width: 1px; -fx-border-radius: 10px;");
        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COLONNES TABLE
    // ─────────────────────────────────────────────────────────────────────────

    private void initializeTableColumns() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom_local"));
        colAdresse.setCellValueFactory(new PropertyValueFactory<>("adresse_local"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville_local"));
        colType.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getType_local().getLibelle()));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone_local"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email_local"));
        colCapacite.setCellValueFactory(new PropertyValueFactory<>("capacite_local"));
        // ── COLONNE IMAGE : affiche la vignette du local ──────────────────────
        colImageURL.setCellValueFactory(new PropertyValueFactory<>("imageURL"));
        colImageURL.setVisible(true);
        colImageURL.setCellFactory(column -> new TableCell<local_psychiatrie, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(70);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(70, 50);
                clip.setArcWidth(8);
                clip.setArcHeight(8);
                imageView.setClip(clip);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isBlank()) {
                    Label lbl = new Label("\uD83C\uDFE5");
                    lbl.setStyle("-fx-font-size: 26px; -fx-text-fill: #9CA3AF;");
                    setGraphic(lbl);
                    setText(null);
                } else {
                    Image img = ImageManager.loadImage(imagePath, 70, 50, true);
                    if (img != null && !img.isError()) {
                        imageView.setImage(img);
                        setGraphic(imageView);
                    } else {
                        Label lbl = new Label("\uD83C\uDFE5");
                        lbl.setStyle("-fx-font-size: 26px; -fx-text-fill: #9CA3AF;");
                        setGraphic(lbl);
                    }
                    setText(null);
                }
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
        setupDisponibiliteColumn();
        addActionButtons();
    }

    private void setupDisponibiliteColumn() {
        colDisponibilite.setCellValueFactory(new PropertyValueFactory<>("disponibilite_local"));
        colDisponibilite.setCellFactory(column -> new TableCell<local_psychiatrie, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                getStyleClass().removeAll("badge", "badge-disponible", "badge-non-disponible", "badge-reservation");
                getStyleClass().add("badge");
                if (item.equals("Disponible"))          getStyleClass().add("badge-disponible");
                else if (item.equals("Non disponible")) getStyleClass().add("badge-non-disponible");
                else                                    getStyleClass().add("badge-reservation");
                setGraphic(null);
            }
        });
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Button btnDetails   = new Button("Détails");
                Button btnModifier  = new Button("Modifier");
                Button btnSupprimer = new Button("Supprimer");

                btnDetails.setStyle("-fx-background-color: #1B4332; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 6px 10px; -fx-background-radius: 8px; -fx-cursor: hand;");
                btnModifier.setStyle("-fx-background-color: #B45309; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 6px 10px; -fx-background-radius: 8px; -fx-cursor: hand;");
                btnSupprimer.setStyle("-fx-background-color: #B91C1C; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: 600; -fx-padding: 6px 10px; -fx-background-radius: 8px; -fx-cursor: hand;");

                // ✅ MODIFIÉ : afficherDetails ouvre maintenant inline (pas de Stage)
                btnDetails.setOnAction(e -> afficherDetails(getTableView().getItems().get(getIndex())));
                btnModifier.setOnAction(e -> ouvrirFormulaireModifier(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> supprimerLocal(getTableView().getItems().get(getIndex())));

                HBox hBox = new HBox(6, btnDetails, btnModifier, btnSupprimer);
                hBox.setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(hBox);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DONNÉES & FILTRES
    // ─────────────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            localList    = FXCollections.observableArrayList(localService.afficherList());
            filteredList = FXCollections.observableArrayList(localList);
            updateStatistics();
            updatePagination();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les données", e.getMessage());
        }
    }

    private void initializeFilters() {
        ObservableList<String> types = FXCollections.observableArrayList("Tous");
        for (typeL type : typeL.values()) types.add(type.getLibelle());
        filterTypeComboBox.setItems(types);
        filterTypeComboBox.setValue("Tous");
        updateVilleFilter();
    }

    private void updateVilleFilter() {
        ObservableList<String> villes = FXCollections.observableArrayList("Toutes");
        localList.forEach(local -> { if (!villes.contains(local.getVille_local())) villes.add(local.getVille_local()); });
        String current = filterVilleComboBox.getValue();
        filterVilleComboBox.setItems(villes);
        filterVilleComboBox.setValue(villes.contains(current) ? current : "Toutes");
    }

    private void filterData() {
        String search = searchField.getText().toLowerCase();
        String type   = filterTypeComboBox.getValue();
        String ville  = filterVilleComboBox.getValue();
        filteredList.clear();
        for (local_psychiatrie local : localList) {
            boolean ms = search.isEmpty() || local.getNom_local().toLowerCase().contains(search)
                    || local.getAdresse_local().toLowerCase().contains(search)
                    || local.getVille_local().toLowerCase().contains(search);
            boolean mt = type  == null || type.equals("Tous") || local.getType_local().getLibelle().equals(type);
            boolean mv = ville == null || ville.equals("Toutes") || local.getVille_local().equals(ville);
            if (ms && mt && mv) filteredList.add(local);
        }
        updateStatistics();
        updatePagination();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PAGINATION
    // ─────────────────────────────────────────────────────────────────────────

    private void updatePagination() {
        int total = filteredList.size();
        totalPages = (total == 0) ? 1 : (int) Math.ceil((double) total / ITEMS_PAR_PAGE);
        if (currentPage < 1) currentPage = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        int debut = (currentPage - 1) * ITEMS_PAR_PAGE;
        int fin   = Math.min(debut + ITEMS_PAR_PAGE, total);
        pageCourante.setAll(filteredList.subList(debut, fin));
        if (tableLocal.getItems() != pageCourante) tableLocal.setItems(pageCourante);
        tableLocal.refresh();
        currentPageLabel.setText(String.valueOf(currentPage));
        paginationInfoLabel.setText("Page " + currentPage + " / " + totalPages + "  •  " + total + " local" + (total > 1 ? "ux" : ""));
        btnPremierePage.setDisable(currentPage == 1);
        btnPagePrecedente.setDisable(currentPage == 1);
        btnPageSuivante.setDisable(currentPage == totalPages);
        btnDernierePage.setDisable(currentPage == totalPages);
    }

    @FXML private void handlePremierePage()   { currentPage = 1; updatePagination(); }
    @FXML private void handlePagePrecedente() { if (currentPage > 1) { currentPage--; updatePagination(); } }
    @FXML private void handlePageSuivante()   { if (currentPage < totalPages) { currentPage++; updatePagination(); } }
    @FXML private void handleDernierePage()   { currentPage = totalPages; updatePagination(); }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATISTIQUES
    // ─────────────────────────────────────────────────────────────────────────

    private void updateStatistics() {
        int total = filteredList.size();
        long disponibles  = filteredList.stream().filter(l -> "Disponible".equalsIgnoreCase(l.getDisponibilite_local())).count();
        long reservations = filteredList.stream().filter(l -> l.getDisponibilite_local() != null
                && !l.getDisponibilite_local().equalsIgnoreCase("Disponible")
                && !l.getDisponibilite_local().equalsIgnoreCase("Non disponible")).count();
        totalLocauxLabel.setText(String.valueOf(total));
        if (totalDisponiblesLabel  != null) totalDisponiblesLabel.setText(String.valueOf(disponibles));
        if (totalReservationsLabel != null) totalReservationsLabel.setText(String.valueOf(reservations));
        if (totalLocauxBadge       != null) totalLocauxBadge.setText(String.valueOf(total));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPPRIMER
    // ─────────────────────────────────────────────────────────────────────────

    private void supprimerLocal(local_psychiatrie local) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer ce local ?");
        alert.setContentText(local.getNom_local() + " - " + local.getVille_local());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                localService.delete(local);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Local supprimé", "Le local a été supprimé avec succès");
                refreshTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le local", e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RAFRAÎCHISSEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleRafraichir() { refreshTable(); applyRefreshAnimation(); }

    public void refreshTable() {
        try {
            localList.clear();
            localList.addAll(localService.afficherList());
            searchField.clear();
            filterTypeComboBox.setValue("Tous");
            filterVilleComboBox.setValue("Toutes");
            updateVilleFilter();
            currentPage = 1;
            filterData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de rafraîchir les données", e.getMessage());
        }
    }

    // ── Navigation methods removed: now handled by Dashboard.fxml shared sidebar ──
    // Previously: handleNavigateToDashboard, handleNavigateToLocaux, handleNavigateToReservations
    // These are replaced by DashboardController's navigation system

    // ─────────────────────────────────────────────────────────────────────────
    //  TRADUCTION
    // ─────────────────────────────────────────────────────────────────────────

    private void onLanguageChanged(String langCode) {
        traduireButton(btnAjouter,     "➕ Nouveau Local", langCode);
        traduireColonne(colNom,          "Nom du Local",  langCode);
        traduireColonne(colAdresse,      "Adresse",       langCode);
        traduireColonne(colVille,        "Ville",         langCode);
        traduireColonne(colType,         "Type",          langCode);
        traduireColonne(colTelephone,    "Téléphone",     langCode);
        traduireColonne(colEmail,        "Email",         langCode);
        traduireColonne(colCapacite,     "Capacité",      langCode);
        traduireColonne(colDisponibilite,"Disponibilité", langCode);
        traduireColonne(colActions,      "Actions",       langCode);
        if (searchField != null) {
            if (TranslationService.LANG_FR.equals(langCode)) searchField.setPromptText("Rechercher un local...");
            else translationService.traduireAsync("Rechercher un local...", searchField::setPromptText);
        }
    }

    private void traduireButton(Button btn, String texteFr, String langCode) {
        if (btn == null) return;
        if (TranslationService.LANG_FR.equals(langCode)) { btn.setText(texteFr); return; }
        translationService.traduireAsync(texteFr, btn::setText);
    }

    private <T> void traduireColonne(TableColumn<local_psychiatrie, T> col, String texteFr, String langCode) {
        if (col == null) return;
        if (TranslationService.LANG_FR.equals(langCode)) { col.setText(texteFr); return; }
        translationService.traduireAsync(texteFr, col::setText);
    }

    @Override
    protected void traduireUI(String langCode) {
        tr(btnAjouter,      "➕ Nouveau Local");
        tr(colNom,          "Nom du Local");
        tr(colAdresse,      "Adresse");
        tr(colVille,        "Ville");
        tr(colType,         "Type");
        tr(colTelephone,    "Téléphone");
        tr(colEmail,        "Email");
        tr(colCapacite,     "Capacité");
        tr(colDisponibilite,"Disponibilité");
        tr(colActions,      "Actions");
        trPrompt(searchField, "Rechercher un local...");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANIMATIONS & UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private void applyCSSStyles() {
        if (btnAjouter    != null) btnAjouter.getStyleClass().add("btn-primary-action");
        if (btnRafraichir != null) btnRafraichir.getStyleClass().add("icon-btn");
    }

    private void applyFadeInAnimation() {
        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), tableLocal);
        fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0); fadeIn.play();
    }

    private void applyRefreshAnimation() {
        if (btnRafraichir == null) return;
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), btnRafraichir);
        scale.setFromX(1.0); scale.setFromY(1.0);
        scale.setToX(0.9);   scale.setToY(0.9);
        scale.setCycleCount(2); scale.setAutoReverse(true); scale.play();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(header);
        alert.setContentText(content); alert.showAndWait();
    }
    @FXML
    private void handleQRCode() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QRCode.fxml"));
            Parent root = loader.load();

            Stage qrStage = new Stage();
            qrStage.setTitle("QR Code — Locaux Disponibles");
            qrStage.setScene(new javafx.scene.Scene(root));
            qrStage.setResizable(false);

            // Arrêter le serveur HTTP quand la fenêtre principale se ferme
            qrStage.getScene().getWindow().setOnHidden(e ->
                    tn.esprit.utils.LocalApiServer.getInstance().stop());

            qrStage.initModality(javafx.stage.Modality.NONE); // Non bloquant
            qrStage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la fenêtre QR Code", e.getMessage());
        }
    }
}