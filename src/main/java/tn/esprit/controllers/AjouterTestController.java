package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.TestPsychoService;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterTestController implements Initializable, DashboardController.DashboardAware {

    @FXML private Label lblTitre;
    @FXML private TextField txtTitre;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<String> cmbType;
    @FXML private Spinner<Integer> spinDuree;
    @FXML private TextArea txtInstructions;
    @FXML private CheckBox chkActif;
    @FXML private Button btnEnregistrer;

    private TestPsycho testEnCours = null;
    private boolean modeModification = false;
    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cmbType.getItems().addAll(
                "Anxiété",
                "Dépression",
                "Stress",
                "Personnalité",
                "Intelligence émotionnelle",
                "Confiance en soi",
                "Autre"
        );

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 120, 10);
        spinDuree.setValueFactory(valueFactory);

        chkActif.setSelected(true);
    }

    public void initModification(TestPsycho test) {
        this.testEnCours = test;
        this.modeModification = true;

        lblTitre.setText("");
        btnEnregistrer.setText("Mettre à jour");

        txtTitre.setText(test.getTitreTest());

        if (test.getDescriptionTest() != null)
            txtDescription.setText(test.getDescriptionTest());

        if (test.getTypeTest() != null)
            cmbType.setValue(test.getTypeTest());

        // ✅ Corrigé : getDureeEstimee() au lieu de getDureeEstimeeTest()
        spinDuree.getValueFactory().setValue(test.getDureeEstimee());

        if (test.getInstructionsTest() != null)
            txtInstructions.setText(test.getInstructionsTest());

        chkActif.setSelected(test.isEstActif());
    }

    @FXML
    void enregistrer(ActionEvent event) {
        StringBuilder errors = new StringBuilder();

        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty()) {
            errors.append("- Le titre est obligatoire\n");
        } else if (txtTitre.getText().trim().length() < 5) {
            errors.append("- Le titre doit contenir au moins 5 caractères\n");
        } else if (txtTitre.getText().trim().length() > 200) {
            errors.append("- Le titre ne peut pas dépasser 200 caractères\n");
        }

        if (cmbType.getValue() == null)
            errors.append("- Le type de test est obligatoire\n");

        if (spinDuree.getValue() == null || spinDuree.getValue() <= 0) {
            errors.append("- La durée estimée doit être supérieure à 0\n");
        } else if (spinDuree.getValue() > 120) {
            errors.append("- La durée estimée ne peut pas dépasser 120 minutes\n");
        }

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return;
        }

        TestPsychoService testPsychoService = new TestPsychoService();

        try {
            if (modeModification) {
                testEnCours.setTitreTest(txtTitre.getText().trim());
                testEnCours.setDescriptionTest(txtDescription.getText().trim());
                testEnCours.setTypeTest(cmbType.getValue());
                // ✅ Corrigé : setDureeEstimee() au lieu de setDureeEstimeeTest()
                testEnCours.setDureeEstimee(spinDuree.getValue());
                testEnCours.setInstructionsTest(txtInstructions.getText().trim());
                testEnCours.setEstActif(chkActif.isSelected());

                testPsychoService.modifier(testEnCours);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("Test modifié avec succès !");
                alert.show();

            } else {
                TestPsycho nouveauTest = new TestPsycho(
                        txtTitre.getText().trim(),
                        txtDescription.getText().trim(),
                        cmbType.getValue(),
                        spinDuree.getValue(),
                        txtInstructions.getText().trim()
                );
                nouveauTest.setEstActif(chkActif.isSelected());

                testPsychoService.addMeth2(nouveauTest);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("Test ajouté avec succès !");
                alert.show();
            }

            naviguerVersListe();

        } catch (SQLException | IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erreur : " + e.getMessage());
            alert.show();
            e.printStackTrace();
        }
    }

    @FXML
    void annuler(ActionEvent event) {
        try {
            naviguerVersListe();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ✅ Méthode extraite pour éviter la duplication
    private void naviguerVersListe() throws IOException {
        if (dashboardController != null) {
            dashboardController.navigateTo(
                    "AfficherTest.fxml",
                    "Tests psychologiques",
                    "Gestion des tests psychologiques");
        } else {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherTest.fxml"));
            txtTitre.getScene().setRoot(root);
        }
    }
}