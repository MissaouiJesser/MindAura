package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.entities.QuestionReponse;
import tn.esprit.entities.ReponseClient;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.QuestionReponseService;
import tn.esprit.services.ReponseClientService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import tn.esprit.services.EmailRepService;

public class PasserTestController implements Initializable {

    // ─── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Label       lblTestTitre;
    @FXML private Label       lblTestType;
    @FXML private Label       lblNumeroQuestion;
    @FXML private Label       lblTexteQuestion;
    @FXML private Label       lblProgression;
    @FXML private Label       userNameLabel;
    @FXML private ProgressBar progressBar;
    @FXML private VBox        vboxQuestion;
    @FXML private VBox        vboxReponses;
    @FXML private Button      btnPrecedent;
    @FXML private Button      btnSuivant;
    @FXML private Button      btnTerminer;

    // ─── État ─────────────────────────────────────────────────────────────────
    private TestPsycho            testCourant;
    private List<QuestionReponse> listeQuestions;
    private int                   indexQuestionActuelle = 0;
    private HashMap<Integer, ReponseClient> reponsesUtilisateur;
    private ToggleGroup           toggleGroup;
    private TextArea              textAreaReponse;

    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController ctrl) {
        this.parentHomeController = ctrl;
    }

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reponsesUtilisateur = new HashMap<>();

        if (userNameLabel != null && SessionManager.isLoggedIn()
                && SessionManager.getCurrentUser() != null) {
            userNameLabel.setText(SessionManager.getNomComplet());
        }
    }

    public void initTest(TestPsycho test) {
        this.testCourant = test;
        lblTestTitre.setText(test.getTitreTest());
        lblTestType.setText("Type: " + test.getTypeTest()
                + " | Durée: " + test.getDureeEstimee() + " min");

        chargerQuestions();

        if (listeQuestions != null && !listeQuestions.isEmpty()) {
            afficherQuestion(0);
        } else {
            new Alert(Alert.AlertType.WARNING, "Ce test ne contient aucune question.").show();
        }
    }

    // =========================================================================
    //  CHARGEMENT & AFFICHAGE QUESTIONS
    // =========================================================================

    private void chargerQuestions() {
        try {
            // ✅ getIdTest() reste inchangé — c'est le getter de TestPsycho, pas de ReponseClient
            listeQuestions = new QuestionReponseService()
                    .getQuestionsByTestId(testCourant.getIdTest());
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur lors du chargement des questions : " + e.getMessage()).show();
        }
    }

    private void afficherQuestion(int index) {
        if (index < 0 || index >= listeQuestions.size()) return;

        indexQuestionActuelle = index;
        QuestionReponse question = listeQuestions.get(index);

        lblNumeroQuestion.setText("Question " + (index + 1) + "/" + listeQuestions.size());
        lblTexteQuestion.setText(question.getTexteQuestion());
        lblProgression.setText("Question " + (index + 1) + "/" + listeQuestions.size());
        progressBar.setProgress((double) (index + 1) / listeQuestions.size());

        vboxReponses.getChildren().clear();

        if ("texte_libre".equals(question.getTypeQuestion())) {
            afficherReponseTexteLibre(question);
        } else {
            afficherOptionsChoix(question);
        }

        btnPrecedent.setDisable(index == 0);
        btnSuivant.setVisible(index < listeQuestions.size() - 1);
        btnTerminer.setVisible(index == listeQuestions.size() - 1);
    }

    private void afficherOptionsChoix(QuestionReponse question) {
        toggleGroup = new ToggleGroup();
        // ✅ getIdQuestionReponse() — getter de QuestionReponse, inchangé
        ReponseClient existing = reponsesUtilisateur.get(question.getIdQuestionReponse());

        addRadio(question.getOption1(), question.getScore1(), existing);
        addRadio(question.getOption2(), question.getScore2(), existing);
        addRadio(question.getOption3(), question.getScore3(), existing);
        addRadio(question.getOption4(), question.getScore4(), existing);
        addRadio(question.getOption5(), question.getScore5(), existing);
    }

    private void addRadio(String texte, int score, ReponseClient existing) {
        if (texte == null || texte.isEmpty()) return;
        RadioButton radio = creerRadioButton(texte, score);
        if (existing != null && texte.equals(existing.getOptionChoisie())) radio.setSelected(true);
        vboxReponses.getChildren().add(radio);
    }

    private RadioButton creerRadioButton(String texte, int score) {
        RadioButton radio = new RadioButton(texte);
        radio.setToggleGroup(toggleGroup);
        radio.setUserData(score);
        radio.setPrefWidth(650);
        radio.setWrapText(true);

        String styleNormal = "-fx-font-size:16px;-fx-padding:18 22;-fx-background-color:white;" +
                "-fx-background-radius:10;-fx-border-color:#e8ecf0;-fx-border-width:2;" +
                "-fx-border-radius:10;-fx-text-fill:#1A1A2E;-fx-cursor:hand;";
        String styleHover  = "-fx-font-size:16px;-fx-padding:18 22;-fx-background-color:#D8EAD8;" +
                "-fx-background-radius:10;-fx-border-color:#2D6A4F;-fx-border-width:2;" +
                "-fx-border-radius:10;-fx-text-fill:#1A1A2E;-fx-cursor:hand;";
        String styleSelect = "-fx-font-size:16px;-fx-padding:18 22;-fx-background-color:#95C9A8;" +
                "-fx-background-radius:10;-fx-border-color:#2D6A4F;-fx-border-width:2;" +
                "-fx-border-radius:10;-fx-text-fill:#1A1A2E;-fx-font-weight:bold;-fx-cursor:hand;";

        radio.setStyle(styleNormal);
        radio.setOnMouseEntered(e -> { if (!radio.isSelected()) radio.setStyle(styleHover);  });
        radio.setOnMouseExited( e -> { if (!radio.isSelected()) radio.setStyle(styleNormal); });
        radio.selectedProperty().addListener((obs, o, n) ->
                radio.setStyle(n ? styleSelect : styleNormal));
        return radio;
    }

    private void afficherReponseTexteLibre(QuestionReponse question) {
        textAreaReponse = new TextArea();
        textAreaReponse.setPromptText("Écrivez votre réponse ici...");
        textAreaReponse.setPrefRowCount(8);
        textAreaReponse.setWrapText(true);
        textAreaReponse.setPrefWidth(650);
        textAreaReponse.setMinHeight(160);
        textAreaReponse.setStyle("-fx-font-size:14px;-fx-padding:16;-fx-background-color:white;" +
                "-fx-background-radius:10;-fx-border-color:#e8ecf0;-fx-border-width:2;" +
                "-fx-border-radius:10;-fx-text-fill:#1A1A2E;");

        ReponseClient existing = reponsesUtilisateur.get(question.getIdQuestionReponse());
        if (existing != null && existing.getReponseTexteLibre() != null) {
            textAreaReponse.setText(existing.getReponseTexteLibre());
        }
        vboxReponses.getChildren().add(textAreaReponse);
    }

    // =========================================================================
    //  SAUVEGARDE RÉPONSE
    // =========================================================================

    private void sauvegarderReponse() {
        QuestionReponse question = listeQuestions.get(indexQuestionActuelle);
        ReponseClient reponse = new ReponseClient();

        // ✅ setIdTestId() — nouveau setter (FK id_test_id)
        reponse.setIdTestId(testCourant.getIdTest());

        // ✅ setIdQuestionReponseId() — nouveau setter (FK id_question_reponse_id)
        reponse.setIdQuestionReponseId(question.getIdQuestionReponse());

        // ✅ getId_utilisateur() retourne String → conversion en int pour utilisateur_id (int en DB)
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
            try {
                int userId = Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur());
                reponse.setUtilisateurId(userId);
            } catch (NumberFormatException e) {
                System.err.println("ID utilisateur invalide : " + e.getMessage());
            }
        }

        if ("texte_libre".equals(question.getTypeQuestion())) {
            if (textAreaReponse != null && !textAreaReponse.getText().trim().isEmpty()) {
                reponse.setReponseTexteLibre(textAreaReponse.getText().trim());
                reponse.setScoreObtenu(0);
                reponsesUtilisateur.put(question.getIdQuestionReponse(), reponse);
            }
        } else {
            if (toggleGroup != null && toggleGroup.getSelectedToggle() != null) {
                RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
                reponse.setOptionChoisie(selected.getText());
                reponse.setScoreObtenu((Integer) selected.getUserData());
                reponsesUtilisateur.put(question.getIdQuestionReponse(), reponse);
            }
        }
    }

    // =========================================================================
    //  ACTIONS NAVIGATION QUESTIONS
    // =========================================================================

    @FXML void questionSuivante(ActionEvent event) {
        sauvegarderReponse();
        if (indexQuestionActuelle < listeQuestions.size() - 1)
            afficherQuestion(indexQuestionActuelle + 1);
    }

    @FXML void questionPrecedente(ActionEvent event) {
        sauvegarderReponse();
        if (indexQuestionActuelle > 0)
            afficherQuestion(indexQuestionActuelle - 1);
    }

    @FXML
    void terminerTest(ActionEvent event) {
        sauvegarderReponse();

        if (reponsesUtilisateur.size() < listeQuestions.size()) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Questions non répondues");
            confirmation.setHeaderText("Vous n'avez pas répondu à toutes les questions");
            confirmation.setContentText("Voulez-vous quand même terminer le test ?");
            if (confirmation.showAndWait().get() != ButtonType.OK) return;
        }

        try {
            ReponseClientService reponseService = new ReponseClientService();
            for (ReponseClient reponse : reponsesUtilisateur.values()) {
                reponseService.addMeth2(reponse);
            }

            int scoreTotal = reponsesUtilisateur.values().stream()
                    .mapToInt(ReponseClient::getScoreObtenu).sum();

            // ── ✅ ENVOI EMAIL ──────────────────────────────────────────────────
            if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
                String emailUser   = SessionManager.getCurrentUser().getEmail_utilisateur();     // ← getter email
                String nomComplet  = SessionManager.getNomComplet();

                // Construire la liste des lignes à partir des réponses sauvegardées
                List<EmailRepService.LigneResultat> lignes = new ArrayList<>();
                for (QuestionReponse question : listeQuestions) {
                    ReponseClient rep = reponsesUtilisateur.get(question.getIdQuestionReponse());
                    if (rep != null) {
                        String reponseAffichee = "texte_libre".equals(question.getTypeQuestion())
                                ? rep.getReponseTexteLibre()
                                : rep.getOptionChoisie();
                        lignes.add(new EmailRepService.LigneResultat(
                                question.getTexteQuestion(),
                                reponseAffichee,
                                rep.getScoreObtenu()
                        ));
                    }
                }

                EmailRepService.envoyerResultatsTest(
                        emailUser,
                        nomComplet,
                        testCourant.getTitreTest(),
                        scoreTotal,
                        lignes
                );
            }
            // ───────────────────────────────────────────────────────────────────

            Alert succes = new Alert(Alert.AlertType.INFORMATION);
            succes.setTitle("Test Terminé");
            succes.setHeaderText("Félicitations !");
            succes.setContentText("Vous avez terminé le test.\nScore total : " + scoreTotal
                    + " points\n📧 Un récapitulatif a été envoyé à votre adresse email.");
            succes.showAndWait();

            if (parentHomeController != null) {
                parentHomeController.loadObjectifsRecommandes(testCourant, scoreTotal);
            } else {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/ObjectifsRecommandes.fxml"));
                Parent root = loader.load();
                ObjectifsRecommandesController ctrl = loader.getController();
                ctrl.initTest(testCourant, scoreTotal);
                lblTestTitre.getScene().setRoot(root);
            }

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR,
                    "Erreur lors de l'enregistrement : " + e.getMessage()).show();
        } catch (IOException ex) {
            ex.printStackTrace();
            retour(null);
        }
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================

    @FXML void retour(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.loadChoisirTest();
        else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/ChoisirTest.fxml"));
                lblTestTitre.getScene().setRoot(root);
            } catch (IOException e) { throw new RuntimeException(e); }
        }
    }

    @FXML void handleNavigateToMesResultats(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.loadMesResultats();
        else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/MesResultats.fxml"));
                lblTestTitre.getScene().setRoot(root);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML void handleNavigateToMesObjectifs(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.loadMesObjectifs();
        else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/MesObjectifs.fxml"));
                lblTestTitre.getScene().setRoot(root);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}