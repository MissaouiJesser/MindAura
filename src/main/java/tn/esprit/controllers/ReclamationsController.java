package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
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
import tn.esprit.utils.SessionManager;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur de l'espace Réclamations (espace client) chargé dans le centre de UserHome.
 *
 * Corrections :
 * - categorieReclamation (String) → categorieId (int FK) résolu via CategorieService cache
 * - idUtilisateur → utilisateurId
 * - cbFiltreRecherche alimenté depuis la DB
 * - Filtre + recherche texte adaptés au nom résolu
 */
public class ReclamationsController {

    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbFiltreRecherche;
    @FXML private ComboBox<String> cbFiltreStatut;
    @FXML private ListView<Reclamation> listReclamations;
    @FXML private Button btnRetourAccueil;
    @FXML private Button btnNouvelleReclamation;
    @FXML private Button btnLesReclamations;
    @FXML private Button btnMesReclamations;
    @FXML private Button btnDeconnexion;
    @FXML private Label lblMessage;
    @FXML private Label lblPage;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label sessionName;
    @FXML private Label sessionRole;
    @FXML private Label avatarInitials;
    @FXML private ImageView avatarImage;
    @FXML private HBox sessionBox;

    private UserHomeController parentHomeController;
    private ReclamationService reclamationService;
    private ReponseService reponseService;
    private CategorieService categorieService;
    private utilisateurs_service utilisateursService;

    // Cache id → nomCategorie
    private final Map<Integer, String> categorieCache = new HashMap<>();

    private List<Reclamation> toutesLesReclamations;
    private List<Reclamation> reclamationsFiltrees;
    private static final int ITEMS_PER_PAGE = 6;
    private int currentPage = 0;
    private String dernierTermeRecherche = "";

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

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
            if (lblMessage != null) lblMessage.setText("Erreur de connexion.");
            return;
        }

        chargerCategoriesEnCache();   // ← avant configurerFiltres()
        bindSessionManager();
        configurerFiltres();
        configurerListView();

        if (btnNouvelleReclamation != null) btnNouvelleReclamation.setOnAction(e -> ouvrirFormulaireAjout());
        if (btnLesReclamations    != null) btnLesReclamations.setOnAction(e -> chargerLesReclamations());
        if (btnMesReclamations    != null) btnMesReclamations.setOnAction(e -> ouvrirMesReclamations());

        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
                dernierTermeRecherche = newVal != null ? newVal : "";
                currentPage = 0;
                rafraichirListe();
            });
        }
        if (cbFiltreRecherche != null) cbFiltreRecherche.setOnAction(e -> { currentPage = 0; rafraichirListe(); });
        if (cbFiltreStatut    != null) cbFiltreStatut.setOnAction(e -> { currentPage = 0; rafraichirListe(); });

        if (btnPrevPage != null) btnPrevPage.setOnAction(e -> {
            if (currentPage > 0) { currentPage--; rafraichirListe(); }
        });
        if (btnNextPage != null) btnNextPage.setOnAction(e -> {
            if (reclamationsFiltrees != null) {
                int totalPages = (int) Math.ceil(reclamationsFiltrees.size() / (double) ITEMS_PER_PAGE);
                if (currentPage < totalPages - 1) { currentPage++; rafraichirListe(); }
            }
        });

        if (btnRetourAccueil != null) btnRetourAccueil.setOnAction(e -> retourAccueil());
        if (btnDeconnexion   != null) btnDeconnexion.setOnAction(e -> handleLogout());

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

    /** Résout le nom d'une catégorie depuis son ID. */
    private String getNomCategorie(int id) {
        return categorieCache.getOrDefault(id, "AUTRE");
    }

    // ---------------------------------------------------------------
    // SESSION
    // ---------------------------------------------------------------

    private void bindSessionManager() {
        if (sessionName     != null) sessionName.setText(SessionManager.getNomComplet());
        if (sessionRole     != null) sessionRole.setText(SessionManager.getRoleLibelle());
        if (avatarInitials  != null) avatarInitials.setText(SessionManager.getInitiales());

        String photoPath = SessionManager.getPhotoPath();
        if (avatarImage != null && photoPath != null && !photoPath.isEmpty()) {
            try (InputStream is = getClass().getResourceAsStream(
                    photoPath.startsWith("/") ? photoPath : "/" + photoPath)) {
                if (is != null) {
                    Image img = new Image(is);
                    if (!img.isError()) {
                        avatarImage.setImage(img);
                        avatarImage.setVisible(true);
                        if (avatarInitials != null) avatarInitials.setVisible(false);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        if (parentHomeController != null) {
            try {
                Stage stage = (Stage) (sessionBox != null
                        ? sessionBox.getScene()
                        : listReclamations.getScene()).getWindow();
                tn.esprit.utils.NavigationManager.navigateTo("Login.fxml", "MindAura – Connexion", stage, false);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void retourAccueil() {
        if (parentHomeController != null) parentHomeController.restoreHome();
    }

    // ---------------------------------------------------------------
    // FILTRES
    // ---------------------------------------------------------------

    /**
     * Alimente cbFiltreRecherche depuis le cache catégories (noms DB).
     * Fallback statique si le cache est vide.
     */
    private void configurerFiltres() {
        if (cbFiltreRecherche != null) {
            cbFiltreRecherche.getItems().clear();
            cbFiltreRecherche.getItems().add("TOUS");

            if (!categorieCache.isEmpty()) {
                categorieCache.values().stream()
                        .sorted()
                        .forEach(nom -> cbFiltreRecherche.getItems().add(nom));
            } else {
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
                    private final VBox  card      = new VBox(8);
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

                        // ✅ utilisateurId
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
    // CHARGEMENT & FILTRAGE
    // ---------------------------------------------------------------

    /** Charge uniquement les réclamations de l'utilisateur connecté. */
    private void chargerReclamations() {
        try {
            int idUser = getCurrentUserId();
            toutesLesReclamations = idUser > 0
                    ? reclamationService.afficherParUtilisateur(idUser)
                    : java.util.Collections.emptyList();
            currentPage = 0;
            rafraichirListe();
        } catch (SQLException ex) {
            if (lblMessage != null) lblMessage.setText("Erreur chargement.");
        }
    }

    /** Charge toutes les réclamations (bouton "Les Réclamations"). */
    @FXML
    private void chargerLesReclamations() {
        try {
            toutesLesReclamations = reclamationService.afficherList();
            currentPage = 0;
            rafraichirListe();
        } catch (SQLException ex) {
            if (lblMessage != null) lblMessage.setText("Erreur chargement.");
        }
    }

    private int getCurrentUserId() {
        if (!SessionManager.isLoggedIn() || SessionManager.getCurrentUser() == null) return 0;
        String idStr = SessionManager.getCurrentUser().getId_utilisateur();
        if (idStr == null || idStr.isEmpty()) return 0;
        try { return Integer.parseInt(idStr.trim()); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Applique les filtres (texte, catégorie par nom résolu, statut) + pagination.
     */
    private void rafraichirListe() {
        if (toutesLesReclamations == null) return;

        String recherche       = dernierTermeRecherche != null ? dernierTermeRecherche.toLowerCase().trim() : "";
        String categorieFiltre = cbFiltreRecherche != null ? cbFiltreRecherche.getValue() : "TOUS";
        String statutFiltre    = cbFiltreStatut    != null ? cbFiltreStatut.getValue()    : "TOUS";

        reclamationsFiltrees = toutesLesReclamations.stream()
                // ✅ Filtre catégorie : comparer le nom résolu
                .filter(r -> {
                    if (categorieFiltre == null || "TOUS".equals(categorieFiltre)) return true;
                    return categorieFiltre.equalsIgnoreCase(getNomCategorie(r.getCategorieId()));
                })
                .filter(r -> statutFiltre == null || "TOUS".equals(statutFiltre)
                        || r.getStatutReclamation().equals(statutFiltre))
                // ✅ Recherche texte : nom catégorie résolu depuis l'ID
                .filter(r -> {
                    if (recherche.isEmpty()) return true;
                    String sujet = r.getSujetReclamation()       != null ? r.getSujetReclamation().toLowerCase()       : "";
                    String desc  = r.getDescriptionReclamation() != null ? r.getDescriptionReclamation().toLowerCase() : "";
                    String cat   = getNomCategorie(r.getCategorieId()).toLowerCase();
                    String st    = r.getStatutReclamation()      != null ? r.getStatutReclamation().toLowerCase()      : "";
                    return sujet.contains(recherche) || desc.contains(recherche)
                            || cat.contains(recherche) || st.contains(recherche);
                })
                .toList();

        int total      = reclamationsFiltrees.size();
        int totalPages = (int) Math.ceil(total / (double) ITEMS_PER_PAGE);
        if (totalPages == 0)              currentPage = 0;
        else if (currentPage >= totalPages) currentPage = totalPages - 1;

        int from = currentPage * ITEMS_PER_PAGE;
        int to   = Math.min(from + ITEMS_PER_PAGE, total);
        if (from < 0 || from > to) { from = 0; to = Math.min(ITEMS_PER_PAGE, total); }

        listReclamations.getItems().setAll(
                total == 0 ? List.of() : reclamationsFiltrees.subList(from, to)
        );

        if (lblMessage != null) {
            String catL = (categorieFiltre == null || "TOUS".equals(categorieFiltre)) ? "" : " en " + categorieFiltre;
            String stL  = (statutFiltre    == null || "TOUS".equals(statutFiltre))    ? "" : " - Statut " + statutFiltre;
            lblMessage.setText(total == 0
                    ? "Aucune réclamation." + catL + stL
                    : total + " réclamation(s)" + catL + stL);
        }
        if (lblPage != null) {
            int pageAff = totalPages == 0 ? 0 : currentPage + 1;
            lblPage.setText("Page " + pageAff + "/" + Math.max(totalPages, 1));
        }
        if (btnPrevPage != null) btnPrevPage.setDisable(currentPage <= 0 || totalPages <= 1);
        if (btnNextPage != null) btnNextPage.setDisable(totalPages <= 1 || currentPage >= totalPages - 1);
    }

    // ---------------------------------------------------------------
    // NAVIGATION
    // ---------------------------------------------------------------

    @FXML
    private void ouvrirFormulaireAjout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientAjouterReclamation.fxml"));
            Parent root = loader.load();
            ClientAjouterReclamationController formCtrl = loader.getController();
            formCtrl.setRetourCallback(this::chargerReclamations);
            formCtrl.setReclamationService(reclamationService);
            formCtrl.setUserId(getCurrentUserId());

            Stage formStage = new Stage();
            formStage.setTitle("Ajouter une réclamation");
            formStage.setScene(new javafx.scene.Scene(root));
            formStage.getScene().getStylesheets().add("/app.css");
            formStage.setWidth(500);
            formStage.setHeight(600);
            formStage.show();
        } catch (IOException ex) {
            if (lblMessage != null) lblMessage.setText("Erreur ouverture formulaire");
            ex.printStackTrace();
        }
    }

    @FXML
    private void ouvrirMesReclamations() {
        if (parentHomeController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MesReclamations.fxml"));
            Parent root = loader.load();
            MesReclamationsController ctrl = loader.getController();
            ctrl.setRetourVersListeCallback(() -> parentHomeController.loadInCenter("/Reclamations.fxml"));
            ctrl.setParentHomeController(parentHomeController);
            parentHomeController.setCenterContent(root);
        } catch (IOException ex) {
            if (lblMessage != null) lblMessage.setText("Erreur ouverture Mes Reclamations.");
            ex.printStackTrace();
        }
    }

    private void ouvrirDetail(Reclamation r) {
        if (parentHomeController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ClientReclamationDetail.fxml"));
            Parent root = loader.load();
            ClientReclamationDetailController ctrl = loader.getController();
            ctrl.setReclamation(r);
            ctrl.setRetourCallback(() -> parentHomeController.loadInCenter("/Reclamations.fxml"));
            ctrl.setParentHomeController(parentHomeController);
            Stage stage = (Stage) listReclamations.getScene().getWindow();
            ctrl.setCurrentStage(stage);
            ctrl.afficher();
            parentHomeController.setCenterContent(root);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}