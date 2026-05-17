package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.MeteoService;
import tn.esprit.services.NewsApiService;
import tn.esprit.services.QuotableService;
import tn.esprit.services.TestPsychoService;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.PaginationHelper;
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ChoisirTestController implements Initializable {

    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    // ─── FXML Tests ───────────────────────────────────────────────────────────
    @FXML private Label                     citationLabel;
    @FXML private Label                     auteurLabel;
    @FXML private ListView<TestPsycho>      listViewTests;
    @FXML private Label                     meteoLabel;
    @FXML private Label                     meteoMessageLabel;
    @FXML private Label                     userNameLabel;
    @FXML private HBox                      paginationContainer;
    @FXML private Button                    btnPrev;
    @FXML private Button                    btnNext;
    @FXML private Label                     lblPageInfo;
    @FXML private TextField                 txtRecherche;
    @FXML private ComboBox<String>          cmbFiltreType;
    @FXML private Label                     lblNbResultats;

    // ─── FXML News ────────────────────────────────────────────────────────────
    @FXML private ComboBox<NewsApiService.Theme> cmbThemeNews;
    @FXML private TextField                      txtRechercheNews;
    @FXML private VBox                           newsContainer;
    @FXML private ProgressIndicator             newsLoading;
    @FXML private Label                          lblNewsErreur;

    // ─── État ─────────────────────────────────────────────────────────────────
    private List<TestPsycho> listeTestsFull    = new ArrayList<>();
    private List<TestPsycho> listeFiltree      = new ArrayList<>();
    private int              currentPage       = 0;
    private static final int PAGE_SIZE         = 5;

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // ── CellFactory tests ─────────────────────────────────────────────────
        listViewTests.setCellFactory(param -> new ListCell<TestPsycho>() {
            @Override
            protected void updateItem(TestPsycho test, boolean empty) {
                super.updateItem(test, empty);
                if (empty || test == null) { setText(null); setGraphic(null); return; }

                VBox vbox = new VBox(6);
                vbox.setPadding(new Insets(4));
                vbox.setMaxWidth(Double.MAX_VALUE);

                Label titre = new Label(test.getTitreTest());
                titre.setFont(Font.font("System", FontWeight.BOLD, 14));
                titre.setStyle("-fx-text-fill: #1A1A2E; -fx-wrap-text: true;");

                // Badge type + durée sur une ligne
                HBox meta = new HBox(8);
                meta.setAlignment(Pos.CENTER_LEFT);

                Label badgeType = new Label(test.getTypeTest());
                badgeType.setStyle(
                        "-fx-font-size: 10px; -fx-font-weight: bold;" +
                                "-fx-padding: 2 8 2 8; -fx-background-radius: 20;" +
                                "-fx-background-color: #D1FAE5; -fx-text-fill: #065F46;"
                );

                Label duree = new Label("⏱ " + test.getDureeEstimee() + " min");
                duree.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");

                meta.getChildren().addAll(badgeType, duree);

                vbox.getChildren().addAll(titre, meta);

                if (test.getDescriptionTest() != null && !test.getDescriptionTest().isEmpty()) {
                    Label desc = new Label(test.getDescriptionTest());
                    desc.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12px;");
                    desc.setWrapText(true);
                    desc.setMaxWidth(Double.MAX_VALUE);
                    vbox.getChildren().add(desc);
                }

                setGraphic(vbox);
                setText(null);
            }
        });

        // ── Nom utilisateur ───────────────────────────────────────────────────
        if (SessionManager.isLoggedIn() && SessionManager.getCurrentUser() != null && userNameLabel != null) {
            var u = SessionManager.getCurrentUser();
            userNameLabel.setText(u.getPrenom_utilisateur() + " " + u.getNom_utilisateur());
        }

        // ── Météo ─────────────────────────────────────────────────────────────
        new Thread(() -> {
            String[] meteo = MeteoService.getMeteo();
            Platform.runLater(() -> {
                meteoLabel.setText(meteo[0]);
                meteoMessageLabel.setText(meteo[1]);
            });
        }).start();

        // ── Citation ──────────────────────────────────────────────────────────
        rafraichirCitation();

        // ── Boutons prev/next ─────────────────────────────────────────────────
        if (btnPrev != null) btnPrev.setOnAction(e -> pagePrecedente());
        if (btnNext != null) btnNext.setOnAction(e -> pageSuivante());

        // ── Tests ─────────────────────────────────────────────────────────────
        chargerTests();

        // ── News ──────────────────────────────────────────────────────────────
        if (cmbThemeNews != null) {
            cmbThemeNews.getItems().addAll(NewsApiService.Theme.values());
            cmbThemeNews.setValue(NewsApiService.Theme.TOUS);
            chargerNews(NewsApiService.Theme.TOUS, null);
        }
    }

    // =========================================================================
    //  PAGINATION COMPACTE
    // =========================================================================

    /**
     * Recalcule et affiche la pagination dans le footer.
     * Génère des boutons numérotés compacts + met à jour btnPrev / btnNext.
     */
    private void updatePagination() {
        if (paginationContainer == null) return;

        int total      = listeFiltree.size();
        int totalPages = (total == 0) ? 1 : (int) Math.ceil((double) total / PAGE_SIZE);

        // Sécurité : page hors-bornes
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        // ── Afficher la page courante dans la ListView ────────────────────────
        int debut = currentPage * PAGE_SIZE;
        int fin   = Math.min(debut + PAGE_SIZE, total);
        listViewTests.setItems(FXCollections.observableArrayList(
                listeFiltree.subList(debut, fin)));

        // ── Boutons numérotés ─────────────────────────────────────────────────
        paginationContainer.getChildren().clear();

        for (int i = 0; i < totalPages; i++) {
            final int page = i;
            Button btn = new Button(String.valueOf(i + 1));

            if (i == currentPage) {
                btn.getStyleClass().add("btn-page-active");
            } else {
                btn.getStyleClass().add("btn-page");
                btn.setOnAction(e -> {
                    currentPage = page;
                    updatePagination();
                });
            }
            paginationContainer.getChildren().add(btn);
        }

        // ── Prev / Next ───────────────────────────────────────────────────────
        if (btnPrev != null) btnPrev.setDisable(currentPage == 0);
        if (btnNext != null) btnNext.setDisable(currentPage >= totalPages - 1);

        // ── Label info page ───────────────────────────────────────────────────
        if (lblPageInfo != null) {
            int affDebut = total == 0 ? 0 : debut + 1;
            int affFin   = fin;
            lblPageInfo.setText(
                    "Page " + (currentPage + 1) + " / " + totalPages +
                            "  ·  " + affDebut + "–" + affFin + " sur " + total
            );
        }

        // ── Cacher la pagination si 1 seule page ─────────────────────────────
        boolean visible = totalPages > 1;
        paginationContainer.setVisible(visible);
        paginationContainer.setManaged(visible);
        if (btnPrev != null) { btnPrev.setVisible(visible); btnPrev.setManaged(visible); }
        if (btnNext != null) { btnNext.setVisible(visible); btnNext.setManaged(visible); }
    }

    @FXML
    public void pagePrecedente() {
        if (currentPage > 0) {
            currentPage--;
            updatePagination();
        }
    }

    @FXML
    public void pageSuivante() {
        int totalPages = (int) Math.ceil((double) listeFiltree.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) {
            currentPage++;
            updatePagination();
        }
    }

    // =========================================================================
    //  TESTS
    // =========================================================================

    @FXML
    private void chargerTests() {
        TestPsychoService service = new TestPsychoService();
        try {
            listeTestsFull = service.afficherTestsActifs();
            listeFiltree   = new ArrayList<>(listeTestsFull);

            if (cmbFiltreType != null) {
                cmbFiltreType.getItems().clear();
                cmbFiltreType.getItems().add("Tous");
                for (TestPsycho t : listeTestsFull) {
                    if (t.getTypeTest() != null && !cmbFiltreType.getItems().contains(t.getTypeTest())) {
                        cmbFiltreType.getItems().add(t.getTypeTest());
                    }
                }
                cmbFiltreType.setValue("Tous");
                txtRecherche.textProperty().addListener((obs, o, n) -> filtrer());
                cmbFiltreType.valueProperty().addListener((obs, o, n) -> filtrer());
            }

            currentPage = 0;
            updatePagination();
            majNbResultats(listeTestsFull.size());

        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur chargement tests : " + e.getMessage()).show();
        }
    }

    private void filtrer() {
        String texte = txtRecherche.getText() == null ? "" : txtRecherche.getText().toLowerCase().trim();
        String type  = cmbFiltreType.getValue();

        listeFiltree = new ArrayList<>();
        for (TestPsycho t : listeTestsFull) {
            boolean okTexte = texte.isEmpty()
                    || (t.getTitreTest()       != null && t.getTitreTest().toLowerCase().contains(texte))
                    || (t.getDescriptionTest() != null && t.getDescriptionTest().toLowerCase().contains(texte));
            boolean okType  = type == null || "Tous".equals(type)
                    || (t.getTypeTest() != null && t.getTypeTest().equals(type));
            if (okTexte && okType) listeFiltree.add(t);
        }

        currentPage = 0;          // revenir à la page 1 après filtre
        updatePagination();
        majNbResultats(listeFiltree.size());
    }

    private void majNbResultats(int nb) {
        if (lblNbResultats != null) lblNbResultats.setText(nb + " test(s) trouvé(s)");
    }

    // =========================================================================
    //  NEWS API
    // =========================================================================

    @FXML
    public void rechercherNews() {
        String texte = txtRechercheNews != null ? txtRechercheNews.getText().trim() : "";
        NewsApiService.Theme theme = cmbThemeNews != null
                ? cmbThemeNews.getValue()
                : NewsApiService.Theme.TOUS;
        chargerNews(theme, texte.isEmpty() ? null : texte);
    }

    private void chargerNews(NewsApiService.Theme theme, String recherche) {
        if (newsContainer == null) return;
        newsContainer.getChildren().clear();
        if (newsLoading != null) newsLoading.setVisible(true);
        if (lblNewsErreur != null) lblNewsErreur.setText("");

        new Thread(() -> {
            List<NewsApiService.Article> articles = (recherche != null && !recherche.isEmpty())
                    ? NewsApiService.search(recherche, 6)
                    : NewsApiService.getArticles(theme, 6);

            Platform.runLater(() -> {
                if (newsLoading != null) newsLoading.setVisible(false);
                if (articles.isEmpty()) {
                    if (lblNewsErreur != null) lblNewsErreur.setText("Aucun article trouvé.");
                    return;
                }
                newsContainer.getChildren().clear();
                for (NewsApiService.Article a : articles)
                    newsContainer.getChildren().add(creerCarteArticle(a));
            });
        }).start();
    }

    private VBox creerCarteArticle(NewsApiService.Article article) {
        VBox carte = new VBox(8);
        carte.setPadding(new Insets(10, 12, 10, 12));
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #E8ECF0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;"
        );

        HBox meta = new HBox(8);
        meta.setAlignment(Pos.CENTER_LEFT);

        Label lblSource = new Label(article.source != null && !article.source.isEmpty()
                ? article.source : "Source inconnue");
        lblSource.setStyle(
                "-fx-font-size: 10px; -fx-font-weight: bold;" +
                        "-fx-text-fill: #065F46;" +
                        "-fx-background-color: #D1FAE5;" +
                        "-fx-background-radius: 20; -fx-padding: 2 8;"
        );

        Label lblDate = new Label("⏱ " + article.date);
        lblDate.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        meta.getChildren().addAll(lblSource, spacer, lblDate);

        Label lblTitre = new Label(article.titre);
        lblTitre.setFont(Font.font("System", FontWeight.BOLD, 12));
        lblTitre.setStyle("-fx-text-fill: #1A1A2E;");
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(Double.MAX_VALUE);

        Hyperlink lien = new Hyperlink("🔗 Lire l'article");
        lien.setStyle(
                "-fx-text-fill: #059669; -fx-font-size: 11px;" +
                        "-fx-font-weight: bold; -fx-border-color: transparent;"
        );
        lien.setOnAction(e -> {
            try { java.awt.Desktop.getDesktop().browse(new java.net.URI(article.url)); }
            catch (Exception ex) { System.err.println("Lien impossible : " + ex.getMessage()); }
        });

        carte.getChildren().addAll(meta, lblTitre, lien);
        return carte;
    }

    // =========================================================================
    //  CITATION
    // =========================================================================

    @FXML
    public void rafraichirCitation() {
        citationLabel.setText("⏳ Chargement...");
        auteurLabel.setText("");
        new Thread(() -> {
            String[] citation = QuotableService.getCitation();
            Platform.runLater(() -> {
                citationLabel.setText("\" " + citation[0] + " \"");
                auteurLabel.setText("— " + citation[1]);
            });
        }).start();
    }

    // =========================================================================
    //  ACTIONS FXML
    // =========================================================================

    @FXML
    void commencerTest(ActionEvent event) {
        TestPsycho testSelectionne = listViewTests.getSelectionModel().getSelectedItem();
        if (testSelectionne == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez sélectionner un test.").show();
            return;
        }
        if (parentHomeController != null) {
            parentHomeController.loadPasserTest(testSelectionne);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/PasserTest.fxml"));
                Parent root = loader.load();
                PasserTestController controller = loader.getController();
                controller.initTest(testSelectionne);
                listViewTests.getScene().setRoot(root);
            } catch (IOException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show();
            }
        }
    }

    @FXML void voirMesResultats(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.loadMesResultats();
        else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/MesResultats.fxml"));
                listViewTests.getScene().setRoot(root);
            } catch (IOException e) { new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show(); }
        }
    }

    @FXML void voirMesObjectifs(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.loadMesObjectifs();
        else {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/MesObjectifs.fxml"));
                listViewTests.getScene().setRoot(root);
            } catch (IOException e) { new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).show(); }
        }
    }

    @FXML void retourAccueil(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.restoreHomeAndScrollToServices();
        else retourMenuPrincipal(event);
    }

    @FXML void handleNavigateToChoisirTest(ActionEvent event) { /* déjà ici */ }

    @FXML void retourMenuPrincipal(ActionEvent event) {
        SessionManager.logout();
        NavigationManager.navigateTo("Login.fxml", "MindAura – Connexion",
                NavigationManager.getStage(listViewTests), false);
    }
}