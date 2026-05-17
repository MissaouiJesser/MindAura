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
import tn.esprit.entities.QuestionReponse;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.QuestionReponseService;
import tn.esprit.utils.PaginationHelper;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class GestionQuestionsController implements Initializable, DashboardController.DashboardAware {

    @FXML private Label                     lblTestTitre;
    @FXML private Label                     lblTestType;
    @FXML private Label                     lblNbQuestions;
    @FXML private ListView<QuestionReponse> listViewQuestions;
    @FXML private javafx.scene.layout.HBox paginationContainer;

    private TestPsycho            testCourant;
    private List<QuestionReponse> listeQuestionsFull = new ArrayList<>();
    private int                   currentPage        = 0;
    private QuestionReponseService questionService;
    private DashboardController   dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // =========================================================================
    //  INITIALISATION
    // =========================================================================
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        questionService = new QuestionReponseService();

        listViewQuestions.setCellFactory(param -> new ListCell<QuestionReponse>() {
            @Override
            protected void updateItem(QuestionReponse question, boolean empty) {
                super.updateItem(question, empty);
                if (empty || question == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // ── Contenu textuel ──────────────────────────────────────────
                Text texte = new Text("Q" + question.getOrdreQuestion() + ":  " + question.getTexteQuestion());
                texte.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                texte.setWrappingWidth(600);

                Label lblType = new Label("📝 Type: " + question.getTypeQuestion());
                lblType.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 12; -fx-font-weight: bold;");

                VBox content = new VBox(5, texte, lblType);

                if (question.getOption1() != null && !question.getOption1().isEmpty()) {
                    StringBuilder opts = new StringBuilder("Options : ");
                    opts.append(question.getOption1()).append(" (").append(question.getScore1()).append(" pts)");
                    if (question.getOption2() != null && !question.getOption2().isEmpty())
                        opts.append("  •  ").append(question.getOption2()).append(" (").append(question.getScore2()).append(" pts)");
                    if (question.getOption3() != null && !question.getOption3().isEmpty())
                        opts.append("  •  ").append(question.getOption3()).append(" (").append(question.getScore3()).append(" pts)");

                    Label lblOptions = new Label(opts.toString());
                    lblOptions.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12;");
                    lblOptions.setWrapText(true);
                    content.getChildren().add(lblOptions);
                }
                content.setPadding(new Insets(4, 0, 4, 0));
                HBox.setHgrow(content, Priority.ALWAYS);

                // ── Boutons inline ───────────────────────────────────────────
                Button btnModifier  = new Button("✏️ Modifier");
                Button btnSupprimer = new Button("🗑️ Supprimer");

                btnModifier.getStyleClass().add("btn-icon-edit");
                btnSupprimer.getStyleClass().add("btn-icon-delete");

                btnModifier.setOnAction(e -> naviguerVersAjouterQuestion(question));
                btnSupprimer.setOnAction(e -> handleSupprimer(question));

                HBox actions = new HBox(8, btnModifier, btnSupprimer);
                actions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(16, content, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 12, 10, 12));

                setText(null);
                setGraphic(row);
            }
        });
    }

    // =========================================================================
    //  INIT DEPUIS L'EXTÉRIEUR
    // =========================================================================
    public void initTest(TestPsycho test) {
        this.testCourant = test;
        lblTestTitre.setText(test.getTitreTest());
        lblTestType.setText(test.getTypeTest());
        chargerQuestions();
    }

    // =========================================================================
    //  CHARGEMENT
    // =========================================================================
    private void chargerQuestions() {
        try {
            listeQuestionsFull = questionService.getQuestionsByTestId(testCourant.getIdTest());
            currentPage = 0;
            applyPagination();
            lblNbQuestions.setText("Total : " + listeQuestionsFull.size() + " question(s)");
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement : " + e.getMessage()).show();
        }
    }

    // =========================================================================
    //  LOGIQUE SUPPRIMER (inline)
    // =========================================================================
    private void handleSupprimer(QuestionReponse question) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la question " + question.getOrdreQuestion() + " ?");
        confirmation.setContentText("Cette action est irréversible.");

        Optional<ButtonType> resultat = confirmation.showAndWait();
        if (resultat.isPresent() && resultat.get() == ButtonType.OK) {
            try {
                questionService.delete(question);
                new Alert(Alert.AlertType.INFORMATION, "Question supprimée avec succès !").show();
                chargerQuestions();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
            }
        }
    }

    // =========================================================================
    //  ACTIONS FXML (barre d'outils)
    // =========================================================================
    @FXML
    void ajouterQuestion(ActionEvent event) {
        naviguerVersAjouterQuestion(null);
    }

    /** Conservé pour compatibilité — sélection dans la liste */
    @FXML
    void modifierQuestion(ActionEvent event) {
        QuestionReponse sel = listViewQuestions.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une question à modifier.").show();
            return;
        }
        naviguerVersAjouterQuestion(sel);
    }

    @FXML
    void supprimerQuestion(ActionEvent event) {
        QuestionReponse sel = listViewQuestions.getSelectionModel().getSelectedItem();
        if (sel == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner une question à supprimer.").show();
            return;
        }
        handleSupprimer(sel);
    }

    @FXML
    void actualiser(ActionEvent event) { chargerQuestions(); }

    @FXML
    void retour(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.navigateTo("AfficherTest.fxml", "Tests psychologiques", "Gestion des tests psychologiques");
        } else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/AfficherTest.fxml"));
                listViewQuestions.getScene().setRoot(root);
            } catch (IOException e) { throw new RuntimeException(e); }
        }
    }

    // =========================================================================
    //  NAVIGATION VERS AJOUTER / MODIFIER QUESTION
    // =========================================================================
    private void naviguerVersAjouterQuestion(QuestionReponse questionAModifier) {
        if (dashboardController != null) {
            AjouterQuestionController ctrl = (AjouterQuestionController)
                    dashboardController.loadViewAndGet(
                            "AjouterQuestion.fxml",
                            questionAModifier == null ? "Nouvelle Question" : "Modifier la Question",
                            "Questions du test : " + testCourant.getTitreTest());
            if (ctrl != null) {
                if (questionAModifier == null) ctrl.initTest(testCourant);
                else ctrl.initModification(testCourant, questionAModifier);
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterQuestion.fxml"));
                Parent root = loader.load();
                AjouterQuestionController ctrl = loader.getController();
                if (questionAModifier == null) ctrl.initTest(testCourant);
                else ctrl.initModification(testCourant, questionAModifier);
                listViewQuestions.getScene().setRoot(root);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
            }
        }
    }

    // =========================================================================
    //  PAGINATION
    // =========================================================================
    private void applyPagination() {
        int total      = listeQuestionsFull.size();
        int totalPages = PaginationHelper.getTotalPages(total);
        currentPage    = Math.min(currentPage, Math.max(0, totalPages - 1));

        listViewQuestions.setItems(FXCollections.observableArrayList(
                PaginationHelper.getPageItems(listeQuestionsFull, currentPage)));

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
}