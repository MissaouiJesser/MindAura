package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.traitement;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Etat;
import tn.esprit.services.traitement_service;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.utils.EmailService;
import tn.esprit.utils.ExportUtils;
import tn.esprit.utils.NotificationManager;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur de la liste des traitements.
 *
 * ✉️  NOUVEAU — JavaMail API :
 *   Chaque ligne du tableau dispose d'un bouton "📧 Rappel" qui envoie
 *   automatiquement un email de rappel HTML au patient concerné.
 *   L'envoi est asynchrone → l'UI ne se bloque pas.
 *
 *   Prérequis : le traitement doit être lié à un utilisateur (id_utilisateur non null).
 */
public class ListeTraitementsController implements Initializable, DashboardController.DashboardAware {

    @FXML private TableView<traitement>           traitementsTable;
    @FXML private TableColumn<traitement, String> colType, colObjectif, colDescription, colDateDebut, colDateFin, colEtat;
    @FXML private TableColumn<traitement, Void>   colActions;
    @FXML private TextField    searchField;
    @FXML private Label        countLabel;
    @FXML private ComboBox<String> filterEtat;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label        pageInfoLabel;
    @FXML private HBox         paginationBox;

    private static final int PAGE_SIZE = 10;
    private int currentPage = 0;

    private DashboardController dashboardController;
    private final traitement_service   service     = new traitement_service();
    private final utilisateurs_service userService = new utilisateurs_service();

    private ObservableList<traitement> allTraitements      = FXCollections.observableArrayList();
    private ObservableList<traitement> filteredTraitements = FXCollections.observableArrayList();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    /** Cache local des utilisateurs pour résoudre id → objet sans requête BDD répétée */
    private Map<String, utilisateurs> usersById = new HashMap<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupColumns();
        loadTraitements();
        preloadUsers();  // ← charge les utilisateurs en mémoire pour les rappels email
    }

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
        NotificationManager.setOwnerWindow(dc.getContentArea().getScene().getWindow());
    }

    // =========================================================================
    //  CHARGEMENT DES UTILISATEURS (cache pour les rappels)
    // =========================================================================

    /** Pré-charge tous les utilisateurs en mémoire (évite des requêtes BDD par ligne) */
    private void preloadUsers() {
        try {
            List<utilisateurs> users = userService.afficherList();
            usersById.clear();
            for (utilisateurs u : users) usersById.put(u.getId_utilisateur(), u);
        } catch (SQLException e) {
            System.err.println("[Traitements] Impossible de pré-charger les utilisateurs : " + e.getMessage());
        }
    }

    // =========================================================================
    //  STYLES HELPERS
    // =========================================================================

    private static final String STYLE_BTN_EDIT =
            "-fx-background-color: linear-gradient(to bottom, #EBF5FB, #D6EAF8);" +
                    "-fx-text-fill: #1A5276; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #AED6F1;" +
                    "-fx-border-width: 1.5; -fx-padding: 5 10 5 10; -fx-cursor: hand;";

    private static final String STYLE_BTN_EDIT_HOVER =
            "-fx-background-color: linear-gradient(to bottom, #D6EAF8, #AED6F1);" +
                    "-fx-text-fill: #154360; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #2E86C1;" +
                    "-fx-border-width: 1.5; -fx-padding: 5 10 5 10; -fx-cursor: hand; -fx-translate-y: -1;";

    private static final String STYLE_BTN_DEL =
            "-fx-background-color: linear-gradient(to bottom, #FDEDEC, #FAD7D3);" +
                    "-fx-text-fill: #922B21; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #F5B7B1;" +
                    "-fx-border-width: 1.5; -fx-padding: 5 10 5 10; -fx-cursor: hand;";

    private static final String STYLE_BTN_DEL_HOVER =
            "-fx-background-color: linear-gradient(to bottom, #FAD7D3, #F5B7B1);" +
                    "-fx-text-fill: #7B241C; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #C0392B;" +
                    "-fx-border-width: 1.5; -fx-padding: 5 10 5 10; -fx-cursor: hand; -fx-translate-y: -1;";

    // ✉️  Styles bouton Rappel Email
    private static final String STYLE_BTN_EMAIL =
            "-fx-background-color: linear-gradient(to bottom, #EAF6FF, #BEE3F8);" +
                    "-fx-text-fill: #1A365D; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #90CDF4;" +
                    "-fx-border-width: 1.5; -fx-padding: 5 10 5 10; -fx-cursor: hand;";

    private static final String STYLE_BTN_EMAIL_HOVER =
            "-fx-background-color: linear-gradient(to bottom, #BEE3F8, #90CDF4);" +
                    "-fx-text-fill: #1A365D; -fx-font-size: 11px; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #3182CE;" +
                    "-fx-border-width: 1.5; -fx-padding: 5 10 5 10; -fx-cursor: hand; -fx-translate-y: -1;";

    private Button createEditButton() {
        Button btn = new Button("✎ Modifier");
        btn.setStyle(STYLE_BTN_EDIT);
        btn.setOnMouseEntered(e -> btn.setStyle(STYLE_BTN_EDIT_HOVER));
        btn.setOnMouseExited(e  -> btn.setStyle(STYLE_BTN_EDIT));
        return btn;
    }

    private Button createDeleteButton() {
        Button btn = new Button("🗑 Supprimer");
        btn.setStyle(STYLE_BTN_DEL);
        btn.setOnMouseEntered(e -> btn.setStyle(STYLE_BTN_DEL_HOVER));
        btn.setOnMouseExited(e  -> btn.setStyle(STYLE_BTN_DEL));
        return btn;
    }

    /** ✉️  Crée le bouton d'envoi de rappel email */
    private Button createEmailButton() {
        Button btn = new Button("📧 Rappel");
        btn.setStyle(STYLE_BTN_EMAIL);
        btn.setOnMouseEntered(e -> btn.setStyle(STYLE_BTN_EMAIL_HOVER));
        btn.setOnMouseExited(e  -> btn.setStyle(STYLE_BTN_EMAIL));
        return btn;
    }

    // =========================================================================
    //  SETUP
    // =========================================================================

    private void setupFilters() {
        if (filterEtat != null) {
            filterEtat.setItems(FXCollections.observableArrayList(
                    "Tous les états", Etat.EN_COURS.getLibelle(), Etat.TERMINE.getLibelle(), Etat.SUSPENDU.getLibelle()));
            filterEtat.setValue("Tous les états");
            filterEtat.valueProperty().addListener((o,ov,nv) -> applyFilters());
        }
        if (filterType != null) {
            filterType.setItems(FXCollections.observableArrayList(
                    "Tous les types","Psychologique","Comportemental","Mixte"));
            filterType.setValue("Tous les types");
            filterType.valueProperty().addListener((o,ov,nv) -> applyFilters());
        }
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Date décroissante","Date croissante","Type A→Z","État","Objectif A→Z"));
            sortCombo.setValue("Date décroissante");
            sortCombo.valueProperty().addListener((o,ov,nv) -> applyFilters());
        }
    }

    private void setupColumns() {
        colType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getType_traitement()!=null?d.getValue().getType_traitement().getLibelle():"–"));
        colObjectif.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getObjectif_traitement()!=null?d.getValue().getObjectif_traitement().getLibelle():"–"));
        colDescription.setCellValueFactory(d -> {
            String desc = d.getValue().getDescription_traitement();
            if (desc == null || desc.isBlank()) return new javafx.beans.property.SimpleStringProperty("–");
            return new javafx.beans.property.SimpleStringProperty(desc.length()>60 ? desc.substring(0,57)+"…" : desc);
        });
        colDateDebut.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDate_debut_traitement()!=null ? sdf.format(d.getValue().getDate_debut_traitement()) : "–"));
        colDateFin.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDate_fin_traitement()!=null ? sdf.format(d.getValue().getDate_fin_traitement()) : "En cours"));
        colEtat.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colEtat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty||getTableRow()==null||getTableRow().getItem()==null){setGraphic(null);return;}
                traitement t = getTableRow().getItem();
                if (t.getEtat_traitement()==null){setGraphic(null);return;}
                String css = switch(t.getEtat_traitement()){
                    case EN_COURS->"badge-encours"; case TERMINE->"badge-termine"; case SUSPENDU->"badge-suspendu";
                };
                Label b = new Label(t.getEtat_traitement().getLibelle());
                b.getStyleClass().addAll("badge",css); setGraphic(b); setText(null);
            }
        });

        // ─── Colonne Actions : Modifier + Supprimer + ✉️  Rappel Email ──────────
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = createEditButton();
            private final Button btnDel   = createDeleteButton();
            private final Button btnEmail = createEmailButton();   // ← nouveau

            {
                btnEdit.setOnAction(e -> {
                    traitement t = getTableView().getItems().get(getIndex());
                    if (t != null) handleModifier(t);
                });
                btnDel.setOnAction(e -> {
                    traitement t = getTableView().getItems().get(getIndex());
                    if (t != null) handleSupprimer(t);
                });
                // ✉️  Action rappel email
                btnEmail.setOnAction(e -> {
                    traitement t = getTableView().getItems().get(getIndex());
                    if (t != null) handleEnvoyerRappel(t);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, btnEdit, btnDel, btnEmail);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });
    }

    // =========================================================================
    //  ✉️  ENVOI DU RAPPEL EMAIL
    // =========================================================================

    /**
     * Envoie un rappel de traitement par email au patient lié au traitement.
     * Si le traitement n'a pas d'utilisateur associé, affiche un avertissement.
     */
    private void handleEnvoyerRappel(traitement t) {
        // Vérifier qu'un utilisateur est associé au traitement
        String idUser = t.getId_utilisateur();
        if (idUser == null || idUser.isBlank()) {
            NotificationManager.warning("Aucun patient associé à ce traitement.\nImpossible d'envoyer un rappel.");
            return;
        }

        // Retrouver l'utilisateur dans le cache
        utilisateurs patient = usersById.get(idUser);
        if (patient == null) {
            NotificationManager.warning("Patient introuvable (id=" + idUser + ").\nVérifiez la base de données.");
            return;
        }

        // Email du patient
        String emailPatient = patient.getEmail_utilisateur();
        if (emailPatient == null || emailPatient.isBlank()) {
            NotificationManager.warning("Le patient n'a pas d'adresse email enregistrée.");
            return;
        }

        // Résoudre le nom du coach si présent
        String nomCoach = null;
        if (t.getId_coach() != null) {
            utilisateurs coach = usersById.get(t.getId_coach());
            if (coach != null)
                nomCoach = coach.getPrenom_utilisateur() + " " + coach.getNom_utilisateur();
        }

        // Confirmation avant envoi
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Envoyer un rappel");
        confirm.setHeaderText("📧 Rappel de traitement");
        confirm.setContentText(
                "Envoyer un rappel de traitement à :\n\n" +
                        "👤  " + patient.getPrenom_utilisateur() + " " + patient.getNom_utilisateur() + "\n" +
                        "📧  " + emailPatient + "\n\n" +
                        "Programme : " + (t.getType_traitement() != null ? t.getType_traitement().getLibelle() : "—") +
                        " / " + (t.getObjectif_traitement() != null ? t.getObjectif_traitement().getLibelle() : "—")
        );

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        // ✉️  Envoi asynchrone
        final String coachFinal = nomCoach;
        EmailService.envoyerRappelTraitement(
                emailPatient,
                patient.getPrenom_utilisateur(),
                t.getType_traitement()     != null ? t.getType_traitement().getLibelle()     : "—",
                t.getObjectif_traitement() != null ? t.getObjectif_traitement().getLibelle() : "—",
                t.getDate_debut_traitement(),
                t.getDate_fin_traitement(),
                t.getDescription_traitement(),
                coachFinal,
                // onSuccess → notification toast verte
                () -> NotificationManager.success(
                        "✅ Rappel envoyé à " + patient.getPrenom_utilisateur() + " (" + emailPatient + ")"),
                // onError → notification toast rouge
                err -> NotificationManager.error(
                        "❌ Échec de l'envoi : " + err)
        );

        // Toast immédiat pour informer l'admin que l'envoi est en cours
        NotificationManager.info("📤 Envoi du rappel en cours...");
    }

    // =========================================================================
    //  CHARGEMENT & FILTRES
    // =========================================================================

    private void loadTraitements() {
        try {
            allTraitements = FXCollections.observableArrayList(service.afficherList());
            applyFilters();
        } catch (SQLException e) {
            NotificationManager.error("Erreur chargement : " + e.getMessage());
        }
    }

    @FXML private void handleSearch() { currentPage=0; applyFilters(); }

    @FXML private void handleResetFilters() {
        if (searchField!=null) searchField.clear();
        if (filterEtat!=null) filterEtat.setValue("Tous les états");
        if (filterType!=null) filterType.setValue("Tous les types");
        if (sortCombo!=null) sortCombo.setValue("Date décroissante");
        applyFilters();
        NotificationManager.info("Filtres réinitialisés");
    }

    private void applyFilters() {
        String search = searchField!=null ? searchField.getText().trim().toLowerCase() : "";
        String etatF  = filterEtat!=null  ? filterEtat.getValue() : "Tous les états";
        String typeF  = filterType!=null  ? filterType.getValue() : "Tous les types";

        List<traitement> result = allTraitements.stream().filter(t -> {
            boolean matchSearch = search.isEmpty()
                    || (t.getType_traitement()!=null && t.getType_traitement().getLibelle().toLowerCase().contains(search))
                    || (t.getObjectif_traitement()!=null && t.getObjectif_traitement().getLibelle().toLowerCase().contains(search))
                    || (t.getDescription_traitement()!=null && t.getDescription_traitement().toLowerCase().contains(search))
                    || (t.getEtat_traitement()!=null && t.getEtat_traitement().getLibelle().toLowerCase().contains(search));
            boolean matchEtat = "Tous les états".equals(etatF) || (t.getEtat_traitement()!=null && t.getEtat_traitement().getLibelle().equals(etatF));
            boolean matchType = "Tous les types".equals(typeF) || (t.getType_traitement()!=null && t.getType_traitement().getLibelle().equals(typeF));
            return matchSearch && matchEtat && matchType;
        }).collect(Collectors.toList());

        if (sortCombo!=null) {
            String sort = sortCombo.getValue();
            if (sort==null) sort="Date décroissante";
            Comparator<traitement> comp = switch(sort) {
                case "Date croissante" -> (a,b) -> {
                    if (a.getDate_debut_traitement()==null) return 1;
                    if (b.getDate_debut_traitement()==null) return -1;
                    return a.getDate_debut_traitement().compareTo(b.getDate_debut_traitement());
                };
                case "Type A→Z" -> (a,b) -> {
                    String ta = a.getType_traitement()!=null?a.getType_traitement().getLibelle():"";
                    String tb = b.getType_traitement()!=null?b.getType_traitement().getLibelle():"";
                    return ta.compareTo(tb);
                };
                case "État" -> (a,b) -> {
                    String ea = a.getEtat_traitement()!=null?a.getEtat_traitement().getLibelle():"";
                    String eb = b.getEtat_traitement()!=null?b.getEtat_traitement().getLibelle():"";
                    return ea.compareTo(eb);
                };
                case "Objectif A→Z" -> (a,b) -> {
                    String oa = a.getObjectif_traitement()!=null?a.getObjectif_traitement().getLibelle():"";
                    String ob = b.getObjectif_traitement()!=null?b.getObjectif_traitement().getLibelle():"";
                    return oa.compareTo(ob);
                };
                default -> (a,b) -> {  // "Date décroissante"
                    if (a.getDate_debut_traitement()==null) return 1;
                    if (b.getDate_debut_traitement()==null) return -1;
                    return b.getDate_debut_traitement().compareTo(a.getDate_debut_traitement());
                };
            };
            result.sort(comp);
        }

        filteredTraitements = FXCollections.observableArrayList(result);
        currentPage = 0;
        refreshPage();
    }

    private void refreshPage() {
        int total = filteredTraitements.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages-1));
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        traitementsTable.setItems(FXCollections.observableArrayList(filteredTraitements.subList(from, to)));
        if (countLabel != null) countLabel.setText("(" + total + " traitement" + (total>1?"s":"") + ")");
        updatePagination(total, totalPages);
    }

    private void updatePagination(int total, int totalPages) {
        if (paginationBox == null) return;
        paginationBox.getChildren().clear();
        if (pageInfoLabel != null)
            pageInfoLabel.setText("Page "+(currentPage+1)+" / "+totalPages+"  •  "+total+" résultat"+(total>1?"s":""));

        Button prev = new Button("←");
        prev.getStyleClass().add("btn-page");
        prev.setDisable(currentPage==0);
        prev.setOnAction(e -> { currentPage--; refreshPage(); });
        paginationBox.getChildren().add(prev);

        for (int i = 0; i < totalPages; i++) {
            if (totalPages>7 && (i>1 && i<totalPages-2 && Math.abs(i-currentPage)>1)) {
                if (i==2||i==totalPages-3) {
                    Label d = new Label("…");
                    d.setStyle("-fx-padding:0 4 0 4;-fx-text-fill:#5A6475;");
                    paginationBox.getChildren().add(d);
                }
                continue;
            }
            final int page = i;
            Button btn = new Button(String.valueOf(i+1));
            btn.getStyleClass().add("btn-page");
            if (i==currentPage) btn.getStyleClass().add("btn-page-active");
            btn.setOnAction(e -> { currentPage=page; refreshPage(); });
            paginationBox.getChildren().add(btn);
        }

        Button next = new Button("→");
        next.getStyleClass().add("btn-page");
        next.setDisable(currentPage>=totalPages-1);
        next.setOnAction(e -> { currentPage++; refreshPage(); });
        paginationBox.getChildren().add(next);
    }

    // =========================================================================
    //  ACTIONS
    // =========================================================================

    @FXML private void handleAjouter() {
        if (dashboardController!=null)
            dashboardController.navigateTo("AjouterTraitement.fxml","Nouveau Traitement","Créer un programme");
    }

    @FXML private void handleExportCSV() {
        try {
            FileChooser fc = new FileChooser(); fc.setTitle("Exporter en CSV");
            fc.setInitialFileName(ExportUtils.getTimestampedName("traitements")+".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV","*.csv"));
            File f = fc.showSaveDialog((Stage)traitementsTable.getScene().getWindow());
            if (f!=null) {
                ExportUtils.exportTraitementsCSV(new ArrayList<>(filteredTraitements), f);
                NotificationManager.success("Export CSV réussi !");
            }
        } catch (Exception e) { NotificationManager.error("Erreur CSV : "+e.getMessage()); }
    }

    @FXML private void handleExportPDF() {
        try {
            FileChooser fc = new FileChooser(); fc.setTitle("Exporter en PDF");
            fc.setInitialFileName(ExportUtils.getTimestampedName("traitements")+".html");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML","*.html"));
            File f = fc.showSaveDialog((Stage)traitementsTable.getScene().getWindow());
            if (f!=null) {
                File ex = ExportUtils.exportTraitementsPDF(new ArrayList<>(filteredTraitements), f);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(ex);
                NotificationManager.success("Rapport généré ! Ctrl+P pour imprimer.");
            }
        } catch (Exception e) { NotificationManager.error("Erreur PDF : "+e.getMessage()); }
    }

    private void handleModifier(traitement t) {
        if (dashboardController==null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ModifierTraitement.fxml"));
            Node view = loader.load();
            ModifierTraitementController ctrl = loader.getController();
            ctrl.setDashboardController(dashboardController);
            ctrl.setTraitementToEdit(t);
            view.setOpacity(0);
            dashboardController.getContentArea().getChildren().setAll(view);
            if (view instanceof Region r) {
                r.prefWidthProperty().bind(dashboardController.getContentArea().widthProperty());
                r.prefHeightProperty().bind(dashboardController.getContentArea().heightProperty());
            }
            FadeTransition ftMod = new FadeTransition(Duration.millis(250), view);
            ftMod.setFromValue(0); ftMod.setToValue(1); ftMod.play();
            dashboardController.updateTopbar("Modifier le traitement",
                    t.getType_traitement()!=null?t.getType_traitement().getLibelle():"");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void handleSupprimer(traitement t) {
        String label = (t.getType_traitement()!=null?t.getType_traitement().getLibelle():"?")
                + " — " + (t.getObjectif_traitement()!=null?t.getObjectif_traitement().getLibelle():"?");
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer \""+label+"\" ?\nCette action est irréversible.");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get()==ButtonType.OK) {
            try {
                service.delete(t);
                loadTraitements();
                NotificationManager.success("Traitement supprimé.");
            } catch (SQLException e) { NotificationManager.error("Erreur : "+e.getMessage()); }
        }
    }
}