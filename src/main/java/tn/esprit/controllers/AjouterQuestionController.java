package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.entities.QuestionReponse;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.QuestionReponseService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterQuestionController implements Initializable, DashboardController.DashboardAware {

    @FXML private Label            lblTitre;
    @FXML private Label            lblTestInfo;
    @FXML private TextArea         txtQuestion;
    @FXML private ComboBox<String> cmbType;
    @FXML private Spinner<Integer> spinOrdre;
    @FXML private CheckBox         chkObligatoire;
    @FXML private VBox             vboxOptions;
    @FXML private TextField        txtOption1;
    @FXML private TextField        txtOption2;
    @FXML private TextField        txtOption3;
    @FXML private TextField        txtOption4;
    @FXML private TextField        txtOption5;
    @FXML private Spinner<Integer> spinScore1;
    @FXML private Spinner<Integer> spinScore2;
    @FXML private Spinner<Integer> spinScore3;
    @FXML private Spinner<Integer> spinScore4;
    @FXML private Spinner<Integer> spinScore5;
    @FXML private Button           btnEnregistrer;

    // ─── État ─────────────────────────────────────────────────────────────────
    private TestPsycho      testCourant;
    private QuestionReponse questionEnCours  = null;
    private boolean         modeModification = false;

    /** Injecté automatiquement par DashboardController.loadView() via DashboardAware */
    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // =========================================================================
    //  INITIALISATION JavaFX
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cmbType.getItems().addAll("choix_multiple", "oui_non");
        cmbType.setValue("choix_multiple");

        spinOrdre.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        spinScore1.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        spinScore2.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        spinScore3.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        spinScore4.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        spinScore5.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));

        chkObligatoire.setSelected(true);
    }

    // =========================================================================
    //  INIT DEPUIS L'EXTÉRIEUR
    // =========================================================================

    /** Mode ajout : appelé après loadView() pour passer le test */
    public void initTest(TestPsycho test) {
        this.testCourant = test;
        lblTestInfo.setText(test.getTitreTest() + " - " + test.getTypeTest());

        try {
            QuestionReponseService service = new QuestionReponseService();
            int nbQuestions = service.countQuestionsByTest(test.getIdTest());
            spinOrdre.getValueFactory().setValue(nbQuestions + 1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /** Mode modification */
    public void initModification(TestPsycho test, QuestionReponse question) {
        this.testCourant      = test;
        this.questionEnCours  = question;
        this.modeModification = true;

        lblTitre.setText("Modifier la Question");
        btnEnregistrer.setText("Mettre à jour");
        lblTestInfo.setText(test.getTitreTest() + " - " + test.getTypeTest());

        txtQuestion.setText(question.getTexteQuestion());
        cmbType.setValue(question.getTypeQuestion());
        spinOrdre.getValueFactory().setValue(question.getOrdreQuestion());
        chkObligatoire.setSelected(question.isEstObligatoire());

        if (question.getOption1() != null) { txtOption1.setText(question.getOption1()); spinScore1.getValueFactory().setValue(question.getScore1()); }
        if (question.getOption2() != null) { txtOption2.setText(question.getOption2()); spinScore2.getValueFactory().setValue(question.getScore2()); }
        if (question.getOption3() != null) { txtOption3.setText(question.getOption3()); spinScore3.getValueFactory().setValue(question.getScore3()); }
        if (question.getOption4() != null) { txtOption4.setText(question.getOption4()); spinScore4.getValueFactory().setValue(question.getScore4()); }
        if (question.getOption5() != null) { txtOption5.setText(question.getOption5()); spinScore5.getValueFactory().setValue(question.getScore5()); }

        changerTypeQuestion(null);
    }

    // =========================================================================
    //  ACTIONS FXML
    // =========================================================================

    @FXML
    void changerTypeQuestion(ActionEvent event) {
        boolean libre = "texte_libre".equals(cmbType.getValue());
        vboxOptions.setVisible(!libre);
        vboxOptions.setManaged(!libre);
    }

    @FXML
    void enregistrer(ActionEvent event) {
        // ── Validation ────────────────────────────────────────────────────────
        StringBuilder errors = new StringBuilder();

        if (txtQuestion.getText() == null || txtQuestion.getText().trim().isEmpty()) {
            errors.append("- Le texte de la question est obligatoire\n");
        } else if (txtQuestion.getText().trim().length() < 10) {
            errors.append("- Le texte de la question doit contenir au moins 10 caractères\n");
        }

        if (cmbType.getValue() == null) {
            errors.append("- Le type de question est obligatoire\n");
        }

        if (cmbType.getValue() != null && !"texte_libre".equals(cmbType.getValue())) {
            boolean hasOption1 = txtOption1.getText() != null && !txtOption1.getText().trim().isEmpty();
            boolean hasOption2 = txtOption2.getText() != null && !txtOption2.getText().trim().isEmpty();
            if (!hasOption1 || !hasOption2) {
                errors.append("- Au moins 2 options de réponse sont obligatoires\n");
            }
        }

        if (spinOrdre.getValue() == null || spinOrdre.getValue() <= 0) {
            errors.append("- L'ordre doit être supérieur à 0\n");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return;
        }

        // ── Persistance ───────────────────────────────────────────────────────
        QuestionReponseService service = new QuestionReponseService();
        try {
            if (modeModification) {
                // ✅ Mise à jour de la question existante
                questionEnCours.setTexteQuestion(txtQuestion.getText().trim());
                questionEnCours.setTypeQuestion(cmbType.getValue());
                questionEnCours.setOrdreQuestion(spinOrdre.getValue());
                questionEnCours.setEstObligatoire(chkObligatoire.isSelected());
                questionEnCours.setOption1(txtOption1.getText().trim()); questionEnCours.setScore1(spinScore1.getValue());
                questionEnCours.setOption2(txtOption2.getText().trim()); questionEnCours.setScore2(spinScore2.getValue());
                questionEnCours.setOption3(txtOption3.getText().trim()); questionEnCours.setScore3(spinScore3.getValue());
                questionEnCours.setOption4(txtOption4.getText().trim()); questionEnCours.setScore4(spinScore4.getValue());
                questionEnCours.setOption5(txtOption5.getText().trim()); questionEnCours.setScore5(spinScore5.getValue());
                service.modifier(questionEnCours);
                new Alert(Alert.AlertType.INFORMATION, "Question modifiée avec succès !").show();
            } else {
                // ✅ Nouvelle question — utilise idTestId (clé étrangère id_test_id)
                QuestionReponse nouvelleQuestion = new QuestionReponse(
                        testCourant.getIdTest(),          // → idTestId (FK id_test_id)
                        txtQuestion.getText().trim(),
                        cmbType.getValue(),
                        spinOrdre.getValue(),
                        txtOption1.getText().trim(), spinScore1.getValue(),
                        txtOption2.getText().trim(), spinScore2.getValue(),
                        txtOption3.getText().trim(), spinScore3.getValue(),
                        txtOption4.getText().trim(), spinScore4.getValue(),
                        txtOption5.getText().trim(), spinScore5.getValue(),
                        chkObligatoire.isSelected()       // ✅ estObligatoire passé au constructeur
                );
                service.addMeth2(nouvelleQuestion);
                new Alert(Alert.AlertType.INFORMATION, "Question ajoutée avec succès !").show();
            }

            retourGestionQuestions();

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
            e.printStackTrace();
        }
    }

    @FXML
    void annuler(ActionEvent event) {
        retourGestionQuestions();
    }

    // =========================================================================
    //  NAVIGATION — retour à GestionQuestions
    // =========================================================================

    private void retourGestionQuestions() {
        if (dashboardController != null) {
            GestionQuestionsController ctrl = (GestionQuestionsController)
                    dashboardController.loadViewAndGet(
                            "GestionQuestions.fxml",
                            "Gestion des Questions",
                            "Questions du test : " + testCourant.getTitreTest()
                    );
            if (ctrl != null) {
                ctrl.initTest(testCourant);
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/GestionQuestions.fxml"));
                Parent root = loader.load();
                GestionQuestionsController controller = loader.getController();
                controller.initTest(testCourant);
                txtQuestion.getScene().setRoot(root);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
                e.printStackTrace();
            }
        }
    }
}