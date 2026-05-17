package tn.esprit.controllers;

import tn.esprit.controllers.ArticleViewerController;
import tn.esprit.controllers.PDFViewerController;
import tn.esprit.controllers.RecommandationRessourceController;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Ressources;
import tn.esprit.services.FavoriService;
import tn.esprit.services.RessourcesService;
import tn.esprit.utils.MyDataBase;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.util.*;
import java.util.stream.Collectors;

public class FrontOfficeController {

    // ─── Référence vers UserHome (pour garder la navbar globale) ─────────────
    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    // ─── CHAMPS FXML ─────────────────────────────────────────
    @FXML private ScrollPane scrollPaneFront;
    @FXML private FlowPane   contentFlowFront;
    @FXML private TextField  searchFieldFront;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private HBox       filterBoxFront;
    @FXML private Label      resultatsLabel;
    @FXML private Label      nombreLabel;
    @FXML private Button     btnFavoris;

    @FXML private Button btnFiltrerTous;
    @FXML private Button btnFiltrerVideo;
    @FXML private Button btnFiltrerPDF;
    @FXML private Button btnFiltrerPodcast;
    @FXML private Button btnFiltrerText;   // correspond à "Article"

    private boolean afficherSeulementFavoris = false;

    // ─── VARIABLES ───────────────────────────────────────────
    private RessourcesService ressourceService;
    private FavoriService favoriService;
    private Connection connection;
    private ObservableList<Ressources> allRessources = FXCollections.observableArrayList();
    private String currentFilter = "TOUS";
    private String tagActif      = null;
    private Set<Integer> favorisIds = new HashSet<>();  // cache des IDs favoris

    // ═══════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        connection       = MyDataBase.getInstance().getConx();
        ressourceService = new RessourcesService(connection);
        favoriService    = new FavoriService(); // userId=1 par défaut

        contentFlowFront.setHgap(0);
        contentFlowFront.setVgap(0);
        contentFlowFront.setPadding(new Insets(24, 44, 40, 44));
        contentFlowFront.setAlignment(Pos.TOP_LEFT);
        contentFlowFront.setPrefWrapLength(1800);

        searchFieldFront.textProperty().addListener((obs, ancien, nouveau) -> {
            if (nouveau == null || nouveau.trim().isEmpty()) {
                tagActif = null;
                appliquerFiltre();
            } else {
                rechercherRessourcesAvancee(nouveau, "TOUS", "TOUS", "TOUS");
            }
        });

        chargerRessources();
        sortComboBox.getItems().addAll("A -> Z", "Recent", "Ancien");
        sortComboBox.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> appliquerTri(newVal));
        setupContentFilters();
    }

    // ─── FILTRES BOUTONS ─────────────────────────────────────
    private void setupContentFilters() { setActiveFilter(btnFiltrerTous); }

    private static final String STYLE_ACTIF =
            "-fx-background-color: white; -fx-text-fill: #1B4332;" +
                    "-fx-background-radius: 20; -fx-padding: 8 20;" +
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 6, 0, 0, 2);";

    private static final String STYLE_INACTIF =
            "-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: rgba(255,255,255,0.85);" +
                    "-fx-background-radius: 20; -fx-padding: 8 20;" +
                    "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;" +
                    "-fx-border-color: rgba(255,255,255,0.20); -fx-border-width: 1; -fx-border-radius: 20;";

    private void setActiveFilter(Button cible) {
        for (Button b : new Button[]{btnFiltrerTous, btnFiltrerVideo,
                btnFiltrerPDF, btnFiltrerPodcast, btnFiltrerText})
            if (b != null) b.setStyle(b == cible ? STYLE_ACTIF : STYLE_INACTIF);
    }

    // ─── NAVIGATION ──────────────────────────────────────────
    @FXML
    private void retourAccueil() {
        if (parentHomeController != null) {
            parentHomeController.restoreHomeAndScrollToServices();
            return;
        }
        try {
            Stage stage = (Stage) scrollPaneFront.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AccueilRessource.fxml"));
            boolean wasMax = stage.isMaximized();
            if (wasMax) stage.setMaximized(false);
            stage.setScene(new Scene(loader.load()));
            if (wasMax) stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── CHARGEMENT ──────────────────────────────────────────
    private void chargerRessources() {
        try {
            allRessources.clear();
            allRessources.addAll(ressourceService.getAllRessources());

            // Charger les favoris depuis la base
            favorisIds = favoriService.chargerFavoris();

            appliquerFiltre();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les ressources");
        }
    }

    @FXML
    private void allerRecommandations() {
        if (parentHomeController != null) {
            parentHomeController.loadRecommandationRessource(new ArrayList<>(allRessources));
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RecommandationRessource.fxml"));
            Parent root = loader.load();
            RecommandationRessourceController ctrl = loader.getController();
            ctrl.setContexte(null, new ArrayList<>(allRessources));
            Stage stage = (Stage) scrollPaneFront.getScene().getWindow();
            boolean wasMax = stage.isMaximized();
            if (wasMax) stage.setMaximized(false);
            stage.setScene(new Scene(root));
            if (wasMax) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les recommandations.");
        }
    }

    // ─── FILTRE + TRI ─────────────────────────────────────────
    @FXML
    private void appliquerFiltre() {
        ObservableList<Ressources> filteredList = FXCollections.observableArrayList();
        for (Ressources r : allRessources) {
            String contenu = r.getContenu();
            boolean matchType = currentFilter.equals("TOUS") ||
                    (contenu != null && contenu.equalsIgnoreCase(currentFilter));
            boolean matchFavoris = !afficherSeulementFavoris || favorisIds.contains(r.getId_ressources());
            if (matchType && matchFavoris) filteredList.add(r);
        }
        String tri = sortComboBox.getSelectionModel().getSelectedItem();
        if (tri != null) appliquerTriSurListe(filteredList, tri);
        afficherRessources(filteredList);
        updateResultsLabel(
                currentFilter.equals("TOUS") ? "Toutes les ressources" : currentFilter,
                filteredList.size());
    }

    private void appliquerTri(String tri) {
        ObservableList<Ressources> currentList = FXCollections.observableArrayList(allRessources);
        appliquerTriSurListe(currentList, tri);
        afficherRessources(currentList);
    }

    private void appliquerTriSurListe(List<Ressources> list, String tri) {
        switch (tri) {
            case "A -> Z" -> list.sort((a, b) -> {
                String tA = a.getTitre() != null ? a.getTitre() : "";
                String tB = b.getTitre() != null ? b.getTitre() : "";
                return tA.compareToIgnoreCase(tB);
            });
            case "Recent" -> list.sort((a, b) -> {
                Date da = a.getDate_publication();
                Date db = b.getDate_publication();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.compareTo(da);
            });
            case "Ancien" -> list.sort((a, b) -> {
                Date da = a.getDate_publication();
                Date db = b.getDate_publication();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return da.compareTo(db);
            });
        }
    }

    // ─── RECHERCHE AVANCÉE ───────────────────────────────────
    public void rechercherRessourcesAvancee(String searchText, String niveauFilter,
                                            String categorieFilter, String tagFilter) {
        String text = searchText != null ? searchText.trim().toLowerCase() : "";
        ObservableList<Ressources> results = FXCollections.observableArrayList();
        for (Ressources r : allRessources) {
            String contenu = r.getContenu();
            boolean matchType = currentFilter.equals("TOUS") ||
                    (contenu != null && contenu.equalsIgnoreCase(currentFilter));
            boolean matchText = text.isEmpty() ||
                    (r.getTitre()  != null && r.getTitre().toLowerCase().contains(text)) ||
                    (r.getResume() != null && r.getResume().toLowerCase().contains(text));
            boolean matchNiveau = niveauFilter == null || niveauFilter.equalsIgnoreCase("TOUS") ||
                    (r.getNiveau() != null && r.getNiveau().equalsIgnoreCase(niveauFilter));
            boolean matchCategorie = categorieFilter == null || categorieFilter.equalsIgnoreCase("TOUS") ||
                    (r.getCategorie() != null && r.getCategorie().equalsIgnoreCase(categorieFilter));
            boolean matchTag = tagFilter == null || tagFilter.equalsIgnoreCase("TOUS") ||
                    (r.getTags() != null && extraireTags(r).stream()
                            .anyMatch(tag -> tag.equalsIgnoreCase(tagFilter)));
            if (matchType && matchText && matchNiveau && matchCategorie && matchTag)
                results.add(r);
        }
        afficherRessources(results);
        if (tagFilter != null && !tagFilter.equalsIgnoreCase("TOUS"))
            updateResultsLabel("Tag : \"" + tagFilter + "\"", results.size());
        else if (!text.isEmpty())
            updateResultsLabel("Recherche : \"" + searchText + "\"", results.size());
        else
            updateResultsLabel("Toutes les ressources", results.size());
    }

    @FXML
    private void rechercherRessourcesFront(ActionEvent event) {
        String texte = searchFieldFront.getText().trim();
        if (texte.isEmpty()) { tagActif = null; appliquerFiltre(); return; }
        boolean estUnTag = allRessources.stream()
                .filter(r -> r.getTags() != null)
                .flatMap(r -> extraireTags(r).stream())
                .anyMatch(tag -> tag.equalsIgnoreCase(texte));
        if (estUnTag) rechercherParTag(texte);
        else { tagActif = null; rechercherRessourcesAvancee(texte, "TOUS", "TOUS", "TOUS"); }
    }

    public void rechercherParTag(String tag) {
        tagActif = tag;
        rechercherRessourcesAvancee("", "TOUS", "TOUS", tag);
    }

    // ═══════════════════════════════════════════════════════════
    // AFFICHAGE PRINCIPAL — sections ordonnées
    // ═══════════════════════════════════════════════════════════
    private void afficherRessources(List<Ressources> ressources) {
        contentFlowFront.getChildren().clear();

        if (ressources.isEmpty()) {
            VBox empty = creerEmptyState();
            empty.setPrefWidth(1400);
            contentFlowFront.getChildren().add(empty);
            return;
        }

        List<Ressources> podcasts = ressources.stream()
                .filter(r -> r.getContenu() != null &&
                        r.getContenu().equalsIgnoreCase("Podcast")).toList();
        List<Ressources> videos   = ressources.stream()
                .filter(r -> r.getContenu() != null &&
                        r.getContenu().equalsIgnoreCase("Video")).toList();
        List<Ressources> pdfs     = ressources.stream()
                .filter(r -> r.getContenu() != null &&
                        r.getContenu().equalsIgnoreCase("PDF")).toList();
        List<Ressources> articles = ressources.stream()
                .filter(r -> r.getContenu() != null &&
                        r.getContenu().equalsIgnoreCase("Article")).toList();
        List<Ressources> autres   = ressources.stream()
                .filter(r -> r.getContenu() == null ||
                        (!r.getContenu().equalsIgnoreCase("Podcast") &&
                                !r.getContenu().equalsIgnoreCase("Video")   &&
                                !r.getContenu().equalsIgnoreCase("PDF")     &&
                                !r.getContenu().equalsIgnoreCase("Article"))).toList();

        VBox mainContainer = new VBox(0);
        mainContainer.setMaxWidth(Double.MAX_VALUE);
        mainContainer.setPrefWidth(1400);

        int[] delay = {0};

        int nbTypes = (podcasts.isEmpty() ? 0 : 1) + (videos.isEmpty() ? 0 : 1) +
                (pdfs.isEmpty() ? 0 : 1)     + (articles.isEmpty() ? 0 : 1) +
                (autres.isEmpty() ? 0 : 1);
        boolean multiTypes = nbTypes > 1;

        // 1. PODCASTS
        if (!podcasts.isEmpty()) {
            if (multiTypes)
                mainContainer.getChildren().add(
                        creerSectionHeader("🎧", "Podcasts", podcasts.size(), "#2563EB"));

            VBox podSection = new VBox(14);
            podSection.setMaxWidth(Double.MAX_VALUE);
            podSection.setStyle("-fx-padding: 0 0 32 0;");

            for (Ressources r : podcasts) {
                VBox card = creerCartePodcast(r);
                card.setOpacity(0);
                card.setTranslateY(16);
                podSection.getChildren().add(card);
                animer(card, delay[0]++);
            }
            mainContainer.getChildren().add(podSection);
        }

        // 2. VIDÉOS
        if (!videos.isEmpty()) {
            if (multiTypes)
                mainContainer.getChildren().add(
                        creerSectionHeader("🎬", "Vidéos", videos.size(), "#047857"));
            mainContainer.getChildren().add(creerGrille(videos, delay));
        }

        // 3. PDF
        if (!pdfs.isEmpty()) {
            if (multiTypes)
                mainContainer.getChildren().add(
                        creerSectionHeader("📄", "PDF", pdfs.size(), "#B91C1C"));
            mainContainer.getChildren().add(creerGrille(pdfs, delay));
        }

        // 4. ARTICLES
        if (!articles.isEmpty()) {
            if (multiTypes)
                mainContainer.getChildren().add(
                        creerSectionHeader("📰", "Articles", articles.size(), "#0369A1"));
            mainContainer.getChildren().add(creerGrille(articles, delay));
        }

        // 5. AUTRES
        if (!autres.isEmpty())
            mainContainer.getChildren().add(creerGrille(autres, delay));

        contentFlowFront.getChildren().add(mainContainer);
    }

    // ─── SECTION HEADER ──────────────────────────────────────
    private Label creerSectionHeader(String emoji, String titre, int count, String couleur) {
        Label label = new Label(emoji + "  " + titre + "   (" + count + ")");
        label.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + couleur + ";" +
                        "-fx-padding: 12 0 10 4;" +
                        "-fx-border-color: transparent transparent " + couleur + "33 transparent;" +
                        "-fx-border-width: 0 0 2 0;");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    // ─── GRILLE INTERNE (FlowPane) ────────────────────────────
    private FlowPane creerGrille(List<Ressources> items, int[] delay) {
        FlowPane grille = new FlowPane();
        grille.setHgap(22);
        grille.setVgap(22);
        grille.setPrefWrapLength(1400);
        grille.setMaxWidth(Double.MAX_VALUE);
        grille.setStyle("-fx-padding: 0 0 32 0; -fx-background-color: transparent;");

        for (Ressources r : items) {
            VBox card = creerCarteRessourceFront(r);
            card.setOpacity(0);
            card.setTranslateY(16);
            grille.getChildren().add(card);
            animer(card, delay[0]++);
        }
        return grille;
    }

    // ─── ANIMATION ENTRÉE ────────────────────────────────────
    private void animer(Node node, int index) {
        FadeTransition fade = new FadeTransition(Duration.millis(320), node);
        fade.setFromValue(0); fade.setToValue(1);
        fade.setDelay(Duration.millis(index * 55L));

        TranslateTransition slide = new TranslateTransition(Duration.millis(320), node);
        slide.setFromY(16); slide.setToY(0);
        slide.setDelay(Duration.millis(index * 55L));

        new ParallelTransition(fade, slide).play();
    }

    // ─── LABEL RÉSULTATS ─────────────────────────────────────
    private void updateResultsLabel(String titre, int count) {
        if (resultatsLabel != null) resultatsLabel.setText(titre);
        if (nombreLabel != null)
            nombreLabel.setText(count + " ressource" + (count > 1 ? "s" : ""));
    }

    // ─── EMPTY STATE ─────────────────────────────────────────
    private VBox creerEmptyState() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPrefHeight(380);
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20;" +
                        "-fx-padding: 60;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 4);");
        Label emoji = new Label(tagActif != null ? "🏷" : "📭");
        emoji.setStyle("-fx-font-size: 52px;");
        Label message = new Label(tagActif != null
                ? "Aucune ressource avec le tag \"" + tagActif + "\""
                : "Aucune ressource trouvée");
        message.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1B4332;");
        Label soustitre = new Label("Essayez de changer vos filtres ou votre recherche");
        soustitre.setStyle("-fx-font-size: 13px; -fx-text-fill: #8A9A8F;");
        box.getChildren().addAll(emoji, message, soustitre);
        return box;
    }

    // ════════════════════════════════════════════════════════════
    // CARTE GRILLE (Video / PDF / Article) — 300px
    // ════════════════════════════════════════════════════════════
    private VBox creerCarteRessourceFront(Ressources r) {
        String type = r.getContenu() != null ? r.getContenu() : "?";
        boolean estFavori = favorisIds.contains(r.getId_ressources());

        VBox card = new VBox(0);
        card.setPrefWidth(300);
        card.setMaxWidth(300);
        card.setMinWidth(300);
        card.setStyle(carteStyleNormal());

        card.setOnMouseEntered(e -> {
            card.setStyle(carteStyleHover());
            ScaleTransition st = new ScaleTransition(Duration.millis(170), card);
            st.setToX(1.025); st.setToY(1.025); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(carteStyleNormal());
            ScaleTransition st = new ScaleTransition(Duration.millis(170), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        // Zone image
        StackPane imageZone = new StackPane();
        imageZone.setPrefHeight(175);
        imageZone.setMinHeight(175);
        imageZone.setMaxHeight(175);
        imageZone.setStyle(
                "-fx-background-radius: 16 16 0 0;" +
                        "-fx-background-color: " + getCouleurFond(type) + ";");

        Rectangle clip = new Rectangle(300, 175);
        clip.setArcWidth(32); clip.setArcHeight(32);
        imageZone.setClip(clip);

        boolean aImage = chargerImage(imageZone, r.getImageUrl(), 300, 175);

        if (!aImage) {
            Label icone = new Label(getIconeType(type));
            icone.setStyle("-fx-font-size: 50px; -fx-opacity: 0.30;");
            imageZone.getChildren().add(icone);
            ajouterCerclesDecor(imageZone);
        } else {
            Region overlay = new Region();
            overlay.setPrefSize(300, 175);
            overlay.setStyle("-fx-background-color: linear-gradient(to bottom," +
                    " transparent 38%, rgba(0,0,0,0.48) 100%);");
            imageZone.getChildren().add(overlay);
        }

        // Badge type
        Label badge = new Label(getIconeType(type) + "  " + type);
        badge.setStyle(getBadgeStyle(type));
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10, 0, 0, 10));
        imageZone.getChildren().add(badge);

        // Bouton favori (n'utilise pas r.isFavori mais le cache)
        Button favoriBtn = new Button(estFavori ? "★" : "☆");
        favoriBtn.setStyle(favoriStyle(estFavori));
        StackPane.setAlignment(favoriBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(favoriBtn, new Insets(10, 10, 0, 0));
        favoriBtn.setOnAction(e -> {
            boolean nouvelEtat = !estFavori;
            if (nouvelEtat) {
                favoriService.ajouterFavori(r.getId_ressources());
                favorisIds.add(r.getId_ressources());
            } else {
                favoriService.supprimerFavori(r.getId_ressources());
                favorisIds.remove(r.getId_ressources());
            }
            favoriBtn.setText(nouvelEtat ? "★" : "☆");
            favoriBtn.setStyle(favoriStyle(nouvelEtat));
            if (afficherSeulementFavoris && !nouvelEtat) appliquerFiltre();
        });
        imageZone.getChildren().add(favoriBtn);

        // Zone texte
        VBox textZone = new VBox(8);
        textZone.setStyle(
                "-fx-padding: 14 16 14 16; -fx-background-color: white;" +
                        "-fx-background-radius: 0 0 16 16;");
        VBox.setVgrow(textZone, Priority.ALWAYS);

        Label titre = new Label(r.getTitre() != null ? r.getTitre() : "Sans titre");
        titre.setWrapText(true);
        titre.setMaxHeight(46);
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;" +
                "-fx-text-fill: #0D2B1E; -fx-line-spacing: 2;");

        Label resume = new Label(r.getResume() != null ? r.getResume() : "");
        resume.setWrapText(true);
        resume.setMaxHeight(48);
        resume.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B8C7A; -fx-line-spacing: 2;");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #EEF3F0;");

        // Footer
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 6 0 0 0;");

        HBox vuesBox  = creerStatInline("👁", String.valueOf(r.getNbr_vues()));
        HBox likesBox = creerStatInline("❤", String.valueOf(r.getLikes()));

        if (r.getNiveau() != null) {
            Label niveauBadge = new Label(r.getNiveau()); // pas .name()
            niveauBadge.setStyle(
                    "-fx-background-color: #F0F9F4; -fx-text-fill: #2D6A4F;" +
                            "-fx-background-radius: 8; -fx-padding: 2 8;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;");
            likesBox.getChildren().add(niveauBadge);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVoir = new Button("Voir →");
        String bvBase  = "-fx-background-color: #1B4332; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 6 14;" +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;";
        String bvHover = "-fx-background-color: #2D6A4F; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-padding: 6 14;" +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;";
        btnVoir.setStyle(bvBase);
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(bvHover));
        btnVoir.setOnMouseExited(e  -> btnVoir.setStyle(bvBase));
        btnVoir.setOnAction(e -> naviguerVersRessource(r, btnVoir));

        footer.getChildren().addAll(vuesBox, likesBox, spacer, btnVoir);
        textZone.getChildren().addAll(titre, resume, sep, footer);
        card.getChildren().addAll(imageZone, textZone);

        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            naviguerVersRessource(r, card);
        });

        return card;
    }

    // ════════════════════════════════════════════════════════════
    // CARTE PODCAST — horizontale pleine largeur
    // ════════════════════════════════════════════════════════════
    private VBox creerCartePodcast(Ressources r) {
        boolean estFavori = favorisIds.contains(r.getId_ressources());

        VBox wrapper = new VBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);

        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(105);
        card.setPrefHeight(112);
        card.setMaxWidth(Double.MAX_VALUE);

        String styleBase =
                "-fx-background-color: linear-gradient(to right, #1E3A8A 0%, #2563EB 55%, #3B82F6 100%);" +
                        "-fx-background-radius: 18; -fx-padding: 16 28 16 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.38), 18, 0, 0, 6);" +
                        "-fx-cursor: hand;";
        String styleHover =
                "-fx-background-color: linear-gradient(to right, #1E40AF 0%, #1D4ED8 55%, #2563EB 100%);" +
                        "-fx-background-radius: 18; -fx-padding: 16 28 16 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(29,78,216,0.55), 24, 0, 0, 10);" +
                        "-fx-cursor: hand;";
        card.setStyle(styleBase);
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e  -> card.setStyle(styleBase));

        // Miniature
        StackPane thumb = new StackPane();
        thumb.setPrefSize(78, 78); thumb.setMinSize(78, 78); thumb.setMaxSize(78, 78);
        thumb.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 14;");
        if (!chargerImage(thumb, r.getImageUrl(), 78, 78)) {
            Label ic = new Label("🎙"); ic.setStyle("-fx-font-size: 30px;");
            thumb.getChildren().add(ic);
        }

        // Bouton play
        StackPane playBtn = new StackPane();
        playBtn.setPrefSize(48, 48); playBtn.setMinSize(48, 48); playBtn.setMaxSize(48, 48);
        String playBase  = "-fx-background-color: white; -fx-background-radius: 50%;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 10, 0, 0, 2); -fx-cursor: hand;";
        String playHover = "-fx-background-color: #EEF2FF; -fx-background-radius: 50%;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 12, 0, 0, 3); -fx-cursor: hand;";
        playBtn.setStyle(playBase);
        playBtn.setOnMouseEntered(e -> playBtn.setStyle(playHover));
        playBtn.setOnMouseExited(e  -> playBtn.setStyle(playBase));
        Label playIc = new Label("▶");
        playIc.setStyle("-fx-font-size: 15px; -fx-text-fill: #2563EB; -fx-padding: 0 0 0 3;");
        playBtn.getChildren().add(playIc);
        playBtn.setOnMouseClicked(e -> naviguerVersRessource(r, (Node) e.getSource()));

        // Waveform animée
        HBox waveform = creerFormeOnde();

        // Séparateur vertical
        Region sepV = new Region();
        sepV.setPrefSize(1.5, 55); sepV.setMinSize(1.5, 55);
        sepV.setStyle("-fx-background-color: rgba(255,255,255,0.22);");

        // Infos
        VBox infos = new VBox(6);
        infos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infos, Priority.ALWAYS);

        HBox headerInfos = new HBox(12);
        headerInfos.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label(r.getTitre() != null ? r.getTitre() : "Sans titre");
        titre.setStyle("-fx-font-size: 15.5px; -fx-font-weight: bold; -fx-text-fill: white;");
        titre.setWrapText(true);
        Label podBadge = new Label("🎧  Podcast");
        podBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white;" +
                        "-fx-background-radius: 20; -fx-padding: 3 11;" +
                        "-fx-font-size: 10.5px; -fx-font-weight: bold;" +
                        "-fx-border-color: rgba(255,255,255,0.28); -fx-border-width: 1; -fx-border-radius: 20;");
        headerInfos.getChildren().addAll(titre, podBadge);

        Label resume = new Label(r.getResume() != null ? r.getResume() : "");
        resume.setWrapText(true); resume.setMaxHeight(38);
        resume.setStyle("-fx-font-size: 12.5px; -fx-text-fill: rgba(255,255,255,0.78);");

        HBox statsBox = new HBox(14);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        Label vues = new Label("👁  " + r.getNbr_vues());
        vues.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.65);");
        Label likes = new Label("❤  " + r.getLikes());
        likes.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.65);");
        statsBox.getChildren().addAll(vues, likes);

        if (r.getNiveau() != null) {
            Label niv = new Label(r.getNiveau()); // pas .name()
            niv.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.16); -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px;");
            statsBox.getChildren().add(niv);
        }

        infos.getChildren().addAll(headerInfos, resume, statsBox);

        // Bouton favori podcast
        Button favoriBtn = new Button(estFavori ? "★" : "☆");
        favoriBtn.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 22px; -fx-cursor: hand;" +
                        "-fx-text-fill: " + (estFavori ? "#FFD700" : "rgba(255,255,255,0.80)") + ";" +
                        "-fx-padding: 0 4;");
        favoriBtn.setOnAction(e -> {
            boolean nouvelEtat = !estFavori;
            if (nouvelEtat) {
                favoriService.ajouterFavori(r.getId_ressources());
                favorisIds.add(r.getId_ressources());
            } else {
                favoriService.supprimerFavori(r.getId_ressources());
                favorisIds.remove(r.getId_ressources());
            }
            favoriBtn.setText(nouvelEtat ? "★" : "☆");
            favoriBtn.setStyle(
                    "-fx-background-color: transparent; -fx-font-size: 22px; -fx-cursor: hand;" +
                            "-fx-text-fill: " + (nouvelEtat ? "#FFD700" : "rgba(255,255,255,0.80)") + ";" +
                            "-fx-padding: 0 4;");
            if (afficherSeulementFavoris && !nouvelEtat) appliquerFiltre();
        });

        card.getChildren().addAll(thumb, playBtn, waveform, sepV, infos, favoriBtn);
        card.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            naviguerVersRessource(r, card);
        });

        wrapper.getChildren().add(card);
        return wrapper;
    }

    // ─── WAVEFORM ────────────────────────────────────────────
    private HBox creerFormeOnde() {
        HBox waveform = new HBox(3.5);
        waveform.setAlignment(Pos.CENTER);
        waveform.setPrefWidth(170); waveform.setMinWidth(150);
        int[] heights = {14,24,38,50,34,54,42,60,46,58,52,64,56,48,60,52,44,38,50,42,34,46,38,30,40,34,28,38,24,32};
        for (int i = 0; i < heights.length; i++) {
            Rectangle bar = new Rectangle(5, heights[i]);
            bar.setArcWidth(3); bar.setArcHeight(3);
            double op = 0.50 + (i % 3) * 0.15;
            bar.setFill(javafx.scene.paint.Color.web("rgba(255,255,255," + op + ")"));

            ScaleTransition st = new ScaleTransition(Duration.millis(500 + (i % 6) * 90), bar);
            st.setFromY(1.0); st.setToY(0.35); st.setAutoReverse(true);
            st.setCycleCount(Animation.INDEFINITE);
            st.setDelay(Duration.millis(i * 55));
            st.play();

            waveform.getChildren().add(bar);
        }
        return waveform;
    }

    // ─── HELPERS VISUELS ─────────────────────────────────────
    private String carteStyleNormal() {
        return "-fx-background-color: white; -fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.09), 12, 0, 0, 4);" +
                "-fx-border-color: #EBF0EC; -fx-border-width: 1; -fx-border-radius: 16; -fx-cursor: hand;";
    }

    private String carteStyleHover() {
        return "-fx-background-color: white; -fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.22), 22, 0, 0, 9);" +
                "-fx-border-color: #52B788; -fx-border-width: 1.5; -fx-border-radius: 16; -fx-cursor: hand;";
    }

    private boolean chargerImage(StackPane container, String imageUrl, double w, double h) {
        if (imageUrl == null || imageUrl.isEmpty()) return false;
        try {
            File imgFile = new File(imageUrl);
            String uri = imgFile.exists() ? imgFile.toURI().toString() : imageUrl;
            ImageView iv = new ImageView(new Image(uri, w, h, false, true));
            iv.setFitWidth(w); iv.setFitHeight(h); iv.setPreserveRatio(false);
            Rectangle clip = new Rectangle(w, h);
            clip.setArcWidth(32); clip.setArcHeight(32);
            iv.setClip(clip);
            container.getChildren().add(iv);
            return true;
        } catch (Exception ignored) { return false; }
    }

    private void ajouterCerclesDecor(StackPane zone) {
        int[] rayons = {28, 52, 78};
        Pos[] positions = {Pos.BOTTOM_RIGHT, Pos.TOP_LEFT, Pos.BOTTOM_LEFT};
        for (int i = 0; i < rayons.length; i++) {
            javafx.scene.shape.Circle c = new javafx.scene.shape.Circle(rayons[i]);
            c.setFill(javafx.scene.paint.Color.web("rgba(255,255,255,0.07)"));
            StackPane.setAlignment(c, positions[i]);
            zone.getChildren().add(0, c);
        }
    }

    private HBox creerStatInline(String emoji, String texte) {
        HBox box = new HBox(4); box.setAlignment(Pos.CENTER_LEFT);
        Label em = new Label(emoji); em.setStyle("-fx-font-size: 11px;");
        Label tx = new Label(texte); tx.setStyle("-fx-font-size: 11px; -fx-text-fill: #8AAA96; -fx-font-weight: bold;");
        box.getChildren().addAll(em, tx);
        return box;
    }

    private String favoriStyle(boolean actif) {
        return "-fx-background-color: rgba(255,255,255,0.90);" +
                "-fx-font-size: 16px; -fx-cursor: hand;" +
                "-fx-text-fill: " + (actif ? "#E53E3E" : "#9AA8B5") + ";" +
                "-fx-background-radius: 50%; -fx-min-width: 34px; -fx-min-height: 34px;" +
                "-fx-padding: 0;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);";
    }

    private String getCouleurFond(String type) {
        return switch (type) {
            case "Video"   -> "linear-gradient(135deg, #064E3B 0%, #065F46 50%, #047857 100%)";
            case "PDF"     -> "linear-gradient(135deg, #7F1D1D 0%, #991B1B 50%, #B91C1C 100%)";
            case "Article" -> "linear-gradient(135deg, #0C4A6E 0%, #0369A1 50%, #0EA5E9 100%)";
            default        -> "linear-gradient(135deg, #374151 0%, #4B5563 100%)";
        };
    }

    private String getIconeType(String type) {
        return switch (type) {
            case "Video"   -> "🎬";
            case "PDF"     -> "📄";
            case "Article" -> "📰";
            case "Podcast" -> "🎙";
            default        -> "📁";
        };
    }

    private String getBadgeStyle(String type) {
        String bg = switch (type) {
            case "Video"   -> "rgba(4,120,87,0.92)";
            case "PDF"     -> "rgba(185,28,28,0.92)";
            case "Article" -> "rgba(3,105,161,0.92)";
            default        -> "rgba(55,65,81,0.92)";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
                "-fx-background-radius: 10; -fx-padding: 4 10;" +
                "-fx-font-size: 10.5px; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 4, 0, 0, 1);";
    }

    // ─── UTILITAIRE TAGS ─────────────────────────────────────
    private List<String> extraireTags(Ressources r) {
        if (r.getTags() == null || r.getTags().isBlank()) return Collections.emptyList();
        return Arrays.stream(r.getTags().split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }

    // ─── NAVIGATION VERS RESSOURCE ────────────────────────────
    private boolean estPodcast(Ressources r) {
        return r.getContenu() != null && r.getContenu().equalsIgnoreCase("Podcast");
    }

    private void naviguerVersRessource(Ressources r, Node source) {
        if (r.getContenu() == null) return;

        if (estPodcast(r)) { ouvrirUrlPodcast(r); return; }

        if (parentHomeController != null) {
            String type = r.getContenu();
            String fxmlPath;
            if (type.equalsIgnoreCase("ARTICLE")) {
                fxmlPath = "/ArticleViewer.fxml";
            } else {
                fxmlPath = type.equalsIgnoreCase("Video") ? "/VideoPlayer.fxml" : "/PDFViewer.fxml";
            }
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent root = loader.load();
                Object controller = loader.getController();

                Stage stage = (Stage) scrollPaneFront.getScene().getWindow();
                if (controller instanceof ArticleViewerController avc) {
                    avc.setRessource(r, stage);
                    avc.setParentHomeController(parentHomeController);
                } else if (controller instanceof PDFViewerController pvc) {
                    pvc.setRessource(r, stage);
                    pvc.setParentHomeController(parentHomeController);
                } else if (controller instanceof VideoPlayerController vpc) {
                    vpc.setRessource(r, stage);
                    vpc.setParentHomeController(parentHomeController);
                }

                parentHomeController.setCenterContent(root);
            } catch (IOException ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la ressource : " + ex.getMessage());
            }
            return;
        }

        // Fallback standalone
        Stage stage = (Stage) scrollPaneFront.getScene().getWindow();
        boolean wasMax = stage.isMaximized();

        if (r.getContenu().equalsIgnoreCase("ARTICLE")) {
            if (r.getUrl() == null || r.getUrl().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Lien manquant", "Cet article n'a pas d'URL.");
                return;
            }
            String[] chemins = {"/ArticleViewer.fxml", "/articleViewer.fxml", "/fxml/ArticleViewer.fxml"};
            for (String chemin : chemins) {
                java.net.URL fxmlUrl = getClass().getResource(chemin);
                if (fxmlUrl != null) {
                    try {
                        FXMLLoader loader = new FXMLLoader(fxmlUrl);
                        Parent root = loader.load();
                        ArticleViewerController ctrl = loader.getController();
                        ctrl.setRessource(r, stage);
                        ctrl.setPageRetour("/FrontOffice_COMPLET.fxml");
                        if (wasMax) stage.setMaximized(false);
                        stage.setScene(new Scene(root, 1280, 820));
                        if (wasMax) stage.setMaximized(true);
                        stage.show();
                        return;
                    } catch (IOException ex) { ex.printStackTrace(); }
                }
            }
            showAlert(Alert.AlertType.ERROR, "Erreur", "ArticleViewer.fxml introuvable.");
            return;
        }

        String type     = r.getContenu();
        String fxmlPath = type.equalsIgnoreCase("Video") ? "/videoPlayer.fxml" : "/PDFViewer.fxml";

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                showAlert(Alert.AlertType.WARNING, "Page introuvable",
                        "Le fichier " + fxmlPath + " n'existe pas.");
                return;
            }
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof tn.esprit.controllers.PDFViewerController) {
                ((tn.esprit.controllers.PDFViewerController) controller).setRessource(r, stage);
                ((PDFViewerController) controller).setPageRetour("/FrontOffice_COMPLET.fxml");
            } else if (controller instanceof VideoPlayerController) {
                ((VideoPlayerController) controller).setRessource(r, stage);
                ((VideoPlayerController) controller).setPageRetour("/FrontOffice_COMPLET.fxml");
            }
            if (wasMax) stage.setMaximized(false);
            stage.setScene(new Scene(root));
            if (wasMax) stage.setMaximized(true);
            stage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir : " + ex.getMessage());
        }
    }

    private void ouvrirUrlPodcast(Ressources r) {
        String url = r.getUrl();
        if (url == null || url.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lien manquant", "Ce podcast n'a pas d'URL associée.");
            return;
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url)); return;
            }
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if      (os.contains("win")) pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
            else if (os.contains("mac")) pb = new ProcessBuilder("open", url);
            else                         pb = new ProcessBuilder("xdg-open", url);
            pb.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlertAvecUrl("Impossible d'ouvrir le lien. Copiez l'URL :", url);
        }
    }

    private void showAlertAvecUrl(String message, String url) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Lien du podcast"); alert.setHeaderText(message);
        TextArea ta = new TextArea(url);
        ta.setEditable(false); ta.setWrapText(true);
        ta.setPrefRowCount(3); ta.setStyle("-fx-font-size: 13px;");
        alert.getDialogPane().setContent(ta);
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();
    }

    // ─── FAVORIS ─────────────────────────────────────────────
    @FXML
    private void afficherFavoris(ActionEvent event) {
        afficherSeulementFavoris = !afficherSeulementFavoris;
        appliquerFiltre();
        if (btnFavoris != null) {
            btnFavoris.setText(afficherSeulementFavoris ? "★ Favoris (actif)" : "☆ Mes favoris");
            btnFavoris.setStyle(afficherSeulementFavoris
                    ? "-fx-background-color: #FFD700; -fx-text-fill: #1B3A2A;" +
                    "-fx-background-radius: 22; -fx-border-color: #FFD700; -fx-border-width: 1.5;" +
                    "-fx-border-radius: 22; -fx-padding: 10 18; -fx-font-size: 13px;" +
                    "-fx-cursor: hand; -fx-font-weight: bold;" +
                    "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.45), 10, 0, 0, 2);"
                    : "-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
                    "-fx-background-radius: 22; -fx-border-color: rgba(255,255,255,0.25);" +
                    "-fx-border-width: 1.5; -fx-border-radius: 22; -fx-padding: 10 18;" +
                    "-fx-font-size: 13px; -fx-cursor: hand; -fx-font-weight: bold;");
        }
    }

    // ─── FILTRES ─────────────────────────────────────────────
    @FXML private void filtrerTous()    { filtrerPar("TOUS");    }
    @FXML private void filtrerVideo()   { filtrerPar("Video");   }
    @FXML private void filtrerPDF()     { filtrerPar("PDF");     }
    @FXML private void filtrerPodcast() { filtrerPar("Podcast"); }
    @FXML private void filtrerText()    { filtrerPar("Article"); }

    private void filtrerPar(String type) {
        currentFilter = type; tagActif = null;
        if (searchFieldFront != null) searchFieldFront.clear();
        Button cible = switch (type) {
            case "Video"   -> btnFiltrerVideo;
            case "PDF"     -> btnFiltrerPDF;
            case "Podcast" -> btnFiltrerPodcast;
            case "Article" -> btnFiltrerText;
            default        -> btnFiltrerTous;
        };
        setActiveFilter(cible);
        appliquerFiltre();
    }

    // ─── ALERTES ─────────────────────────────────────────────
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}