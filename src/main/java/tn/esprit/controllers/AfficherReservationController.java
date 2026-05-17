package tn.esprit.controllers;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_historique;
import tn.esprit.entities.reservation_local;
import tn.esprit.entities.salle;
import tn.esprit.enums.MotifReservation;
import tn.esprit.enums.StatutReservation;
import tn.esprit.services.EmailService;
import tn.esprit.services.LocalCapacityService;
import tn.esprit.services.NotificationService;
import tn.esprit.services.PriceCalculatorService;
import tn.esprit.services.ReservationHistoriqueService;
import tn.esprit.services.ReservationPDFService;
import tn.esprit.services.ReservationValidationService;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.services.salle_SERVICE;
import tn.esprit.utils.LocalComboItem;

public class AfficherReservationController extends TranslatableController {

    // ── TABLE ─────────────────────────────────────────────────────────────────
    @FXML private TableView<reservation_local>          tableReservation;
    @FXML private TableColumn<reservation_local, String>  colNomClient;
    @FXML private TableColumn<reservation_local, String>  colPrenomClient;
    @FXML private TableColumn<reservation_local, Date>    colDateReservation;
    @FXML private TableColumn<reservation_local, Date>    colHeureDebut;
    @FXML private TableColumn<reservation_local, Date>    colHeureFin;
    @FXML private TableColumn<reservation_local, String>  colStatut;
    @FXML private TableColumn<reservation_local, String>  colMotif;
    @FXML private TableColumn<reservation_local, String>  colSalle;
    @FXML private TableColumn<reservation_local, Integer> colPrix;
    @FXML private TableColumn<reservation_local, Void>    colActions;

    // ── FILTRES & RECHERCHE ───────────────────────────────────────────────────
    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterStatutComboBox;
    @FXML private ComboBox<String> filterMotifComboBox;

    // ── STATS ─────────────────────────────────────────────────────────────────
    @FXML private Label totalReservationsLabel;
    @FXML private Label totalReservationsBadge;
    @FXML private Label totalConfirmeesLabel;
    @FXML private Label totalEnAttenteLabel;

    // ── BOUTONS LISTE ─────────────────────────────────────────────────────────
    @FXML private Button    btnAjouter;
    @FXML private Button    btnRafraichir;
    @FXML private Button    btnExportPDF;
    @FXML private Button    btnNotification;
    @FXML private Label     badgeNotification;

    /**
     * Conteneur VBox du panneau notifications (côté gauche).
     * Sa largeur est 0 quand le panneau est fermé, 370 quand il est ouvert.
     * Injecté depuis le FXML via fx:id="notifPanelContainer".
     */
    @FXML private VBox notifPanelContainer;

    // ── SWAP DE VUES ─────────────────────────────────────────────────────────
    @FXML private VBox vueListe;
    @FXML private VBox vueFormulaire;

    // ── CHAMPS FORMULAIRE ─────────────────────────────────────────────────────
    @FXML private Label                    lblTitreFormulaire;
    @FXML private Label                    lblSousTitreFormulaire;
    @FXML private TextField                txtNomClient;
    @FXML private TextField                txtPrenomClient;
    @FXML private ComboBox<LocalComboItem> comboLocal;
    @FXML private ComboBox<AjouterReservationController.SalleComboItem> comboSalle;
    @FXML private DatePicker               dateReservation;
    @FXML private Spinner<Integer>         spinnerHeureDebut;
    @FXML private Spinner<Integer>         spinnerMinuteDebut;
    @FXML private Spinner<Integer>         spinnerHeureFin;
    @FXML private Spinner<Integer>         spinnerMinuteFin;
    @FXML private ComboBox<MotifReservation>   comboMotif;
    @FXML private ComboBox<StatutReservation>  comboStatut;
    @FXML private Label                    lblPrixCalcule;
    @FXML private Label                    lblMessage;
    @FXML private Button                   btnSauvegarder;

    // ── ÉTAT INTERNE ──────────────────────────────────────────────────────────
    private reservation_local_SERVICE    reservationService;
    private local_psychiatrie_SERVICE    localService;
    private salle_SERVICE                salleService;
    private ReservationPDFService        pdfService;
    private LocalCapacityService         capacityService;
    private ReservationHistoriqueService historiqueService;
    private ReservationValidationService validationService;

    private ObservableList<reservation_local> reservationList;
    private ObservableList<reservation_local> filteredList;

    /** null → AJOUTER  /  non-null → MODIFIER */
    private reservation_local reservationEnCours = null;

    // ✅ Pour stocker la vue détails chargée dynamiquement
    private Parent detailsPane = null;

    /** true quand le panneau de notifications est ouvert */
    private boolean panneauNotifOuvert = false;

    private static final int    ID_UTILISATEUR_PAR_DEFAUT = 1;
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    //  INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void initComponents(URL url, ResourceBundle resourceBundle) {
        reservationService = new reservation_local_SERVICE();
        localService       = new local_psychiatrie_SERVICE();
        salleService       = new salle_SERVICE();
        pdfService         = new ReservationPDFService();
        capacityService    = new LocalCapacityService();
        historiqueService  = new ReservationHistoriqueService();
        validationService  = new ReservationValidationService();

        NotificationService.getInstance().demarrer(
                "jdbc:mysql://localhost:3306/mindaura", "root", "");
        NotificationService.getInstance().setOnChangement(nonLues ->
                javafx.application.Platform.runLater(() -> mettreAJourBadge(nonLues)));
        mettreAJourBadge(NotificationService.getInstance().getNonLues());

        comboMotif.setItems(FXCollections.observableArrayList(MotifReservation.values()));
        comboMotif.setValue(MotifReservation.CONSULTATION_INDIVIDUELLE);
        comboStatut.setItems(FXCollections.observableArrayList(StatutReservation.values()));
        comboStatut.setValue(StatutReservation.EN_ATTENTE);
        setupTimeSpinners();
        setupPriceCalculation();
        setupDatePicker();

        initializeTableColumns();
        loadData();
        initializeFilters();

        searchField.textProperty().addListener((obs, o, n) -> filterData());
        filterStatutComboBox.valueProperty().addListener((obs, o, n) -> filterData());
        filterMotifComboBox.valueProperty().addListener((obs, o, n) -> filterData());

        applyFadeInAnimation();
        applyCSSStyles();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SWAP DE VUES — liste / formulaire / détails
    // ─────────────────────────────────────────────────────────────────────────

    /** Cache tout sauf la liste */
    public void showListView() {
        if (vueFormulaire != null) {
            vueFormulaire.setVisible(false);
            vueFormulaire.setManaged(false);
        }
        // Masquer + retirer les détails injectés dynamiquement
        if (detailsPane != null) {
            detailsPane.setVisible(false);
            detailsPane.setManaged(false);
            Pane parent = (Pane) vueListe.getParent();
            if (parent != null) parent.getChildren().remove(detailsPane);
            detailsPane = null;
        }
        vueListe.setVisible(true);
        vueListe.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), vueListe);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
        reservationEnCours = null;
    }

    /** Affiche le formulaire plein écran */
    private void afficherFormulaire() {
        vueListe.setVisible(false);
        vueListe.setManaged(false);
        if (detailsPane != null) { detailsPane.setVisible(false); detailsPane.setManaged(false); }

        vueFormulaire.setVisible(true);
        vueFormulaire.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(250), vueFormulaire);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    /**
     * Affiche les détails INLINE (sans nouveau Stage).
     * La vue DetailsReservation.fxml est injectée dans le conteneur parent de vueListe.
     */
    private void afficherDetails(reservation_local reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/DetailsReservation.fxml"));
            Parent detailsRoot = loader.load();
            detailsPane = detailsRoot;

            DetailsReservationController ctrl = loader.getController();
            ctrl.setReservation(reservation);
            ctrl.setParentController(this);

            // Masquer liste et formulaire
            vueListe.setVisible(false);
            vueListe.setManaged(false);
            if (vueFormulaire != null) { vueFormulaire.setVisible(false); vueFormulaire.setManaged(false); }

            // Injecter dans le parent de vueListe
            Pane parent = (Pane) vueListe.getParent();
            if (!parent.getChildren().contains(detailsRoot)) {
                parent.getChildren().add(detailsRoot);
            }
            detailsRoot.setVisible(true);
            detailsRoot.setManaged(true);

            if (parent instanceof VBox) {
                VBox.setVgrow(detailsRoot, Priority.ALWAYS);
            } else if (parent instanceof HBox) {
                HBox.setHgrow(detailsRoot, Priority.ALWAYS);
            }

            FadeTransition ft = new FadeTransition(Duration.millis(300), detailsRoot);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'afficher les détails", e.getMessage());
        }
    }

    /** Retourne à la liste depuis le formulaire */
    @FXML
    private void handleRetourListe() {
        showListView();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AJOUTER
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleAjouterReservation() {
        reservationEnCours = null;
        viderFormulaire();
        chargerLocauxDisponibles();
        chargerSalles();
        dateReservation.setValue(LocalDate.now().plusDays(1));
        lblTitreFormulaire.setText("➕ Nouvelle Réservation");
        lblSousTitreFormulaire.setText("Remplissez les informations de la réservation");
        btnSauvegarder.setText("✓ Ajouter la Réservation");
        lblMessage.setText("");
        afficherFormulaire();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MODIFIER
    // ─────────────────────────────────────────────────────────────────────────

    private void ouvrirFormulaireModifier(reservation_local reservation) {
        reservationEnCours = reservation;
        viderFormulaire();
        chargerTousLocaux();
        chargerSalles();

        txtNomClient.setText(reservation.getNom_cl());
        txtPrenomClient.setText(reservation.getPrenom_cl());

        comboLocal.getItems().stream()
                .filter(item -> item.getIdLocal() == reservation.getId_local())
                .findFirst().ifPresent(item -> comboLocal.setValue(item));

        // Pré-sélectionner la salle si elle existe
        if (comboSalle != null && reservation.getSalle_id() > 0) {
            comboSalle.getItems().stream()
                    .filter(item -> item.getIdSalle() == reservation.getSalle_id())
                    .findFirst().ifPresent(item -> comboSalle.setValue(item));
        }

        if (reservation.getDate_reservation() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(reservation.getDate_reservation());
            dateReservation.setValue(LocalDate.of(
                    cal.get(java.util.Calendar.YEAR),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH)));
        }

        if (reservation.getHeure_debut_reservation() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(reservation.getHeure_debut_reservation());
            spinnerHeureDebut.getValueFactory().setValue(cal.get(java.util.Calendar.HOUR_OF_DAY));
            spinnerMinuteDebut.getValueFactory().setValue(cal.get(java.util.Calendar.MINUTE));
        }
        if (reservation.getHeure_fin_reservation() != null) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(reservation.getHeure_fin_reservation());
            spinnerHeureFin.getValueFactory().setValue(cal.get(java.util.Calendar.HOUR_OF_DAY));
            spinnerMinuteFin.getValueFactory().setValue(cal.get(java.util.Calendar.MINUTE));
        }

        comboMotif.setValue(reservation.getMotif_reservation());
        comboStatut.setValue(reservation.getStatus_reservation());
        calculerEtAfficherPrix();

        lblTitreFormulaire.setText("✏️ Modifier la Réservation");
        lblSousTitreFormulaire.setText("Modifiez les informations de : "
                + reservation.getNom_cl() + " " + reservation.getPrenom_cl());
        btnSauvegarder.setText("✓ Enregistrer les Modifications");
        lblMessage.setText("");
        afficherFormulaire();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAUVEGARDER (AJOUTER OU MODIFIER)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleSauvegarderRes() {
        if (!validateForm()) return;

        if (reservationEnCours == null) {
            // ── MODE AJOUTER ──────────────────────────────────────────────
            try {
                LocalComboItem localItem = comboLocal.getValue();
                local_psychiatrie localSel = localService.afficherList().stream()
                        .filter(l -> l.getId_local() == localItem.getIdLocal())
                        .findFirst().orElse(null);
                if (localSel == null) { showMessage("❌ Local introuvable.", "error"); return; }

                LocalDate dateRes = dateReservation.getValue();
                LocalTime td = LocalTime.of(spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
                LocalTime tf = LocalTime.of(spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());

                if (!tf.isAfter(td)) { showMessage("❌ L'heure de fin doit être après l'heure de début.", "error"); return; }

                Date dateResDate = Date.from(dateRes.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date heureDebut  = Date.from(dateRes.atTime(td).atZone(ZoneId.systemDefault()).toInstant());
                Date heureFin    = Date.from(dateRes.atTime(tf).atZone(ZoneId.systemDefault()).toInstant());

                if (validationService.hasConflict(localSel.getId_local(), dateResDate, heureDebut, heureFin, null)) {
                    showMessage(validationService.getConflictMessage(localSel.getId_local(), heureDebut, heureFin), "error");
                    return;
                }

                int prix = PriceCalculatorService.calculerPrix(heureDebut, heureFin);

                int salleId = (comboSalle != null && comboSalle.getValue() != null)
                        ? comboSalle.getValue().getIdSalle() : 0;

                reservation_local reservation = new reservation_local(
                        0, ID_UTILISATEUR_PAR_DEFAUT, localSel.getId_local(),
                        salleId,
                        dateResDate, heureDebut, heureFin,
                        comboStatut.getValue(), comboMotif.getValue(), prix,
                        txtNomClient.getText().trim(), txtPrenomClient.getText().trim());

                reservationService.add(reservation);
                capacityService.onReservationAdded(localSel.getId_local());
                NotificationService.getInstance().notifierNouvelleReservation(reservation);

                final String nomComplet = txtNomClient.getText().trim() + " " + txtPrenomClient.getText().trim();
                final String nomLocal = localSel.getNom_local();
                final int prixFinal = prix;
                final java.time.LocalDateTime dateHeure = dateRes.atTime(td);
                final java.time.LocalDateTime dateFin   = dateRes.atTime(tf);
                new Thread(() -> EmailService.envoyerConfirmation(
                        "chaffaiichrak298@gmail.com", nomComplet, "MindAura", dateHeure,
                        comboMotif.getValue().toString() + " - " + nomLocal,
                        nomLocal + " | " + dateFin.toLocalTime() + " | " + prixFinal + " DT")).start();

                showMessage("✅ Réservation ajoutée avec succès!\n💰 Prix: " + prix + " DT", "success");
                refreshTable();
                retournerApresDelai();

            } catch (SQLException e) {
                showMessage("❌ Erreur lors de l'ajout: " + e.getMessage(), "error");
            }

        } else {
            // ── MODE MODIFIER ─────────────────────────────────────────────
            try {
                reservation_local res = reservationEnCours;

                String ancNom    = res.getNom_cl();
                String ancPrenom = res.getPrenom_cl();
                int ancLocalId   = res.getId_local();
                String ancDate   = res.getDate_reservation() != null ? DATE_FMT.format(res.getDate_reservation()) : "";
                String ancDebut  = res.getHeure_debut_reservation() != null ? TIME_FMT.format(res.getHeure_debut_reservation()) : "";
                String ancFin    = res.getHeure_fin_reservation() != null ? TIME_FMT.format(res.getHeure_fin_reservation()) : "";
                String ancStatut = res.getStatus_reservation().getLibelle();
                String ancMotif  = res.getMotif_reservation().getLibelle();
                int    ancPrix   = res.getPrix_reservation();

                local_psychiatrie ancienLocal = localService.afficherList().stream()
                        .filter(l -> l.getId_local() == ancLocalId).findFirst().orElse(null);

                LocalComboItem localItem = comboLocal.getValue();
                local_psychiatrie localSel = localService.afficherList().stream()
                        .filter(l -> l.getId_local() == localItem.getIdLocal()).findFirst().orElse(null);
                if (localSel == null) { showMessage("❌ Local introuvable.", "error"); return; }

                LocalDate dateRes = dateReservation.getValue();
                LocalTime td = LocalTime.of(spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
                LocalTime tf = LocalTime.of(spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
                Date dateResDate = Date.from(dateRes.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date heureDebut  = Date.from(dateRes.atTime(td).atZone(ZoneId.systemDefault()).toInstant());
                Date heureFin    = Date.from(dateRes.atTime(tf).atZone(ZoneId.systemDefault()).toInstant());
                int prixCalcule  = PriceCalculatorService.calculerPrix(heureDebut, heureFin);

                List<reservation_historique> mods = new ArrayList<>();
                int idRes = res.getId_reservation();
                String nouvNom    = txtNomClient.getText().trim();
                String nouvPrenom = txtPrenomClient.getText().trim();
                String nouvDate   = DATE_FMT.format(dateResDate);
                String nouvDebut  = TIME_FMT.format(heureDebut);
                String nouvFin    = TIME_FMT.format(heureFin);
                String nouvStatut = comboStatut.getValue().getLibelle();
                String nouvMotif  = comboMotif.getValue().getLibelle();
                int    nouvLocalId= localSel.getId_local();

                if (!ancNom.equals(nouvNom))       mods.add(h(idRes,"Nom client",ancNom,nouvNom));
                if (!ancPrenom.equals(nouvPrenom)) mods.add(h(idRes,"Prénom client",ancPrenom,nouvPrenom));
                if (ancLocalId != nouvLocalId)     mods.add(h(idRes,"Local réservé","Local #"+ancLocalId,localSel.getNom_local()));
                if (!ancDate.equals(nouvDate))     mods.add(h(idRes,"Date de réservation",ancDate,nouvDate));
                if (!ancDebut.equals(nouvDebut))   mods.add(h(idRes,"Heure début",ancDebut,nouvDebut));
                if (!ancFin.equals(nouvFin))       mods.add(h(idRes,"Heure fin",ancFin,nouvFin));
                if (!ancStatut.equals(nouvStatut)) mods.add(h(idRes,"Statut",ancStatut,nouvStatut));
                if (!ancMotif.equals(nouvMotif))   mods.add(h(idRes,"Motif",ancMotif,nouvMotif));
                if (ancPrix != prixCalcule)        mods.add(h(idRes,"Prix (DT)",String.valueOf(ancPrix),String.valueOf(prixCalcule)));

                res.setNom_cl(nouvNom); res.setPrenom_cl(nouvPrenom);
                res.setId_local(nouvLocalId);
                res.setDate_reservation(dateResDate);
                res.setHeure_debut_reservation(heureDebut);
                res.setHeure_fin_reservation(heureFin);
                res.setStatus_reservation(comboStatut.getValue());
                res.setMotif_reservation(comboMotif.getValue());
                res.setPrix_reservation(prixCalcule);
                if (comboSalle != null && comboSalle.getValue() != null)
                    res.setSalle_id(comboSalle.getValue().getIdSalle());

                reservationService.modifier(res);
                if (!mods.isEmpty()) historiqueService.enregistrerModifications(mods);

                if (ancienLocal != null && ancLocalId != nouvLocalId) {
                    ancienLocal.setDisponibilite_local("Disponible");
                    localService.modifier(ancienLocal);
                    localSel.setDisponibilite_local("Sur réservation");
                    localService.modifier(localSel);
                } else if (comboStatut.getValue() == StatutReservation.ANNULEE && ancienLocal != null) {
                    ancienLocal.setDisponibilite_local("Disponible");
                    localService.modifier(ancienLocal);
                }

                showMessage("✅ Réservation modifiée avec succès!\n💰 Nouveau prix: " + prixCalcule + " DT", "success");
                refreshTable();
                retournerApresDelai();

            } catch (SQLException e) {
                showMessage("❌ Erreur lors de la modification: " + e.getMessage(), "error");
            }
        }
    }

    private reservation_historique h(int idRes, String champ, String anc, String nouv) {
        return new reservation_historique(idRes, champ, anc, nouv, "Admin");
    }

    private void retournerApresDelai() {
        new Thread(() -> {
            try { Thread.sleep(1800); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(this::handleRetourListe);
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CHARGEMENT LOCAUX
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerLocauxDisponibles() {
        try {
            ObservableList<LocalComboItem> items = FXCollections.observableArrayList();
            for (local_psychiatrie l : localService.afficherList())
                if ("Disponible".equalsIgnoreCase(l.getDisponibilite_local()))
                    items.add(new LocalComboItem(l));
            comboLocal.setItems(items);
            if (items.isEmpty()) showMessage("⚠️ Aucun local disponible actuellement.", "warning");
        } catch (SQLException e) { showMessage("❌ Erreur chargement locaux: " + e.getMessage(), "error"); }
    }

    private void chargerTousLocaux() {
        try {
            ObservableList<LocalComboItem> items = FXCollections.observableArrayList();
            for (local_psychiatrie l : localService.afficherList())
                items.add(new LocalComboItem(l));
            comboLocal.setItems(items);
        } catch (SQLException e) { showMessage("❌ Erreur chargement locaux: " + e.getMessage(), "error"); }
    }

    private void chargerSalles() {
        try {
            ObservableList<AjouterReservationController.SalleComboItem> items = FXCollections.observableArrayList();
            items.add(new AjouterReservationController.SalleComboItem(null)); // aucune salle
            for (salle s : salleService.afficherList())
                items.add(new AjouterReservationController.SalleComboItem(s));
            if (comboSalle != null) {
                comboSalle.setItems(items);
                comboSalle.setValue(items.get(0));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur chargement salles: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SPINNERS & PRIX
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
            if (date == null) { if (lblPrixCalcule != null) lblPrixCalcule.setText("0 DT"); return; }
            LocalTime td = LocalTime.of(spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
            LocalTime tf = LocalTime.of(spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
            Date hd = Date.from(date.atTime(td).atZone(ZoneId.systemDefault()).toInstant());
            Date hf = Date.from(date.atTime(tf).atZone(ZoneId.systemDefault()).toInstant());
            int prix = PriceCalculatorService.calculerPrix(hd, hf);
            if (lblPrixCalcule != null) lblPrixCalcule.setText(prix + " DT");
        } catch (Exception e) {
            if (lblPrixCalcule != null) lblPrixCalcule.setText("0 DT");
        }
    }

    private void setupDatePicker() {
        dateReservation.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date != null && !date.isAfter(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #F3E8FF; -fx-text-fill: #9B7DC4;");
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (txtNomClient.getText().trim().isEmpty())    { errors.append("- Le nom du client est obligatoire\n");    txtNomClient.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;"); } else txtNomClient.setStyle("");
        if (txtPrenomClient.getText().trim().isEmpty()) { errors.append("- Le prénom du client est obligatoire\n"); txtPrenomClient.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;"); } else txtPrenomClient.setStyle("");
        if (comboLocal.getValue() == null) { errors.append("- Veuillez sélectionner un local\n"); comboLocal.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;"); } else comboLocal.setStyle("");
        if (dateReservation.getValue() == null) {
            errors.append("- La date de réservation est obligatoire\n");
            dateReservation.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else if (reservationEnCours == null && !dateReservation.getValue().isAfter(LocalDate.now())) {
            errors.append("- La date doit être après aujourd'hui\n");
            dateReservation.setStyle("-fx-border-color: #7B5EA7; -fx-border-width: 2px;");
        } else { dateReservation.setStyle(""); }
        if (comboMotif.getValue() == null)  errors.append("- Veuillez sélectionner un motif\n");
        if (comboStatut.getValue() == null) errors.append("- Veuillez sélectionner un statut\n");
        if (errors.length() > 0) { showMessage("❌ Erreurs:\n" + errors, "error"); return false; }
        return true;
    }

    private void viderFormulaire() {
        txtNomClient.clear(); txtPrenomClient.clear();
        comboLocal.setValue(null); comboMotif.setValue(MotifReservation.CONSULTATION_INDIVIDUELLE);
        comboStatut.setValue(StatutReservation.EN_ATTENTE);
        dateReservation.setValue(null);
        if (spinnerHeureDebut.getValueFactory() != null) spinnerHeureDebut.getValueFactory().setValue(9);
        if (spinnerMinuteDebut.getValueFactory() != null) spinnerMinuteDebut.getValueFactory().setValue(0);
        if (spinnerHeureFin.getValueFactory() != null) spinnerHeureFin.getValueFactory().setValue(10);
        if (spinnerMinuteFin.getValueFactory() != null) spinnerMinuteFin.getValueFactory().setValue(0);
        if (lblPrixCalcule != null) lblPrixCalcule.setText("0 DT");
        txtNomClient.setStyle(""); txtPrenomClient.setStyle(""); comboLocal.setStyle(""); dateReservation.setStyle("");
    }

    private void showMessage(String message, String type) {
        lblMessage.setText(message);
        if ("success".equals(type))
            lblMessage.setStyle("-fx-text-fill: #1B4332; -fx-font-weight: bold; -fx-background-color: #D8F3DC; -fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #2D6A4F; -fx-border-width: 1px; -fx-border-radius: 10px;");
        else if ("warning".equals(type))
            lblMessage.setStyle("-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #FFF4E6; -fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #B794F4; -fx-border-width: 1px; -fx-border-radius: 10px;");
        else
            lblMessage.setStyle("-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-background-color: #F3E8FF; -fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-border-color: #9B7DC4; -fx-border-width: 1px; -fx-border-radius: 10px;");
        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COLONNES TABLE
    // ─────────────────────────────────────────────────────────────────────────

    private void initializeTableColumns() {
        colNomClient.setCellValueFactory(new PropertyValueFactory<>("nom_cl"));
        colPrenomClient.setCellValueFactory(new PropertyValueFactory<>("prenom_cl"));

        colDateReservation.setCellValueFactory(new PropertyValueFactory<>("date_reservation"));
        colDateReservation.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Date d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : DATE_FMT.format(d));
            }
        });
        colHeureDebut.setCellValueFactory(new PropertyValueFactory<>("heure_debut_reservation"));
        colHeureDebut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Date d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : TIME_FMT.format(d));
            }
        });
        colHeureFin.setCellValueFactory(new PropertyValueFactory<>("heure_fin_reservation"));
        colHeureFin.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Date d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : TIME_FMT.format(d));
            }
        });
        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_reservation"));
        colPrix.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer prix, boolean empty) {
                super.updateItem(prix, empty);
                if (empty || prix == null) { setText(null); }
                else { setText(prix + " DT"); setStyle("-fx-font-weight: bold; -fx-text-fill: #1B4332;"); }
            }
        });
        setupStatutColumn();
        setupMotifColumn();
        setupSalleColumn();
        addActionButtons();
    }

    private void setupStatutColumn() {
        colStatut.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getStatus_reservation().getLibelle()));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                setText(item);
                getStyleClass().removeAll("badge","badge-disponible","badge-non-disponible","badge-reservation");
                getStyleClass().add("badge");
                reservation_local r = getTableView().getItems().get(getIndex());
                if (r.getStatus_reservation() == StatutReservation.CONFIRMEE)     getStyleClass().add("badge-disponible");
                else if (r.getStatus_reservation() == StatutReservation.ANNULEE)  getStyleClass().add("badge-non-disponible");
                else                                                               getStyleClass().add("badge-reservation");
                setGraphic(null);
            }
        });
    }

    private void setupMotifColumn() {
        colMotif.setCellValueFactory(cd ->
                new javafx.beans.property.SimpleStringProperty(
                        cd.getValue().getMotif_reservation().getLibelle()));
    }

    private void setupSalleColumn() {
        if (colSalle == null) return;
        colSalle.setCellValueFactory(cd -> {
            int salleId = cd.getValue().getSalle_id();
            if (salleId <= 0) return new javafx.beans.property.SimpleStringProperty("—");
            try {
                for (salle s : salleService.afficherList()) {
                    if (s.getId_salle() == salleId)
                        return new javafx.beans.property.SimpleStringProperty(s.getNom_salle());
                }
            } catch (SQLException ignored) {}
            return new javafx.beans.property.SimpleStringProperty("Salle #" + salleId);
        });
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier  = new Button("Modifier");
            private final Button btnSupprimer = new Button("Supprimer");
            private final Button btnDetails   = new Button("Détails");
            private final HBox   hBox         = new HBox(8, btnDetails, btnModifier, btnSupprimer);
            {
                btnModifier.getStyleClass().add("btn-modifier");
                btnSupprimer.getStyleClass().add("btn-supprimer");
                btnDetails.getStyleClass().add("btn-details");
                hBox.setAlignment(javafx.geometry.Pos.CENTER);
                btnModifier.setOnAction(e  -> ouvrirFormulaireModifier(getTableView().getItems().get(getIndex())));
                btnSupprimer.setOnAction(e -> supprimerReservation(getTableView().getItems().get(getIndex())));
                btnDetails.setOnAction(e   -> afficherDetails(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hBox);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DONNÉES & FILTRES
    // ─────────────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            reservationList = FXCollections.observableArrayList(reservationService.afficherList());
            filteredList    = FXCollections.observableArrayList(reservationList);
            tableReservation.setItems(filteredList);
            updateStatistics();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les données", e.getMessage());
        }
    }

    private void initializeFilters() {
        ObservableList<String> statuts = FXCollections.observableArrayList("Tous");
        for (StatutReservation s : StatutReservation.values()) statuts.add(s.getLibelle());
        filterStatutComboBox.setItems(statuts);
        filterStatutComboBox.setValue("Tous");
        ObservableList<String> motifs = FXCollections.observableArrayList("Tous");
        for (MotifReservation m : MotifReservation.values()) motifs.add(m.getLibelle());
        filterMotifComboBox.setItems(motifs);
        filterMotifComboBox.setValue("Tous");
    }

    private void filterData() {
        String search = searchField.getText().toLowerCase();
        String statut = filterStatutComboBox.getValue();
        String motif  = filterMotifComboBox.getValue();
        filteredList.clear();
        for (reservation_local r : reservationList) {
            boolean ms  = search.isEmpty() || r.getNom_cl().toLowerCase().contains(search) || r.getPrenom_cl().toLowerCase().contains(search);
            boolean mst = statut.equals("Tous") || r.getStatus_reservation().getLibelle().equals(statut);
            boolean mm  = motif.equals("Tous")  || r.getMotif_reservation().getLibelle().equals(motif);
            if (ms && mst && mm) filteredList.add(r);
        }
        tableReservation.setItems(filteredList);
        updateStatistics();
    }

    private void updateStatistics() {
        long total = filteredList.size();
        long conf  = filteredList.stream().filter(r -> r.getStatus_reservation() == StatutReservation.CONFIRMEE).count();
        long att   = filteredList.stream().filter(r -> r.getStatus_reservation() == StatutReservation.EN_ATTENTE).count();
        totalReservationsLabel.setText(String.valueOf(total));
        if (totalConfirmeesLabel   != null) totalConfirmeesLabel.setText(String.valueOf(conf));
        if (totalEnAttenteLabel    != null) totalEnAttenteLabel.setText(String.valueOf(att));
        if (totalReservationsBadge != null) totalReservationsBadge.setText(String.valueOf(total));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SUPPRIMER
    // ─────────────────────────────────────────────────────────────────────────

    private void supprimerReservation(reservation_local reservation) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation"); alert.setHeaderText("Supprimer cette réservation ?");
        alert.setContentText(reservation.getNom_cl() + " " + reservation.getPrenom_cl()
                + " - " + DATE_FMT.format(reservation.getDate_reservation()));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int idLocal = reservation.getId_local();
                reservationService.delete(reservation);
                capacityService.onReservationRemoved(idLocal);
                NotificationService.getInstance().notifierSuppression(
                        reservation.getNom_cl() + " " + reservation.getPrenom_cl(),
                        DATE_FMT.format(reservation.getDate_reservation()));
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Supprimé", "Réservation supprimée avec succès.");
                refreshTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer", e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RAFRAÎCHISSEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void handleRafraichir() { refreshTable(); applyRefreshAnimation(); }

    public void refreshTable() {
        try {
            reservationList.clear();
            reservationList.addAll(reservationService.afficherList());
            searchField.clear();
            filterStatutComboBox.setValue("Tous");
            filterMotifComboBox.setValue("Tous");
            filterData();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de rafraîchir", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT PDF
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleExportPDF() {
        if (filteredList.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Aucune donnée",
                    "Aucune réservation à exporter", "Ajoutez des réservations d'abord.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("MindAura_Reservations_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF (*.pdf)", "*.pdf"));
        File file = fc.showSaveDialog(btnExportPDF.getScene().getWindow());
        if (file == null) return;

        btnExportPDF.setDisable(true);
        btnExportPDF.setText("⏳ Génération...");

        final String filePath = file.getAbsolutePath();
        final List<reservation_local> snapshot = new ArrayList<>(filteredList);

        new Thread(() -> {
            try {
                pdfService.exportReservationsToPDF(snapshot, filePath);

                javafx.application.Platform.runLater(() -> {
                    btnExportPDF.setDisable(false);
                    btnExportPDF.setText("📄 Export PDF");

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export réussi");
                    alert.setHeaderText("✅  PDF généré avec succès !");
                    alert.setContentText(
                            "Fichier : " + filePath + "\n"
                                    + "Réservations exportées : " + snapshot.size() + "\n\n"
                                    + "Voulez-vous ouvrir le PDF maintenant ?");
                    ButtonType btnOuvrir = new ButtonType("📂 Ouvrir");
                    ButtonType btnFermer = new ButtonType("Fermer", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(btnOuvrir, btnFermer);

                    alert.showAndWait().ifPresent(bt -> {
                        if (bt == btnOuvrir) {
                            try {
                                java.awt.Desktop.getDesktop().open(new File(filePath));
                            } catch (Exception ex) {
                                showAlert(Alert.AlertType.WARNING, "Impossible d'ouvrir",
                                        "Ouvrez le fichier manuellement", filePath);
                            }
                        }
                    });
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    btnExportPDF.setDisable(false);
                    btnExportPDF.setText("📄 Export PDF");
                    showAlert(Alert.AlertType.ERROR, "Erreur d'export",
                            "Impossible de générer le PDF", e.getMessage());
                });
            }
        }, "PDF-Export-Thread").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  NOTIFICATIONS — DRAWER GAUCHE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Toggle du panneau de notifications.
     * Ouvre/ferme un drawer intégré à GAUCHE du contenu principal.
     * Le panneau pousse le contenu vers la droite (pas de popup flottant).
     */
    @FXML
    private void handleToggleNotifications() {
        if (panneauNotifOuvert) {
            // ── FERMETURE : slide-out vers la gauche puis réduction du conteneur ──
            Node panel = notifPanelContainer.getChildren().isEmpty()
                    ? null : notifPanelContainer.getChildren().get(0);

            if (panel != null) {
                TranslateTransition slide = new TranslateTransition(Duration.millis(260), panel);
                slide.setFromX(0);
                slide.setToX(-370);
                slide.setOnFinished(e -> {
                    notifPanelContainer.getChildren().clear();
                    notifPanelContainer.setPrefWidth(0);
                    notifPanelContainer.setMinWidth(0);
                    notifPanelContainer.setMaxWidth(0);
                    panel.setTranslateX(0);
                });
                slide.play();
            } else {
                // Fallback si le panneau a déjà été retiré
                notifPanelContainer.setPrefWidth(0);
                notifPanelContainer.setMinWidth(0);
                notifPanelContainer.setMaxWidth(0);
            }
            panneauNotifOuvert = false;

        } else {
            // ── OUVERTURE : charger le FXML et l'injecter dans notifPanelContainer ──
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/NotificationPanel.fxml"));
                Node panel = loader.load();

                NotificationPanelController ctrl = loader.getController();
                // Passe le conteneur et le callback de fermeture au contrôleur
                ctrl.setParentContainer(notifPanelContainer, () -> {
                    panneauNotifOuvert = false;
                });

                // Vider et injecter
                notifPanelContainer.getChildren().clear();
                notifPanelContainer.getChildren().add(panel);
                VBox.setVgrow(panel, Priority.ALWAYS);

                // Rendre visible le conteneur avec la bonne largeur
                notifPanelContainer.setPrefWidth(370);
                notifPanelContainer.setMinWidth(370);
                notifPanelContainer.setMaxWidth(370);

                // Marquer les notifications comme lues
                NotificationService.getInstance().marquerToutesLues();

                // Animer l'entrée depuis la gauche
                ctrl.animerEntree();

                panneauNotifOuvert = true;

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible d'ouvrir le panneau de notifications", e.getMessage());
            }
        }
    }

    private void mettreAJourBadge(long nonLues) {
        if (badgeNotification == null) return;
        if (nonLues > 0) {
            badgeNotification.setText(nonLues > 99 ? "99+" : String.valueOf(nonLues));
            badgeNotification.setVisible(true); badgeNotification.setManaged(true);
        } else {
            badgeNotification.setVisible(false); badgeNotification.setManaged(false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TRADUCTION
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void traduireUI(String langCode) {
        tr(btnAjouter, "➕ Nouvelle Réservation");
        tr(btnExportPDF, "📄 Export PDF");
        tr(colNomClient, "Nom Client"); tr(colPrenomClient, "Prénom Client");
        tr(colDateReservation, "Date"); tr(colHeureDebut, "Heure Début");
        tr(colHeureFin, "Heure Fin"); tr(colStatut, "Statut");
        tr(colMotif, "Motif"); tr(colPrix, "Prix"); tr(colActions, "Actions");
        trPrompt(searchField, "Rechercher une réservation...");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private void applyCSSStyles() {
        if (btnAjouter    != null) btnAjouter.getStyleClass().add("btn-primary-action");
        if (btnRafraichir != null) btnRafraichir.getStyleClass().add("icon-btn");
        if (btnExportPDF  != null) btnExportPDF.getStyleClass().add("btn-outline");
    }

    private void applyFadeInAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(800), tableReservation);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
    }

    private void applyRefreshAnimation() {
        if (btnRafraichir == null) return;
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), btnRafraichir);
        scale.setFromX(1.0); scale.setFromY(1.0); scale.setToX(0.9); scale.setToY(0.9);
        scale.setCycleCount(2); scale.setAutoReverse(true); scale.play();
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(header);
        alert.setContentText(content); alert.showAndWait();
    }
}