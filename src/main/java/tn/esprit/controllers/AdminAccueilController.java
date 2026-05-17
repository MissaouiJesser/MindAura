package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import tn.esprit.entities.Categorie;
import tn.esprit.entities.Reclamation;
import tn.esprit.services.CategorieService;
import tn.esprit.services.ReclamationService;
import tn.esprit.services.ReponseService;
import tn.esprit.services.utilisateurs_service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Espace Admin : liste des réclamations avec recherche et filtre.
 * S'intègre au Dashboard : sidebar reste affichée, navigation via DashboardController.
 *
 * Corrections apportées :
 * - categorieReclamation (String) remplacé par categorieId (int FK) → résolution via CategorieService
 * - idUtilisateur → utilisateurId
 * - idReclamation → idReclamation (inchangé mais vérifier getter)
 * - cbFiltreRecherche alimenté depuis la DB (noms des catégories)
 * - Filtre catégorie adapté pour comparer par nom résolu
 * - Recherche texte adaptée (cat → nom résolu)
 */
public class AdminAccueilController implements DashboardController.DashboardAware {

    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbFiltreRecherche;
    @FXML private ComboBox<String> cbFiltreStatut;
    @FXML private ListView<Reclamation> listReclamations;
    @FXML private Button btnRetourAccueil;
    @FXML private Button btnStatistiques;
    @FXML private Label lblMessage;
    @FXML private Label lblPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private ImageView imgLogo;

    private ReclamationService reclamationService;
    private ReponseService reponseService;
    private CategorieService categorieService;
    private utilisateurs_service utilisateursService;

    private Stage currentStage;
    private List<Reclamation> toutesLesReclamations;
    private List<Reclamation> reclamationsFiltrees;

    // Cache id → nomCategorie pour éviter N requêtes dans les cellules
    private Map<Integer, String> categorieCache = new HashMap<>();

    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 0;
    private String dernierTermeRecherche = "";

    // ---------------------------------------------------------------
    // INITIALISATION
    // ---------------------------------------------------------------

    @FXML
    public void initialize() {
        try {
            reclamationService  = new ReclamationService();
            reponseService      = new ReponseService();
            utilisateursService = new utilisateurs_service();
            categorieService    = new CategorieService();
        } catch (Exception e) {
            setMessage("Erreur de connexion.", "#c62828");
            return;
        }

        chargerLogo();
        chargerCategoriesEnCache();   // ← charge le cache AVANT de remplir le filtre
        configurerFiltres();
        configurerListView();

        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
                dernierTermeRecherche = newVal != null ? newVal : "";
                currentPage = 0;
                rafraichirListe();
            });
        }

        if (cbFiltreRecherche != null) {
            cbFiltreRecherche.setOnAction(e -> { currentPage = 0; rafraichirListe(); });
        }
        if (cbFiltreStatut != null) {
            cbFiltreStatut.setOnAction(e -> { currentPage = 0; rafraichirListe(); });
        }

        if (btnPrevPage != null) {
            btnPrevPage.setOnAction(e -> {
                if (currentPage > 0) { currentPage--; rafraichirListe(); }
            });
        }
        if (btnNextPage != null) {
            btnNextPage.setOnAction(e -> {
                if (reclamationsFiltrees != null) {
                    int totalPages = (int) Math.ceil(reclamationsFiltrees.size() / (double) ITEMS_PER_PAGE);
                    if (currentPage < totalPages - 1) { currentPage++; rafraichirListe(); }
                }
            });
        }

        if (btnRetourAccueil != null) {
            btnRetourAccueil.setOnAction(e -> retourAccueil());
        }
        if (btnStatistiques != null) {
            btnStatistiques.setOnAction(e -> ouvrirStatistiques());
        }

        chargerReclamations();
    }

    // ---------------------------------------------------------------
    // CACHE CATÉGORIES
    // ---------------------------------------------------------------

    /**
     * Charge toutes les catégories de la DB dans un Map<id, nom>.
     * Appelé une seule fois au démarrage.
     */
    private void chargerCategoriesEnCache() {
        categorieCache.clear();
        try {
            List<Categorie> categories = categorieService.afficherList();
            if (categories != null) {
                for (Categorie cat : categories) {
                    categorieCache.put(cat.getIdCategorie(), cat.getNomCategorie());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement catégories : " + e.getMessage());
        }
    }

    /**
     * Résout le nom d'une catégorie depuis son ID.
     * Retourne "AUTRE" si l'ID est inconnu.
     */
    private String getNomCategorie(int id) {
        return categorieCache.getOrDefault(id, "AUTRE");
    }

    // ---------------------------------------------------------------
    // LOGO
    // ---------------------------------------------------------------

    private void chargerLogo() {
        if (imgLogo == null) return;
        String[] paths = {"/images/logo.png", "/logo.png", "images/logo.png"};
        for (String path : paths) {
            try {
                InputStream is = getClass().getResourceAsStream(path);
                if (is != null) {
                    Image logo = new Image(is);
                    if (!logo.isError()) { imgLogo.setImage(logo); return; }
                }
            } catch (Exception ignored) {}
        }
    }

    // ---------------------------------------------------------------
    // FILTRES
    // ---------------------------------------------------------------

    /**
     * Alimente cbFiltreRecherche depuis le cache catégories (noms DB).
     * Fallback vers valeurs statiques si le cache est vide.
     */
    private void configurerFiltres() {
        if (cbFiltreRecherche != null) {
            cbFiltreRecherche.getItems().clear();
            cbFiltreRecherche.getItems().add("TOUS");

            if (!categorieCache.isEmpty()) {
                // Noms issus de la DB, triés alphabétiquement
                categorieCache.values().stream()
                        .sorted()
                        .forEach(nom -> cbFiltreRecherche.getItems().add(nom));
            } else {
                // Fallback statique
                cbFiltreRecherche.getItems().addAll("COACH", "PSYCHOLOGUE", "EVENEMENT", "AUTRE");
            }
            cbFiltreRecherche.setValue("TOUS");
        }

        if (cbFiltreStatut != null) {
            cbFiltreStatut.getItems().addAll("TOUS", "EN_ATTENTE", "EN_COURS", "TRAITEE");
            cbFiltreStatut.setValue("TOUS");
        }
    }

    // ---------------------------------------------------------------
    // LISTVIEW
    // ---------------------------------------------------------------

    private void configurerListView() {
        listReclamations.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Reclamation> call(ListView<Reclamation> list) {
                return new ListCell<>() {
                    private final VBox card   = new VBox(8);
                    private final Label lblAuteur = new Label();
                    private final Label lblSujet  = new Label();
                    private final Label lblDesc   = new Label();
                    private final HBox  badges    = new HBox(8);

                    {
                        card.setStyle(
                                "-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 8; " +
                                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
                                        "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
                        );
                        lblAuteur.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 11; -fx-font-weight: bold;");
                        lblSujet.setStyle("-fx-text-fill: #1A1A2E; -fx-font-size: 14; -fx-font-weight: bold;");
                        lblDesc.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 12; -fx-wrap-text: true;");
                        lblDesc.setMaxWidth(700);
                        card.getChildren().addAll(lblAuteur, lblSujet, lblDesc, badges);
                        setGraphic(card);
                    }

                    @Override
                    protected void updateItem(Reclamation r, boolean empty) {
                        super.updateItem(r, empty);
                        if (empty || r == null) { setGraphic(null); return; }

                        // ✅ utilisateurId (anciennement idUtilisateur)
                        String nomAuteur = utilisateursService != null
                                ? utilisateursService.getNomCompletById(String.valueOf(r.getUtilisateurId()))
                                : "Utilisateur";
                        lblAuteur.setText("Par " + nomAuteur);
                        lblSujet.setText(r.getSujetReclamation());

                        String desc = r.getDescriptionReclamation();
                        lblDesc.setText(desc != null && desc.length() > 120
                                ? desc.substring(0, 120) + "..." : desc);

                        badges.getChildren().clear();

                        // ✅ Résolution nom catégorie depuis l'ID (FK)
                        Label cat = new Label(getNomCategorie(r.getCategorieId()));
                        cat.setStyle("-fx-background-color: #7B5EA7; -fx-text-fill: white; " +
                                "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11;");

                        Label statut = new Label(r.getStatutReclamation());
                        statut.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; " +
                                "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11;");

                        int nb = 0;
                        try {
                            nb = reponseService.compterParReclamation(r.getIdReclamation());
                        } catch (SQLException ignored) {}
                        Label comm = new Label(nb + " réponse(s)");
                        comm.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 11;");

                        badges.getChildren().addAll(cat, statut, comm);
                        setGraphic(card);
                    }
                };
            }
        });

        listReclamations.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 1) {
                Reclamation r = listReclamations.getSelectionModel().getSelectedItem();
                if (r != null) ouvrirDetail(r);
            }
        });
    }

    // ---------------------------------------------------------------
    // CHARGEMENT & FILTRAGE
    // ---------------------------------------------------------------

    private void chargerReclamations() {
        try {
            toutesLesReclamations = reclamationService.afficherList();
            currentPage = 0;
            rafraichirListe();
        } catch (SQLException ex) {
            setMessage("Erreur chargement.", "#c62828");
        }
    }

    /**
     * Applique les filtres (texte, catégorie par nom, statut) + pagination.
     */
    private void rafraichirListe() {
        if (toutesLesReclamations == null) return;

        String recherche = dernierTermeRecherche != null
                ? dernierTermeRecherche.toLowerCase().trim() : "";
        String categorieFiltre = cbFiltreRecherche != null ? cbFiltreRecherche.getValue() : "TOUS";
        String statutFiltre    = cbFiltreStatut   != null ? cbFiltreStatut.getValue()    : "TOUS";

        reclamationsFiltrees = toutesLesReclamations.stream()
                // ✅ Filtre catégorie : comparer le nom résolu depuis le cache
                .filter(r -> {
                    if (categorieFiltre == null || "TOUS".equals(categorieFiltre)) return true;
                    return categorieFiltre.equalsIgnoreCase(getNomCategorie(r.getCategorieId()));
                })
                // Filtre statut (inchangé)
                .filter(r -> statutFiltre == null || "TOUS".equals(statutFiltre)
                        || r.getStatutReclamation().equals(statutFiltre))
                // ✅ Recherche texte : nom catégorie résolu depuis l'ID
                .filter(r -> {
                    if (recherche.isEmpty()) return true;
                    String sujet  = r.getSujetReclamation()        != null ? r.getSujetReclamation().toLowerCase()        : "";
                    String desc   = r.getDescriptionReclamation()  != null ? r.getDescriptionReclamation().toLowerCase()  : "";
                    String cat    = getNomCategorie(r.getCategorieId()).toLowerCase();
                    String stat   = r.getStatutReclamation()       != null ? r.getStatutReclamation().toLowerCase()       : "";
                    String idRec  = String.valueOf(r.getIdReclamation());
                    String idUser = String.valueOf(r.getUtilisateurId()); // ✅ utilisateurId
                    return sujet.contains(recherche) || desc.contains(recherche) || cat.contains(recherche)
                            || stat.contains(recherche) || idRec.contains(recherche) || idUser.contains(recherche);
                })
                .toList();

        int total      = reclamationsFiltrees.size();
        int totalPages = (int) Math.ceil(total / (double) ITEMS_PER_PAGE);

        if (totalPages == 0)             currentPage = 0;
        else if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, total);
        if (from < 0 || from > to) { from = 0; to = Math.min(ITEMS_PER_PAGE, total); }

        listReclamations.getItems().setAll(
                total == 0 ? List.of() : reclamationsFiltrees.subList(from, to)
        );

        // Labels informatifs
        String catLabel  = (categorieFiltre == null || "TOUS".equals(categorieFiltre)) ? "" : " en " + categorieFiltre;
        String statLabel = (statutFiltre    == null || "TOUS".equals(statutFiltre))    ? "" : " - Statut " + statutFiltre;

        setMessage(
                recherche.isEmpty()
                        ? (total == 0 ? "Aucune réclamation à traiter." + catLabel + statLabel
                        : total + " réclamation(s) à traiter" + catLabel + statLabel)
                        : (total == 0 ? "Aucune correspondance pour \"" + dernierTermeRecherche + "\"" + catLabel + statLabel
                        : total + " résultat(s) pour \"" + dernierTermeRecherche + "\"" + catLabel + statLabel),
                "#1A1A2E"
        );

        if (lblPage != null) {
            int pageAffichee = totalPages == 0 ? 0 : currentPage + 1;
            lblPage.setText("Page " + pageAffichee + "/" + Math.max(totalPages, 1));
        }
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 0 || totalPages <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(totalPages <= 1 || currentPage >= totalPages - 1);
    }

    // ---------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------

    private void ouvrirDetail(Reclamation r) {
        if (dashboardController != null) {
            Object ctrl = dashboardController.loadViewAndGet(
                    "AdminReclamationDetail.fxml",
                    "Détail réclamation #" + r.getIdReclamation(),
                    "Réclamation et réponses"
            );
            if (ctrl instanceof AdminReclamationDetailController detailCtrl) {
                detailCtrl.setReclamation(r);
                detailCtrl.setRetourCallback(() ->
                        dashboardController.navigateTo(
                                "AdminAccueil.fxml",
                                "Réclamations et Réponses",
                                "Gestion des réclamations"
                        )
                );
                Stage stage = dashboardController.getStage();
                detailCtrl.setCurrentStage(stage);
                javafx.application.Platform.runLater(() -> detailCtrl.afficher());
            }
            return;
        }
        // Mode standalone (sans Dashboard)
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminReclamationDetail.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) listReclamations.getScene().getWindow();
            currentStage = stage;
            AdminReclamationDetailController ctrl = loader.getController();
            ctrl.setReclamation(r);
            ctrl.setRetourCallback(this::retourListe);
            ctrl.setCurrentStage(stage);
            ctrl.afficher();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Gestion - Réclamation #" + r.getIdReclamation());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void retourListe() {
        if (dashboardController != null) {
            dashboardController.navigateTo("AdminAccueil.fxml", "Réclamations et Réponses", "Gestion des réclamations");
            return;
        }
        Stage stage = currentStage;
        if (stage == null && listReclamations != null && listReclamations.getScene() != null) {
            stage = (Stage) listReclamations.getScene().getWindow();
        }
        if (stage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminAccueil.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Espace Admin - Gestion des réclamations");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void ouvrirStatistiques() {
        if (dashboardController != null) {
            dashboardController.navigateTo("AdminStatistique.fxml", "Statistiques des réclamations", "Graphiques et indicateurs");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AdminStatistique.fxml"));
            Parent root = loader.load();
            Stage stage = currentStage != null ? currentStage : (Stage) listReclamations.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Statistiques des Réclamations");
        } catch (IOException ex) {
            ex.printStackTrace();
            setMessage("Erreur ouverture statistiques.", "#c62828");
        }
    }

    private void retourAccueil() {
        if (dashboardController != null) {
            dashboardController.navigateTo("DashboardHome.fxml", "Tableau de Bord", "Vue d'ensemble de MindAura");
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Accueil.fxml"));
            Stage stage = currentStage != null ? currentStage : (Stage) btnRetourAccueil.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Accueil");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // UTILITAIRE
    // ---------------------------------------------------------------

    private void setMessage(String msg, String couleur) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setStyle("-fx-text-fill: " + couleur + ";");
        }
    }
}