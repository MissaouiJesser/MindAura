package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import tn.esprit.entities.Categorie;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.CategorieService;
import tn.esprit.services.EmailServiceR;
import tn.esprit.services.ProfanityFilterService;
import tn.esprit.services.ReclamationService;

import java.util.Date;
import java.util.List;

public class ClientAjouterReclamationController {

    @FXML private TextField tfSujet;
    @FXML private TextArea taDescription;
    @FXML private ComboBox<String> cbCategorie;
    @FXML private Button btnAjouter;
    @FXML private Button btnReinitialiser;
    @FXML private Button btnRetourner;
    @FXML private Label lblMessage;

    private ReclamationService reclamationService;
    private CategorieService categorieService;
    private Runnable retourCallback;
    private int userId = 1;
    private List<Categorie> categoriesChargees;

    public void setUserId(int userId) { this.userId = userId; }

    @FXML
    public void initialize() {
        this.categorieService = new CategorieService();
        remplirComboBoxCategories();

        if (btnAjouter       != null) btnAjouter.setOnAction(e -> ajouterReclamation());
        if (btnReinitialiser != null) btnReinitialiser.setOnAction(e -> reinitialiserFormulaire());
        if (btnRetourner     != null) btnRetourner.setOnAction(e -> retourner());
    }

    private void remplirComboBoxCategories() {
        if (cbCategorie == null) return;
        cbCategorie.getItems().clear();
        try {
            categoriesChargees = categorieService.afficherList();
            if (categoriesChargees == null || categoriesChargees.isEmpty()) {
                cbCategorie.getItems().addAll("COACH", "PSYCHOLOGUE", "EVENEMENT", "AUTRE");
                cbCategorie.setValue("COACH");
                afficherMessage("⚠️ Aucune catégorie trouvée en base, valeurs par défaut utilisées", "#FF9800");
                return;
            }
            for (Categorie cat : categoriesChargees) {
                cbCategorie.getItems().add(cat.getNomCategorie());
            }
            cbCategorie.setValue(categoriesChargees.get(0).getNomCategorie());
        } catch (Exception e) {
            cbCategorie.getItems().addAll("COACH", "PSYCHOLOGUE", "EVENEMENT", "AUTRE");
            cbCategorie.setValue("COACH");
            System.err.println("❌ Erreur chargement catégories : " + e.getMessage());
        }
    }

    private int getIdCategorieSelectionnee() {
        String nomSelectionne = cbCategorie.getValue();
        if (nomSelectionne == null || categoriesChargees == null) return -1;
        for (Categorie cat : categoriesChargees) {
            if (cat.getNomCategorie().equals(nomSelectionne)) return cat.getIdCategorie();
        }
        return -1;
    }

    private void ajouterReclamation() {
        String sujet        = tfSujet != null ? tfSujet.getText().trim() : "";
        String description  = taDescription != null ? taDescription.getText().trim() : "";
        String categorieNom = cbCategorie != null ? cbCategorie.getValue() : null;

        if (sujet.isEmpty()) {
            afficherMessage("❌ Le sujet est obligatoire", "#f44336"); return;
        }
        if (sujet.length() < 3) {
            afficherMessage("❌ Le sujet doit avoir au moins 3 caractères", "#f44336"); return;
        }
        if (description.isEmpty()) {
            afficherMessage("❌ La description est obligatoire", "#f44336"); return;
        }
        if (description.length() < 10) {
            afficherMessage("❌ La description doit avoir au moins 10 caractères", "#f44336"); return;
        }
        if (categorieNom == null || categorieNom.isEmpty()) {
            afficherMessage("❌ Sélectionnez une catégorie", "#f44336"); return;
        }

        int categorieId = getIdCategorieSelectionnee();
        if (categorieId == -1) {
            afficherMessage("❌ Catégorie invalide, veuillez réessayer", "#f44336"); return;
        }

        try {
            String sujetFiltre        = ProfanityFilterService.filter(sujet);
            String descriptionFiltree = ProfanityFilterService.filter(description);

            double rateValue = 0.0;

            Reclamation newReclamation = new Reclamation(
                    sujetFiltre,
                    descriptionFiltree,
                    new Date(),
                    "EN_ATTENTE",
                    userId,
                    rateValue,
                    categorieId
            );

            if (reclamationService != null) {
                reclamationService.addMeth2(newReclamation);
                afficherMessage("✓ Réclamation ajoutée avec succès!", "#4CAF50");

                final String ADMIN_EMAIL = "benrabehyassinne@gmail.com";
                final String catNomFinal = categorieNom;
                new Thread(() -> {
                    boolean emailEnvoye = EmailServiceR.envoyerNouvelleReclamationAdmin(
                            ADMIN_EMAIL, sujetFiltre, descriptionFiltree, catNomFinal
                    );
                    System.out.println(emailEnvoye
                            ? "✅ Email envoyé à l'admin: " + ADMIN_EMAIL
                            : "❌ Email admin échoué");
                }).start();

                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        javafx.application.Platform.runLater(this::retourner);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } else {
                afficherMessage("❌ Service non disponible", "#f44336");
            }

        } catch (Exception ex) {
            afficherMessage("❌ Erreur: " + ex.getMessage(), "#f44336");
            ex.printStackTrace();
        }
    }

    private void reinitialiserFormulaire() {
        if (tfSujet       != null) tfSujet.clear();
        if (taDescription != null) taDescription.clear();
        if (lblMessage    != null) lblMessage.setText("");
        if (cbCategorie   != null && !cbCategorie.getItems().isEmpty()) {
            cbCategorie.setValue(cbCategorie.getItems().get(0));
        }
    }

    private void retourner() {
        if (retourCallback != null) retourCallback.run();
    }

    private void afficherMessage(String message, String couleur) {
        if (lblMessage != null) {
            lblMessage.setText(message);
            lblMessage.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 12; -fx-font-weight: bold;");
        }
    }

    public void setReclamationService(ReclamationService service) { this.reclamationService = service; }

    public void setCategorieService(CategorieService service) {
        this.categorieService = service;
        remplirComboBoxCategories();
    }

    public void setRetourCallback(Runnable callback) { this.retourCallback = callback; }
}