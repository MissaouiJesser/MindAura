package tn.esprit.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Objectif;
import tn.esprit.services.ObjectifService;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class AjouterObjectifController implements Initializable, DashboardController.DashboardAware {

    @FXML private Label            lblTitre;
    @FXML private TextField        txtTitre;
    @FXML private TextArea         txtDescription;
    @FXML private ComboBox<String> cmbType;
    @FXML private Spinner<Integer> spinDuree;
    @FXML private ComboBox<String> cmbDifficulte;
    @FXML private ComboBox<String> cmbTypeTest;
    @FXML private ComboBox<String> cmbNiveauRecommande;
    @FXML private Spinner<Integer> spinScoreMin;
    @FXML private Spinner<Integer> spinScoreMax;
    @FXML private CheckBox         chkPublic;
    @FXML private Button           btnEnregistrer;

    private Objectif         objectifEnCours  = null;
    private boolean          modeModification = false;
    private String           pageRetour       = "/GestionObjectifs.fxml";

    // ✅ idUtilisateur est int (id_utilisateur est int(11) en DB)
    private int              idUtilisateur    = 0;

    // ── Contextes de navigation ──────────────────────────────────────────────
    private DashboardController dashboardController;
    private UserHomeController  userHomeController;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    public void setUserHomeController(UserHomeController ctrl) {
        this.userHomeController = ctrl;
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        spinDuree.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 30));
        spinScoreMin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        spinScoreMax.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 100));

        cmbType.setValue("Santé mentale");
        cmbDifficulte.setValue("Moyen");
        cmbNiveauRecommande.setValue("Modéré");
    }

    public void setPageRetour(String pageRetour) {
        this.pageRetour = pageRetour;
    }

    // ✅ idUtilisateur est maintenant int
    public void setIdUtilisateur(int idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public void initModification(Objectif objectif) {
        this.objectifEnCours = objectif;
        this.modeModification = true;

        lblTitre.setText("Modifier l'Objectif");
        btnEnregistrer.setText("Mettre à jour");

        txtTitre.setText(objectif.getTitre());
        txtDescription.setText(objectif.getDescription());
        cmbType.setValue(objectif.getCategorie());
        spinDuree.getValueFactory().setValue(objectif.getDureeEstimee());
        cmbDifficulte.setValue(objectif.getDifficulte());
        if (objectif.getTypeTest() != null)         cmbTypeTest.setValue(objectif.getTypeTest());
        if (objectif.getNiveauRecommande() != null) cmbNiveauRecommande.setValue(objectif.getNiveauRecommande());
        spinScoreMin.getValueFactory().setValue(objectif.getScoreMin());
        spinScoreMax.getValueFactory().setValue(objectif.getScoreMax());
        chkPublic.setSelected(objectif.isEstPublic());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ENREGISTRER
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void enregistrer(ActionEvent event) {
        // ── Validation ───────────────────────────────────────────────────────
        StringBuilder errors = new StringBuilder();

        if (txtTitre.getText() == null || txtTitre.getText().trim().isEmpty())
            errors.append("- Le titre est obligatoire\n");
        else if (txtTitre.getText().trim().length() < 5)
            errors.append("- Le titre doit contenir au moins 5 caractères\n");
        else if (txtTitre.getText().trim().length() > 200)
            errors.append("- Le titre ne peut pas dépasser 200 caractères\n");

        if (cmbType.getValue() == null)
            errors.append("- La catégorie est obligatoire\n");

        if (spinDuree.getValue() == null || spinDuree.getValue() <= 0)
            errors.append("- La durée estimée doit être supérieure à 0\n");
        else if (spinDuree.getValue() > 365)
            errors.append("- La durée estimée ne peut pas dépasser 365 jours\n");

        if (cmbDifficulte.getValue() == null)
            errors.append("- La difficulté est obligatoire\n");

        if (spinScoreMin.getValue() != null && spinScoreMax.getValue() != null
                && spinScoreMin.getValue() > spinScoreMax.getValue())
            errors.append("- Le score minimum ne peut pas être supérieur au score maximum\n");

        if (errors.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Validation");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(errors.toString());
            alert.showAndWait();
            return;
        }

        // ── Persistance ──────────────────────────────────────────────────────
        ObjectifService objectifService = new ObjectifService();
        try {
            if (modeModification) {
                // ── Mise à jour des champs existants ─────────────────────────
                objectifEnCours.setTitre(txtTitre.getText().trim());
                objectifEnCours.setDescription(txtDescription.getText().trim());
                objectifEnCours.setCategorie(cmbType.getValue());
                objectifEnCours.setDureeEstimee(spinDuree.getValue());
                objectifEnCours.setDifficulte(cmbDifficulte.getValue());
                objectifEnCours.setTypeTest(cmbTypeTest.getValue());
                objectifEnCours.setNiveauRecommande(cmbNiveauRecommande.getValue());
                objectifEnCours.setScoreMin(spinScoreMin.getValue());
                objectifEnCours.setScoreMax(spinScoreMax.getValue());
                objectifEnCours.setEstPublic(chkPublic.isSelected());
                objectifService.modifier(objectifEnCours);
                showInfo("Objectif modifié avec succès !");

            } else {
                // ── date_echeance obligatoire en DB → 30 jours par défaut ────
                Date dateEcheance = new Date(System.currentTimeMillis()
                        + (long) spinDuree.getValue() * 24 * 60 * 60 * 1000);

                if (idUtilisateur != 0) {
                    // ✅ Contexte PATIENT — constructeur patient (int idUtilisateur)
                    Objectif nouvelObjectif = new Objectif(
                            idUtilisateur,
                            txtTitre.getText().trim(),
                            txtDescription.getText().trim(),
                            cmbType.getValue(),
                            cmbDifficulte.getValue(),
                            dateEcheance
                    );
                    nouvelObjectif.setTypeTest(cmbTypeTest.getValue());
                    nouvelObjectif.setNiveauRecommande(cmbNiveauRecommande.getValue());
                    nouvelObjectif.setScoreMin(spinScoreMin.getValue());
                    nouvelObjectif.setScoreMax(spinScoreMax.getValue());
                    objectifService.addMeth2(nouvelObjectif);

                } else {
                    // ✅ Contexte ADMIN — constructeur admin
                    Objectif nouvelObjectif = new Objectif(
                            txtTitre.getText().trim(),
                            txtDescription.getText().trim(),
                            cmbTypeTest.getValue(),
                            cmbNiveauRecommande.getValue(),
                            spinScoreMin.getValue(),
                            spinScoreMax.getValue(),
                            cmbType.getValue(),
                            spinDuree.getValue(),
                            cmbDifficulte.getValue(),
                            dateEcheance
                    );
                    nouvelObjectif.setEstPublic(chkPublic.isSelected());
                    // ✅ getId_utilisateur() retourne String → Integer.parseInt
                    try {
                        nouvelObjectif.setIdUtilisateur(
                                Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur()));
                    } catch (NumberFormatException ignored) {}
                    objectifService.addMeth2(nouvelObjectif);
                }

                showInfo("Objectif ajouté avec succès !");
            }

            retournerVersListe(event);

        } catch (SQLException | IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANNULER
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    void annuler(ActionEvent event) {
        try {
            retournerVersListe(event);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOGIQUE DE RETOUR (3 contextes)
    // ─────────────────────────────────────────────────────────────────────────

    private void retournerVersListe(ActionEvent event) throws IOException {
        if (dashboardController != null && "/GestionObjectifs.fxml".equals(pageRetour)) {
            dashboardController.navigateTo("GestionObjectifs.fxml", "Objectifs", "Gestion des objectifs");
            dashboardController.maintainActiveButton("objectifs");
            return;
        }
        if (userHomeController != null) {
            userHomeController.loadMesObjectifs();
            return;
        }
        naviguerVers(event, pageRetour);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private void naviguerVers(ActionEvent event, String fxmlPath) throws IOException {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        double w = stage.getWidth(); double h = stage.getHeight(); boolean max = stage.isMaximized();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setScene(new Scene(root));
        stage.setWidth(w); stage.setHeight(h);
        if (max) stage.setMaximized(true);
        stage.show();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}