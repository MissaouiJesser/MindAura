package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import netscape.javascript.JSObject;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.services.local_psychiatrie_SERVICE;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CarteGeographiqueController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private WebView          carteWebView;
    @FXML private ComboBox<String> comboVille;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField        txtRecherche;
    @FXML private Button           btnRechercher;
    @FXML private Button           btnReinitialiser;
    @FXML private Button           btnRetour;
    @FXML private Label            lblNbLocaux;
    @FXML private Label            lblNbDisponibles;
    @FXML private Label            lblNbVilles;
    @FXML private VBox             loadingOverlay;   // spinner de chargement

    // ── Data ──────────────────────────────────────────────────────
    private local_psychiatrie_SERVICE localService;
    private List<local_psychiatrie>   tousLesLocaux;
    private List<local_psychiatrie>   locauxFiltres;
    private WebEngine                 webEngine;
    private JavaBridge                javaBridge;

    // Référence au contrôleur parent UserHome pour la navigation inline
    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    // ── Coordonnées des villes tunisiennes ────────────────────────
    private static final Map<String, double[]> COORDS_VILLES = new LinkedHashMap<String, double[]>() {{
        put("Tunis",          new double[]{36.8065, 10.1815});
        put("Ariana",         new double[]{36.8625, 10.1956});
        put("Ben Arous",      new double[]{36.7533, 10.2287});
        put("La Marsa",       new double[]{36.8786, 10.3245});
        put("Sfax",           new double[]{34.7398, 10.7600});
        put("Sousse",         new double[]{35.8256, 10.6369});
        put("Monastir",       new double[]{35.7643, 10.8113});
        put("Kairouan",       new double[]{35.6781, 10.0963});
        put("Bizerte",        new double[]{37.2744,  9.8739});
        put("Nabeul",         new double[]{36.4561, 10.7376});
        put("Hammamet",       new double[]{36.4000, 10.6167});
        put("Gabès",          new double[]{33.8828, 10.0982});
        put("Gafsa",          new double[]{34.4250,  8.7842});
        put("Médenine",       new double[]{33.3549, 10.5055});
        put("Jendouba",       new double[]{36.5011,  8.7802});
        put("Kélibia",        new double[]{36.8500, 11.1000});
        put("Tozeur",         new double[]{33.9197,  8.1336});
    }};

    // ── Couleurs par type ─────────────────────────────────────────
    private static final Map<String, String> COULEURS_TYPE = new LinkedHashMap<String, String>() {{
        put("HOPITAL",                 "#EF4444");
        put("CABINET_PRIVE",           "#7C3AED");
        put("CLINIQUE_PSYCHIATRIQUE",  "#3B82F6");
        put("CENTRE_DE_SANTE_MENTALE", "#10B981");
        put("CENTRE_DE_BIEN_ETRE",     "#F59E0B");
        put("ESPACE_DE_THERAPIE",      "#EC4899");
    }};

    // ─────────────────────────────────────────────────────────────
    //  INITIALISATION
    // ─────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        localService = new local_psychiatrie_SERVICE();
        javaBridge   = new JavaBridge();

        chargerDonnees();
        initialiserFiltres();
        initialiserCarte();
    }

    private void chargerDonnees() {
        try {
            tousLesLocaux = localService.afficherList();
            locauxFiltres = new ArrayList<>(tousLesLocaux);
        } catch (SQLException e) {
            tousLesLocaux = new ArrayList<>();
            locauxFiltres = new ArrayList<>();
            showError("Erreur de chargement : " + e.getMessage());
        }
    }

    private void initialiserFiltres() {
        // Villes
        List<String> villes = tousLesLocaux.stream()
                .map(local_psychiatrie::getVille_local)
                .distinct().sorted().collect(Collectors.toList());
        villes.add(0, "Toutes les villes");
        comboVille.setItems(FXCollections.observableArrayList(villes));
        comboVille.setValue("Toutes les villes");

        // Types
        comboType.setItems(FXCollections.observableArrayList(
                "Tous les types", "Hôpital", "Cabinet privé",
                "Clinique psychiatrique", "Centre de santé mentale",
                "Centre de bien-être", "Espace de thérapie"
        ));
        comboType.setValue("Tous les types");

        comboVille.valueProperty().addListener((obs, o, n) -> appliquerFiltres());
        comboType.valueProperty().addListener((obs, o, n)  -> appliquerFiltres());

        mettreAJourCompteurs();
    }

    // ─────────────────────────────────────────────────────────────
    //  CARTE LEAFLET (WebView)
    // ─────────────────────────────────────────────────────────────

    private void initialiserCarte() {
        webEngine = carteWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);

        // ✅ FIX : le WebKit de JavaFX envoie un User-Agent vide par défaut
        // ce qui cause le refus des tuiles par CartoDB/OSM → carte blanche au zoom
        webEngine.setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        // Quand la page est chargée → injecter le pont Java et les marqueurs
        webEngine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", javaBridge);

                // ⚠️ La map est initialisée dans window.onload > setTimeout(0) côté JS.
                // On doit attendre qu'elle soit prête avant d'ajouter les marqueurs.
                // On utilise un PauseTransition pour laisser le event loop JS s'exécuter.
                PauseTransition attente = new PauseTransition(Duration.millis(800));
                attente.setOnFinished(ev -> {
                    chargerMarqueursJS(locauxFiltres);
                    // Forcer Leaflet à recalculer sa taille après que JavaFX a finalisé le layout
                    try { webEngine.executeScript("if(map) map.invalidateSize(true);"); } catch(Exception ignored) {}
                });
                attente.play();

                // Masquer le spinner
                if (loadingOverlay != null) {
                    FadeTransition ft = new FadeTransition(Duration.millis(500), loadingOverlay);
                    ft.setDelay(Duration.millis(600));
                    ft.setToValue(0);
                    ft.setOnFinished(e -> {
                        loadingOverlay.setVisible(false);
                        loadingOverlay.setManaged(false);
                    });
                    ft.play();
                }
            }
        });

        webEngine.loadContent(genererHtmlCarte());
    }

    // ─────────────────────────────────────────────────────────────
    //  HTML DE LA CARTE — Leaflet avec style cartographique moderne
    // ─────────────────────────────────────────────────────────────

    private String genererHtmlCarte() {
        // =====================================================================
        // CORRECTIONS APPLIQUÉES :
        // 1. Scripts Leaflet chargés depuis CDN jsDelivr (plus stable que unpkg)
        // 2. Emojis remplacés par entités HTML (évite la corruption WebKit JavaFX)
        // 3. setMaxBounds() supprimé (bloquait le zoom)
        // 4. crossOrigin: false sur les tuiles (JavaFX WebKit ne gère pas CORS)
        // 5. preferCanvas:true pour un meilleur rendu au zoom
        // =====================================================================
        return "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset='UTF-8'/>\n"
                + "  <meta name='viewport' content='width=device-width,initial-scale=1'/>\n"
                + "  <title>MindAura Map</title>\n"

                // ── CSS Leaflet INLINE (les <link> CDN sont ignorés par JavaFX WebKit) ──
                + "  <style>\n"

                // ── Reset + layout ──
                + "* { margin:0; padding:0; box-sizing:border-box; }\n"
                + "html, body { width:100%; height:100%; overflow:hidden; font-family:Arial,sans-serif; }\n"
                + "#map { position:absolute; top:0; left:0; right:0; bottom:0; width:100%; height:100%; }\n"

                // ── Leaflet core CSS (inliné) ──
                + ".leaflet-pane,.leaflet-tile,.leaflet-marker-icon,.leaflet-marker-shadow,"
                + ".leaflet-tile-container,.leaflet-pane>svg,.leaflet-pane>canvas,"
                + ".leaflet-zoom-box,.leaflet-image-layer,.leaflet-layer{"
                + "position:absolute;left:0;top:0}\n"
                + ".leaflet-container{overflow:hidden}\n"
                + ".leaflet-tile,.leaflet-marker-icon,.leaflet-marker-shadow{-webkit-user-select:none;-moz-user-select:none;user-select:none;-webkit-user-drag:none}\n"
                + ".leaflet-tile::selection{background:0 0}\n"
                + ".leaflet-safari .leaflet-tile{image-rendering:-webkit-optimize-contrast}\n"
                + ".leaflet-safari .leaflet-tile-container{width:1600px;height:1600px;-webkit-transform-origin:0 0}\n"
                + ".leaflet-marker-icon,.leaflet-marker-shadow{display:block}\n"
                + ".leaflet-container .leaflet-overlay-pane svg{max-width:none!important;max-height:none!important}\n"
                + ".leaflet-container.leaflet-touch-zoom{-ms-touch-action:pan-x pan-y;touch-action:pan-x pan-y}\n"
                + ".leaflet-container.leaflet-touch-drag{-ms-touch-action:pinch-zoom;touch-action:none;touch-action:pinch-zoom}\n"
                + ".leaflet-container.leaflet-touch-drag.leaflet-touch-zoom{-ms-touch-action:none;touch-action:none}\n"
                + ".leaflet-container{-webkit-tap-highlight-color:transparent}\n"
                + ".leaflet-container a{-webkit-tap-highlight-color:rgba(51,181,229,.4)}\n"
                + ".leaflet-tile{filter:inherit;visibility:hidden}\n"
                + ".leaflet-tile-loaded{visibility:inherit}\n"
                + ".leaflet-zoom-box{width:0;height:0;-moz-box-sizing:border-box;box-sizing:border-box;z-index:800}\n"
                + ".leaflet-overlay-pane svg{-moz-user-select:none}\n"
                + ".leaflet-pane{z-index:400}\n"
                + ".leaflet-tile-pane{z-index:200}\n"
                + ".leaflet-overlay-pane{z-index:400}\n"
                + ".leaflet-shadow-pane{z-index:500}\n"
                + ".leaflet-marker-pane{z-index:600}\n"
                + ".leaflet-tooltip-pane{z-index:650}\n"
                + ".leaflet-popup-pane{z-index:700}\n"
                + ".leaflet-map-pane canvas{z-index:100}\n"
                + ".leaflet-map-pane svg{z-index:200}\n"
                + ".leaflet-vml-shape{width:1px;height:1px}\n"
                + ".lvml{behavior:url(#default#VML);display:inline-block;position:absolute}\n"
                + ".leaflet-control{position:relative;z-index:800;pointer-events:visiblePainted;pointer-events:auto}\n"
                + ".leaflet-top,.leaflet-bottom{position:absolute;z-index:1000;pointer-events:none}\n"
                + ".leaflet-top{top:0}\n"
                + ".leaflet-right{right:0}\n"
                + ".leaflet-bottom{bottom:0}\n"
                + ".leaflet-left{left:0}\n"
                + ".leaflet-control{float:left;clear:both}\n"
                + ".leaflet-right .leaflet-control{float:right}\n"
                + ".leaflet-top .leaflet-control{margin-top:10px}\n"
                + ".leaflet-bottom .leaflet-control{margin-bottom:10px}\n"
                + ".leaflet-left .leaflet-control{margin-left:10px}\n"
                + ".leaflet-right .leaflet-control{margin-right:10px}\n"
                + ".leaflet-fade-anim .leaflet-popup{opacity:0;-webkit-transition:opacity .2s linear;-moz-transition:opacity .2s linear;transition:opacity .2s linear}\n"
                + ".leaflet-fade-anim .leaflet-map-pane .leaflet-popup{opacity:1}\n"
                + ".leaflet-zoom-animated{-webkit-transform-origin:0 0;-ms-transform-origin:0 0;transform-origin:0 0}\n"
                + "svg.leaflet-zoom-animated{will-change:transform}\n"
                + ".leaflet-zoom-anim .leaflet-zoom-animated{-webkit-transition:-webkit-transform .25s cubic-bezier(0,0,.25,1);-moz-transition:-moz-transform .25s cubic-bezier(0,0,.25,1);transition:transform .25s cubic-bezier(0,0,.25,1)}\n"
                + ".leaflet-zoom-anim .leaflet-tile,.leaflet-pan-anim .leaflet-tile{-webkit-transition:none;-moz-transition:none;transition:none}\n"
                + ".leaflet-zoom-anim .leaflet-zoom-animated{will-change:transform}\n"
                + ".leaflet-interactive{cursor:pointer}\n"
                + ".leaflet-grab{cursor:-webkit-grab;cursor:-moz-grab;cursor:grab}\n"
                + ".leaflet-crosshair,.leaflet-crosshair .leaflet-interactive{cursor:crosshair}\n"
                + ".leaflet-popup-pane,.leaflet-control{cursor:auto}\n"
                + ".leaflet-dragging .leaflet-grab,.leaflet-dragging .leaflet-grab .leaflet-interactive,.leaflet-dragging .leaflet-marker-draggable{cursor:move;cursor:-webkit-grabbing;cursor:-moz-grabbing;cursor:grabbing}\n"
                + ".leaflet-marker-icon,.leaflet-marker-shadow,.leaflet-image-layer,.leaflet-pane>svg path,.leaflet-tile-container{pointer-events:none}\n"
                + ".leaflet-marker-icon.leaflet-interactive,.leaflet-image-layer.leaflet-interactive,.leaflet-pane>svg path.leaflet-interactive,svg.leaflet-image-layer.leaflet-interactive path{pointer-events:visiblePainted;pointer-events:auto}\n"
                + ".leaflet-container{background:#ddd;outline-offset:1px}\n"
                + ".leaflet-container a.leaflet-active{outline:2px solid orange}\n"
                + ".leaflet-zoom-box{border:2px dotted #38f;background:rgba(255,255,255,.5)}\n"
                + ".leaflet-bar{box-shadow:0 1px 5px rgba(0,0,0,.65);border-radius:4px}\n"
                + ".leaflet-bar a{background-color:#fff;border-bottom:1px solid #ccc;width:26px;height:26px;line-height:26px;display:block;text-align:center;text-decoration:none;color:black}\n"
                + ".leaflet-bar a,.leaflet-control-layers-toggle{background-position:50% 50%;background-repeat:no-repeat;display:block}\n"
                + ".leaflet-bar a:hover,.leaflet-bar a:focus{background-color:#f4f4f4}\n"
                + ".leaflet-bar a:first-child{border-top-left-radius:4px;border-top-right-radius:4px}\n"
                + ".leaflet-bar a:last-child{border-bottom-left-radius:4px;border-bottom-right-radius:4px;border-bottom:none}\n"
                + ".leaflet-bar a.leaflet-disabled{cursor:default;background-color:#f4f4f4;color:#bbb}\n"
                + ".leaflet-touch .leaflet-bar a{width:30px;height:30px;line-height:30px}\n"
                + ".leaflet-touch .leaflet-bar a:first-child{border-top-left-radius:2px;border-top-right-radius:2px}\n"
                + ".leaflet-touch .leaflet-bar a:last-child{border-bottom-left-radius:2px;border-bottom-right-radius:2px}\n"
                + ".leaflet-control-zoom-in,.leaflet-control-zoom-out{font:bold 18px 'Lucida Console',Monaco,monospace;text-indent:1px}\n"
                + ".leaflet-touch .leaflet-control-zoom-in{font-size:22px}\n"
                + ".leaflet-touch .leaflet-control-zoom-out{font-size:20px}\n"
                + ".leaflet-control-layers{box-shadow:0 1px 5px rgba(0,0,0,.4);background:#fff;border-radius:5px}\n"
                + ".leaflet-control-layers-toggle{background-image:url(https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/images/layers.png);width:36px;height:36px}\n"
                + ".leaflet-retina .leaflet-control-layers-toggle{background-image:url(https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/images/layers-2x.png);background-size:26px 26px}\n"
                + ".leaflet-control-layers .leaflet-control-layers-list,.leaflet-control-layers-expanded .leaflet-control-layers-toggle{display:none}\n"
                + ".leaflet-control-layers-expanded .leaflet-control-layers-list{display:block;position:relative}\n"
                + ".leaflet-control-layers-expanded{padding:6px 10px 6px 6px;color:#333;background:#fff}\n"
                + ".leaflet-control-layers-scrollbar{overflow-y:scroll;overflow-x:hidden;padding-right:5px}\n"
                + ".leaflet-control-layers-selector{margin-top:2px;position:relative;top:1px}\n"
                + ".leaflet-control-layers label{display:block;font-size:13px;font-size:1.08333em}\n"
                + ".leaflet-control-layers-separator{height:0;border-top:1px solid #ddd;margin:5px -10px 5px -6px}\n"
                + ".leaflet-default-icon-path{background-image:url(https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/images/marker-icon.png)}\n"
                + ".leaflet-container .leaflet-control-attribution{background:#fff;background:rgba(255,255,255,.8);margin:0}\n"
                + ".leaflet-control-attribution,.leaflet-control-scale-line{padding:0 5px;color:#333;line-height:1.4}\n"
                + ".leaflet-control-attribution a{text-decoration:none}\n"
                + ".leaflet-control-attribution a:hover,.leaflet-control-attribution a:focus{text-decoration:underline}\n"
                + ".leaflet-attribution-flag{display:inline!important;vertical-align:baseline!important;width:1em;height:0.6667em}\n"
                + ".leaflet-left .leaflet-control-scale{margin-left:5px}\n"
                + ".leaflet-bottom .leaflet-control-scale{margin-bottom:5px}\n"
                + ".leaflet-control-scale-line{border:2px solid #777;border-top:none;line-height:1.1;padding:2px 5px 1px;font-size:11px;white-space:nowrap;overflow:hidden;-moz-box-sizing:border-box;box-sizing:border-box;background:#fff;background:rgba(255,255,255,.5)}\n"
                + ".leaflet-control-scale-line:not(:first-child){border-top:2px solid #777;border-bottom:none;margin-top:-2px}\n"
                + ".leaflet-control-scale-line:not(:first-child):not(:last-child){border-bottom:2px solid #777}\n"
                + ".leaflet-touch .leaflet-control-attribution,.leaflet-touch .leaflet-control-layers,.leaflet-touch .leaflet-bar{box-shadow:none}\n"
                + ".leaflet-touch .leaflet-control-layers,.leaflet-touch .leaflet-bar{border:2px solid rgba(0,0,0,.2);background-clip:padding-box}\n"
                + ".leaflet-popup{position:absolute;text-align:center;margin-bottom:20px}\n"
                + ".leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#fff;color:#333;box-shadow:0 3px 14px rgba(0,0,0,.4)}\n"
                + ".leaflet-popup-content-wrapper{padding:1px;text-align:left;border-radius:12px}\n"
                + ".leaflet-popup-tip-container{width:40px;height:20px;position:absolute;left:50%;margin-left:-20px;overflow:hidden;pointer-events:none}\n"
                + ".leaflet-popup-tip{width:17px;height:17px;padding:1px;margin:-10px auto 0;-webkit-transform:rotate(45deg);-moz-transform:rotate(45deg);-ms-transform:rotate(45deg);transform:rotate(45deg)}\n"
                + ".leaflet-popup-content-wrapper,.leaflet-popup-tip{background:#fff;color:#333;box-shadow:0 3px 14px rgba(0,0,0,.4)}\n"
                + ".leaflet-popup-content{margin:13px 24px 13px 20px;line-height:1.3;font-size:13px;font-size:1.08333em;min-height:1px}\n"
                + ".leaflet-popup-content p{margin:17px 0;margin:1.3em 0}\n"
                + ".leaflet-popup-close-button{position:absolute;top:0;right:0;border:none;text-align:center;width:24px;height:24px;font:16px/24px Tahoma,Verdana,sans-serif;color:#757575;text-decoration:none;background:0 0}\n"
                + ".leaflet-popup-close-button:hover,.leaflet-popup-close-button:focus{color:#585858}\n"
                + ".leaflet-popup-scrolled{overflow-y:scroll;border-bottom:1px solid #ddd;border-top:1px solid #ddd}\n"
                + ".leaflet-oldie .leaflet-popup-content-wrapper{-ms-zoom:1}\n"
                + ".leaflet-oldie .leaflet-popup-tip{width:24px;-ms-filter:'progid:DXImageTransform.Microsoft.Matrix(M11=0.70710678, M12=0.70710678, M21=-0.70710678, M22=0.70710678)';filter:progid:DXImageTransform.Microsoft.Matrix(M11=0.70710678, M12=0.70710678, M21=-0.70710678, M22=0.70710678)}\n"
                + ".leaflet-oldie .leaflet-control-zoom,.leaflet-oldie .leaflet-control-layers,.leaflet-oldie .leaflet-popup-content-wrapper,.leaflet-oldie .leaflet-popup-tip{border:1px solid #999}\n"
                + ".leaflet-div-icon{background:#fff;border:1px solid #666}\n"
                + ".leaflet-tooltip{position:absolute;padding:6px;background-color:#fff;border:1px solid #fff;border-radius:3px;color:#222;white-space:nowrap;-webkit-user-select:none;-moz-user-select:none;user-select:none;pointer-events:none;box-shadow:0 1px 3px rgba(0,0,0,.4)}\n"
                + ".leaflet-tooltip.leaflet-interactive{cursor:pointer;pointer-events:auto}\n"
                + ".leaflet-tooltip-top:before,.leaflet-tooltip-bottom:before,.leaflet-tooltip-left:before,.leaflet-tooltip-right:before{position:absolute;pointer-events:none;border:6px solid transparent;background:0 0;content:''}\n"
                + ".leaflet-tooltip-bottom{margin-top:6px}.leaflet-tooltip-top{margin-top:-6px}\n"
                + ".leaflet-tooltip-bottom:before,.leaflet-tooltip-top:before{left:50%;margin-left:-6px}\n"
                + ".leaflet-tooltip-top:before{bottom:0;margin-bottom:-12px;border-top-color:#fff}\n"
                + ".leaflet-tooltip-bottom:before{top:0;margin-top:-12px;margin-left:-6px;border-bottom-color:#fff}\n"
                + ".leaflet-tooltip-left:before{top:50%;margin-top:-6px;right:0;margin-right:-12px;border-left-color:#fff}\n"
                + ".leaflet-tooltip-right:before{top:50%;margin-top:-6px;left:0;margin-left:-12px;border-right-color:#fff}\n"

                // ── MarkerCluster CSS inliné ──
                + ".leaflet-cluster-anim .leaflet-marker-icon,.leaflet-cluster-anim .leaflet-marker-shadow{-webkit-transition:-webkit-transform .3s ease-out,opacity .3s ease-in;-moz-transition:-moz-transform .3s ease-out,opacity .3s ease-in;-o-transition:-o-transform .3s ease-out,opacity .3s ease-in;transition:transform .3s ease-out,opacity .3s ease-in}\n"
                + ".leaflet-cluster-spider-leg{-webkit-transition:-webkit-transform .3s ease-out,opacity .3s ease-in;-moz-transition:-moz-transform .3s ease-out,opacity .3s ease-in;-o-transition:-o-transform .3s ease-out,opacity .3s ease-in;transition:transform .3s ease-out,opacity .3s ease-in}\n"
                + ".marker-cluster-small{background-color:rgba(181,226,140,.6)}\n"
                + ".marker-cluster-small div{background-color:rgba(110,204,57,.6)}\n"
                + ".marker-cluster-medium{background-color:rgba(241,211,87,.6)}\n"
                + ".marker-cluster-medium div{background-color:rgba(240,194,12,.6)}\n"
                + ".marker-cluster-large{background-color:rgba(253,156,115,.6)}\n"
                + ".marker-cluster-large div{background-color:rgba(241,128,23,.6)}\n"
                + ".leaflet-oldie .marker-cluster-small{background-color:rgb(181,226,140)}\n"
                + ".leaflet-oldie .marker-cluster-small div{background-color:rgb(110,204,57)}\n"
                + ".leaflet-oldie .marker-cluster-medium{background-color:rgb(241,211,87)}\n"
                + ".leaflet-oldie .marker-cluster-medium div{background-color:rgb(240,194,12)}\n"
                + ".leaflet-oldie .marker-cluster-large{background-color:rgb(253,156,115)}\n"
                + ".leaflet-oldie .marker-cluster-large div{background-color:rgb(241,128,23)}\n"
                + ".marker-cluster{background-clip:padding-box;border-radius:20px}\n"
                + ".marker-cluster div{width:30px;height:30px;margin-left:5px;margin-top:5px;text-align:center;border-radius:15px;font:bold 12px 'Helvetica Neue',Arial,Helvetica,sans-serif}\n"
                + ".marker-cluster span{line-height:30px}\n"

                // ── Surcharge couleurs cluster MindAura ──
                + ".marker-cluster-small div,.marker-cluster-medium div,.marker-cluster-large div{"
                + "background-color:rgba(124,58,237,0.85)!important;color:white!important;font-weight:700!important}\n"
                + ".marker-cluster-small,.marker-cluster-medium,.marker-cluster-large{"
                + "background-color:rgba(124,58,237,0.25)!important}\n"

                // ── Popup custom ──
                + ".leaflet-popup-content-wrapper{border-radius:14px!important;padding:0!important;overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,0.25)!important}\n"
                + ".leaflet-popup-content{margin:0!important;width:290px!important}\n"
                + ".leaflet-popup-tip{background:#fff!important}\n"
                + ".p-bar{height:6px}\n"
                + ".p-head{padding:14px 16px 10px;border-bottom:1px solid #F0F0F0}\n"
                + ".p-nom{font-size:15px;font-weight:800;color:#111;margin-bottom:4px}\n"
                + ".p-badge{display:inline-block;padding:2px 10px;border-radius:20px;font-size:10px;font-weight:700}\n"
                + ".p-body{padding:12px 16px 8px}\n"
                + ".p-row{font-size:12px;color:#555;margin:5px 0;display:flex;align-items:flex-start;gap:6px}\n"
                + ".p-icon{min-width:16px;font-size:13px}\n"
                + ".d-badge{display:inline-block;padding:4px 12px;border-radius:20px;font-size:11px;font-weight:700;margin:6px 0 2px}\n"
                + ".d-ok{background:#D1FAE5;color:#065F46}\n"
                + ".d-non{background:#FEE2E2;color:#991B1B}\n"
                + ".d-res{background:#FEF3C7;color:#92400E}\n"
                + ".p-foot{padding:10px 16px 14px}\n"
                + ".btn-rsv{display:block;width:100%;padding:10px 0;background:linear-gradient(135deg,#1B4332,#2D6A4F);color:white;border:none;border-radius:8px;font-size:13px;font-weight:700;cursor:pointer}\n"
                + ".btn-rsv:disabled{background:#CBD5E1;color:#94A3B8;cursor:not-allowed}\n"
                + "  </style>\n"
                + "</head>\n"
                + "<body>\n"
                + "<div id='map'></div>\n"

                // Scripts via jsDelivr (JS se charge bien, seul le CSS posait problème)
                + "<script src='https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'></script>\n"
                + "<script src='https://cdn.jsdelivr.net/npm/leaflet.markercluster@1.5.3/dist/leaflet.markercluster.js'></script>\n"

                + "<script>\n"
                + "var map, cluster, marqueurs = {};\n\n"

                // Toute l'init dans window.onload pour garantir que Leaflet.js est chargé
                + "window.onload = function() {\n"
                + "  setTimeout(function() {\n\n"

                // Carte Leaflet
                + "  map = L.map('map', {\n"
                + "    center: [34.0, 9.0],\n"
                + "    zoom: 6,\n"
                + "    preferCanvas: true,\n"
                + "    zoomControl: true,\n"
                + "    attributionControl: true\n"
                + "  });\n\n"

                // Tuile OSM standard
                + "  var tileOSM = L.tileLayer(\n"
                + "    'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',\n"
                + "    {\n"
                + "      attribution: '&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a>',\n"
                + "      maxZoom: 18,\n"
                + "      crossOrigin: false\n"
                + "    }\n"
                + "  );\n\n"

                // Tuile CartoDB claire
                + "  var tileLight = L.tileLayer(\n"
                + "    'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',\n"
                + "    {\n"
                + "      attribution: '&copy; OpenStreetMap &copy; CARTO',\n"
                + "      subdomains: 'abcd',\n"
                + "      maxZoom: 18,\n"
                + "      crossOrigin: false\n"
                + "    }\n"
                + "  );\n\n"

                // Satellite Esri
                + "  var tileSat = L.tileLayer(\n"
                + "    'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',\n"
                + "    {\n"
                + "      attribution: '&copy; Esri',\n"
                + "      maxZoom: 18,\n"
                + "      crossOrigin: false\n"
                + "    }\n"
                + "  );\n\n"

                + "  tileOSM.addTo(map);\n\n"

                + "  L.control.layers(\n"
                + "    { 'OpenStreetMap': tileOSM, 'Carte claire': tileLight, 'Satellite': tileSat },\n"
                + "    {},\n"
                + "    { position: 'topright' }\n"
                + "  ).addTo(map);\n\n"

                + "  map.setMinZoom(5);\n"
                + "  map.setMaxZoom(18);\n\n"

                + "  cluster = L.markerClusterGroup({\n"
                + "    showCoverageOnHover: false,\n"
                + "    maxClusterRadius: 60,\n"
                + "    spiderfyOnMaxZoom: true\n"
                + "  });\n"
                + "  map.addLayer(cluster);\n\n"

                // Forcer recalcul taille après init (critique pour JavaFX WebView)
                + "  setTimeout(function() { map.invalidateSize(true); }, 200);\n"
                + "  setTimeout(function() { map.invalidateSize(true); }, 600);\n\n"

                + "  }, 0);\n"  // fin setTimeout
                + "};\n\n"      // fin window.onload

                // Icône pin — SVG inline (aucun emoji, rendu parfait dans WebKit)
                + "  function creerIcone(couleur) {\n"
                + "    var svg = '<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"28\" height=\"38\" viewBox=\"0 0 28 38\">'\n"
                + "      + '<path d=\"M14 0C6.27 0 0 6.27 0 14c0 9.75 14 24 14 24s14-14.25 14-24C28 6.27 21.73 0 14 0z\"'\n"
                + "      + ' fill=\"' + couleur + '\" stroke=\"white\" stroke-width=\"2\"/>'\n"
                + "      + '<circle cx=\"14\" cy=\"14\" r=\"6\" fill=\"rgba(255,255,255,0.85)\"/>'\n"
                + "      + '</svg>';\n"
                + "    return L.divIcon({\n"
                + "      html: svg,\n"
                + "      iconSize: [28, 38],\n"
                + "      iconAnchor: [14, 38],\n"
                + "      popupAnchor: [0, -40],\n"
                + "      className: ''\n"
                + "    });\n"
                + "  }\n\n"

                // Texte de dispo — SANS emojis (correction du bug ◆◆)
                + "  function dispoClass(d) {\n"
                + "    if (d === 'Disponible') return 'd-ok';\n"
                + "    if (d === 'Non disponible') return 'd-non';\n"
                + "    return 'd-res';\n"
                + "  }\n"
                + "  function dispoText(d) {\n"
                + "    if (d === 'Disponible') return '[OK] Disponible';\n"
                + "    if (d === 'Non disponible') return '[X] Non disponible';\n"
                + "    return '[~] Sur reservation';\n"
                + "  }\n\n"

                // Ajouter un marqueur
                + "  function ajouterMarqueur(id,lat,lng,nom,adresse,ville,type,dispo,tel,couleur) {\n"
                + "    var icone = creerIcone(couleur);\n"
                + "    var marker = L.marker([lat,lng], {icon:icone});\n\n"
                + "    var btnDisabled = (dispo === 'Non disponible') ? 'disabled' : '';\n"
                + "    var btnTxt = (dispo === 'Disponible') ? 'Reserver maintenant' : 'Non disponible';\n\n"
                + "    var popup =\n"
                + "      '<div class=\"p-bar\" style=\"background:' + couleur + '\"></div>'\n"
                + "      + '<div class=\"p-head\">'\n"
                + "      +   '<div class=\"p-nom\">' + nom + '</div>'\n"
                + "      +   '<span class=\"p-badge\" style=\"background:' + couleur + '22;color:' + couleur + ';\">' + type + '</span>'\n"
                + "      + '</div>'\n"
                + "      + '<div class=\"p-body\">'\n"
                + "      +   '<div class=\"p-row\"><span class=\"p-icon\">&#128205;</span>' + adresse + ', ' + ville + '</div>'\n"
                + "      +   '<div class=\"p-row\"><span class=\"p-icon\">&#128222;</span>' + tel + '</div>'\n"
                + "      +   '<div class=\"d-badge ' + dispoClass(dispo) + '\">' + dispoText(dispo) + '</div>'\n"
                + "      + '</div>'\n"
                + "      + '<div class=\"p-foot\">'\n"
                + "      +   '<button class=\"btn-rsv\" ' + btnDisabled + ' onclick=\"reserverLocal(' + id + ')\">' + btnTxt + '</button>'\n"
                + "      + '</div>';\n\n"
                + "    marker.bindPopup(popup, {maxWidth:310, minWidth:290, closeButton:true});\n"
                + "    cluster.addLayer(marker);\n"
                + "    marqueurs[id] = marker;\n"
                + "    marker.on('mouseover', function() { this.openPopup(); });\n"
                + "  }\n\n"

                // Fonctions appelées depuis Java
                + "  function reserverLocal(id) {\n"
                + "    if (window.javaApp) window.javaApp.onReserverClique(id);\n"
                + "  }\n"
                + "  function supprimerTousMarqueurs() {\n"
                + "    if (!cluster) return;\n"
                + "    cluster.clearLayers();\n"
                + "    marqueurs = {};\n"
                + "  }\n"
                + "  function zoomerSurVille(lat,lng) {\n"
                + "    if (!map) return;\n"
                + "    map.setView([lat,lng], 13, {animate:true, duration:1.0});\n"
                + "  }\n"
                + "  function zoomerSurMarqueur(id) {\n"
                + "    if (!cluster || !marqueurs[id]) return;\n"
                + "    cluster.zoomToShowLayer(marqueurs[id], function() {\n"
                + "      marqueurs[id].openPopup();\n"
                + "    });\n"
                + "  }\n"
                + "  function geolocaliser() {\n"
                + "    if (!map) return;\n"
                + "    if (navigator.geolocation) {\n"
                + "      navigator.geolocation.getCurrentPosition(function(pos) {\n"
                + "        var lat = pos.coords.latitude, lng = pos.coords.longitude;\n"
                + "        map.setView([lat,lng], 15, {animate:true});\n"
                + "        L.circleMarker([lat,lng], {radius:10, color:'#6D28D9', fillColor:'#7C3AED', fillOpacity:0.9, weight:3})\n"
                + "          .addTo(map).bindPopup('<b>Vous etes ici</b>').openPopup();\n"
                + "      }, function() { alert('Geolocalisation refusee ou indisponible.'); });\n"
                + "    }\n"
                + "  }\n"

                + "</script>\n"
                + "</body>\n"
                + "</html>";
    }

    // ─────────────────────────────────────────────────────────────
    //  MARQUEURS
    // ─────────────────────────────────────────────────────────────

    private void chargerMarqueursJS(List<local_psychiatrie> locaux) {
        // Vérifier que la map est initialisée côté JS avant d'ajouter des marqueurs
        try {
            Object mapReady = webEngine.executeScript("typeof map !== 'undefined' && map !== null");
            if (!"true".equals(String.valueOf(mapReady))) {
                // Map pas encore prête, réessayer dans 500ms
                PauseTransition retry = new PauseTransition(Duration.millis(500));
                retry.setOnFinished(e -> chargerMarqueursJS(locaux));
                retry.play();
                return;
            }
        } catch (Exception e) {
            return;
        }

        webEngine.executeScript("supprimerTousMarqueurs()");

        // Offset aléatoire fixe par local (même valeur à chaque rechargement)
        Random rng = new Random(42);

        for (local_psychiatrie local : locaux) {
            double[] coords = obtenirCoordonnees(local);
            if (coords == null) continue;

            String couleur = COULEURS_TYPE.getOrDefault(local.getType_local().name(), "#7C3AED");

            // Léger décalage pour éviter la superposition dans une même ville
            double lat = coords[0] + (rng.nextDouble() - 0.5) * 0.009;
            double lng = coords[1] + (rng.nextDouble() - 0.5) * 0.009;

            String script = String.format(Locale.US,
                    "ajouterMarqueur(%d,%.6f,%.6f,'%s','%s','%s','%s','%s','%s','%s')",
                    local.getId_local(), lat, lng,
                    escapeJs(local.getNom_local()),
                    escapeJs(local.getAdresse_local()),
                    escapeJs(local.getVille_local()),
                    escapeJs(local.getType_local().getLibelle()),
                    escapeJs(local.getDisponibilite_local()),
                    escapeJs(String.valueOf(local.getTelephone_local())),
                    couleur
            );
            webEngine.executeScript(script);
        }

        mettreAJourCompteurs();
    }

    // ─────────────────────────────────────────────────────────────
    //  FILTRES & ACTIONS
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void appliquerFiltres() {
        String ville = comboVille.getValue();
        String type  = comboType.getValue();
        if (ville == null) ville = "Toutes les villes";
        if (type  == null) type  = "Tous les types";

        final String fVille = ville, fType = type;
        locauxFiltres = tousLesLocaux.stream()
                .filter(l -> fVille.equals("Toutes les villes") || l.getVille_local().equals(fVille))
                .filter(l -> fType.equals("Tous les types")    || l.getType_local().getLibelle().equals(fType))
                .collect(Collectors.toList());

        chargerMarqueursJS(locauxFiltres);

        // Zoomer sur la ville choisie
        if (!ville.equals("Toutes les villes") && COORDS_VILLES.containsKey(ville)) {
            double[] c = COORDS_VILLES.get(ville);
            webEngine.executeScript(String.format(Locale.US, "zoomerSurVille(%.6f,%.6f)", c[0], c[1]));
        }
    }

    @FXML
    private void handleRechercher() {
        String terme = txtRecherche.getText().trim().toLowerCase();
        if (terme.isEmpty()) { appliquerFiltres(); return; }

        Optional<local_psychiatrie> trouve = locauxFiltres.stream()
                .filter(l -> l.getNom_local().toLowerCase().contains(terme)
                        || l.getAdresse_local().toLowerCase().contains(terme)
                        || l.getVille_local().toLowerCase().contains(terme))
                .findFirst();

        if (trouve.isPresent()) {
            webEngine.executeScript("zoomerSurMarqueur(" + trouve.get().getId_local() + ")");
        } else {
            showInfo("Aucun local trouvé pour : \"" + txtRecherche.getText() + "\"");
        }
    }

    @FXML
    private void handleReinitialiser() {
        comboVille.setValue("Toutes les villes");
        comboType.setValue("Tous les types");
        txtRecherche.clear();
        locauxFiltres = new ArrayList<>(tousLesLocaux);
        chargerMarqueursJS(locauxFiltres);
        webEngine.executeScript("map.setView([34.0,9.0],6,{animate:true,duration:1.2})");
    }

    @FXML
    private void handleGeolocaliser() {
        webEngine.executeScript("geolocaliser()");
    }

    // ─────────────────────────────────────────────────────────────
    //  COMPTEURS DU HEADER
    // ─────────────────────────────────────────────────────────────

    private void mettreAJourCompteurs() {
        if (lblNbLocaux != null)
            lblNbLocaux.setText(String.valueOf(locauxFiltres.size()));

        if (lblNbDisponibles != null) {
            long dispo = locauxFiltres.stream()
                    .filter(l -> "Disponible".equals(l.getDisponibilite_local()))
                    .count();
            lblNbDisponibles.setText(String.valueOf(dispo));
        }

        if (lblNbVilles != null) {
            long villes = locauxFiltres.stream()
                    .map(local_psychiatrie::getVille_local)
                    .distinct().count();
            lblNbVilles.setText(String.valueOf(villes));
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  PONT JAVA ↔ JAVASCRIPT
    // ─────────────────────────────────────────────────────────────

    public class JavaBridge {
        public void onReserverClique(int idLocal) {
            javafx.application.Platform.runLater(() -> {
                local_psychiatrie local = tousLesLocaux.stream()
                        .filter(l -> l.getId_local() == idLocal)
                        .findFirst().orElse(null);
                if (local == null) return;

                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterReservation.fxml"));
                    Parent root = loader.load();
                    Stage stage = new Stage();
                    stage.setTitle("Réserver — " + local.getNom_local());
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.setMinWidth(800);
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                    stage.setScene(scene);
                    AjouterReservationController ctrl = loader.getController();
                    ctrl.setLocalPreselectionne(local);
                    stage.showAndWait();
                    chargerDonnees();
                    chargerMarqueursJS(locauxFiltres);
                } catch (IOException e) {
                    showError("Erreur : " + e.getMessage());
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  COORDONNÉES
    // ─────────────────────────────────────────────────────────────

    private double[] obtenirCoordonnees(local_psychiatrie local) {
        String ville = local.getVille_local();
        if (ville != null && COORDS_VILLES.containsKey(ville))
            return COORDS_VILLES.get(ville);

        System.out.println("⚠ Ville inconnue : " + ville + " → centre Tunisie");
        return new double[]{34.0, 9.0};
    }

    // ─────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────

    @FXML
    private void handleRetour() {
        if (parentHomeController != null) {
            parentHomeController.loadFrontOfficeAccueil();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOfficeAccueil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setWidth(1400); stage.setHeight(850);
            stage.centerOnScreen();
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Navigation impossible : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────

    private String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'",  "\\'")
                .replace("\"", "\\\"")
                .replace("\n", " ")
                .replace("\r", "");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Information"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}