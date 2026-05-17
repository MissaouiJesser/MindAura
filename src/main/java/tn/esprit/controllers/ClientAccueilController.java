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
import tn.esprit.entities.Reclamation;
import tn.esprit.services.CategorieService;
import tn.esprit.services.ReclamationService;
import tn.esprit.services.ReponseService;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.entities.Categorie;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientAccueilController {

    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbFiltreRecherche;
    @FXML private ComboBox<String> cbFiltreStatut;
    @FXML private ListView<Reclamation> listReclamations;
    @FXML private Button btnRetourAccueil;
    @FXML private Label lblMessage;
    @FXML private Label lblPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private ImageView imgLogo;
    @FXML private Button btnNouvelleReclamation;
    @FXML private Button btnMesReclamations;

    private ReclamationService reclamationService;
    private ReponseService reponseService;
    private utilisateurs_service utilisateursService;
    private CategorieService categorieService;
    private Stage currentStage;
    private List<Reclamation> toutesLesReclamations;
    private List<Reclamation> reclamationsFiltrees;
    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 0;
    private String dernierTermeRecherche = "";

    // ✅ Cache catégories : id → nom
    private final Map<Integer, String> categorieCache = new HashMap<>();

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
            lblMessage.setText("Erreur de connexion.");
            return;
        }

        chargerCategoriesEnCache();
        chargerLogo();
        configurerFiltres();
        configurerListView();

        if (btnMesReclamations     != null) btnMesReclamations.setOnAction(e -> ouvrirMesReclamations());
        if (btnNouvelleReclamation != null) btnNouvelleReclamation.setOnAction(e -> ouvrirFormulaireAjout());
        if (btnRetourAccueil       != null) btnRetourAccueil.setOnAction(e -> retourAccueil());

        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
                dernierTermeRecherche = newVal != null ? newVal : "";
                currentPage = 0;
                rafraichirListe();
            });
        }
        if (cbFiltreRecherche != null) cbFiltreRecherche.setOnAction(e -> { currentPage = 0; rafraichirListe(); });
        if (cbFiltreStatut    != null) cbFiltreStatut.setOnAction(e -> { currentPage = 0; rafraichirListe(); });

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

        chargerReclamations();
    }

    // ---------------------------------------------------------------
    // CACHE CATÉGORIES
    // ---------------------------------------------------------------

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

    private String getNomCategorie(int id) {
        return categorieCache.getOrDefault(id, "AUTRE");
    }

    // ---------------------------------------------------------------
    // FORMULAIRE AJOUT
    // ---------------------------------------------------------------

    private void ouvrirFormulaireAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientAjouterReclamation.fxml"));
            Parent root = loader.load();
            ClientAjouterReclamationController formCtrl = loader.getController();
            formCtrl.setRetourCallback(this::chargerReclamations);
            formCtrl.setReclamationService(reclamationService);
            Stage stage = (Stage) btnNouvelleReclamation.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Ajouter une réclamation");
            stage.setMaximized(true);
        } catch (IOException ex) {
            lblMessage.setText("Erreur ouverture formulaire");
            ex.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // LOGO
    // ---------------------------------------------------------------

    private void chargerLogo() {
        if (imgLogo == null) return;
        String[] possiblePaths = { "/images/logo.png", "/logo.png", "images/logo.png" };
        for (String path : possiblePaths) {
            try {
                InputStream inputStream = getClass().getResourceAsStream(path);
                if (inputStream != null) {
                    Image logo = new Image(inputStream);
                    if (!logo.isError()) { imgLogo.setImage(logo); return; }
                }
            } catch (Exception ignored) {}
        }
    }

    // ---------------------------------------------------------------
    // FILTRES
    // ---------------------------------------------------------------

    private void configurerFiltres() {
        if (cbFiltreRecherche != null) {
            cbFiltreRecherche.getItems().addAll("TOUS", "COACH", "PSYCHOLOGUE", "EVENEMENT", "AUTRE");
            cbFiltreRecherche.setValue("TOUS");
        }
        if (cbFiltreStatut != null) {
            cbFiltreStatut.getItems().addAll("TOUS", "EN_ATTENTE", "EN_COURS", "TRAITEE");
            cbFiltreStatut.setValue("TOUS");
        }
    }

    // ---------------------------------------------------------------
    // LIST VIEW
    // ---------------------------------------------------------------

    private void configurerListView() {
        listReclamations.setCellFactory(new Callback<ListView<Reclamation>, ListCell<Reclamation>>() {
            @Override
            public ListCell<Reclamation> call(ListView<Reclamation> list) {
                return new ListCell<Reclamation>() {
                    private final VBox  card      = new VBox(8);
                    private final Label lblAuteur = new Label();
                    private final Label lblSujet  = new Label();
                    private final Label lblDesc   = new Label();
                    private final HBox  badges    = new HBox(8);

                    {
                        card.setStyle("-fx-background-color: white; -fx-padding: 16; -fx-background-radius: 8; " +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
                                "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
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

                        // ✅ CORRECTION : getUtilisateurId() (pas getIdUtilisateur())
                        String nomAuteur = utilisateursService != null
                                ? utilisateursService.getNomCompletById(String.valueOf(r.getUtilisateurId()))
                                : "Utilisateur";
                        lblAuteur.setText("Par " + nomAuteur);
                        lblSujet.setText(r.getSujetReclamation());

                        String desc = r.getDescriptionReclamation();
                        lblDesc.setText(desc != null && desc.length() > 120 ? desc.substring(0, 120) + "..." : desc);

                        badges.getChildren().clear();

                        // ✅ CORRECTION : getNomCategorie(getCategorieId()) (pas getCategorieReclamation())
                        Label cat = new Label(getNomCategorie(r.getCategorieId()));
                        cat.setStyle("-fx-background-color: #7B5EA7; -fx-text-fill: white; " +
                                "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11;");

                        Label statut = new Label(r.getStatutReclamation());
                        statut.setStyle("-fx-background-color: #2D6A4F; -fx-text-fill: white; " +
                                "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11;");

                        int nb = 0;
                        try { nb = reponseService.compterParReclamation(r.getIdReclamation()); }
                        catch (SQLException ignored) {}

                        Label comm = new Label(nb + " commentaire(s)");
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
    // CHARGEMENT
    // ---------------------------------------------------------------

    private void chargerReclamations() {
        try {
            toutesLesReclamations = reclamationService.afficherList();
            currentPage = 0;
            rafraichirListe();
        } catch (SQLException ex) {
            lblMessage.setText("Erreur chargement.");
        }
    }

    // ---------------------------------------------------------------
    // MES RÉCLAMATIONS
    // ---------------------------------------------------------------

    private void ouvrirMesReclamations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesReclamations.fxml"));
            Parent root = loader.load();
            Stage stage = currentStage != null ? currentStage
                    : (Stage) (listReclamations != null && listReclamations.getScene() != null
                    ? listReclamations.getScene().getWindow()
                    : btnMesReclamations.getScene().getWindow());
            currentStage = stage;
            MesReclamationsController ctrl = loader.getController();
            ctrl.setCurrentStage(stage);
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Mes Reclamations");
        } catch (IOException ex) {
            lblMessage.setText("Erreur ouverture Mes Reclamations.");
            ex.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // FILTRAGE + PAGINATION
    // ---------------------------------------------------------------

    private void rafraichirListe() {
        if (toutesLesReclamations == null) return;

        String recherche = dernierTermeRecherche != null ? dernierTermeRecherche.toLowerCase().trim() : "";
        String categorie = cbFiltreRecherche != null ? cbFiltreRecherche.getValue() : "TOUS";
        String statut    = cbFiltreStatut    != null ? cbFiltreStatut.getValue()    : "TOUS";

        reclamationsFiltrees = toutesLesReclamations.stream()
                // ✅ CORRECTION : filtrer par getNomCategorie(getCategorieId()) (pas getCategorieReclamation())
                .filter(r -> categorie == null || "TOUS".equals(categorie)
                        || getNomCategorie(r.getCategorieId()).equalsIgnoreCase(categorie))
                .filter(r -> statut == null || "TOUS".equals(statut)
                        || r.getStatutReclamation().equals(statut))
                .filter(r -> {
                    if (recherche.isEmpty()) return true;
                    String sujet = r.getSujetReclamation()        != null ? r.getSujetReclamation().toLowerCase()        : "";
                    String desc  = r.getDescriptionReclamation()  != null ? r.getDescriptionReclamation().toLowerCase()  : "";
                    // ✅ CORRECTION : utiliser getNomCategorie pour la recherche texte
                    String cat   = getNomCategorie(r.getCategorieId()).toLowerCase();
                    String stat  = r.getStatutReclamation()       != null ? r.getStatutReclamation().toLowerCase()       : "";
                    return sujet.contains(recherche) || desc.contains(recherche)
                            || cat.contains(recherche) || stat.contains(recherche);
                })
                .toList();

        int total      = reclamationsFiltrees.size();
        int totalPages = (int) Math.ceil(total / (double) ITEMS_PER_PAGE);
        if (totalPages == 0)            currentPage = 0;
        else if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, total);
        if (from < 0 || from > to) { from = 0; to = Math.min(ITEMS_PER_PAGE, total); }

        if (total == 0) listReclamations.getItems().clear();
        else            listReclamations.getItems().setAll(reclamationsFiltrees.subList(from, to));

        String categorieLabel = (categorie == null || "TOUS".equals(categorie)) ? "" : " en " + categorie;
        String statutLabel    = (statut    == null || "TOUS".equals(statut))    ? "" : " - Statut " + statut;

        if (lblMessage != null) {
            lblMessage.setText(recherche.isEmpty()
                    ? (total == 0 ? "Aucune réclamation." + categorieLabel + statutLabel
                    : total + " réclamation(s)" + categorieLabel + statutLabel)
                    : (total == 0 ? "Aucune correspondance pour \"" + dernierTermeRecherche + "\"" + categorieLabel + statutLabel
                    : total + " résultat(s) pour \"" + dernierTermeRecherche + "\"" + categorieLabel + statutLabel));
        }

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientReclamationDetail.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) listReclamations.getScene().getWindow();
            currentStage = stage;
            ClientReclamationDetailController ctrl = loader.getController();
            ctrl.setReclamation(r);
            ctrl.setRetourCallback(this::retourListe);
            ctrl.setCurrentStage(stage);
            ctrl.afficher();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Réclamation - Commentaires");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void retourListe() {
        Stage stage = currentStage;
        if (stage == null && listReclamations != null && listReclamations.getScene() != null)
            stage = (Stage) listReclamations.getScene().getWindow();
        if (stage == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientAccueil.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.getScene().getStylesheets().add("/app.css");
            stage.setTitle("Espace Client - Réclamations");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void retourAccueil() {
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
}