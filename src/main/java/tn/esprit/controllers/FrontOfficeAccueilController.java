package tn.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.components.LanguageSelectorComponent;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.services.TranslationService;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.utils.ImageManager;
import tn.esprit.utils.LanguageManager;

public class FrontOfficeAccueilController extends TranslatableController {

    // ── FXML existants ───────────────────────────────────────────────────────────
    @FXML private GridPane             locauxGridPane;
    @FXML private TextField            searchField;
    @FXML private ComboBox<String>     filterTypeComboBox;
    @FXML private ComboBox<String>     filterVilleComboBox;
    @FXML private Button               btnRafraichir;
    @FXML private Button               btnMesReservations;
    @FXML private Label                lblStatistiques;
    @FXML private VBox                 emptyStateContainer;
    @FXML private Button               btnChatbotFlottant;
    @FXML private Button               btnQRCode;

    // ── VUE INLINE : swap liste ↔ réservation ──────────────────────────────────
    @FXML private StackPane            vueLocaux;
    @FXML private VBox                 vueReservation;
    @FXML private StackPane            reservationContainer;
    @FXML private Label                lblBreadcrumbLocal;
    @FXML private HBox                 barreRecherche;

    // ── ★ NOUVEAU : vue Salles ──────────────────────────────────────────────────
    @FXML private StackPane            vueSalles;
    @FXML private Button               btnSalles;

    // ── Sélecteur de langue ──────────────────────────────────────────────────────
    @FXML private ComboBox<String>     comboLangue;

    // Labels traduisibles
    @FXML private Label                lblTitreSection;
    @FXML private Label                lblAucunLocal;
    @FXML private Label                lblAideMessage;

    // ── Référence au contrôleur parent (UserHome) ────────────────────────────────
    private UserHomeController         parentHomeController;

    // ── Services ─────────────────────────────────────────────────────────────────
    private local_psychiatrie_SERVICE              localService;
    private ObservableList<local_psychiatrie>      localList;
    private ObservableList<local_psychiatrie>      filteredList;
    private final TranslationService               translationService = TranslationService.getInstance();

    // ── ★ NOUVEAU : controller des salles (chargé une seule fois) ───────────────
    private FrontOfficeSallesController            sallesController;

    // ─────────────────────────────────────────────────────────────────────────────

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    public UserHomeController getParentHomeController() {
        return parentHomeController;
    }

    @Override
    protected void initComponents(URL url, ResourceBundle resourceBundle) {
        localService = new local_psychiatrie_SERVICE();
        localList    = FXCollections.observableArrayList();
        filteredList = FXCollections.observableArrayList();

        // 1. Sélecteur de langue
        if (comboLangue != null) {
            LanguageSelectorComponent.setup(comboLangue);
        }

        // 2. Écouter les changements de langue
        LanguageManager.getInstance().addListener(this::onLanguageChanged);

        // 3. Chargement données locaux
        loadData();
        initializeFilters();

        searchField.textProperty().addListener((obs, o, n) -> filterData());
        filterTypeComboBox.valueProperty().addListener((obs, o, n) -> filterData());
        filterVilleComboBox.valueProperty().addListener((obs, o, n) -> filterData());

        displayLocauxCards();
        applyFadeInAnimation();

        if (btnChatbotFlottant != null) {
            applyFABPulseAnimation();
            applyFABHoverEffect();
        }

        // 4. S'assurer que vueSalles est caché au démarrage
        if (vueSalles != null) {
            vueSalles.setVisible(false);
            vueSalles.setManaged(false);
        }

        // 5. Traduire l'UI si la langue n'est pas le français
        String currentLang = LanguageManager.getInstance().getCurrentLanguage();
        if (!TranslationService.LANG_FR.equals(currentLang)) {
            onLanguageChanged(currentLang);
        }

        System.out.println("✅ Front Office Accueil chargé — " + localList.size() + " locaux");
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  ★ NOUVEAU — NAVIGATION VERS LES SALLES
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Affiche la vue des salles en grille de cartes.
     * Le FXML FrontOfficeSalles.fxml est chargé une seule fois puis réutilisé.
     */
    @FXML
    private void handleNavigateToSalles() {
        if (vueSalles == null) return;

        // Charger le FXML une seule fois (on teste sallesController, pas getChildren(),
        // car le StackPane peut contenir des commentaires XML comptés comme enfants)
        if (sallesController == null) {
            try {
                URL fxmlUrl = getClass().getResource("/FrontOfficeSalles.fxml");

                // ── CORRECTIF : vérification null avant load() ──────────────────
                if (fxmlUrl == null) {
                    showAlert(Alert.AlertType.ERROR, "Fichier FXML introuvable",
                            "FrontOfficeSalles.fxml est introuvable dans le classpath.",
                            "Vérifiez que le fichier est bien placé dans :\n" +
                                    "src/main/resources/FrontOfficeSalles.fxml\n" +
                                    "puis faites un clean + rebuild du projet.");
                    return;
                }

                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                Parent node = loader.load();
                sallesController = loader.getController();
                sallesController.setParentAccueilController(this);  // injection du parent
                vueSalles.getChildren().clear();   // retire les éventuels commentaires XML
                vueSalles.getChildren().add(node);
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de charger la vue Salles", e.getMessage());
                return;
            }
        } else {
            // Rafraîchit les données à chaque visite
            sallesController.loadData();
        }

        afficherVuePrincipale(vueSalles);
        surlignerBoutonSalles(true);
    }

    /**
     * Bascule entre les vues principales : locaux / salles / réservation.
     * Masque toutes les autres vues et affiche uniquement vueActive.
     */
    private void afficherVuePrincipale(javafx.scene.Node vueActive) {
        // Masquer la barre de recherche/filtres (appartient à vueLocaux)
        if (barreRecherche != null) {
            boolean montrerBarre = (vueActive == vueLocaux);
            barreRecherche.setVisible(montrerBarre);
            barreRecherche.setManaged(montrerBarre);
        }

        javafx.scene.Node[] toutesLesVues = { vueLocaux, vueSalles, vueReservation };
        for (javafx.scene.Node v : toutesLesVues) {
            if (v == null) continue;
            boolean actif = (v == vueActive);
            v.setVisible(actif);
            v.setManaged(actif);
        }

        // Animation fade-in sur la vue activée
        if (vueActive != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(350), vueActive);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }

    /**
     * Met en évidence le bouton Salles quand la vue salles est active.
     */
    private void surlignerBoutonSalles(boolean actif) {
        if (btnSalles == null) return;
        if (actif) {
            btnSalles.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #047857, #065F46);" +
                            "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 12px; -fx-padding: 11px 18px; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(5,150,105,0.8), 14, 0, 0, 4);" +
                            "-fx-border-color: rgba(255,255,255,0.5); -fx-border-width: 2px; -fx-border-radius: 12px;");
        } else {
            btnSalles.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #059669, #047857);" +
                            "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 12px;" +
                            "-fx-background-radius: 12px; -fx-padding: 11px 18px; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(5,150,105,0.5), 10, 0, 0, 3);");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  NAVIGATION DÉTAIL SALLE (inline, comme local)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Ouvre le détail d'une salle + formulaire de réservation (inline, comme les locaux).
     * Appelé depuis FrontOfficeSallesController quand l'utilisateur clique sur "Réserver".
     */
    public void ouvrirDetailsSalle(tn.esprit.entities.salle salle, String nomLocal) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FrontOfficeDetailsSalle.fxml"));
            Parent root = loader.load();

            FrontOfficeDetailsSalleController ctrl = loader.getController();
            ctrl.setSalle(salle, nomLocal);
            ctrl.setParentController(this);

            if (reservationContainer != null) {
                reservationContainer.getChildren().setAll(root);
            }
            if (lblBreadcrumbLocal != null) {
                lblBreadcrumbLocal.setText(salle.getNom_salle());
            }

            showReservationView();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger le détail de la salle", e.getMessage());
        }
    }

    /**
     * Retourne à la vue des salles depuis le détail/formulaire d'une salle.
     */
    public void retourVueSalles() {
        if (vueReservation != null) {
            vueReservation.setVisible(false);
            vueReservation.setManaged(false);
        }
        if (reservationContainer != null) {
            reservationContainer.getChildren().clear();
        }
        afficherVuePrincipale(vueSalles);
        surlignerBoutonSalles(true);
        if (sallesController != null) sallesController.loadData();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  TRADUCTION
    // ─────────────────────────────────────────────────────────────────────────────

    private void onLanguageChanged(String langCode) {
        System.out.println("🌐 FrontOffice: traduction vers " + TranslationService.getNomLangue(langCode));

        traduireLabel(btnMesReservations, "📋  Mes Réservations", langCode);

        if (searchField != null) {
            traduirePrompt(searchField, "Rechercher un local...", langCode);
        }
        if (lblTitreSection != null) {
            traduireLabel(lblTitreSection, "Locaux Disponibles", langCode);
        }
        if (lblAucunLocal != null) {
            traduireLabel(lblAucunLocal, "Aucun local disponible", langCode);
        }
        if (lblAideMessage != null) {
            traduireLabel(lblAideMessage, "💬  Besoin d'aide ?", langCode);
        }

        if (TranslationService.LANG_AR.equals(langCode)) {
            appliquerRTL();
        } else {
            supprimerRTL();
        }
    }

    private void traduireLabel(Button btn, String texteFr, String langCode) {
        if (btn == null) return;
        if (TranslationService.LANG_FR.equals(langCode)) {
            btn.setText(texteFr);
            return;
        }
        String emoji = extraireEmoji(texteFr);
        String texteSeul = texteFr.replace(emoji, "").trim();
        translationService.traduireAsync(texteSeul, traduit ->
                btn.setText(emoji.isEmpty() ? traduit : emoji + "  " + traduit));
    }

    private void traduireLabel(Label lbl, String texteFr, String langCode) {
        if (lbl == null) return;
        if (TranslationService.LANG_FR.equals(langCode)) {
            lbl.setText(texteFr);
            return;
        }
        String emoji = extraireEmoji(texteFr);
        String texteSeul = texteFr.replace(emoji, "").trim();
        translationService.traduireAsync(texteSeul, traduit ->
                lbl.setText(emoji.isEmpty() ? traduit : emoji + "  " + traduit));
    }

    private void traduirePrompt(TextField field, String texteFr, String langCode) {
        if (TranslationService.LANG_FR.equals(langCode)) {
            field.setPromptText(texteFr);
            return;
        }
        translationService.traduireAsync(texteFr, traduit -> field.setPromptText(traduit));
    }

    private String extraireEmoji(String texte) {
        if (texte == null || texte.isEmpty()) return "";
        StringBuilder emoji = new StringBuilder();
        int i = 0;
        while (i < texte.length()) {
            int cp = texte.codePointAt(i);
            if (cp > 0x2000) {
                emoji.appendCodePoint(cp);
                i += Character.charCount(cp);
            } else {
                break;
            }
        }
        return emoji.toString();
    }

    private void appliquerRTL() {
        if (locauxGridPane != null)
            locauxGridPane.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
    }

    private void supprimerRTL() {
        if (locauxGridPane != null)
            locauxGridPane.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  DONNÉES
    // ─────────────────────────────────────────────────────────────────────────────

    private void loadData() {
        try {
            localList.clear();
            localList.addAll(localService.afficherList());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les locaux", e.getMessage());
        }
    }

    private void initializeFilters() {
        List<String> types = localList.stream()
                .map(l -> l.getType_local().getLibelle())
                .distinct().sorted().collect(Collectors.toList());
        types.add(0, "Tous les types");
        filterTypeComboBox.setItems(FXCollections.observableArrayList(types));
        filterTypeComboBox.setValue("Tous les types");

        List<String> villes = localList.stream()
                .map(local_psychiatrie::getVille_local)
                .distinct().sorted().collect(Collectors.toList());
        villes.add(0, "Toutes les villes");
        filterVilleComboBox.setItems(FXCollections.observableArrayList(villes));
        filterVilleComboBox.setValue("Toutes les villes");
    }

    private void filterData() {
        String search     = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String typeFilter = filterTypeComboBox.getValue();
        String villeFilter = filterVilleComboBox.getValue();

        filteredList.clear();
        filteredList.addAll(localList.stream().filter(local -> {
            boolean matchSearch = search.isEmpty()
                    || local.getNom_local().toLowerCase().contains(search)
                    || local.getVille_local().toLowerCase().contains(search)
                    || local.getAdresse_local().toLowerCase().contains(search);
            boolean matchType  = typeFilter  == null || typeFilter .equals("Tous les types")
                    || local.getType_local().getLibelle().equals(typeFilter);
            boolean matchVille = villeFilter == null || villeFilter.equals("Toutes les villes")
                    || local.getVille_local().equals(villeFilter);
            return matchSearch && matchType && matchVille;
        }).collect(Collectors.toList()));

        displayLocauxCards();
        updateStatistiques();
    }

    private void displayLocauxCards() {
        locauxGridPane.getChildren().clear();
        List<local_psychiatrie> toDisplay =
                filteredList.isEmpty() && searchField.getText().isEmpty()
                        ? localList : filteredList;

        boolean isEmpty = toDisplay.isEmpty();
        if (emptyStateContainer != null) {
            emptyStateContainer.setVisible(isEmpty);
            emptyStateContainer.setManaged(isEmpty);
        }

        int col = 0, row = 0;
        final int maxCols = 3;
        for (local_psychiatrie local : toDisplay) {
            VBox card = createLocalCard(local);
            locauxGridPane.add(card, col, row);
            col++;
            if (col >= maxCols) { col = 0; row++; }
        }
        updateStatistiques();
    }

    private VBox createLocalCard(local_psychiatrie local) {
        VBox card = new VBox(0);
        card.setPrefWidth(380);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 18, 0, 0, 5); " +
                        "-fx-cursor: hand;");

        // Image
        ImageView imgView = new ImageView();
        imgView.setFitWidth(380); imgView.setFitHeight(200);
        imgView.setPreserveRatio(false);
        if (local.getImageURL() != null && !local.getImageURL().isEmpty()) {
            Image img = ImageManager.loadImage(local.getImageURL(), 380, 200, true);
            if (img != null) imgView.setImage(img);
        }

        // Contenu
        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        Label nom = new Label(local.getNom_local());
        nom.setStyle("-fx-font-size:16px; -fx-font-weight:900; -fx-text-fill:#1B4332;");

        Label ville = new Label("📍 " + local.getVille_local() + " • " + local.getType_local().getLibelle());
        ville.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280;");

        // Badge dispo
        Label badge = new Label();
        if ("Disponible".equals(local.getDisponibilite_local())) {
            badge.setText("✓ Disponible");
            badge.setStyle("-fx-background-color:#D8F3DC; -fx-text-fill:#1B4332; " +
                    "-fx-font-weight:700; -fx-padding:4px 12px; -fx-background-radius:20px;");
        } else {
            badge.setText("✗ " + local.getDisponibilite_local());
            badge.setStyle("-fx-background-color:#FEE2E2; -fx-text-fill:#DC2626; " +
                    "-fx-font-weight:700; -fx-padding:4px 12px; -fx-background-radius:20px;");
        }

        // Bouton Réserver
        Button btnReserver = new Button("📅  Réserver");
        btnReserver.setStyle(
                "-fx-background-color: linear-gradient(to right, #1B4332, #2D6A4F); " +
                        "-fx-text-fill: white; -fx-font-weight:700; -fx-font-size:13px; " +
                        "-fx-background-radius:10px; -fx-padding:10px 20px; -fx-cursor:hand; " +
                        "-fx-max-width: Infinity;");
        HBox.setHgrow(btnReserver, Priority.ALWAYS);

        String lang = LanguageManager.getInstance().getCurrentLanguage();
        if (!TranslationService.LANG_FR.equals(lang)) {
            translationService.traduireAsync("Réserver", traduit ->
                    btnReserver.setText("📅  " + traduit));
        }

        btnReserver.setOnAction(e -> ouvrirDetailsLocal(local));
        if (!"Disponible".equals(local.getDisponibilite_local())) {
            btnReserver.setDisable(true);
            btnReserver.setStyle(btnReserver.getStyle() + "; -fx-opacity:0.5;");
        }

        content.getChildren().addAll(nom, ville, badge, btnReserver);
        card.getChildren().addAll(imgView, content);

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.25), 28, 0, 0, 10); " +
                        "-fx-cursor: hand; -fx-translate-y: -3;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 18px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 18, 0, 0, 5); " +
                        "-fx-cursor: hand;"));

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  SWAP DE VUES : liste ↔ réservation inline
    // ─────────────────────────────────────────────────────────────────────────────

    private void ouvrirDetailsLocal(local_psychiatrie local) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/FrontOfficeDetailsLocal.fxml"));
            Parent root = loader.load();

            FrontOfficeDetailsLocalController ctrl = loader.getController();
            ctrl.setLocal(local);
            ctrl.setParentController(this);

            if (reservationContainer != null) {
                reservationContainer.getChildren().setAll(root);
            }
            if (lblBreadcrumbLocal != null) {
                lblBreadcrumbLocal.setText(local.getNom_local());
            }

            showReservationView();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible de charger la réservation", e.getMessage());
        }
    }

    private void showReservationView() {
        // Masquer la barre de recherche/filtres
        if (barreRecherche != null) {
            barreRecherche.setVisible(false);
            barreRecherche.setManaged(false);
        }
        // Masquer locaux et salles, afficher réservation
        if (vueLocaux     != null) { vueLocaux    .setVisible(false); vueLocaux    .setManaged(false); }
        if (vueSalles     != null) { vueSalles    .setVisible(false); vueSalles    .setManaged(false); }
        if (vueReservation != null) {
            vueReservation.setVisible(true);
            vueReservation.setManaged(true);
            FadeTransition fade = new FadeTransition(Duration.millis(350), vueReservation);
            fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        }
        // Réinitialiser le style du bouton Salles
        surlignerBoutonSalles(false);
    }

    /**
     * Retourne à la liste des locaux depuis la vue réservation.
     * Appelé par le bouton "← Retour" et par FrontOfficeDetailsLocalController.
     */
    public void showListView() {
        if (vueReservation != null) {
            vueReservation.setVisible(false);
            vueReservation.setManaged(false);
        }
        if (reservationContainer != null) {
            reservationContainer.getChildren().clear();
        }
        // Réafficher barre de recherche et vue locaux
        if (barreRecherche != null) {
            barreRecherche.setVisible(true);
            barreRecherche.setManaged(true);
        }
        if (vueLocaux != null) {
            vueLocaux.setVisible(true);
            vueLocaux.setManaged(true);
            FadeTransition fade = new FadeTransition(Duration.millis(350), vueLocaux);
            fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        }
        surlignerBoutonSalles(false);
        refreshDisplay();
    }

    @FXML
    public void handleRetourListe() {
        showListView();
    }

    private void updateStatistiques() {
        if (lblStatistiques == null) return;
        long dispo = localList.stream()
                .filter(l -> "Disponible".equals(l.getDisponibilite_local())).count();
        lblStatistiques.setText(localList.size() + " locaux • " + dispo + " disponibles");
    }

    public void refreshDisplay() {
        loadData();
        filterData();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleNavigateToAccueil() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToAccueil(); return; }
        refreshDisplay();
    }

    @FXML
    private void handleNavigateToServices() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToServices(); return; }
        refreshDisplay();
    }

    @FXML
    private void handleNavigateToEquipe() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToEquipe(); return; }
        refreshDisplay();
    }

    @FXML
    private void handleNavigateToContact() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToContact(); return; }
        refreshDisplay();
    }

    @FXML private void handleRafraichir() { refreshDisplay(); }

    @FXML
    private void handleNavigateToReservations() {
        if (parentHomeController != null) {
            parentHomeController.loadMesReservations();
            return;
        }
        naviguerVers("/FrontOfficeMesReservations.fxml", "MindAura – Mes Réservations", 1200, 750);
    }

    @FXML
    private void handleNavigateToRecommandations() {
        if (parentHomeController != null) {
            parentHomeController.loadRecommandations();
            return;
        }
        naviguerVers("/Recommandation.fxml", "MindAura – Recommandations", 950, 750);
    }

    @FXML
    private void handleNavigateToCalendrier() {
        if (parentHomeController != null) {
            parentHomeController.loadCalendrier();
            return;
        }
        naviguerVers("/CalendrierReservation.fxml", "MindAura – Calendrier", 1100, 750);
    }

    @FXML
    private void handleNavigateToCarte() {
        if (parentHomeController != null) {
            parentHomeController.loadCarte();
            return;
        }
        naviguerVers("/CarteGeographique.fxml", "MindAura – Carte", 1100, 750);
    }

    @FXML
    private void handleNavigateToChatbot() {
        if (parentHomeController != null) {
            parentHomeController.loadChatbot();
            return;
        }
        naviguerVers("/Chatbot.fxml", "MindAura – Assistant IA", 800, 650);
    }

    @FXML
    private void handleQRCode() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QRCode.fxml"));
            Parent root = loader.load();

            Stage qrStage = new Stage();
            qrStage.setTitle("QR Code - Locaux Disponibles");
            qrStage.setResizable(false);

            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            qrStage.setScene(scene);

            qrStage.setOnHidden(e -> tn.esprit.utils.LocalApiServer.getInstance().stop());
            qrStage.initModality(javafx.stage.Modality.NONE);
            qrStage.centerOnScreen();
            qrStage.show();

        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir la fenêtre QR Code", e.getMessage());
        }
    }

    @FXML
    private void handleRetourChoix() {
        if (parentHomeController != null) {
            parentHomeController.restoreHome();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Choix.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRafraichir.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setTitle("MindAura - Choix");
            stage.setWidth(900); stage.setHeight(650);
            stage.centerOnScreen();
            stage.setScene(scene);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void naviguerVers(String fxmlPath, String titre, double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnRafraichir.getScene().getWindow();
            Scene scene = new Scene(root);
            URL cssUrl = getClass().getResource("/style.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setTitle(titre);
            stage.setWidth(w); stage.setHeight(h);
            stage.centerOnScreen();
            stage.setScene(scene);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  ANIMATIONS
    // ─────────────────────────────────────────────────────────────────────────────

    private void applyFadeInAnimation() {
        if (locauxGridPane != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(800), locauxGridPane);
            fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        }
    }

    private void applyFABPulseAnimation() {
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1200), btnChatbotFlottant);
        pulse.setFromX(1.0); pulse.setToX(1.08);
        pulse.setFromY(1.0); pulse.setToY(1.08);
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.setAutoReverse(true); pulse.play();
    }

    private void applyFABHoverEffect() {
        btnChatbotFlottant.setOnMouseEntered(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(150), btnChatbotFlottant);
            s.setToX(1.15); s.setToY(1.15); s.play();
        });
        btnChatbotFlottant.setOnMouseExited(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(150), btnChatbotFlottant);
            s.setToX(1.0); s.setToY(1.0); s.play();
        });
    }

    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(header);
        alert.setContentText(content); alert.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    //  TRADUCTION — appelée automatiquement par TranslatableController
    // ─────────────────────────────────────────────────────────────────────────────

    @Override
    protected void traduireUI(String langCode) {
        tr(btnMesReservations,  "📋  Mes Réservations");
        trPrompt(searchField,   "Rechercher un local...");
        if (lblTitreSection != null) tr(lblTitreSection, "Locaux Disponibles");
        if (lblAucunLocal   != null) tr(lblAucunLocal,   "Aucun local disponible");
        if (lblAideMessage  != null) tr(lblAideMessage,  "💬  Besoin d'aide ?");
        if ("ar".equals(langCode)) {
            if (locauxGridPane != null)
                locauxGridPane.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
        } else {
            if (locauxGridPane != null)
                locauxGridPane.setNodeOrientation(javafx.geometry.NodeOrientation.LEFT_TO_RIGHT);
        }
    }
}