package tn.esprit.controllers;

import tn.esprit.utils.SessionManager;

import java.sql.SQLException;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.Commentaires;
import tn.esprit.entities.Ressources;
import tn.esprit.services.CommentairesServices;
import tn.esprit.services.RessourcesService;
import tn.esprit.utils.MyDataBase;

import java.awt.Desktop;
import java.net.URI;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ArticleViewerController {

    // â”€â”€ WebView & Navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private WebView            webView;
    @FXML private Label              labelTitreHeader;
    @FXML private Label              labelUrlHeader;
    @FXML private Label              labelUrlBarre;
    @FXML private Label              labelChargementUrl;
    @FXML private Label              labelStatutPage;
    @FXML private Label              labelCategorie;
    @FXML private Label              labelDuree;
    @FXML private Label              labelZoom;
    @FXML private ProgressBar        progressLecture;
    @FXML private ProgressIndicator  indicateurChargement;
    @FXML private VBox               overlayChargement;
    @FXML private Button             btnPrecedent;
    @FXML private Button             btnSuivant;
    @FXML private Button             btnFermer;
    @FXML private Button             btnOuvrirNavigateur;
    @FXML private Button             btnRecharger;

    // â”€â”€ Stats â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private Label              lblNbrVues;
    @FXML private Label              lblNbrLikes;
    @FXML private Button             btnLike;
    @FXML private Label              lblNombreCommentaires;

    // â”€â”€ Commentaires â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @FXML private TextField          txtNouveauCommentaire;
    @FXML private Button             btnAjouterCommentaire;
    @FXML private Button             btnAnnulerEdition;
    @FXML private VBox               commentairesContainer;

    // â”€â”€ Ã‰tat WebView â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private WebEngine webEngine;
    private Ressources ressource;
    private Stage      stage;
    private UserHomeController parentHomeController;
    private double     niveauZoom  = 1.0;
    private String     urlCourante = "";

    // â”€â”€ Ã‰tat likes & commentaires â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private int          nbrLikes             = 0;
    private boolean      isLiked              = false;
    private boolean      modeEdition          = false;
    private Commentaires commentaireEnEdition = null;

    // â”€â”€ Services â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private CommentairesServices commentairesServices;
    private RessourcesService    ressourceService;

    // â”€â”€ Page de retour â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private String pageRetour = null;

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // UTILITAIRES NULL-SAFE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    private void setLabel(Label label, String text) {
        if (label != null) label.setText(text);
    }
    private void setNodeVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) { node.setVisible(visible); node.setManaged(visible); }
    }
    private void setButtonDisable(Button btn, boolean disable) {
        if (btn != null) btn.setDisable(disable);
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // INITIALIZE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    @FXML
    public void initialize() {
        Connection connection = MyDataBase.getInstance().getConx();
        commentairesServices = new CommentairesServices(connection);
        ressourceService     = new RessourcesService(connection);

        setNodeVisible(btnAnnulerEdition, false);

        webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setOnAlert(e -> {});

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            Platform.runLater(() -> {
                if (newState == Worker.State.RUNNING) {
                    setNodeVisible(indicateurChargement, true);
                    setNodeVisible(overlayChargement, true);
                    setLabel(labelStatutPage, "Chargement...");

                } else if (newState == Worker.State.SUCCEEDED) {
                    setNodeVisible(indicateurChargement, false);
                    setNodeVisible(overlayChargement, false);
                    String url = webEngine.getLocation();
                    setLabel(labelUrlBarre, url);
                    urlCourante = url;
                    String titrePage = webEngine.getTitle();
                    if (titrePage != null && !titrePage.isEmpty())
                        setLabel(labelTitreHeader, titrePage.length() > 70
                                ? titrePage.substring(0, 70) + "â€¦" : titrePage);
                    setLabel(labelStatutPage, "âœ“ Page chargÃ©e");
                    setButtonDisable(btnPrecedent,
                            webEngine.getHistory().getCurrentIndex() <= 0);
                    setButtonDisable(btnSuivant,
                            webEngine.getHistory().getCurrentIndex() >=
                                    webEngine.getHistory().getEntries().size() - 1);
                    injecterStyleLisibilite();
                    injecterTrackerScroll();

                } else if (newState == Worker.State.FAILED) {
                    setNodeVisible(indicateurChargement, false);
                    afficherPageErreur();
                    setLabel(labelStatutPage, "âš  Ã‰chec de chargement");
                }
            });
        });

        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
            if (newEx != null) Platform.runLater(this::afficherPageErreur);
        });
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // SET RESSOURCE
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    public void setRessource(Ressources r, Stage stg) {
        this.ressource = r;
        this.stage     = stg;

        if (stg != null)
            stg.setTitle("ðŸ“° " + (r.getTitre() != null ? r.getTitre() : "Article"));

        setLabel(labelTitreHeader, r.getTitre() != null ? r.getTitre() : "Article");

        if (r.getDuree_lecture() > 0)
            setLabel(labelDuree, "â± " + r.getDuree_lecture() + " min de lecture");

        if (r.getCategorie() != null) {
            setLabel(labelCategorie, r.getCategorie().replace("_", " "));
            if (labelCategorie != null) labelCategorie.setVisible(true);
        }

        setLabel(lblNbrVues,  String.valueOf(r.getNbr_vues()));
        setLabel(lblNbrLikes, String.valueOf(r.getLikes()));
        nbrLikes = r.getLikes();

        ressourceService.incrementerVues(r.getId_ressources());
        chargerCommentaires();

        String url = r.getUrl();
        if (url == null || url.trim().isEmpty()) {
            afficherPageAucunUrl(r.getTitre()); return;
        }
        url = url.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://"))
            url = "https://" + url;

        urlCourante = url;
        setLabel(labelUrlHeader, extraireHote(url));
        setLabel(labelUrlBarre, url);
        setLabel(labelChargementUrl, url);

        final String urlFinale = url;
        new Thread(() -> extraireEtAfficher(urlFinale)).start();
    }

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }
    public void setPageRetour(String fxmlPath) {
        this.pageRetour = fxmlPath;
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // FERMER / RETOUR
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    @FXML
    private void fermer() {
        if (parentHomeController != null) {
            parentHomeController.loadInCenter("/FrontOffice_COMPLET.fxml");
            return;
        }
        if (pageRetour != null && !pageRetour.isEmpty()) {
            try {
                java.net.URL fxmlResource = getClass().getResource(pageRetour);
                if (fxmlResource == null) {
                    afficherErreur("Fichier de retour introuvable : " + pageRetour);
                    fermerFenetre(); return;
                }
                FXMLLoader loader = new FXMLLoader(fxmlResource);
                Parent root  = loader.load();
                Stage  s     = (stage != null) ? stage
                        : (Stage) btnFermer.getScene().getWindow();
                Scene  scene = new Scene(root);
                ajouterCss(scene, pageRetour);
                s.setScene(scene);
                s.setMaximized(true);
                s.show();
            } catch (Exception e) {
                e.printStackTrace();
                afficherErreur("Impossible de retourner Ã  la page prÃ©cÃ©dente.");
                fermerFenetre();
            }
        } else {
            fermerFenetre();
        }
    }

    private void fermerFenetre() {
        if (stage != null) stage.close();
        else if (btnFermer != null) ((Stage) btnFermer.getScene().getWindow()).close();
    }

    private void ajouterCss(Scene scene, String fxmlPath) {
        String cssPath = null;
        if (fxmlPath.contains("Accueil") || fxmlPath.contains("accueil"))
            cssPath = "/css/accueil.css";
        else if (fxmlPath.contains("FrontOffice"))
            cssPath = "/css/frontoffice.css";
        if (cssPath != null) {
            java.net.URL css = getClass().getResource(cssPath);
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        }
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // COMMENTAIRES
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    private void chargerCommentaires() {
        if (commentairesContainer == null) return;
        commentairesContainer.getChildren().clear();

        // â”€â”€ Utilise ressource_id (via alias getId_ressources()) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        List<Commentaires> liste = commentairesServices
                .getCommentairesByArticle(ressource.getId_ressources());

        if (lblNombreCommentaires != null)
            lblNombreCommentaires.setText("(" + liste.size() + ")");

        if (liste.isEmpty()) {
            VBox vide = new VBox(8);
            vide.setAlignment(Pos.CENTER);
            vide.setStyle("-fx-padding: 30;");
            Label emoji = new Label("ðŸ’¬");
            emoji.setStyle("-fx-font-size: 32px;");
            Label msg = new Label("Aucun commentaire pour le moment");
            msg.setStyle("-fx-text-fill: #9AA89F; -fx-font-size: 13px;");
            Label sub = new Label("Soyez le premier Ã  commenter !");
            sub.setStyle("-fx-text-fill: #B0BDB5; -fx-font-size: 11px; -fx-font-style: italic;");
            vide.getChildren().addAll(emoji, msg, sub);
            commentairesContainer.getChildren().add(vide);
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy Ã  HH:mm");
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
        carte.setOnMouseExited (e -> carte.setStyle(styleNormal));

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label avatar = new Label("ðŸ‘¤");
        avatar.setStyle("-fx-font-size: 15px;");

        Label nom = new Label(c.getUser_name() != null ? c.getUser_name() : "Utilisateur");
        nom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1B4332;");

        Label date = new Label("â€¢ " + (c.getDatePublication() != null
                ? sdf.format(c.getDatePublication()) : ""));
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

        Button btnEdit = creerBtnAction("âœï¸", "Modifier",
                "rgba(27,67,50,0.12)", e -> entrerModeEdition(c));
        Button btnDel  = creerBtnAction("ðŸ—‘ï¸", "Supprimer",
                "rgba(220,38,38,0.12)", e -> supprimerCommentaire(c));

        actions.getChildren().addAll(btnEdit, btnDel);
        return actions;
    }

    private Button creerBtnAction(String txt, String tooltip, String hoverColor,
                                  javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(txt);
        String base = "-fx-background-color: transparent; -fx-font-size: 14px;" +
                "-fx-cursor: hand; -fx-padding: 3 6;";
        btn.setStyle(base);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(action);
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:" + hoverColor + "; -fx-font-size: 14px;" +
                        "-fx-cursor: hand; -fx-padding: 3 6; -fx-background-radius: 6;"));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

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
                afficherSucces("Commentaire modifiÃ© !");
            } else {
                // â”€â”€ RÃ©cupÃ©rer l'utilisateur connectÃ© â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                int    idUser   = 0;
                String userName = "Utilisateur";
                if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null) {
                    String idStr = SessionManager.getCurrentUser().getId_utilisateur();
                    if (idStr != null) {
                        try { idUser = Integer.parseInt(idStr.trim()); }
                        catch (NumberFormatException ignored) {}
                    }
                    userName = SessionManager.getNomComplet();
                }

                Commentaires com = new Commentaires();
                // â”€â”€ Utilise ressource_id (plus id_Article) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                com.setRessource_id(ressource.getId_ressources());
                com.setId_user     (idUser);
                com.setUser_name   (userName);
                com.setContenu     (texte);
                com.setDate        (new Date());
                com.setLikes       (0);
                com.setReponse     (0);
                com.setStatus      (Commentaires.StatusCommentaires.ACTIF);

                commentairesServices.add(com);
                afficherSucces("Commentaire publiÃ© !");
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
        if (btnAjouterCommentaire != null) btnAjouterCommentaire.setText("âœ“ Modifier");
        setNodeVisible(btnAnnulerEdition, true);
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
        setNodeVisible(btnAnnulerEdition, false);
    }

    private void supprimerCommentaire(Commentaires c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce commentaire ?");
        confirm.setContentText("Cette action est irrÃ©versible.");
        confirm.getDialogPane().setStyle("-fx-background-color: white;");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                commentairesServices.delete(c);
                if (modeEdition && commentaireEnEdition != null &&
                        commentaireEnEdition.getId_commentaires() == c.getId_commentaires())
                    annulerEdition();
                chargerCommentaires();
                afficherSucces("Commentaire supprimÃ© !");
            } catch (Exception e) {
                e.printStackTrace();
                afficherErreur("Impossible de supprimer : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleLike() {
        if (!isLiked) {
            nbrLikes++; isLiked = true;
            try {
                ressourceService.incrementerLikes(ressource.getId_ressources());
            } catch (SQLException e) {
                e.printStackTrace();
                afficherErreur("Erreur lors de l'ajout du like");
            }
            if (btnLike != null) btnLike.setText("ðŸ’š AimÃ©");
        } else {
            nbrLikes--; isLiked = false;
            if (btnLike != null) btnLike.setText("â¤ J'aime");
        }
        setLabel(lblNbrLikes, String.valueOf(nbrLikes));
    }

    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    // WEBVIEW
    // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
    private void extraireEtAfficher(String url) {
        Platform.runLater(() -> {
            setNodeVisible(overlayChargement, true);
            setNodeVisible(indicateurChargement, true);
            setLabel(labelStatutPage, "Extraction du contenu...");
        });
        try {
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 Chrome/120.0 Safari/537.36")
                    .timeout(10_000).get();

            String titreExtrait = doc.title();
            if (titreExtrait == null || titreExtrait.isEmpty())
                titreExtrait = ressource.getTitre() != null ? ressource.getTitre() : "Article";

            doc.select("script, style, nav, footer, header, aside, iframe, " +
                    ".ads, .advertisement, .cookie, .popup, .social-share, " +
                    ".newsletter, .related, .comments-section, h1, .article-title, " +
                    ".entry-title, .post-title, .page-title, .headline").remove();

            String contenuHtml = "";
            String[] selecteurs = {
                    "article","main",".article-content",".post-content",".entry-content",
                    ".article-body","#content",".content","[itemprop='articleBody']",".story-body"
            };
            for (String sel : selecteurs) {
                org.jsoup.select.Elements els = doc.select(sel);
                if (!els.isEmpty() && els.text().length() > 200) {
                    contenuHtml = els.first().html(); break;
                }
            }
            if (contenuHtml.isEmpty()) contenuHtml = doc.body().html();

            org.jsoup.nodes.Document cDoc = org.jsoup.Jsoup.parseBodyFragment(contenuHtml);
            cDoc.select("h1").remove();
            for (org.jsoup.nodes.Element h2 : cDoc.select("h2")) {
                if (titreExtrait.toLowerCase().contains(
                        h2.text().toLowerCase().substring(0, Math.min(20, h2.text().length())))) {
                    h2.remove(); break;
                }
            }
            contenuHtml = cDoc.body().html();

            final String tFinal = titreExtrait;
            final String hFinal = construireHtmlPropre(tFinal, contenuHtml, url);

            Platform.runLater(() -> {
                webEngine.loadContent(hFinal, "text/html");
                setNodeVisible(overlayChargement, false);
                setNodeVisible(indicateurChargement, false);
                setLabel(labelTitreHeader, tFinal.length() > 70
                        ? tFinal.substring(0, 70) + "â€¦" : tFinal);
                setLabel(labelStatutPage, "âœ“ Contenu extrait avec succÃ¨s");
            });
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                webEngine.load(url);
                setLabel(labelStatutPage, "âš  Chargement direct (extraction Ã©chouÃ©e)");
            });
        }
    }

    private String construireHtmlPropre(String titre, String contenu, String urlSource) {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    *{box-sizing:border-box;margin:0;padding:0}
                    body{font-family:'Georgia',serif;background:#FAFBF9;color:#2C3E35;line-height:1.85;font-size:17px}
                    .container{max-width:780px;margin:0 auto;padding:48px 32px 80px}
                    .source-bar{display:flex;align-items:center;gap:8px;background:#F0F7F2;
                        border:1px solid #C8E6D4;border-radius:24px;padding:8px 16px;
                        margin-bottom:32px;font-size:12px;color:#2D6A4F;
                        font-family:'Segoe UI',sans-serif;width:fit-content}
                    h1{font-size:28px;font-weight:bold;color:#0D2B1E;line-height:1.35;
                        margin-bottom:24px;border-left:5px solid #52B788;padding-left:18px}
                    h2{font-size:22px;color:#1B4332;margin:36px 0 14px}
                    h3{font-size:18px;color:#2D6A4F;margin:28px 0 10px}
                    p{margin-bottom:20px;text-align:justify}
                    a{color:#2D6A4F;text-decoration:underline;text-decoration-color:rgba(45,106,79,0.35)}
                    img{max-width:100%%;height:auto;border-radius:12px;margin:24px 0;
                        box-shadow:0 4px 16px rgba(0,0,0,0.10)}
                    blockquote{border-left:4px solid #52B788;background:#F0F7F2;margin:24px 0;
                        padding:16px 20px;border-radius:0 10px 10px 0;color:#4A6B5A;font-style:italic}
                    ul,ol{margin:16px 0 20px 28px} li{margin-bottom:8px}
                    hr{border:none;border-top:2px solid #E2EDE7;margin:32px 0}
                    ::-webkit-scrollbar{width:6px}
                    ::-webkit-scrollbar-thumb{background:#52B788;border-radius:3px}
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="source-bar">ðŸ”— Source : <strong>%s</strong></div>
                    <h1>%s</h1><hr>%s
                </div>
            </body>
            </html>
            """.formatted(titre, extraireHote(urlSource), titre, contenu);
    }

    private void injecterStyleLisibilite() {
        try {
            webEngine.executeScript(
                    "var s=document.createElement('style');" +
                            "s.innerHTML='::-webkit-scrollbar{width:6px}" +
                            "::-webkit-scrollbar-thumb{background:#52B788;border-radius:3px}';" +
                            "document.head.appendChild(s);");
        } catch (Exception ignored) {}
    }

    private void injecterTrackerScroll() {
        try {
            webEngine.executeScript(
                    "window.addEventListener('scroll',function(){" +
                            "var d=document.documentElement;" +
                            "var sh=d.scrollHeight-d.clientHeight;" +
                            "if(sh>0) window.progressValue=(window.pageYOffset||d.scrollTop)/sh;});");
            Timeline tl = new Timeline(new KeyFrame(Duration.millis(400), e -> {
                try {
                    Object val = webEngine.executeScript("window.progressValue||0");
                    if (val instanceof Number && progressLecture != null)
                        progressLecture.setProgress(((Number) val).doubleValue());
                } catch (Exception ignored) {}
            }));
            tl.setCycleCount(Animation.INDEFINITE);
            tl.play();
        } catch (Exception ignored) {}
    }

    private void afficherPageErreur() {
        setNodeVisible(overlayChargement, false);
        webEngine.loadContent(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                        "body{font-family:'Segoe UI',sans-serif;background:#F5F7F5;" +
                        "display:flex;align-items:center;justify-content:center;min-height:100vh}" +
                        ".card{background:white;border-radius:20px;padding:48px;text-align:center;max-width:520px}" +
                        "h2{color:#0D2B1E}p{color:#7A8B7F;font-size:14px}" +
                        ".url{font-size:12px;color:#9AA89F;background:#F0F4F2;padding:10px 16px;" +
                        "border-radius:10px;margin:16px 0;word-break:break-all}" +
                        ".btn{background:#1B4332;color:white;border:none;border-radius:24px;" +
                        "padding:12px 28px;font-size:14px;font-weight:bold;cursor:pointer;margin:4px}" +
                        "</style></head><body><div class='card'>" +
                        "<h2>âš  Impossible de charger l'article</h2>" +
                        "<p>Le site peut Ãªtre inaccessible.</p>" +
                        "<div class='url'>" + urlCourante + "</div>" +
                        "<button class='btn' onclick=\"location.href='" + urlCourante + "'\">â†» RÃ©essayer</button>" +
                        "</div></body></html>");
        setLabel(labelStatutPage, "âš  Impossible de charger la page");
    }

    private void afficherPageAucunUrl(String titre) {
        setNodeVisible(overlayChargement, false);
        webEngine.loadContent(
                "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                        "body{font-family:'Segoe UI',sans-serif;background:#F5F7F5;" +
                        "display:flex;align-items:center;justify-content:center;min-height:100vh}" +
                        ".card{background:white;border-radius:20px;padding:48px;text-align:center}" +
                        "h2{color:#0D2B1E}p{color:#9AA89F;font-size:14px}" +
                        "</style></head><body><div class='card'>" +
                        "<div style='font-size:56px'>ðŸ“­</div>" +
                        "<h2>" + (titre != null ? titre : "Article") + "</h2>" +
                        "<p>Aucune URL n'est associÃ©e Ã  cet article.</p>" +
                        "</div></body></html>");
        setLabel(labelStatutPage, "Aucune URL disponible");
    }

    @FXML private void ouvrirDansNavigateur() {
        if (urlCourante == null || urlCourante.isEmpty()) return;
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(urlCourante));
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                ProcessBuilder pb;
                if (os.contains("win"))
                    pb = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", urlCourante);
                else if (os.contains("mac"))
                    pb = new ProcessBuilder("open", urlCourante);
                else
                    pb = new ProcessBuilder("xdg-open", urlCourante);
                pb.start();
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void allerPrecedent() {
        javafx.scene.web.WebHistory h = webEngine.getHistory();
        if (h.getCurrentIndex() > 0) h.go(-1);
    }
    @FXML private void allerSuivant() {
        javafx.scene.web.WebHistory h = webEngine.getHistory();
        if (h.getCurrentIndex() < h.getEntries().size() - 1) h.go(1);
    }
    @FXML private void recharger()  { webEngine.reload(); }

    @FXML private void zoomPlus() {
        niveauZoom = Math.min(niveauZoom + 0.10, 2.5);
        webView.setZoom(niveauZoom);
        setLabel(labelZoom, Math.round(niveauZoom * 100) + "%");
    }
    @FXML private void zoomMoins() {
        niveauZoom = Math.max(niveauZoom - 0.10, 0.4);
        webView.setZoom(niveauZoom);
        setLabel(labelZoom, Math.round(niveauZoom * 100) + "%");
    }

    private String extraireHote(String url) {
        try { return new java.net.URL(url).getHost(); }
        catch (Exception e) { return url; }
    }

    private void afficherErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color: white;");
        a.showAndWait();
    }
    private void afficherSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("SuccÃ¨s"); a.setHeaderText(null); a.setContentText(msg);
        a.getDialogPane().setStyle("-fx-background-color: white;");
        a.showAndWait();
    }
}
