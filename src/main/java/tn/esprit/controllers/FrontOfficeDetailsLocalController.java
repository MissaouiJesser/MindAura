package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_local;
import tn.esprit.enums.MotifReservation;
import tn.esprit.enums.StatutReservation;
import tn.esprit.services.EmailService;
import tn.esprit.services.LocalCapacityService;
import tn.esprit.services.PriceCalculatorService;
import tn.esprit.services.ReservationValidationService;
import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.utils.ImageManager;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * Contrôleur pour afficher les détails d'un local et permettre la réservation
 * VERSION FRONT OFFICE - Interface simplifiée pour les utilisateurs
 */
public class FrontOfficeDetailsLocalController implements Initializable {

    @FXML private ImageView imgLocal;
    @FXML private Label lblNomLocal;
    @FXML private Label lblType;
    @FXML private Label lblVille;
    @FXML private Label lblAdresse;
    @FXML private Label lblCapacite;
    @FXML private Label lblTelephone;
    @FXML private Label lblEmail;
    @FXML private Label lblDescription;
    @FXML private Label lblDisponibilite;
    @FXML private TextField txtNomClient;
    @FXML private TextField txtPrenomClient;
    @FXML private DatePicker dateReservation;
    @FXML private Spinner<Integer> spinnerHeureDebut;
    @FXML private Spinner<Integer> spinnerMinuteDebut;
    @FXML private Spinner<Integer> spinnerHeureFin;
    @FXML private Spinner<Integer> spinnerMinuteFin;
    @FXML private ComboBox<MotifReservation> comboMotif;
    @FXML private Label lblPrixCalcule;
    @FXML private Label lblMessage;
    @FXML private Button btnReserver;

    private local_psychiatrie currentLocal;
    private reservation_local_SERVICE reservationService;
    private ReservationValidationService validationService;
    private LocalCapacityService capacityService;
    private FrontOfficeAccueilController parentController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reservationService = new reservation_local_SERVICE();
        validationService  = new ReservationValidationService();
        capacityService    = new LocalCapacityService();

        initializeFormControls();
        setupPriceCalculation();
        applyFadeInAnimation();
    }

    private void initializeFormControls() {
        comboMotif.setItems(FXCollections.observableArrayList(MotifReservation.values()));
        comboMotif.setValue(MotifReservation.CONSULTATION_INDIVIDUELLE);

        dateReservation.setValue(LocalDate.now().plusDays(1));
        dateReservation.setDayCellFactory(picker -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        spinnerHeureDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9));
        spinnerMinuteDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));
        spinnerHeureFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 11));
        spinnerMinuteFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));

        spinnerHeureDebut.setEditable(true);
        spinnerMinuteDebut.setEditable(true);
        spinnerHeureFin.setEditable(true);
        spinnerMinuteFin.setEditable(true);
    }

    private void setupPriceCalculation() {
        spinnerHeureDebut.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        spinnerMinuteDebut.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        spinnerHeureFin.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
        spinnerMinuteFin.valueProperty().addListener((obs, oldVal, newVal) -> calculerPrix());
    }

    public void setLocal(local_psychiatrie local) {
        this.currentLocal = local;

        lblNomLocal.setText(local.getNom_local());
        lblType.setText(local.getType_local().getLibelle());
        lblVille.setText(local.getVille_local());
        lblAdresse.setText(local.getAdresse_local());
        lblCapacite.setText(local.getCapacite_local() + " personnes");
        lblTelephone.setText(formatPhoneNumber(String.valueOf(local.getTelephone_local())));
        lblEmail.setText(local.getEmail_local());
        lblDescription.setText(local.getDescription_local());

        setupDisponibiliteBadge(local);
        loadImage(local.getImageURL());

        if (!"Disponible".equals(local.getDisponibilite_local())) {
            btnReserver.setDisable(true);
            btnReserver.setText("❌ Local non disponible");
            showMessage("⚠️ Ce local n'est pas disponible actuellement", "warning");
        }

        calculerPrix();
        prefillFromSession();
    }

    /**
     * Pré-remplit nom et prénom depuis la session et les verrouille en lecture seule.
     * Appelé dans setLocal() pour garantir l'exécution après le rendu des champs.
     */
    private void prefillFromSession() {
        if (SessionManager.isLoggedIn()) {
            tn.esprit.entities.utilisateurs user = SessionManager.getCurrentUser();
            txtNomClient.setText(user.getNom_utilisateur());
            txtPrenomClient.setText(user.getPrenom_utilisateur());
            txtNomClient.setEditable(false);
            txtPrenomClient.setEditable(false);
            txtNomClient.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #4B5563; " +
                            "-fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px;");
            txtPrenomClient.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #4B5563; " +
                            "-fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px;");
        }
    }

    private void setupDisponibiliteBadge(local_psychiatrie local) {
        lblDisponibilite.getStyleClass().clear();
        lblDisponibilite.getStyleClass().add("badge");

        if ("Disponible".equals(local.getDisponibilite_local())) {
            lblDisponibilite.getStyleClass().add("badge-disponible");
            lblDisponibilite.setText("✓ Disponible");
        } else if ("Non disponible".equals(local.getDisponibilite_local())) {
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
                Image image = ImageManager.loadImage(imageURL, 350, 250, true);
                if (image != null) {
                    imgLocal.setImage(image);
                    return;
                }
            }
        } catch (Exception ignored) {}
        setPlaceholder();
    }

    private void setPlaceholder() {
        imgLocal.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #D8F3DC, #F0F4F8); " +
                        "-fx-background-radius: 12px; -fx-border-color: #2D6A4F; " +
                        "-fx-border-width: 2px; -fx-border-radius: 12px; -fx-border-style: dashed;"
        );
    }

    private void calculerPrix() {
        try {
            // ✅ CORRIGÉ : vérifier que la date n'est pas null avant d'appeler createDateTime
            if (dateReservation.getValue() == null) return;
            Date heureDebut = createDateTime(dateReservation.getValue(),
                    spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
            Date heureFin = createDateTime(dateReservation.getValue(),
                    spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
            int prix = PriceCalculatorService.calculerPrix(heureDebut, heureFin);
            lblPrixCalcule.setText(prix + " DT");
        } catch (Exception e) {
            lblPrixCalcule.setText("0 DT");
        }
    }

    @FXML
    private void handleReserver() {
        if (validateForm()) {
            try {
                Date heureDebut = createDateTime(dateReservation.getValue(),
                        spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
                Date heureFin = createDateTime(dateReservation.getValue(),
                        spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
                Date dateRes = Date.from(dateReservation.getValue()
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());

                if (validationService.hasConflict(currentLocal.getId_local(), dateRes,
                        heureDebut, heureFin, null)) {
                    showMessage(validationService.getConflictMessage(
                            currentLocal.getId_local(), heureDebut, heureFin), "error");
                    return;
                }

                int prix = PriceCalculatorService.calculerPrix(heureDebut, heureFin);

                reservation_local reservation = new reservation_local();
                reservation.setId_local(currentLocal.getId_local());
                reservation.setDate_reservation(dateRes);
                reservation.setHeure_debut_reservation(heureDebut);
                reservation.setHeure_fin_reservation(heureFin);
                reservation.setStatus_reservation(StatutReservation.CONFIRMEE);
                reservation.setMotif_reservation(comboMotif.getValue());
                reservation.setPrix_reservation(prix);
                reservation.setNom_cl(txtNomClient.getText().trim());
                reservation.setPrenom_cl(txtPrenomClient.getText().trim());
                // ID utilisateur depuis la session
                if (SessionManager.isLoggedIn()) {
                    try {
                        reservation.setId_utilisateur(
                                Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur()));
                    } catch (NumberFormatException ex) {
                        reservation.setId_utilisateur(1);
                    }
                } else {
                    reservation.setId_utilisateur(1);
                }

                reservationService.add(reservation);
                capacityService.onReservationAdded(currentLocal.getId_local());

                // ✅ Envoi de l'email de confirmation depuis la session
                if (SessionManager.isLoggedIn()) {
                    tn.esprit.entities.utilisateurs user = SessionManager.getCurrentUser();
                    String emailUser = user.getEmail_utilisateur();
                    String nomComplet = user.getPrenom_utilisateur() + " " + user.getNom_utilisateur();
                    LocalDateTime dateSeance = dateReservation.getValue()
                            .atTime(spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
                    String lieu = currentLocal.getNom_local() + " - " + currentLocal.getAdresse_local()
                            + ", " + currentLocal.getVille_local();

                    new Thread(() -> {
                        EmailService.EmailResult result = EmailService.envoyerConfirmation(
                                emailUser,
                                nomComplet,
                                currentLocal.getNom_local(),
                                dateSeance,
                                comboMotif.getValue().getLibelle(),
                                lieu
                        );
                        System.out.println("[Email] " + result);
                    }).start();
                }

                showMessage("✅ Réservation confirmée ! Un email de confirmation vous a été envoyé.", "success");

                if (parentController != null) parentController.refreshDisplay();

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(this::redirectionnerVersMesReservations);
                    } catch (InterruptedException ignored) {}
                }).start();

            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("❌ Erreur lors de la réservation: " + e.getMessage(), "error");
            }
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNomClient.getText().trim().isEmpty())
            errors.append("- Le nom est obligatoire\n");

        if (txtPrenomClient.getText().trim().isEmpty())
            errors.append("- Le prénom est obligatoire\n");

        // ✅ CORRIGÉ : sortie anticipée si date null pour éviter NullPointerException
        if (dateReservation.getValue() == null) {
            errors.append("- La date est obligatoire\n");
            showMessage(errors.toString(), "error");
            return false;
        }

        Date heureDebut = createDateTime(dateReservation.getValue(),
                spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
        Date heureFin = createDateTime(dateReservation.getValue(),
                spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());

        if (!heureFin.after(heureDebut))
            errors.append("- L'heure de fin doit être après l'heure de début\n");

        if (errors.length() > 0) {
            showMessage(errors.toString(), "error");
            return false;
        }

        return true;
    }

    private Date createDateTime(LocalDate date, int heure, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR,         date.getYear());
        calendar.set(Calendar.MONTH,        date.getMonthValue() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        calendar.set(Calendar.HOUR_OF_DAY,  heure);
        calendar.set(Calendar.MINUTE,       minute);
        calendar.set(Calendar.SECOND,       0);
        calendar.set(Calendar.MILLISECOND,  0);
        return calendar.getTime();
    }

    private String formatPhoneNumber(String phone) {
        if (phone != null && phone.length() == 8)
            return phone.substring(0, 2) + " " + phone.substring(2, 5) + " " + phone.substring(5);
        return phone != null ? phone : "Non spécifié";
    }

    private void showMessage(String message, String type) {
        lblMessage.setText(message);

        if ("success".equals(type)) {
            lblMessage.setStyle(
                    "-fx-text-fill: #1B4332; -fx-font-weight: bold; -fx-background-color: #D8F3DC; " +
                            "-fx-padding: 12px 20px; -fx-background-radius: 10px; " +
                            "-fx-border-color: #2D6A4F; -fx-border-width: 1px; -fx-border-radius: 10px;"
            );
        } else if ("warning".equals(type)) {
            lblMessage.setStyle(
                    "-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #FFF4E6; " +
                            "-fx-padding: 12px 20px; -fx-background-radius: 10px; " +
                            "-fx-border-color: #B794F4; -fx-border-width: 1px; -fx-border-radius: 10px;"
            );
        } else {
            lblMessage.setStyle(
                    "-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #F3E8FF; " +
                            "-fx-padding: 12px 20px; -fx-background-radius: 10px; " +
                            "-fx-border-color: #9B7DC4; -fx-border-width: 1px; -fx-border-radius: 10px;"
            );
        }

        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void redirectionnerVersMesReservations() {
        try {
            if (parentController != null && parentController.getParentHomeController() != null) {
                parentController.getParentHomeController().loadMesReservations();
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FrontOfficeMesReservations.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) btnReserver.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL css = getClass().getResource("/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("MindAura – Mes Réservations");
            stage.setWidth(1400);
            stage.setHeight(850);
            stage.centerOnScreen();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFermer() {
        if (parentController != null && parentController.getParentHomeController() != null) {
            parentController.getParentHomeController().loadFrontOfficeAccueil();
        } else if (parentController != null) {
            parentController.handleRetourListe();
        }
    }

    public void setParentController(FrontOfficeAccueilController controller) {
        this.parentController = controller;
    }

    private void applyFadeInAnimation() {
        if (lblNomLocal != null && lblNomLocal.getParent() != null &&
                lblNomLocal.getParent().getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(600),
                    lblNomLocal.getParent().getParent());
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }
}