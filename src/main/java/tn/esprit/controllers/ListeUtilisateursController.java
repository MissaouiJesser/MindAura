package tn.esprit.controllers;

import javafx.animation.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.Role;
import tn.esprit.services.utilisateurs_service;
import tn.esprit.utils.ExportUtils;
import tn.esprit.utils.NotificationManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.awt.Desktop;

public class ListeUtilisateursController implements Initializable, DashboardController.DashboardAware {

    // ─── Table et colonnes ────────────────────────────────────────────────────
    @FXML private TableView<utilisateurs>           usersTable;
    @FXML private TableColumn<utilisateurs, Void>   colPhoto;
    @FXML private TableColumn<utilisateurs, String> colPrenom;
    @FXML private TableColumn<utilisateurs, String> colNom;
    @FXML private TableColumn<utilisateurs, String> colEmail;
    @FXML private TableColumn<utilisateurs, String> colTel;
    @FXML private TableColumn<utilisateurs, String> colRole;
    @FXML private TableColumn<utilisateurs, String> colStatut;
    @FXML private TableColumn<utilisateurs, Void>   colActions;

    // ─── Barre d'outils ───────────────────────────────────────────────────────
    @FXML private TextField  searchField;
    @FXML private Label      countLabel;
    @FXML private ComboBox<String> filterRole;
    @FXML private ComboBox<String> filterStatut;
    @FXML private ComboBox<String> sortCombo;

    // ─── Pagination ───────────────────────────────────────────────────────────
    @FXML private Label  pageInfoLabel;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private HBox   paginationBox;

    private static final int PAGE_SIZE = 8;
    private int currentPage = 0;

    private DashboardController dashboardController;
    private final utilisateurs_service service = new utilisateurs_service();
    private ObservableList<utilisateurs> allUsers     = FXCollections.observableArrayList();
    private ObservableList<utilisateurs> filteredUsers = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupFilters();
        setupColumns();
        loadUsers();
    }

    @Override public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
        NotificationManager.setOwnerWindow(dc.getContentArea().getScene().getWindow());
    }

    // =========================================================================
    //  STYLES HELPERS — boutons créés en Java avec styles inline robustes
    // =========================================================================

    /** Style de base pour le bouton Modifier */
    private static final String STYLE_BTN_EDIT =
            "-fx-background-color: linear-gradient(to bottom, #EBF5FB, #D6EAF8);" +
                    "-fx-text-fill: #1A5276;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-color: #AED6F1;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-padding: 6 14 6 14;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(36,113,163,0.12), 5, 0, 0, 1);";

    private static final String STYLE_BTN_EDIT_HOVER =
            "-fx-background-color: linear-gradient(to bottom, #D6EAF8, #AED6F1);" +
                    "-fx-text-fill: #154360;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-color: #2E86C1;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-padding: 6 14 6 14;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(36,113,163,0.30), 12, 0, 0, 4);" +
                    "-fx-translate-y: -1;";

    /** Style de base pour le bouton Supprimer */
    private static final String STYLE_BTN_DEL =
            "-fx-background-color: linear-gradient(to bottom, #FDEDEC, #FAD7D3);" +
                    "-fx-text-fill: #922B21;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-color: #F5B7B1;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-padding: 6 14 6 14;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(146,43,33,0.12), 5, 0, 0, 1);";

    private static final String STYLE_BTN_DEL_HOVER =
            "-fx-background-color: linear-gradient(to bottom, #FAD7D3, #F5B7B1);" +
                    "-fx-text-fill: #7B241C;" +
                    "-fx-font-size: 12px;" +
                    "-fx-font-weight: bold;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-radius: 8;" +
                    "-fx-border-color: #C0392B;" +
                    "-fx-border-width: 1.5;" +
                    "-fx-padding: 6 14 6 14;" +
                    "-fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(146,43,33,0.30), 12, 0, 0, 4);" +
                    "-fx-translate-y: -1;";

    /** Crée un bouton Modifier correctement stylisé */
    private Button createEditButton() {
        Button btn = new Button("✎  Modifier");
        btn.setStyle(STYLE_BTN_EDIT);
        btn.setOnMouseEntered(e -> btn.setStyle(STYLE_BTN_EDIT_HOVER));
        btn.setOnMouseExited(e  -> btn.setStyle(STYLE_BTN_EDIT));
        btn.setOnMousePressed(e -> btn.setStyle(STYLE_BTN_EDIT +
                "-fx-translate-y: 1; -fx-effect: dropshadow(gaussian, rgba(36,113,163,0.08), 3, 0, 0, 1);"));
        btn.setOnMouseReleased(e -> btn.setStyle(STYLE_BTN_EDIT));
        return btn;
    }

    /** Crée un bouton Supprimer correctement stylisé */
    private Button createDeleteButton() {
        Button btn = new Button("🗑  Supprimer");
        btn.setStyle(STYLE_BTN_DEL);
        btn.setOnMouseEntered(e -> btn.setStyle(STYLE_BTN_DEL_HOVER));
        btn.setOnMouseExited(e  -> btn.setStyle(STYLE_BTN_DEL));
        btn.setOnMousePressed(e -> btn.setStyle(STYLE_BTN_DEL +
                "-fx-translate-y: 1; -fx-effect: dropshadow(gaussian, rgba(146,43,33,0.08), 3, 0, 0, 1);"));
        btn.setOnMouseReleased(e -> btn.setStyle(STYLE_BTN_DEL));
        return btn;
    }

    // =========================================================================
    //  SETUP
    // =========================================================================

    private void setupFilters() {
        if (filterRole != null) {
            filterRole.setItems(FXCollections.observableArrayList(
                    "Tous les rôles",
                    Role.ROLE_ADMIN.getLibelle(), Role.ROLE_PSYCHOLOGUE.getLibelle(),
                    Role.ROLE_PATIENT.getLibelle(), Role.ROLE_COACH.getLibelle(), Role.ROLE_USER.getLibelle()
            ));
            filterRole.setValue("Tous les rôles");
            filterRole.valueProperty().addListener((o,ov,nv) -> applyFilters());
        }
        if (filterStatut != null) {
            filterStatut.setItems(FXCollections.observableArrayList("Tous", "Actif", "Inactif"));
            filterStatut.setValue("Tous");
            filterStatut.valueProperty().addListener((o,ov,nv) -> applyFilters());
        }
        if (sortCombo != null) {
            sortCombo.setItems(FXCollections.observableArrayList(
                    "Nom A→Z", "Nom Z→A", "Prénom A→Z", "Email A→Z", "Récents d'abord"
            ));
            sortCombo.setValue("Nom A→Z");
            sortCombo.valueProperty().addListener((o,ov,nv) -> applyFilters());
        }
    }

    private void setupColumns() {
        if (colPhoto != null) {
            colPhoto.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(null));
            colPhoto.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getTableRow()==null || getTableRow().getItem()==null) { setGraphic(null); return; }
                    setGraphic(buildPhotoWidget(getTableRow().getItem(), 34));
                }
            });
        }
        colPrenom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(safe(d.getValue().getPrenom_utilisateur())));
        colNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(safe(d.getValue().getNom_utilisateur())));
        colEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(safe(d.getValue().getEmail_utilisateur())));
        colTel.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getTelephone_utilisateur() != null ? d.getValue().getTelephone_utilisateur() : "–"));

        colRole.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty||getTableRow()==null||getTableRow().getItem()==null) { setGraphic(null); return; }
                utilisateurs u = getTableRow().getItem();
                Label b = new Label(u.getRole_utilisateur().getLibelle());
                String style = switch(u.getRole_utilisateur()) {
                    case ROLE_ADMIN       -> "badge-admin";
                    case ROLE_PSYCHOLOGUE -> "badge-psy";
                    case ROLE_COACH       -> "badge-coach";
                    case ROLE_PATIENT     -> "badge-patient";
                    default               -> "badge-patient";
                };
                b.getStyleClass().addAll("badge", style); setGraphic(b);
            }
        });

        colStatut.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty||getTableRow()==null||getTableRow().getItem()==null) { setGraphic(null); return; }
                utilisateurs u = getTableRow().getItem();
                Label b = new Label(u.isEst_actif_utilisateur() ? "Actif" : "Inactif");
                b.getStyleClass().addAll("badge", u.isEst_actif_utilisateur() ? "badge-actif" : "badge-inactif");
                setGraphic(b);
            }
        });

        // ── Colonne Actions : boutons stylisés avec inline styles + hover ──
        colActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                Button btnEdit = createEditButton();
                Button btnDel  = createDeleteButton();
                utilisateurs user = getTableView().getItems().get(getIndex());
                btnEdit.setOnAction(e -> handleModifier(user));
                btnDel.setOnAction(e  -> handleSupprimer(user));
                HBox box = new HBox(6, btnEdit, btnDel);
                box.setAlignment(Pos.CENTER_LEFT);
                setGraphic(box);
            }
        });
    }

    // =========================================================================
    //  CHARGEMENT + FILTRES + TRI + PAGINATION
    // =========================================================================

    public void loadUsers() {
        try {
            List<utilisateurs> list = service.afficherList();
            allUsers = FXCollections.observableArrayList(list);
            applyFilters();
        } catch (SQLException e) {
            NotificationManager.error("Impossible de charger les utilisateurs : " + e.getMessage());
        }
    }

    private void applyFilters() {
        String kw     = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String role   = filterRole   != null ? filterRole.getValue() : "Tous les rôles";
        String statut = filterStatut != null ? filterStatut.getValue() : "Tous";
        String sort   = sortCombo    != null ? sortCombo.getValue() : "Nom A→Z";

        List<utilisateurs> result = allUsers.stream()
                .filter(u -> kw.isEmpty()
                        || safe(u.getNom_utilisateur()).toLowerCase().contains(kw)
                        || safe(u.getPrenom_utilisateur()).toLowerCase().contains(kw)
                        || safe(u.getEmail_utilisateur()).toLowerCase().contains(kw)
                        || safe(u.getTelephone_utilisateur()).toLowerCase().contains(kw))
                .filter(u -> role == null || role.equals("Tous les rôles")
                        || u.getRole_utilisateur().getLibelle().equals(role))
                .filter(u -> {
                    if (statut == null || statut.equals("Tous")) return true;
                    return statut.equals("Actif") ? u.isEst_actif_utilisateur() : !u.isEst_actif_utilisateur();
                })
                .collect(Collectors.toList());

        if (sort != null) {
            Comparator<utilisateurs> comparator;
            switch (sort) {
                case "Nom Z→A":
                    comparator = (a, b) -> {
                        String na = a.getNom_utilisateur() != null ? a.getNom_utilisateur() : "";
                        String nb = b.getNom_utilisateur() != null ? b.getNom_utilisateur() : "";
                        return nb.compareTo(na);
                    };
                    break;
                case "Prénom A→Z":
                    comparator = (a, b) -> {
                        String pa = a.getPrenom_utilisateur() != null ? a.getPrenom_utilisateur() : "";
                        String pb = b.getPrenom_utilisateur() != null ? b.getPrenom_utilisateur() : "";
                        return pa.compareTo(pb);
                    };
                    break;
                case "Email A→Z":
                    comparator = (a, b) -> {
                        String ea = a.getEmail_utilisateur() != null ? a.getEmail_utilisateur() : "";
                        String eb = b.getEmail_utilisateur() != null ? b.getEmail_utilisateur() : "";
                        return ea.compareTo(eb);
                    };
                    break;
                case "Récents d'abord":
                    comparator = (a, b) -> {
                        if (a.getDate_inscription_utilisateur() == null) return 1;
                        if (b.getDate_inscription_utilisateur() == null) return -1;
                        return b.getDate_inscription_utilisateur().compareTo(a.getDate_inscription_utilisateur());
                    };
                    break;
                default:
                    comparator = (a, b) -> {
                        String na = a.getNom_utilisateur() != null ? a.getNom_utilisateur() : "";
                        String nb = b.getNom_utilisateur() != null ? b.getNom_utilisateur() : "";
                        return na.compareTo(nb);
                    };
            }
            result.sort(comparator);
        }

        filteredUsers = FXCollections.observableArrayList(result);
        currentPage = 0;
        refreshPage();
    }

    private void refreshPage() {
        int total = filteredUsers.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        currentPage = Math.max(0, Math.min(currentPage, totalPages - 1));

        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, total);
        usersTable.setItems(FXCollections.observableArrayList(filteredUsers.subList(from, to)));
        usersTable.refresh(); // Force le rafraîchissement des cellules pour afficher les images
        if (countLabel != null)
            countLabel.setText("(" + total + " utilisateur" + (total > 1 ? "s" : "") + ")");
        updatePagination(total, totalPages);
    }

    private void updatePagination(int total, int totalPages) {
        if (paginationBox == null) return;
        paginationBox.getChildren().clear();

        if (pageInfoLabel != null)
            pageInfoLabel.setText("Page " + (currentPage+1) + " / " + totalPages +
                    "  •  " + total + " résultat" + (total>1?"s":""));

        Button prev = new Button("←");
        prev.getStyleClass().add("btn-page");
        prev.setDisable(currentPage == 0);
        prev.setOnAction(e -> { currentPage--; refreshPage(); });
        paginationBox.getChildren().add(prev);

        for (int i = 0; i < totalPages; i++) {
            if (totalPages > 7 && (i > 1 && i < totalPages-2 && Math.abs(i - currentPage) > 1)) {
                if (i == 2 || i == totalPages-3) {
                    Label dots = new Label("…");
                    dots.setStyle("-fx-padding:0 4 0 4; -fx-text-fill:#5A6475;");
                    paginationBox.getChildren().add(dots);
                }
                continue;
            }
            final int page = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.getStyleClass().add("btn-page");
            if (i == currentPage) btn.getStyleClass().add("btn-page-active");
            btn.setOnAction(e -> { currentPage = page; refreshPage(); });
            paginationBox.getChildren().add(btn);
        }

        Button next = new Button("→");
        next.getStyleClass().add("btn-page");
        next.setDisable(currentPage >= totalPages - 1);
        next.setOnAction(e -> { currentPage++; refreshPage(); });
        paginationBox.getChildren().add(next);
    }

    // =========================================================================
    //  ACTIONS BARRE
    // =========================================================================

    @FXML private void handleSearch() { currentPage = 0; applyFilters(); }

    @FXML private void handleAjouter() {
        if (dashboardController != null)
            dashboardController.navigateTo("AjouterUtilisateur.fxml",
                    "Ajouter un Utilisateur", "Créer un nouveau compte");
    }

    @FXML private void handleExportCSV() {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Exporter en CSV");
            fc.setInitialFileName(ExportUtils.getTimestampedName("utilisateurs") + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            Stage stage = (Stage) usersTable.getScene().getWindow();
            File f = fc.showSaveDialog(stage);
            if (f != null) {
                ExportUtils.exportUsersCSV(new ArrayList<>(filteredUsers), f);
                NotificationManager.success("Export CSV réussi ! (" + filteredUsers.size() + " utilisateurs)");
            }
        } catch (Exception e) {
            NotificationManager.error("Erreur export CSV : " + e.getMessage());
        }
    }

    @FXML private void handleExportPDF() {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Exporter en PDF");
            fc.setInitialFileName(ExportUtils.getTimestampedName("utilisateurs") + ".html");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("HTML (impression)", "*.html"));
            Stage stage = (Stage) usersTable.getScene().getWindow();
            File f = fc.showSaveDialog(stage);
            if (f != null) {
                File exported = ExportUtils.exportUsersPDF(new ArrayList<>(filteredUsers), f);
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(exported);
                NotificationManager.success("Rapport généré ! Utilisez Ctrl+P pour imprimer en PDF.");
            }
        } catch (Exception e) {
            NotificationManager.error("Erreur export : " + e.getMessage());
        }
    }

    @FXML private void handleResetFilters() {
        if (searchField != null) searchField.clear();
        if (filterRole != null) filterRole.setValue("Tous les rôles");
        if (filterStatut != null) filterStatut.setValue("Tous");
        if (sortCombo != null) sortCombo.setValue("Nom A→Z");
        applyFilters();
        NotificationManager.info("Filtres réinitialisés");
    }

    // ─── Modifier ──────────────────────────────────────────────────────────────
    private void handleModifier(utilisateurs user) {
        if (dashboardController == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ModifierUtilisateur.fxml"));
            Node view = loader.load();
            ModifierUtilisateurController ctrl = loader.getController();
            ctrl.setDashboardController(dashboardController);
            ctrl.setUserToEdit(user);
            view.setOpacity(0);
            dashboardController.getContentArea().getChildren().setAll(view);
            if (view instanceof Region r) {
                r.prefWidthProperty().bind(dashboardController.getContentArea().widthProperty());
                r.prefHeightProperty().bind(dashboardController.getContentArea().heightProperty());
            }
            FadeTransition ft = new FadeTransition(Duration.millis(250), view);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            dashboardController.updateTopbar("Modifier l'utilisateur",
                    user.getPrenom_utilisateur() + " " + user.getNom_utilisateur());
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─── Supprimer ─────────────────────────────────────────────────────────────
    private void handleSupprimer(utilisateurs user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer \"" + user.getPrenom_utilisateur() + " " + user.getNom_utilisateur() + "\" ?\nCette action est irréversible.");
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try {
                service.delete(user);
                loadUsers();
                NotificationManager.success("Utilisateur supprimé avec succès.");
            } catch (SQLException e) {
                NotificationManager.error("Suppression impossible : " + e.getMessage());
            }
        }
    }

    // ─── Photo widget ──────────────────────────────────────────────────────────
    private Node buildPhotoWidget(utilisateurs u, int size) {
        String path = u.getPhoto_profil_utilisateur();
        if (path != null && !path.isBlank()) {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get("C:/Users/LOQ/Desktop/3A3/Semestre 2/PI DEV/MindAura_Sym_Integration_Finale/public/avatars/" + path);
                if (java.nio.file.Files.exists(p)) {
                    Image img = new Image(p.toUri().toString(), size, size, false, true,true);
                    ImageView iv = new ImageView(img);
                    iv.setFitWidth(size); iv.setFitHeight(size);
                    iv.setClip(new Circle(size/2.0, size/2.0, size/2.0));
                    return iv;
                }
            } catch (Exception ignored) {}
        }
        String pi = (u.getPrenom_utilisateur()!=null && !u.getPrenom_utilisateur().isEmpty())
                ? String.valueOf(u.getPrenom_utilisateur().charAt(0)).toUpperCase() : "?";
        String ni = (u.getNom_utilisateur()!=null && !u.getNom_utilisateur().isEmpty())
                ? String.valueOf(u.getNom_utilisateur().charAt(0)).toUpperCase() : "";
        Label l = new Label(pi+ni);
        l.setStyle("-fx-text-fill:#FFFFFF; -fx-font-size:"+(size/3)+"px; -fx-font-weight:bold;");
        StackPane sp = new StackPane(l);
        sp.setStyle("-fx-background-color:linear-gradient(to bottom right,#2D6A4F,#1B4332);"
                +"-fx-background-radius:50; -fx-min-width:"+size+"; -fx-min-height:"+size+";"
                +"-fx-pref-width:"+size+"; -fx-pref-height:"+size+";");
        return sp;
    }

    private String safe(String s) { return s != null ? s : ""; }
}