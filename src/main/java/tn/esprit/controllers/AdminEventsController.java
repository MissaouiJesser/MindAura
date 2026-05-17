package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.Evenements;
import tn.esprit.services.EvenementService;
import tn.esprit.utils.FileStorageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminEventsController implements DashboardController.DashboardAware {

    @FXML private FlowPane eventsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private HBox statsRow;

    private final EvenementService evenementService = new EvenementService();
    private DashboardController dashboardController;
    private List<Evenements> allEvents;

    // ── Palette ──────────────────────────────────────────────
    private static final String C_PRIMARY = "#1B4332";
    private static final String C_ACCENT  = "#2D6A4F";
    private static final String C_HOVER   = "#40916C";
    private static final String C_VIOLET  = "#7B5EA7";
    private static final String C_TEXT    = "#1A1A2E";
    private static final String C_SUBTEXT = "#5A6475";
    private static final String C_WHITE   = "#FFFFFF";
    private static final String C_CARD_BG = "#FFFFFF";
    private static final String C_BORDER  = "#E2E8F0";

    // =========================================================================
    //  RESOLUTION IMAGE
    //  Utilise FileStorageUtil.SYMFONY_UPLOADS_PATH comme référence unique.
    //  Supporte 2 formats stockés en base :
    //    1. Nom de fichier seul  : "1712345678_photo.jpg"  (format actuel)
    //    2. Chemin absolu ancien : "C:\AppData\...\photo.jpg" (migration)
    // =========================================================================

    private File resolveImageFile(String imgValue) {
        if (imgValue == null || imgValue.trim().isEmpty()) return null;

        // Format 1 : chemin absolu déjà existant sur le disque
        File absolute = new File(imgValue);
        if (absolute.isAbsolute() && absolute.exists()) return absolute;

        // Format 2 : nom de fichier seul → chercher dans le dossier Symfony uploads
        File symfony = Paths.get(FileStorageUtil.SYMFONY_UPLOADS_PATH, imgValue).toFile();
        if (symfony.exists()) return symfony;

        // Format 3 : ancien chemin absolu dont le fichier a été migré
        // → extraire le nom et chercher dans Symfony
        String fileName = absolute.getName();
        if (!fileName.isEmpty()) {
            File migrated = Paths.get(FileStorageUtil.SYMFONY_UPLOADS_PATH, fileName).toFile();
            if (migrated.exists()) return migrated;
        }

        return null;
    }

    // =========================================================================
    //  INIT
    // =========================================================================

    @FXML
    public void initialize() throws SQLException {
        filterCombo.getItems().addAll(
                "Titre A -> Z",
                "Titre Z -> A",
                "Date debut (recent)",
                "Date debut (ancien)",
                "Capacite (desc)",
                "Capacite (asc)"
        );
        loadData();
    }

    @Override
    public void setDashboardController(DashboardController c) { this.dashboardController = c; }

    private void loadData() throws SQLException {
        allEvents = evenementService.getAllEvenements();
        renderStats();
        renderEvents(allEvents);
    }

    @FXML
    public void refreshEvents() throws SQLException {
        searchField.clear();
        filterCombo.getSelectionModel().clearSelection();
        loadData();
    }

    @FXML
    public void goToAddEvent() {
        if (dashboardController != null)
            dashboardController.loadView("EventForm.fxml", "Creer un Evenement", "Nouvel evenement");
    }

    // =========================================================================
    //  STATS
    // =========================================================================

    private void renderStats() {
        statsRow.getChildren().clear();

        int total         = allEvents.size();
        int totalCapacity = allEvents.stream().mapToInt(Evenements::getCapacite_evenement).sum();
        long actifs       = allEvents.stream()
                .filter(e -> e.getStatut_evenemnt() != null
                        && e.getStatut_evenemnt().equalsIgnoreCase("actif"))
                .count();

        statsRow.getChildren().addAll(
                createStatCard("📅", String.valueOf(total),         "Evenements",    C_PRIMARY, "#E8F5EE"),
                createStatCard("👥", String.valueOf(totalCapacity), "Places totales", C_VIOLET, "#F0ECF8"),
                createStatCard("✅", String.valueOf(actifs),        "Actifs",         C_HOVER,  "#E6F4ED")
        );

        int delay = 0;
        for (Node n : statsRow.getChildren()) { animateIn(n, delay); delay += 80; }
    }

    private HBox createStatCard(String icon, String value, String label,
                                String color, String bgLight) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(0);
        card.setStyle(
                "-fx-background-color: " + C_WHITE + ";" +
                        "-fx-background-radius: 16;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 16;" +
                        "-fx-padding: 18 22 18 22;" +
                        "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.08), 12, 0, 0, 3);"
        );

        StackPane iconBox = new StackPane();
        iconBox.setStyle(
                "-fx-background-color: " + bgLight + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-min-width: 48; -fx-min-height: 48;" +
                        "-fx-max-width: 48; -fx-max-height: 48;"
        );
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(ico);

        VBox info = new VBox(4);
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 28px; -fx-font-weight: 900; -fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + C_SUBTEXT + "; -fx-font-weight: 600;");
        info.getChildren().addAll(val, lbl);
        card.getChildren().addAll(iconBox, info);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, rgba(27,67,50,0.08), 12, 0, 0, 3)",
                "dropshadow(gaussian, rgba(27,67,50,0.18), 20, 0, 0, 6)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, rgba(27,67,50,0.18), 20, 0, 0, 6)",
                "dropshadow(gaussian, rgba(27,67,50,0.08), 12, 0, 0, 3)")));
        return card;
    }

    // =========================================================================
    //  FILTRES
    // =========================================================================

    @FXML public void searchEvents() { applyFilters(); }
    @FXML public void sortEvents()   { applyFilters(); }

    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String sort    = filterCombo.getSelectionModel().getSelectedItem();

        List<Evenements> filtered = allEvents.stream()
                .filter(e -> keyword.isEmpty()
                        || contains(e.getTitre_evenement(),       keyword)
                        || contains(e.getLieu_evenement(),        keyword)
                        || contains(e.getTypeEvenement(),         keyword)
                        || contains(e.getStatut_evenemnt(),       keyword)
                        || contains(e.getDescription_evenement(), keyword))
                .collect(Collectors.toList());

        if (sort != null) switch (sort) {
            case "Titre A -> Z"        -> filtered.sort(Comparator.comparing(
                    Evenements::getTitre_evenement, String.CASE_INSENSITIVE_ORDER));
            case "Titre Z -> A"        -> filtered.sort(Comparator.comparing(
                    Evenements::getTitre_evenement, String.CASE_INSENSITIVE_ORDER).reversed());
            case "Date debut (recent)" -> filtered.sort((a, b) -> {
                if (a.getDatedebut_evenemnt() == null) return 1;
                if (b.getDatedebut_evenemnt() == null) return -1;
                return b.getDatedebut_evenemnt().compareTo(a.getDatedebut_evenemnt());
            });
            case "Date debut (ancien)" -> filtered.sort((a, b) -> {
                if (a.getDatedebut_evenemnt() == null) return 1;
                if (b.getDatedebut_evenemnt() == null) return -1;
                return a.getDatedebut_evenemnt().compareTo(b.getDatedebut_evenemnt());
            });
            case "Capacite (desc)"     -> filtered.sort((a, b) ->
                    b.getCapacite_evenement() - a.getCapacite_evenement());
            case "Capacite (asc)"      -> filtered.sort(
                    Comparator.comparingInt(Evenements::getCapacite_evenement));
        }
        renderEvents(filtered);
    }

    private boolean contains(String s, String kw) {
        return s != null && s.toLowerCase().contains(kw);
    }

    // =========================================================================
    //  RENDER CARDS
    // =========================================================================

    private void renderEvents(List<Evenements> events) {
        eventsContainer.getChildren().clear();

        if (events.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(80));
            Label ico = new Label("📭");
            ico.setStyle("-fx-font-size: 52px;");
            Label msg = new Label("Aucun evenement trouve");
            msg.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 16px; -fx-font-weight: 700;");
            Label sub = new Label("Essayez d'autres mots-cles ou modifiez le filtre");
            sub.setStyle("-fx-text-fill: " + C_SUBTEXT + "; -fx-font-size: 13px;");
            empty.getChildren().addAll(ico, msg, sub);
            eventsContainer.getChildren().add(empty);
            return;
        }

        int i = 0;
        for (Evenements e : events) {
            Node card = buildEventCard(e);
            eventsContainer.getChildren().add(card);
            animateCardIn(card, i * 45);
            i++;
        }
    }

    private Node buildEventCard(Evenements event) {
        VBox card = new VBox(0);
        card.setPrefWidth(290);
        card.setMaxWidth(290);
        card.setMinWidth(260);
        card.setStyle(
                "-fx-background-color: " + C_CARD_BG + ";" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 18;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.10), 16, 0, 0, 4);"
        );

        // ── Bannière image / placeholder ──
        // resolveImageFile() supporte les 2 formats (nom seul + chemin absolu)
        File imgFile = FileStorageUtil.resolveImageFile(event.getImage());
        boolean hasImage  = imgFile != null;
        String accentColor = getTypeColor(event.getTypeEvenement());

        if (hasImage) {
            try {
                Image img = new Image(new FileInputStream(imgFile), 290, 150, false, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(290); iv.setFitHeight(150);
                iv.setPreserveRatio(false); iv.setSmooth(true);

                StackPane imgPane = new StackPane(iv);
                imgPane.setMinHeight(150); imgPane.setMaxHeight(150);
                imgPane.setStyle("-fx-background-radius: 18 18 0 0;");

                Region gradient = new Region();
                gradient.setStyle(
                        "-fx-background-color: linear-gradient(to bottom, transparent 35%," +
                                " rgba(26,26,46,0.75) 100%); -fx-background-radius: 0;");
                gradient.setPrefSize(290, 150);

                addTypeBadge(imgPane, event.getTypeEvenement(), accentColor, true);
                imgPane.getChildren().add(gradient);
                card.getChildren().add(imgPane);
            } catch (Exception ignored) {
                // En cas d'erreur de lecture, afficher le placeholder
                card.getChildren().add(buildPlaceholder(event, accentColor));
            }
        } else {
            card.getChildren().add(buildPlaceholder(event, accentColor));
        }

        // ── Corps ──
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 16, 18));
        VBox.setVgrow(body, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.TOP_LEFT);
        Label title = new Label(event.getTitre_evenement() != null
                ? event.getTitre_evenement() : "-");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: "
                + C_TEXT + "; -fx-wrap-text: true;");
        title.setMaxWidth(195);
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);
        titleRow.getChildren().add(title);
        if (event.getStatut_evenemnt() != null && !event.getStatut_evenemnt().isEmpty())
            titleRow.getChildren().add(buildStatusBadge(event.getStatut_evenemnt()));

        Label dateLabel = buildInfoRow("📅", formatDate(event));
        Label lieuLabel = buildInfoRow("📍", event.getLieu_evenement() != null
                ? event.getLieu_evenement() : "-");
        boolean isFull  = event.getCapacite_evenement() <= 0;
        Label capLabel  = buildInfoRow(isFull ? "🔴" : "👥",
                isFull ? "Complet" : event.getCapacite_evenement() + " places disponibles");
        if (isFull) capLabel.setStyle(capLabel.getStyle() + " -fx-text-fill: #DC2626;");

        Region sep = new Region();
        sep.setStyle("-fx-background-color: " + C_BORDER + ";" +
                "-fx-pref-height:1; -fx-max-height:1;");
        VBox.setMargin(sep, new Insets(4, 0, 4, 0));

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✏  Modifier");
        editBtn.setStyle(
                "-fx-background-color: linear-gradient(to bottom, " + C_ACCENT + ", " + C_PRIMARY + ");" +
                        "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 16 8 16;"
        );
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.setOnMouseEntered(e -> editBtn.setStyle(editBtn.getStyle()
                .replace(C_ACCENT + ", " + C_PRIMARY, C_HOVER + ", " + C_ACCENT)));
        editBtn.setOnMouseExited(e -> editBtn.setStyle(editBtn.getStyle()
                .replace(C_HOVER + ", " + C_ACCENT, C_ACCENT + ", " + C_PRIMARY)));
        editBtn.setOnAction(e -> {
            if (dashboardController != null) {
                Object ctrl = dashboardController.loadViewAndGet(
                        "EventForm.fxml",
                        "Modifier l'evenement",
                        "Formulaire de modification"
                );
                if (ctrl instanceof EventFormController formCtrl)
                    formCtrl.setEvent(event);
            }
        });

        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;" +
                        "-fx-border-color: #FECACA; -fx-border-width: 1.5; -fx-border-radius: 10;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 12 8 12;" +
                        "-fx-font-size: 14px;"
        );
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(deleteBtn.getStyle()
                .replace("#FEE2E2", "#991B1B")
                .replace("#991B1B; -fx-border", "white; -fx-border")));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;" +
                        "-fx-border-color: #FECACA; -fx-border-width: 1.5; -fx-border-radius: 10;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 12 8 12;" +
                        "-fx-font-size: 14px;"
        ));
        deleteBtn.setOnAction(e -> {
            try { confirmDelete(event); }
            catch (SQLException ex) { throw new RuntimeException(ex); }
        });

        actions.getChildren().addAll(editBtn, deleteBtn);
        body.getChildren().addAll(titleRow, dateLabel, lieuLabel, capLabel, sep, actions);
        card.getChildren().add(body);

        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, rgba(27,67,50,0.10), 16, 0, 0, 4)",
                "dropshadow(gaussian, rgba(27,67,50,0.22), 28, 0, -2, 8)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, rgba(27,67,50,0.22), 28, 0, -2, 8)",
                "dropshadow(gaussian, rgba(27,67,50,0.10), 16, 0, 0, 4)")));

        return card;
    }

    /** Construit le placeholder quand aucune image n'est disponible. */
    private StackPane buildPlaceholder(Evenements event, String accentColor) {
        StackPane placeholder = new StackPane();
        placeholder.setMinHeight(150); placeholder.setMaxHeight(150);
        placeholder.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " +
                        accentColor + "28, " + accentColor + "0D);" +
                        "-fx-background-radius: 18 18 0 0;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 0 0 1.5 0;"
        );
        Label emojiLbl = new Label(getTypeEmoji(event.getTypeEvenement()));
        emojiLbl.setStyle("-fx-font-size: 42px;");
        placeholder.getChildren().add(emojiLbl);
        addTypeBadge(placeholder, event.getTypeEvenement(), accentColor, false);
        return placeholder;
    }

    private void addTypeBadge(StackPane pane, String type, String color, boolean onImage) {
        if (type == null || type.isEmpty()) return;
        Label badge = new Label(type);
        String bg = onImage ? "rgba(0,0,0,0.50)" : color + "28";
        String fg = onImage ? "white" : color;
        badge.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-background-radius: 20; -fx-padding: 4 12 4 12;" +
                        "-fx-text-fill: " + fg + "; -fx-font-size: 10.5px; -fx-font-weight: 700;" +
                        (onImage ? "" : "-fx-border-color: " + color + "55;" +
                                "-fx-border-width:1; -fx-border-radius:20;")
        );
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(10, 0, 0, 12));
        pane.getChildren().add(badge);
    }

    private Label buildInfoRow(String emoji, String text) {
        Label lbl = new Label(emoji + "  " + text);
        lbl.setStyle("-fx-font-size: 12.5px; -fx-text-fill: " + C_SUBTEXT + "; -fx-font-weight: 500;");
        lbl.setWrapText(true);
        return lbl;
    }

    private Label buildStatusBadge(String status) {
        String color = getStatusColor(status);
        Label badge = new Label(status);
        badge.setStyle(
                "-fx-background-color: " + color + "1A;" +
                        "-fx-border-color: " + color + "55;" +
                        "-fx-border-width: 1; -fx-border-radius: 20;" +
                        "-fx-background-radius: 20; -fx-padding: 3 10 3 10;" +
                        "-fx-text-fill: " + color + "; -fx-font-size: 10px; -fx-font-weight: 700;"
        );
        return badge;
    }

    private String formatDate(Evenements e) {
        if (e.getDatedebut_evenemnt() != null && e.getDatefin_evenemnt() != null)
            return e.getDatedebut_evenemnt() + " -> " + e.getDatefin_evenemnt();
        if (e.getDatedebut_evenemnt() != null) return e.getDatedebut_evenemnt().toString();
        return "Date non definie";
    }

    // =========================================================================
    //  ANIMATIONS
    // =========================================================================

    private void animateIn(Node n, int delayMs) {
        n.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), n);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), n);
        tt.setDelay(Duration.millis(delayMs));
        tt.setFromY(12); tt.setToY(0);
        ft.play(); tt.play();
    }

    private void animateCardIn(Node n, int delayMs) {
        n.setOpacity(0);
        n.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(320), n);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(320), n);
        tt.setDelay(Duration.millis(delayMs));
        tt.setFromY(20); tt.setToY(0);
        ft.play(); tt.play();
    }

    // =========================================================================
    //  HELPERS COULEURS
    // =========================================================================

    private String getTypeColor(String type) {
        if (type == null) return C_VIOLET;
        return switch (type.toLowerCase()) {
            case "concert"    -> "#8B5CF6";
            case "sport"      -> C_HOVER;
            case "conference" -> C_VIOLET;
            case "atelier"    -> "#F59E0B";
            case "exposition" -> "#0EA5E9";
            case "networking" -> "#EC4899";
            case "formation"  -> C_ACCENT;
            default           -> C_VIOLET;
        };
    }

    private String getTypeEmoji(String type) {
        if (type == null) return "📅";
        return switch (type.toLowerCase()) {
            case "concert"    -> "🎵";
            case "sport"      -> "⚽";
            case "conference" -> "🎤";
            case "atelier"    -> "🛠";
            case "exposition" -> "🖼";
            case "networking" -> "🤝";
            case "formation"  -> "📚";
            case "seminaire"  -> "💡";
            default           -> "📅";
        };
    }

    private String getStatusColor(String status) {
        if (status == null) return C_SUBTEXT;
        return switch (status.toLowerCase()) {
            case "actif"    -> C_HOVER;
            case "complet"  -> "#EF4444";
            case "annule"   -> "#F59E0B";
            case "planifie" -> C_VIOLET;
            default         -> C_SUBTEXT;
        };
    }

    // =========================================================================
    //  DIALOG SUPPRESSION
    // =========================================================================

    private void confirmDelete(Evenements event) throws SQLException {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer << " + event.getTitre_evenement() + " >> ?");
        alert.setContentText(
                "Cette action est irreversible.\n" +
                        "Tous les participants lies seront egalement supprimes."
        );

        ButtonType btnSupprimer = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler   = new ButtonType("Annuler",        ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnSupprimer, btnAnnuler);

        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
                "-fx-background-color: #FFFFFF; -fx-font-family: 'Segoe UI';" +
                        "-fx-border-color: " + C_BORDER + "; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 16; -fx-background-radius: 16;"
        );
        try {
            dp.lookup(".content.label").setStyle(
                    "-fx-text-fill: " + C_SUBTEXT + "; -fx-font-size: 13px;");
            dp.lookup(".header-panel").setStyle(
                    "-fx-background-color: #F8FAFC; -fx-background-radius: 16 16 0 0;");
            dp.lookup(".header-panel .label").setStyle(
                    "-fx-text-fill: " + C_TEXT + "; -fx-font-weight: 800; -fx-font-size: 15px;");
        } catch (Exception ignored) {}

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnSupprimer) {
            // ✅ Supprimer aussi le fichier image du dossier partagé
            if (event.getImage() != null && !event.getImage().isEmpty()) {
                FileStorageUtil.deleteImage(event.getImage());
            }
            evenementService.supprimerEvenement(event.getId_evenemnt());
            loadData();
        }
    }
}