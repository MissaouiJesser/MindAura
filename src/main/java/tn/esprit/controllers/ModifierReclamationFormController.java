package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import tn.esprit.entities.Categorie;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.CategorieService;
import tn.esprit.services.ProfanityFilterService;
import tn.esprit.services.ReclamationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formulaire de modification de réclamation (réutilisable Client / Admin).
 * Le Statut est désactivé pour le Client (seul l'Admin peut le modifier).
 */
public class ModifierReclamationFormController implements DashboardController.DashboardAware {

    private DashboardController dashboardController;

    @FXML private Label           lblId;
    @FXML private TextField       tfSujet;
    @FXML private TextArea        taDescription;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private ComboBox<String> cbStatut;
    @FXML private Spinner<Integer> spRate;
    @FXML private Button          btnValider;
    @FXML private Button          btnAnnuler;
    @FXML private Button          btnRetour;
    @FXML private Label           lblMessage;

    private ReclamationService reclamationService;
    private CategorieService   categorieService;
    private Reclamation        reclamationEnCours;
    private Runnable           retourCallback;
    private Stage              currentStage;
    private boolean            isAdmin = false;

    // ✅ Cache id → nom catégorie
    private final Map<Integer, String> categorieCache    = new HashMap<>();
    // ✅ Cache nom → id (pour sauvegarder le choix du ComboBox)
    private final Map<String, Integer> nomToCategorieId  = new HashMap<>();

    // ---------------------------------------------------------------
    // SETTERS
    // ---------------------------------------------------------------

    public void setReclamation(Reclamation r)          { this.reclamationEnCours = r; }
    public void setRetourCallback(Runnable r)          { this.retourCallback = r; }
    public void setCurrentStage(Stage stage)           { this.currentStage = stage; }

    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
        if (cbStatut != null) cbStatut.setDisable(!isAdmin);
    }

    // ---------------------------------------------------------------
    // INITIALISATION
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        try {
            reclamationService = new ReclamationService();
            categorieService   = new CategorieService();
        } catch (Exception e) {
            if (lblMessage != null) lblMessage.setText("Erreur de connexion.");
        }

        // ✅ CORRECTION : charger les catégories depuis la BDD au lieu de valeurs hardcodées
        chargerCategoriesEnCache();

        if (cbStatut != null) cbStatut.getItems().setAll("EN_ATTENTE", "EN_COURS", "TRAITEE");

        SpinnerValueFactory<Integer> vf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0);
        if (spRate != null) spRate.setValueFactory(vf);

        if (btnValider != null) btnValider.setOnAction(e -> enregistrer());
        if (btnAnnuler != null) btnAnnuler.setOnAction(e -> retourner());
        if (btnRetour  != null) btnRetour.setOnAction(e -> retourner());

        // Désactiver le Statut par défaut (pour Client)
        if (cbStatut != null) cbStatut.setDisable(true);
    }

    // ---------------------------------------------------------------
    // CACHE CATÉGORIES
    // ---------------------------------------------------------------

    private void chargerCategoriesEnCache() {
        categorieCache.clear();
        nomToCategorieId.clear();
        if (cbCategorie != null) cbCategorie.getItems().clear();
        try {
            List<Categorie> categories = categorieService.afficherList();
            if (categories != null) {
                for (Categorie cat : categories) {
                    categorieCache.put(cat.getIdCategorie(), cat.getNomCategorie());
                    nomToCategorieId.put(cat.getNomCategorie(), cat.getIdCategorie());
                    if (cbCategorie != null) cbCategorie.getItems().add(cat.getNomCategorie());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement catégories : " + e.getMessage());
        }
    }

    private String getNomCategorie(int id) {
        return categorieCache.getOrDefault(id, "AUTRE");
    }

    // ---------------------------------------------------------------
    // REMPLISSAGE DU FORMULAIRE
    // ---------------------------------------------------------------

    public void remplir() {
        if (reclamationEnCours == null) return;

        if (isAdmin) {
            // Admin : seul le statut est modifiable
            if (tfSujet      != null) tfSujet.setDisable(true);
            if (taDescription!= null) taDescription.setDisable(true);
            if (cbCategorie  != null) cbCategorie.setDisable(true);
            if (spRate       != null) spRate.setDisable(true);
            if (cbStatut     != null) cbStatut.setDisable(false);
        } else {
            if (cbStatut != null) cbStatut.setDisable(true);
        }

        if (lblId        != null) lblId.setText("Réclamation #" + reclamationEnCours.getIdReclamation());
        if (tfSujet      != null) tfSujet.setText(reclamationEnCours.getSujetReclamation());
        if (taDescription!= null) taDescription.setText(reclamationEnCours.getDescriptionReclamation());

        // ✅ CORRECTION : getCategorieId() → getNomCategorie() pour alimenter le ComboBox
        if (cbCategorie != null)
            cbCategorie.setValue(getNomCategorie(reclamationEnCours.getCategorieId()));

        if (cbStatut != null)
            cbStatut.setValue(reclamationEnCours.getStatutReclamation());

        // ✅ CORRECTION : getRateReclamation() retourne double — cast en int
        if (spRate != null && spRate.getValueFactory() != null)
            spRate.getValueFactory().setValue((int) reclamationEnCours.getRateReclamation());
    }

    // ---------------------------------------------------------------
    // ENREGISTREMENT
    // ---------------------------------------------------------------

    private void enregistrer() {
        if (reclamationEnCours == null || reclamationService == null) return;

        // Admin : ne peut modifier que le statut
        if (isAdmin) {
            if (cbStatut.getValue() == null) {
                afficherMessage("Statut requis.", true); return;
            }
            reclamationEnCours.setStatutReclamation(cbStatut.getValue());
            try {
                reclamationService.modifier(reclamationEnCours);
                afficherMessage("Statut enregistré.", false);
                retourner();
            } catch (SQLException e) {
                afficherMessage("Erreur : " + e.getMessage(), true);
            }
            return;
        }

        // Client : modification sujet, description, catégorie, note (pas le statut)
        String sujet       = tfSujet.getText().trim();
        String description = taDescription.getText().trim();

        if (sujet.length() < 3) {
            afficherMessage("Sujet : min 3 caractères.", true); return;
        }
        if (description.length() < 10) {
            afficherMessage("Description : min 10 caractères.", true); return;
        }
        if (cbCategorie.getValue() == null) {
            afficherMessage("Catégorie requise.", true); return;
        }

        reclamationEnCours.setSujetReclamation(ProfanityFilterService.filter(sujet));
        reclamationEnCours.setDescriptionReclamation(ProfanityFilterService.filter(description));

        // ✅ CORRECTION : setCategorieId(int) au lieu de setCategorieReclamation(String)
        String nomCategorie = cbCategorie.getValue();
        int    categorieId  = nomToCategorieId.getOrDefault(nomCategorie, reclamationEnCours.getCategorieId());
        reclamationEnCours.setCategorieId(categorieId);

        // ✅ CORRECTION : setRateReclamation() attend double — doubleValue() au lieu de floatValue()
        reclamationEnCours.setRateReclamation(
                spRate.getValue() != null ? spRate.getValue().doubleValue() : 0.0);

        try {
            reclamationService.modifier(reclamationEnCours);
            afficherMessage("Modifications enregistrées.", false);
            retourner();
        } catch (SQLException e) {
            afficherMessage("Erreur : " + e.getMessage(), true);
        }
    }

    // ---------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------

    private void retourner() {
        if (dashboardController != null && retourCallback != null) {
            retourCallback.run(); return;
        }

        if (isAdmin && currentStage != null) {
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
                currentStage.setTitle("Gestion - Réclamation #" + reclamationEnCours.getIdReclamation());
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        if (retourCallback != null) { retourCallback.run(); return; }

        if (currentStage == null) {
            try { currentStage = (Stage) btnRetour.getScene().getWindow(); }
            catch (Exception ignored) {}
        }
        if (currentStage != null) currentStage.close();
    }

    // ---------------------------------------------------------------
    // UTILITAIRES
    // ---------------------------------------------------------------

    private void afficherMessage(String msg, boolean erreur) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setStyle(erreur ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2D6A4F;");
        }
    }
}