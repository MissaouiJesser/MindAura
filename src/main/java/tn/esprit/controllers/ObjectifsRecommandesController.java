package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import tn.esprit.entities.Objectif;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.GeminiService;
import tn.esprit.services.ObjectifService;
import tn.esprit.utils.PaginationHelper;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ObjectifsRecommandesController implements Initializable {

    // ─── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label              lblTestTitre;
    @FXML private Label              lblScore;
    @FXML private Label              userNameLabel;
    @FXML private ListView<Objectif> listViewObjectifs;
    @FXML private javafx.scene.layout.HBox paginationContainer;

    @FXML private ListView<String>   objectifsListView;
    @FXML private ProgressIndicator  loadingIndicator;

    // ─── État ─────────────────────────────────────────────────────────────────
    private TestPsycho     testCourant;
    private int            scoreTotalObtenu;
    private List<Objectif> listeObjectifsFull = new ArrayList<>();
    private int            currentPage        = 0;

    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController ctrl) {
        this.parentHomeController = ctrl;
    }

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (userNameLabel != null && SessionManager.isLoggedIn()
                && SessionManager.getCurrentUser() != null) {
            userNameLabel.setText(SessionManager.getNomComplet());
        }

        listViewObjectifs.setCellFactory(param -> new ListCell<Objectif>() {
            @Override
            protected void updateItem(Objectif obj, boolean empty) {
                super.updateItem(obj, empty);
                if (empty || obj == null) { setText(null); setGraphic(null); return; }

                VBox vbox = new VBox(12);
                vbox.setPadding(new javafx.geometry.Insets(15));
                vbox.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                        "-fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

                Text titre = new Text("🎯 " + obj.getTitre());
                titre.setFont(Font.font("System", FontWeight.BOLD, 18));

                Label desc = new Label(obj.getDescription());
                desc.setStyle("-fx-text-fill: #34495e; -fx-font-size: 13;");
                desc.setWrapText(true);

                Label info = new Label(String.format("📂 %s | ⏱️ %d jours | 💪 %s",
                        obj.getCategorie(), obj.getDureeEstimee(), obj.getDifficulte()));
                info.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12;");

                vbox.getChildren().addAll(titre, desc, info);
                setGraphic(vbox);
                setText(null);
            }
        });
    }

    // =========================================================================
    //  INIT DEPUIS L'EXTÉRIEUR
    // =========================================================================

    public void initTest(TestPsycho test, int scoreTotal) {
        this.testCourant      = test;
        this.scoreTotalObtenu = scoreTotal;

        lblTestTitre.setText(test.getTitreTest());
        lblScore.setText(scoreTotal + " points");

        chargerObjectifs();
    }

    // =========================================================================
    //  CHARGEMENT
    // =========================================================================

    private void chargerObjectifs() {
        try {
            listeObjectifsFull = new ObjectifService()
                    .recommanderObjectifs(testCourant.getTypeTest(), scoreTotalObtenu);
            currentPage = 0;
            applyPagination();

            if (listeObjectifsFull.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Aucun objectif recommandé pour votre score.\n" +
                                "Vous pouvez créer votre propre objectif !").show();
            }
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
            e.printStackTrace();
        }
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
                        () -> { currentPage++; applyPagination(); }
                );
                paginationContainer.getChildren().add(bar.container);
            }
        }
    }

    // =========================================================================
    //  ACTION — ADOPTER
    // =========================================================================

    @FXML
    void adopterObjectif(ActionEvent event) {
        Objectif sel = listViewObjectifs.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un objectif.").show();
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Adopter l'objectif");
        confirmation.setHeaderText(sel.getTitre());
        confirmation.setContentText("Voulez-vous adopter cet objectif ?");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            try {
                // ✅ getId_utilisateur() retourne String → conversion en int
                int userId;
                try {
                    userId = Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur());
                } catch (NumberFormatException ex) {
                    new Alert(Alert.AlertType.ERROR, "ID utilisateur invalide.").show();
                    return;
                }

                // ✅ date_echeance NOT NULL → durée estimée de l'objectif recommandé
                Date dateEcheance = new Date(System.currentTimeMillis()
                        + (long) sel.getDureeEstimee() * 24 * 60 * 60 * 1000);

                // ✅ Constructeur patient (int idUtilisateur, ...) — pas de setters supprimés
                Objectif objectifPatient = new Objectif(
                        userId,
                        sel.getTitre(),
                        sel.getDescription(),
                        sel.getCategorie(),
                        sel.getDifficulte(),
                        dateEcheance
                );
                // Champs complémentaires disponibles dans l'entité
                objectifPatient.setTypeTest(sel.getTypeTest());
                objectifPatient.setNiveauRecommande(sel.getNiveauRecommande());
                objectifPatient.setScoreMin(sel.getScoreMin());
                objectifPatient.setScoreMax(sel.getScoreMax());
                objectifPatient.setDureeEstimee(sel.getDureeEstimee());

                new ObjectifService().addMeth2(objectifPatient);

                Alert succes = new Alert(Alert.AlertType.INFORMATION);
                succes.setTitle("Succès");
                succes.setHeaderText("Objectif adopté ! 🎉");
                succes.setContentText("Vous pouvez voir vos objectifs dans 'Mes Objectifs'.");
                succes.showAndWait();

            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
                e.printStackTrace();
            }
        }
    }

    // =========================================================================
    //  GÉNÉRATION IA
    // =========================================================================

    public void genererObjectifsIA(String testName, int score, int maxScore) {
        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        String prompt = """
                Un utilisateur a obtenu %d/%d au test psychologique "%s".
                Génère exactement 4 objectifs SMART de développement personnel.
                
                Format OBLIGATOIRE pour chaque objectif:
                🎯 [Titre court]
                → [Explication en 1-2 phrases]
                ⏱ Délai: [délai réaliste]
                
                Réponds uniquement en français.
                """.formatted(score, maxScore, testName);

        new Thread(() -> {
            String result   = GeminiService.askGemini(prompt);
            String[] objArr = result.split("🎯");
            javafx.application.Platform.runLater(() -> {
                if (objectifsListView != null) {
                    objectifsListView.getItems().clear();
                    for (String obj : objArr)
                        if (!obj.trim().isEmpty())
                            objectifsListView.getItems().add("🎯" + obj.trim());
                }
                if (loadingIndicator != null) loadingIndicator.setVisible(false);
            });
        }).start();
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================

    @FXML
    void retour(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadChoisirTest(); return; }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ChoisirTest.fxml"));
            listViewObjectifs.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    void handleNavigateToChoisirTest(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadChoisirTest(); return; }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ChoisirTest.fxml"));
            listViewObjectifs.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    void handleNavigateToMesResultats(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadMesResultats(); return; }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MesResultats.fxml"));
            listViewObjectifs.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    void handleNavigateToMesObjectifs(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadMesObjectifs(); return; }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/MesObjectifs.fxml"));
            listViewObjectifs.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }
}