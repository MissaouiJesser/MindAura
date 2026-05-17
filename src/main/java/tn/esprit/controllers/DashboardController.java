package tn.esprit.controllers;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.SessionManager;

public class DashboardController implements Initializable {

    public Button btnReclamation;
    // ─── Sidebar / Topbar ────────────────────────────────────────────────────
    @FXML private Button    btnDashboard;
    @FXML private Button    btnLocaux;
    @FXML private Button    btnReservations;
    @FXML private Button    btnSalles;
    @FXML private Button    btnUtilisateurs;
    @FXML private Button    btnTraitements;
    @FXML private Button    btnTest;
    @FXML private Button    btnObjectif;
    @FXML private Button    btnRessources;
    @FXML private Button    btnEvenements;
    @FXML private Button    btnParticipants;
    @FXML private Button    btnToggleSidebar;   // ← nouveau bouton toggle
    @FXML private VBox      sidebar;            // ← référence à la sidebar
    @FXML private StackPane avatarPane;
    @FXML private ImageView avatarImageView;
    @FXML private Label     avatarLabel;
    @FXML private Label     userNameLabel;
    @FXML private Label     userRoleLabel;
    @FXML private Label     topbarTitle;
    @FXML private Label     topbarSubtitle;
    @FXML private StackPane contentArea;

    // ─── Widget météo ─────────────────────────────────────────────────────────
    @FXML private HBox  weatherWidget;
    @FXML private Label weatherIcon;
    @FXML private Label weatherTempLabel;
    @FXML private Label weatherDescLabel;
    @FXML private Label weatherCityLabel;
    @FXML private Label topbarDate;

    // ─── Constantes météo ─────────────────────────────────────────────────────
    private static final String OWM_API_KEY = "252790d466d5c3f3057431236c802988";
    private static final String OWM_CITY    = "Tunis";
    private static final String OWM_COUNTRY = "TN";
    private static final String OWM_LANG    = "fr";
    private static final String OWM_UNITS   = "metric";

    private static final int WEATHER_REFRESH_MINUTES = 1;

    // ─── État sidebar ─────────────────────────────────────────────────────────
    private boolean sidebarVisible = true;
    private static final double SIDEBAR_WIDTH   = 220.0;  // largeur normale
    private static final double ANIM_DURATION_MS = 300.0; // durée animation ms

    private Button activeBtn;

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUserInfo();
        setupDateClock();
        setupWeather();
        loadView("DashboardHome.fxml", "Tableau de Bord", "Vue d'ensemble de MindAura");
        setActive(btnDashboard);
    }

    // =========================================================================
    //  TOGGLE SIDEBAR — logique principale
    // =========================================================================

    /**
     * Masque ou affiche la sidebar avec une animation de glissement fluide.
     * Appelé par le bouton ☰ dans la topbar (onAction="#toggleSidebar").
     */
    @FXML
    private void toggleSidebar() {
        if (sidebarVisible) {
            hideSidebar();
        } else {
            showSidebar();
        }
    }

    /** Masque la sidebar en la faisant glisser vers la gauche. */
    private void hideSidebar() {
        sidebarVisible = false;

        // Animation : réduire la largeur préférée à 0 + fade out
        Timeline anim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(sidebar.prefWidthProperty(), sidebar.getWidth()),
                        new KeyValue(sidebar.opacityProperty(), 1.0),
                        new KeyValue(sidebar.minWidthProperty(), sidebar.getWidth())
                ),
                new KeyFrame(Duration.millis(ANIM_DURATION_MS),
                        new KeyValue(sidebar.prefWidthProperty(), 0),
                        new KeyValue(sidebar.opacityProperty(), 0.0),
                        new KeyValue(sidebar.minWidthProperty(), 0)
                )
        );
        anim.setOnFinished(e -> {
            sidebar.setManaged(false);
            sidebar.setVisible(false);
            // Remettre les propriétés pour le re-show
            sidebar.setPrefWidth(SIDEBAR_WIDTH);
            sidebar.setMinWidth(SIDEBAR_WIDTH);
        });
        anim.play();

        // Mettre à jour le bouton toggle : flèche droite pour indiquer "rouvrir"
        updateToggleButton(false);
    }

    /** Réaffiche la sidebar avec une animation de glissement depuis la gauche. */
    private void showSidebar() {
        sidebarVisible = true;

        // Rendre visible AVANT l'animation
        sidebar.setVisible(true);
        sidebar.setManaged(true);
        sidebar.setPrefWidth(0);
        sidebar.setMinWidth(0);
        sidebar.setOpacity(0.0);

        Timeline anim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(sidebar.prefWidthProperty(), 0),
                        new KeyValue(sidebar.opacityProperty(), 0.0),
                        new KeyValue(sidebar.minWidthProperty(), 0)
                ),
                new KeyFrame(Duration.millis(ANIM_DURATION_MS),
                        new KeyValue(sidebar.prefWidthProperty(), SIDEBAR_WIDTH),
                        new KeyValue(sidebar.opacityProperty(), 1.0),
                        new KeyValue(sidebar.minWidthProperty(), SIDEBAR_WIDTH)
                )
        );
        anim.setOnFinished(e -> {
            // Restaurer les valeurs naturelles (laisser le CSS gérer la largeur)
            sidebar.setPrefWidth(Region.USE_COMPUTED_SIZE);
            sidebar.setMinWidth(Region.USE_COMPUTED_SIZE);
        });
        anim.play();

        // Mettre à jour le bouton toggle : hamburger pour indiquer "masquer"
        updateToggleButton(true);
    }

    /**
     * Met à jour l'icône et le tooltip du bouton toggle selon l'état.
     * @param sidebarIsOpen true = sidebar ouverte, false = fermée
     */
    private void updateToggleButton(boolean sidebarIsOpen) {
        if (btnToggleSidebar == null) return;
        // Swap the graphic FontIcon — no text overlay
        org.kordamp.ikonli.javafx.FontIcon icon = new org.kordamp.ikonli.javafx.FontIcon();
        icon.setIconSize(17);
        icon.setIconColor(javafx.scene.paint.Color.WHITE);
        if (sidebarIsOpen) {
            icon.setIconLiteral("fas-bars");
            javafx.scene.control.Tooltip.install(btnToggleSidebar,
                    new javafx.scene.control.Tooltip("Masquer le menu"));
        } else {
            icon.setIconLiteral("fas-angle-right");
            javafx.scene.control.Tooltip.install(btnToggleSidebar,
                    new javafx.scene.control.Tooltip("Afficher le menu"));
        }
        btnToggleSidebar.setGraphic(icon);
        btnToggleSidebar.setText("");
    }

    // =========================================================================
    //  MÉTÉO — Setup & Appel API
    // =========================================================================

    private void setupWeather() {
        fetchWeatherAsync();

        Timeline refreshTimer = new Timeline(
                new KeyFrame(
                        Duration.minutes(WEATHER_REFRESH_MINUTES),
                        e -> fetchWeatherAsync()
                )
        );
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
        refreshTimer.play();
    }

    private void fetchWeatherAsync() {
        Thread t = new Thread(() -> {
            try {
                String url = String.format(
                        "https://api.openweathermap.org/data/2.5/weather" +
                                "?q=%s,%s&appid=%s&units=%s&lang=%s",
                        OWM_CITY, OWM_COUNTRY, OWM_API_KEY, OWM_UNITS, OWM_LANG
                );

                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(8))
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .GET()
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    WeatherData data = parseWeatherJson(response.body());
                    Platform.runLater(() -> updateWeatherUI(data));
                } else if (response.statusCode() == 401) {
                    Platform.runLater(() -> setWeatherError("Clé API invalide"));
                } else {
                    Platform.runLater(() -> setWeatherError("Erreur " + response.statusCode()));
                }

            } catch (java.net.UnknownHostException e) {
                Platform.runLater(() -> setWeatherError("Hors ligne"));
            } catch (Exception e) {
                Platform.runLater(() -> setWeatherError("Météo indisponible"));
                System.err.println("[Météo] " + e.getMessage());
            }
        }, "weather-fetch-thread");
        t.setDaemon(true);
        t.start();
    }

    private WeatherData parseWeatherJson(String json) {
        WeatherData d = new WeatherData();

        d.temp        = extractDouble(json, "\"temp\"");
        d.humidity    = (int) extractDouble(json, "\"humidity\"");
        d.windSpeed   = extractDouble(json, "\"speed\"");
        d.description = extractString(json, "\"description\"");
        d.iconCode    = extractString(json, "\"icon\"");
        d.cityName    = extractString(json, "\"name\"");

        return d;
    }

    private void updateWeatherUI(WeatherData d) {
        if (weatherTempLabel == null) return;

        weatherTempLabel.setText(String.format("%.0f°C", d.temp));
        weatherDescLabel.setText(capitalize(d.description));
        weatherCityLabel.setText(d.cityName.isEmpty() ? OWM_CITY : d.cityName);
        weatherIcon.setText(owmIconToEmoji(d.iconCode));

        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                String.format("Humidité : %d%%\nVent : %.1f km/h", d.humidity, d.windSpeed * 3.6)
        );
        javafx.scene.control.Tooltip.install(weatherWidget, tip);

        FadeTransition fade = new FadeTransition(Duration.millis(500), weatherWidget);
        fade.setFromValue(0.6);
        fade.setToValue(1.0);
        fade.play();
    }

    private void setWeatherError(String msg) {
        if (weatherTempLabel == null) return;
        weatherIcon.setText("🌐");
        weatherTempLabel.setText("--°C");
        weatherDescLabel.setText(msg);
    }

    private void setupDateClock() {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm", new Locale("fr", "FR"));
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(30), e ->
                updateClock(timeFmt)));
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
        updateClock(timeFmt);
    }

    private void updateClock(DateTimeFormatter fmt) {
        if (topbarDate != null)
            topbarDate.setText(LocalDateTime.now().format(fmt));
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0.0;
        String rest = json.substring(idx + key.length()).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        StringBuilder sb = new StringBuilder();
        for (char c : rest.toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == '-') sb.append(c);
            else if (sb.length() > 0) break;
        }
        try { return sb.length() > 0 ? Double.parseDouble(sb.toString()) : 0.0; }
        catch (NumberFormatException e) { return 0.0; }
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        String rest = json.substring(idx + key.length()).trim();
        if (rest.startsWith(":")) rest = rest.substring(1).trim();
        if (!rest.startsWith("\"")) return "";
        rest = rest.substring(1);
        int end = rest.indexOf("\"");
        return end >= 0 ? rest.substring(0, end) : "";
    }

    private String owmIconToEmoji(String code) {
        if (code == null || code.isEmpty()) return "🌤";
        return switch (code.substring(0, 2)) {
            case "01" -> code.endsWith("d") ? "☀️"  : "🌙";
            case "02" -> "⛅";
            case "03" -> "🌥";
            case "04" -> "☁️";
            case "09" -> "🌧";
            case "10" -> "🌦";
            case "11" -> "⛈";
            case "13" -> "❄️";
            case "50" -> "🌫";
            default   -> "🌤";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ─── Modèle de données météo ──────────────────────────────────────────────
    private static class WeatherData {
        double temp;
        int    humidity;
        double windSpeed;
        String description = "";
        String iconCode    = "";
        String cityName    = "";
    }

    // =========================================================================
    //  ACCESSEURS PUBLICS (utilisés par les sous-contrôleurs)
    // =========================================================================

    public StackPane getContentArea() { return contentArea; }

    public Stage getStage() {
        if (contentArea == null || contentArea.getScene() == null) return null;
        return (Stage) contentArea.getScene().getWindow();
    }

    public void updateTopbar(String title, String subtitle) {
        topbarTitle.setText(title);
        topbarSubtitle.setText(subtitle);
    }

    public void maintainActiveButton(String section) {
        switch (section.toLowerCase()) {
            case "dashboard"    -> setActive(btnDashboard);
            case "locaux"       -> setActive(btnLocaux);
            case "reservations" -> setActive(btnReservations);
            case "salles"       -> { if (btnSalles != null) setActive(btnSalles); }
            case "utilisateurs" -> setActive(btnUtilisateurs);
            case "traitements"  -> setActive(btnTraitements);
            case "tests"        -> { if (btnTest != null) setActive(btnTest); }
            case "objectifs"    -> { if (btnObjectif != null) setActive(btnObjectif); }
            case "reclamations" -> { if (btnReclamation != null) setActive(btnReclamation); }
            case "ressources"   -> { if (btnRessources != null) setActive(btnRessources); }
            case "evenements"   -> { if (btnEvenements  != null) setActive(btnEvenements); }
            case "participants" -> { if (btnParticipants != null) setActive(btnParticipants); }
        }
    }

    // ─── Navigation ──────────────────────────────────────────────────────────

    @FXML private void showDashboard()    { loadView("DashboardHome.fxml",      "Tableau de Bord",           "Vue d'ensemble de MindAura");   setActive(btnDashboard);   }
    @FXML private void showLocaux()       { loadView("AfficherLocal.fxml",      "Locaux Psychiatriques",     "Gestion des établissements de santé mentale");   setActive(btnLocaux);      }
    @FXML private void showReservations() { loadView("AfficherReservation.fxml","Réservations",              "Planification et horaires");    setActive(btnReservations); }
    @FXML private void showSalles()       { loadView("AfficherSalle.fxml",      "Salles",                    "Gestion des salles");           if (btnSalles != null) setActive(btnSalles); }
    @FXML private void showUtilisateurs() { loadView("ListeUtilisateurs.fxml",  "Liste Des Utilisateurs",    "Liste et gestion des comptes"); setActive(btnUtilisateurs); }
    @FXML private void showTraitements()  { loadView("ListeTraitements.fxml",   "Liste Des Traitements",     "Programmes thérapeutiques");    setActive(btnTraitements); }

    @FXML private void showTests() {
        loadView("AfficherTest.fxml", "Tests psychologiques", "Gestion des tests psychologiques");
        if (btnTest != null) setActive(btnTest);
    }

    @FXML private void showObjectifs() {
        loadView("GestionObjectifs.fxml", "Objectifs", "Gestion des objectifs");
        if (btnObjectif != null) setActive(btnObjectif);
    }

    @FXML
    private void showRessources() {
        loadView("AfficherRe.fxml", "Gestion des ressources", "Découvrir notre ressources et ses commentaires");
        if (btnRessources != null) setActive(btnRessources);
    }

    @FXML
    private void showEvenements() {
        loadView("AdminEventsView.fxml", "Gestion des Événements", "Liste et gestion des événements");
        if (btnEvenements != null) setActive(btnEvenements);
    }

    @FXML
    private void showParticipants() {
        loadView("AdminParticipantsView.fxml", "Gestion des Participants", "Liste et gestion des participants");
        if (btnParticipants != null) setActive(btnParticipants);
    }

    @FXML
    private void showReclamations() {
        loadView("AdminAccueil.fxml", "Réclamations et Réponses", "Gestion des réclamations et ses réponses");
        if (btnReclamation != null) setActive(btnReclamation);
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        Stage stage = (Stage) contentArea.getScene().getWindow();
        NavigationManager.navigateTo("Login.fxml", "MindAura – Connexion", stage, false);
    }

    public void loadView(String fxmlFile, String title, String subtitle) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/" + fxmlFile));
            Node view = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardAware aware) {
                aware.setDashboardController(this);
            }

            view.setOpacity(0);
            contentArea.getChildren().setAll(view);
            if (view instanceof Region r) {
                r.setMaxWidth(Double.MAX_VALUE);
                r.setMaxHeight(Double.MAX_VALUE);
                r.prefWidthProperty().bind(contentArea.widthProperty());
                r.prefHeightProperty().bind(contentArea.heightProperty());
            }
            FadeTransition ft = new FadeTransition(Duration.millis(280), view);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            topbarTitle.setText(title);
            topbarSubtitle.setText(subtitle);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Object loadViewAndGet(String fxmlFile, String title, String subtitle) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/" + fxmlFile));
            Node view = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof DashboardAware aware) {
                aware.setDashboardController(this);
            }

            view.setOpacity(0);
            contentArea.getChildren().setAll(view);
            if (view instanceof Region r) {
                r.setMaxWidth(Double.MAX_VALUE);
                r.setMaxHeight(Double.MAX_VALUE);
                r.prefWidthProperty().bind(contentArea.widthProperty());
                r.prefHeightProperty().bind(contentArea.heightProperty());
            }
            FadeTransition ft = new FadeTransition(Duration.millis(280), view);
            ft.setFromValue(0); ft.setToValue(1); ft.play();

            topbarTitle.setText(title);
            topbarSubtitle.setText(subtitle);

            switch (fxmlFile) {
                case "GestionQuestions.fxml",
                     "AjouterQuestion.fxml"  -> { if (btnTest != null) setActive(btnTest); }
                case "AdminAccueil.fxml", "AdminReclamationDetail.fxml", "AdminStatistique.fxml",
                     "ModifierReclamationForm.fxml", "ModifierReponse.fxml" -> { if (btnReclamation != null) setActive(btnReclamation); }
                case "AfficherRe.fxml", "ajouter.fxml", "modifierRessources.fxml", "PDFViewer.fxml", "pdfViewer.fxml", "VideoPlayer.fxml", "videoPlayer.fxml" -> { if (btnRessources != null) setActive(btnRessources); }
                case "AdminEventsView.fxml", "EventForm.fxml"     -> { if (btnEvenements  != null) setActive(btnEvenements); }
                case "AdminParticipantsView.fxml"                 -> { if (btnParticipants != null) setActive(btnParticipants); }
                case "AfficherSalle.fxml", "AjouterSalle.fxml", "ModifierSalle.fxml" -> { if (btnSalles != null) setActive(btnSalles); }
                case "AfficherLocal.fxml", "AjouterLocal.fxml", "ModifierLocal.fxml" -> setActive(btnLocaux);
                case "AfficherReservation.fxml", "AjouterReservation.fxml", "ModifierReservation.fxml" -> setActive(btnReservations);
                default                      -> {}
            }

            return ctrl;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void navigateTo(String fxmlFile, String title, String subtitle) {
        loadView(fxmlFile, title, subtitle);
        switch (fxmlFile) {
            case "DashboardHome.fxml"          -> setActive(btnDashboard);
            case "AfficherLocal.fxml"          -> setActive(btnLocaux);
            case "AfficherReservation.fxml"    -> setActive(btnReservations);
            case "AfficherSalle.fxml"          -> { if (btnSalles != null) setActive(btnSalles); }
            case "ListeUtilisateurs.fxml"      -> setActive(btnUtilisateurs);
            case "ListeTraitements.fxml"       -> setActive(btnTraitements);
            case "AfficherTest.fxml"           -> { if (btnTest != null) setActive(btnTest); }
            case "GestionObjectifs.fxml"       -> { if (btnObjectif != null) setActive(btnObjectif); }
            case "AjouterUtilisateur.fxml", "ModifierUtilisateur.fxml" -> setActive(btnUtilisateurs);
            case "AjouterTraitement.fxml", "ModifierTraitement.fxml"   -> setActive(btnTraitements);
            case "AjouterLocal.fxml", "ModifierLocal.fxml"             -> setActive(btnLocaux);
            case "AjouterReservation.fxml", "ModifierReservation.fxml" -> setActive(btnReservations);
            case "AjouterSalle.fxml", "ModifierSalle.fxml"             -> { if (btnSalles != null) setActive(btnSalles); }
            case "AdminAccueil.fxml", "AdminReclamationDetail.fxml", "AdminStatistique.fxml", "ModifierReclamationForm.fxml", "ModifierReponse.fxml" -> { if (btnReclamation != null) setActive(btnReclamation); }
            case "AfficherRe.fxml", "ajouter.fxml", "modifierRessources.fxml", "PDFViewer.fxml", "VideoPlayer.fxml" -> { if (btnRessources != null) setActive(btnRessources); }
            case "AdminEventsView.fxml", "EventForm.fxml"          -> { if (btnEvenements  != null) setActive(btnEvenements); }
            case "AdminParticipantsView.fxml"                      -> { if (btnParticipants != null) setActive(btnParticipants); }
            default -> {}
        }
    }

    private void setActive(Button btn) {
        if (activeBtn != null) {
            activeBtn.getStyleClass().remove("sidebar-btn-active");
            if (!activeBtn.getStyleClass().contains("sidebar-btn"))
                activeBtn.getStyleClass().add("sidebar-btn");
        }
        btn.getStyleClass().add("sidebar-btn-active");
        activeBtn = btn;
    }

    private void setupUserInfo() {
        avatarLabel.setText(SessionManager.getInitiales());
        userNameLabel.setText(SessionManager.getNomComplet());
        userRoleLabel.setText(SessionManager.getRoleLibelle());
        loadUserPhoto();
    }

    private void loadUserPhoto() {
        String photoPath = SessionManager.getPhotoPath();
        if (photoPath != null && !photoPath.isBlank()) {
            try {
                File photoFile = Paths.get("C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/avatars/" + photoPath).toFile();
                if (photoFile.exists()) {
                    Image img = new Image(photoFile.toURI().toString(), 36, 36, false, true);
                    avatarImageView.setImage(img);
                    avatarImageView.setVisible(true);
                    avatarLabel.setVisible(false);
                    return;
                }
            } catch (Exception e) {
                System.out.println("Photo de profil introuvable : " + e.getMessage());
            }
        }
        avatarImageView.setVisible(false);
        avatarLabel.setVisible(true);
    }

    /** Interface pour les contrôleurs enfants */
    public interface DashboardAware {
        void setDashboardController(DashboardController dc);
    }
}