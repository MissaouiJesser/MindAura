package tn.esprit.controllers;

import java.sql.SQLException;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import tn.esprit.entities.Commentaires;
import tn.esprit.entities.Ressources;
import tn.esprit.services.CommentairesServices;
import tn.esprit.services.RessourcesService;
import tn.esprit.utils.MyDataBase;
import tn.esprit.utils.SessionManager;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class PDFViewerController implements DashboardController.DashboardAware {

    // ══════════════════════════════════════════════════════
    // CONSTANTE SYMFONY
    // ══════════════════════════════════════════════════════
    private static final String SYMFONY_BASE = "http://127.0.0.1:8000";

    // ── Labels ──────────────────────────────────────────────
    @FXML private Label     lblTitrePDF;
    @FXML private Label     lblInfoPDF;
    @FXML private Label     lblInfoPDF2;
    @FXML private Label     lblDescription;
    @FXML private Label     lblPageInfo;
    @FXML private Label     lblNbrLikes;
    @FXML private Label     lblNbrVues;
    @FXML private Label     lblNombreCommentaires;

    // ── Boutons ─────────────────────────────────────────────
    @FXML private Button    btnRetour;
    @FXML private Button    btnOuvrirPDF;
    @FXML private Button    btnTelecharger;
    @FXML private Button    btnLike;
    @FXML private Button    btnAjouterCommentaire;
    @FXML private Button    btnAnnulerEdition;

    // ── Saisie commentaire ──────────────────────────────────
    @FXML private TextField txtNouveauCommentaire;

    // ── Conteneurs ──────────────────────────────────────────
    @FXML private VBox      contentContainer;
    @FXML private VBox      pdfReaderBox;
    @FXML private VBox      pdfPagesContainer;
    @FXML private VBox      commentairesContainer;

    // ── Images ──────────────────────────────────────────────
    @FXML private ImageView pdfThumbnail;
    @FXML private ImageView pdfPageView;

    // ── Compatibilité FXML existant ─────────────────────────
    @FXML private Button    btnPagePrecedente;
    @FXML private Button    btnPageSuivante;

    // ── État PDF ────────────────────────────────────────────
    private PDDocument  pdfDocument;
    private PDFRenderer pdfRenderer;
    private int         totalPages = 0;

    // ── État ────────────────────────────────────────────────
    private int          nbrLikes             = 0;
    private boolean      isLiked              = false;
    private boolean      modeEdition          = false;
    private Commentaires commentaireEnEdition = null;

    // ── Services ────────────────────────────────────────────
    private Ressources           ressource;
    private Stage                currentStage;
    private String               pageRetour = "/FrontOffice_COMPLET.fxml";
    private UserHomeController   parentHomeController;
    private DashboardController  dashboardController;
    private RessourcesService    ressourceService;
    private CommentairesServices commentairesServices;

    // ════════════════════════════════════════════════════════
    // INITIALIZE
    // ════════════════════════════════════════════════════════
    @FXML
    public void initialize() {
        Connection connection = MyDataBase.getInstance().getConx();
        ressourceService     = new RessourcesService(connection);
        commentairesServices = new CommentairesServices(connection);

        if (btnAnnulerEdition != null) {
            btnAnnulerEdition.setVisible(false);
            btnAnnulerEdition.setManaged(false);
        }
    }

    // ════════════════════════════════════════════════════════
    // MÉTHODES URL — construire l'URL complète
    // ════════════════════════════════════════════════════════

    /**
     * Construit l'URL HTTP complète depuis l'URL stockée en BDD.
     * "/uploads/flysystem-test/resources/fichier.pdf"
     *   → "http://127.0.0.1:8000/uploads/flysystem-test/resources/fichier.pdf"
     * "https://exemple.com/..." → inchangé
     * "C:\Users\..." → null (invalide)
     */
    private String construireUrlComplete(String url) {
        if (url == null || url.isBlank()) return null;

        // URL externe complète → inchangée
        if (url.startsWith("http://") || url.startsWith("https://")) return url;

        // Chemin Windows local → invalide
        if (url.contains(":\\") || url.startsWith("C:/") || url.startsWith("C:\\")) {
            System.err.println("⚠ PDF : chemin local détecté (non uploadé) : " + url);
            return null;
        }

        // Chemin relatif Symfony → préfixer
        return SYMFONY_BASE + (url.startsWith("/") ? url : "/" + url);
    }

    /**
     * Télécharge le PDF depuis Symfony dans un fichier temporaire
     * pour que PDFBox puisse le lire localement.
     */
    private File telechargerPDFDepuisSymfony(String urlComplete) throws IOException {
        System.out.println("📥 Téléchargement PDF depuis : " + urlComplete);

        URL url = new URL(urlComplete);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);
        conn.setRequestProperty("User-Agent", "JavaFX-MindAura");

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("Serveur Symfony a répondu HTTP " + code + " pour : " + urlComplete);
        }

        // Créer un fichier temporaire
        Path tempFile = Files.createTempFile("mindaura_pdf_", ".pdf");
        tempFile.toFile().deleteOnExit();

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        System.out.println("✅ PDF téléchargé : " + tempFile.toAbsolutePath()
                + " (" + tempFile.toFile().length() / 1024 + " KB)");
        return tempFile.toFile();
    }

    // ════════════════════════════════════════════════════════
    // SET RESSOURCE
    // ════════════════════════════════════════════════════════
    public void setRessource(Ressources ressource, Stage stage) {
        this.ressource    = ressource;
        this.currentStage = stage;
        if (ressource == null) return;

        // ── Remplir les labels ──────────────────────────────
        if (lblInfoPDF2    != null) lblInfoPDF2.setText(ressource.getNbr_vues() + " vues");
        if (lblTitrePDF    != null) lblTitrePDF.setText(ressource.getTitre());
        if (lblInfoPDF     != null) lblInfoPDF.setText(ressource.getNbr_vues() + " vues");
        if (lblNbrVues     != null) lblNbrVues.setText(String.valueOf(ressource.getNbr_vues()));
        if (lblNbrLikes    != null) lblNbrLikes.setText(String.valueOf(ressource.getLikes()));
        if (lblDescription != null) lblDescription.setText(
                ressource.getResume() != null ? ressource.getResume() : "Aucune description.");

        nbrLikes = ressource.getLikes();

        // ── Image de couverture ─────────────────────────────
        if (pdfThumbnail != null && ressource.getImageUrl() != null) {
            String urlImg = construireUrlComplete(ressource.getImageUrl());
            if (urlImg != null) {
                try {
                    Image img = new Image(urlImg, true);
                    pdfThumbnail.setImage(img);
                } catch (Exception ignored) {
                    System.err.println("⚠ Image couverture non chargée : " + urlImg);
                }
            }
        }

        ressourceService.incrementerVues(ressource.getId_ressources());

        // ── Charger le PDF ──────────────────────────────────
        String urlBrute = ressource.getUrl();
        if (urlBrute != null && !urlBrute.trim().isEmpty()) {
            chargerPDF(urlBrute.trim());
        } else {
            if (lblPageInfo != null) lblPageInfo.setText("Aucun fichier PDF associé");
        }

        chargerCommentaires();
    }

    public void setPageRetour(String fxmlPath) {
        this.pageRetour = fxmlPath;
    }

    // ════════════════════════════════════════════════════════
    // CHARGEMENT PDF — ✅ CORRIGÉ
    // ════════════════════════════════════════════════════════
    private void chargerPDF(String urlBrute) {
        if (urlBrute == null || urlBrute.isBlank()) return;

        // ── Cas 1 : chemin Windows local → invalide ─────────
        if (urlBrute.contains(":\\") || urlBrute.startsWith("C:/")) {
            afficherErreurPDF(
                    "❌ Ce fichier n'est pas disponible.\n\n" +
                            "Il a été ajouté sans upload vers le serveur.\n" +
                            "Chemin local détecté : " + urlBrute
            );
            return;
        }

        // ── Cas 2 : fichier local existant (legacy) ─────────
        File localFile = new File(urlBrute);
        if (localFile.exists()) {
            chargerPDFDepuisFichier(localFile);
            return;
        }

        // ── Cas 3 : URL Symfony → télécharger puis afficher ─
        String urlComplete = construireUrlComplete(urlBrute);
        if (urlComplete == null) {
            afficherErreurPDF("URL invalide : " + urlBrute);
            return;
        }

        if (lblPageInfo != null) lblPageInfo.setText("⏳ Chargement du PDF...");

        // Téléchargement en background pour ne pas bloquer l'UI
        new Thread(() -> {
            try {
                File pdfTemp = telechargerPDFDepuisSymfony(urlComplete);
                javafx.application.Platform.runLater(() -> chargerPDFDepuisFichier(pdfTemp));
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                        afficherErreurPDF(
                                "Impossible de charger le PDF.\n\n" +
                                        "URL : " + urlComplete + "\n" +
                                        "Erreur : " + e.getMessage()
                        )
                );
            }
        }).start();
    }

    private void chargerPDFDepuisFichier(File pdfFile) {
        try {
            pdfDocument = Loader.loadPDF(pdfFile);
            pdfRenderer = new PDFRenderer(pdfDocument);
            totalPages  = pdfDocument.getNumberOfPages();

            System.out.println("✅ PDF chargé : " + totalPages + " page(s)");

            if (pdfPagesContainer != null) {
                afficherToutesLesPages();
            }
        } catch (Exception e) {
            e.printStackTrace();
            afficherErreurPDF("Erreur lors de l'ouverture du PDF : " + e.getMessage());
        }
    }

    private void afficherErreurPDF(String message) {
        if (lblPageInfo != null) lblPageInfo.setText("Fichier PDF introuvable");
        if (pdfPagesContainer != null) {
            pdfPagesContainer.getChildren().clear();
            Label lbl = new Label("📄 " + message);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px; -fx-padding: 20;");
            pdfPagesContainer.getChildren().add(lbl);
        }
        System.err.println("⚠ PDF : " + message);
    }

    private void afficherToutesLesPages() {
        if (pdfRenderer == null) return;
        pdfPagesContainer.getChildren().clear();

        for (int i = 0; i < totalPages; i++) {
            try {
                BufferedImage bImage = pdfRenderer.renderImageWithDPI(i, 150);
                Image fxImage = SwingFXUtils.toFXImage(bImage, null);

                Label lblPage = new Label("Page " + (i + 1) + " / " + totalPages);
                lblPage.setStyle("-fx-font-size: 12px; -fx-text-fill: #9AA89F; -fx-padding: 4 0 4 0;");

                ImageView pageView = new ImageView(fxImage);
                pageView.setFitWidth(820);
                pageView.setPreserveRatio(true);
                pageView.setSmooth(true);
                pageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);");

                Separator sep = new Separator();
                sep.setStyle("-fx-padding: 8 0;");

                pdfPagesContainer.getChildren().addAll(lblPage, pageView, sep);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (lblPageInfo != null) lblPageInfo.setText(totalPages + " page(s)");
    }

    // ════════════════════════════════════════════════════════
    // OUVRIR / TÉLÉCHARGER — ✅ CORRIGÉ
    // ════════════════════════════════════════════════════════
    @FXML
    private void handleOuvrirPDF() {
        if (ressource == null || ressource.getUrl() == null) return;

        String urlComplete = construireUrlComplete(ressource.getUrl());

        if (urlComplete == null) {
            afficherErreur(
                    "Ce PDF n'est pas disponible sur le serveur.\n" +
                            "Il n'a pas été uploadé correctement."
            );
            return;
        }

        try {
            // Ouvrir dans le navigateur par défaut
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(urlComplete));
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Afficher l'URL pour copier-coller
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Lien PDF");
            alert.setHeaderText("Copiez ce lien dans votre navigateur :");
            TextArea ta = new TextArea(urlComplete);
            ta.setEditable(false);
            ta.setWrapText(true);
            ta.setPrefRowCount(2);
            alert.getDialogPane().setContent(ta);
            alert.getDialogPane().setMinWidth(500);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleTelecharger() {
        handleOuvrirPDF(); // Le navigateur proposera le téléchargement
    }

    // ════════════════════════════════════════════════════════
    // COMMENTAIRES — CHARGEMENT BDD
    // ════════════════════════════════════════════════════════
    private void chargerCommentaires() {
        if (commentairesContainer == null) return;
        commentairesContainer.getChildren().clear();

        List<Commentaires> liste = commentairesServices
                .getCommentairesByArticle(ressource.getId_ressources());

        if (lblNombreCommentaires != null)
            lblNombreCommentaires.setText("(" + liste.size() + ")");

        if (liste.isEmpty()) {
            VBox vide = new VBox(8);
            vide.setAlignment(Pos.CENTER);
            vide.setStyle("-fx-padding: 30;");
            Label emoji = new Label("💬");
            emoji.setStyle("-fx-font-size: 36px;");
            Label msg = new Label("Aucun commentaire pour le moment");
            msg.setStyle("-fx-text-fill: #9AA89F; -fx-font-size: 13px;");
            Label sub = new Label("Soyez le premier à commenter !");
            sub.setStyle("-fx-text-fill: #B0BDB5; -fx-font-size: 11px; -fx-font-style: italic;");
            vide.getChildren().addAll(emoji, msg, sub);
            commentairesContainer.getChildren().add(vide);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy à HH:mm");
        for (Commentaires c : liste)
            commentairesContainer.getChildren().add(creerCarteCommentaire(c, sdf));
    }

    private VBox creerCarteCommentaire(Commentaires c, SimpleDateFormat sdf) {
        VBox carte = new VBox(8);
        String styleNormal =
                "-fx-background-color: #F5F8F6; -fx-background-radius: 12; -fx-padding: 14;" +
                        "-fx-border-color: #E0EAE5; -fx-border-width: 1; -fx-border-radius: 12;";
        String styleHover =
                "-fx-background-color: #EBF3EE; -fx-background-radius: 12; -fx-padding: 14;" +
                        "-fx-border-color: #C8DDD4; -fx-border-width: 1; -fx-border-radius: 12;";
        carte.setStyle(styleNormal);
        carte.setOnMouseEntered(e -> carte.setStyle(styleHover));
        carte.setOnMouseExited(e  -> carte.setStyle(styleNormal));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("👤");
        avatar.setStyle("-fx-font-size: 16px;");

        Label nom = new Label(c.getUser_name() != null ? c.getUser_name() : "Utilisateur");
        nom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1B4332;");

        Label date = new Label("• " + (c.getDate() != null ? sdf.format(c.getDate()) : ""));
        date.setStyle("-fx-font-size: 11px; -fx-text-fill: #9AA89F;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(avatar, nom, date, spacer, creerBoutonsActions(c));

        Label contenu = new Label(c.getContenu());
        contenu.setWrapText(true);
        contenu.setStyle("-fx-font-size: 13px; -fx-text-fill: #2C3E35; -fx-line-spacing: 2;");

        carte.getChildren().addAll(header, contenu);
        return carte;
    }

    private HBox creerBoutonsActions(Commentaires c) {
        HBox actions = new HBox(4);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = new Button("✏️");
        btnEdit.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 3 6;");
        btnEdit.setTooltip(new Tooltip("Modifier"));
        btnEdit.setOnAction(e -> entrerModeEdition(c));
        btnEdit.setOnMouseEntered(e -> btnEdit.setStyle(
                "-fx-background-color: rgba(27,67,50,0.12); -fx-font-size: 14px;" +
                        "-fx-cursor: hand; -fx-padding: 3 6; -fx-background-radius: 6;"));
        btnEdit.setOnMouseExited(e -> btnEdit.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 3 6;"));

        Button btnDel = new Button("🗑️");
        btnDel.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 3 6;");
        btnDel.setTooltip(new Tooltip("Supprimer"));
        btnDel.setOnAction(e -> supprimerCommentaire(c));
        btnDel.setOnMouseEntered(e -> btnDel.setStyle(
                "-fx-background-color: rgba(220,38,38,0.12); -fx-font-size: 14px;" +
                        "-fx-cursor: hand; -fx-padding: 3 6; -fx-background-radius: 6;"));
        btnDel.setOnMouseExited(e -> btnDel.setStyle(
                "-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 3 6;"));

        actions.getChildren().addAll(btnEdit, btnDel);
        return actions;
    }

    // ════════════════════════════════════════════════════════
    // COMMENTAIRES — AJOUTER / MODIFIER
    // ════════════════════════════════════════════════════════
    @FXML
    private void handleAjouterCommentaire() {
        if (txtNouveauCommentaire == null) return;
        String texte = txtNouveauCommentaire.getText().trim();
        if (texte.isEmpty()) return;

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
                afficherSucces("Commentaire publié !");
            }

            txtNouveauCommentaire.clear();
            chargerCommentaires();

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur : " + e.getMessage());
        }
    }

    private void entrerModeEdition(Commentaires c) {
        modeEdition          = true;
        commentaireEnEdition = c;
        if (txtNouveauCommentaire != null) {
            txtNouveauCommentaire.setText(c.getContenu());
            txtNouveauCommentaire.requestFocus();
            txtNouveauCommentaire.setStyle(
                    "-fx-background-color: #FEF3C7; -fx-border-color: #1B4332;" +
                            "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;" +
                            "-fx-padding: 10 12; -fx-font-size: 12px;");
        }
        if (btnAjouterCommentaire != null) btnAjouterCommentaire.setText("✓ Modifier");
        if (btnAnnulerEdition != null) {
            btnAnnulerEdition.setVisible(true);
            btnAnnulerEdition.setManaged(true);
        }
    }

    @FXML
    private void handleAnnulerEdition() { annulerEdition(); }

    private void annulerEdition() {
        modeEdition          = false;
        commentaireEnEdition = null;
        if (txtNouveauCommentaire != null) {
            txtNouveauCommentaire.clear();
            txtNouveauCommentaire.setStyle(
                    "-fx-background-color: #F5F8F6; -fx-border-color: #D4E0DA;" +
                            "-fx-border-width: 1.5; -fx-border-radius: 10; -fx-background-radius: 10;" +
                            "-fx-padding: 10 12; -fx-font-size: 12px;");
        }
        if (btnAjouterCommentaire != null) btnAjouterCommentaire.setText("Publier");
        if (btnAnnulerEdition != null) {
            btnAnnulerEdition.setVisible(false);
            btnAnnulerEdition.setManaged(false);
        }
    }

    private void supprimerCommentaire(Commentaires c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce commentaire ?");
        confirm.setContentText("Cette action est irréversible.");
        confirm.getDialogPane().setStyle("-fx-background-color: white;");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentairesServices.delete(c);
                if (modeEdition && commentaireEnEdition != null &&
                        commentaireEnEdition.getId_commentaires() == c.getId_commentaires())
                    annulerEdition();
                chargerCommentaires();
                afficherSucces("Commentaire supprimé !");
            } catch (Exception e) {
                e.printStackTrace();
                afficherErreur("Impossible de supprimer : " + e.getMessage());
            }
        }
    }

    // ════════════════════════════════════════════════════════
    // LIKES
    // ════════════════════════════════════════════════════════
    @FXML
    private void handleLike() {
        if (!isLiked) {
            nbrLikes++;
            isLiked = true;
            try { ressourceService.incrementerLikes(ressource.getId_ressources()); }
            catch (SQLException e) { e.printStackTrace(); afficherErreur("Erreur like"); }
            if (btnLike != null) btnLike.setText("💚 Aimé");
        } else {
            nbrLikes--;
            isLiked = false;
            if (btnLike != null) btnLike.setText("❤ J'aime");
        }
        if (lblNbrLikes != null) lblNbrLikes.setText(String.valueOf(nbrLikes));
    }

    // ════════════════════════════════════════════════════════
    // NAVIGATION
    // ════════════════════════════════════════════════════════
    @Override
    public void setDashboardController(DashboardController dc) { this.dashboardController = dc; }

    @FXML
    public void handleRetour() {
        try { if (pdfDocument != null) pdfDocument.close(); } catch (Exception ignored) {}

        if (dashboardController != null) {
            dashboardController.navigateTo("AfficherRe.fxml", "Gestion des ressources",
                    "Découvrir notre ressources éducatives et ses commentaires");
            return;
        }
        if (parentHomeController != null) {
            parentHomeController.loadInCenter("/FrontOffice_COMPLET.fxml");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(pageRetour));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setParentHomeController(UserHomeController parent) { this.parentHomeController = parent; }

    // ════════════════════════════════════════════════════════
    // UTILITAIRES
    // ════════════════════════════════════════════════════════
    private void afficherErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color: white;");
        a.showAndWait();
    }

    private void afficherSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color: white;");
        a.showAndWait();
    }
}