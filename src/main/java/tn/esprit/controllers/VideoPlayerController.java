package tn.esprit.controllers;

import java.sql.SQLException;
import com.google.api.services.youtube.model.SearchResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Commentaires;
import tn.esprit.entities.Ressources;
import tn.esprit.services.CommentairesServices;
import tn.esprit.services.RessourcesService;
import tn.esprit.services.YouTubeService;
import tn.esprit.utils.MyDataBase;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class VideoPlayerController implements DashboardController.DashboardAware {

    // ══════════════════════════════════════════════════════
    // CONSTANTE SYMFONY
    // ══════════════════════════════════════════════════════
    private static final String SYMFONY_BASE = "http://127.0.0.1:8000";

    // ─── FXML ────────────────────────────────────────────────────────────────
    @FXML private Label    lblTitreVideo;
    @FXML private Label    lblInfoVideo;
    @FXML private Label    lblDescription;
    @FXML private Label    lblNombreLikes;
    @FXML private Label    lblNombreCommentaires;
    @FXML private Label    lblCaracteres;
    @FXML private Label    lblTempsActuel;
    @FXML private Label    lblTempstotal;

    @FXML private Button   btnRetour;
    @FXML private Button   btnLike;
    @FXML private Button   btnPlayPause;
    @FXML private Button   btnEnvoyerCommentaire;
    @FXML private Button   btnPlay;
    @FXML private Button   btnAnnulerEdition;

    @FXML private TextArea  txtCommentaire;
    @FXML private VBox      commentairesContainer;
    @FXML private VBox      youtubeContainer;
    @FXML private StackPane videoContainer;
    @FXML private MediaView mediaView;
    @FXML private ImageView thumbnailImage;
    @FXML private Slider    timeSlider;
    @FXML private Slider    volumeSlider;
    @FXML private HBox      controlsContainer;
    @FXML private VBox      thumbnailContainer;
    @FXML private TextField txtCommentaireInline;
    @FXML private Label     lblNombreVues;

    private Ressources           ressource;
    private Stage                currentStage;
    private UserHomeController   parentHomeController;
    private DashboardController  dashboardController;
    private MediaPlayer          mediaPlayer;
    private RessourcesService    ressourceService;
    private CommentairesServices commentairesServices;

    private boolean      isLiked              = false;
    private int          likesCount           = 0;
    private boolean      modeEdition          = false;
    private Commentaires commentaireEnEdition = null;
    private File         videoFile;
    private Stage        youtubeStage;
    private WebView      youtubeWebView;
    private String       pageRetour;

    public void setVideoFile(File file)          { this.videoFile  = file; }
    public void setPageRetour(String pageRetour) { this.pageRetour = pageRetour; }

    // ════════════════════════════════════════════════════════
    // MÉTHODES URL — construire l'URL complète
    // ════════════════════════════════════════════════════════

    /**
     * Construit l'URL HTTP complète depuis l'URL stockée en BDD.
     * "/uploads/flysystem-test/resources/video.mp4"
     *   → "http://127.0.0.1:8000/uploads/flysystem-test/resources/video.mp4"
     * "https://..." → inchangé
     * "C:\..." → null (invalide)
     */
    private String construireUrlComplete(String url) {
        if (url == null || url.isBlank()) return null;

        // URL externe complète → inchangée
        if (url.startsWith("http://") || url.startsWith("https://")) return url;

        // Chemin Windows local → invalide
        if (url.contains(":\\") || url.startsWith("C:/") || url.startsWith("C:\\")) {
            System.err.println("⚠ Vidéo : chemin local détecté (non uploadé) : " + url);
            return null;
        }

        // Chemin relatif Symfony → préfixer
        return SYMFONY_BASE + (url.startsWith("/") ? url : "/" + url);
    }

    // ════════════════════════════════════════════════════════
    // INITIALIZE
    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        Connection connection = MyDataBase.getInstance().getConx();
        ressourceService     = new RessourcesService(connection);
        commentairesServices = new CommentairesServices(connection);

        if (txtCommentaire != null) {
            txtCommentaire.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > 500) txtCommentaire.setText(oldVal);
                else if (lblCaracteres != null) lblCaracteres.setText(newVal.length() + "/500");
            });
        }
        if (volumeSlider != null) {
            volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (mediaPlayer != null) mediaPlayer.setVolume(newVal.doubleValue());
            });
        }
        setupButtonHoverEffects();
    }

    // ════════════════════════════════════════════════════════
    // SET RESSOURCE — ✅ CORRIGÉ
    // ════════════════════════════════════════════════════════
    public void setRessource(Ressources ressource, Stage stage) {
        this.ressource    = ressource;
        this.currentStage = stage;
        if (ressource == null) return;

        // ── Labels ──────────────────────────────────────────
        if (lblNombreVues != null) lblNombreVues.setText(String.valueOf(ressource.getNbr_vues()));
        if (lblTitreVideo != null) lblTitreVideo.setText(ressource.getTitre());
        if (lblInfoVideo  != null) lblInfoVideo.setText(formatNumber(ressource.getNbr_vues()) + " vues");
        if (lblDescription!= null) lblDescription.setText(ressource.getResume());

        // ── Image de couverture ─────────────────────────────
        // ✅ CORRIGÉ : construire l'URL complète au lieu de File(url)
        if (thumbnailImage != null && ressource.getImageUrl() != null) {
            String urlImg = construireUrlComplete(ressource.getImageUrl());
            if (urlImg != null) {
                try {
                    Image img = new Image(urlImg, true); // true = background loading
                    thumbnailImage.setImage(img);
                } catch (Exception e) {
                    System.err.println("⚠ Image couverture non chargée : " + urlImg);
                }
            }
        }

        chargerLikes();
        chargerCommentaires();
        ressourceService.incrementerVues(ressource.getId_ressources());
        chargerSuggestionsYouTube(ressource.getTitre());
    }

    // ════════════════════════════════════════════════════════
    // PLAY — ✅ CORRIGÉ : supporte URLs Symfony
    // ════════════════════════════════════════════════════════
    @FXML
    public void handlePlay() {
        if (ressource == null || ressource.getUrl() == null) {
            afficherErreur("Aucune vidéo associée à cette ressource.");
            return;
        }

        String urlBrute = ressource.getUrl().trim();

        // ── Chemin Windows local → invalide ─────────────────
        if (urlBrute.contains(":\\") || urlBrute.startsWith("C:/")) {
            afficherErreur(
                    "❌ Cette vidéo n'est pas disponible.\n\n" +
                            "Elle a été ajoutée sans upload vers le serveur.\n" +
                            "Chemin local détecté : " + urlBrute
            );
            return;
        }

        // ── Construire l'URL finale ──────────────────────────
        String urlComplete;

        // Priorité 1 : fichier local explicitement défini (via setVideoFile)
        if (videoFile != null && videoFile.exists()) {
            urlComplete = videoFile.toURI().toString();
            System.out.println("📁 Lecture vidéo locale : " + urlComplete);

            // Priorité 2 : fichier local existant (chemin direct en BDD)
        } else if (new File(urlBrute).exists()) {
            urlComplete = new File(urlBrute).toURI().toString();
            System.out.println("📁 Lecture vidéo locale : " + urlComplete);

            // Priorité 3 : URL Symfony ou externe
        } else {
            urlComplete = construireUrlComplete(urlBrute);
            if (urlComplete == null) {
                afficherErreur("URL vidéo invalide : " + urlBrute);
                return;
            }
            System.out.println("🌐 Lecture vidéo depuis Symfony : " + urlComplete);
        }

        // ── Masquer thumbnail, afficher player ───────────────
        if (thumbnailContainer != null) {
            thumbnailContainer.setVisible(false);
            thumbnailContainer.setManaged(false);
        }
        if (mediaView != null)       mediaView.setVisible(true);
        if (controlsContainer != null) controlsContainer.setVisible(true);

        // ── Créer MediaPlayer ────────────────────────────────
        try {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            }

            Media media = new Media(urlComplete);
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            if (volumeSlider != null)
                mediaPlayer.setVolume(volumeSlider.getValue());
            else
                mediaPlayer.setVolume(0.8);

            // Mise à jour du slider de temps
            if (timeSlider != null) {
                mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
                    if (!timeSlider.isValueChanging()) timeSlider.setValue(newVal.toSeconds());
                    if (lblTempsActuel != null) lblTempsActuel.setText(formatTime(newVal));
                });

                mediaPlayer.setOnReady(() -> {
                    Duration total = mediaPlayer.getTotalDuration();
                    timeSlider.setMax(total.toSeconds());
                    if (lblTempstotal != null) lblTempstotal.setText(formatTime(total));
                });

                timeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (timeSlider.isValueChanging())
                        mediaPlayer.seek(Duration.seconds(newVal.doubleValue()));
                });
            }

            mediaPlayer.setOnError(() -> {
                String errMsg = mediaPlayer.getError() != null
                        ? mediaPlayer.getError().getMessage()
                        : "Erreur inconnue";
                System.err.println("❌ Erreur MediaPlayer : " + errMsg);
                Platform.runLater(() -> afficherErreur("Erreur lecture vidéo : " + errMsg));
            });

            mediaPlayer.play();
            if (btnPlayPause != null) btnPlayPause.setText("⏸");

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Impossible de lire la vidéo : " + e.getMessage());
        }
    }

    @FXML
    public void handlePlayPause() {
        if (mediaPlayer == null) {
            handlePlay();
        } else if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            if (btnPlayPause != null) btnPlayPause.setText("▶");
        } else {
            mediaPlayer.play();
            if (btnPlayPause != null) btnPlayPause.setText("⏸");
        }
    }

    // ════════════════════════════════════════════════════════
    // LIKES
    // ════════════════════════════════════════════════════════
    @FXML
    public void handleLike() {
        if (!isLiked) {
            likesCount++;
            isLiked = true;
            try { ressourceService.incrementerLikes(ressource.getId_ressources()); }
            catch (SQLException e) { e.printStackTrace(); afficherErreur("Erreur like"); }
            if (btnLike != null) {
                btnLike.setText("💚  Aimé !");
                btnLike.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 12 20; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;");
            }
        } else {
            likesCount--;
            isLiked = false;
            if (btnLike != null) {
                btnLike.setText("❤  J'aime cette ressource");
                btnLike.setStyle("-fx-background-color: #7B5EA7; -fx-text-fill: white;" +
                        "-fx-background-radius: 10; -fx-padding: 12 20; -fx-font-size: 13px;" +
                        "-fx-font-weight: bold; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(123,94,167,0.35), 10, 0, 0, 3);");
            }
        }
        if (lblNombreLikes != null) lblNombreLikes.setText(String.valueOf(likesCount));
    }

    // ════════════════════════════════════════════════════════
    // YOUTUBE
    // ════════════════════════════════════════════════════════
    private void chargerSuggestionsYouTube(String titre) {
        new Thread(() -> {
            try {
                YouTubeService service = new YouTubeService();
                List<SearchResult> resultats = service.rechercherVideos(titre);
                Platform.runLater(() -> afficherSuggestionsYouTube(resultats));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (youtubeContainer != null) {
                        youtubeContainer.getChildren().clear();
                        Label lblErreur = new Label("⚠️ Impossible de charger les suggestions YouTube.");
                        lblErreur.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px;");
                        youtubeContainer.getChildren().add(lblErreur);
                    }
                });
            }
        }).start();
    }

    private void afficherSuggestionsYouTube(List<SearchResult> resultats) {
        if (youtubeContainer == null) return;
        youtubeContainer.getChildren().clear();

        Label lblSection = new Label("📺 Vidéos YouTube liées");
        lblSection.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 0 8 0;");
        youtubeContainer.getChildren().add(lblSection);

        if (resultats == null || resultats.isEmpty()) {
            Label lblVide = new Label("Aucune suggestion trouvée.");
            lblVide.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 13px;");
            youtubeContainer.getChildren().add(lblVide);
            return;
        }

        for (SearchResult video : resultats) {
            try {
                String titreVideo  = video.getSnippet().getTitle();
                String videoId     = video.getId().getVideoId();
                if (videoId == null) continue;
                String thumbUrl    = video.getSnippet().getThumbnails().getDefault().getUrl();
                String channelName = video.getSnippet().getChannelTitle();

                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #0D1117; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;");

                try {
                    ImageView thumb = new ImageView(new Image(thumbUrl, 100, 60, true, true));
                    thumb.setFitWidth(100); thumb.setFitHeight(60);
                    StackPane thumbPane = new StackPane(thumb);
                    Label playBadge = new Label("▶");
                    playBadge.setStyle("-fx-text-fill: white; -fx-font-size: 18px;" +
                            "-fx-background-color: rgba(255,0,0,0.8);" +
                            "-fx-background-radius: 4; -fx-padding: 2 6;");
                    thumbPane.getChildren().add(playBadge);
                    row.getChildren().add(thumbPane);
                } catch (Exception ignored) {}

                VBox infoBox = new VBox(4);
                HBox.setHgrow(infoBox, Priority.ALWAYS);
                Label lblTitre = new Label(titreVideo);
                lblTitre.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                lblTitre.setWrapText(true);
                lblTitre.setMaxWidth(350);
                infoBox.getChildren().add(lblTitre);
                if (channelName != null && !channelName.isEmpty()) {
                    Label lblChaine = new Label("🎬 " + channelName);
                    lblChaine.setStyle("-fx-text-fill: rgba(255,255,255,0.5); -fx-font-size: 11px;");
                    infoBox.getChildren().add(lblChaine);
                }

                Button btnOuvrir = new Button("▶ Lire");
                btnOuvrir.setStyle("-fx-background-color: #FF0000; -fx-text-fill: white;" +
                        "-fx-background-radius: 6; -fx-font-size: 12px;" +
                        "-fx-font-weight: bold; -fx-padding: 6 14; -fx-cursor: hand;");
                btnOuvrir.setOnMouseEntered(e -> btnOuvrir.setStyle(
                        "-fx-background-color: #CC0000; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-font-size: 12px;" +
                                "-fx-font-weight: bold; -fx-padding: 6 14; -fx-cursor: hand;"));
                btnOuvrir.setOnMouseExited(e -> btnOuvrir.setStyle(
                        "-fx-background-color: #FF0000; -fx-text-fill: white;" +
                                "-fx-background-radius: 6; -fx-font-size: 12px;" +
                                "-fx-font-weight: bold; -fx-padding: 6 14; -fx-cursor: hand;"));

                final String fVideoId = videoId;
                final String fTitre   = titreVideo;
                btnOuvrir.setOnAction(e -> ouvrirYouTubeEnIntegre(fVideoId, fTitre));
                row.setOnMouseClicked(e -> ouvrirYouTubeEnIntegre(fVideoId, fTitre));
                row.setOnMouseEntered(e -> row.setStyle(
                        "-fx-background-color: #1A1A2E; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;"));
                row.setOnMouseExited(e -> row.setStyle(
                        "-fx-background-color: #0D1117; -fx-background-radius: 8; -fx-padding: 10; -fx-cursor: hand;"));

                row.getChildren().addAll(infoBox, btnOuvrir);
                youtubeContainer.getChildren().add(row);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void ouvrirYouTubeEnIntegre(String videoId, String titreVideo) {
        if (youtubeStage != null && youtubeStage.isShowing()) {
            chargerVideoYouTube(videoId, titreVideo);
            youtubeStage.toFront();
            return;
        }
        youtubeStage = new Stage();
        youtubeStage.initModality(Modality.NONE);
        youtubeStage.setTitle("📺 " + titreVideo);
        youtubeStage.setWidth(920);
        youtubeStage.setHeight(620);

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #FF0000; -fx-padding: 12 20;");
        Label lblYT = new Label("▶ YouTube");
        lblYT.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        Label lblTitreYT = new Label(titreVideo);
        lblTitreYT.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        lblTitreYT.setMaxWidth(580);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button btnFermer = new Button("✕ Fermer");
        btnFermer.setStyle("-fx-background-color: white; -fx-text-fill: #FF0000;" +
                "-fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 14;");
        btnFermer.setOnAction(e -> {
            if (youtubeWebView != null) youtubeWebView.getEngine().load("about:blank");
            youtubeStage.close();
        });
        header.getChildren().addAll(lblYT, lblTitreYT, spacer, btnFermer);

        youtubeWebView = new WebView();
        youtubeWebView.setContextMenuEnabled(false);
        VBox root = new VBox(header, youtubeWebView);
        VBox.setVgrow(youtubeWebView, Priority.ALWAYS);
        root.setStyle("-fx-background-color: black;");

        youtubeStage.setScene(new Scene(root));
        youtubeStage.setOnCloseRequest(e -> {
            if (youtubeWebView != null) youtubeWebView.getEngine().load("about:blank");
        });
        youtubeStage.show();
        chargerVideoYouTube(videoId, titreVideo);
    }

    private void chargerVideoYouTube(String videoId, String titreVideo) {
        if (youtubeWebView == null) return;
        if (youtubeStage != null) youtubeStage.setTitle("📺 " + titreVideo);
        WebEngine engine = youtubeWebView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        engine.load("https://www.youtube.com/watch?v=" + videoId + "&autoplay=1");
    }

    // ════════════════════════════════════════════════════════
    // COMMENTAIRES
    // ════════════════════════════════════════════════════════
    @FXML
    public void handleEnvoyerCommentaire() {
        String texte = "";
        if (txtCommentaireInline != null && txtCommentaireInline.getText() != null)
            texte = txtCommentaireInline.getText().trim();
        else if (txtCommentaire != null && txtCommentaire.getText() != null)
            texte = txtCommentaire.getText().trim();

        if (texte.isEmpty()) { afficherErreur("Veuillez écrire un commentaire"); return; }

        try {
            if (modeEdition && commentaireEnEdition != null) {
                commentaireEnEdition.setContenu(texte);
                commentairesServices.modifier(commentaireEnEdition);
                annulerEdition();
                afficherSucces("Commentaire modifié !");
            } else {
                Commentaires com = new Commentaires();
                com.setRessource_id(ressource.getId_ressources());
                int    idUser   = 0;
                String userName = "Utilisateur";
                if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
                    String idStr = SessionManager.getCurrentUser().getId_utilisateur();
                    if (idStr != null) {
                        try { idUser = Integer.parseInt(idStr.trim()); } catch (NumberFormatException ignored) {}
                    }
                    userName = SessionManager.getNomComplet();
                }
                com.setId_user(idUser);
                com.setUser_name(userName);
                com.setContenu(texte);
                com.setDatePublication(new Date());
                com.setLikes(0);
                com.setReponse(0);
                com.setStatus(Commentaires.StatusCommentaires.ACTIF);
                commentairesServices.add(com);
            }
            if (txtCommentaireInline != null) txtCommentaireInline.clear();
            if (txtCommentaire      != null) txtCommentaire.clear();
            chargerCommentaires();
        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    @FXML
    public void handleAnnulerEdition() { annulerEdition(); }

    private void chargerLikes() {
        likesCount = ressourceService.getLikes(ressource.getId_ressources());
        if (lblNombreLikes != null) lblNombreLikes.setText(String.valueOf(likesCount));
    }

    private void chargerCommentaires() {
        if (commentairesContainer == null) return;
        commentairesContainer.getChildren().clear();

        List<Commentaires> commentaires = commentairesServices
                .getCommentairesByArticle(ressource.getId_ressources());
        if (lblNombreCommentaires != null)
            lblNombreCommentaires.setText("(" + commentaires.size() + ")");

        if (commentaires.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 40;");
            Label emoji    = new Label("💭"); emoji.setStyle("-fx-font-size: 48px;");
            Label empty    = new Label("Aucun commentaire pour le moment");
            empty.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 14px;");
            Label subtitle = new Label("Soyez le premier à commenter !");
            subtitle.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12px; -fx-font-style: italic;");
            emptyBox.getChildren().addAll(emoji, empty, subtitle);
            commentairesContainer.getChildren().add(emptyBox);
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
        for (Commentaires c : commentaires)
            commentairesContainer.getChildren().add(creerCommentaireBox(c, sdf));
    }

    private VBox creerCommentaireBox(Commentaires c, SimpleDateFormat sdf) {
        VBox commentBox = new VBox(8);
        commentBox.setStyle("-fx-background-color: #F0F4F8; -fx-background-radius: 8; -fx-padding: 15;");
        commentBox.setOnMouseEntered(e -> commentBox.setStyle(
                "-fx-background-color: #D9E8E3; -fx-background-radius: 8; -fx-padding: 15;"));
        commentBox.setOnMouseExited(e  -> commentBox.setStyle(
                "-fx-background-color: #F0F4F8; -fx-background-radius: 8; -fx-padding: 15;"));

        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label userIcon  = new Label("👤"); userIcon.setStyle("-fx-font-size: 16px;");
        Label userName  = new Label(c.getUser_name());
        userName.setStyle("-fx-text-fill: #7B5EA7; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label dateLabel = new Label("• " + sdf.format(c.getDatePublication()));
        dateLabel.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 11px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(userIcon, userName, dateLabel, spacer, creerBoutonsActions(c));

        Label contenuLabel = new Label(c.getContenu());
        contenuLabel.setWrapText(true);
        contenuLabel.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 13px; -fx-line-spacing: 2px;");

        commentBox.getChildren().addAll(headerBox, contenuLabel);
        return commentBox;
    }

    private HBox creerBoutonsActions(Commentaires commentaire) {
        HBox actionsBox = new HBox(5);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnModifier = new Button("✏️");
        btnModifier.setStyle("-fx-background-color: transparent; -fx-text-fill: #7B5EA7;" +
                "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 3 8;");
        btnModifier.setTooltip(new Tooltip("Modifier"));
        btnModifier.setOnAction(e -> modifierCommentaire(commentaire));

        Button btnSupprimer = new Button("🗑️");
        btnSupprimer.setStyle("-fx-background-color: transparent; -fx-text-fill: #DC2626;" +
                "-fx-font-size: 16px; -fx-cursor: hand; -fx-padding: 3 8;");
        btnSupprimer.setTooltip(new Tooltip("Supprimer"));
        btnSupprimer.setOnAction(e -> supprimerCommentaire(commentaire));

        actionsBox.getChildren().addAll(btnModifier, btnSupprimer);
        return actionsBox;
    }

    private void modifierCommentaire(Commentaires commentaire) {
        modeEdition          = true;
        commentaireEnEdition = commentaire;
        if (txtCommentaireInline != null) {
            txtCommentaireInline.setText(commentaire.getContenu());
            txtCommentaireInline.requestFocus();
            txtCommentaireInline.setStyle(
                    "-fx-background-color: #FEF3C7; -fx-border-color: #1B4332;" +
                            "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;" +
                            "-fx-padding: 10 14; -fx-font-size: 12px;");
        }
        if (btnEnvoyerCommentaire != null) btnEnvoyerCommentaire.setText("✓ Modifier");
        if (btnAnnulerEdition     != null) {
            btnAnnulerEdition.setVisible(true);
            btnAnnulerEdition.setManaged(true);
        }
    }

    private void supprimerCommentaire(Commentaires commentaire) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer ce commentaire ?");
        confirmation.setContentText("Cette action est irréversible.");
        confirmation.getDialogPane().setStyle("-fx-background-color: white;");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentairesServices.delete(commentaire);
                chargerCommentaires();
                afficherSucces("Commentaire supprimé !");
                if (modeEdition && commentaireEnEdition != null &&
                        commentaireEnEdition.getId_commentaires() == commentaire.getId_commentaires())
                    annulerEdition();
            } catch (Exception e) {
                e.printStackTrace();
                afficherErreur("Erreur lors de la suppression");
            }
        }
    }

    private void annulerEdition() {
        modeEdition          = false;
        commentaireEnEdition = null;
        if (txtCommentaireInline != null) {
            txtCommentaireInline.clear();
            txtCommentaireInline.setStyle(
                    "-fx-background-color: #F5F8F6; -fx-border-color: #C8DDD2;" +
                            "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;" +
                            "-fx-padding: 10 14; -fx-font-size: 12px;");
        }
        if (txtCommentaire        != null) txtCommentaire.clear();
        if (btnEnvoyerCommentaire != null) btnEnvoyerCommentaire.setText("Publier");
        if (btnAnnulerEdition     != null) {
            btnAnnulerEdition.setVisible(false);
            btnAnnulerEdition.setManaged(false);
        }
    }

    // ════════════════════════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════════════════════════
    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    @FXML
    public void handleRetour() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }
        if (youtubeStage != null && youtubeStage.isShowing()) {
            if (youtubeWebView != null) youtubeWebView.getEngine().load("about:blank");
            youtubeStage.close();
        }
        if (dashboardController != null) {
            dashboardController.navigateTo("AfficherRe.fxml", "Gestion des ressources",
                    "Découvrir notre ressources éducatives et ses commentaires");
            return;
        }
        if (parentHomeController != null) {
            parentHomeController.loadInCenter("/FrontOffice_COMPLET.fxml");
            return;
        }
        if (pageRetour != null && !pageRetour.isEmpty()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(pageRetour));
                Parent root = loader.load();
                Stage stage = (Stage) btnRetour.getScene().getWindow();
                Scene scene = new Scene(root);
                try {
                    scene.getStylesheets().add(
                            getClass().getResource("/css/accueil.css").toExternalForm());
                } catch (Exception ignored) {}
                stage.setScene(scene);
                stage.setMaximized(true);
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
                afficherErreur("Impossible de retourner à la page précédente.");
                if (currentStage != null) currentStage.close();
            }
        } else {
            if (currentStage != null) currentStage.close();
        }
    }

    public void setParentHomeController(UserHomeController parent) { this.parentHomeController = parent; }

    // ════════════════════════════════════════════════════════
    // EFFETS HOVER
    // ════════════════════════════════════════════════════════
    private void setupButtonHoverEffects() {
        if (btnRetour != null) {
            btnRetour.setOnMouseEntered(e -> btnRetour.setStyle(
                    "-fx-background-color: #F0F4F8; -fx-text-fill: #1B4332;" +
                            "-fx-background-radius: 8; -fx-padding: 10 20;" +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.2), 8, 0, 0, 2);"));
            btnRetour.setOnMouseExited(e -> btnRetour.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1B4332;" +
                            "-fx-background-radius: 8; -fx-padding: 10 20;" +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-cursor: hand;"));
        }
        if (btnPlay != null) {
            btnPlay.setOnMouseEntered(e -> btnPlay.setStyle(
                    "-fx-background-color: #2D6A4F; -fx-text-fill: white;" +
                            "-fx-font-size: 48px; -fx-background-radius: 50%;" +
                            "-fx-min-width: 100px; -fx-min-height: 100px; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(45,106,79,0.6), 25, 0, 0, 5);"));
            btnPlay.setOnMouseExited(e -> btnPlay.setStyle(
                    "-fx-background-color: #1B4332; -fx-text-fill: white;" +
                            "-fx-font-size: 48px; -fx-background-radius: 50%;" +
                            "-fx-min-width: 100px; -fx-min-height: 100px; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.5), 20, 0, 0, 5);"));
        }
        if (btnPlayPause != null) {
            btnPlayPause.setOnMouseEntered(e -> btnPlayPause.setStyle(
                    "-fx-background-color: #2D6A4F; -fx-text-fill: white;" +
                            "-fx-font-size: 20px; -fx-background-radius: 8;" +
                            "-fx-min-width: 50px; -fx-min-height: 40px; -fx-cursor: hand;"));
            btnPlayPause.setOnMouseExited(e -> btnPlayPause.setStyle(
                    "-fx-background-color: #1B4332; -fx-text-fill: white;" +
                            "-fx-font-size: 20px; -fx-background-radius: 8;" +
                            "-fx-min-width: 50px; -fx-min-height: 40px; -fx-cursor: hand;"));
        }
    }

    // ════════════════════════════════════════════════════════
    // UTILITAIRES
    // ════════════════════════════════════════════════════════
    private String formatTime(Duration d) {
        int totalSeconds = (int) d.toSeconds();
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private String formatNumber(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private void afficherErreur(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white;");
        alert.showAndWait();
    }

    private void afficherSucces(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.getDialogPane().setStyle("-fx-background-color: white;");
        alert.showAndWait();
    }
}