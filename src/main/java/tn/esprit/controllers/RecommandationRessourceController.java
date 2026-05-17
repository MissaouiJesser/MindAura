package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Ressources;
import tn.esprit.services.FavoriService;
import tn.esprit.services.RecommandationService;
import tn.esprit.services.RessourcesService;
import tn.esprit.utils.MyDataBase;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🎯 CONTROLLER DE LA SECTION RECOMMANDATIONS
 *
 * Usage :
 *   1. Charger recommandations.fxml dans votre page front office
 *   2. Appeler setContexte(ressourceActuelle, toutesRessources) au chargement
 *
 * Le controller choisit automatiquement entre :
 *   - Mode hybride  : si favoris ou ressource de référence disponibles
 *   - Mode cold-start : popularité pure sinon
 */
public class RecommandationRessourceController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private HBox cardsContainer;
    @FXML private Label titreSection;
    @FXML private Label sousTexteSection;
    @FXML private Label badgeMode;
    @FXML private HBox  dotsContainer;
    @FXML private Button btnPrecedent;
    @FXML private Button btnSuivant;
    @FXML private VBox emptyState;

    // ── Services ──────────────────────────────────────────────────────────────
    private RecommandationService recommandationService;
    private RessourcesService ressourcesService;

    // ── Référence parent (UserHome) pour garder la navbar ────────────────────
    private UserHomeController parentHomeController;

    // ── État ──────────────────────────────────────────────────────────────────
    private List<RecommandationService.RessourceScore> recommandations;
    private Ressources ressourceActuelle;
    private List<Ressources>      toutesRessources;
    private int                   pageActuelle = 0;
    private static final int      CARTES_PAR_PAGE = 3;
    private Set<Integer>          favorisIds;   // cache des IDs favoris de l'utilisateur

    // ═════════════════════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        recommandationService = new RecommandationService();
        ressourcesService     = new RessourcesService(MyDataBase.getInstance().getConx());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE — À appeler depuis le controller parent
    // ═════════════════════════════════════════════════════════════════════════

    public void setContexte(Ressources ressourceActuelle, List<Ressources> toutesRessources) {
        this.ressourceActuelle = ressourceActuelle;
        this.toutesRessources  = toutesRessources;
        calculerEtAfficher();
    }

    private void calculerEtAfficher() {
        // ✅ Correction : charger les favoris via FavoriService
        FavoriService favService = new FavoriService();
        favorisIds = favService.chargerFavoris();

        List<Ressources> favoris = toutesRessources.stream()
                .filter(r -> favorisIds.contains(r.getId_ressources()))
                .collect(Collectors.toList());

        boolean modeHybride = !favoris.isEmpty() || ressourceActuelle != null;

        if (modeHybride) {
            recommandations = recommandationService.recommander(
                    ressourceActuelle, favoris, toutesRessources);
            mettreAJourEnTete("Recommandé pour vous",
                    favoris.isEmpty()
                            ? "Basé sur votre navigation actuelle"
                            : "Basé sur vos " + favoris.size() + " favori(s)",
                    "🧠 Hybride", "#1B4332", "white");
        } else {
            recommandations = recommandationService.recommanderParPopularite(toutesRessources);
            mettreAJourEnTete("Tendances du moment",
                    "Les ressources les plus populaires de la communauté",
                    "🔥 Populaire", "#B45309", "white");
        }

        pageActuelle = 0;

        if (recommandations.isEmpty()) {
            afficherEtatVide();
        } else {
            if (emptyState != null) emptyState.setVisible(false);
            afficherPage(0);
            mettreAJourDots();
            mettreAJourBoutons();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PAGINATION
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    private void pageSuivante() {
        int totalPages = getTotalPages();
        if (pageActuelle < totalPages - 1) {
            pageActuelle++;
            afficherPage(pageActuelle);
            mettreAJourDots();
            mettreAJourBoutons();
        }
    }

    @FXML
    private void pagePrecedente() {
        if (pageActuelle > 0) {
            pageActuelle--;
            afficherPage(pageActuelle);
            mettreAJourDots();
            mettreAJourBoutons();
        }
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) recommandations.size() / CARTES_PAR_PAGE);
    }

    private void afficherPage(int page) {
        if (cardsContainer == null) return;
        cardsContainer.getChildren().clear();

        int debut = page * CARTES_PAR_PAGE;
        int fin   = Math.min(debut + CARTES_PAR_PAGE, recommandations.size());

        for (int i = debut; i < fin; i++) {
            RecommandationService.RessourceScore rs = recommandations.get(i);
            VBox carte = creerCarteRecommandation(rs);

            carte.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(300), carte);
            ft.setToValue(1.0);
            ft.setDelay(Duration.millis((i - debut) * 80L));
            ft.play();

            TranslateTransition tt = new TranslateTransition(Duration.millis(300), carte);
            tt.setFromY(20);
            tt.setToY(0);
            tt.setDelay(Duration.millis((i - debut) * 80L));
            tt.play();

            cardsContainer.getChildren().add(carte);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CARTE DE RECOMMANDATION
    // ═════════════════════════════════════════════════════════════════════════

    private VBox creerCarteRecommandation(RecommandationService.RessourceScore rs) {
        Ressources r = rs.getRessource();

        VBox carte = new VBox(0);
        carte.setPrefWidth(320);
        carte.setMaxWidth(320);
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,45,31,0.10), 14, 0, 0, 4);" +
                        "-fx-border-color: #E8F0EC; -fx-border-width: 1; -fx-border-radius: 16;" +
                        "-fx-cursor: hand;");

        carte.setOnMouseEntered(e -> carte.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.22), 22, 0, 0, 8);" +
                        "-fx-border-color: #2D6A4F; -fx-border-width: 1.5; -fx-border-radius: 16;" +
                        "-fx-cursor: hand; -fx-scale-x: 1.015; -fx-scale-y: 1.015;"));
        carte.setOnMouseExited(e -> carte.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(15,45,31,0.10), 14, 0, 0, 4);" +
                        "-fx-border-color: #E8F0EC; -fx-border-width: 1; -fx-border-radius: 16;" +
                        "-fx-cursor: hand;"));

        // ── Image ──────────────────────────────────────────────
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(160);
        imageContainer.setStyle("-fx-background-color: #E8F0EC; -fx-background-radius: 16 16 0 0;");

        if (r.getImageUrl() != null && !r.getImageUrl().isEmpty()) {
            try {
                File f = new File(r.getImageUrl());
                String uri = f.exists() ? f.toURI().toString() : r.getImageUrl();
                ImageView iv = new ImageView(new Image(uri, 320, 160, false, true));
                iv.setFitWidth(320); iv.setFitHeight(160); iv.setPreserveRatio(false);
                imageContainer.getChildren().add(iv);
            } catch (Exception ignored) {
                imageContainer.getChildren().add(placeholderLabel(r));
            }
        } else {
            imageContainer.getChildren().add(placeholderLabel(r));
        }

        // Badge type — ✅ Correction : r.getContenu() est un String
        if (r.getContenu() != null && !r.getContenu().isEmpty()) {
            Label badge = new Label(r.getContenu());
            badge.setStyle(
                    "-fx-background-color: " + couleurType(r.getContenu()) + ";" +
                            "-fx-text-fill: white; -fx-background-radius: 8;" +
                            "-fx-padding: 3 10; -fx-font-size: 10.5px; -fx-font-weight: bold;");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            StackPane.setMargin(badge, new javafx.geometry.Insets(10, 0, 0, 10));
            imageContainer.getChildren().add(badge);
        }

        // Badge MATCH %
        int matchPct = rs.getMatchPourcent();
        Label matchBadge = new Label(matchPct + "% match");
        String matchColor = matchPct >= 70 ? "#15803D" : matchPct >= 40 ? "#B45309" : "#6B7280";
        matchBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.92);" +
                        "-fx-text-fill: " + matchColor + ";" +
                        "-fx-background-radius: 20; -fx-padding: 3 9;" +
                        "-fx-font-size: 10px; -fx-font-weight: bold;");
        StackPane.setAlignment(matchBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(matchBadge, new javafx.geometry.Insets(10, 10, 0, 0));
        imageContainer.getChildren().add(matchBadge);

        carte.getChildren().add(imageContainer);

        // ── Corps ──────────────────────────────────────────────
        VBox corps = new VBox(8);
        corps.setStyle("-fx-padding: 14 16 12 16;");

        Label titre = new Label(r.getTitre() != null ? r.getTitre() : "Sans titre");
        titre.setWrapText(true);
        titre.setMaxWidth(288);
        titre.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0D2B1E;");

        Label resume = new Label(r.getResume() != null ? r.getResume() : "");
        resume.setWrapText(true); resume.setMaxWidth(288); resume.setMaxHeight(36);
        resume.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #7A8B7F;");

        Label raison = new Label("💡 " + rs.getRaison());
        raison.setStyle(
                "-fx-font-size: 10.5px; -fx-text-fill: #2D6A4F;" +
                        "-fx-background-color: #E8F5EE; -fx-background-radius: 6;" +
                        "-fx-padding: 3 8;");

        HBox stats = new HBox(12);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label vues = new Label("👁 " + r.getNbr_vues());
        vues.setStyle("-fx-font-size: 11px; -fx-text-fill: #9AA89F;");
        // ✅ Correction : test du favori via le cache favorisIds
        if (favorisIds != null && favorisIds.contains(r.getId_ressources())) {
            Label fav = new Label("★ Favori");
            fav.setStyle("-fx-font-size: 11px; -fx-text-fill: #F59E0B;");
            stats.getChildren().addAll(vues, fav);
        } else {
            stats.getChildren().add(vues);
        }

        HBox barreScore = creerBarreScore(rs);

        Button btnVoir = new Button("Voir la ressource →");
        btnVoir.setMaxWidth(Double.MAX_VALUE);
        btnVoir.setStyle(
                "-fx-background-color: #1B4332; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 9 0;" +
                        "-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-cursor: hand;");
        btnVoir.setOnMouseEntered(e -> btnVoir.setStyle(
                "-fx-background-color: #2D6A4F; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 9 0;" +
                        "-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-cursor: hand;"));
        btnVoir.setOnMouseExited(e -> btnVoir.setStyle(
                "-fx-background-color: #1B4332; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 9 0;" +
                        "-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-cursor: hand;"));
        btnVoir.setOnAction(e -> naviguerVersRessource(r));

        corps.getChildren().addAll(titre, resume, raison, stats, barreScore, btnVoir);
        carte.getChildren().add(corps);

        carte.setOnMouseClicked(e -> {
            if (e.getTarget() instanceof Button) return;
            naviguerVersRessource(r);
        });

        return carte;
    }

    private HBox creerBarreScore(RecommandationService.RessourceScore rs) {
        VBox container = new VBox(4);
        Label labelScore = new Label(String.format(
                "Score : %.0f%%  (contenu %.0f%%  •  popularité %.0f%%)",
                rs.getScore() * 100, rs.getScoreContent() * 100, rs.getScorePopularite() * 100));
        labelScore.setStyle("-fx-font-size: 9.5px; -fx-text-fill: #9AA89F;");

        HBox barre = new HBox(0);
        barre.setPrefHeight(6);
        barre.setMaxHeight(6);
        barre.setStyle("-fx-background-color: #F0F4F2; -fx-background-radius: 3;");
        barre.setPrefWidth(288);

        Region segContent = new Region();
        double largeurContent = Math.max(0, rs.getScoreContent() * 0.65 * 288);
        segContent.setPrefWidth(largeurContent);
        segContent.setStyle("-fx-background-color: #2D6A4F; -fx-background-radius: 3 0 0 3;");

        Region segPop = new Region();
        double largeurPop = Math.max(0, rs.getScorePopularite() * 0.35 * 288);
        segPop.setPrefWidth(largeurPop);
        segPop.setStyle("-fx-background-color: #F59E0B; -fx-background-radius: 0 3 3 0;");

        barre.getChildren().addAll(segContent, segPop);

        VBox result = new VBox(3);
        result.getChildren().addAll(labelScore, barre);

        HBox hWrapper = new HBox();
        hWrapper.getChildren().add(result);
        HBox.setHgrow(result, Priority.ALWAYS);
        return hWrapper;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UI HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private void mettreAJourEnTete(String titre, String sousTitre, String badge, String bgBadge, String fgBadge) {
        if (titreSection    != null) titreSection.setText(titre);
        if (sousTexteSection != null) sousTexteSection.setText(sousTitre);
        if (badgeMode != null) {
            badgeMode.setText(badge);
            badgeMode.setStyle(
                    "-fx-background-color: " + bgBadge + "; -fx-text-fill: " + fgBadge + ";" +
                            "-fx-background-radius: 20; -fx-padding: 4 12;" +
                            "-fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    private void mettreAJourDots() {
        if (dotsContainer == null) return;
        dotsContainer.getChildren().clear();
        int totalPages = getTotalPages();
        for (int i = 0; i < totalPages; i++) {
            Region dot = new Region();
            dot.setPrefSize(i == pageActuelle ? 20 : 8, 8);
            dot.setMinSize(i == pageActuelle ? 20 : 8, 8);
            dot.setStyle("-fx-background-color: " + (i == pageActuelle ? "#1B4332" : "#D1D5DB") + ";" +
                    "-fx-background-radius: 4;");
            dotsContainer.getChildren().add(dot);
        }
    }

    private void mettreAJourBoutons() {
        if (btnPrecedent != null) btnPrecedent.setDisable(pageActuelle == 0);
        if (btnSuivant   != null) btnSuivant.setDisable(pageActuelle >= getTotalPages() - 1);
    }

    private void afficherEtatVide() {
        if (cardsContainer != null) cardsContainer.getChildren().clear();
        if (emptyState != null) emptyState.setVisible(true);
    }

    private Label placeholderLabel(Ressources r) {
        String type = r.getContenu() != null ? r.getContenu() : "";
        String icone = switch (type.toUpperCase()) {
            case "VIDEO"   -> "🎬";
            case "PDF"     -> "📄";
            case "PODCAST" -> "🎧";
            default        -> "📚";
        };
        Label ph = new Label(icone);
        ph.setStyle("-fx-font-size: 32px; -fx-opacity: 0.5;");
        return ph;
    }

    private String couleurType(String type) {
        return switch (type.toUpperCase()) {
            case "VIDEO"   -> "#1B4332";
            case "PDF"     -> "#B91C1C";
            case "PODCAST" -> "#6D28D9";
            default        -> "#0369A1";
        };
    }

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    @FXML
    private void retourFront() {
        if (parentHomeController != null) {
            parentHomeController.loadInCenter("/FrontOffice_COMPLET.fxml");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOffice_COMPLET.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void naviguerVersRessource(Ressources r) {
        // Incrémenter les vues
        r.setNbr_vues(r.getNbr_vues() + 1);
        try {
            ressourcesService.modifier(r);
        } catch (Exception ignored) {}

        if (r.getContenu() == null) return;
        String type = r.getContenu().toUpperCase();
        String fxmlPath = switch (type) {
            case "VIDEO"   -> "/videoPlayer.fxml";
            case "PDF"     -> "/PDFViewer.fxml";
            case "PODCAST" -> null;
            default        -> null;
        };

        if (fxmlPath != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                if (loader.getLocation() == null) return;
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setTitle(r.getTitre());
                stage.setScene(new Scene(root));
                Object ctrl = loader.getController();
                if (ctrl instanceof PDFViewerController)
                    ((PDFViewerController) ctrl).setRessource(r, stage);
                else if (ctrl instanceof VideoPlayerController)
                    ((VideoPlayerController) ctrl).setRessource(r, stage);
                stage.show();
            } catch (Exception e) { e.printStackTrace(); }
        } else if (type.equals("PODCAST") && r.getUrl() != null) {
            try {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(r.getUrl()));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}