package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.Participation;
import tn.esprit.services.ParticipationService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminParticipantsController implements DashboardController.DashboardAware {

    @FXML private VBox participantsContainer;
    @FXML private TextField searchField;
    @FXML private HBox statsRow;

    private final ParticipationService participationService = new ParticipationService();
    private DashboardController dashboardController;
    private List<Participation> allParticipants;

    // ── Palette ──────────────────────────────────────────────
    private static final String C_PRIMARY  = "#1B4332";
    private static final String C_ACCENT   = "#2D6A4F";
    private static final String C_HOVER    = "#40916C";
    private static final String C_BG       = "#F0F4F8";
    private static final String C_VIOLET   = "#7B5EA7";
    private static final String C_TEXT     = "#1A1A2E";
    private static final String C_SUBTEXT  = "#5A6475";
    private static final String C_WHITE    = "#FFFFFF";
    private static final String C_BORDER   = "#E2E8F0";
    private static final String C_CONFIRMED = "#1B4332";
    private static final String C_PENDING   = "#7B5EA7";

    @FXML
    public void initialize() { loadData(); }

    @Override
    public void setDashboardController(DashboardController c) { this.dashboardController = c; }

    private void loadData() {
        try {
            allParticipants = participationService.recuperer();
            renderStats();
            renderParticipants(allParticipants);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML public void refresh() { searchField.clear(); loadData(); }

    @FXML
    public void goToAddParticipation() {
        if (dashboardController != null)
            dashboardController.loadView("ParticipationForm.fxml", "Inscrire un Participant", "Nouvelle inscription");
    }

    @FXML
    public void filterParticipants() {
        String kw = searchField.getText().trim().toLowerCase();
        List<Participation> filtered = allParticipants.stream()
                .filter(p -> kw.isEmpty()
                        || (p.getNom_participation()    != null && p.getNom_participation().toLowerCase().contains(kw))
                        || (p.getStatutParticipation()  != null && p.getStatutParticipation().toLowerCase().contains(kw))
                        || (p.getNom_evenement()         != null && p.getNom_evenement().toLowerCase().contains(kw)))
                .collect(Collectors.toList());
        renderParticipants(filtered);
    }

    // ══════════════════════════════════════════════════════════
    //  STATS
    // ══════════════════════════════════════════════════════════
    private void renderStats() {
        statsRow.getChildren().clear();

        long confirmed = allParticipants.stream()
                .filter(p -> p.getStatutParticipation() != null && p.getStatutParticipation().toLowerCase().contains("confirm"))
                .count();
        long pending = allParticipants.stream()
                .filter(p -> p.getStatutParticipation() == null || p.getStatutParticipation().toLowerCase().contains("attente"))
                .count();

        statsRow.getChildren().addAll(
                createStatCard("👥", String.valueOf(allParticipants.size()), "Total participants", C_PRIMARY, "#E8F5EE"),
                createStatCard("✅", String.valueOf(confirmed),              "Confirmés",           C_HOVER,   "#E6F4ED"),
                createStatCard("⏳", String.valueOf(pending),               "En attente",           C_VIOLET,  "#F0ECF8")
        );

        int delay = 0;
        for (Node n : statsRow.getChildren()) {
            animateIn(n, delay);
            delay += 80;
        }
    }

    private HBox createStatCard(String icon, String value, String label, String color, String bgLight) {
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
                        "-fx-min-width: 48; -fx-min-height: 48; -fx-max-width: 48; -fx-max-height: 48;"
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
        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  RENDER PARTICIPANTS
    // ══════════════════════════════════════════════════════════
    private void renderParticipants(List<Participation> list) {
        participantsContainer.getChildren().clear();

        if (list.isEmpty()) {
            VBox empty = new VBox(14);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(80));
            Label ico = new Label("👤");
            ico.setStyle("-fx-font-size: 52px;");
            Label msg = new Label("Aucun participant trouvé");
            msg.setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-size: 16px; -fx-font-weight: 700;");
            Label sub = new Label("Essayez d'autres mots-clés");
            sub.setStyle("-fx-text-fill: " + C_SUBTEXT + "; -fx-font-size: 13px;");
            empty.getChildren().addAll(ico, msg, sub);
            participantsContainer.getChildren().add(empty);
            return;
        }

        int i = 0;
        for (Participation p : list) {
            HBox card = buildParticipantCard(p);
            participantsContainer.getChildren().add(card);
            animateCardIn(card, i * 35);
            i++;
        }
    }

    private HBox buildParticipantCard(Participation p) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: " + C_WHITE + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + C_BORDER + ";" +
                        "-fx-border-width: 1.5;" +
                        "-fx-border-radius: 14;" +
                        "-fx-padding: 14 18 14 18;" +
                        "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.07), 10, 0, 0, 2);"
        );

        // ── Avatar avec initiale colorée ──
        String initials = (p.getNom_participation() != null && !p.getNom_participation().isEmpty())
                ? String.valueOf(p.getNom_participation().charAt(0)).toUpperCase() : "?";
        // Couleur avatar alternée selon statut
        boolean isConfirmed = p.getStatutParticipation() != null
                && p.getStatutParticipation().toLowerCase().contains("confirm");
        String avatarColor = isConfirmed ? C_PRIMARY : C_VIOLET;
        String avatarBg    = isConfirmed ? "#E8F5EE"  : "#F0ECF8";

        StackPane avatar = new StackPane();
        avatar.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + avatarColor + ", " + (isConfirmed ? C_HOVER : "#9B7EC8") + ");" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 48; -fx-min-height: 48; -fx-max-width: 48; -fx-max-height: 48;" +
                        "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.20), 8, 0, 0, 2);"
        );
        Label initialsLbl = new Label(initials);
        initialsLbl.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 18px;");
        avatar.getChildren().add(initialsLbl);
        avatar.setMinSize(48, 48); avatar.setMaxSize(48, 48);

        // ── Infos principales ──
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(p.getNom_participation() != null ? p.getNom_participation() : "—");
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + C_TEXT + ";");

        HBox meta = new HBox(20);
        meta.setAlignment(Pos.CENTER_LEFT);

        String eventDisplay = (p.getNom_evenement() != null && !p.getNom_evenement().isEmpty())
                ? "🎪  " + p.getNom_evenement()
                : "🎪  Événement #" + p.getId_evenemnt();
        Label event = new Label(eventDisplay);
        event.setStyle("-fx-font-size: 12px; -fx-text-fill: " + C_SUBTEXT + ";");

        Label date = new Label("📅  " + (p.getDateInscription() != null ? p.getDateInscription() : "—"));
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: " + C_SUBTEXT + ";");

        meta.getChildren().addAll(event, date);
        info.getChildren().addAll(name, meta);

        // ── Badge statut ──
        String statut = p.getStatutParticipation();
        String badgeColor  = isConfirmed ? C_HOVER    : C_VIOLET;
        String badgeBg     = isConfirmed ? "#E6F4ED"  : "#F0ECF8";
        String badgeBorder = isConfirmed ? C_HOVER + "55" : C_VIOLET + "55";

        Label statusLbl = new Label(statut != null ? statut : "En attente");
        statusLbl.setStyle(
                "-fx-background-color: " + badgeBg + ";" +
                        "-fx-border-color: " + badgeBorder + ";" +
                        "-fx-border-width: 1; -fx-border-radius: 20;" +
                        "-fx-background-radius: 20; -fx-padding: 5 14 5 14;" +
                        "-fx-text-fill: " + badgeColor + ";" +
                        "-fx-font-size: 11px; -fx-font-weight: 700;"
        );

        // ── Bouton supprimer ──
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;" +
                        "-fx-border-color: #FECACA; -fx-border-width: 1.5; -fx-border-radius: 10;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 12 8 12;" +
                        "-fx-font-size: 14px;"
        );
        deleteBtn.setTooltip(new Tooltip("Supprimer ce participant"));
        deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle(
                "-fx-background-color: #991B1B; -fx-text-fill: white;" +
                        "-fx-border-color: #991B1B; -fx-border-width: 1.5; -fx-border-radius: 10;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 12 8 12; -fx-font-size: 14px;"
        ));
        deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle(
                "-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B;" +
                        "-fx-border-color: #FECACA; -fx-border-width: 1.5; -fx-border-radius: 10;" +
                        "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 12 8 12; -fx-font-size: 14px;"
        ));
        deleteBtn.setOnAction(e -> confirmDeleteParticipant(p));

        card.getChildren().addAll(avatar, info, statusLbl, deleteBtn);

        // Hover lift
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, rgba(27,67,50,0.07), 10, 0, 0, 2)",
                "dropshadow(gaussian, rgba(27,67,50,0.16), 18, 0, -2, 5)"
        ).replace("-fx-border-color: " + C_BORDER, "-fx-border-color: " + C_ACCENT)));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace(
                "dropshadow(gaussian, rgba(27,67,50,0.16), 18, 0, -2, 5)",
                "dropshadow(gaussian, rgba(27,67,50,0.07), 10, 0, 0, 2)"
        ).replace("-fx-border-color: " + C_ACCENT, "-fx-border-color: " + C_BORDER)));

        return card;
    }

    // ══════════════════════════════════════════════════════════
    //  ANIMATIONS
    // ══════════════════════════════════════════════════════════
    private void animateIn(Node n, int delayMs) {
        n.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), n);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(350), n);
        tt.setDelay(Duration.millis(delayMs));
        tt.setFromY(10); tt.setToY(0);
        ft.play(); tt.play();
    }

    private void animateCardIn(Node n, int delayMs) {
        n.setOpacity(0);
        n.setTranslateY(16);
        FadeTransition ft = new FadeTransition(Duration.millis(300), n);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), n);
        tt.setDelay(Duration.millis(delayMs));
        tt.setFromY(16); tt.setToY(0);
        ft.play(); tt.play();
    }

    // ══════════════════════════════════════════════════════════
    //  DIALOG SUPPRESSION
    // ══════════════════════════════════════════════════════════
    private void confirmDeleteParticipant(Participation p) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer « " + p.getNom_participation() + " » ?");
        alert.setContentText("Cette inscription sera définitivement supprimée.");

        ButtonType btnOui     = new ButtonType("Oui, supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler",        ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(btnOui, btnAnnuler);

        DialogPane dp = alert.getDialogPane();
        dp.setStyle(
                "-fx-background-color: " + C_WHITE + "; -fx-font-family: 'Segoe UI';" +
                        "-fx-border-color: " + C_BORDER + "; -fx-border-width: 1.5;" +
                        "-fx-border-radius: 16; -fx-background-radius: 16;"
        );
        try {
            dp.lookup(".content.label").setStyle("-fx-text-fill: " + C_SUBTEXT + "; -fx-font-size: 13px;");
            dp.lookup(".header-panel").setStyle("-fx-background-color: #F8FAFC; -fx-background-radius: 16 16 0 0;");
            dp.lookup(".header-panel .label").setStyle("-fx-text-fill: " + C_TEXT + "; -fx-font-weight: 800; -fx-font-size: 15px;");
        } catch (Exception ignored) {}

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == btnOui) {
            try {
                participationService.supprimer(p);
                loadData();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
}