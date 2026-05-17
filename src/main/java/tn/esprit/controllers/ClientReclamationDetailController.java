package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import tn.esprit.entities.Reclamation;
import tn.esprit.entities.Reponse;
import tn.esprit.services.ProfanityFilterService;
import tn.esprit.services.ReclamationService;
import tn.esprit.services.ReponseService;
import tn.esprit.services.TranslationserviceR;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

public class ClientReclamationDetailController {

    @FXML private VBox  cardPublication;
    @FXML private Label lblAuteurReclamation;
    @FXML private Label lblSujet;
    @FXML private Label lblDescription;
    @FXML private Label lblCategorie;
    @FXML private Label lblStatut;
    @FXML private Label lblDateReclamation;
    @FXML private VBox  boxCommentaires;
    @FXML private Label lblMessage;
    @FXML private TextArea taContenu;
    @FXML private Spinner<Integer> spRate;
    @FXML private Button btnPublier;
    @FXML private Button btnRetour;
    @FXML private Button btnModifierReclamation;
    @FXML private Button btnSupprimerReclamation;
    @FXML private VBox  boxContent;
    @FXML private HBox  boxRateReclamation;

    private Reclamation reclamation;
    private Runnable retourCallback;
    private ReponseService reponseService;
    private ReclamationService reclamationService;
    private utilisateurs_service utilisateursService;
    private Stage currentStage;
    private UserHomeController parentHomeController;

    // ---------------------------------------------------------------
    // SETTERS
    // ---------------------------------------------------------------

    public void setReclamation(Reclamation reclamation) { this.reclamation = reclamation; }
    public void setRetourCallback(Runnable retourCallback) { this.retourCallback = retourCallback; }
    public void setCurrentStage(Stage stage) { this.currentStage = stage; }
    public void setParentHomeController(UserHomeController c) { this.parentHomeController = c; }

    // ---------------------------------------------------------------
    // INITIALISATION
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        try {
            reponseService      = new ReponseService();
            reclamationService  = new ReclamationService();
            utilisateursService = new utilisateurs_service();
        } catch (Exception e) {
            if (lblMessage != null) lblMessage.setText("Erreur connexion. Vous pouvez revenir à la liste.");
        }

        SpinnerValueFactory<Integer> vf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0);
        if (spRate != null) spRate.setValueFactory(vf);

        if (btnPublier              != null) btnPublier.setOnAction(e -> publier());
        if (btnRetour               != null) btnRetour.setOnAction(e -> { if (retourCallback != null) retourCallback.run(); });
        if (btnModifierReclamation  != null) btnModifierReclamation.setOnAction(e -> ouvrirModifierReclamation());
        if (btnSupprimerReclamation != null) btnSupprimerReclamation.setOnAction(e -> supprimerReclamation());
    }

    // ---------------------------------------------------------------
    // MODIFIER RÉCLAMATION
    // ---------------------------------------------------------------

    private void ouvrirModifierReclamation() {
        if (reclamation == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierReclamationForm.fxml"));
            Parent root = loader.load();
            ModifierReclamationFormController ctrl = loader.getController();
            ctrl.setReclamation(reclamation);
            ctrl.setIsAdmin(false);

            if (parentHomeController != null) {
                ctrl.setRetourCallback(this::rechargerDetailDansCentre);
                ctrl.remplir();
                root.getStylesheets().add("/app.css");
                parentHomeController.setCenterContent(root);
            } else {
                ctrl.setRetourCallback(retourCallback != null ? retourCallback : () -> {});
                ctrl.setCurrentStage(currentStage);
                ctrl.remplir();
                if (currentStage != null) {
                    currentStage.setScene(new Scene(root));
                    currentStage.getScene().getStylesheets().add("/app.css");
                    currentStage.setTitle("Modifier la réclamation");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void rechargerDetailDansCentre() {
        if (parentHomeController == null || reclamation == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientReclamationDetail.fxml"));
            Parent root = loader.load();
            ClientReclamationDetailController ctrl = loader.getController();
            ctrl.setReclamation(reclamation);
            ctrl.setRetourCallback(retourCallback);
            ctrl.setParentHomeController(parentHomeController);
            ctrl.setCurrentStage(currentStage);
            ctrl.afficher();
            root.getStylesheets().add("/app.css");
            parentHomeController.setCenterContent(root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // SUPPRIMER RÉCLAMATION
    // ---------------------------------------------------------------

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
            afficherMessage("Erreur : " + e.getMessage(), "#c62828");
        }
    }

    // ---------------------------------------------------------------
    // AFFICHAGE PRINCIPAL
    // ---------------------------------------------------------------

    public void afficher() {
        if (reclamation == null) return;

        // ✅ CORRECTION : getUtilisateurId() (pas getIdUtilisateur())
        String nomAuteur = utilisateursService != null
                ? utilisateursService.getNomCompletById(String.valueOf(reclamation.getUtilisateurId()))
                : "Utilisateur";
        if (lblAuteurReclamation != null) lblAuteurReclamation.setText("Par " + nomAuteur);

        if (lblSujet        != null) lblSujet.setText(reclamation.getSujetReclamation());
        if (lblDescription  != null) lblDescription.setText(
                reclamation.getDescriptionReclamation() != null ? reclamation.getDescriptionReclamation() : "");

        // ✅ CORRECTION : getCategorieId() retourne int — afficher l'id ou adapter selon votre logique
        if (lblCategorie    != null) lblCategorie.setText(String.valueOf(reclamation.getCategorieId()));
        if (lblStatut       != null) lblStatut.setText(reclamation.getStatutReclamation());

        if (lblDateReclamation != null && reclamation.getDateCreationReclamation() != null) {
            lblDateReclamation.setText(
                    new SimpleDateFormat("dd/MM/yyyy HH:mm").format(reclamation.getDateCreationReclamation()));
        }

        // ✅ CORRECTION : getUtilisateurId() (pas getIdUtilisateur())
        boolean isAuthor = isCurrentUserAuthor(reclamation.getUtilisateurId());
        if (btnModifierReclamation  != null) btnModifierReclamation.setVisible(isAuthor);
        if (btnSupprimerReclamation != null) btnSupprimerReclamation.setVisible(isAuthor);

        mettreAJourRateReclamation();
        ajouterBoutonTraduire();
        chargerCommentaires();
    }

    // ---------------------------------------------------------------
    // RATE RÉCLAMATION
    // ---------------------------------------------------------------

    private void mettreAJourRateReclamation() {
        if (boxRateReclamation == null || reclamation == null) return;
        boxRateReclamation.getChildren().clear();

        // ✅ CORRECTION : getRateReclamation() retourne double, cast en float
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
    // BOUTON TRADUIRE
    // ---------------------------------------------------------------

    private void ajouterBoutonTraduire() {
        if (lblSujet == null || lblSujet.getParent() == null) return;
        if (lblSujet.getParent() instanceof HBox) return;

        Button btnTraduire = new Button("🌍 Traduire");
        btnTraduire.setStyle("-fx-background-color: #7B5EA7; -fx-text-fill: white; " +
                "-fx-padding: 4 12; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11;");

        HBox hboxSujet = new HBox(10);
        hboxSujet.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) lblSujet.getParent();
        int index = parent.getChildren().indexOf(lblSujet);
        parent.getChildren().remove(lblSujet);
        hboxSujet.getChildren().addAll(lblSujet, btnTraduire);
        parent.getChildren().add(index, hboxSujet);

        btnTraduire.setOnAction(e -> {
            String sujetOriginal = reclamation.getSujetReclamation();
            btnTraduire.setText("⏳ Traduction...");
            btnTraduire.setDisable(true);

            new Thread(() -> {
                String sujetTraduit = TranslationserviceR.versFrancais(sujetOriginal);
                javafx.application.Platform.runLater(() -> {
                    if (sujetTraduit != null && !sujetTraduit.equals(sujetOriginal)) {
                        lblSujet.setText("🇫🇷 " + sujetTraduit);
                        btnTraduire.setText("↩ Original");
                        btnTraduire.setDisable(false);
                        btnTraduire.setOnAction(ev -> {
                            lblSujet.setText(sujetOriginal);
                            btnTraduire.setText("🌍 Traduire");
                            btnTraduire.setOnAction(evt -> {
                                lblSujet.setText("🇫🇷 " + sujetTraduit);
                                btnTraduire.setText("↩ Original");
                            });
                        });
                    } else {
                        lblSujet.setText(sujetOriginal);
                        btnTraduire.setText("✅ Déjà en FR");
                        btnTraduire.setDisable(false);
                    }
                });
            }).start();
        });
    }

    // ---------------------------------------------------------------
    // COMMENTAIRES
    // ---------------------------------------------------------------

    private void chargerCommentaires() {
        if (boxCommentaires == null) return;
        boxCommentaires.getChildren().clear();
        try {
            List<Reponse> list = reponseService.afficherParReclamation(reclamation.getIdReclamation());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            // ✅ CORRECTION : vraie moyenne globale via rateSum / rateCount
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
                VBox card = new VBox(6);
                card.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 1); " +
                        "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8;");

                String nomAuteurRep = utilisateursService != null
                        ? utilisateursService.getNomCompletById(String.valueOf(r.getIdUtilisateur()))
                        : "Utilisateur";
                Label lblAuteurRep = new Label("Par " + nomAuteurRep);
                lblAuteurRep.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 11; -fx-font-weight: bold;");

                String contenuFiltre = ProfanityFilterService.filter(r.getContenuReponse());
                Label contenu = new Label(contenuFiltre);
                contenu.setWrapText(true);
                contenu.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 13;");
                contenu.setMaxWidth(700);

                // ✅ CORRECTION : getRateReponse() retourne double, cast en float
                float rateRep = (float) r.getRateReponse();
                HBox starsRep = RatingUtils.createClickableStars(rateRep, value -> {
                    try {
                        reponseService.addVoteReponse(r, value);
                        chargerCommentaires();
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
                rateBox.setAlignment(Pos.CENTER_LEFT);
                rateBox.getChildren().addAll(starsRep, lblNoteRep);

                Label meta = new Label(" • " + (r.getDateReponse() != null ? sdf.format(r.getDateReponse()) : ""));
                meta.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 11;");

                HBox metaLine = new HBox(8);
                metaLine.getChildren().addAll(rateBox, meta);

                HBox actionBox = new HBox(10);
                actionBox.setAlignment(Pos.CENTER_RIGHT);
                if (isCurrentUserAuthor(r.getIdUtilisateur())) {
                    Button btnModifier = new Button("Modifier");
                    btnModifier.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; " +
                            "-fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11;");
                    btnModifier.setOnAction(e -> modifierReponse(r));
                    Button btnSupprimer = new Button("Supprimer");
                    btnSupprimer.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; " +
                            "-fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 11;");
                    btnSupprimer.setOnAction(e -> supprimerReponse(r));
                    actionBox.getChildren().addAll(btnModifier, btnSupprimer);
                }

                card.getChildren().addAll(lblAuteurRep, contenu, metaLine, actionBox);
                boxCommentaires.getChildren().add(card);
            }

            String texteNote = list.isEmpty()
                    ? "Aucune note"
                    : String.format("Note moyenne: %.1f/5 %s", moyenne, RatingUtils.toStars(moyenne));
            if (lblMessage != null) {
                lblMessage.setText(list.size() + " commentaire(s) • " + texteNote);
                lblMessage.setStyle("-fx-text-fill: #5A6475;");
            }

        } catch (SQLException e) {
            afficherMessage("Erreur chargement commentaires.", "#c62828");
        }
    }

    // ---------------------------------------------------------------
    // MODIFIER RÉPONSE
    // ---------------------------------------------------------------

    private void modifierReponse(Reponse rep) {
        if (rep == null || reponseService == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText("Modifier votre commentaire");

        VBox vbox = new VBox(10);
        vbox.setPadding(new javafx.geometry.Insets(10));

        Label lblContenu = new Label("Contenu :");
        TextArea taModifier = new TextArea(rep.getContenuReponse());
        taModifier.setWrapText(true);
        taModifier.setPrefRowCount(4);

        Label lblNote = new Label("Note (0-5) :");
        // ✅ CORRECTION : getRateReponse() retourne double, cast en int
        Spinner<Integer> spModifier = new Spinner<>(0, 5, (int) rep.getRateReponse());
        spModifier.setPrefWidth(80);

        vbox.getChildren().addAll(lblContenu, taModifier, lblNote, spModifier);
        dialog.getDialogPane().setContent(vbox);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        String nouveauContenu = taModifier.getText().trim();
        if (nouveauContenu.length() < 5) {
            afficherMessage("Contenu min. 5 caractères.", "#c62828"); return;
        }

        rep.setContenuReponse(ProfanityFilterService.filter(nouveauContenu));
        // ✅ CORRECTION : setRateReponse() attend double, doubleValue() au lieu de floatValue()
        rep.setRateReponse(spModifier.getValue().doubleValue());

        try {
            reponseService.modifier(rep);
            afficherMessage("Commentaire modifié.", "#2D6A4F");
            chargerCommentaires();
        } catch (SQLException e) {
            afficherMessage("Erreur : " + e.getMessage(), "#c62828");
        }
    }

    // ---------------------------------------------------------------
    // SUPPRIMER RÉPONSE
    // ---------------------------------------------------------------

    private void supprimerReponse(Reponse rep) {
        if (rep == null || reponseService == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer ce commentaire ?");
        String texteFiltre = ProfanityFilterService.filter(rep.getContenuReponse());
        alert.setContentText(texteFiltre.substring(0, Math.min(100, texteFiltre.length())) + "...");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            reponseService.delete(rep);
            afficherMessage("Commentaire supprimé.", "#2D6A4F");
            chargerCommentaires();
        } catch (SQLException e) {
            afficherMessage("Erreur : " + e.getMessage(), "#c62828");
        }
    }

    // ---------------------------------------------------------------
    // PUBLICATION
    // ---------------------------------------------------------------

    private void publier() {
        if (reclamation == null) {
            afficherMessage("Réclamation non chargée. Retournez à la liste.", "#c62828"); return;
        }
        if (reponseService == null) {
            afficherMessage("Erreur de connexion. Impossible d'ajouter le commentaire.", "#c62828"); return;
        }
        if (taContenu == null) return;

        String contenu = taContenu.getText().trim();
        if (contenu.length() < 5) {
            afficherMessage("Contenu min. 5 caractères.", "#c62828"); return;
        }

        int currentUserId = getCurrentUserIdAsInt();
        if (currentUserId == 0) {
            afficherMessage("Connectez-vous pour publier un commentaire.", "#c62828"); return;
        }

        // ✅ CORRECTION : rate en 4e position (double), reclamationId en 5e position
        double rate = (spRate != null && spRate.getValue() != null) ? spRate.getValue().doubleValue() : 0.0;
        String contenuFiltre = ProfanityFilterService.filter(contenu);

        Reponse rep = new Reponse(
                contenuFiltre,
                new Timestamp(System.currentTimeMillis()),
                currentUserId,
                rate,                              // ✅ rateReponse (double) en 4e
                reclamation.getIdReclamation()     // ✅ reclamationId (int)  en 5e
        );

        try {
            reponseService.addMeth2(rep);
            taContenu.clear();
            if (spRate != null && spRate.getValueFactory() != null) spRate.getValueFactory().setValue(0);
            afficherMessage("Commentaire publié.", "#2D6A4F");
            chargerCommentaires();
        } catch (SQLException e) {
            afficherMessage("Erreur : " + e.getMessage(), "#c62828");
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

    private boolean isCurrentUserAuthor(int authorId) {
        int currentId = getCurrentUserIdAsInt();
        return currentId != 0 && currentId == authorId;
    }

    private int getCurrentUserIdAsInt() {
        if (!SessionManager.isLoggedIn() || SessionManager.getCurrentUser() == null) return 0;
        String idStr = SessionManager.getCurrentUser().getId_utilisateur();
        if (idStr == null || idStr.isEmpty()) return 0;
        try { return Integer.parseInt(idStr.trim()); } catch (NumberFormatException e) { return 0; }
    }
}