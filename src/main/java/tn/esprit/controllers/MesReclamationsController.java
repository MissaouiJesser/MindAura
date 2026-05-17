package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.ReclamationService;
import tn.esprit.services.ReponseService;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class MesReclamationsController {

    @FXML private Button btnRetour;
    @FXML private ListView<Reclamation> listReclamations;
    @FXML private Label lblMessage;

    private ReclamationService reclamationService;
    private ReponseService reponseService;
    private utilisateurs_service utilisateursService;
    private Stage currentStage;
    private Runnable retourVersListeCallback;
    private UserHomeController parentHomeController;

    public void setRetourVersListeCallback(Runnable r) { this.retourVersListeCallback = r; }
    public void setParentHomeController(UserHomeController c) { this.parentHomeController = c; }
    public void setCurrentStage(Stage stage) { this.currentStage = stage; }

    // ---------------------------------------------------------------
    // INITIALISATION
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        try {
            reclamationService  = new ReclamationService();
            reponseService      = new ReponseService();
            utilisateursService = new utilisateurs_service();
        } catch (Exception e) {
            if (lblMessage != null) lblMessage.setText("Erreur de connexion.");
            return;
        }

        configurerListView();

        if (btnRetour != null) btnRetour.setOnAction(e -> retourListeToutes());

        chargerMesReclamations();
    }

    // ---------------------------------------------------------------
    // LISTE VIEW
    // ---------------------------------------------------------------

    private void configurerListView() {
        if (listReclamations == null) return;

        listReclamations.setCellFactory(new Callback<ListView<Reclamation>, ListCell<Reclamation>>() {
            @Override
            public ListCell<Reclamation> call(ListView<Reclamation> list) {
                return new ListCell<Reclamation>() {
                    private final VBox  card    = new VBox(8);
                    private final Label lblAuteur = new Label();
                    private final Label lblSujet  = new Label();
                    private final Label lblDesc   = new Label();
                    private final HBox  badges    = new HBox(8);

                    {
                        card.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 8; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
                                "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
                        lblAuteur.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 11; -fx-font-weight: bold;");
                        lblSujet.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 14; -fx-font-weight: bold;");
                        lblDesc.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12; -fx-wrap-text: true;");
                        lblDesc.setMaxWidth(700);
                        card.getChildren().addAll(lblAuteur, lblSujet, lblDesc, badges);
                        setGraphic(card);
                    }

                    @Override
                    protected void updateItem(Reclamation r, boolean empty) {
                        super.updateItem(r, empty);
                        if (empty || r == null) { setGraphic(null); return; }

                        // ✅ CORRECTION : getUtilisateurId() (pas getIdUtilisateur())
                        String nomAuteur = utilisateursService != null
                                ? utilisateursService.getNomCompletById(String.valueOf(r.getUtilisateurId()))
                                : "Utilisateur";
                        lblAuteur.setText("Par " + nomAuteur);
                        lblSujet.setText(r.getSujetReclamation());

                        String desc = r.getDescriptionReclamation();
                        lblDesc.setText(desc != null && desc.length() > 120 ? desc.substring(0, 120) + "..." : desc);

                        badges.getChildren().clear();

                        // ✅ CORRECTION : getCategorieId() retourne int (pas getCategorieReclamation())
                        Label cat = new Label("Catégorie #" + r.getCategorieId());
                        cat.setStyle("-fx-background-color: #7B5EA7; -fx-text-fill: white; " +
                                "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11;");

                        Label statut = new Label(r.getStatutReclamation());
                        statut.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; " +
                                "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11;");

                        int nb = 0;
                        try {
                            if (reponseService != null)
                                nb = reponseService.compterParReclamation(r.getIdReclamation());
                        } catch (SQLException ignored) {}

                        Label comm = new Label(nb + " commentaire(s)");
                        comm.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 11;");

                        badges.getChildren().addAll(cat, statut, comm);
                        setGraphic(card);
                    }
                };
            }
        });

        listReclamations.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) {
                Reclamation r = listReclamations.getSelectionModel().getSelectedItem();
                if (r != null) ouvrirDetail(r);
            }
        });
    }

    // ---------------------------------------------------------------
    // CHARGEMENT
    // ---------------------------------------------------------------

    private void chargerMesReclamations() {
        try {
            List<Reclamation> mesReclamations;
            if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
                int idUtilisateur = Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur());
                mesReclamations = reclamationService.afficherParUtilisateur(idUtilisateur);
            } else {
                mesReclamations = Collections.emptyList();
            }
            listReclamations.getItems().setAll(mesReclamations);
            if (lblMessage != null) {
                lblMessage.setText(mesReclamations.isEmpty()
                        ? "Aucune réclamation."
                        : mesReclamations.size() + " réclamation(s).");
            }
        } catch (SQLException ex) {
            if (lblMessage != null) lblMessage.setText("Erreur chargement.");
        } catch (NumberFormatException ex) {
            listReclamations.getItems().clear();
            if (lblMessage != null) lblMessage.setText("Session invalide.");
        }
    }

    // ---------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------

    private void retourAMesReclamations() {
        if (parentHomeController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesReclamations.fxml"));
                Parent root = loader.load();
                MesReclamationsController ctrl = loader.getController();
                ctrl.setRetourVersListeCallback(retourVersListeCallback);
                ctrl.setParentHomeController(parentHomeController);
                parentHomeController.setCenterContent(root);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        Stage stage = currentStage != null ? currentStage
                : (btnRetour != null && btnRetour.getScene() != null)
                ? (Stage) btnRetour.getScene().getWindow() : null;
        if (stage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesReclamations.fxml"));
            Parent root = loader.load();
            MesReclamationsController ctrl = loader.getController();
            ctrl.setCurrentStage(stage);
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Mes Reclamations");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void retourListeToutes() {
        if (retourVersListeCallback != null) { retourVersListeCallback.run(); return; }
        Stage stage = currentStage;
        if (stage == null && btnRetour != null && btnRetour.getScene() != null)
            stage = (Stage) btnRetour.getScene().getWindow();
        if (stage == null) return;
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ClientAccueil.fxml"));
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Reclamations et Reponses - Espace Client");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void ouvrirDetail(Reclamation r) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientReclamationDetail.fxml"));
            Parent root = loader.load();
            ClientReclamationDetailController ctrl = loader.getController();
            ctrl.setReclamation(r);
            ctrl.setRetourCallback(this::retourAMesReclamations);

            if (parentHomeController != null) {
                Stage stage = (Stage) listReclamations.getScene().getWindow();
                ctrl.setCurrentStage(stage);
                ctrl.setParentHomeController(parentHomeController);
                ctrl.afficher();
                root.getStylesheets().add("/app.css");
                parentHomeController.setCenterContent(root);
            } else {
                Stage stage = currentStage != null ? currentStage
                        : (Stage) listReclamations.getScene().getWindow();
                currentStage = stage;
                ctrl.setCurrentStage(stage);
                ctrl.afficher();
                stage.setScene(new Scene(root));
                stage.getScene().getStylesheets().add("/app.css");
                stage.setTitle("Réclamation - Commentaires");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}