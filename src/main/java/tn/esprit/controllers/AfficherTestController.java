package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.TestPsychoService;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.PaginationHelper;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherTestController implements Initializable, DashboardController.DashboardAware {

    @FXML private TableView<TestPsycho> tableViewTests;
    @FXML private TextField txtRecherche;

    @FXML private TableColumn<TestPsycho, String>  colTitre;
    @FXML private TableColumn<TestPsycho, String>  colType;
    @FXML private TableColumn<TestPsycho, String>  colDescription;
    @FXML private TableColumn<TestPsycho, Integer> colDuree;
    @FXML private TableColumn<TestPsycho, String>  colDateCreation;
    @FXML private TableColumn<TestPsycho, String>  colDateModification;
    @FXML private TableColumn<TestPsycho, String>  colStatut;
    @FXML private TableColumn<TestPsycho, Void>    colActions;

    @FXML private ComboBox<String> cmbRechercheType;
    @FXML private ComboBox<String> cmbRechercheStatut;
    @FXML private Label            lblNbResultats;
    @FXML private HBox             paginationContainer;

    private ObservableList<TestPsycho> listeTests;
    private ObservableList<TestPsycho> listeTestsFiltres;
    private int currentPage = 0;

    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // Colonnes classiques
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titreTest"));
        colType.setCellValueFactory(new PropertyValueFactory<>("typeTest"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("descriptionTest"));
        // ✅ Corrigé : "dureeEstimee" au lieu de "dureeEstimeeTest"
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeEstimee"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        // ✅ Corrigé : getDateCreation() au lieu de getDateCreationTest()
        colDateCreation.setCellValueFactory(cellData -> {
            Date date = cellData.getValue().getDateCreation();
            return new SimpleStringProperty(date != null ? dateFormat.format(date) : "-");
        });

        // ✅ Corrigé : getDateModification() au lieu de getDateModificationTest()
        colDateModification.setCellValueFactory(cellData -> {
            Date date = cellData.getValue().getDateModification();
            return new SimpleStringProperty(date != null ? dateFormat.format(date) : "-");
        });

        // Colonne Statut — badge coloré
        colStatut.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isEstActif() ? "Actif" : "Inactif"));

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Actif".equals(item)
                        ? "-fx-background-color:#d4edda;-fx-text-fill:#155724;-fx-font-weight:bold;-fx-alignment:center;"
                        : "-fx-background-color:#f8d7da;-fx-text-fill:#721c24;-fx-font-weight:bold;-fx-alignment:center;");
            }
        });

        // Colonne Actions — boutons Modifier & Supprimer par ligne
        colActions.setCellFactory(column -> new TableCell<>() {

            private final Button btnModifier  = new Button("✏️ Modifier");
            private final Button btnSupprimer = new Button("🗑️ Supprimer");
            private final HBox   box          = new HBox(6, btnModifier, btnSupprimer);

            {
                btnModifier.setStyle(
                        "-fx-background-color:#f6ad55;-fx-text-fill:white;" +
                                "-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:4 8 4 8;");
                btnSupprimer.setStyle(
                        "-fx-background-color:#fc8181;-fx-text-fill:white;" +
                                "-fx-font-size:11;-fx-background-radius:5;-fx-cursor:hand;-fx-padding:4 8 4 8;");

                box.setStyle("-fx-alignment:center;");

                btnModifier.setOnAction(e -> {
                    TestPsycho test = getTableView().getItems().get(getIndex());
                    handleModifier(test);
                });

                btnSupprimer.setOnAction(e -> {
                    TestPsycho test = getTableView().getItems().get(getIndex());
                    handleSupprimer(test);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        // Filtres par défaut
        cmbRechercheType.setValue("Tous");
        cmbRechercheStatut.setValue("Tous");

        // Recherche en temps réel
        txtRecherche.textProperty().addListener((obs, o, n) -> rechercherParType(null));

        actualiser(null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void handleModifier(TestPsycho testSelectionne) {
        if (dashboardController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterTest.fxml"));
                Parent view = loader.load();
                AjouterTestController ctrl = loader.getController();
                ctrl.setDashboardController(dashboardController);
                ctrl.initModification(testSelectionne);

                view.setOpacity(0);
                dashboardController.getContentArea().getChildren().setAll(view);
                if (view instanceof Region r) {
                    r.prefWidthProperty().bind(dashboardController.getContentArea().widthProperty());
                    r.prefHeightProperty().bind(dashboardController.getContentArea().heightProperty());
                }
                FadeTransition ft1 = new FadeTransition(Duration.millis(250), view);
                ft1.setFromValue(0); ft1.setToValue(1); ft1.play();
                dashboardController.updateTopbar("Modifier le test", testSelectionne.getTitreTest());
                dashboardController.maintainActiveButton("tests");
            } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterTest.fxml"));
            Parent root = loader.load();
            AjouterTestController ctrl = loader.getController();
            ctrl.initModification(testSelectionne);
            tableViewTests.getScene().setRoot(root);
        } catch (IOException e) {
            showError("Erreur : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void handleSupprimer(TestPsycho testSelectionne) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Supprimer le test : " + testSelectionne.getTitreTest());
        confirmation.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        Optional<ButtonType> resultat = confirmation.showAndWait();
        if (resultat.isPresent() && resultat.get() == ButtonType.OK) {
            try {
                new TestPsychoService().delete(testSelectionne);
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Succès");
                ok.setHeaderText("Test supprimé");
                ok.setContentText("Le test a été supprimé avec succès !");
                ok.show();
                actualiser(null);
            } catch (SQLException e) {
                showError("Erreur lors de la suppression : " + e.getMessage());
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    void rechercherParType(ActionEvent event) {
        String typeSelectionne   = cmbRechercheType.getValue();
        String statutSelectionne = cmbRechercheStatut.getValue();
        String textRecherche     = txtRecherche.getText();

        if (listeTests == null) return;

        List<TestPsycho> testsFiltres = new ArrayList<>();
        for (TestPsycho test : listeTests) {

            boolean correspondType = "Tous".equals(typeSelectionne) || typeSelectionne.equals(test.getTypeTest());

            boolean correspondStatut;
            if ("Tous".equals(statutSelectionne))        correspondStatut = true;
            else if ("Actif".equals(statutSelectionne))  correspondStatut = test.isEstActif();
            else                                          correspondStatut = !test.isEstActif();

            boolean correspondRecherche;
            if (textRecherche == null || textRecherche.trim().isEmpty()) {
                correspondRecherche = true;
            } else {
                String q = textRecherche.toLowerCase().trim();
                correspondRecherche =
                        (test.getTitreTest()       != null && test.getTitreTest().toLowerCase().contains(q)) ||
                                (test.getDescriptionTest() != null && test.getDescriptionTest().toLowerCase().contains(q));
            }

            if (correspondType && correspondStatut && correspondRecherche)
                testsFiltres.add(test);
        }

        listeTestsFiltres = FXCollections.observableArrayList(testsFiltres);
        currentPage = 0;
        applyPagination();
    }

    @FXML
    void ajouterTest(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.navigateTo("AjouterTest.fxml",
                    "Ajouter un test psychologique", "Créer un nouveau test psychologique");
            dashboardController.maintainActiveButton("tests");
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterTest.fxml"));
            tableViewTests.getScene().setRoot(root);
        } catch (IOException e) { showError("Erreur : " + e.getMessage()); }
    }

    @FXML
    void modifierTest(ActionEvent event) {
        TestPsycho sel = tableViewTests.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarning("Veuillez sélectionner un test dans le tableau."); return; }
        handleModifier(sel);
    }

    @FXML
    void supprimerTest(ActionEvent event) {
        TestPsycho sel = tableViewTests.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarning("Veuillez sélectionner un test dans le tableau."); return; }
        handleSupprimer(sel);
    }

    @FXML
    void gererQuestions(ActionEvent event) {
        TestPsycho testSelectionne = tableViewTests.getSelectionModel().getSelectedItem();
        if (testSelectionne == null) {
            showWarning("Cliquez sur un test dans le tableau pour gérer ses questions.");
            return;
        }

        if (dashboardController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionQuestions.fxml"));
                Parent view = loader.load();
                GestionQuestionsController ctrl = loader.getController();
                ctrl.setDashboardController(dashboardController);
                ctrl.initTest(testSelectionne);

                view.setOpacity(0);
                dashboardController.getContentArea().getChildren().setAll(view);
                if (view instanceof Region r) {
                    r.prefWidthProperty().bind(dashboardController.getContentArea().widthProperty());
                    r.prefHeightProperty().bind(dashboardController.getContentArea().heightProperty());
                }
                FadeTransition ft2 = new FadeTransition(Duration.millis(250), view);
                ft2.setFromValue(0); ft2.setToValue(1); ft2.play();
                dashboardController.updateTopbar("Questions du test", testSelectionne.getTitreTest());
                dashboardController.maintainActiveButton("tests");
            } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GestionQuestions.fxml"));
            Parent root = loader.load();
            GestionQuestionsController ctrl = loader.getController();
            ctrl.initTest(testSelectionne);
            tableViewTests.getScene().setRoot(root);
        } catch (IOException e) { showError("Erreur : " + e.getMessage()); }
    }

    @FXML
    void actualiser(ActionEvent event) {
        try {
            List<TestPsycho> tests = new TestPsychoService().afficherList();
            listeTests = FXCollections.observableArrayList(tests);
            listeTestsFiltres = FXCollections.observableArrayList(tests);
            currentPage = 0;
            cmbRechercheType.setValue("Tous");
            cmbRechercheStatut.setValue("Tous");
            txtRecherche.clear();
            applyPagination();
        } catch (SQLException e) { showError("Erreur lors du chargement : " + e.getMessage()); }
    }

    @FXML
    public void gererObjectifs(ActionEvent actionEvent) {
        if (dashboardController != null) {
            dashboardController.navigateTo("GestionObjectifs.fxml", "Objectifs", "Gestion des objectifs");
            dashboardController.maintainActiveButton("objectifs");
            return;
        }
        try {
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            double w = stage.getWidth(); double h = stage.getHeight(); boolean max = stage.isMaximized();
            Parent root = FXMLLoader.load(getClass().getResource("/GestionObjectifs.fxml"));
            stage.setScene(new Scene(root));
            stage.setWidth(w); stage.setHeight(h);
            if (max) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) { showError("Impossible de charger la page des objectifs: " + e.getMessage()); }
    }

    @FXML
    void ouvrirStatistiques(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.navigateTo("StatistiquesParType.fxml",
                    "Statistiques par type de test", "Analyse des passations par type");
            dashboardController.maintainActiveButton("tests");
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/StatistiquesParType.fxml"));
            tableViewTests.getScene().setRoot(root);
        } catch (IOException e) { showError("Erreur : " + e.getMessage()); }
    }

    @FXML
    void retourMenuPrincipal(ActionEvent event) {
        SessionManager.logout();
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        NavigationManager.navigateTo("Login.fxml", "MindAura – Connexion", stage, false);
    }

    @FXML
    public void AfficherTest(ActionEvent actionEvent) { actualiser(actionEvent); }

    // ─────────────────────────────────────────────────────────────────────────
    private void applyPagination() {
        List<TestPsycho> source = listeTestsFiltres != null ? listeTestsFiltres
                : (listeTests != null ? listeTests : List.of());
        int total = source.size();
        int totalPages = PaginationHelper.getTotalPages(total);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));

        tableViewTests.setItems(FXCollections.observableArrayList(PaginationHelper.getPageItems(source, currentPage)));
        lblNbResultats.setText(total + " test(s) trouvé(s)");

        paginationContainer.getChildren().clear();
        if (totalPages > 1) {
            PaginationHelper.PaginationBar bar = PaginationHelper.createPaginationBarWithRef(
                    currentPage, totalPages, total,
                    () -> { currentPage--; applyPagination(); },
                    () -> { currentPage++; applyPagination(); });
            paginationContainer.getChildren().add(bar.container);
        }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setContentText(msg); a.show();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention"); a.setHeaderText(null); a.setContentText(msg); a.show();
    }
}