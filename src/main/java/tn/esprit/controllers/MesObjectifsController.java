package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import tn.esprit.entities.Objectif;
import tn.esprit.services.GeminiService;
import tn.esprit.services.ObjectifService;
import tn.esprit.services.TextToSpeechService;
import tn.esprit.services.WeatherService;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.PaginationHelper;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MesObjectifsController implements Initializable {

    // ─── FXML ─────────────────────────────────────────────────────────────────
    @FXML private ListView<Objectif>        listViewObjectifs;
    @FXML private Label                     userNameLabel;
    @FXML private HBox                      paginationContainer;

    // Météo
    @FXML private HBox                      meteoContainer;
    @FXML private Label                     lblMeteoErreur;

    // Chatbot IA
    @FXML private TextArea                  chatArea;
    @FXML private TextField                 chatInput;
    @FXML private ProgressIndicator         chatLoading;

    // ─── État ─────────────────────────────────────────────────────────────────
    private List<Objectif>     listeObjectifsFull = new ArrayList<>();
    private int                currentPage        = 0;
    private ObjectifService    objectifService;
    private UserHomeController parentHomeController;
    private String             userContext        = "";

    public void setParentHomeController(UserHomeController ctrl) {
        this.parentHomeController = ctrl;
    }

    public void setUserContext(String testName, int score, int maxScore) {
        this.userContext = "L'utilisateur a passé le test '%s' avec un score de %d/%d."
                .formatted(testName, score, maxScore);
    }

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        objectifService = new ObjectifService();

        if (!SessionManager.isLoggedIn() || SessionManager.getCurrentUser() == null) {
            NavigationManager.navigateTo("Login.fxml", "MindAura – Connexion",
                    NavigationManager.getStage(listViewObjectifs), false);
            return;
        }

        listViewObjectifs.setCellFactory(param -> new ListCell<Objectif>() {
            @Override
            protected void updateItem(Objectif obj, boolean empty) {
                super.updateItem(obj, empty);
                if (empty || obj == null) { setText(null); setGraphic(null); return; }

                Text titre = new Text("🎯 " + obj.getTitre());
                titre.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

                Label desc = new Label(obj.getDescription());
                desc.setStyle("-fx-text-fill: #374151; -fx-font-size: 13;");
                desc.setWrapText(true);

                Label lblInfo = new Label(String.format("📂 %s   |   💪 %s   |   ⏱️ %d jours",
                        obj.getCategorie(), obj.getDifficulte(), obj.getDureeEstimee()));
                lblInfo.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12;");

                VBox content = new VBox(6, titre, desc, lblInfo);

                if (obj.getDateCreation() != null) {
                    SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
                    Label date = new Label("📅 Adopté le : " + fmt.format(obj.getDateCreation()));
                    date.setStyle("-fx-text-fill: #95A3B0; -fx-font-size: 11;");
                    content.getChildren().add(date);
                }

                content.setPadding(new Insets(4, 0, 4, 0));
                HBox.setHgrow(content, Priority.ALWAYS);

                Button btnModifier  = new Button("✏️ Modifier");
                Button btnSupprimer = new Button("🗑️ Supprimer");
                btnModifier.getStyleClass().add("btn-icon-edit");
                btnSupprimer.getStyleClass().add("btn-icon-delete");
                btnModifier.setOnAction(e -> handleModifier(obj));
                btnSupprimer.setOnAction(e -> handleSupprimer(obj));

                HBox actions = new HBox(8, btnModifier, btnSupprimer);
                actions.setAlignment(Pos.CENTER_RIGHT);

                HBox row = new HBox(16, content, actions);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 14, 12, 14));

                setText(null);
                setGraphic(row);
            }
        });

        chargerObjectifs();
        chargerMeteo();  // ✅ Chargement météo au démarrage
    }

    // =========================================================================
    //  MÉTÉO
    // =========================================================================

    private void chargerMeteo() {
        new Thread(() -> {
            try {
                List<WeatherService.PrevisionJour> previsions =
                        WeatherService.getPrevisions7Jours();

                Platform.runLater(() -> afficherMeteo(previsions));

            } catch (Exception e) {
                Platform.runLater(() -> {
                    if (lblMeteoErreur != null)
                        lblMeteoErreur.setText("⚠️ Météo indisponible (serveur Flask arrêté)");
                });
            }
        }).start();
    }

    private void afficherMeteo(List<WeatherService.PrevisionJour> previsions) {
        if (meteoContainer == null) return;
        meteoContainer.getChildren().clear();

        for (WeatherService.PrevisionJour p : previsions) {

            // ── Emoji + couleur de fond selon météo ──────────────────────────
            String emoji;
            String bgColor;
            String borderColor;

            switch (p.prediction.toLowerCase()) {
                case "sunny"        -> { emoji = "☀️";  bgColor = "#FFFBEB"; borderColor = "#FCD34D"; }
                case "rainy"        -> { emoji = "🌧️"; bgColor = "#EFF6FF"; borderColor = "#93C5FD"; }
                case "cloudy"       -> { emoji = "☁️";  bgColor = "#F9FAFB"; borderColor = "#D1D5DB"; }
                case "snowy"        -> { emoji = "❄️";  bgColor = "#F0F9FF"; borderColor = "#BAE6FD"; }
                case "foggy"        -> { emoji = "🌫️"; bgColor = "#F3F4F6"; borderColor = "#9CA3AF"; }
                case "drizzle"      -> { emoji = "🌦️"; bgColor = "#ECFDF5"; borderColor = "#6EE7B7"; }
                case "thunderstorm" -> { emoji = "⛈️"; bgColor = "#FAF5FF"; borderColor = "#C4B5FD"; }
                default             -> { emoji = "🌤️"; bgColor = "#ECFDF5"; borderColor = "#b7e4c7"; }
            }

            // ── Carte principale ─────────────────────────────────────────────
            VBox carte = new VBox(8);
            carte.setAlignment(Pos.CENTER);
            carte.setPadding(new Insets(16, 18, 16, 18));
            carte.setMinWidth(115);
            carte.setMaxWidth(115);
            carte.setStyle(
                    "-fx-background-color: " + bgColor + ";" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: " + borderColor + ";" +
                            "-fx-border-width: 2;" +
                            "-fx-border-radius: 14;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 10, 0, 0, 3);"
            );

            // Jour
            Label lblJour = new Label(p.label);
            lblJour.setStyle(
                    "-fx-font-weight: bold;" +
                            "-fx-font-size: 13;" +
                            "-fx-text-fill: #1B4332;" +
                            "-fx-alignment: center;"
            );
            lblJour.setMaxWidth(Double.MAX_VALUE);
            lblJour.setAlignment(Pos.CENTER);

            // Séparateur fin
            Region sep = new Region();
            sep.setPrefHeight(1);
            sep.setStyle("-fx-background-color: " + borderColor + ";");

            // Emoji météo — rendu via Text pour fiabilité
            javafx.scene.text.Text txtEmoji = new javafx.scene.text.Text(emoji);
            txtEmoji.setStyle("-fx-font-size: 32;");
            javafx.scene.layout.StackPane emojiPane = new javafx.scene.layout.StackPane(txtEmoji);
            emojiPane.setAlignment(Pos.CENTER);
            emojiPane.setPrefHeight(44);

            // Température
            Label lblTemp = new Label(p.tempMin + "° / " + p.tempMax + "°");
            lblTemp.setStyle(
                    "-fx-font-size: 13;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: #111827;"
            );
            lblTemp.setMaxWidth(Double.MAX_VALUE);
            lblTemp.setAlignment(Pos.CENTER);

            // Pluie
            Label lblPluie = new Label("● " + p.precipitation + " mm");
            lblPluie.setStyle(
                    "-fx-font-size: 11;" +
                            "-fx-text-fill: #3B82F6;"
            );
            lblPluie.setMaxWidth(Double.MAX_VALUE);
            lblPluie.setAlignment(Pos.CENTER);

            // Vent
            Label lblVent = new Label("⇒ " + p.windspeed + " km/h");
            lblVent.setStyle(
                    "-fx-font-size: 11;" +
                            "-fx-text-fill: #6B7280;"
            );
            lblVent.setMaxWidth(Double.MAX_VALUE);
            lblVent.setAlignment(Pos.CENTER);

            carte.getChildren().addAll(lblJour, sep, emojiPane, lblTemp, lblPluie, lblVent);
            meteoContainer.getChildren().add(carte);
        }
    }

    // =========================================================================
    //  LOGIQUE MODIFIER
    // =========================================================================

    private void handleModifier(Objectif obj) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterObjectif.fxml"));
            Parent view = loader.load();
            AjouterObjectifController ctrl = loader.getController();
            ctrl.setPageRetour("/MesObjectifs.fxml");
            ctrl.initModification(obj);

            if (parentHomeController != null) {
                ctrl.setUserHomeController(parentHomeController);
                parentHomeController.loadView(view);
                return;
            }

            Stage stage = (Stage) listViewObjectifs.getScene().getWindow();
            double w = stage.getWidth(); double h = stage.getHeight(); boolean max = stage.isMaximized();
            stage.setScene(new Scene(view));
            stage.setWidth(w); stage.setHeight(h);
            if (max) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }

    // =========================================================================
    //  LOGIQUE SUPPRIMER
    // =========================================================================

    private void handleSupprimer(Objectif obj) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer : " + obj.getTitre());
        confirmation.setContentText("Cette action est irréversible. Voulez-vous continuer ?");

        Optional<ButtonType> resultat = confirmation.showAndWait();
        if (resultat.isPresent() && resultat.get() == ButtonType.OK) {
            try {
                objectifService.delete(obj);
                new Alert(Alert.AlertType.INFORMATION, "Objectif supprimé avec succès !").show();
                chargerObjectifs();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
            }
        }
    }

    // =========================================================================
    //  CHARGEMENT OBJECTIFS
    // =========================================================================

    private void chargerObjectifs() {
        try {
            int userId;
            try {
                userId = Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur());
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "ID utilisateur invalide.").show();
                return;
            }

            listeObjectifsFull = objectifService.getObjectifsPatient(userId);
            currentPage = 0;
            applyPagination();

            if (listeObjectifsFull.isEmpty()) {
                Label messageVide = new Label(
                        "Aucun objectif adopté pour le moment.\n" +
                                "Passez un test pour découvrir des objectifs recommandés !");
                messageVide.setStyle("-fx-font-size: 14; -fx-text-fill: #5A6475;");
                messageVide.setWrapText(true);
                VBox vbox = new VBox(messageVide);
                vbox.setAlignment(Pos.CENTER);
                vbox.setPadding(new Insets(50));
                listViewObjectifs.setPlaceholder(vbox);
            }

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
        }
    }

    private void applyPagination() {
        int total      = listeObjectifsFull.size();
        int totalPages = PaginationHelper.getTotalPages(total);
        currentPage    = Math.min(currentPage, Math.max(0, totalPages - 1));

        listViewObjectifs.setItems(FXCollections.observableArrayList(
                PaginationHelper.getPageItems(listeObjectifsFull, currentPage)));

        if (paginationContainer != null) {
            paginationContainer.getChildren().clear();
            if (totalPages > 1) {
                PaginationHelper.PaginationBar bar = PaginationHelper.createPaginationBarWithRef(
                        currentPage, totalPages, total,
                        () -> { currentPage--; applyPagination(); },
                        () -> { currentPage++; applyPagination(); });
                paginationContainer.getChildren().add(bar.container);
            }
        }
    }

    // =========================================================================
    //  ACTIONS FXML
    // =========================================================================

    @FXML
    void modifierObjectif(ActionEvent event) {
        Objectif sel = listViewObjectifs.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un objectif.").show(); return; }
        handleModifier(sel);
    }

    @FXML
    void supprimerObjectif(ActionEvent event) {
        Objectif sel = listViewObjectifs.getSelectionModel().getSelectedItem();
        if (sel == null) { new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un objectif.").show(); return; }
        handleSupprimer(sel);
    }

    @FXML
    void actualiser(ActionEvent event) {
        chargerObjectifs();
        chargerMeteo();
    }

    // =========================================================================
    //  LECTURE AUDIO
    // =========================================================================

    @FXML
    public void lireDescription() {
        if (TextToSpeechService.isEnLecture()) return;
        Objectif selected = listViewObjectifs.getSelectionModel().getSelectedItem();
        if (selected == null) { new Alert(Alert.AlertType.WARNING, "Sélectionnez un objectif.").show(); return; }
        TextToSpeechService.parler(selected.getTitre() + ". " + selected.getDescription());
    }

    @FXML
    public void arreterLecture() { TextToSpeechService.arreter(); }

    // =========================================================================
    //  CHATBOT IA
    // =========================================================================

    @FXML
    public void envoyerMessage() {
        String msg = chatInput.getText().trim();
        if (msg.isEmpty()) return;

        chatArea.appendText("🧑 Vous: " + msg + "\n\n");
        chatInput.clear();
        chatLoading.setVisible(true);

        String prompt = """
                Tu es un assistant psychologue bienveillant et encourageant.
                Contexte utilisateur: %s
                
                Question: %s
                
                Réponds en français, de façon courte (max 120 mots),
                chaleureuse et professionnelle.
                """.formatted(userContext, msg);

        new Thread(() -> {
            String response = GeminiService.askGemini(prompt);
            Platform.runLater(() -> {
                chatArea.appendText("🤖 Assistant: " + response + "\n\n─────────────\n\n");
                chatArea.setScrollTop(Double.MAX_VALUE);
                chatLoading.setVisible(false);
            });
        }).start();
    }

    @FXML
    public void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) envoyerMessage();
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================

    @FXML
    void retour(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadChoisirTest(); return; }
        try { naviguerVers(event, FXMLLoader.load(getClass().getResource("/ChoisirTest.fxml"))); }
        catch (IOException e) { new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show(); }
    }

    @FXML
    void handleNavigateToChoisirTest(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadChoisirTest(); return; }
        try { naviguerVers(event, FXMLLoader.load(getClass().getResource("/ChoisirTest.fxml"))); }
        catch (IOException e) { new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show(); }
    }

    @FXML
    void handleNavigateToMesResultats(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadMesResultats(); return; }
        try { naviguerVers(event, FXMLLoader.load(getClass().getResource("/MesResultats.fxml"))); }
        catch (IOException e) { new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show(); }
    }

    private void naviguerVers(ActionEvent event, Parent root) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        double w = stage.getWidth(); double h = stage.getHeight(); boolean max = stage.isMaximized();
        stage.setScene(new Scene(root));
        stage.setWidth(w); stage.setHeight(h);
        if (max) stage.setMaximized(true);
        stage.show();
    }
}
