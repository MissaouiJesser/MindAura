package tn.esprit.controllers;

import tn.esprit.controllers.ArticleViewerController;
import tn.esprit.controllers.ModifierRessourceController;
import tn.esprit.controllers.PDFViewerController;
import tn.esprit.controllers.VideoPlayerController;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Ressources;
import tn.esprit.services.RessourcesService;
import tn.esprit.utils.MyDataBase;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class Afficher implements DashboardController.DashboardAware {

    // ─── FXML ─────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private VBox ressourcesContainer;
    @FXML private Label resultatsLabel;
    @FXML private Label nombreLabel;
    @FXML private Button btnTous;
    @FXML private Button btnLivres;
    @FXML private Button btnVideos;
    @FXML private Button btnPodcasts;
    @FXML private Button btnArticles;
    @FXML private ComboBox<String> comboTri;
    @FXML private Label lblVuesTotales;
    @FXML private Label lblDureeMoyenne;
    @FXML private Label meteoTemperature;
    @FXML private Label meteoCondition;
    @FXML private Label meteoLieu;
    @FXML private Label meteoHeure;

    private DashboardController dashboardController;
    private RessourcesService ressourceService;
    private List<Ressources> toutesRessources;
    private String filtreActif = "TOUS";

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    @FXML
    public void initialize() {
        ressourceService = new RessourcesService(MyDataBase.getInstance().getConx());
        if (comboTri != null) {
            comboTri.getItems().addAll("Plus récent", "Plus ancien", "Titre A-Z", "Plus vu");
            comboTri.setValue("Plus récent");
        }
        mettreAJourBoutonActif(btnTous);
        chargerRessources();
    }

    private void chargerRessources() {
        try {
            toutesRessources = ressourceService.afficherList();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        afficherRessources(toutesRessources);
    }

    private void afficherRessources(List<Ressources> liste) {
        if (ressourcesContainer == null) return;
        ressourcesContainer.getChildren().clear();

        if (resultatsLabel != null)
            resultatsLabel.setText(filtreActif.equals("TOUS") ? "Toutes les ressources" : filtreActif);
        if (nombreLabel != null)
            nombreLabel.setText(liste.size() + (liste.size() > 1 ? " résultats" : " résultat"));

        List<Ressources> podcasts = liste.stream()
                .filter(r -> r.getContenuEnum() != null && r.getContenuEnum().name().equalsIgnoreCase("PODCAST")).toList();
        List<Ressources> grille = liste.stream()
                .filter(r -> r.getContenuEnum() == null || !r.getContenuEnum().name().equalsIgnoreCase("PODCAST")).toList();

        int[] delay = {0};

        if (!podcasts.isEmpty()) {
            if (!grille.isEmpty()) {
                ressourcesContainer.getChildren().add(creerSectionHeader("🎧", "Podcasts", podcasts.size(), "#2563EB"));
            }
            VBox podcastsSection = new VBox(14);
            podcastsSection.setMaxWidth(Double.MAX_VALUE);
            podcastsSection.setStyle("-fx-padding: 0 0 28 0;");
            for (Ressources r : podcasts) {
                VBox card = creerCartePodcast(r);
                card.setOpacity(0);
                card.setTranslateY(18);
                podcastsSection.getChildren().add(card);
                animer(card, delay[0]++);
            }
            ressourcesContainer.getChildren().add(podcastsSection);
        }

        if (!grille.isEmpty()) {
            List<Ressources> videos   = grille.stream().filter(r -> r.getContenuEnum() != null && r.getContenuEnum().name().equalsIgnoreCase("VIDEO")).toList();
            List<Ressources> pdfs     = grille.stream().filter(r -> r.getContenuEnum() != null && r.getContenuEnum().name().equalsIgnoreCase("PDF")).toList();
            List<Ressources> articles = grille.stream().filter(r -> r.getContenuEnum() != null && r.getContenuEnum().name().equalsIgnoreCase("ARTICLE")).toList();
            List<Ressources> autres   = grille.stream().filter(r -> r.getContenuEnum() == null ||
                    (!r.getContenuEnum().name().equalsIgnoreCase("VIDEO") &&
                            !r.getContenuEnum().name().equalsIgnoreCase("PDF") &&
                            !r.getContenuEnum().name().equalsIgnoreCase("ARTICLE"))).toList();

            boolean multiTypes = (videos.isEmpty() ? 0 : 1) +
                    (pdfs.isEmpty()   ? 0 : 1) +
                    (articles.isEmpty()? 0 : 1) +
                    (autres.isEmpty() ? 0 : 1) > 1;

            if (!videos.isEmpty()) {
                if (multiTypes || !podcasts.isEmpty())
                    ressourcesContainer.getChildren().add(creerSectionHeader("🎬", "Vidéos", videos.size(), "#047857"));
                ressourcesContainer.getChildren().add(creerGrille(videos, delay));
            }
            if (!pdfs.isEmpty()) {
                if (multiTypes || !podcasts.isEmpty())
                    ressourcesContainer.getChildren().add(creerSectionHeader("📄", "PDF", pdfs.size(), "#B91C1C"));
                ressourcesContainer.getChildren().add(creerGrille(pdfs, delay));
            }
            if (!articles.isEmpty()) {
                if (multiTypes || !podcasts.isEmpty())
                    ressourcesContainer.getChildren().add(creerSectionHeader("📰", "Articles", articles.size(), "#0369A1"));
                ressourcesContainer.getChildren().add(creerGrille(articles, delay));
            }
            if (!autres.isEmpty()) {
                ressourcesContainer.getChildren().add(creerGrille(autres, delay));
            }
        }
    }

    private Label creerSectionHeader(String emoji, String titre, int count, String couleur) {
        Label label = new Label(emoji + "  " + titre + "   (" + count + ")");
        label.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold;" +
                        "-fx-text-fill: " + couleur + ";" +
                        "-fx-padding: 10 0 10 4;" +
                        "-fx-border-color: transparent transparent " + couleur + "22 transparent;" +
                        "-fx-border-width: 0 0 2 0;");
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private FlowPane creerGrille(List<Ressources> items, int[] delay) {
        FlowPane grille = new FlowPane();
        grille.setHgap(22);
        grille.setVgap(22);
        grille.setPrefWrapLength(1300);
        grille.setMaxWidth(Double.MAX_VALUE);
        grille.setStyle("-fx-padding: 0 0 28 0;");

        for (Ressources r : items) {
            VBox card = creerCarte(r);
            card.setOpacity(0);
            card.setTranslateY(18);
            grille.getChildren().add(card);
            animer(card, delay[0]++);
        }
        return grille;
    }

    private void animer(javafx.scene.Node node, int index) {
        FadeTransition fade = new FadeTransition(Duration.millis(340), node);
        fade.setFromValue(0); fade.setToValue(1);
        fade.setDelay(Duration.millis(index * 55L));

        TranslateTransition slide = new TranslateTransition(Duration.millis(340), node);
        slide.setFromY(18); slide.setToY(0);
        slide.setDelay(Duration.millis(index * 55L));

        new ParallelTransition(fade, slide).play();
    }

    private VBox creerEmptyState() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(1200);
        box.setPrefHeight(350);
        box.setStyle(
                "-fx-background-color: white; -fx-background-radius: 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4);" +
                        "-fx-padding: 60;");

        Label emoji = new Label("📭");
        emoji.setStyle("-fx-font-size: 52px;");

        Label msg = new Label("Aucune ressource trouvée");
        msg.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0D2B1E;");

        Label sub = new Label("Essayez un autre filtre ou ajoutez une nouvelle ressource");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #9AA89F;");

        box.getChildren().addAll(emoji, msg, sub);
        return box;
    }

    private VBox creerCarte(Ressources ressource) {
        String type = ressource.getContenuEnum() != null
                ? ressource.getContenuEnum().name().toUpperCase() : "";
        if (type.equals("PODCAST")) return creerCartePodcast(ressource);
        if (type.equals("ARTICLE")) return creerCarteArticle(ressource);
        return creerCarteStandard(ressource);
    }

    private VBox creerCarteStandard(Ressources ressource) {
        String type = ressource.getContenuEnum() != null ? ressource.getContenuEnum().name() : "?";

        VBox card = new VBox(0);
        card.setPrefWidth(295);
        card.setMaxWidth(295);
        card.setMinWidth(295);
        card.setStyle(carteStyleNormal());

        card.setOnMouseEntered(e -> {
            card.setStyle(carteStyleHover());
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.025); st.setToY(1.025); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(carteStyleNormal());
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        StackPane imageZone = new StackPane();
        imageZone.setPrefHeight(180);
        imageZone.setMinHeight(180);
        imageZone.setMaxHeight(180);
        imageZone.setStyle(
                "-fx-background-radius: 16 16 0 0;" +
                        "-fx-background-color: " + getCouleurFond(type) + ";");

        Rectangle clip = new Rectangle(295, 180);
        clip.setArcWidth(32); clip.setArcHeight(32);
        imageZone.setClip(clip);

        boolean aImage = chargerImage(imageZone, ressource.getImageUrl(), 295, 180);

        if (!aImage) {
            Label icone = new Label(getIconeType(type));
            icone.setStyle("-fx-font-size: 52px; -fx-opacity: 0.30;");
            imageZone.getChildren().add(icone);
            ajouterCerclesDecor(imageZone);
        } else {
            Region overlay = new Region();
            overlay.setPrefSize(295, 180);
            overlay.setStyle("-fx-background-color: linear-gradient(to bottom, " +
                    "transparent 35%, rgba(0,0,0,0.50) 100%);");
            imageZone.getChildren().add(overlay);
        }

        Label badge = new Label(getIconeType(type) + "  " + type);
        badge.setStyle(getBadgeStyle(type));
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12, 0, 0, 12));
        imageZone.getChildren().add(badge);

        if (ressource.getNiveauEnum() != null) {
            Label niveauBadge = new Label(niveauLabel(ressource.getNiveauEnum().name()));
            niveauBadge.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.22); -fx-text-fill: white;" +
                            "-fx-background-radius: 10; -fx-padding: 4 10;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-border-color: rgba(255,255,255,0.30); -fx-border-width: 1; -fx-border-radius: 10;");
            StackPane.setAlignment(niveauBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(niveauBadge, new Insets(12, 12, 0, 0));
            imageZone.getChildren().add(niveauBadge);
        }

        VBox textZone = new VBox(9);
        textZone.setStyle("-fx-padding: 16 18 15 18; -fx-background-color: white;" +
                "-fx-background-radius: 0 0 16 16;");
        VBox.setVgrow(textZone, Priority.ALWAYS);

        Label titre = new Label(ressource.getTitre() != null ? ressource.getTitre() : "Sans titre");
        titre.setWrapText(true);
        titre.setMaxHeight(48);
        titre.setStyle("-fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #0D2B1E;" +
                "-fx-line-spacing: 2;");

        Label resume = new Label(ressource.getResume() != null ? ressource.getResume() : "");
        resume.setWrapText(true);
        resume.setMaxHeight(44);
        resume.setStyle("-fx-font-size: 12px; -fx-text-fill: #7A8B7F; -fx-line-spacing: 2;");

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #F0F4F2;");

        HBox footer = new HBox(0);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 8 0 0 0;");

        VBox stats = new VBox(3);
        HBox vuesBox = creerStatBox("👁", formatNombre(ressource.getNbr_vues()) + " vues");
        String dureeStr = ressource.getDuree_lecture() > 0 ? ressource.getDuree_lecture() + " min" : "—";
        HBox dureeBox = creerStatBox("⏱", dureeStr);
        stats.getChildren().addAll(vuesBox, dureeBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox btnBox = new VBox(5);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnVoir = creerBoutonPrimaire("Voir →", "#1B4332", "#2D6A4F");
        btnVoir.setOnAction(e -> ouvrirDetail(ressource));

        HBox secondaires = new HBox(6);
        secondaires.setAlignment(Pos.CENTER_RIGHT);
        Button btnMod = creerBoutonSecondaire("✏", "#EAF4EE", "#2D6A4F");
        btnMod.setOnAction(e -> ouvrirModification(ressource));
        Button btnSup = creerBoutonSecondaire("🗑", "#FEF2F2", "#DC2626");
        btnSup.setOnAction(e -> supprimerRessource(ressource));
        secondaires.getChildren().addAll(btnMod, btnSup);

        btnBox.getChildren().addAll(btnVoir, secondaires);
        footer.getChildren().addAll(stats, spacer, btnBox);

        textZone.getChildren().addAll(titre, resume, sep, footer);
        card.getChildren().addAll(imageZone, textZone);

        card.setOnMouseClicked(e -> {
            javafx.scene.Node source = (javafx.scene.Node) e.getTarget();
            while (source != null) {
                if (source instanceof Button) return;
                source = source.getParent();
            }
            ouvrirDetail(ressource);
        });

        return card;
    }

    private VBox creerCarteArticle(Ressources ressource) {
        VBox card = new VBox(0);
        card.setPrefWidth(295);
        card.setMaxWidth(295);
        card.setMinWidth(295);
        card.setStyle(carteStyleArticle());

        card.setOnMouseEntered(e -> {
            card.setStyle(carteStyleArticleHover());
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.025); st.setToY(1.025); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(carteStyleArticle());
            ScaleTransition st = new ScaleTransition(Duration.millis(180), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        StackPane imageZone = new StackPane();
        imageZone.setPrefHeight(180);
        imageZone.setMinHeight(180);
        imageZone.setStyle(
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #0C4A6E 0%, #0369A1 60%, #0EA5E9 100%);" +
                        "-fx-background-radius: 16 16 0 0;");
        Rectangle clip = new Rectangle(295, 180);
        clip.setArcWidth(32); clip.setArcHeight(32);
        imageZone.setClip(clip);

        boolean aImage = chargerImage(imageZone, ressource.getImageUrl(), 295, 180);
        if (!aImage) {
            Label icone = new Label("📰");
            icone.setStyle("-fx-font-size: 52px; -fx-opacity: 0.30;");
            imageZone.getChildren().add(icone);
            ajouterCerclesDecor(imageZone);
        } else {
            Region overlay = new Region();
            overlay.setPrefSize(295, 180);
            overlay.setStyle("-fx-background-color: linear-gradient(to bottom, " +
                    "transparent 35%, rgba(3,69,161,0.55) 100%);");
            imageZone.getChildren().add(overlay);
        }

        Label badge = new Label("📰  Article");
        badge.setStyle(
                "-fx-background-color: rgba(3,105,161,0.92); -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 5 11;" +
                        "-fx-font-size: 10.5px; -fx-font-weight: bold;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 4, 0, 0, 1);");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12, 0, 0, 12));
        imageZone.getChildren().add(badge);

        if (ressource.getNiveauEnum() != null) {
            Label niveauBadge = new Label(niveauLabel(ressource.getNiveauEnum().name()));
            niveauBadge.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: white;" +
                            "-fx-background-radius: 10; -fx-padding: 4 10;" +
                            "-fx-font-size: 10px; -fx-font-weight: bold;" +
                            "-fx-border-color: rgba(255,255,255,0.30); -fx-border-width: 1; -fx-border-radius: 10;");
            StackPane.setAlignment(niveauBadge, Pos.TOP_RIGHT);
            StackPane.setMargin(niveauBadge, new Insets(12, 12, 0, 0));
            imageZone.getChildren().add(niveauBadge);
        }

        VBox textZone = new VBox(9);
        textZone.setStyle("-fx-padding: 16 18 15 18; -fx-background-color: white;" +
                "-fx-background-radius: 0 0 16 16;");
        VBox.setVgrow(textZone, Priority.ALWAYS);

        Label titre = new Label(ressource.getTitre() != null ? ressource.getTitre() : "Sans titre");
        titre.setWrapText(true);
        titre.setMaxHeight(48);
        titre.setStyle("-fx-font-size: 14.5px; -fx-font-weight: bold; -fx-text-fill: #0D2B1E;");

        Label resume = new Label(ressource.getResume() != null ? ressource.getResume() : "");
        resume.setWrapText(true);
        resume.setMaxHeight(44);
        resume.setStyle("-fx-font-size: 12px; -fx-text-fill: #7A8B7F;");

        if (ressource.getUrl() != null && !ressource.getUrl().isEmpty()) {
            String hote = extraireHote(ressource.getUrl());
            Label urlLabel = new Label("🔗 " + hote);
            urlLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #0369A1;" +
                    "-fx-font-style: italic;");
            urlLabel.setMaxWidth(259);
            textZone.getChildren().addAll(titre, resume, urlLabel);
        } else {
            textZone.getChildren().addAll(titre, resume);
        }

        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #EFF6FF;");

        HBox footer = new HBox(0);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setStyle("-fx-padding: 8 0 0 0;");

        VBox stats = new VBox(3);
        HBox vuesBox = creerStatBox("👁", formatNombre(ressource.getNbr_vues()) + " vues");
        String dureeStr = ressource.getDuree_lecture() > 0 ? ressource.getDuree_lecture() + " min lect." : "—";
        HBox dureeBox = creerStatBox("⏱", dureeStr);
        stats.getChildren().addAll(vuesBox, dureeBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox btnBox = new VBox(5);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnLire = creerBoutonPrimaire("Lire →", "#0369A1", "#025A8E");
        btnLire.setOnAction(e -> ouvrirArticleViewer(ressource));

        HBox secondaires = new HBox(6);
        secondaires.setAlignment(Pos.CENTER_RIGHT);
        Button btnMod = creerBoutonSecondaire("✏", "#EFF6FF", "#0369A1");
        btnMod.setOnAction(e -> ouvrirModification(ressource));
        Button btnSup = creerBoutonSecondaire("🗑", "#FEF2F2", "#DC2626");
        btnSup.setOnAction(e -> supprimerRessource(ressource));
        secondaires.getChildren().addAll(btnMod, btnSup);

        btnBox.getChildren().addAll(btnLire, secondaires);
        footer.getChildren().addAll(stats, spacer, btnBox);

        textZone.getChildren().addAll(sep, footer);
        card.getChildren().addAll(imageZone, textZone);

        card.setOnMouseClicked(e -> {
            javafx.scene.Node source = (javafx.scene.Node) e.getTarget();
            while (source != null) {
                if (source instanceof Button) return;
                source = source.getParent();
            }
            ouvrirArticleViewer(ressource);
        });

        return card;
    }

    private VBox creerCartePodcast(Ressources ressource) {
        VBox wrapper = new VBox();
        wrapper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wrapper, Priority.ALWAYS);

        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMinHeight(108);
        card.setPrefHeight(115);
        card.setMaxWidth(Double.MAX_VALUE);

        String styleBase =
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #1E3A8A 0%, #2563EB 55%, #3B82F6 100%);"  +
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 18 28 18 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(37,99,235,0.38), 18, 0, 0, 6);" +
                        "-fx-cursor: hand;";
        String styleHover =
                "-fx-background-color: linear-gradient(from 0% 0% to 100% 0%, #1E40AF 0%, #1D4ED8 55%, #2563EB 100%);"+
                        "-fx-background-radius: 18;" +
                        "-fx-padding: 18 28 18 20;" +
                        "-fx-effect: dropshadow(gaussian, rgba(29,78,216,0.55), 24, 0, 0, 10);" +
                        "-fx-cursor: hand;";

        card.setStyle(styleBase);
        card.setOnMouseEntered(e -> card.setStyle(styleHover));
        card.setOnMouseExited(e  -> card.setStyle(styleBase));

        StackPane thumb = new StackPane();
        thumb.setPrefSize(78, 78);
        thumb.setMinSize(78, 78);
        thumb.setMaxSize(78, 78);
        thumb.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 14;");

        boolean aImg = chargerImage(thumb, ressource.getImageUrl(), 78, 78);
        if (!aImg) {
            Label ic = new Label("🎧");
            ic.setStyle("-fx-font-size: 30px;");
            thumb.getChildren().add(ic);
        }

        StackPane playBtn = new StackPane();
        playBtn.setPrefSize(48, 48);
        playBtn.setMinSize(48, 48);
        playBtn.setMaxSize(48, 48);
        playBtn.setStyle(
                "-fx-background-color: white; -fx-background-radius: 50%;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 10, 0, 0, 2);" +
                        "-fx-cursor: hand;");
        Label playIc = new Label("▶");
        playIc.setStyle("-fx-font-size: 15px; -fx-text-fill: #2563EB;" +
                "-fx-padding: 0 0 0 3;");
        playBtn.getChildren().add(playIc);
        playBtn.setOnMouseClicked(e -> ouvrirDetail(ressource));

        playBtn.setOnMouseEntered(e -> playBtn.setStyle(
                "-fx-background-color: #EEF2FF; -fx-background-radius: 50%;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.28), 12, 0, 0, 3);" +
                        "-fx-cursor: hand;"));
        playBtn.setOnMouseExited(e -> playBtn.setStyle(
                "-fx-background-color: white; -fx-background-radius: 50%;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 10, 0, 0, 2);" +
                        "-fx-cursor: hand;"));

        HBox waveform = creerFormeOnde();

        Region sep = new Region();
        sep.setPrefSize(1.5, 55);
        sep.setMinSize(1.5, 55);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.22);");

        VBox infos = new VBox(6);
        infos.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infos, Priority.ALWAYS);

        HBox headerInfos = new HBox(10);
        headerInfos.setAlignment(Pos.CENTER_LEFT);

        Label titre = new Label(ressource.getTitre() != null ? ressource.getTitre() : "Sans titre");
        titre.setStyle("-fx-font-size: 15.5px; -fx-font-weight: bold; -fx-text-fill: white;");
        titre.setWrapText(true);

        Label podBadge = new Label("🎧  Podcast");
        podBadge.setStyle(
                "-fx-background-color: rgba(255,255,255,0.18); -fx-text-fill: white;" +
                        "-fx-background-radius: 20; -fx-padding: 3 11;" +
                        "-fx-font-size: 10.5px; -fx-font-weight: bold;" +
                        "-fx-border-color: rgba(255,255,255,0.28); -fx-border-width: 1; -fx-border-radius: 20;");

        headerInfos.getChildren().addAll(titre, podBadge);

        Label resume = new Label(ressource.getResume() != null ? ressource.getResume() : "");
        resume.setWrapText(true);
        resume.setMaxHeight(38);
        resume.setStyle("-fx-font-size: 12.5px; -fx-text-fill: rgba(255,255,255,0.78);");

        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        Label vues = new Label("👁  " + formatNombre(ressource.getNbr_vues()));
        vues.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.65);");
        if (ressource.getNiveauEnum() != null) {
            Label niv = new Label(niveauLabel(ressource.getNiveauEnum().name()));
            niv.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.16); -fx-text-fill: white;" +
                            "-fx-background-radius: 8; -fx-padding: 2 8; -fx-font-size: 10px;");
            statsBox.getChildren().addAll(vues, niv);
        } else {
            statsBox.getChildren().add(vues);
        }

        infos.getChildren().addAll(headerInfos, resume, statsBox);

        VBox actions = new VBox(8);
        actions.setAlignment(Pos.CENTER);

        Button btnMod = new Button("✏");
        btnMod.setStyle(
                "-fx-background-color: rgba(255,255,255,0.14); -fx-text-fill: white;" +
                        "-fx-background-radius: 8; -fx-padding: 7 12; -fx-font-size: 13px;" +
                        "-fx-cursor: hand; -fx-border-color: rgba(255,255,255,0.22);" +
                        "-fx-border-width: 1; -fx-border-radius: 8;");
        btnMod.setOnAction(e -> ouvrirModification(ressource));

        Button btnSup = new Button("🗑");
        btnSup.setStyle(
                "-fx-background-color: rgba(239,68,68,0.22); -fx-text-fill: #FCA5A5;" +
                        "-fx-background-radius: 8; -fx-padding: 7 12; -fx-font-size: 13px;" +
                        "-fx-cursor: hand; -fx-border-color: rgba(239,68,68,0.30);" +
                        "-fx-border-width: 1; -fx-border-radius: 8;");
        btnSup.setOnAction(e -> supprimerRessource(ressource));

        actions.getChildren().addAll(btnMod, btnSup);

        card.getChildren().addAll(thumb, playBtn, waveform, sep, infos, actions);

        card.setOnMouseClicked(e -> {
            javafx.scene.Node source = (javafx.scene.Node) e.getTarget();
            while (source != null) {
                if (source instanceof Button) return;
                source = source.getParent();
            }
            ouvrirDetail(ressource);
        });

        wrapper.getChildren().add(card);
        return wrapper;
    }

    private HBox creerFormeOnde() {
        HBox waveform = new HBox(3.5);
        waveform.setAlignment(Pos.CENTER);
        waveform.setPrefWidth(170);
        waveform.setMinWidth(150);

        int[] heights = {14,22,36,48,32,52,40,58,44,56,50,62,54,46,58,50,42,36,48,40,32,44,36,28,38,32,26,36,22,30};
        for (int i = 0; i < heights.length; i++) {
            Rectangle bar = new Rectangle(5, heights[i]);
            bar.setArcWidth(3); bar.setArcHeight(3);
            double opacity = 0.55 + (i % 3) * 0.15;
            bar.setFill(javafx.scene.paint.Color.web("rgba(255,255,255," + opacity + ")"));
            waveform.getChildren().add(bar);
        }
        return waveform;
    }

    private String carteStyleNormal() {
        return "-fx-background-color: white; -fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(13,43,30,0.08), 14, 0, 0, 4);" +
                "-fx-border-color: #EBF0EC; -fx-border-width: 1; -fx-border-radius: 16;" +
                "-fx-cursor: hand;";
    }

    private String carteStyleHover() {
        return "-fx-background-color: white; -fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.20), 22, 0, 0, 9);" +
                "-fx-border-color: #52B788; -fx-border-width: 1.5; -fx-border-radius: 16;" +
                "-fx-cursor: hand;";
    }

    private String carteStyleArticle() {
        return "-fx-background-color: white; -fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(3,105,161,0.08), 14, 0, 0, 4);" +
                "-fx-border-color: #DBEAFE; -fx-border-width: 1; -fx-border-radius: 16;" +
                "-fx-cursor: hand;";
    }

    private String carteStyleArticleHover() {
        return "-fx-background-color: white; -fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian, rgba(3,105,161,0.22), 22, 0, 0, 9);" +
                "-fx-border-color: #0369A1; -fx-border-width: 1.5; -fx-border-radius: 16;" +
                "-fx-cursor: hand;";
    }

    private boolean chargerImage(StackPane container, String imageUrl, double w, double h) {
        if (imageUrl == null || imageUrl.isEmpty()) return false;
        try {
            String uri;
            if (imageUrl.startsWith("/uploads")) {
                // Image uploadée sur Symfony → construire l'URL complète
                uri = "http://localhost:8000" + imageUrl;
            } else if (imageUrl.startsWith("http")) {
                // URL externe (Pexels, etc.)
                uri = imageUrl;
            } else {
                // Fichier local (ancien comportement)
                File imgFile = new File(imageUrl);
                uri = imgFile.exists() ? imgFile.toURI().toString() : imageUrl;
            }
            ImageView iv = new ImageView(new Image(uri, w, h, false, true));
            iv.setFitWidth(w); iv.setFitHeight(h);
            iv.setPreserveRatio(false);
            Rectangle clip = new Rectangle(w, h);
            clip.setArcWidth(32); clip.setArcHeight(32);
            iv.setClip(clip);
            container.getChildren().add(iv);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void ajouterCerclesDecor(StackPane zone) {
        int[] rayons = {28, 50, 75};
        Pos[] positions = {Pos.BOTTOM_RIGHT, Pos.TOP_LEFT, Pos.BOTTOM_LEFT};
        for (int i = 0; i < rayons.length; i++) {
            Circle c = new Circle(rayons[i]);
            c.setFill(javafx.scene.paint.Color.web("rgba(255,255,255,0.07)"));
            StackPane.setAlignment(c, positions[i]);
            zone.getChildren().add(0, c);
        }
    }

    private HBox creerStatBox(String emoji, String texte) {
        HBox box = new HBox(5);
        box.setAlignment(Pos.CENTER_LEFT);
        Label em = new Label(emoji);
        em.setStyle("-fx-font-size: 11px;");
        Label txt = new Label(texte);
        txt.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #9AA89F;");
        box.getChildren().addAll(em, txt);
        return box;
    }

    private Button creerBoutonPrimaire(String texte, String bg, String bgHover) {
        Button btn = new Button(texte);
        String base = "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
                "-fx-background-radius: 20; -fx-padding: 7 16;" +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;";
        String hover = "-fx-background-color: " + bgHover + "; -fx-text-fill: white;" +
                "-fx-background-radius: 20; -fx-padding: 7 16;" +
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e  -> btn.setStyle(base));
        return btn;
    }

    private Button creerBoutonSecondaire(String texte, String bg, String fg) {
        Button btn = new Button(texte);
        String base = "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 8; -fx-padding: 5 10;" +
                "-fx-font-size: 12px; -fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + darken(bg) + "; -fx-text-fill: " + fg + ";" +
                        "-fx-background-radius: 8; -fx-padding: 5 10; -fx-font-size: 12px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private String darken(String hex) {
        return switch (hex) {
            case "#EAF4EE" -> "#D0EBD9";
            case "#FEF2F2" -> "#FEE2E2";
            case "#EFF6FF" -> "#DBEAFE";
            default -> hex;
        };
    }

    private String getCouleurFond(String type) {
        return switch (type.toUpperCase()) {
            case "VIDEO"   -> "linear-gradient(from 0% 0% to 100% 100%, #064E3B 0%, #065F46 50%, #047857 100%)";
            case "PDF"     -> "linear-gradient(from 0% 0% to 100% 100%, #7F1D1D 0%, #991B1B 50%, #B91C1C 100%)";
            case "PODCAST" -> "linear-gradient(from 0% 0% to 100% 100%, #1E3A8A 0%, #1D4ED8 50%, #2563EB 100%)";
            default        -> "linear-gradient(from 0% 0% to 100% 100%, #374151 0%, #4B5563 100%)";
        };
    }

    private String getIconeType(String type) {
        return switch (type.toUpperCase()) {
            case "VIDEO"   -> "🎬";
            case "PDF"     -> "📄";
            case "PODCAST" -> "🎙";
            case "ARTICLE" -> "📰";
            default        -> "📁";
        };
    }

    private String getBadgeStyle(String type) {
        String bg = switch (type.toUpperCase()) {
            case "VIDEO"   -> "rgba(4,120,87,0.92)";
            case "PDF"     -> "rgba(185,28,28,0.92)";
            default        -> "rgba(55,65,81,0.92)";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: white;" +
                "-fx-background-radius: 10; -fx-padding: 5 11;" +
                "-fx-font-size: 10.5px; -fx-font-weight: bold;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 4, 0, 0, 1);";
    }

    private String extraireHote(String url) {
        try { return new java.net.URL(url).getHost(); }
        catch (Exception e) { return url; }
    }

    private void ouvrirArticleViewer(Ressources ressource) {
        if (ressource.getUrl() == null || ressource.getUrl().trim().isEmpty()) {
            afficherErreur("Cet article n'a pas d'URL associée.");
            return;
        }

        String url = ressource.getUrl().trim().toLowerCase();
        if (url.endsWith(".pdf")) {
            String[] ch = {"PDFViewer.fxml", "pdfViewer.fxml"};
            for (String fxmlFile : ch) {
                if (getClass().getResource("/" + fxmlFile) == null) continue;
                try {
                    if (dashboardController != null) {
                        Object ctrlObj = dashboardController.loadViewAndGet(fxmlFile,
                                "PDF - " + (ressource.getTitre() != null ? ressource.getTitre() : "Ressource"),
                                "Gestion de Rania");
                        if (ctrlObj instanceof PDFViewerController ctrl) {
                            ctrl.setRessource(ressource, dashboardController.getStage());
                            ctrl.setPageRetour("/AfficherRe.fxml");
                        }
                    } else {
                        java.net.URL u = getClass().getResource("/" + fxmlFile);
                        if (u == null) continue;
                        FXMLLoader l = new FXMLLoader(u);
                        Parent root = l.load();
                        PDFViewerController ctrl = l.getController();
                        Stage stage = (Stage) ressourcesContainer.getScene().getWindow();
                        ctrl.setRessource(ressource, stage);
                        ctrl.setPageRetour("/AfficherRe.fxml");
                        boolean wasMax = stage.isMaximized();
                        if (wasMax) stage.setMaximized(false);
                        stage.setScene(new Scene(root));
                        if (wasMax) stage.setMaximized(true);
                        stage.show();
                    }
                    return;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            ouvrirFichierSysteme(ressource.getUrl(), "PDF");
            return;
        }

        String[] chemins = {"/ArticleViewer.fxml", "/articleViewer.fxml", "/fxml/ArticleViewer.fxml"};
        java.net.URL fxmlUrl = null;
        for (String c : chemins) {
            fxmlUrl = getClass().getResource(c);
            if (fxmlUrl != null) break;
        }
        if (fxmlUrl == null) {
            ouvrirUrlNavigateur(ressource.getUrl(), "article");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            ArticleViewerController ctrl = loader.getController();

            if (dashboardController != null) {
                Stage popup = new Stage();
                ctrl.setRessource(ressource, popup);
                popup.setScene(new Scene(root, 1280, 820));
                popup.setTitle("📰 " + (ressource.getTitre() != null ? ressource.getTitre() : "Article"));
                popup.initOwner(dashboardController.getStage());
                popup.show();
            } else {
                Stage stage = (Stage) ressourcesContainer.getScene().getWindow();
                ctrl.setPageRetour("/AfficherRe.fxml");
                ctrl.setRessource(ressource, stage);
                boolean wasMax = stage.isMaximized();
                if (wasMax) stage.setMaximized(false);
                stage.setScene(new Scene(root, 1280, 820));
                if (wasMax) stage.setMaximized(true);
                stage.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            ouvrirUrlNavigateur(ressource.getUrl(), "article");
        }
    }

    private void ouvrirDetail(Ressources ressource) {
        if (ressource.getContenuEnum() == null) {
            afficherErreur("Type de contenu inconnu.");
            return;
        }
        String type = ressource.getContenuEnum().name().toUpperCase();
        String urlStr = ressource.getUrl();
        switch (type) {
            case "PODCAST" -> ouvrirUrlNavigateur(urlStr, "podcast");
            case "ARTICLE" -> ouvrirArticleViewer(ressource);
            case "PDF" -> {
                String[] ch = {"PDFViewer.fxml", "pdfViewer.fxml"};
                for (String fxmlFile : ch) {
                    if (getClass().getResource("/" + fxmlFile) == null) continue;
                    try {
                        if (dashboardController != null) {
                            Object ctrlObj = dashboardController.loadViewAndGet(fxmlFile,
                                    "PDF - " + (ressource.getTitre() != null ? ressource.getTitre() : "Ressource"),
                                    "Gestion de Rania");
                            if (ctrlObj instanceof PDFViewerController ctrl) {
                                ctrl.setRessource(ressource, dashboardController.getStage());
                                ctrl.setPageRetour("/AfficherRe.fxml");
                            }
                        } else {
                            java.net.URL u = getClass().getResource("/" + fxmlFile);
                            if (u == null) continue;
                            FXMLLoader l = new FXMLLoader(u);
                            Parent root = l.load();
                            PDFViewerController ctrl = l.getController();
                            Stage stage = (Stage) ressourcesContainer.getScene().getWindow();
                            ctrl.setRessource(ressource, stage);
                            ctrl.setPageRetour("/AfficherRe.fxml");
                            boolean wasMax = stage.isMaximized();
                            if (wasMax) stage.setMaximized(false);
                            stage.setScene(new Scene(root));
                            if (wasMax) stage.setMaximized(true);
                            stage.show();
                        }
                        return;
                    } catch (IOException ex) { ex.printStackTrace(); }
                }
                ouvrirFichierSysteme(urlStr, "PDF");
            }
            case "VIDEO" -> {
                String[] ch = {"VideoPlayer.fxml", "videoPlayer.fxml"};
                for (String fxmlFile : ch) {
                    if (getClass().getResource("/" + fxmlFile) == null) continue;
                    try {
                        if (dashboardController != null) {
                            Object ctrlObj = dashboardController.loadViewAndGet(fxmlFile,
                                    "Vidéo - " + (ressource.getTitre() != null ? ressource.getTitre() : "Ressource"),
                                    "Gestion de Rania");
                            if (ctrlObj instanceof VideoPlayerController ctrl) {
                                ctrl.setRessource(ressource, dashboardController.getStage());
                                ctrl.setPageRetour("/AfficherRe.fxml");
                            }
                        } else {
                            java.net.URL u = getClass().getResource("/" + fxmlFile);
                            if (u == null) continue;
                            FXMLLoader l = new FXMLLoader(u);
                            Parent root = l.load();
                            VideoPlayerController ctrl = l.getController();
                            Stage stage = (Stage) ressourcesContainer.getScene().getWindow();
                            ctrl.setRessource(ressource, stage);
                            ctrl.setPageRetour("/AfficherRe.fxml");
                            boolean wasMax = stage.isMaximized();
                            if (wasMax) stage.setMaximized(false);
                            stage.setScene(new Scene(root));
                            if (wasMax) stage.setMaximized(true);
                            stage.show();
                        }
                        return;
                    } catch (IOException ex) { ex.printStackTrace(); }
                }
                ouvrirFichierSysteme(urlStr, "Vidéo");
            }
            default -> afficherErreur("Type non pris en charge : " + type);
        }
    }

    private void ouvrirUrlNavigateur(String urlStr, String typeLabel) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            afficherErreur("Ce " + typeLabel + " n'a pas d'URL.");
            return;
        }
        urlStr = urlStr.trim();
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://"))
            urlStr = "https://" + urlStr;
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(urlStr));
                return;
            }
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb = os.contains("win")
                    ? new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", urlStr)
                    : os.contains("mac") ? new ProcessBuilder("open", urlStr)
                    : new ProcessBuilder("xdg-open", urlStr);
            pb.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Lien " + typeLabel);
            alert.setHeaderText("Copiez ce lien dans votre navigateur :");
            TextArea ta = new TextArea(urlStr);
            ta.setEditable(false); ta.setWrapText(true); ta.setPrefRowCount(2);
            alert.getDialogPane().setContent(ta);
            alert.getDialogPane().setMinWidth(480);
            alert.showAndWait();
        }
    }

    private void ouvrirFichierSysteme(String urlStr, String typeLabel) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            afficherErreur("Aucun fichier " + typeLabel + " associé.");
            return;
        }
        try {
            File f = new File(urlStr.trim());
            if (f.exists()) Desktop.getDesktop().open(f);
            else Desktop.getDesktop().browse(new URI(urlStr.trim()));
        } catch (Exception ex) {
            ex.printStackTrace();
            afficherErreur("Impossible d'ouvrir le fichier : " + ex.getMessage());
        }
    }

    private void supprimerRessource(Ressources ressource) {
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.setTitle("Confirmation");
        conf.setHeaderText("Supprimer \"" + ressource.getTitre() + "\" ?");
        conf.setContentText("Cette action est définitive et irréversible.");
        ButtonType btnOui = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNon = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        conf.getButtonTypes().setAll(btnOui, btnNon);
        Optional<ButtonType> result = conf.showAndWait();
        if (result.isPresent() && result.get() == btnOui) {
            try {
                ressourceService.delete(ressource);
                toutesRessources.remove(ressource);
                appliquerFiltre(filtreActif);
                afficherToast("Ressource supprimée avec succès ✓");
            } catch (Exception e) {
                e.printStackTrace();
                afficherErreur("Impossible de supprimer : " + e.getMessage());
            }
        }
    }

    private void ouvrirModification(Ressources ressource) {
        String[] chemins = {"/modifierRessources.fxml", "/ModifierRessources.fxml", "/ModifierRessource.fxml"};
        java.net.URL fxmlUrl = null;
        for (String c : chemins) { fxmlUrl = getClass().getResource(c); if (fxmlUrl != null) break; }
        if (fxmlUrl == null) { afficherErreur("Fichier modifierRessources.fxml introuvable."); return; }
        try {
            if (dashboardController != null) {
                Object ctrlObj = dashboardController.loadViewAndGet("modifierRessources.fxml",
                        "Modifier une ressource", "Gestion de Rania");
                if (ctrlObj instanceof ModifierRessourceController ctrl) {
                    Stage stage = dashboardController.getStage();
                    ctrl.initData(ressource, stage);
                }
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            ModifierRessourceController ctrl = loader.getController();
            Stage stage = (Stage) ressourcesContainer.getScene().getWindow();
            ctrl.initData(ressource, stage);
            boolean wasMax = stage.isMaximized();
            if (wasMax) stage.setMaximized(false);
            stage.setScene(new Scene(root));
            if (wasMax) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            afficherErreur("Impossible d'ouvrir l'éditeur : " + e.getMessage());
        }
    }

    @FXML public void filtrerTous()     { filtreActif = "TOUS";    mettreAJourBoutonActif(btnTous);     afficherRessources(toutesRessources); }
    @FXML public void filtrerLivres()   { filtreActif = "PDF";     mettreAJourBoutonActif(btnLivres);   appliquerFiltre("PDF"); }
    @FXML public void filtrerVideos()   { filtreActif = "VIDEO";   mettreAJourBoutonActif(btnVideos);   appliquerFiltre("VIDEO"); }
    @FXML public void filtrerPodcasts() { filtreActif = "PODCAST"; mettreAJourBoutonActif(btnPodcasts); appliquerFiltre("PODCAST"); }
    @FXML public void filtrerArticles() { filtreActif = "ARTICLE"; mettreAJourBoutonActif(btnArticles); appliquerFiltre("ARTICLE"); }

    private void appliquerFiltre(String filtre) {
        if (filtre.equals("TOUS")) { afficherRessources(toutesRessources); return; }
        afficherRessources(toutesRessources.stream()
                .filter(r -> r.getContenuEnum() != null &&
                        r.getContenuEnum().name().toUpperCase().equals(filtre)).toList());
    }

    private void mettreAJourBoutonActif(Button actif) {
        String inactif =
                "-fx-background-color: rgba(255,255,255,0.10); -fx-text-fill: rgba(255,255,255,0.88);" +
                        "-fx-background-radius: 20; -fx-padding: 9 18; -fx-cursor: hand;" +
                        "-fx-border-color: rgba(255,255,255,0.15); -fx-border-width: 1; -fx-border-radius: 20;" +
                        "-fx-font-size: 13px;";
        String actifStyle =
                "-fx-background-color: white; -fx-text-fill: #1B4332;" +
                        "-fx-background-radius: 20; -fx-padding: 9 22; -fx-cursor: hand;" +
                        "-fx-font-weight: bold; -fx-font-size: 13px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);";
        for (Button btn : new Button[]{btnTous, btnLivres, btnVideos, btnPodcasts, btnArticles})
            if (btn != null) btn.setStyle(inactif);
        if (actif != null) actif.setStyle(actifStyle);
    }

    @FXML public void handleRechercher() {
        String terme = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        if (terme.isEmpty()) { afficherRessources(toutesRessources); return; }
        afficherRessources(toutesRessources.stream()
                .filter(r ->
                        (r.getTitre()  != null && r.getTitre().toLowerCase().contains(terme)) ||
                                (r.getResume() != null && r.getResume().toLowerCase().contains(terme))
                ).toList());
    }

    @FXML public void handleTrier() {
        if (comboTri == null || comboTri.getValue() == null) return;
        List<Ressources> liste = new java.util.ArrayList<>(toutesRessources);
        switch (comboTri.getValue()) {
            case "Titre A-Z" -> liste.sort((a, b) ->
                    a.getTitre() == null ? 1 : b.getTitre() == null ? -1 :
                            a.getTitre().compareToIgnoreCase(b.getTitre()));
            case "Plus vu" -> liste.sort((a, b) -> b.getNbr_vues() - a.getNbr_vues());
            case "Plus ancien" -> liste.sort((a, b) ->
                    a.getDate_publication() == null ? 1 : b.getDate_publication() == null ? -1 :
                            a.getDate_publication().compareTo(b.getDate_publication()));
            default -> liste.sort((a, b) ->
                    a.getDate_publication() == null ? 1 : b.getDate_publication() == null ? -1 :
                            b.getDate_publication().compareTo(a.getDate_publication()));
        }
        afficherRessources(liste);
    }

    @FXML public void retourAccueil() {
        if (dashboardController != null) {
            dashboardController.navigateTo("DashboardHome.fxml", "Tableau de Bord", "Vue d'ensemble de MindAura");
            return;
        }
        try {
            Stage stage = (Stage) ressourcesContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AccueilRessource.fxml"));
            boolean wasMax = stage.isMaximized();
            if (wasMax) stage.setMaximized(false);
            stage.setScene(new Scene(loader.load()));
            if (wasMax) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML public void ouvrirAjoutRessource() {
        if (dashboardController != null) {
            dashboardController.navigateTo("ajouter.fxml", "Ajouter une ressource", "Gestion de Rania");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouter.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ressourcesContainer.getScene().getWindow();
            boolean wasMax = stage.isMaximized();
            if (wasMax) stage.setMaximized(false);
            stage.setScene(new Scene(root));
            if (wasMax) stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            afficherErreur("Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }

    private String niveauLabel(String niveau) {
        return switch (niveau.toUpperCase()) {
            case "DEBUTANT"      -> "Débutant";
            case "INTERMEDIAIRE" -> "Intermédiaire";
            case "AVANCE"        -> "Avancé";
            case "TOUS_NIVEAUX"  -> "Tous niveaux";
            default              -> niveau.replace("_", " ");
        };
    }

    private String formatNombre(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fk", n / 1_000.0);
        return String.valueOf(n);
    }

    private void afficherErreur(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur"); alert.setHeaderText(null); alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white;");
        alert.showAndWait();
    }

    private void afficherToast(String msg) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Succès"); info.setHeaderText(null); info.setContentText(msg);
        info.getDialogPane().setStyle("-fx-background-color: white;");
        info.showAndWait();
    }
}