package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Reponse;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.ProfanityFilterService;
import tn.esprit.services.ReponseService;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Contrôleur pour la modification d'une réponse (commentaire).
 * Seuls le contenu et la note sont modifiables ; les IDs ne sont pas affichés ni modifiés.
 */
public class ModifierReponse implements DashboardController.DashboardAware {

    private DashboardController dashboardController;
    private Runnable retourCallback;

    @FXML
    private Label lblId;

    @FXML
    private TextArea taContenu;

    @FXML
    private Spinner<Integer> spRate;

    @FXML
    private Button btnValider;

    @FXML
    private Button btnAnnuler;

    @FXML
    private Label lblMessage;

    private ReponseService reponseService;
    private Reponse reponseEnCours;
    private Reclamation reclamationEnCours;
    private Stage currentStage;

    @FXML
    public void initialize() {
        try {
            reponseService = new ReponseService();
        } catch (Exception e) {
            afficherMessage("❌ Erreur de connexion BD", "#f44336");
            return;
        }

        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0);
        if (spRate != null) spRate.setValueFactory(valueFactory);

        if (btnValider != null) btnValider.setOnAction(e -> modifierReponse());
        if (btnAnnuler != null) btnAnnuler.setOnAction(e -> retournerAReclamation());
    }

    public void remplirAvecReponse(Reponse reponse) {
        this.reponseEnCours = reponse;

        if (lblId != null) lblId.setText("Commentaire #" + reponse.getIdReponse());
        if (taContenu != null) taContenu.setText(reponse.getContenuReponse());
        if (spRate != null && spRate.getValueFactory() != null) {
            spRate.getValueFactory().setValue((int) reponse.getRateReponse());
        }
        afficherMessage("✓ Données chargées. Modifiez contenu et note puis validez.", "#4CAF50");
    }

    public void setReclamation(Reclamation reclamation) {
        this.reclamationEnCours = reclamation;
    }

    public void setCurrentStage(Stage stage) {
        this.currentStage = stage;
    }

    public void setRetourCallback(Runnable retourCallback) {
        this.retourCallback = retourCallback;
    }

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    public void setListeController(Object listeController) {
        // Cette méthode est gardée pour compatibilité avec ListeResponse.java
        // mais n'est pas utilisée dans AdminReclamationDetail
    }

    private void modifierReponse() {
        if (reponseEnCours == null) {
            afficherMessage("❌ Pas de réponse à modifier", "#f44336");
            return;
        }
        if (!validerFormulaire()) return;

        try {
            String contenu = taContenu.getText().trim();
            String contenuFiltre = ProfanityFilterService.filter(contenu);

            reponseEnCours.setContenuReponse(contenuFiltre);
            reponseEnCours.setRateReponse(spRate.getValue() != null ? spRate.getValue().floatValue() : 0f);
            // id_utilisateur et id_reclamation restent inchangés

            reponseService.modifier(reponseEnCours);
            afficherMessage("✓ Commentaire modifié avec succès !", "#4CAF50");
            retournerAReclamation();

        } catch (SQLException e) {
            afficherMessage("❌ Erreur BD: " + e.getMessage(), "#f44336");
            e.printStackTrace();
        }
    }

    private boolean validerFormulaire() {
        if (taContenu.getText().trim().isEmpty()) {
            afficherMessage("❌ Contenu obligatoire (min 5 caractères)", "#f44336");
            return false;
        }
        if (taContenu.getText().trim().length() < 5) {
            afficherMessage("❌ Contenu: minimum 5 caractères", "#f44336");
            return false;
        }
        return true;
    }

    private void retournerAReclamation() {
        // Dans le Dashboard : Valider / Annuler → garder la sidebar (même logique que Statistiques / Retour)
        if (dashboardController != null && retourCallback != null) {
            retourCallback.run();
            return;
        }

        if (reclamationEnCours == null || currentStage == null) {
            fermerFenetre();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminReclamationDetail.fxml"));
            Parent root = loader.load();
            AdminReclamationDetailController ctrl = loader.getController();
            ctrl.setReclamation(reclamationEnCours);
            ctrl.setRetourCallback(() -> {});
            ctrl.setCurrentStage(currentStage);
            ctrl.afficher();

            currentStage.setScene(new Scene(root));
            currentStage.getScene().getStylesheets().add("/app.css");
            currentStage.setTitle("Détail de la réclamation");
        } catch (IOException ex) {
            ex.printStackTrace();
            fermerFenetre();
        }
    }

    private void fermerFenetre() {
        Stage stage = currentStage;
        if (stage == null && btnAnnuler != null && btnAnnuler.getScene() != null) {
            stage = (Stage) btnAnnuler.getScene().getWindow();
        }
        if (stage != null) {
            stage.close();
        }
    }

    private void afficherMessage(String message, String couleur) {
        if (lblMessage != null) {
            lblMessage.setText(message);
            lblMessage.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 12; -fx-font-weight: bold;");
        }
    }
}