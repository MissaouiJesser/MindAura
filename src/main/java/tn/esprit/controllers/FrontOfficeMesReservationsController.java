package tn.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.SequentialTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_local;
import tn.esprit.enums.StatutReservation;
import tn.esprit.services.LocalCapacityService;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.utils.SessionManager;

public class FrontOfficeMesReservationsController implements Initializable {

    @FXML private TableView<reservation_local> tableReservations;
    @FXML private TableColumn<reservation_local, Integer> colId;
    @FXML private TableColumn<reservation_local, String> colDate;
    @FXML private TableColumn<reservation_local, String> colLocal;
    @FXML private TableColumn<reservation_local, String> colHeureDebut;
    @FXML private TableColumn<reservation_local, String> colHeureFin;
    @FXML private TableColumn<reservation_local, String> colMotif;
    @FXML private TableColumn<reservation_local, String> colStatut;
    @FXML private TableColumn<reservation_local, Integer> colPrix;
    @FXML private TableColumn<reservation_local, Void> colActions;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatutComboBox;
    @FXML private Button btnRafraichir;
    @FXML private Label lblStatistiques;

    // ── Cartes statistiques ──────────────────────────────────────────────────
    @FXML private VBox cardTotal;
    @FXML private VBox cardConfirmees;
    @FXML private VBox cardAttente;
    @FXML private VBox cardAnnulees;
    @FXML private VBox cardTerminees;
    @FXML private Label statTotal;
    @FXML private Label statConfirmees;
    @FXML private Label statAttente;
    @FXML private Label statAnnulees;
    @FXML private Label statTerminees;
    @FXML private HBox statsBox;
    @FXML private VBox tableCard;

    private reservation_local_SERVICE reservationService;
    private local_psychiatrie_SERVICE localService;
    private LocalCapacityService capacityService;
    private ObservableList<reservation_local> reservationList;
    private ObservableList<reservation_local> filteredList;

    // Référence au contrôleur parent UserHome pour la navigation inline
    private UserHomeController parentHomeController;

    /**
     * Injecte la référence au contrôleur UserHome.
     * Appelé par UserHomeController.loadInCenter().
     */
    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reservationService = new reservation_local_SERVICE();
        localService = new local_psychiatrie_SERVICE();
        capacityService = new LocalCapacityService();
        reservationList = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        initializeTableColumns();
        setupTableRowHover();
        loadData();
        initializeFilters();
        filterData();

        searchField.textProperty().addListener((obs, old, val) -> filterData());
        filterStatutComboBox.valueProperty().addListener((obs, old, val) -> filterData());

        applyEntranceAnimations();
    }

    private void initializeTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id_reservation"));

        colDate.setCellValueFactory(cellData -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            return new javafx.beans.property.SimpleStringProperty(
                    sdf.format(cellData.getValue().getDate_reservation()));
        });

        colLocal.setCellValueFactory(cellData -> {
            int idLocal = cellData.getValue().getId_local();
            String nomLocal = getNomLocal(idLocal);
            return new javafx.beans.property.SimpleStringProperty(nomLocal);
        });

        colHeureDebut.setCellValueFactory(cellData -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                    sdf.format(cellData.getValue().getHeure_debut_reservation()));
        });

        colHeureFin.setCellValueFactory(cellData -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return new javafx.beans.property.SimpleStringProperty(
                    sdf.format(cellData.getValue().getHeure_fin_reservation()));
        });

        colMotif.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getMotif_reservation().getLibelle()));

        colPrix.setCellValueFactory(new PropertyValueFactory<>("prix_reservation"));

        setupStatutColumn();
        addActionButtons();
    }

    private void setupStatutColumn() {
        colStatut.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getStatus_reservation().getLibelle()));

        colStatut.setCellFactory(column -> new TableCell<reservation_local, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setGraphic(null);
                    setStyle("");
                } else {
                    setText(null);
                    reservation_local res = getTableView().getItems().get(getIndex());
                    Label badge = new Label(item);
                    badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; " +
                            "-fx-background-radius: 20px; -fx-padding: 4px 14px;");
                    switch (res.getStatus_reservation()) {
                        case CONFIRMEE:
                            badge.setStyle(badge.getStyle() +
                                    "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;");
                            break;
                        case EN_ATTENTE:
                            badge.setStyle(badge.getStyle() +
                                    "-fx-background-color: #EDE9FE; -fx-text-fill: #5B21B6;");
                            break;
                        case ANNULEE:
                            badge.setStyle(badge.getStyle() +
                                    "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;");
                            break;
                        case TERMINEE:
                            badge.setStyle(badge.getStyle() +
                                    "-fx-background-color: #F3F4F6; -fx-text-fill: #4B5563;");
                            break;
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                    setStyle("");
                }
            }
        });
    }

    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnAnnuler = new Button("✗  Annuler");
            private final HBox hBox = new HBox(8);

            {
                btnAnnuler.setStyle(
                        "-fx-background-color: linear-gradient(to bottom right, #EF4444, #DC2626); " +
                                "-fx-text-fill: white; -fx-background-radius: 8px; " +
                                "-fx-padding: 7px 16px; -fx-font-size: 11.5px; -fx-font-weight: 700; " +
                                "-fx-cursor: hand; -fx-border-color: transparent;");

                btnAnnuler.setOnMouseEntered(e -> btnAnnuler.setStyle(
                        "-fx-background-color: linear-gradient(to bottom right, #DC2626, #B91C1C); " +
                                "-fx-text-fill: white; -fx-background-radius: 8px; " +
                                "-fx-padding: 7px 16px; -fx-font-size: 11.5px; -fx-font-weight: 700; " +
                                "-fx-cursor: hand; -fx-border-color: transparent; " +
                                "-fx-effect: dropshadow(gaussian, rgba(220,38,38,0.45), 10, 0, 0, 3);"));

                btnAnnuler.setOnMouseExited(e -> {
                    reservation_local res = getTableView().getItems().get(getIndex());
                    if (res.getStatus_reservation() != StatutReservation.ANNULEE &&
                            res.getStatus_reservation() != StatutReservation.TERMINEE) {
                        btnAnnuler.setStyle(
                                "-fx-background-color: linear-gradient(to bottom right, #EF4444, #DC2626); " +
                                        "-fx-text-fill: white; -fx-background-radius: 8px; " +
                                        "-fx-padding: 7px 16px; -fx-font-size: 11.5px; -fx-font-weight: 700; " +
                                        "-fx-cursor: hand; -fx-border-color: transparent;");
                    }
                });

                btnAnnuler.setOnAction(event -> {
                    reservation_local res = getTableView().getItems().get(getIndex());
                    annulerReservation(res);
                });

                hBox.getChildren().add(btnAnnuler);
                hBox.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    reservation_local res = getTableView().getItems().get(getIndex());
                    if (res.getStatus_reservation() == StatutReservation.ANNULEE ||
                            res.getStatus_reservation() == StatutReservation.TERMINEE) {
                        btnAnnuler.setDisable(true);
                        btnAnnuler.setText("— N/A");
                        btnAnnuler.setStyle(
                                "-fx-background-color: #E5E7EB; -fx-text-fill: #9CA3AF; " +
                                        "-fx-background-radius: 8px; -fx-padding: 7px 16px; " +
                                        "-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-cursor: default;");
                    } else {
                        btnAnnuler.setDisable(false);
                        btnAnnuler.setText("✗  Annuler");
                        btnAnnuler.setStyle(
                                "-fx-background-color: linear-gradient(to bottom right, #EF4444, #DC2626); " +
                                        "-fx-text-fill: white; -fx-background-radius: 8px; " +
                                        "-fx-padding: 7px 16px; -fx-font-size: 11.5px; -fx-font-weight: 700; " +
                                        "-fx-cursor: hand; -fx-border-color: transparent;");
                    }
                    setGraphic(hBox);
                }
            }
        });
    }

    private void loadData() {
        try {
            reservationList.clear();
            List<reservation_local> toutes = reservationService.afficherList();
            if (SessionManager.isLoggedIn()) {
                try {
                    int idUser = Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur());
                    toutes.stream()
                            .filter(r -> r.getId_utilisateur() == idUser)
                            .forEach(reservationList::add);
                } catch (NumberFormatException e) {
                    reservationList.addAll(toutes);
                }
            } else {
                reservationList.addAll(toutes);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les réservations", e.getMessage());
        }
    }

    private void initializeFilters() {
        filterStatutComboBox.setItems(FXCollections.observableArrayList(
                "Tous", "Confirmée", "En Attente", "Annulée", "Terminée"));
        filterStatutComboBox.setValue("Tous");
    }

    private void filterData() {
        String search = searchField.getText().toLowerCase().trim();
        String statut = filterStatutComboBox.getValue();

        filteredList.clear();
        filteredList.addAll(reservationList.stream()
                .filter(res -> {
                    boolean matchSearch = search.isEmpty() ||
                            getNomLocal(res.getId_local()).toLowerCase().contains(search) ||
                            String.valueOf(res.getId_reservation()).contains(search);

                    boolean matchStatut = statut.equals("Tous") ||
                            res.getStatus_reservation().getLibelle().equals(statut);

                    return matchSearch && matchStatut;
                })
                .collect(Collectors.toList()));

        tableReservations.setItems(filteredList.isEmpty() && reservationList.isEmpty() ?
                reservationList : filteredList);
        updateStatistiques();
    }

    private void annulerReservation(reservation_local res) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Annuler cette réservation ?");
        confirm.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                res.setStatus_reservation(StatutReservation.ANNULEE);
                reservationService.modifier(res);

                capacityService.onReservationRemoved(res.getId_local());

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Réservation annulée",
                        "Votre réservation a été annulée avec succès");
                refreshTable();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'annuler", e.getMessage());
            }
        }
    }

    private String getNomLocal(int idLocal) {
        try {
            List<local_psychiatrie> locaux = localService.afficherList();
            return locaux.stream()
                    .filter(l -> l.getId_local() == idLocal)
                    .map(local_psychiatrie::getNom_local)
                    .findFirst()
                    .orElse("Local #" + idLocal);
        } catch (SQLException e) {
            return "Local #" + idLocal;
        }
    }

    private void updateStatistiques() {
        List<reservation_local> displayed = filteredList.isEmpty() && searchField.getText().isEmpty()
                ? reservationList : filteredList;

        long confirmees = displayed.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.CONFIRMEE).count();
        long attente = displayed.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.EN_ATTENTE).count();
        long annulees = displayed.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.ANNULEE).count();
        long terminees = displayed.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.TERMINEE).count();
        long actives = confirmees + attente;

        lblStatistiques.setText(String.format(
                "%d réservation(s)  •  %d active(s)  •  %d en attente",
                displayed.size(), actives, attente));

        // Mettre à jour les cartes stats
        if (statTotal     != null) statTotal.setText(String.valueOf(displayed.size()));
        if (statConfirmees != null) statConfirmees.setText(String.valueOf(confirmees));
        if (statAttente    != null) statAttente.setText(String.valueOf(attente));
        if (statAnnulees   != null) statAnnulees.setText(String.valueOf(annulees));
        if (statTerminees  != null) statTerminees.setText(String.valueOf(terminees));
    }

    @FXML
    private void handleRafraichir() {
        loadData();
        searchField.clear();
        filterStatutComboBox.setValue("Tous");
        filterData();
    }

    public void refreshTable() {
        handleRafraichir();
    }

    // ─── Navigation navbar UserHome (Accueil / Services / Equipe / Contact) ──────────────────
    //  Permet à la navbar de UserHome de fonctionner depuis FrontOfficeMesReservations.

    @FXML
    private void handleNavigateToAccueil() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToAccueil(); return; }
        naviguerVers("/FrontOfficeAccueil.fxml", "MindAura \u2013 Accueil", 1400, 850);
    }

    @FXML
    private void handleNavigateToServices() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToServices(); return; }
        naviguerVers("/FrontOfficeAccueil.fxml", "MindAura \u2013 Accueil", 1400, 850);
    }

    @FXML
    private void handleNavigateToEquipe() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToEquipe(); return; }
    }

    @FXML
    private void handleNavigateToContact() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToContact(); return; }
    }

    @FXML
    private void handleNavigateToRecommandations() {
        if (parentHomeController != null) { parentHomeController.loadRecommandations(); return; }
        naviguerVers("/Recommandation.fxml", "MindAura \u2013 Recommandations", 950, 750);
    }

    @FXML
    private void handleNavigateToCalendrier() {
        if (parentHomeController != null) { parentHomeController.loadCalendrier(); return; }
        naviguerVers("/CalendrierReservation.fxml", "MindAura \u2013 Calendrier", 1100, 750);
    }

    @FXML
    private void handleNavigateToCarte() {
        if (parentHomeController != null) { parentHomeController.loadCarte(); return; }
        naviguerVers("/CarteGeographique.fxml", "MindAura \u2013 Carte", 1100, 750);
    }

    private void naviguerVers(String fxmlPath, String titre, double largeur, double hauteur) {
        try {
            java.net.URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                showAlert(Alert.AlertType.ERROR, "Fichier introuvable",
                        "Le fichier FXML est absent : " + fxmlPath,
                        "➡ Vérifiez que le fichier est dans src/main/resources/\n" +
                                "➡ Faites Maven > Reload + Build > Rebuild Project\n" +
                                "➡ Vérifiez l'orthographe exacte du nom");
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Stage stage = (Stage) tableReservations.getScene().getWindow();
            Scene scene = new Scene(root);
            java.net.URL cssUrl = getClass().getResource("/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setTitle(titre);
            stage.setWidth(largeur);
            stage.setHeight(hauteur);
            stage.centerOnScreen();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur de navigation",
                    "Impossible d'ouvrir : " + fxmlPath,
                    e.getClass().getSimpleName() + ":\n" + e.getMessage());
        }
    }

    @FXML
    private void handleRetourChoix() {
        if (parentHomeController != null) {
            parentHomeController.loadFrontOfficeAccueil();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOfficeAccueil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tableReservations.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setTitle("MindAura – Locaux Psychologiques");
            stage.setWidth(1400);
            stage.setHeight(850);
            stage.centerOnScreen();
            stage.setScene(scene);
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner", e.getMessage());
        }
    }

    /** Hover effect on table rows */
    private void setupTableRowHover() {
        tableReservations.setRowFactory(tv -> {
            TableRow<reservation_local> row = new TableRow<>();
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-background-color: #F8FAFC;");
                }
            });
            row.setOnMouseExited(e -> row.setStyle(""));
            return row;
        });
    }

    /** Animations d'entrée en cascade : cartes stats puis tableau */
    private void applyEntranceAnimations() {
        // Animer les cartes stats en cascade
        VBox[] cards = { cardTotal, cardConfirmees, cardAttente, cardAnnulees, cardTerminees };
        SequentialTransition cascade = new SequentialTransition();

        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) continue;
            cards[i].setOpacity(0);
            cards[i].setTranslateY(18);

            FadeTransition fade = new FadeTransition(Duration.millis(320), cards[i]);
            fade.setFromValue(0); fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(320), cards[i]);
            slide.setFromY(18); slide.setToY(0);

            ParallelTransition cardAnim = new ParallelTransition(fade, slide);
            cardAnim.setDelay(Duration.millis(i * 70));
            cascade.getChildren().add(cardAnim);
        }
        cascade.play();

        // Animer le tableau avec un léger délai après les cartes
        if (tableCard != null) {
            tableCard.setOpacity(0);
            tableCard.setTranslateY(22);
            FadeTransition tf = new FadeTransition(Duration.millis(450), tableCard);
            tf.setFromValue(0); tf.setToValue(1);
            tf.setDelay(Duration.millis(420));
            TranslateTransition ts = new TranslateTransition(Duration.millis(450), tableCard);
            ts.setFromY(22); ts.setToY(0);
            ts.setDelay(Duration.millis(420));
            new ParallelTransition(tf, ts).play();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}