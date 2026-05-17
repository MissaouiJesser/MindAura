package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import tn.esprit.entities.Ressources;

import java.io.File;
import java.io.IOException;

public class PodcastPlayerController {

    @FXML private Button    btnRetour;
    @FXML private Button    btnPlayPause;
    @FXML private Button    btnReculer;
    @FXML private Button    btnAvancer;
    @FXML private Slider    sliderProgression;
    @FXML private Slider    sliderVolume;
    @FXML private Label     lblTitre;
    @FXML private Label     lblNomEpisode;
    @FXML private Label     lblDescription;
    @FXML private Label     lblTempsActuel;
    @FXML private Label     lblDureeTotal;
    @FXML private Label     lblErreur;
    @FXML private ImageView imgPochette;
    @FXML private ComboBox<String> comboVitesse;

    // VBox parent des contrôles (pour injection dynamique du bouton navigateur)
    @FXML private VBox conteneurPrincipal;

    private MediaPlayer mediaPlayer;
    private Ressources  ressource;
    private Stage       currentStage;
    private String      pageRetour = "/AfficherRe.fxml";
    private boolean     sliderEnDeplacement = false;
    private boolean     enLecture = false;

    // ─── INITIALIZE ──────────────────────────────────────────
    @FXML
    public void initialize() {
        comboVitesse.getItems().addAll("0.5x", "0.75x", "1x", "1.25x", "1.5x", "2x");
        comboVitesse.setValue("1x");
        comboVitesse.setOnAction(e -> changerVitesse());

        sliderVolume.valueProperty().addListener((obs, old, val) -> {
            if (mediaPlayer != null)
                mediaPlayer.setVolume(val.doubleValue());
        });

        sliderProgression.setOnMousePressed(e  -> sliderEnDeplacement = true);
        sliderProgression.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                Duration total = mediaPlayer.getTotalDuration();
                if (total != null && !total.isUnknown()) {
                    double seek = sliderProgression.getValue() / 100.0 * total.toSeconds();
                    mediaPlayer.seek(Duration.seconds(seek));
                }
            }
            sliderEnDeplacement = false;
        });
    }

    // ─── INIT RESSOURCE ──────────────────────────────────────
    public void setRessource(Ressources ressource, Stage stage) {
        this.ressource    = ressource;
        this.currentStage = stage;
        if (ressource == null) return;

        if (lblTitre       != null) lblTitre.setText(nvl(ressource.getTitre(), "Podcast"));
        if (lblNomEpisode  != null) lblNomEpisode.setText(nvl(ressource.getTitre(), ""));
        if (lblDescription != null) lblDescription.setText(nvl(ressource.getResume(), ""));

        // Pochette
        if (imgPochette != null && ressource.getImageUrl() != null
                && !ressource.getImageUrl().isEmpty()) {
            try {
                File f = new File(ressource.getImageUrl());
                String uri = f.exists() ? f.toURI().toString() : ressource.getImageUrl();
                imgPochette.setImage(new Image(uri, 240, 240, true, true));
            } catch (Exception ignored) {}
        }

        chargerAudio(ressource.getUrl());
    }

    public void setPageRetour(String fxmlPath) {
        this.pageRetour = fxmlPath;
    }

    // ─── CHARGEMENT AUDIO ────────────────────────────────────
    private void chargerAudio(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) {
            afficherErreur("Aucune URL audio associée à ce podcast.");
            return;
        }

        String url = urlStr.trim();

        // ── 1. Fichier local ──
        File fichierLocal = new File(url);
        if (fichierLocal.exists()) {
            url = fichierLocal.toURI().toString();
            lancerMediaPlayer(url, urlStr);
            return;
        }

        // ── 2. URL distante ──
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            url = "https://" + url;

        // ── 3. Détection des plateformes sans lien direct (Spotify, etc.)
        if (url.contains("spotify.com") || url.contains("deezer.com") || url.contains("apple.com")) {
            ouvrirDansWebView(url);
            return;
        }

        // ── 4. Détection d'URL audio directe ──
        String urlLower = url.toLowerCase();
        boolean estAudioDirect =
                urlLower.endsWith(".mp3")  ||
                        urlLower.endsWith(".m4a")  ||
                        urlLower.endsWith(".wav")  ||
                        urlLower.endsWith(".ogg")  ||
                        urlLower.endsWith(".aac")  ||
                        urlLower.contains("/audio/") ||
                        urlLower.contains("stream")  ||
                        urlLower.contains("podcast.") ||
                        urlLower.contains(".mp3?");

        if (estAudioDirect) {
            lancerMediaPlayer(url, urlStr);
            return;
        }

        // ── 5. Tentative d'extraction depuis la page web ──
        afficherErreur("Tentative d'extraction du lien audio depuis la page...");

        final String finalUrl = url;

        new Thread(() -> {
            String audioUrl = extraireUrlAudioDepuisPage(finalUrl);

            Platform.runLater(() -> {
                if (audioUrl != null) {
                    lblErreur.setText("");
                    lancerMediaPlayer(audioUrl, urlStr);
                } else {
                    afficherErreur("Lecture via WebView intégrée...");
                    ouvrirDansWebView(finalUrl);
                }
            });
        }).start();
    }

    /**
     * Extrait la première URL audio (MP3, M4A, etc.) trouvée dans une page web
     * @param pageUrl L'URL de la page web à analyser
     * @return L'URL directe du fichier audio, ou null si non trouvée
     */
    private String extraireUrlAudioDepuisPage(String pageUrl) {
        try {
            Document doc = Jsoup.connect(pageUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            // Stratégie 1: Balises <a> avec href se terminant par .mp3, .m4a, etc.
            Elements audioLinks = doc.select("a[href$=.mp3], a[href$=.m4a], a[href$=.wav], a[href$=.ogg], a[href$=.aac]");
            if (!audioLinks.isEmpty()) {
                String url = audioLinks.first().attr("abs:href");
                System.out.println("URL audio trouvée dans un lien: " + url);
                return url;
            }

            // Stratégie 2: Balises <audio> avec src
            Elements audioTags = doc.select("audio[src]");
            if (!audioTags.isEmpty()) {
                String url = audioTags.first().attr("abs:src");
                System.out.println("URL audio trouvée dans balise audio: " + url);
                return url;
            }

            // Stratégie 3: Balises <source> dans <audio>
            Elements sourceTags = doc.select("audio source[src]");
            if (!sourceTags.isEmpty()) {
                String url = sourceTags.first().attr("abs:src");
                System.out.println("URL audio trouvée dans source: " + url);
                return url;
            }

            // Stratégie 4: Recherche dans le contenu JavaScript
            String html = doc.html();
            if (html.contains("mp3:\"")) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("mp3:\"([^\"]+)\"");
                java.util.regex.Matcher m = p.matcher(html);
                if (m.find()) {
                    String url = m.group(1);
                    System.out.println("URL audio trouvée dans JS: " + url);
                    return url;
                }
            }

            return null;

        } catch (IOException e) {
            System.err.println("Erreur lors de l'analyse de la page: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /* // Méthode Selenium (commentée si non utilisée)
    private String extraireUrlAudioAvecSelenium(String pageUrl) {
        WebDriver driver = new ChromeDriver();
        try {
            driver.get(pageUrl);
            Thread.sleep(3000);
            WebElement audioElement = driver.findElement(By.cssSelector("audio[src]"));
            return audioElement.getAttribute("src");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            driver.quit();
        }
    }
    */

    private void lancerMediaPlayer(String mediaUrl, String urlOriginal) {
        try {
            Media media = new Media(mediaUrl);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setVolume(sliderVolume != null ? sliderVolume.getValue() : 0.8);

            mediaPlayer.setOnReady(() -> Platform.runLater(() -> {
                Duration total = mediaPlayer.getTotalDuration();
                if (lblDureeTotal != null) lblDureeTotal.setText(formaterDuree(total));
                if (sliderProgression != null) sliderProgression.setMax(100);
            }));

            mediaPlayer.currentTimeProperty().addListener((obs, old, now) ->
                    Platform.runLater(() -> {
                        if (!sliderEnDeplacement) {
                            Duration total = mediaPlayer.getTotalDuration();
                            if (total != null && total.toSeconds() > 0 && sliderProgression != null) {
                                sliderProgression.setValue(now.toSeconds() / total.toSeconds() * 100);
                            }
                            if (lblTempsActuel != null) lblTempsActuel.setText(formaterDuree(now));
                        }
                    })
            );

            mediaPlayer.setOnEndOfMedia(() -> Platform.runLater(() -> {
                enLecture = false;
                if (btnPlayPause != null) btnPlayPause.setText("▶");
                mediaPlayer.seek(Duration.ZERO);
            }));

            mediaPlayer.setOnError(() -> Platform.runLater(() -> {
                String msg = mediaPlayer.getError() != null
                        ? mediaPlayer.getError().getMessage() : "Erreur inconnue";
                afficherErreur("Impossible de lire ce média.\n" + msg);
                afficherBoutonNavigateur(urlOriginal);
            }));

        } catch (Exception e) {
            afficherErreur("Erreur de chargement : " + e.getMessage());
            afficherBoutonNavigateur(urlOriginal);
            e.printStackTrace();
        }
    }

    // ─── BOUTON FALLBACK NAVIGATEUR ──────────────────────────
    private void afficherBoutonNavigateur(final String url) {
        Platform.runLater(() -> {
            VBox parent = null;

            if (conteneurPrincipal != null) {
                parent = conteneurPrincipal;
            } else if (lblErreur != null && lblErreur.getParent() instanceof VBox) {
                parent = (VBox) lblErreur.getParent();
            }

            if (parent == null) return;

            parent.getChildren().removeIf(node -> {
                if (node instanceof Button) {
                    String txt = ((Button) node).getText();
                    return txt != null && txt.contains("navigateur");
                }
                return false;
            });

            Button btnOuvrir = new Button("🌐  Ouvrir dans le navigateur");
            btnOuvrir.setStyle(
                    "-fx-background-color: #1F6FEB; -fx-text-fill: white;" +
                            "-fx-background-radius: 20; -fx-padding: 11 28;" +
                            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(31,111,235,0.40), 10, 0, 0, 3);"
            );
            btnOuvrir.setOnMouseEntered(e -> btnOuvrir.setStyle(
                    "-fx-background-color: #1A5FCC; -fx-text-fill: white;" +
                            "-fx-background-radius: 20; -fx-padding: 11 28;" +
                            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;"
            ));
            btnOuvrir.setOnMouseExited(e -> btnOuvrir.setStyle(
                    "-fx-background-color: #1F6FEB; -fx-text-fill: white;" +
                            "-fx-background-radius: 20; -fx-padding: 11 28;" +
                            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand;" +
                            "-fx-effect: dropshadow(gaussian, rgba(31,111,235,0.40), 10, 0, 0, 3);"
            ));
            btnOuvrir.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            parent.getChildren().add(btnOuvrir);
        });
    }

    // ─── CONTRÔLES LECTURE ───────────────────────────────────
    @FXML
    private void handlePlayPause() {
        if (mediaPlayer == null) return;
        if (enLecture) {
            mediaPlayer.pause();
            enLecture = false;
            if (btnPlayPause != null) btnPlayPause.setText("▶");
        } else {
            mediaPlayer.play();
            enLecture = true;
            if (btnPlayPause != null) btnPlayPause.setText("⏸");
        }
    }

    @FXML
    private void handleReculer() {
        if (mediaPlayer == null) return;
        Duration now = mediaPlayer.getCurrentTime();
        mediaPlayer.seek(now.subtract(Duration.seconds(15)));
    }

    @FXML
    private void handleAvancer() {
        if (mediaPlayer == null) return;
        Duration now   = mediaPlayer.getCurrentTime();
        Duration total = mediaPlayer.getTotalDuration();
        Duration cible = now.add(Duration.seconds(15));
        if (total != null && cible.greaterThan(total)) cible = total;
        mediaPlayer.seek(cible);
    }

    private void changerVitesse() {
        if (mediaPlayer == null || comboVitesse.getValue() == null) return;
        try {
            double rate = Double.parseDouble(comboVitesse.getValue().replace("x", ""));
            mediaPlayer.setRate(rate);
        } catch (NumberFormatException ignored) {}
    }

    // ─── RETOUR ──────────────────────────────────────────────
    @FXML
    private void handleRetour() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(pageRetour));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─── UTILITAIRES ─────────────────────────────────────────
    private String formaterDuree(Duration d) {
        if (d == null || d.isUnknown() || d.isIndefinite()) return "0:00";
        int total  = (int) d.toSeconds();
        int heures = total / 3600;
        int min    = (total % 3600) / 60;
        int sec    = total % 60;
        return heures > 0
                ? String.format("%d:%02d:%02d", heures, min, sec)
                : String.format("%d:%02d", min, sec);
    }

    private void afficherErreur(String msg) {
        Platform.runLater(() -> {
            if (lblErreur != null) lblErreur.setText("⚠ " + msg);
        });
    }

    private String nvl(String val, String defaut) {
        return (val != null && !val.isEmpty()) ? val : defaut;
    }

// ... dans votre contrôleur ...

    private void ouvrirDansWebView(String url) {
        Platform.runLater(() -> {
            // Créer une nouvelle fenêtre
            Stage webStage = new Stage();
            webStage.setTitle("Lecture - " + (ressource != null ? ressource.getTitre() : "Podcast"));

            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            webEngine.load(url);

            // Optionnel : ajuster le titre de la fenêtre avec celui de la page
            webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
                if (newTitle != null) {
                    webStage.setTitle(newTitle);
                }
            });

            Scene scene = new Scene(webView, 900, 600);
            webStage.setScene(scene);
            webStage.show();

            // Fermer proprement le MediaPlayer s'il était en cours
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        });
    }
}