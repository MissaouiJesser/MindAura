package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tn.esprit.entities.Evenements;
import tn.esprit.services.EvenementService;

import java.io.File;
import java.io.FileInputStream;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

public class EventFormController implements DashboardController.DashboardAware {

    @FXML private Label formIcon;
    @FXML private Label formTitle;
    @FXML private Label formSubtitle;
    @FXML private Label formBreadcrumb;

    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker datedebutPicker;
    @FXML private DatePicker datefinPicker;
    @FXML private TextField lieuField;
    @FXML private TextField capaciteField;
    @FXML private ComboBox<String> statutCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private Button uploadImageButton;
    @FXML private Label imagePathLabel;
    @FXML private ImageView imagePreview;
    @FXML private StackPane imagePreviewPane;
    @FXML private Label imagePreviewPlaceholder;

    @FXML private Label dateFinError;
    @FXML private HBox errorBanner;
    @FXML private Label errorMessage;

    private String imagePath;
    private final EvenementService evenementService = new EvenementService();
    private Evenements eventToEdit;
    private DashboardController dashboardController;

    @FXML
    public void initialize() {
        statutCombo.getItems().addAll("Actif", "Inactif", "Complet", "Annulé", "Planifié");
        typeCombo.getItems().addAll("Conférence", "Séminaire", "Atelier", "Concert",
                "Exposition", "Sport", "Networking", "Formation", "Autre");
        setupDateFinConstraint();
        // Animations d'entrée en cascade
        javafx.application.Platform.runLater(this::playEntranceAnimations);
    }

    private void playEntranceAnimations() {
        try {
            // Animer les enfants directs de la carte formulaire (premier VBox enfant de la scène)
            javafx.scene.Parent root = titreField.getScene().getRoot();
            // Trouver la grande carte blanche (VBox avec effet)
            VBox formCard = findFormCard(root);
            if (formCard == null) return;
            int delay = 0;
            for (Node child : formCard.getChildrenUnmodifiable()) {
                animateIn(child, delay);
                delay += 60;
            }
        } catch (Exception ignored) {}
    }

    private VBox findFormCard(javafx.scene.Parent root) {
        // Cherche le premier VBox avec styleClass contenant "form-card" ou ayant un background blanc
        for (javafx.scene.Node node : root.getChildrenUnmodifiable()) {
            if (node instanceof VBox vbox && !vbox.getChildrenUnmodifiable().isEmpty()) {
                String style = vbox.getStyle();
                if (style != null && style.contains("#FFFFFF") && style.contains("background-radius")) {
                    return vbox;
                }
                if (node instanceof javafx.scene.Parent p) {
                    VBox found = findFormCard(p);
                    if (found != null) return found;
                }
            } else if (node instanceof javafx.scene.Parent p) {
                VBox found = findFormCard(p);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void animateIn(Node n, int delayMs) {
        n.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(380), n);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(380), n);
        tt.setDelay(Duration.millis(delayMs));
        tt.setFromY(14); tt.setToY(0);
        ft.play(); tt.play();
    }

    private void setupDateFinConstraint() {
        datefinPicker.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (datedebutPicker.getValue() != null && date != null
                        && !date.isAfter(datedebutPicker.getValue())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #111827; -fx-text-fill: #4B607A;");
                }
            }
        });
    }

    @Override
    public void setDashboardController(DashboardController c) {
        this.dashboardController = c;
    }

    public void setEvent(Evenements event) {
        setEventToEdit(event);
    }

    public void setEventToEdit(Evenements event) {
        this.eventToEdit = event;
        if (event != null) {
            formIcon.setText("✏");
            formTitle.setText("Modifier l'Événement");
            formSubtitle.setText("Mettre à jour les informations de l'événement");
            if (formBreadcrumb != null) formBreadcrumb.setText("Modification");

            titreField.setText(event.getTitre_evenement());
            descriptionField.setText(event.getDescription_evenement());
            if (event.getDatedebut_evenemnt() != null)
                datedebutPicker.setValue(toLocalDate(event.getDatedebut_evenemnt()));
            if (event.getDatefin_evenemnt() != null)
                datefinPicker.setValue(toLocalDate(event.getDatefin_evenemnt()));
            lieuField.setText(event.getLieu_evenement());
            capaciteField.setText(String.valueOf(event.getCapacite_evenement()));
            if (event.getStatut_evenemnt() != null)
                statutCombo.setValue(event.getStatut_evenemnt());
            if (event.getTypeEvenement() != null)
                typeCombo.setValue(event.getTypeEvenement());
            imagePath = event.getImage();
            if (imagePath != null) {
                imagePathLabel.setText(new File(imagePath).getName());
                File imgFile = new File(imagePath);
                if (imgFile.exists()) showImagePreview(imgFile);
            }
        }
    }

    @FXML
    private void onDateDebutChanged() {

        datefinPicker.setValue(null);
        setupDateFinConstraint();
    }

    @FXML
    private void uploadImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image de couverture");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );
        File file = fc.showOpenDialog(uploadImageButton.getScene().getWindow());
        if (file != null) {
            imagePath = file.getAbsolutePath();
            imagePathLabel.setText(file.getName());
            imagePathLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px;");
            showImagePreview(file);
        }
    }

    private void showImagePreview(File file) {
        if (imagePreview == null || imagePreviewPane == null) return;
        try {
            Image img = new Image(new FileInputStream(file), 0, 200, true, true);
            imagePreview.setImage(img);
            imagePreviewPane.setVisible(true);
            imagePreviewPane.setManaged(true);
            if (imagePreviewPlaceholder != null) {
                imagePreviewPlaceholder.setVisible(false);
                imagePreviewPlaceholder.setManaged(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void saveEvent() {
        if (!validate()) return;

        try {
            int capacite = Integer.parseInt(capaciteField.getText().trim());
            Date debut = Date.from(datedebutPicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date fin   = Date.from(datefinPicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant());

            if (eventToEdit != null) {

                eventToEdit.setTitre_evenement(titreField.getText().trim());
                eventToEdit.setDescription_evenement(descriptionField.getText().trim());
                eventToEdit.setDatedebut_evenemnt(debut);
                eventToEdit.setDatefin_evenemnt(fin);
                eventToEdit.setLieu_evenement(lieuField.getText().trim());
                eventToEdit.setCapacite_evenement(capacite);
                eventToEdit.setStatut_evenemnt(statutCombo.getValue());
                eventToEdit.setTypeEvenement(typeCombo.getValue());
                eventToEdit.setImage(imagePath);
                evenementService.modifierEvenement(eventToEdit);
                showSuccess("✅  Événement modifié avec succès !");
            } else {

                Evenements newEvent = new Evenements();
                newEvent.setIdentifiant_evenemnt(UUID.randomUUID().toString());
                newEvent.setTitre_evenement(titreField.getText().trim());
                newEvent.setDescription_evenement(descriptionField.getText().trim());
                newEvent.setDatedebut_evenemnt(debut);
                newEvent.setDatefin_evenemnt(fin);
                newEvent.setLieu_evenement(lieuField.getText().trim());
                newEvent.setCapacite_evenement(capacite);
                newEvent.setStatut_evenemnt(statutCombo.getValue());
                newEvent.setTypeEvenement(typeCombo.getValue());
                newEvent.setImage(imagePath);
                evenementService.ajouterEvenement(newEvent);
                showSuccess("✅  Événement créé avec succès !");
            }

            if (dashboardController != null)
                dashboardController.navigateTo("AdminEventsView.fxml", "Gestion des Événements", "Liste et gestion des événements");

        } catch (NumberFormatException ex) {
            showError("La capacité doit être un nombre entier valide.");
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Erreur : " + ex.getMessage());
        }
    }

    private boolean validate() {
        hideError();


        if (titreField.getText().trim().isEmpty()) {
            showError("Le titre de l'événement est obligatoire.");
            markError(titreField);
            return false;
        }

        if (descriptionField.getText().trim().isEmpty()) {
            showError("La description est obligatoire.");
            return false;
        }

        if (datedebutPicker.getValue() == null) {
            showError("La date de début est obligatoire.");
            return false;
        }

        if (datefinPicker.getValue() == null) {
            showError("La date de fin est obligatoire.");
            return false;
        }

        if (!datefinPicker.getValue().isAfter(datedebutPicker.getValue())) {
            showError("La date de fin doit être strictement après la date de début.");
            dateFinError.setVisible(true);
            dateFinError.setManaged(true);
            return false;
        }

        if (lieuField.getText().trim().isEmpty()) {
            showError("Le lieu est obligatoire.");
            return false;
        }

        if (capaciteField.getText().trim().isEmpty()) {
            showError("La capacité est obligatoire.");
            return false;
        }
        try {
            int cap = Integer.parseInt(capaciteField.getText().trim());
            if (cap <= 0) {
                showError("La capacité doit être supérieure à 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("La capacité doit être un nombre entier valide.");
            return false;
        }
        return true;
    }

    private void markError(TextField field) {
        if (!field.getStyleClass().contains("form-field-error"))
            field.getStyleClass().add("form-field-error");
        field.textProperty().addListener((obs, o, n) -> field.getStyleClass().remove("form-field-error"));
    }

    private void showError(String msg) {
        errorMessage.setText(msg);
        errorBanner.setVisible(true);
        errorBanner.setManaged(true);
    }

    private void hideError() {
        errorBanner.setVisible(false);
        errorBanner.setManaged(false);
        dateFinError.setVisible(false);
        dateFinError.setManaged(false);
        titreField.getStyleClass().remove("form-field-error");
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color: #141E2E; -fx-font-family: 'Segoe UI';" +
                "-fx-border-color: #1E2D45; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;");
        try {
            dp.lookup(".content.label").setStyle(
                    "-fx-text-fill: #34D399; -fx-font-size: 14px; -fx-font-weight: 600;");
        } catch (Exception ignored) {}
        alert.showAndWait();
    }

    @FXML
    private void cancel() {
        if (dashboardController != null)
            dashboardController.navigateTo("AdminEventsView.fxml", "Gestion des Événements", "Liste et gestion des événements");
    }

    private LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        if (date instanceof java.sql.Date) return ((java.sql.Date) date).toLocalDate();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}