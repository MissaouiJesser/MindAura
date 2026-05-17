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
import tn.esprit.entities.reservation_historique;
import tn.esprit.enums.StatutReservation;
import tn.esprit.enums.MotifReservation;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.services.ReservationHistoriqueService;
import tn.esprit.services.PriceCalculatorService;
import tn.esprit.services.ReservationValidationService;
import tn.esprit.utils.LocalComboItem;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller pour la modification d'une réservation existante.
 *
 * ✅ NOUVEAU : Enregistre automatiquement l'historique complet de chaque
 *             modification (qui a changé quoi et quand) dans la table
 *             reservation_historique.
 */
public class ModifierReservationController implements Initializable {

    @FXML private TextField txtNomClient;
    @FXML private TextField txtPrenomClient;
    @FXML private ComboBox<LocalComboItem> comboLocal;
    @FXML private DatePicker dateReservation;
    @FXML private Spinner<Integer> spinnerHeureDebut;
    @FXML private Spinner<Integer> spinnerMinuteDebut;
    @FXML private Spinner<Integer> spinnerHeureFin;
    @FXML private Spinner<Integer> spinnerMinuteFin;
    @FXML private ComboBox<MotifReservation> comboMotif;
    @FXML private ComboBox<StatutReservation> comboStatut;
    @FXML private Label lblPrixCalcule;
    @FXML private Button btnModifier;
    @FXML private Button btnAnnuler;
    @FXML private Label lblMessage;
    @FXML private Label lblIdReservation;

    private reservation_local_SERVICE reservationService;
    private local_psychiatrie_SERVICE localService;
    private ReservationValidationService validationService;
    private ReservationHistoriqueService historiqueService;   // ✅ NOUVEAU
    private AfficherReservationController parentController;
    private reservation_local currentReservation;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        reservationService = new reservation_local_SERVICE();
        localService       = new local_psychiatrie_SERVICE();
        validationService  = new ReservationValidationService();
        historiqueService  = new ReservationHistoriqueService();   // ✅ NOUVEAU

        chargerLocaux();
        comboMotif.setItems(FXCollections.observableArrayList(MotifReservation.values()));
        comboStatut.setItems(FXCollections.observableArrayList(StatutReservation.values()));
        setupTimeSpinners();
        setupPriceCalculation();
        applyFadeInAnimation();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT DES LOCAUX
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerLocaux() {
        try {
            List<local_psychiatrie> locaux = localService.afficherList();
            ObservableList<LocalComboItem> items = FXCollections.observableArrayList();
            for (local_psychiatrie l : locaux) items.add(new LocalComboItem(l));
            comboLocal.setItems(items);
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("❌ Erreur lors du chargement des locaux", "error");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  REMPLISSAGE DU FORMULAIRE (appelé depuis AfficherReservationController)
    // ─────────────────────────────────────────────────────────────────────────

    public void setReservation(reservation_local reservation) {
        this.currentReservation = reservation;
        if (lblIdReservation != null)
            lblIdReservation.setText("Réservation #" + reservation.getId_reservation());

        txtNomClient.setText(reservation.getNom_cl());
        txtPrenomClient.setText(reservation.getPrenom_cl());

        // Sélectionner le bon local dans la combobox
        comboLocal.getItems().stream()
                .filter(item -> item.getIdLocal() == reservation.getId_local())
                .findFirst()
                .ifPresent(item -> comboLocal.setValue(item));

        // Remplir la date
        // ✅ FIX : java.sql.Date.toInstant() lève UnsupportedOperationException
        //         → on passe par Calendar pour éviter ce bug JDBC
        if (reservation.getDate_reservation() != null) {
            java.util.Calendar calDate = java.util.Calendar.getInstance();
            calDate.setTime(reservation.getDate_reservation());
            dateReservation.setValue(LocalDate.of(
                    calDate.get(java.util.Calendar.YEAR),
                    calDate.get(java.util.Calendar.MONTH) + 1,
                    calDate.get(java.util.Calendar.DAY_OF_MONTH)
            ));
        }

        // Remplir les heures
        if (reservation.getHeure_debut_reservation() != null) {
            java.util.Calendar calDebut = java.util.Calendar.getInstance();
            calDebut.setTime(reservation.getHeure_debut_reservation());
            spinnerHeureDebut.getValueFactory().setValue(calDebut.get(java.util.Calendar.HOUR_OF_DAY));
            spinnerMinuteDebut.getValueFactory().setValue(calDebut.get(java.util.Calendar.MINUTE));
        }

        if (reservation.getHeure_fin_reservation() != null) {
            java.util.Calendar calFin = java.util.Calendar.getInstance();
            calFin.setTime(reservation.getHeure_fin_reservation());
            spinnerHeureFin.getValueFactory().setValue(calFin.get(java.util.Calendar.HOUR_OF_DAY));
            spinnerMinuteFin.getValueFactory().setValue(calFin.get(java.util.Calendar.MINUTE));
        }

        comboMotif.setValue(reservation.getMotif_reservation());
        comboStatut.setValue(reservation.getStatus_reservation());
        calculerEtAfficherPrix();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIX
    // ─────────────────────────────────────────────────────────────────────────

    private void setupPriceCalculation() {
        spinnerHeureDebut.valueProperty().addListener((obs, o, n)  -> calculerEtAfficherPrix());
        spinnerMinuteDebut.valueProperty().addListener((obs, o, n) -> calculerEtAfficherPrix());
        spinnerHeureFin.valueProperty().addListener((obs, o, n)    -> calculerEtAfficherPrix());
        spinnerMinuteFin.valueProperty().addListener((obs, o, n)   -> calculerEtAfficherPrix());
        dateReservation.valueProperty().addListener((obs, o, n)    -> calculerEtAfficherPrix());
    }

    private void calculerEtAfficherPrix() {
        try {
            LocalDate date = dateReservation.getValue();
            if (date == null) { lblPrixCalcule.setText("0 DT"); return; }
            LocalTime td = LocalTime.of(spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
            LocalTime tf = LocalTime.of(spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
            Date hd = Date.from(date.atTime(td).atZone(ZoneId.systemDefault()).toInstant());
            Date hf = Date.from(date.atTime(tf).atZone(ZoneId.systemDefault()).toInstant());
            int prix = PriceCalculatorService.calculerPrix(hd, hf);
            lblPrixCalcule.setText(prix + " DT");
        } catch (Exception e) {
            lblPrixCalcule.setText("0 DT");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SPINNERS
    // ─────────────────────────────────────────────────────────────────────────

    private void setupTimeSpinners() {
        spinnerHeureDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9));
        spinnerHeureDebut.setEditable(true);
        spinnerMinuteDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));
        spinnerMinuteDebut.setEditable(true);
        spinnerHeureFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 22, 10));
        spinnerHeureFin.setEditable(true);
        spinnerMinuteFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));
        spinnerMinuteFin.setEditable(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HANDLER PRINCIPAL : MODIFIER + ENREGISTRER HISTORIQUE
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleModifier() {
        if (!validateForm()) return;

        try {
            // ── Snapshots AVANT modification ──────────────────────────────
            String ancNom      = currentReservation.getNom_cl();
            String ancPrenom   = currentReservation.getPrenom_cl();
            int    ancLocalId  = currentReservation.getId_local();
            String ancDate     = currentReservation.getDate_reservation() != null
                    ? DATE_FMT.format(currentReservation.getDate_reservation()) : "";
            String ancDebut    = currentReservation.getHeure_debut_reservation() != null
                    ? TIME_FMT.format(currentReservation.getHeure_debut_reservation()) : "";
            String ancFin      = currentReservation.getHeure_fin_reservation() != null
                    ? TIME_FMT.format(currentReservation.getHeure_fin_reservation()) : "";
            String ancStatut   = currentReservation.getStatus_reservation().getLibelle();
            String ancMotif    = currentReservation.getMotif_reservation().getLibelle();
            int    ancPrix     = currentReservation.getPrix_reservation();

            // Récupérer l'ancien local pour gérer la disponibilité
            local_psychiatrie ancienLocal = localService.afficherList().stream()
                    .filter(l -> l.getId_local() == ancLocalId)
                    .findFirst().orElse(null);

            // ── Calcul des nouvelles valeurs ──────────────────────────────
            LocalComboItem localItem = comboLocal.getValue();
            local_psychiatrie localSelectionne = localService.afficherList().stream()
                    .filter(l -> l.getId_local() == localItem.getIdLocal())
                    .findFirst().orElse(null);

            if (localSelectionne == null) {
                showMessage("❌ Local sélectionné introuvable.", "error");
                return;
            }

            LocalDate dateRes = dateReservation.getValue();
            LocalTime td = LocalTime.of(spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
            LocalTime tf = LocalTime.of(spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
            Date dateReservationFinal = Date.from(dateRes.atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date heureDebut = Date.from(dateRes.atTime(td).atZone(ZoneId.systemDefault()).toInstant());
            Date heureFin   = Date.from(dateRes.atTime(tf).atZone(ZoneId.systemDefault()).toInstant());
            int  prixCalcule = PriceCalculatorService.calculerPrix(heureDebut, heureFin);

            // ── Construire la liste des modifications ─────────────────────
            // ✅ On compare champ par champ et on enregistre uniquement les vrais changements
            List<reservation_historique> modifications = new ArrayList<>();
            int idRes = currentReservation.getId_reservation();

            String nouvNom    = txtNomClient.getText().trim();
            String nouvPrenom = txtPrenomClient.getText().trim();
            String nouvDate   = DATE_FMT.format(dateReservationFinal);
            String nouvDebut  = TIME_FMT.format(heureDebut);
            String nouvFin    = TIME_FMT.format(heureFin);
            String nouvStatut = comboStatut.getValue().getLibelle();
            String nouvMotif  = comboMotif.getValue().getLibelle();
            int    nouvLocalId= localSelectionne.getId_local();
            String nouvLocalNom = localSelectionne.getNom_local();

            if (!ancNom.equals(nouvNom))
                modifications.add(h(idRes, "Nom client", ancNom, nouvNom));
            if (!ancPrenom.equals(nouvPrenom))
                modifications.add(h(idRes, "Prénom client", ancPrenom, nouvPrenom));
            if (ancLocalId != nouvLocalId)
                modifications.add(h(idRes, "Local réservé",
                        "Local #" + ancLocalId, nouvLocalNom + " (#" + nouvLocalId + ")"));
            if (!ancDate.equals(nouvDate))
                modifications.add(h(idRes, "Date de réservation", ancDate, nouvDate));
            if (!ancDebut.equals(nouvDebut))
                modifications.add(h(idRes, "Heure début", ancDebut, nouvDebut));
            if (!ancFin.equals(nouvFin))
                modifications.add(h(idRes, "Heure fin", ancFin, nouvFin));
            if (!ancStatut.equals(nouvStatut))
                modifications.add(h(idRes, "Statut", ancStatut, nouvStatut));
            if (!ancMotif.equals(nouvMotif))
                modifications.add(h(idRes, "Motif", ancMotif, nouvMotif));
            if (ancPrix != prixCalcule)
                modifications.add(h(idRes, "Prix (DT)",
                        String.valueOf(ancPrix), String.valueOf(prixCalcule)));

            // ── Mettre à jour l'objet réservation ─────────────────────────
            currentReservation.setNom_cl(nouvNom);
            currentReservation.setPrenom_cl(nouvPrenom);
            currentReservation.setId_local(nouvLocalId);
            currentReservation.setDate_reservation(dateReservationFinal);
            currentReservation.setHeure_debut_reservation(heureDebut);
            currentReservation.setHeure_fin_reservation(heureFin);
            currentReservation.setStatus_reservation(comboStatut.getValue());
            currentReservation.setMotif_reservation(comboMotif.getValue());
            currentReservation.setPrix_reservation(prixCalcule);

            // ── Persister en base ──────────────────────────────────────────
            reservationService.modifier(currentReservation);

            // ✅ Enregistrer l'historique (seulement s'il y a eu des changements)
            if (!modifications.isEmpty()) {
                historiqueService.enregistrerModifications(modifications);
                System.out.println("✅ " + modifications.size() + " modification(s) enregistrée(s) dans l'historique.");
            }

            // ── Gérer la disponibilité des locaux ─────────────────────────
            if (ancienLocal != null && ancLocalId != nouvLocalId) {
                ancienLocal.setDisponibilite_local("Disponible");
                localService.modifier(ancienLocal);
                localSelectionne.setDisponibilite_local("Sur réservation");
                localService.modifier(localSelectionne);
            } else if (comboStatut.getValue() == StatutReservation.ANNULEE && ancienLocal != null) {
                ancienLocal.setDisponibilite_local("Disponible");
                localService.modifier(ancienLocal);
            }

            showMessage("✅ Réservation modifiée avec succès!\n💰 Nouveau prix: " + prixCalcule + " DT", "success");
            if (parentController != null) parentController.refreshTable();

            // Fermer après 2 secondes
            new Thread(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(this::handleAnnuler);
            }).start();

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("❌ Erreur lors de la modification: " + e.getMessage(), "error");
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("❌ Erreur inattendue: " + e.getMessage(), "error");
        }
    }

    /** Fabrique un objet reservation_historique pour le batch. */
    private reservation_historique h(int idRes, String champ, String ancVal, String nouvVal) {
        return new reservation_historique(idRes, champ, ancVal, nouvVal, "Admin");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNomClient.getText().trim().isEmpty()) {
            errors.append("- Le nom du client est obligatoire\n");
            txtNomClient.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtNomClient.setStyle("");

        if (txtPrenomClient.getText().trim().isEmpty()) {
            errors.append("- Le prénom du client est obligatoire\n");
            txtPrenomClient.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else txtPrenomClient.setStyle("");

        if (comboLocal.getValue() == null) {
            errors.append("- Veuillez sélectionner un local\n");
            comboLocal.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else comboLocal.setStyle("");

        if (dateReservation.getValue() == null) {
            errors.append("- La date de réservation est obligatoire\n");
            dateReservation.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else dateReservation.setStyle("");

        if (comboMotif.getValue() == null) {
            errors.append("- Veuillez sélectionner un motif\n");
        }
        if (comboStatut.getValue() == null) {
            errors.append("- Veuillez sélectionner un statut\n");
        }

        if (errors.length() > 0) {
            showMessage("❌ Erreurs de validation:\n" + errors, "error");
            return false;
        }
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleAnnuler() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }

    private void showMessage(String message, String type) {
        lblMessage.setText(message);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);

        String style;
        if ("success".equals(type)) {
            style = "-fx-text-fill: #1B4332; -fx-font-weight: bold; -fx-background-color: #D8F3DC; " +
                    "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #2D6A4F; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;";
        } else if ("warning".equals(type)) {
            style = "-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #FFF4E6; " +
                    "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #B794F4; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;";
        } else {
            style = "-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #F3E8FF; " +
                    "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #9B7DC4; " +
                    "-fx-border-width: 1px; -fx-border-radius: 10px;";
        }
        lblMessage.setStyle(style);

        FadeTransition ft = new FadeTransition(Duration.millis(300), lblMessage);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void applyFadeInAnimation() {
        if (txtNomClient != null && txtNomClient.getParent() != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), txtNomClient.getParent());
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        }
    }

    public void setParentController(AfficherReservationController controller) {
        this.parentController = controller;
    }
}