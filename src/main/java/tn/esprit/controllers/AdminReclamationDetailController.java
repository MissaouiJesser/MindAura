package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.application.Platform;
import tn.esprit.entities.Categorie;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.Reponse;
import tn.esprit.services.*;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminReclamationDetailController implements DashboardController.DashboardAware {

    private DashboardController dashboardController;

    @FXML private Label lblAuteurReclamation;
    @FXML private Label lblSujet;
    @FXML private Label lblDescription;
    @FXML private Label lblCategorie;
    @FXML private Label lblStatut;
    @FXML private Label lblDateReclamation;
    @FXML private VBox  boxReponses;
    @FXML private HBox  boxRateReclamation;
    @FXML private Label lblMessage;
    @FXML private TextArea taContenu;
    @FXML private Button btnPublier;
    @FXML private Button btnRetour;
    @FXML private Button btnModifierReclamation;
    @FXML private Button btnSupprimerReclamation;
    @FXML private Button btnGenererAvecIA;
    @FXML private ProgressBar pbChargement;
    @FXML private Button btnEnvoyerEmail;
    @FXML private Button btnExporterPdf;

    private Reclamation reclamation;
    private Runnable retourCallback;
    private ReponseService reponseService;
    private ReclamationService reclamationService;
    private CategorieService categorieService;
    private utilisateurs_service utilisateursService;
    private Stage currentStage;
    private Reponse dernierReponse;

    private final Map<Integer, String> categorieCache = new HashMap<>();

    // ---------------------------------------------------------------
    // SETTERS
    // ---------------------------------------------------------------

    public void setReclamation(Reclamation reclamation) { this.reclamation = reclamation; }
    public void setRetourCallback(Runnable retourCallback) { this.retourCallback = retourCallback; }
    public void setCurrentStage(Stage stage) { this.currentStage = stage; }

    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    // ---------------------------------------------------------------
    // INITIALISATION
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        try {
            reponseService      = new ReponseService();
            reclamationService  = new ReclamationService();
            utilisateursService = new utilisateurs_service();
            categorieService    = new CategorieService();
        } catch (Exception e) {
            if (lblMessage != null) lblMessage.setText("Erreur connexion.");
        }

        chargerCategoriesEnCache();

        if (btnPublier              != null) btnPublier.setOnAction(e -> publier());
        if (btnRetour               != null) btnRetour.setOnAction(e -> allerALaListe());
        if (btnModifierReclamation  != null) btnModifierReclamation.setOnAction(e -> ouvrirModifierReclamation());
        if (btnSupprimerReclamation != null) btnSupprimerReclamation.setOnAction(e -> supprimerReclamation());
        if (btnGenererAvecIA        != null) btnGenererAvecIA.setOnAction(e -> genererReponseAvecIA());
        if (btnEnvoyerEmail         != null) btnEnvoyerEmail.setOnAction(e -> envoyerEmailAuClient());
        if (btnExporterPdf          != null) btnExporterPdf.setOnAction(e -> exporterPdf());
        if (pbChargement            != null) pbChargement.setVisible(false);
    }

    // ---------------------------------------------------------------
    // CACHE CATÉGORIES
    // ---------------------------------------------------------------

    private void chargerCategoriesEnCache() {
        categorieCache.clear();
        try {
            List<Categorie> categories = categorieService.afficherList();
            if (categories != null) {
                for (Categorie cat : categories) {
                    categorieCache.put(cat.getIdCategorie(), cat.getNomCategorie());
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
    // AFFICHAGE PRINCIPAL
    // ---------------------------------------------------------------

    public void afficher() {
        if (reclamation == null) return;

        String nomAuteur = utilisateursService != null
                ? utilisateursService.getNomCompletById(String.valueOf(reclamation.getUtilisateurId()))
                : "Utilisateur";
        if (lblAuteurReclamation != null) lblAuteurReclamation.setText("Par " + nomAuteur);
        if (lblSujet             != null) lblSujet.setText(reclamation.getSujetReclamation());
        if (lblDescription       != null) lblDescription.setText(
                reclamation.getDescriptionReclamation() != null ? reclamation.getDescriptionReclamation() : "");
        if (lblCategorie         != null) lblCategorie.setText(getNomCategorie(reclamation.getCategorieId()));
        if (lblStatut            != null) lblStatut.setText(reclamation.getStatutReclamation());

        if (lblDateReclamation != null && reclamation.getDateCreationReclamation() != null) {
            lblDateReclamation.setText(
                    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(reclamation.getDateCreationReclamation()));
        }

        if (btnSupprimerReclamation != null) btnSupprimerReclamation.setVisible(false);

        mettreAJourRateReclamation();
        chargerReponses();
    }

    // ---------------------------------------------------------------
    // RATE RÉCLAMATION
    // ---------------------------------------------------------------

    private void mettreAJourRateReclamation() {
        if (boxRateReclamation == null || reclamation == null) return;
        boxRateReclamation.getChildren().clear();

        float rate = (float) reclamation.getRateReclamation();

        HBox stars = RatingUtils.createClickableStars(rate, value -> {
            try {
                reclamationService.addVoteReclamation(reclamation, value);
                mettreAJourRateReclamation();
            } catch (SQLException e) {
                afficherMessage("Erreur mise à jour note: " + e.getMessage(), "#c62828");
            }
        });

        int n = reclamation.getRateCount();
        String lbl = n == 0
                ? "  Moyenne: -/5"
                : String.format("  Moyenne: %.1f/5 (%d clic%s)", rate, n, n > 1 ? "s" : "");
        Label lblMoyenne = new Label(lbl);
        lblMoyenne.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12;");
        boxRateReclamation.getChildren().addAll(stars, lblMoyenne);
    }

    // ---------------------------------------------------------------
    // RÉPONSES
    // ---------------------------------------------------------------

    private void chargerReponses() {
        if (boxReponses == null) return;
        boxReponses.getChildren().clear();
        try {
            List<Reponse> list = reponseService.afficherParReclamation(reclamation.getIdReclamation());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            float moyenne = 0f;
            if (!list.isEmpty()) {
                int totalSum   = 0;
                int totalCount = 0;
                for (Reponse r : list) {
                    totalSum   += r.getRateSum();
                    totalCount += r.getRateCount();
                }
                moyenne = totalCount > 0 ? (float) totalSum / totalCount : 0f;
            }

            for (Reponse r : list) {
                dernierReponse = r;

                VBox card = new VBox(6);
                card.setStyle(
                        "-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 8; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 1); " +
                                "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8;"
                );

                String nomAuteurRep = utilisateursService != null
                        ? utilisateursService.getNomCompletById(String.valueOf(r.getIdUtilisateur()))
                        : "Utilisateur";
                if (utilisateursService != null
                        && utilisateursService.isAdminById(String.valueOf(r.getIdUtilisateur()))) {
                    nomAuteurRep += " (admin)";
                }

                Label lblAuteurRep = new Label("Par " + nomAuteurRep);
                lblAuteurRep.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 11; -fx-font-weight: bold;");

                String contenuFiltre = ProfanityFilterService.filter(r.getContenuReponse());
                Label contenu = new Label(contenuFiltre);
                contenu.setWrapText(true);
                contenu.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 13;");
                contenu.setMaxWidth(700);

                float rateRep = (float) r.getRateReponse();
                HBox starsRep = RatingUtils.createClickableStars(rateRep, value -> {
                    try {
                        reponseService.addVoteReponse(r, value);
                        chargerReponses();
                    } catch (SQLException ex) {
                        afficherMessage("Erreur mise à jour note: " + ex.getMessage(), "#c62828");
                    }
                });
                int nc = r.getRateCount();
                String noteLbl = nc == 0
                        ? "  Note: -/5"
                        : String.format("  Note: %.1f/5 (%d clic%s)", rateRep, nc, nc > 1 ? "s" : "");
                Label lblNoteRep = new Label(noteLbl);
                lblNoteRep.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 11;");

                HBox rateBox = new HBox(8);
                rateBox.getChildren().addAll(starsRep, lblNoteRep);

                Label meta = new Label(" • " + (r.getDateReponse() != null ? sdf.format(r.getDateReponse()) : ""));
                meta.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 11;");

                HBox metaLine = new HBox(8);
                metaLine.getChildren().addAll(rateBox, meta);

                HBox bas = new HBox(10);
                bas.getChildren().add(metaLine);

                if (isCurrentUserAuthor(r.getIdUtilisateur())) {
                    Button btnModifier = new Button("Modifier");
                    btnModifier.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 4 10; -fx-font-size: 11; -fx-cursor: hand; -fx-background-radius: 4;");
                    btnModifier.setOnAction(e -> modifierReponse(r));
                    Button btnSuppr = new Button("Supprimer");
                    btnSuppr.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-padding: 4 10; -fx-font-size: 11; -fx-cursor: hand; -fx-background-radius: 4;");
                    btnSuppr.setOnAction(e -> supprimerReponse(r));
                    bas.getChildren().addAll(btnModifier, btnSuppr);
                }

                card.getChildren().addAll(lblAuteurRep, contenu, bas);
                boxReponses.getChildren().add(card);
            }

            if (lblMessage != null) {
                String texteNote = list.isEmpty()
                        ? "Aucune note"
                        : String.format("Note moyenne: %.1f/5 %s", moyenne, RatingUtils.toStars(moyenne));
                lblMessage.setText(list.size() + " réponse(s) • " + texteNote);
            }

        } catch (SQLException e) {
            if (lblMessage != null) lblMessage.setText("Erreur chargement réponses.");
        }
    }

    // ---------------------------------------------------------------
    // PUBLICATION
    // ---------------------------------------------------------------

    private void publier() {
        String contenu = taContenu != null ? taContenu.getText().trim() : "";
        if (contenu.length() < 5) {
            afficherMessage("Contenu min. 5 caractères.", "#c62828"); return;
        }
        int adminUserId = getCurrentUserIdAsInt();
        if (adminUserId == 0) {
            afficherMessage("Session admin requise pour publier.", "#c62828"); return;
        }

        double rate = 0.0;
        String contenuFiltre = ProfanityFilterService.filter(contenu);

        Reponse rep = new Reponse(
                contenuFiltre,
                new Timestamp(System.currentTimeMillis()),
                adminUserId,
                rate,
                reclamation.getIdReclamation()
        );
        try {
            reponseService.addMeth2(rep);
            if (taContenu != null) taContenu.clear();
            afficherMessage("Réponse publiée.", "#2D6A4F");
            chargerReponses();
        } catch (SQLException e) {
            afficherMessage("Erreur: " + e.getMessage(), "#c62828");
        }
    }

    // ---------------------------------------------------------------
    // IA
    // ---------------------------------------------------------------

    private void genererReponseAvecIA() {
        if (reclamation == null) {
            afficherMessage("❌ Aucune réclamation sélectionnée", "#c62828"); return;
        }
        if (pbChargement    != null) { pbChargement.setVisible(true); pbChargement.setProgress(-1); }
        if (btnGenererAvecIA != null) btnGenererAvecIA.setDisable(true);
        afficherMessage("⏳ Génération en cours avec Gemini...", "#2196F3");

        new Thread(() -> {
            try {
                String prompt = construirePrompt(
                        reclamation.getSujetReclamation(),
                        reclamation.getDescriptionReclamation(),
                        getNomCategorie(reclamation.getCategorieId())
                );
                String reponse = GeminiServiceR.askGemini(prompt);
                Platform.runLater(() -> {
                    if (taContenu != null) taContenu.setText(reponse);
                    afficherMessage("✅ Réponse générée par IA! Vous pouvez l'éditer avant de publier.", "#2D6A4F");
                    if (pbChargement    != null) pbChargement.setVisible(false);
                    if (btnGenererAvecIA != null) btnGenererAvecIA.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    afficherMessage("❌ Erreur Gemini: " + e.getMessage(), "#c62828");
                    if (pbChargement    != null) pbChargement.setVisible(false);
                    if (btnGenererAvecIA != null) btnGenererAvecIA.setDisable(false);
                });
            }
        }).start();
    }

    private String construirePrompt(String sujet, String description, String categorie) {
        return "Tu es un représentant du service client professionnel dans le département \"" + categorie + "\".\n\n" +
                "Un client a soumis la plainte suivante:\n\n" +
                "Sujet: " + sujet + "\n" +
                "Description: " + description + "\n\n" +
                "Génère une réponse professionnelle, empathique et utile qui:\n" +
                "1. Reconnaît leur préoccupation\n" +
                "2. S'excuse si approprié\n" +
                "3. Explique la solution ou les prochaines étapes\n" +
                "4. Montre un véritable souci du client\n\n" +
                "Garde la réponse concise (3-4 paragraphes max) et professionnelle.\n" +
                "Écris en français.";
    }

    // ---------------------------------------------------------------
    // EMAIL
    // ---------------------------------------------------------------

    private void envoyerEmailAuClient() {
        if (reclamation == null) {
            afficherMessage("❌ Aucune réclamation sélectionnée", "#c62828"); return;
        }
        if (dernierReponse == null) {
            afficherMessage("❌ Aucune réponse à envoyer. Publiez d'abord une réponse.", "#c62828"); return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Envoyer par Email");
        dialog.setHeaderText("Entrez l'email du client:");
        dialog.setContentText("Email:");
        var result = dialog.showAndWait();
        if (result.isEmpty()) return;

        String emailClient = result.get().trim();
        if (!emailClient.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            afficherMessage("❌ Email invalide", "#c62828"); return;
        }

        if (pbChargement    != null) pbChargement.setVisible(true);
        if (btnEnvoyerEmail != null) btnEnvoyerEmail.setDisable(true);
        afficherMessage("📤 Envoi de l'email...", "#2196F3");

        new Thread(() -> {
            try {
                String contenuFiltre = ProfanityFilterService.filter(dernierReponse.getContenuReponse());
                boolean success = EmailServiceR.envoyerReponseClient(
                        emailClient,
                        reclamation.getSujetReclamation(),
                        contenuFiltre,
                        (float) dernierReponse.getRateReponse()
                );

                Platform.runLater(() -> {
                    afficherMessage(success ? "✅ Email envoyé avec succès!" : "❌ Erreur lors de l'envoi de l'email",
                            success ? "#2D6A4F" : "#c62828");
                    if (pbChargement    != null) pbChargement.setVisible(false);
                    if (btnEnvoyerEmail != null) btnEnvoyerEmail.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    afficherMessage("❌ Erreur: " + e.getMessage(), "#c62828");
                    if (pbChargement    != null) pbChargement.setVisible(false);
                    if (btnEnvoyerEmail != null) btnEnvoyerEmail.setDisable(false);
                });
            }
        }).start();
    }

    // ---------------------------------------------------------------
    // PDF
    // ---------------------------------------------------------------

    private void exporterPdf() {
        if (reclamation == null) {
            afficherMessage("❌ Aucune réclamation sélectionnée", "#c62828"); return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le PDF");
        fileChooser.setInitialFileName("reclamation_" + reclamation.getIdReclamation() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(currentStage);
        if (file == null) return;

        afficherMessage("⏳ Génération du PDF...", "#2196F3");
        if (btnExporterPdf != null) btnExporterPdf.setDisable(true);

        new Thread(() -> {
            try {
                List<Reponse> reponses = reponseService.afficherParReclamation(reclamation.getIdReclamation());
                Pdfservice.genererPdfReclamation(reclamation, reponses, file.getAbsolutePath());
                Platform.runLater(() -> {
                    afficherMessage("✅ PDF exporté avec succès!", "#2D6A4F");
                    if (btnExporterPdf != null) btnExporterPdf.setDisable(false);
                    try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {}
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    afficherMessage("❌ Erreur PDF: " + e.getMessage(), "#c62828");
                    if (btnExporterPdf != null) btnExporterPdf.setDisable(false);
                });
            }
        }).start();
    }

    // ---------------------------------------------------------------
    // MODIFIER / SUPPRIMER RÉCLAMATION
    // ---------------------------------------------------------------

    private void ouvrirModifierReclamation() {
        if (reclamation == null) return;
        if (dashboardController != null) {
            Object ctrl = dashboardController.loadViewAndGet(
                    "ModifierReclamationForm.fxml",
                    "Modifier la réclamation",
                    "Modification de la réclamation #" + reclamation.getIdReclamation()
            );
            if (ctrl instanceof ModifierReclamationFormController formCtrl) {
                formCtrl.setReclamation(reclamation);
                formCtrl.setIsAdmin(true);
                formCtrl.setRetourCallback(() -> {
                    Object d = dashboardController.loadViewAndGet(
                            "AdminReclamationDetail.fxml",
                            "Détail réclamation #" + reclamation.getIdReclamation(),
                            "Réclamation et réponses"
                    );
                    if (d instanceof AdminReclamationDetailController dc) {
                        dc.setReclamation(reclamation);
                        dc.setRetourCallback(() -> dashboardController.navigateTo(
                                "AdminAccueil.fxml", "Réclamations et Réponses", "Gestion des réclamations"));
                        dc.setCurrentStage(dashboardController.getStage());
                        Platform.runLater(dc::afficher);
                    }
                });
                Platform.runLater(formCtrl::remplir);
            }
            return;
        }
        if (currentStage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierReclamationForm.fxml"));
            Parent root = loader.load();
            ModifierReclamationFormController ctrl = loader.getController();
            ctrl.setReclamation(reclamation);
            ctrl.setRetourCallback(retourCallback != null ? retourCallback : () -> {});
            ctrl.setCurrentStage(currentStage);
            ctrl.setIsAdmin(true);
            ctrl.remplir();
            currentStage.setScene(new Scene(root));
            currentStage.getScene().getStylesheets().add("/app.css");
            currentStage.setTitle("Modifier la réclamation");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void supprimerReclamation() {
        if (reclamation == null || reclamationService == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer cette réclamation ?");
        alert.setContentText(reclamation.getSujetReclamation());
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            reclamationService.delete(reclamation);
            if (retourCallback != null) retourCallback.run();
        } catch (SQLException e) {
            if (lblMessage != null) lblMessage.setText("Erreur : " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // MODIFIER / SUPPRIMER RÉPONSE
    // ---------------------------------------------------------------

    private void modifierReponse(Reponse r) {
        if (r == null) return;
        if (dashboardController != null) {
            Object ctrl = dashboardController.loadViewAndGet(
                    "ModifierReponse.fxml", "Modifier la réponse", "Commentaire #" + r.getIdReponse());
            if (ctrl instanceof ModifierReponse formCtrl) {
                formCtrl.remplirAvecReponse(r);
                formCtrl.setReclamation(reclamation);
                formCtrl.setRetourCallback(() -> {
                    Object d = dashboardController.loadViewAndGet(
                            "AdminReclamationDetail.fxml",
                            "Détail réclamation #" + reclamation.getIdReclamation(),
                            "Réclamation et réponses"
                    );
                    if (d instanceof AdminReclamationDetailController dc) {
                        dc.setReclamation(reclamation);
                        dc.setRetourCallback(() -> dashboardController.navigateTo(
                                "AdminAccueil.fxml", "Réclamations et Réponses", "Gestion des réclamations"));
                        dc.setCurrentStage(dashboardController.getStage());
                        Platform.runLater(dc::afficher);
                    }
                });
            }
            return;
        }
        if (currentStage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierReponse.fxml"));
            Parent root = loader.load();
            ModifierReponse ctrl = loader.getController();
            ctrl.remplirAvecReponse(r);
            ctrl.setReclamation(reclamation);
            ctrl.setCurrentStage(currentStage);
            currentStage.setScene(new Scene(root));
            currentStage.getScene().getStylesheets().add("/app.css");
            currentStage.setTitle("Modifier la réponse");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void supprimerReponse(Reponse r) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer cette réponse ?");
        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            reponseService.delete(r);
            chargerReponses();
        } catch (SQLException e) {
            if (lblMessage != null) lblMessage.setText("Erreur : " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------

    private void allerALaListe() {
        if (retourCallback != null) { retourCallback.run(); return; }
        if (currentStage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminAccueil.fxml"));
            Parent root = loader.load();
            currentStage.setScene(new Scene(root));
            currentStage.getScene().getStylesheets().add("/app.css");
            currentStage.setTitle("Espace Admin - Gestion des réclamations");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // UTILITAIRES
    // ---------------------------------------------------------------

    private void afficherMessage(String message, String couleur) {
        if (lblMessage != null) {
            lblMessage.setText(message);
            lblMessage.setStyle("-fx-text-fill: " + couleur + ";");
        }
    }

    private int getCurrentUserIdAsInt() {
        if (!SessionManager.isLoggedIn() || SessionManager.getCurrentUser() == null) return 0;
        String idStr = SessionManager.getCurrentUser().getId_utilisateur();
        if (idStr == null || idStr.isEmpty()) return 0;
        try { return Integer.parseInt(idStr.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private boolean isCurrentUserAuthor(int authorId) {
        int currentId = getCurrentUserIdAsInt();
        return currentId != 0 && currentId == authorId;
    }
}