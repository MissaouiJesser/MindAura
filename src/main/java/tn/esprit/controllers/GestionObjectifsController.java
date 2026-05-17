package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import tn.esprit.entities.Objectif;
import tn.esprit.services.ObjectifService;
import tn.esprit.utils.PaginationHelper;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class GestionObjectifsController implements Initializable, DashboardController.DashboardAware {

    @FXML private ListView<Objectif>            listViewObjectifs;
    @FXML private javafx.scene.layout.HBox      paginationContainer;

    private List<Objectif>      listeObjectifsFull = new ArrayList<>();
    private int                 currentPage        = 0;
    private ObjectifService     objectifService;
    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        objectifService = new ObjectifService();

        listViewObjectifs.setCellFactory(param -> new ListCell<Objectif>() {
            @Override
            protected void updateItem(Objectif obj, boolean empty) {
                super.updateItem(obj, empty);
                if (empty || obj == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // ── Contenu textuel ──────────────────────────────────────────
                Text titre = new Text(obj.getTitre());
                titre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));

                String infoTxt = String.format(
                        "📋 Type: %s   |   🎯 Niveau: %s   |   Score: %d – %d   |   ⏱️ Durée: %d jours",
                        obj.getTypeTest()          != null ? obj.getTypeTest()          : "-",
                        obj.getNiveauRecommande()  != null ? obj.getNiveauRecommande()  : "-",
                        obj.getScoreMin(), obj.getScoreMax(),
                        obj.getDureeEstimee());

                Label lblInfo = new Label(infoTxt);
                lblInfo.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12;");

                VBox content = new VBox(5, titre, lblInfo);
                if (obj.getDescription() != null && !obj.getDescription().isEmpty()) {
                    Label desc = new Label(obj.getDescription());
                    desc.setStyle("-fx-text-fill: #374151; -fx-font-size: 12;");
                    desc.setWrapText(true);
                    content.getChildren().add(desc);
                }
                content.setPadding(new Insets(4, 0, 4, 0));
                HBox.setHgrow(content, Priority.ALWAYS);

                // ── Boutons inline ───────────────────────────────────────────
                Button btnModifier  = new Button("✏️ Modifier");
                Button btnSupprimer = new Button("🗑️ Supprimer");

                btnModifier.getStyleClass().add("btn-icon-edit");
                btnSupprimer.getStyleClass().add("btn-icon-delete");

                btnModifier.setOnAction(e -> handleModifier(obj));
                btnSupprimer.setOnAction(e -> handleSupprimer(obj));

                HBox actions = new HBox(8, btnModifier, btnSupprimer);
                actions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(16, content, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 12, 10, 12));

                setText(null);
                setGraphic(row);
            }
        });

        chargerObjectifs();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logique Modifier
    // ─────────────────────────────────────────────────────────────────────────
    private void handleModifier(Objectif obj) {
        if (dashboardController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterObjectif.fxml"));
                Parent view = loader.load();
                AjouterObjectifController ctrl = loader.getController();
                ctrl.setDashboardController(dashboardController);
                ctrl.initModification(obj);

                view.setOpacity(0);
                dashboardController.getContentArea().getChildren().setAll(view);
                if (view instanceof Region r) {
                    r.prefWidthProperty().bind(dashboardController.getContentArea().widthProperty());
                    r.prefHeightProperty().bind(dashboardController.getContentArea().heightProperty());
                }
                javafx.animation.FadeTransition ft =
                        new javafx.animation.FadeTransition(javafx.util.Duration.millis(250), view);
                ft.setFromValue(0); ft.setToValue(1); ft.play();

                dashboardController.updateTopbar("Modifier l'objectif", obj.getTitre());
                dashboardController.maintainActiveButton("objectifs");
            } catch (IOException e) { e.printStackTrace(); }
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterObjectif.fxml"));
            Parent root = loader.load();
            AjouterObjectifController ctrl = loader.getController();
            ctrl.initModification(obj);
            listViewObjectifs.getScene().setRoot(root);
        } catch (IOException e) { showError("Erreur : " + e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logique Supprimer
    // ─────────────────────────────────────────────────────────────────────────
    private void handleSupprimer(Objectif obj) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer : " + obj.getTitre());
        confirmation.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        Optional<ButtonType> resultat = confirmation.showAndWait();
        if (resultat.isPresent() && resultat.get() == ButtonType.OK) {
            try {
                objectifService.delete(obj);
                showInfo("Objectif supprimé avec succès !");
                chargerObjectifs();
            } catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Actions FXML (boutons de la barre d'outils)
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    void ajouterObjectif(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.navigateTo("AjouterObjectif.fxml", "Ajouter un objectif", "Créer un nouvel objectif");
            dashboardController.maintainActiveButton("objectifs");
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AjouterObjectif.fxml"));
            listViewObjectifs.getScene().setRoot(root);
        } catch (IOException e) { showError("Erreur : " + e.getMessage()); }
    }

    /** Conservé pour compatibilité clavier / raccourcis éventuels */
    @FXML
    void modifierObjectif(ActionEvent event) {
        Objectif sel = listViewObjectifs.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarning("Veuillez sélectionner un objectif."); return; }
        handleModifier(sel);
    }

    @FXML
    void supprimerObjectif(ActionEvent event) {
        Objectif sel = listViewObjectifs.getSelectionModel().getSelectedItem();
        if (sel == null) { showWarning("Veuillez sélectionner un objectif."); return; }
        handleSupprimer(sel);
    }

    @FXML
    void actualiser(ActionEvent event) { chargerObjectifs(); }

    @FXML
    void retour(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.navigateTo("AfficherTest.fxml", "Tests psychologiques", "Gestion des tests psychologiques");
            dashboardController.maintainActiveButton("tests");
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherTest.fxml"));
            listViewObjectifs.getScene().setRoot(root);
        } catch (IOException e) { showError("Impossible de charger la page : " + e.getMessage()); }
    }

    @FXML
    public void AfficherTest(ActionEvent event) {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage)
                    ((javafx.scene.Node) event.getSource()).getScene().getWindow();
            double w = stage.getWidth(); double h = stage.getHeight(); boolean max = stage.isMaximized();
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherTest.fxml"));
            stage.setScene(new javafx.scene.Scene(root));
            stage.setWidth(w); stage.setHeight(h);
            if (max) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) { showError("Impossible de charger la page des tests : " + e.getMessage()); }
    }

    @FXML
    public void gererObjectifs(ActionEvent event) { /* déjà sur cette page */ }

    // ─────────────────────────────────────────────────────────────────────────
    private void chargerObjectifs() {
        try {
            listeObjectifsFull = objectifService.getObjectifsAdmin();
            currentPage = 0;
            applyPagination();
        } catch (SQLException e) { showError("Erreur : " + e.getMessage()); }
    }

    private void applyPagination() {
        int total      = listeObjectifsFull.size();
        int totalPages = PaginationHelper.getTotalPages(total);
        currentPage    = Math.min(currentPage, Math.max(0, totalPages - 1));
        listViewObjectifs.setItems(FXCollections.observableArrayList(
                PaginationHelper.getPageItems(listeObjectifsFull, currentPage)));

        if (paginationContainer != null) {
            paginationContainer.getChildren().clear();
            if (totalPages > 1) {
                PaginationHelper.PaginationBar bar = PaginationHelper.createPaginationBarWithRef(
                        currentPage, totalPages, total,
                        () -> { currentPage--; applyPagination(); },
                        () -> { currentPage++; applyPagination(); });
                paginationContainer.getChildren().add(bar.container);
            }
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).show();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setContentText(msg); a.show();
    }
    private void showWarning(String msg) {
        new Alert(Alert.AlertType.WARNING, msg).show();
    }
}