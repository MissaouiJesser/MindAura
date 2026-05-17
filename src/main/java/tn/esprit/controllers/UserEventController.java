package tn.esprit.controllers;

import com.google.gson.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;
import tn.esprit.services.EvenementService;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.GeocodingService;
import tn.esprit.services.OverpassService;
import tn.esprit.utils.FileStorageUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UserEventController {

    // ── FXML refs ─────────────────────────────────────────────────
    @FXML private FlowPane         userEventContainer;
    @FXML private TextField        userSearchField;
    @FXML private ComboBox<String> sortCombo;

    private final EvenementService evenementService = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();
    private UserHomeController     parentHomeController;
    private List<Evenements>       allEvents;

    // ── API Groq (assistant intelligent) ──────────────────────────
    private static final String GROQ_API_KEY = "";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";
    private static final String API_URL      = "https://api.groq.com/openai/v1/chat/completions";
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();

    // ── Injection parent ──────────────────────────────────────────
    public void setParentHomeController(UserHomeController c) {
        this.parentHomeController = c;
    }

    // ── Init ──────────────────────────────────────────────────────
    @FXML
    public void initialize() throws SQLException {
        sortCombo.getItems().addAll(
                "Titre A → Z", "Titre Z → A",
                "Date (récent)", "Date (ancien)",
                "Capacité (desc)"
        );
        loadEvents();
    }

    private void loadEvents() throws SQLException {
        allEvents = evenementService.getAllEvenements();
        renderEvents(allEvents);
    }

    // ── Filtrage / Tri ────────────────────────────────────────────
    @FXML
    public void filterEvents() {
        String kw = userSearchField.getText().trim().toLowerCase();
        String sort = sortCombo.getValue();

        List<Evenements> filtered = allEvents.stream()
                .filter(e -> kw.isEmpty()
                        || contains(e.getTitre_evenement(), kw)
                        || contains(e.getLieu_evenement(), kw)
                        || contains(e.getTypeEvenement(), kw)
                        || contains(e.getStatut_evenemnt(), kw))
                .collect(Collectors.toList());

        if (sort != null) {
            switch (sort) {
                case "Titre A → Z" -> filtered.sort(Comparator.comparing(Evenements::getTitre_evenement, String.CASE_INSENSITIVE_ORDER));
                case "Titre Z → A" -> filtered.sort(Comparator.comparing(Evenements::getTitre_evenement, String.CASE_INSENSITIVE_ORDER).reversed());
                case "Date (récent)" -> filtered.sort((a, b) -> {
                    if (a.getDatedebut_evenemnt() == null) return 1;
                    if (b.getDatedebut_evenemnt() == null) return -1;
                    return b.getDatedebut_evenemnt().compareTo(a.getDatedebut_evenemnt());
                });
                case "Date (ancien)" -> filtered.sort((a, b) -> {
                    if (a.getDatedebut_evenemnt() == null) return 1;
                    if (b.getDatedebut_evenemnt() == null) return -1;
                    return a.getDatedebut_evenemnt().compareTo(b.getDatedebut_evenemnt());
                });
                case "Capacité (desc)" -> filtered.sort((a, b) -> b.getCapacite_evenement() - a.getCapacite_evenement());
            }
        }
        renderEvents(filtered);
    }

    private boolean contains(String s, String kw) {
        return s != null && s.toLowerCase().contains(kw);
    }

    // ── Rendu des cartes événements ───────────────────────────────
    private void renderEvents(List<Evenements> events) {
        userEventContainer.getChildren().clear();
        if (events.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(80));
            Label ico = new Label("📭");
            ico.setStyle("-fx-font-size:46px;");
            Label msg = new Label("Aucun événement disponible");
            msg.setStyle("-fx-text-fill:#5A6475; -fx-font-size:16px; -fx-font-weight:700;");
            Label sub = new Label("Revenez bientôt pour découvrir nos prochains événements.");
            sub.setStyle("-fx-text-fill:#8899AA; -fx-font-size:12px;");
            empty.getChildren().addAll(ico, msg, sub);
            userEventContainer.getChildren().add(empty);
            return;
        }

        int i = 0;
        for (Evenements ev : events) {
            Node card = buildCard(ev);
            userEventContainer.getChildren().add(card);
            FadeTransition ft = new FadeTransition(Duration.millis(260), card);
            ft.setDelay(Duration.millis(i * 35));
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            i++;
        }
    }

    private Node buildCard(Evenements event) {
        VBox card = new VBox(0);
        card.setPrefWidth(290);
        card.setMaxWidth(290);
        card.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#E2E8F0; -fx-border-radius:16; -fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(27,67,50,0.08),14,0,0,3);" +
                        "-fx-cursor:hand;");

        // ✅ CORRECTION : résolution robuste de l'image via FileStorageUtil
        String imgPath = event.getImage();
        boolean imgLoaded = false;
        if (imgPath != null && !imgPath.isBlank()) {
            try {
                File f = FileStorageUtil.resolveImageFile(imgPath);
                if (f != null && f.exists()) {
                    Image img = new Image(new FileInputStream(f), 290, 150, false, true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(290); iv.setFitHeight(150);
                    iv.setPreserveRatio(false);
                    StackPane imgPane = new StackPane(iv);
                    imgPane.setMinHeight(150); imgPane.setMaxHeight(150);
                    imgPane.setStyle("-fx-background-radius:14 14 0 0;");
                    card.getChildren().add(imgPane);
                    imgLoaded = true;
                }
            } catch (Exception ignored) {}
        }
        if (!imgLoaded) card.getChildren().add(buildPlaceholder(event));

        // Corps
        VBox body = new VBox(10);
        body.setPadding(new Insets(14, 16, 16, 16));
        VBox.setVgrow(body, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.TOP_LEFT);
        Label title = new Label(event.getTitre_evenement() != null ? event.getTitre_evenement() : "—");
        title.setStyle("-fx-text-fill:#1A1A2E; -fx-font-size:14px; -fx-font-weight:800; -fx-font-family:'Georgia';");
        title.setWrapText(true);
        title.setMaxWidth(190);
        HBox.setHgrow(title, Priority.ALWAYS);
        titleRow.getChildren().add(title);

        if (event.getStatut_evenemnt() != null) {
            String sc = getStatusColor(event.getStatut_evenemnt());
            Label badge = new Label(event.getStatut_evenemnt());
            badge.setStyle("-fx-background-color:" + sc + "1A; -fx-background-radius:20; -fx-padding:2 8;" +
                    "-fx-text-fill:" + sc + "; -fx-font-size:9px; -fx-font-weight:700;" +
                    "-fx-border-color:" + sc + "40; -fx-border-radius:20; -fx-border-width:1;");
            titleRow.getChildren().add(badge);
        }

        String dateStr = (event.getDatedebut_evenemnt() != null && event.getDatefin_evenemnt() != null)
                ? "📅 " + event.getDatedebut_evenemnt() + " → " + event.getDatefin_evenemnt()
                : "📅 Date non définie";
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill:#5A6475; -fx-font-size:11px;");
        dateLabel.setWrapText(true);

        Label lieuLabel = new Label("📍 " + (event.getLieu_evenement() != null ? event.getLieu_evenement() : "—"));
        lieuLabel.setStyle("-fx-text-fill:#5A6475; -fx-font-size:11px;");

        boolean isFull = event.getCapacite_evenement() <= 0;
        Label capLabel = new Label(isFull ? "🔴 Complet" : "👥 " + event.getCapacite_evenement() + " places");
        capLabel.setStyle("-fx-text-fill:" + (isFull ? "#EF4444" : "#1B4332") + "; -fx-font-size:11px; -fx-font-weight:600;");

        Region sep = new Region();
        sep.setStyle("-fx-background-color:#E2E8F0; -fx-pref-height:1; -fx-max-height:1;");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);

        Button inscBtn = new Button("✅  S'inscrire");
        inscBtn.setStyle("-fx-background-color:#1B4332; -fx-text-fill:#FFFFFF; -fx-font-size:11.5px; -fx-font-weight:700; -fx-background-radius:10; -fx-padding:8 14; -fx-cursor:hand;");
        inscBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(inscBtn, Priority.ALWAYS);
        if (isFull) { inscBtn.setDisable(true); inscBtn.setOpacity(0.5); }
        inscBtn.setOnAction(e -> openParticipationForm(event));

        Button chatBtn = new Button("💬");
        chatBtn.setStyle("-fx-background-color:#EDE9FE; -fx-text-fill:#7B5EA7; -fx-font-size:13px; -fx-background-radius:10; -fx-padding:8 12; -fx-cursor:hand;");
        chatBtn.setTooltip(new Tooltip("Chat du groupe"));
        chatBtn.setOnAction(e -> openGroupChat(event));

        Button mapBtn = new Button("🗺️");
        mapBtn.setStyle("-fx-background-color:#E6F4EA; -fx-text-fill:#1B4332; -fx-font-size:14px; -fx-background-radius:10; -fx-padding:8 12; -fx-cursor:hand;");
        mapBtn.setTooltip(new Tooltip("Voir sur la carte"));
        mapBtn.setOnAction(e -> openMapModal(event));

        actions.getChildren().addAll(inscBtn, chatBtn, mapBtn);

        body.getChildren().addAll(titleRow, dateLabel, lieuLabel, capLabel, sep, actions);
        card.getChildren().add(body);

        String baseStyle = card.getStyle();
        card.setOnMouseEntered(e -> card.setStyle(baseStyle
                .replace("-fx-border-color:#E2E8F0;", "-fx-border-color:#1B4332;")
                .replace("-fx-effect:dropshadow(gaussian,rgba(27,67,50,0.08),14,0,0,3);",
                        "-fx-effect:dropshadow(gaussian,rgba(27,67,50,0.18),18,0,0,6);")));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        return card;
    }

    private StackPane buildPlaceholder(Evenements event) {
        String color = getTypeColor(event.getTypeEvenement());
        StackPane ph = new StackPane();
        ph.setMinHeight(120); ph.setMaxHeight(120);
        ph.setStyle("-fx-background-color:linear-gradient(to bottom right," + color + "33," + color + "11); -fx-background-radius:14 14 0 0;");
        Label ico = new Label(getTypeEmoji(event.getTypeEvenement()));
        ico.setStyle("-fx-font-size:34px;");
        ph.getChildren().add(ico);
        if (event.getTypeEvenement() != null && !event.getTypeEvenement().isEmpty()) {
            Label badge = new Label(event.getTypeEvenement());
            badge.setStyle("-fx-background-color:" + color + "33; -fx-background-radius:20; -fx-padding:3 10; -fx-text-fill:" + color + "; -fx-font-size:10px; -fx-font-weight:700; -fx-border-color:" + color + "55; -fx-border-width:1; -fx-border-radius:20;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
            ph.getChildren().add(badge);
        }
        return ph;
    }

    // ── Actions ───────────────────────────────────────────────────
    private void openParticipationForm(Evenements event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ParticipationForm.fxml"));
            Parent root = loader.load();
            ParticipationFormController ctrl = loader.getController();
            ctrl.setEvent(event);
            ctrl.setOnSuccess(() -> { try { loadEvents(); } catch (Exception ignored) {} });
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("S'inscrire — " + event.getTitre_evenement());
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openGroupChat(Evenements event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/GroupChatView.fxml"));
            Parent root = loader.load();
            GroupChatController ctrl = loader.getController();
            ctrl.setEvent(event);
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("💬 Chat — " + event.getTitre_evenement());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void goRecommandations() {
        if (parentHomeController != null) parentHomeController.loadInCenter("/RecommandationView.fxml");
    }

    @FXML public void goBack() {
        if (parentHomeController != null) parentHomeController.restoreHome();
    }

    // ═══════════════════════════════════════════════════════════════
    //  CARTE GLOBALE (tous événements) - conservée mais bouton retiré du FXML
    // ═══════════════════════════════════════════════════════════════
    @FXML
    public void openGlobalMap() {
        if (allEvents == null || allEvents.isEmpty()) {
            showAlert("Aucun événement à afficher sur la carte.");
            return;
        }
        showAlert("Fonctionnalité carte globale disponible.\nÉvénements chargés : " + allEvents.size());
    }

    // ═══════════════════════════════════════════════════════════════
    //  CARTE POUR UN ÉVÉNEMENT (ouvre une fenêtre modale)
    // ═══════════════════════════════════════════════════════════════
    private void openMapModal(Evenements event) {
        String lieu  = event.getLieu_evenement();
        String titre = event.getTitre_evenement();

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.DECORATED);
        modal.setTitle("📍 " + titre + " — Lieu & environs");
        modal.setMinWidth(960);
        modal.setMinHeight(680);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#1a1f2e;");

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.setStyle("-fx-background-color:#1e2a38;-fx-border-color:rgba(82,183,136,0.2);-fx-border-width:0 0 1 0;");
        Label ico  = new Label("📍"); ico.setStyle("-fx-font-size:20px;");
        Label tit  = new Label(titre); tit.setStyle("-fx-font-size:15px;-fx-font-weight:700;-fx-text-fill:#52B788;");
        Label addr = new Label(lieu != null ? lieu : "Lieu non renseigné");
        addr.setStyle("-fx-font-size:12px;-fx-text-fill:#8899aa;");
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getChildren().addAll(
                makeLegendDot("#e74c3c", "📍 Événement"),
                makeLegendDot("#3498db", "🏨 Hôtels"),
                makeLegendDot("#e67e22", "🍽 Restaurants")
        );
        header.getChildren().addAll(ico, tit, addr, hSpacer, legend);
        root.setTop(header);

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0");
        root.setCenter(webView);

        ProgressBar progressBar = new ProgressBar(-1);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(3);
        progressBar.setStyle("-fx-accent:#52B788;-fx-background-color:transparent;");
        Label statusLbl = new Label("Géolocalisation du lieu en cours…");
        statusLbl.setStyle("-fx-text-fill:#8899aa;-fx-font-size:11px;-fx-padding:0 0 0 10;");
        HBox statusBar = new HBox(8, progressBar, statusLbl);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        statusBar.setPadding(new Insets(6, 12, 6, 12));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color:#161b27;");
        root.setBottom(statusBar);

        // Charger la carte de base (centre Tunis par défaut)
        engine.loadContent(buildMapHtml(36.8188, 10.1657, titre, lieu));

        // Géocodage + POIs
        CompletableFuture
                .supplyAsync(() -> GeocodingService.geocode(lieu != null ? lieu : "Tunis", event.getId()))
                .thenApplyAsync(coords -> {
                    double lat = coords[0], lng = coords[1];
                    String poisJson = "[]";
                    try {
                        poisJson = OverpassService.fetchPOIs(lat, lng);
                    } catch (Exception ex) { ex.printStackTrace(); }
                    return new Object[]{coords, poisJson};
                })
                .thenAccept(result -> Platform.runLater(() -> {
                    double[] coords   = (double[]) result[0];
                    String  poisJson  = (String)   result[1];
                    double  lat       = coords[0],  lng = coords[1];

                    engine.getLoadWorker().stateProperty().addListener((obs, oldSt, newSt) -> {
                        if (newSt == Worker.State.SUCCEEDED) {
                            injectMapData(engine, lat, lng, titre, lieu, poisJson, statusLbl, progressBar);
                        }
                    });
                    if (engine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                        injectMapData(engine, lat, lng, titre, lieu, poisJson, statusLbl, progressBar);
                    } else {
                        engine.loadContent(buildMapHtml(lat, lng, titre, lieu));
                        statusLbl.setText("Lieu trouvé · Chargement des hôtels et restaurants…");
                    }
                }));

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                progressBar.setVisible(false);
                statusLbl.setText("Carte chargée · " + (lieu != null ? lieu : ""));
            } else if (newState == Worker.State.RUNNING) {
                progressBar.setVisible(true);
            }
        });

        Scene scene = new Scene(root, 960, 680);
        modal.setScene(scene);
        modal.show();
    }

    private String buildMapHtml(double lat, double lng, String titre, String lieu) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<style>" +
                "* {margin:0;padding:0;}" +
                "#map {width:100vw;height:100vh;}" +
                ".p-title{font-size:13px;font-weight:700;color:#52B788;}" +
                ".p-sub{font-size:11px;color:#8899aa;}" +
                ".p-badge{display:inline-block;padding:2px 8px;border-radius:10px;font-size:10px;font-weight:700;margin-top:6px;}" +
                ".badge-event{background:rgba(231,76,60,0.2);color:#e74c3c;border:1px solid #e74c3c;}" +
                ".badge-hotel{background:rgba(52,152,219,0.2);color:#3498db;border:1px solid #3498db;}" +
                ".badge-resto{background:rgba(230,126,34,0.2);color:#e67e22;border:1px solid #e67e22;}" +
                "</style>" +
                "</head><body><div id='map'></div>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<script>" +
                "function makeIcon(color,size){size=size||28;var svg='<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"'+size+'\" height=\"'+(size*1.3)+'\" viewBox=\"0 0 28 36\"><path d=\"M14 0C6.27 0 0 6.27 0 14c0 9.33 14 22 14 22S28 23.33 28 14C28 6.27 21.73 0 14 0z\" fill=\"'+color+'\"/><circle cx=\"14\" cy=\"14\" r=\"6\" fill=\"white\" opacity=\"0.9\"/></svg>';return L.divIcon({html:svg,className:'',iconSize:[size,size*1.3],iconAnchor:[size/2,size*1.3],popupAnchor:[0,-size*1.3]});}" +
                "var icons={event:makeIcon('#e74c3c',36),hotel:makeIcon('#3498db',26),resto:makeIcon('#e67e22',26)};" +
                "var map=L.map('map').setView([" + lat + "," + lng + "],14);" +
                "L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png',{attribution:'© CARTO',maxZoom:19,subdomains:'abcd'}).addTo(map);" +
                "</script></body></html>";
    }

    private void injectMapData(WebEngine engine, double lat, double lng,
                               String titre, String lieu, String poisJson,
                               Label statusLbl, ProgressBar progressBar) {
        try {
            String safeJson = poisJson.replace("\\", "\\\\").replace("'", "\\'");
            String js =
                    "map.setView([" + lat + "," + lng + "], 15);" +
                            "if(window._evMarker) map.removeLayer(window._evMarker);" +
                            "window._evMarker = L.marker([" + lat + "," + lng + "], {icon: icons.event, zIndexOffset:1000}).addTo(map);" +
                            "window._evMarker.bindPopup('<div class=\"p-title\">🎪 " + escapeJs(titre) + "</div><div class=\"p-sub\">📍 " + escapeJs(lieu != null ? lieu : "") + "</div><span class=\"p-badge badge-event\">📍 Lieu de l\\'événement</span>',{maxWidth:260}).openPopup();" +
                            "if(window._poiLayers){window._poiLayers.forEach(function(l){map.removeLayer(l);});}" +
                            "window._poiLayers = [];" +
                            "(function(){ var pois = JSON.parse('" + safeJson + "'); var hotelsCount=0, restoCount=0;" +
                            "  pois.forEach(function(poi){ if(!poi.lat||!poi.lng) return;" +
                            "    var isHotel = poi.type === 'hotel' || poi.type === 'guest_house' || poi.type === 'motel';" +
                            "    var icon    = isHotel ? icons.hotel : icons.resto;" +
                            "    var badgeClass = isHotel ? 'badge-hotel' : 'badge-resto';" +
                            "    var badgeLabel = isHotel ? '🏨 Hôtel' : '🍽 Restaurant';" +
                            "    var distTxt  = poi.dist < 1000 ? poi.dist + ' m' : (poi.dist/1000).toFixed(1) + ' km';" +
                            "    var walkMins = poi.walkMinutes || Math.round((poi.dist||0)/70);" +
                            "    var walkTxt  = walkMins <= 1 ? 'moins d\\'1 min' : walkMins + ' min à pied';" +
                            "    var stars    = poi.stars > 0 ? '★'.repeat(Math.min(poi.stars,5)) : '';" +
                            "    var cuisine  = poi.cuisine ? ' · '+poi.cuisine : '';" +
                            "    var popup    = '<div class=\"p-title\">' + (poi.name||badgeLabel) + ' ' + stars + '</div>'" +
                            "      + '<div class=\"p-walk\">🚶 ' + walkTxt + ' · ' + distTxt + '</div>'" +
                            "      + '<div class=\"p-sub\">📍 du lieu de l\\'événement' + cuisine + '</div>'" +
                            "      + (poi.phone   ? '<div class=\"p-sub\">📞 ' + poi.phone + '</div>' : '')" +
                            "      + (poi.opening_hours ? '<div class=\"p-sub\">🕙 ' + poi.opening_hours + '</div>' : '')" +
                            "      + '<span class=\"p-badge ' + badgeClass + '\">' + badgeLabel + '</span>';" +
                            "    var m = L.marker([poi.lat, poi.lng], {icon:icon}).addTo(map).bindPopup(popup,{maxWidth:260});" +
                            "    m.bindTooltip((poi.name||badgeLabel) + ' — ' + walkTxt, {direction:'top'});" +
                            "    window._poiLayers.push(m);" +
                            "    if(isHotel) hotelsCount++; else restoCount++;" +
                            "  });" +
                            "  console.log('[POI] Hôtels: '+hotelsCount+', Restos: '+restoCount);" +
                            "})();";
            engine.executeScript(js);
            progressBar.setVisible(false);
            int total = (int) poisJson.chars().filter(c -> c == '{').count();
            statusLbl.setText("Carte chargée · " + (lieu != null ? lieu : "") + " · " + total + " lieu(x) à proximité");
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // ═══════════════════════════════════════════════════════════════
    //  CHATBOT INTELLIGENT (API Groq)
    // ═══════════════════════════════════════════════════════════════
    @FXML
    public void openChatbot() {
        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle("🤖 Assistant IA (Groq)");
        stage.setMinWidth(450);
        stage.setMinHeight(600);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#F8FAFE;");

        VBox chatArea = new VBox(10);
        chatArea.setPadding(new Insets(15));
        chatArea.setStyle("-fx-background-color:#FFFFFF;");
        ScrollPane scrollPane = new ScrollPane(chatArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        root.setCenter(scrollPane);

        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(12));
        inputBox.setStyle("-fx-background-color:#EDF2F7;");
        TextField userInput = new TextField();
        userInput.setPromptText("Posez votre question...");
        userInput.setStyle("-fx-background-radius:20; -fx-padding:10 15; -fx-font-size:13px;");
        HBox.setHgrow(userInput, Priority.ALWAYS);
        Button sendBtn = new Button("Envoyer");
        sendBtn.setStyle("-fx-background-color:#1B4332; -fx-text-fill:white; -fx-background-radius:20; -fx-padding:8 18;");
        inputBox.getChildren().addAll(userInput, sendBtn);
        root.setBottom(inputBox);

        addChatMessage(chatArea, "🤖", "Bonjour ! Je suis l'assistant IA MindAura.\nJe peux vous parler de nos événements, des places disponibles, vous recommander des ateliers, concerts, etc.\nPosez-moi votre question !", scrollPane);

        sendBtn.setOnAction(e -> processQuery(userInput.getText(), chatArea, userInput, scrollPane));
        userInput.setOnAction(e -> processQuery(userInput.getText(), chatArea, userInput, scrollPane));

        Scene scene = new Scene(root, 480, 650);
        stage.setScene(scene);
        stage.show();
    }

    private void processQuery(String query, VBox chatArea, TextField inputField, ScrollPane scrollPane) {
        if (query == null || query.trim().isEmpty()) return;
        String q = query.trim();
        addChatMessage(chatArea, "👤", q, scrollPane);
        inputField.clear();

        CompletableFuture.supplyAsync(() -> {
            try {
                String context = buildContextData();
                return callGroqAPI(q, context);
            } catch (Exception e) {
                return "❌ Erreur API : " + e.getMessage() + "\nVérifiez votre clé Groq ou la connexion Internet.";
            }
        }).thenAccept(response -> Platform.runLater(() -> addChatMessage(chatArea, "🤖", response, scrollPane)));
    }

    private String callGroqAPI(String userMessage, String contextData) throws IOException {
        String systemPrompt = "Tu es un assistant spécialisé dans les événements MindAura.\n" +
                "Voici les données en temps réel :\n" + contextData + "\n\n" +
                "Règles : réponds en français, utilise les données fournies, sois précis et chaleureux. " +
                "Pour les listes, utilise des puces. Ne mentionne pas les IDs internes.";

        JsonArray messages = new JsonArray();
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        Map<String, String> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("content", userMessage);
        conversationHistory.add(userTurn);

        int start = Math.max(0, conversationHistory.size() - 10);
        for (int i = start; i < conversationHistory.size(); i++) {
            Map<String, String> turn = conversationHistory.get(i);
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", turn.get("role"));
            msgObj.addProperty("content", turn.get("content"));
            messages.add(msgObj);
        }

        JsonObject body = new JsonObject();
        body.addProperty("model", GROQ_MODEL);
        body.addProperty("max_tokens", 1024);
        body.addProperty("temperature", 0.7);
        body.add("messages", messages);

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder raw = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) raw.append(line);
        }

        if (code != 200) {
            throw new IOException("HTTP " + code + " : " + raw);
        }

        String assistantText = JsonParser.parseString(raw.toString())
                .getAsJsonObject()
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        Map<String, String> asstTurn = new HashMap<>();
        asstTurn.put("role", "assistant");
        asstTurn.put("content", assistantText);
        conversationHistory.add(asstTurn);

        return assistantText;
    }

    private String buildContextData() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        try {
            List<Evenements> events = evenementService.getAllEvenements();
            sb.append("=== BASE DE DONNÉES MINDÁURA ===\n");
            sb.append("Nombre total d'événements : ").append(events.size()).append("\n\n");

            for (Evenements ev : events) {
                sb.append("- ").append(ev.getTitre_evenement())
                        .append(" (").append(ev.getTypeEvenement()).append(")")
                        .append(", lieu : ").append(ev.getLieu_evenement())
                        .append(", statut : ").append(ev.getStatut_evenemnt())
                        .append(", capacité : ").append(ev.getCapacite_evenement()).append(" places");
                if (ev.getDatedebut_evenemnt() != null)
                    sb.append(", début : ").append(sdf.format(ev.getDatedebut_evenemnt()));
                sb.append("\n");
            }

            try {
                List<Participation> parts = participationService.afficherList();
                Map<Integer, Long> countByEvent = parts.stream()
                        .collect(Collectors.groupingBy(Participation::getEvenementId, Collectors.counting()));

                sb.append("\nOCCUPATION :\n");
                for (Evenements ev : events) {
                    long inscrits = countByEvent.getOrDefault(ev.getId(), 0L);
                    long restantes = ev.getCapacite_evenement() - inscrits;
                    sb.append(ev.getTitre_evenement()).append(" : ")
                            .append(inscrits).append(" inscrits");
                    if (restantes > 0) sb.append(", ").append(restantes).append(" places libres");
                    else sb.append(" (COMPLET)");
                    sb.append("\n");
                }
            } catch (Exception e) {
                sb.append("\n(Données de participation non disponibles)\n");
            }
        } catch (SQLException e) {
            sb.append("Erreur de connexion à la base de données.");
        }
        return sb.toString();
    }

    private void addChatMessage(VBox container, String sender, String message, ScrollPane scrollPane) {
        HBox msgBox = new HBox(10);
        msgBox.setAlignment(sender.equals("🤖") ? Pos.TOP_LEFT : Pos.TOP_RIGHT);
        Label avatar = new Label(sender);
        avatar.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-background-radius:30; -fx-padding:6; -fx-background-color:#E2E8F0;");
        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(380);
        text.setStyle("-fx-background-color:" + (sender.equals("🤖") ? "#EDF2F7" : "#1B4332") + "; -fx-text-fill:" + (sender.equals("🤖") ? "#1A202C" : "#FFFFFF") + "; -fx-padding:10 14; -fx-background-radius:12; -fx-font-size:13px;");
        if (sender.equals("🤖")) msgBox.getChildren().addAll(avatar, text);
        else msgBox.getChildren().addAll(text, avatar);
        container.getChildren().add(msgBox);
        if (scrollPane != null) Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    // ── Helpers ────────────────────────────────────────────────────
    private HBox makeLegendDot(String color, String label) {
        Region dot = new Region();
        dot.setPrefSize(10, 10); dot.setMaxSize(10, 10);
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:50%;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:#8899aa;-fx-font-size:11px;");
        HBox box = new HBox(6, dot, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private String getTypeColor(String type) {
        if (type == null) return "#1B4332";
        return switch (type.toLowerCase()) {
            case "concert" -> "#8B5CF6";
            case "sport" -> "#10B981";
            case "conférence" -> "#4F6EF7";
            case "atelier" -> "#F59E0B";
            case "exposition" -> "#22D3EE";
            case "networking" -> "#EC4899";
            default -> "#1B4332";
        };
    }

    private String getTypeEmoji(String type) {
        if (type == null) return "📅";
        return switch (type.toLowerCase()) {
            case "concert" -> "🎵";
            case "sport" -> "⚽";
            case "conférence" -> "🎤";
            case "atelier" -> "🛠";
            case "exposition" -> "🖼";
            case "networking" -> "🤝";
            default -> "📅";
        };
    }

    private String getStatusColor(String status) {
        if (status == null) return "#8899AA";
        return switch (status.toLowerCase()) {
            case "actif" -> "#10B981";
            case "complet" -> "#EF4444";
            case "annulé" -> "#F59E0B";
            case "planifié" -> "#22D3EE";
            default -> "#8899AA";
        };
    }

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}