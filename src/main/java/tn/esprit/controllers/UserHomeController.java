package tn.esprit.controllers;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.ResourceBundle;

import tn.esprit.entities.Ressources;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.SessionManager;

/**
 * UserHomeController v4 — MindAura
 *
 * Nouveautés v4 :
 *  - loadView(Parent)   : charge un nœud déjà instancié dans contentHolder
 *                         (permet à MesObjectifsController de garder la navbar
 *                          visible lors de la modification d'un objectif)
 *  - loadMesObjectifs() : charge MesObjectifs.fxml dans le centre
 */
public class UserHomeController implements Initializable {

    // ─── Navbar ───────────────────────────────────────────────────────────────
    @FXML private HBox      navbar;
    @FXML private Label     brandLabel;
    @FXML private Label     navAccueil;
    @FXML private Label     navServices;
    @FXML private Label     navEquipe;
    @FXML private Label     navContact;
    @FXML private StackPane sessionBtn;
    @FXML private HBox      sessionHBox;
    @FXML private StackPane avatarPane;
    @FXML private ImageView avatarImage;
    @FXML private Label     avatarInitials;
    @FXML private Label     sessionName;
    @FXML private Label     sessionRole;

    // ─── Sections (pour scroll) ───────────────────────────────────────────────
    @FXML private ScrollPane mainScroll;
    @FXML private StackPane  contentHolder;
    @FXML private StackPane  sectionAccueil;   // Hero section avec image de fond
    @FXML private ImageView  heroBgImage;      // Image de fond du hero
    @FXML private VBox       sectionServices;
    @FXML private VBox       sectionEquipe;
    @FXML private VBox       sectionContact;

    private Node initialContent;

    // ─── Welcome ──────────────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;

    // ─── Quote ────────────────────────────────────────────────────────────────
    @FXML private Label quoteText;
    @FXML private Label quoteAuthor;

    // ─── Avatars membres équipe ───────────────────────────────────────────────
    @FXML private StackPane avatarM1; @FXML private ImageView imgM1;
    @FXML private StackPane avatarM2; @FXML private ImageView imgM2;
    @FXML private StackPane avatarM3; @FXML private ImageView imgM3;
    @FXML private StackPane avatarM4; @FXML private ImageView imgM4;
    @FXML private StackPane avatarM5; @FXML private ImageView imgM5;
    @FXML private StackPane avatarM6; @FXML private ImageView imgM6;

    // ─── Dropdown Popup ───────────────────────────────────────────────────────
    private Popup   dropdownPopup;
    private boolean dropdownOpen = false;

    // ─── Quotes ───────────────────────────────────────────────────────────────
    private final String[][] QUOTES_FALLBACK = {
            {"La sante mentale est une priorite, pas un luxe.",     "MindAura"},
            {"Chaque jour est une nouvelle opportunite de grandir.", "Anonyme"},
            {"La guerison n'est pas lineaire, elle est courageuse.", "MindAura"},
            {"Prendre soin de soi est un acte de courage.",         "Brene Brown"},
            {"Votre bien-etre mental est votre plus grand tresor.",  "MindAura"},
            {"La vulnerabilite est la naissance de l'innovation.",   "Brene Brown"},
            {"Ce qui ne nous tue pas nous rend plus forts.",         "Nietzsche"},
            {"La pleine conscience transforme le chaos en clarte.",  "MindAura"}
    };
    private int      currentQuoteIndex = 0;
    private Timeline quoteTimer;
    private String[][] loadedQuotes    = null;

    private static final String[] TEAM_PHOTOS = {
            "Azer.png",
            "ichrak.jpg",
            "menyar.png",
            "jasser.png",
            "yassine.png",
            "rania.png"
    };

    // =========================================================================
    //  INIT
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupUserInfo();
        applyCircleClipToAvatar();
        loadTeamPhotos();
        buildDropdownPopup();
        animateBrand();
        startQuoteRotation();
        loadQuotesFromApi();
        Platform.runLater(() -> mainScroll.setVvalue(0.0));

        // Binding largeur image hero : s'adapte quand la fenêtre est redimensionnée
        if (heroBgImage != null && sectionAccueil != null) {
            heroBgImage.fitWidthProperty().bind(sectionAccueil.widthProperty());
        }

        // Injecter styleevents.css dans la Scene dès qu'elle est disponible
        navbar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                java.net.URL cssRes = getClass().getResource("/styleevents.css");
                if (cssRes != null) {
                    String cssUrl = cssRes.toExternalForm();
                    if (!newScene.getStylesheets().contains(cssUrl))
                        newScene.getStylesheets().add(cssUrl);
                }
            }
        });
    }

    // =========================================================================
    //  SESSION — INFO & AVATAR CIRCULAIRE
    // =========================================================================

    private void setupUserInfo() {
        String fullName = SessionManager.getNomComplet();
        String role     = SessionManager.getRoleLibelle();

        if (sessionName  != null) sessionName.setText(fullName);
        if (sessionRole  != null) sessionRole.setText(role);
        if (welcomeLabel != null) welcomeLabel.setText("Bonjour, " + fullName + " !");
        if (roleLabel    != null) roleLabel.setText(role);

        String photoPath = SessionManager.getPhotoPath();
        boolean photoLoaded = false;
        if (photoPath != null && !photoPath.isBlank()) {
            photoLoaded = tryLoadAvatarFile("C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/avatars/" + photoPath);
            if (!photoLoaded) photoLoaded = tryLoadAvatarFile(photoPath);
            if (!photoLoaded) photoLoaded = tryLoadAvatarResource("/" + photoPath);
        }
        if (!photoLoaded) {
            if (avatarInitials != null) {
                avatarInitials.setText(SessionManager.getInitiales());
                avatarInitials.setVisible(true);
            }
            if (avatarImage != null) avatarImage.setVisible(false);
        }
    }

    private void applyCircleClipToAvatar() {
        if (avatarImage == null) return;
        double r = 19;
        Circle clip = new Circle(r, r, r);
        avatarImage.setClip(clip);
    }

    private boolean tryLoadAvatarFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) return false;
            Image img = new Image(f.toURI().toString(), 38, 38, false, true);
            if (img.isError()) return false;
            if (avatarImage != null) {
                avatarImage.setImage(img);
                avatarImage.setVisible(true);
                if (avatarInitials != null) avatarInitials.setVisible(false);
            }
            return true;
        } catch (Exception e) { return false; }
    }

    private boolean tryLoadAvatarResource(String resourcePath) {
        try {
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) return false;
            Image img = new Image(is, 38, 38, false, true);
            if (img.isError()) return false;
            if (avatarImage != null) {
                avatarImage.setImage(img);
                avatarImage.setVisible(true);
                if (avatarInitials != null) avatarInitials.setVisible(false);
            }
            return true;
        } catch (Exception e) { return false; }
    }

    // =========================================================================
    //  PHOTOS ÉQUIPE
    // =========================================================================

    private void loadTeamPhotos() {
        ImageView[] views = { imgM1, imgM2, imgM3, imgM4, imgM5, imgM6 };
        StackPane[] panes = { avatarM1, avatarM2, avatarM3, avatarM4, avatarM5, avatarM6 };
        Label[] initialLabels = new Label[6];

        for (int i = 0; i < panes.length; i++) {
            if (panes[i] != null) {
                for (javafx.scene.Node node : panes[i].getChildren()) {
                    if (node instanceof Label) {
                        initialLabels[i] = (Label) node;
                        break;
                    }
                }
            }
        }

        double bannerW = 340;
        double bannerH = 200;

        for (int i = 0; i < TEAM_PHOTOS.length; i++) {
            ImageView iv = views[i];
            StackPane sp = panes[i];
            String    fn = TEAM_PHOTOS[i];
            Label initialLabel = initialLabels[i];

            if (iv == null || sp == null) continue;

            iv.setFitWidth(bannerW);
            iv.setFitHeight(bannerH);
            iv.setPreserveRatio(false);

            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(bannerW, bannerH);
            clip.setArcWidth(16);
            clip.setArcHeight(16);
            iv.setClip(clip);

            Image img = loadTeamPhoto(fn, bannerW, bannerH);
            if (img != null && !img.isError()) {
                iv.setImage(img);
                iv.setVisible(true);
                if (initialLabel != null) initialLabel.setVisible(false);
            } else {
                iv.setVisible(false);
                if (initialLabel != null) {
                    initialLabel.setVisible(true);
                    initialLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 42px; " +
                            "-fx-font-weight: 900; -fx-font-family: 'Georgia'; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 6, 0, 0, 2);");
                }
            }
        }
    }

    private Image loadTeamPhoto(String filename, double width, double height) {
        try {
            InputStream is = getClass().getResourceAsStream("/" + filename);
            if (is != null) { Image img = new Image(is, width, height, false, true); if (!img.isError()) return img; }
        } catch (Exception ignored) {}
        try {
            InputStream is = getClass().getResourceAsStream("/equipe/" + filename);
            if (is != null) { Image img = new Image(is, width, height, false, true); if (!img.isError()) return img; }
        } catch (Exception ignored) {}
        if (!filename.toLowerCase().endsWith(".png")) {
            String pngName = filename.substring(0, filename.lastIndexOf('.')) + ".png";
            try {
                InputStream is = getClass().getResourceAsStream("/equipe/" + pngName);
                if (is != null) { Image img = new Image(is, width, height, false, true); if (!img.isError()) return img; }
            } catch (Exception ignored) {}
        }
        if (!filename.toLowerCase().endsWith(".jpg") && !filename.toLowerCase().endsWith(".jpeg")) {
            String jpgName = filename.substring(0, filename.lastIndexOf('.')) + ".jpg";
            try {
                InputStream is = getClass().getResourceAsStream("/equipe/" + jpgName);
                if (is != null) { Image img = new Image(is, width, height, false, true); if (!img.isError()) return img; }
            } catch (Exception ignored) {}
        }
        try {
            File f = new File("src/main/resources/equipe/" + filename);
            if (f.exists()) { Image img = new Image(f.toURI().toString(), width, height, false, true); if (!img.isError()) return img; }
        } catch (Exception ignored) {}
        return null;
    }

    // =========================================================================
    //  ANIMATION BRAND
    // =========================================================================

    private void animateBrand() {
        if (brandLabel == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(900), brandLabel);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(900), brandLabel);
        tt.setFromX(-18); tt.setToX(0);
        new ParallelTransition(ft, tt).play();
    }

    // =========================================================================
    //  NAVBAR — SCROLL VERS SECTIONS
    // =========================================================================

    @FXML private void scrollToAccueil()  { restoreHomeAndScrollTo(sectionAccueil,  navAccueil);  }
    @FXML private void scrollToServices() { restoreHomeAndScrollTo(sectionServices, navServices); }
    @FXML private void scrollToEquipe()   { restoreHomeAndScrollTo(sectionEquipe,   navEquipe);   }
    @FXML private void scrollToContact()  { restoreHomeAndScrollTo(sectionContact,  navContact);  }

    private void restoreHomeAndScrollTo(Region section, Label activeNav) {
        boolean isSubViewActive = !contentHolder.getChildren().contains(mainScroll);
        if (isSubViewActive) {
            restoreHome();
            Platform.runLater(() -> Platform.runLater(() -> scrollTo(section, activeNav)));
        } else {
            scrollTo(section, activeNav);
        }
    }

    private void scrollTo(Region section, Label activeNav) {
        if (mainScroll == null || section == null) return;

        for (Label lbl : new Label[]{navAccueil, navServices, navEquipe, navContact}) {
            if (lbl == null) continue;
            if (lbl == activeNav) {
                lbl.setStyle("-fx-text-fill:#BBF7D0; -fx-font-size:13px; -fx-font-weight:700;" +
                        "-fx-cursor:hand; -fx-border-color:#BBF7D0;" +
                        "-fx-border-width:0 0 2 0; -fx-padding:4 2 4 2;");
            } else {
                lbl.setStyle("-fx-text-fill:rgba(187,247,208,0.8); -fx-font-size:13px;" +
                        "-fx-font-weight:600; -fx-cursor:hand; -fx-padding:4 2 4 2;");
            }
        }

        Platform.runLater(() -> {
            double contentHeight   = mainScroll.getContent().getBoundsInLocal().getHeight();
            double scrollableHeight = contentHeight - mainScroll.getViewportBounds().getHeight();
            if (scrollableHeight <= 0) return;
            double sectionY = section.getBoundsInParent().getMinY();
            double ratio    = Math.min(1.0, sectionY / scrollableHeight);
            Timeline tl = new Timeline();
            double start = mainScroll.getVvalue();
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(500),
                    new KeyValue(mainScroll.vvalueProperty(), ratio, Interpolator.EASE_BOTH)));
            tl.play();
        });
    }

    // =========================================================================
    //  DROPDOWN POPUP
    // =========================================================================

    private void buildDropdownPopup() {
        dropdownPopup = new Popup();
        dropdownPopup.setAutoHide(true);
        dropdownPopup.setAutoFix(true);
        dropdownPopup.setOnHidden(e -> dropdownOpen = false);

        VBox menu = new VBox(0);
        menu.setMinWidth(250);
        menu.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:14;" +
                        "-fx-border-color:#E2E8F0; -fx-border-radius:14; -fx-border-width:1.5;" +
                        "-fx-effect:dropshadow(gaussian,rgba(27,67,50,0.18),24,0,0,10);"
        );

        VBox header = new VBox(4);
        header.setPadding(new Insets(14, 18, 14, 18));
        header.setStyle("-fx-border-color:#EDF2F7; -fx-border-width:0 0 1 0;");
        Label lName = new Label(SessionManager.getNomComplet());
        lName.setStyle("-fx-text-fill:#1A1A2E; -fx-font-weight:800; -fx-font-size:13px;");
        Label lRole = new Label(SessionManager.getRoleLibelle());
        lRole.setStyle("-fx-text-fill:#7B5EA7; -fx-font-size:10.5px; -fx-font-weight:700;" +
                "-fx-background-color:#EDE9FE; -fx-background-radius:8; -fx-padding:2 10 2 10;");
        header.getChildren().addAll(lName, lRole);

        HBox itemProfil = buildMenuItem("\u2699", "#1B4332", "#F0FFF4",
                "Mon profil", "Modifier mes informations", false);
        itemProfil.setOnMouseClicked(e -> { closeDropdown(); handleProfil(); });

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color:#EDF2F7;");
        VBox.setMargin(sep, new Insets(3, 0, 3, 0));

        HBox itemLogout = buildMenuItem("\u23FB", "#DC2626", "#FEF2F2",
                "Se deconnecter", "Fermer la session", true);
        itemLogout.setOnMouseClicked(e -> { closeDropdown(); handleLogout(); });

        menu.getChildren().addAll(header, itemProfil, sep, itemLogout);
        dropdownPopup.getContent().add(menu);
    }

    private HBox buildMenuItem(String icon, String iconColor, String hoverBg,
                               String title, String subtitle, boolean danger) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 18, 10, 18));
        row.setCursor(javafx.scene.Cursor.HAND);
        row.setStyle("-fx-background-color:transparent;");

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(34, 34); iconBox.setPrefSize(34, 34);
        iconBox.setStyle("-fx-background-color:" + (danger ? "#FEE2E2" : "#F0FFF4") + "; -fx-background-radius:50%;");
        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size:14px; -fx-text-fill:" + iconColor + ";");
        iconBox.getChildren().add(ic);

        VBox texts = new VBox(2);
        Label lt = new Label(title);
        lt.setStyle("-fx-text-fill:" + (danger ? "#DC2626" : "#1A1A2E") + "; -fx-font-size:12.5px; -fx-font-weight:700;");
        Label ls = new Label(subtitle);
        ls.setStyle("-fx-text-fill:" + (danger ? "#FCA5A5" : "#5A6475") + "; -fx-font-size:10.5px;");
        texts.getChildren().addAll(lt, ls);
        row.getChildren().addAll(iconBox, texts);

        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:" + hoverBg + "; -fx-cursor:hand;"));
        row.setOnMouseExited( e -> row.setStyle("-fx-background-color:transparent; -fx-cursor:hand;"));
        return row;
    }

    @FXML
    private void toggleDropdown() {
        if (dropdownOpen) closeDropdown();
        else openDropdown();
    }

    private void openDropdown() {
        if (dropdownPopup == null || sessionBtn == null) return;
        javafx.geometry.Bounds b = sessionBtn.localToScreen(sessionBtn.getBoundsInLocal());
        if (b == null) return;
        dropdownPopup.show(sessionBtn.getScene().getWindow(), b.getMaxX() - 250, b.getMaxY() + 8);
        dropdownOpen = true;

        VBox menu = (VBox) dropdownPopup.getContent().get(0);
        menu.setOpacity(0); menu.setTranslateY(-6);
        FadeTransition ft = new FadeTransition(Duration.millis(180), menu);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(180), menu);
        tt.setFromY(-6); tt.setToY(0);
        new ParallelTransition(ft, tt).play();
    }

    private void closeDropdown() {
        if (dropdownPopup != null && dropdownOpen) {
            dropdownPopup.hide();
            dropdownOpen = false;
        }
    }

    // =========================================================================
    //  NAVIGATION PROFIL
    // =========================================================================

    private void handleProfil() {
        try {
            Stage stage = (Stage) navbar.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierUtilisateur.fxml"));
            Parent profilView = loader.load();

            Object ctrl = loader.getController();
            if (ctrl instanceof ModifierUtilisateurController muc) {
                muc.setUserToEdit(SessionManager.getCurrentUser());
                muc.setOnDone(() -> backToHome(stage));
            }

            Parent currentRoot = stage.getScene().getRoot();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), currentRoot);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                stage.getScene().setRoot(profilView);
                profilView.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), profilView);
                fadeIn.setFromValue(0); fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void backToHome(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/UserHome.fxml"));
            Parent homeView = loader.load();
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), stage.getScene().getRoot());
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> {
                stage.getScene().setRoot(homeView);
                homeView.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(250), homeView);
                fadeIn.setFromValue(0); fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            Stage stage = (Stage) navbar.getScene().getWindow();
            NavigationManager.navigateTo("Login.fxml", "MindAura \u2013 Connexion", stage, false);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================================
    //  ACTIONS SERVICES
    // =========================================================================

    @FXML private void goLocaux()     { loadInCenter("/FrontOfficeAccueil.fxml");    }
    @FXML private void goTests()      { loadInCenter("/ChoisirTest.fxml");            }
    @FXML private void goResources()  { loadInCenter("/FrontOffice_COMPLET.fxml");   }
    @FXML private void goEvenements() { loadInCenter("/Evenements.fxml");             }
    @FXML public  void goReclamations(){ loadInCenter("/Reclamations.fxml");          }

    // =========================================================================
    //  NAVIGATION CENTRALISÉE — loadInCenter
    // =========================================================================

    /**
     * Charge un FXML dans contentHolder (zone centrale) sans toucher à la navbar.
     * Injecte automatiquement la référence parent au contrôleur chargé.
     */
    public void loadInCenter(String resourcePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
            Parent root = loader.load();

            if (initialContent == null && mainScroll != null) initialContent = mainScroll;

            contentHolder.getChildren().setAll(root);

            Object ctrl = loader.getController();
            if      (ctrl instanceof FrontOfficeAccueilController         c) c.setParentHomeController(this);
            else if (ctrl instanceof FrontOfficeMesReservationsController  c) c.setParentHomeController(this);
            else if (ctrl instanceof RecommandationController              c) c.setParentHomeController(this);
            else if (ctrl instanceof CalendrierReservationController       c) c.setParentHomeController(this);
            else if (ctrl instanceof CarteGeographiqueController           c) c.setParentHomeController(this);
            else if (ctrl instanceof ChatbotController                     c) c.setParentHomeController(this);
            else if (ctrl instanceof ChoisirTestController                 c) c.setParentHomeController(this);
            else if (ctrl instanceof MesResultatsController                c) c.setParentHomeController(this);
            else if (ctrl instanceof MesObjectifsController                c) c.setParentHomeController(this);
            else if (ctrl instanceof PasserTestController                  c) c.setParentHomeController(this);
            else if (ctrl instanceof ObjectifsRecommandesController        c) c.setParentHomeController(this);
            else if (ctrl instanceof ReclamationsController                c) c.setParentHomeController(this);
            else if (ctrl instanceof FrontOfficeController                 c) c.setParentHomeController(this);
            else if (ctrl instanceof StatistiquesController                c) c.setParentHomeController(this);
            else if (ctrl instanceof UserEventController                   c) c.setParentHomeController(this);
            else if (ctrl instanceof RecommandationControllerParticipation c) c.setParentHomeController(this);

        } catch (Exception e) { e.printStackTrace(); }
    }

    // =========================================================================
    //  ✅ NOUVEAU — loadView(Parent)
    //  Charge un Parent déjà instancié dans contentHolder.
    //  Utilisé par MesObjectifsController.handleModifier() pour afficher
    //  AjouterObjectif.fxml sans perdre la navbar.
    // =========================================================================

    /**
     * Remplace le contenu central par un nœud {@link Parent} déjà chargé.
     * La navbar reste intacte.
     *
     * @param view le nœud Parent à afficher (ex: formulaire AjouterObjectif)
     */
    public void loadView(Parent view) {
        if (contentHolder == null || view == null) return;
        if (initialContent == null && mainScroll != null) initialContent = mainScroll;
        contentHolder.getChildren().setAll(view);
    }

    // =========================================================================
    //  NAVIGATION PUBLIQUE — appelée par les contrôleurs enfants
    // =========================================================================

    /** Retour à la page d'accueil (sections Accueil/Services/Équipe/Contact). */
    public void restoreHome() {
        if (initialContent != null) contentHolder.getChildren().setAll(initialContent);
    }

    /** Remplace le contenu central (utilisé par ReclamationsController, etc.). */
    public void setCenterContent(Parent root) {
        if (contentHolder != null && root != null) contentHolder.getChildren().setAll(root);
    }

    public void loadMesReservations()   { loadInCenter("/FrontOfficeMesReservations.fxml"); }
    public void loadRecommandations()   { loadInCenter("/Recommandation.fxml"); }
    public void loadCalendrier()        { loadInCenter("/CalendrierReservation.fxml"); }
    public void loadCarte()             { loadInCenter("/CarteGeographique.fxml"); }
    public void loadChatbot()           { loadInCenter("/Chatbot.fxml"); }
    public void loadFrontOfficeAccueil(){ loadInCenter("/FrontOfficeAccueil.fxml"); }
    public void loadChoisirTest()       { loadInCenter("/ChoisirTest.fxml"); }
    public void loadMesResultats()      { loadInCenter("/MesResultats.fxml"); }

    /** ✅ Charge MesObjectifs.fxml dans la zone centrale (navbar conservée). */
    public void loadMesObjectifs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesObjectifs.fxml"));
            Parent view = loader.load();
            MesObjectifsController ctrl = loader.getController();
            ctrl.setParentHomeController(this);
            if (initialContent == null && mainScroll != null) initialContent = mainScroll;
            contentHolder.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadPasserTest(tn.esprit.entities.TestPsycho test) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/PasserTest.fxml"));
            javafx.scene.Parent root = loader.load();
            if (initialContent == null && mainScroll != null) initialContent = mainScroll;
            contentHolder.getChildren().setAll(root);
            PasserTestController ctrl = loader.getController();
            ctrl.setParentHomeController(this);
            ctrl.initTest(test);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadObjectifsRecommandes(tn.esprit.entities.TestPsycho test, int scoreTotal) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ObjectifsRecommandes.fxml"));
            javafx.scene.Parent root = loader.load();
            if (initialContent == null && mainScroll != null) initialContent = mainScroll;
            contentHolder.getChildren().setAll(root);
            ObjectifsRecommandesController ctrl = loader.getController();
            ctrl.setParentHomeController(this);
            ctrl.initTest(test, scoreTotal);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void loadRecommandationRessource(List<Ressources> allRessources) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RecommandationRessource.fxml"));
            Parent root = loader.load();
            RecommandationRessourceController ctrl = loader.getController();
            ctrl.setParentHomeController(this);
            ctrl.setContexte(null, allRessources != null ? allRessources : java.util.Collections.emptyList());
            if (initialContent == null && mainScroll != null) initialContent = mainScroll;
            contentHolder.getChildren().setAll(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    /** Méthodes publiques pour les boutons de navbar des contrôleurs enfants. */
    public void restoreHomeAndScrollToAccueil()  { restoreHomeAndScrollTo(sectionAccueil,  navAccueil);  }
    public void restoreHomeAndScrollToServices() { restoreHomeAndScrollTo(sectionServices, navServices); }
    public void restoreHomeAndScrollToEquipe()   { restoreHomeAndScrollTo(sectionEquipe,   navEquipe);   }
    public void restoreHomeAndScrollToContact()  { restoreHomeAndScrollTo(sectionContact,  navContact);  }

    // =========================================================================
    //  QUOTES
    // =========================================================================

    private void startQuoteRotation() {
        displayQuote(QUOTES_FALLBACK[0][0], QUOTES_FALLBACK[0][1]);
        quoteTimer = new Timeline(new KeyFrame(Duration.seconds(8), e -> nextQuote()));
        quoteTimer.setCycleCount(Timeline.INDEFINITE);
        quoteTimer.play();
    }

    private void nextQuote() {
        String[][] src = loadedQuotes != null ? loadedQuotes : QUOTES_FALLBACK;
        currentQuoteIndex = (currentQuoteIndex + 1) % src.length;
        if (quoteText == null) return;
        FadeTransition out = new FadeTransition(Duration.millis(400), quoteText);
        out.setFromValue(1); out.setToValue(0);
        out.setOnFinished(ev -> {
            displayQuote(src[currentQuoteIndex][0], src[currentQuoteIndex][1]);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), quoteText);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);
            fadeIn.play();
        });
        out.play();
    }

    private void displayQuote(String text, String author) {
        if (quoteText   != null) quoteText.setText("\u275D  " + text + "  \u275E");
        if (quoteAuthor != null) quoteAuthor.setText("\u2014 " + (author != null ? author : "Anonyme"));
    }

    private void loadQuotesFromApi() {
        Thread t = new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(java.time.Duration.ofSeconds(6)).build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("https://zenquotes.io/api/quotes"))
                        .timeout(java.time.Duration.ofSeconds(8)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    String[][] parsed = parseZenQuotes(resp.body());
                    if (parsed != null && parsed.length > 0)
                        Platform.runLater(() -> loadedQuotes = parsed);
                }
            } catch (Exception ignored) {}
        }, "quotes-thread");
        t.setDaemon(true);
        t.start();
    }

    private String[][] parseZenQuotes(String json) {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        int idx = 0;
        while (list.size() < 15) {
            int qi = json.indexOf("\"q\":", idx);
            if (qi < 0) break;
            String q = extractStr(json, qi + 4);
            int ai = json.indexOf("\"a\":", qi);
            String a = ai >= 0 ? extractStr(json, ai + 4) : "Anonyme";
            if (q != null && !q.isBlank()) list.add(new String[]{q, a});
            idx = qi + 5;
        }
        return list.toArray(new String[0][]);
    }

    private String extractStr(String json, int start) {
        try {
            int s = json.indexOf('"', start) + 1;
            StringBuilder sb = new StringBuilder();
            for (int i = s; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '"' && json.charAt(i - 1) != '\\') break;
                sb.append(c);
            }
            return sb.toString().replace("\\\"", "\"").replace("\\/", "/");
        } catch (Exception e) { return null; }
    }

    // =========================================================================
    //  RÉSEAUX SOCIAUX
    // =========================================================================

    @FXML private void openFacebookAzer()    { openLink("https://www.facebook.com/laifiazer2/"); }
    @FXML private void openInstagramAzer()   { openLink("https://www.instagram.com/laifi_azer1/"); }
    @FXML private void openFacebookIchrak()  { openLink("https://www.facebook.com/ichrak.chaffai"); }
    @FXML private void openInstagramIchrak() { openLink("https://www.instagram.com/ichrakchaffai/"); }
    @FXML private void openFacebookMenyar()  { openLink("https://www.facebook.com/guesmi.menyar"); }
    @FXML private void openInstagramMenyar() { openLink("https://www.instagram.com/guesmi.menyar"); }
    @FXML private void openFacebookJasser()  { openLink("https://www.facebook.com/jasser.missaoui.2025"); }
    @FXML private void openInstagramJasser() { openLink("https://www.instagram.com/jassermiss/"); }
    @FXML private void openFacebookYassine() { openLink("https://www.facebook.com/yassine.benrabeh.3"); }
    @FXML private void openInstagramYassine(){ openLink("https://www.instagram.com/yassine_ben_rabeh/"); }
    @FXML private void openFacebookRania()   { openLink("https://www.facebook.com/zairi.rania"); }
    @FXML private void openInstagramRania()  { openLink("https://www.instagram.com/zairi.rania"); }
    @FXML private void openFacebook()  { openLink("https://www.facebook.com/profile.php?id=61587275842434"); }
    @FXML private void openInstagram() { openLink("https://www.instagram.com/mindaura3/"); }

    private void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir le lien");
            alert.setContentText("Vérifiez votre connexion internet.");
            alert.showAndWait();
        }
    }
}