package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_local;
import tn.esprit.entities.salle;
import tn.esprit.enums.StatutReservation;
import tn.esprit.enums.MotifReservation;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.services.salle_SERVICE;
import tn.esprit.services.PriceCalculatorService;
import tn.esprit.services.ReservationValidationService;
import tn.esprit.services.LocalCapacityService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.EmailService;
import tn.esprit.utils.LocalComboItem;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller pour l'ajout d'une nouvelle réservation
 * ✅ AVEC GESTION DE LA CAPACITÉ DU LOCAL
 */
public class AjouterReservationController implements Initializable {

    @FXML
    private TextField txtNomClient;

    @FXML
    private TextField txtPrenomClient;

    @FXML
    private ComboBox<LocalComboItem> comboLocal;

    @FXML
    private ComboBox<SalleComboItem> comboSalle;

    @FXML
    private DatePicker dateReservation;

    @FXML
    private Spinner<Integer> spinnerHeureDebut;
    @FXML
    private Spinner<Integer> spinnerMinuteDebut;
    @FXML
    private Spinner<Integer> spinnerHeureFin;
    @FXML
    private Spinner<Integer> spinnerMinuteFin;

    @FXML
    private ComboBox<MotifReservation> comboMotif;

    @FXML
    private ComboBox<StatutReservation> comboStatut;

    @FXML
    private Label lblPrixCalcule;

    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnAnnuler;

    @FXML
    private Label lblMessage;

    private reservation_local_SERVICE reservationService;
    private local_psychiatrie_SERVICE localService;
    private salle_SERVICE salleService;
    private ReservationValidationService validationService;
    private LocalCapacityService capacityService; // ✅ NOUVEAU
    private AfficherReservationController parentController;

    private static final int ID_UTILISATEUR_PAR_DEFAUT = 1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reservationService = new reservation_local_SERVICE();
        localService = new local_psychiatrie_SERVICE();
        salleService = new salle_SERVICE();
        validationService = new ReservationValidationService();
        capacityService = new LocalCapacityService();

        chargerLocauxDisponibles();
        chargerSalles();

        comboMotif.setItems(FXCollections.observableArrayList(MotifReservation.values()));
        comboMotif.setValue(MotifReservation.CONSULTATION_INDIVIDUELLE);

        comboStatut.setItems(FXCollections.observableArrayList(StatutReservation.values()));
        comboStatut.setValue(StatutReservation.EN_ATTENTE);

        dateReservation.setValue(LocalDate.now().plusDays(1)); // Date par défaut: demain

        setupDatePicker(); // Configurer le DatePicker pour bloquer les dates passées

        setupTimeSpinners();
        applyCSSStyles();
        applyFadeInAnimation();
        setupPriceCalculation();
    }

    private void chargerLocauxDisponibles() {
        try {
            List<local_psychiatrie> locaux = localService.afficherList();
            ObservableList<LocalComboItem> locauxDisponibles = FXCollections.observableArrayList();

            for (local_psychiatrie local : locaux) {
                if ("Disponible".equalsIgnoreCase(local.getDisponibilite_local())) {
                    locauxDisponibles.add(new LocalComboItem(local));
                }
            }

            comboLocal.setItems(locauxDisponibles);

            if (locauxDisponibles.isEmpty()) {
                showMessage("⚠️ Aucun local disponible actuellement", "warning");
            } else {
                System.out.println("✅ " + locauxDisponibles.size() + " locaux disponibles chargés");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("❌ Erreur lors du chargement des locaux", "error");
        }
    }

    private void chargerSalles() {
        try {
            ObservableList<SalleComboItem> items = FXCollections.observableArrayList();
            items.add(new SalleComboItem(null)); // option "Aucune salle"
            for (salle s : salleService.afficherList()) {
                items.add(new SalleComboItem(s));
            }
            comboSalle.setItems(items);
            comboSalle.setValue(items.get(0)); // sélection par défaut : aucune
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("❌ Erreur lors du chargement des salles", "error");
        }
    }

    /** Item affiché dans le ComboBox des salles. */
    public static class SalleComboItem {
        private final salle salle;
        public SalleComboItem(salle s) { this.salle = s; }
        public salle getSalle() { return salle; }
        public int getIdSalle() { return salle != null ? salle.getId_salle() : 0; }
        @Override public String toString() {
            return salle == null ? "— Aucune salle —" : salle.getNom_salle();
        }
    }

    private void setupPriceCalculation() {
        spinnerHeureDebut.valueProperty().addListener((obs, oldVal, newVal) -> calculerEtAfficherPrix());
        spinnerMinuteDebut.valueProperty().addListener((obs, oldVal, newVal) -> calculerEtAfficherPrix());
        spinnerHeureFin.valueProperty().addListener((obs, oldVal, newVal) -> calculerEtAfficherPrix());
        spinnerMinuteFin.valueProperty().addListener((obs, oldVal, newVal) -> calculerEtAfficherPrix());
        dateReservation.valueProperty().addListener((obs, oldVal, newVal) -> calculerEtAfficherPrix());
    }

    private void calculerEtAfficherPrix() {
        try {
            LocalDate dateRes = dateReservation.getValue();
            if (dateRes == null) {
                lblPrixCalcule.setText("0 DT");
                return;
            }

            LocalTime timeDebut = LocalTime.of(
                    spinnerHeureDebut.getValue(),
                    spinnerMinuteDebut.getValue()
            );
            LocalTime timeFin = LocalTime.of(
                    spinnerHeureFin.getValue(),
                    spinnerMinuteFin.getValue()
            );

            Date heureDebut = Date.from(dateRes.atTime(timeDebut)
                    .atZone(ZoneId.systemDefault()).toInstant());
            Date heureFin = Date.from(dateRes.atTime(timeFin)
                    .atZone(ZoneId.systemDefault()).toInstant());

            int prix = PriceCalculatorService.calculerPrix(heureDebut, heureFin);
            String detail = PriceCalculatorService.getDetailPrix(heureDebut, heureFin);

            lblPrixCalcule.setText(prix + " DT");

            Tooltip tooltip = new Tooltip(detail);
            tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: #D8F3DC; " +
                    "-fx-text-fill: #1B4332; -fx-border-color: #2D6A4F; " +
                    "-fx-border-width: 1px;");
            lblPrixCalcule.setTooltip(tooltip);

        } catch (Exception e) {
            lblPrixCalcule.setText("0 DT");
        }
    }

    /**
     * Configure le DatePicker pour bloquer les dates passées et aujourd'hui
     * Seules les dates futures sont sélectionnables
     */
    private void setupDatePicker() {
        // Désactiver les dates passées et aujourd'hui
        dateReservation.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                // Désactiver toutes les dates <= aujourd'hui
                if (date != null && !date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #F3E8FF; -fx-text-fill: #9B7DC4;");
                }
            }
        });
    }

    private void setupTimeSpinners() {
        SpinnerValueFactory<Integer> heureDebutFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9);
        spinnerHeureDebut.setValueFactory(heureDebutFactory);
        spinnerHeureDebut.setEditable(true);

        SpinnerValueFactory<Integer> minuteDebutFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);
        spinnerMinuteDebut.setValueFactory(minuteDebutFactory);
        spinnerMinuteDebut.setEditable(true);

        SpinnerValueFactory<Integer> heureFinFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 10);
        spinnerHeureFin.setValueFactory(heureFinFactory);
        spinnerHeureFin.setEditable(true);

        SpinnerValueFactory<Integer> minuteFinFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15);
        spinnerMinuteFin.setValueFactory(minuteFinFactory);
        spinnerMinuteFin.setEditable(true);
    }

    private void applyCSSStyles() {
        btnAjouter.getStyleClass().add("btn-primary");
        btnAnnuler.getStyleClass().add("btn-annuler");
    }

    @FXML
    private void handleAjouter() {
        if (validateForm()) {
            try {
                LocalComboItem selectedItem = comboLocal.getValue();
                if (selectedItem == null) {
                    showMessage("❌ Veuillez sélectionner un local", "error");
                    return;
                }

                local_psychiatrie localSelectionne = selectedItem.getLocal();

                LocalDate dateRes = dateReservation.getValue();
                LocalTime timeDebut = LocalTime.of(
                        spinnerHeureDebut.getValue(),
                        spinnerMinuteDebut.getValue()
                );
                LocalTime timeFin = LocalTime.of(
                        spinnerHeureFin.getValue(),
                        spinnerMinuteFin.getValue()
                );

                Date dateReservationFinal = Date.from(dateRes.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date heureDebut = Date.from(dateRes.atTime(timeDebut).atZone(ZoneId.systemDefault()).toInstant());
                Date heureFin = Date.from(dateRes.atTime(timeFin).atZone(ZoneId.systemDefault()).toInstant());

                if (!heureFin.after(heureDebut)) {
                    showMessage("❌ L'heure de fin doit être après l'heure de début", "error");
                    return;
                }

                if (validationService.hasConflict(
                        localSelectionne.getId_local(),
                        dateReservationFinal,
                        heureDebut,
                        heureFin,
                        null)) {
                    showMessage(validationService.getConflictMessage(
                            localSelectionne.getId_local(),
                            heureDebut,
                            heureFin), "error");
                    return;
                }

                int prixCalcule = PriceCalculatorService.calculerPrix(heureDebut, heureFin);

                int salleId = (comboSalle.getValue() != null) ? comboSalle.getValue().getIdSalle() : 0;

                reservation_local reservation = new reservation_local(
                        0,
                        ID_UTILISATEUR_PAR_DEFAUT,
                        localSelectionne.getId_local(),
                        salleId,
                        dateReservationFinal,
                        heureDebut,
                        heureFin,
                        comboStatut.getValue(),
                        comboMotif.getValue(),
                        prixCalcule,
                        txtNomClient.getText().trim(),
                        txtPrenomClient.getText().trim()
                );

                // Ajouter la réservation
                reservationService.add(reservation);

                // ✅ NOUVEAU : Mettre à jour la capacité et la disponibilité du local
                capacityService.onReservationAdded(localSelectionne.getId_local());

                // 🔔 Notification nouvelle réservation
                NotificationService.getInstance().notifierNouvelleReservation(reservation);

                // 📧 Email de confirmation automatique
                final String nomComplet   = txtNomClient.getText().trim() + " " + txtPrenomClient.getText().trim();
                final String nomLocal     = localSelectionne.getNom_local();
                final int    prixFinal    = prixCalcule;
                final java.time.LocalDateTime dateHeure = dateRes.atTime(timeDebut);
                final java.time.LocalDateTime dateFin   = dateRes.atTime(timeFin);
                new Thread(() -> {
                    EmailService.EmailResult emailResult = EmailService.envoyerConfirmation(
                            "chaffaiichrak298@gmail.com",
                            nomComplet,
                            "MindAura",
                            dateHeure,
                            comboMotif.getValue().toString() + " - " + nomLocal,
                            nomLocal + " | " + dateFin.toLocalTime().toString() + " | " + prixFinal + " DT"
                    );
                    System.out.println("📧 Email confirmation: " + emailResult);
                }).start();

                System.out.println("✅ Réservation ajoutée avec succès!");
                System.out.println("   Local: " + localSelectionne.getNom_local());
                System.out.println("   Prix: " + prixCalcule + " DT");
                System.out.println("   ✅ Capacité du local diminuée et disponibilité mise à jour");

                showMessage("✅ Réservation ajoutée avec succès!\n" +
                        "Local: " + localSelectionne.getNom_local() + "\n" +
                        "💰 Prix: " + prixCalcule + " DT\n" +
                        "📊 Capacité du local mise à jour", "success");

                if (parentController != null) {
                    parentController.refreshTable();
                }

                new Thread(() -> {
                    try {
                        Thread.sleep(2000);
                        javafx.application.Platform.runLater(() -> handleAnnuler());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();

            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("❌ Erreur lors de l'ajout: " + e.getMessage(), "error");
            } catch (Exception e) {
                e.printStackTrace();
                showMessage("❌ Erreur inattendue: " + e.getMessage(), "error");
            }
        }
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNomClient.getText().trim().isEmpty()) {
            errors.append("- Le nom du client est obligatoire\n");
            txtNomClient.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else {
            txtNomClient.setStyle("");
        }

        if (txtPrenomClient.getText().trim().isEmpty()) {
            errors.append("- Le prénom du client est obligatoire\n");
            txtPrenomClient.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else {
            txtPrenomClient.setStyle("");
        }

        if (comboLocal.getValue() == null) {
            errors.append("- Veuillez sélectionner un local\n");
            comboLocal.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else {
            comboLocal.setStyle("");
        }

        if (dateReservation.getValue() == null) {
            errors.append("- La date de réservation est obligatoire\n");
            dateReservation.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else if (!dateReservation.getValue().isAfter(LocalDate.now())) {
            errors.append("- La date de réservation doit être après aujourd'hui\n");
            dateReservation.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else {
            dateReservation.setStyle("");
        }

        if (errors.length() > 0) {
            showMessage("❌ Erreurs de validation:\n" + errors.toString(), "error");
            return false;
        }

        return true;
    }

    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String message, String type) {
        lblMessage.setText(message);

        if (type.equals("success")) {
            lblMessage.setStyle("-fx-text-fill: #1B4332; -fx-font-weight: bold; -fx-background-color: #D8F3DC; " +
                    "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #2D6A4F; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;");
        } else if (type.equals("warning")) {
            lblMessage.setStyle("-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #FFF4E6; " +
                    "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #B794F4; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;");
        } else {
            lblMessage.setStyle("-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #F3E8FF; " +
                    "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #9B7DC4; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;");
        }

        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void applyFadeInAnimation() {
        if (txtNomClient != null && txtNomClient.getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), txtNomClient.getParent());
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    public void setParentController(AfficherReservationController controller) {
        this.parentController = controller;
    }

    /**
     * Pré-sélectionne un local dans le ComboBox.
     * Appelée depuis la Carte Géographique et les Recommandations.
     */
    public void setLocalPreselectionne(local_psychiatrie local) {
        if (local == null) return;
        // Chercher le LocalComboItem correspondant et le sélectionner
        comboLocal.getItems().stream()
                .filter(item -> item.getIdLocal() == local.getId_local())
                .findFirst()
                .ifPresent(item -> comboLocal.setValue(item));
    }

    /**
     * Pré-remplit la date de réservation.
     * Appelée depuis le Calendrier Interactif.
     */
    public void setDatePreselectionne(LocalDate date) {
        if (date != null && date.isAfter(LocalDate.now())) {
            dateReservation.setValue(date);
        }
    }
}