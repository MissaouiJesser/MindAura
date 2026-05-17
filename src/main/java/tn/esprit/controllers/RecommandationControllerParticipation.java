package tn.esprit.controllers;

import tn.esprit.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.Evenements;
import tn.esprit.services.RecommandationServiceParticipation;
import tn.esprit.services.RecommandationServiceParticipation.EvenementScore;

import java.sql.SQLException;
import java.util.List;

public class RecommandationControllerParticipation {

    @FXML private VBox   recoContainer;
    @FXML private VBox   tendanceContainer;
    @FXML private Label  profilLabel;
    @FXML private Label  userEmailLabel;
    @FXML private Label  statusLabel;
    @FXML private Button btnRetour;

    private final RecommandationServiceParticipation recoService = new RecommandationServiceParticipation();

    /** Email de l'utilisateur connecte */
    private String currentUserEmail = null;

    private UserHomeController parentController;

    // ── Initialisation ────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        tn.esprit.entities.utilisateurs u = SessionManager.getCurrentUser();

        if (u == null || u.getEmail_utilisateur() == null || u.getEmail_utilisateur().isBlank()) {
            showError("Aucun utilisateur connecte. Veuillez vous authentifier.");
            return;
        }

        currentUserEmail = u.getEmail_utilisateur();

        if (userEmailLabel != null) {
            userEmailLabel.setText(currentUserEmail);
        }

        chargerTout();
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    public void setParentHomeController(UserHomeController c) {
        this.parentController = c;
    }

    @FXML
    public void retourPagePrecedente() {
        if (parentController != null) {
            parentController.restoreHome();
        } else {
            if (btnRetour != null && btnRetour.getScene() != null) {
                btnRetour.getScene().getWindow().hide();
            }
        }
    }

    @FXML
    public void rafraichir() {
        if (currentUserEmail == null) {
            showError("Utilisateur non connecte.");
            return;
        }
        chargerTout();
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerTout() {
        clearStatus();
        chargerProfil();
        try { chargerRecommandations(); } catch (SQLException e) { showError("Erreur recommandations : " + e.getMessage()); }
        try { chargerTendances();       } catch (SQLException e) { showError("Erreur tendances : " + e.getMessage()); }
    }

    private void chargerProfil() {
        if (profilLabel == null) return;
        try {
            String profil = recoService.profilUtilisateur(currentUserEmail);
            profilLabel.setText(profil);
            profilLabel.setStyle("-fx-text-fill:#1B5E20; -fx-font-size:13px; -fx-font-weight:700;");
        } catch (SQLException e) {
            profilLabel.setText("Profil indisponible");
            profilLabel.setStyle("-fx-text-fill:#888888; -fx-font-size:13px;");
        }
    }

    private void chargerRecommandations() throws SQLException {
        if (recoContainer == null) return;
        recoContainer.getChildren().clear();

        List<EvenementScore> recos = recoService.recommendationsForUser(currentUserEmail, 6);
        if (recos.isEmpty()) {
            recoContainer.getChildren().add(buildEmptyState("Aucune recommandation",
                    "Aucun nouvel evenement susceptible de vous plaire pour le moment."));
            return;
        }
        int i = 0;
        for (EvenementScore es : recos) recoContainer.getChildren().add(buildRecoCard(es, i++));
    }

    private void chargerTendances() throws SQLException {
        if (tendanceContainer == null) return;
        tendanceContainer.getChildren().clear();

        List<EvenementScore> tendances = recoService.tendances(4);
        if (tendances.isEmpty()) {
            tendanceContainer.getChildren().add(buildEmptyState("Aucune tendance",
                    "Les evenements populaires apparaitront ici."));
            return;
        }
        int i = 0;
        for (EvenementScore es : tendances) tendanceContainer.getChildren().add(buildTendanceCard(es, i++));
    }

    // ── Cartes ────────────────────────────────────────────────────────────────

    private Node buildRecoCard(EvenementScore es, int index) {
        Evenements e      = es.evenement();
        double     score  = Math.min(es.score(), 100);

        String scoreColor   = score >= 70 ? "#2E7D32" : score >= 40 ? "#E65100" : "#6A1B9A";
        String scoreBgColor = score >= 70 ? "#E8F5E9" : score >= 40 ? "#FFF3E0" : "#F3E5F5";
        String scoreBorder  = score >= 70 ? "#A5D6A7" : score >= 40 ? "#FFCC80" : "#CE93D8";

        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:12;-fx-padding:14 18;" +
                        "-fx-border-color:#C8E6C9;-fx-border-width:1;-fx-border-radius:12;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,100,0,0.07),8,0,0,2);"
        );

        // Badge score
        StackPane scoreBadge = new StackPane();
        scoreBadge.setStyle(
                "-fx-background-color:" + scoreBgColor + ";" +
                        "-fx-background-radius:10;" +
                        "-fx-min-width:58;-fx-min-height:58;-fx-max-width:58;-fx-max-height:58;" +
                        "-fx-border-color:" + scoreBorder + ";-fx-border-width:1.5;-fx-border-radius:10;"
        );
        VBox scoreBox = new VBox(0);
        scoreBox.setAlignment(Pos.CENTER);
        Label scoreLbl = new Label(String.format("%.0f", score));
        scoreLbl.setStyle("-fx-font-size:20px;-fx-font-weight:900;-fx-text-fill:" + scoreColor + ";");
        Label scoreSub = new Label("/ 100");
        scoreSub.setStyle("-fx-font-size:9px;-fx-text-fill:" + scoreColor + ";-fx-opacity:0.7;");
        scoreBox.getChildren().addAll(scoreLbl, scoreSub);
        scoreBadge.getChildren().add(scoreBox);

        // Infos
        VBox info = new VBox(5);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label(e.getTitre_evenement() != null ? e.getTitre_evenement() : "-");
        titre.setStyle("-fx-text-fill:#1A1A1A;-fx-font-size:14px;-fx-font-weight:800;");
        titleRow.getChildren().add(titre);

        if (e.getTypeEvenement() != null && !e.getTypeEvenement().isEmpty()) {
            titleRow.getChildren().add(buildBadge(e.getTypeEvenement(), "#2E7D32", "#E8F5E9", "#A5D6A7"));
        }
        if (e.getStatut_evenemnt() != null && !e.getStatut_evenemnt().isEmpty()) {
            String sc = getStatutColor(e.getStatut_evenemnt());
            titleRow.getChildren().add(buildBadge(e.getStatut_evenemnt(), sc, sc + "18", sc + "55"));
        }

        HBox meta = new HBox(18);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label lieu = new Label("Lieu : " + (e.getLieu_evenement() != null ? e.getLieu_evenement() : "-"));
        lieu.setStyle("-fx-text-fill:#555555;-fx-font-size:11.5px;");
        String dateStr = (e.getDatedebut_evenemnt() != null && e.getDatefin_evenemnt() != null)
                ? "Du " + e.getDatedebut_evenemnt() + " au " + e.getDatefin_evenemnt()
                : "Date non definie";
        Label dates = new Label(dateStr);
        dates.setStyle("-fx-text-fill:#555555;-fx-font-size:11.5px;");
        meta.getChildren().addAll(lieu, dates);

        Label raison = new Label("Recommande : " + es.raisonPrincipale());
        raison.setStyle("-fx-text-fill:#388E3C;-fx-font-size:11px;-fx-font-weight:600;");
        raison.setWrapText(true);

        info.getChildren().addAll(titleRow, meta, raison);

        int   cap    = e.getCapacite_evenement();
        Label capLbl = new Label(cap + " place" + (cap > 1 ? "s" : ""));
        capLbl.setStyle(
                "-fx-background-color:#E8F5E9;-fx-background-radius:20;-fx-padding:5 12;" +
                        "-fx-text-fill:#1B5E20;-fx-font-size:11px;-fx-font-weight:700;" +
                        "-fx-border-color:#A5D6A7;-fx-border-radius:20;-fx-border-width:1;"
        );

        card.getChildren().addAll(scoreBadge, info, capLbl);
        animateFade(card, index * 50L);
        return card;
    }

    private Node buildTendanceCard(EvenementScore es, int index) {
        Evenements e = es.evenement();

        HBox card = new HBox(12);
        card.setStyle(
                "-fx-background-color:#FFFFFF;-fx-background-radius:10;-fx-padding:12 14;" +
                        "-fx-border-color:#C8E6C9;-fx-border-width:1;-fx-border-radius:10;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,100,0,0.06),6,0,0,2);"
        );
        card.setAlignment(Pos.CENTER_LEFT);

        String[] ranks      = {"#1", "#2", "#3", "#4"};
        String[] rankColors = {"#C62828", "#E65100", "#6A1B9A", "#00838F"};
        String[] rankBgs    = {"#FFEBEE", "#FFF3E0", "#F3E5F5", "#E0F7FA"};
        String   rankColor  = index < rankColors.length ? rankColors[index] : "#388E3C";
        String   rankBg     = index < rankBgs.length    ? rankBgs[index]    : "#E8F5E9";
        String   rankText   = index < ranks.length      ? ranks[index]      : "#" + (index + 1);

        StackPane rankBox = new StackPane();
        rankBox.setStyle(
                "-fx-background-color:" + rankBg + ";-fx-background-radius:10;" +
                        "-fx-min-width:42;-fx-min-height:42;-fx-max-width:42;-fx-max-height:42;" +
                        "-fx-border-color:" + rankColor + "33;-fx-border-radius:10;-fx-border-width:1;"
        );
        Label rankLbl = new Label(rankText);
        rankLbl.setStyle("-fx-font-size:16px;-fx-font-weight:900;-fx-text-fill:" + rankColor + ";");
        rankBox.getChildren().add(rankLbl);

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titre = new Label(e.getTitre_evenement() != null ? e.getTitre_evenement() : "-");
        titre.setStyle("-fx-text-fill:#1A1A1A;-fx-font-size:12.5px;-fx-font-weight:700;");
        Label sub = new Label(es.raisonPrincipale());
        sub.setStyle("-fx-text-fill:#666666;-fx-font-size:10.5px;");
        info.getChildren().addAll(titre, sub);
        if (e.getLieu_evenement() != null) {
            Label lieu = new Label(e.getLieu_evenement());
            lieu.setStyle("-fx-text-fill:#9E9E9E;-fx-font-size:10px;");
            info.getChildren().add(lieu);
        }

        double score = Math.min(es.score(), 100);
        VBox scoreBox = new VBox(3);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label pctLbl = new Label(String.format("%.0f%%", score));
        pctLbl.setStyle("-fx-text-fill:" + rankColor + ";-fx-font-size:12px;-fx-font-weight:800;");

        StackPane barBg = new StackPane();
        barBg.setStyle("-fx-background-color:#E8F5E9;-fx-background-radius:4;");
        barBg.setMinWidth(80); barBg.setMaxWidth(80);
        barBg.setMinHeight(5); barBg.setMaxHeight(5);
        Region fill = new Region();
        fill.setStyle("-fx-background-color:" + rankColor + ";-fx-background-radius:4;");
        fill.setPrefWidth(Math.max(score / 100.0 * 80, 4));
        fill.setPrefHeight(5);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        barBg.getChildren().add(fill);

        scoreBox.getChildren().addAll(pctLbl, barBg);
        card.getChildren().addAll(rankBox, info, scoreBox);
        animateFade(card, index * 60L);
        return card;
    }

    private Node buildEmptyState(String title, String subtitle) {
        VBox empty = new VBox(10);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(40));
        empty.setStyle(
                "-fx-background-color:#F9FBF9;-fx-background-radius:12;" +
                        "-fx-border-color:#C8E6C9;-fx-border-width:1;-fx-border-radius:12;"
        );
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-text-fill:#388E3C;-fx-font-size:14px;-fx-font-weight:700;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-text-fill:#888888;-fx-font-size:12px;");
        subLbl.setWrapText(true);
        subLbl.setMaxWidth(300);
        empty.getChildren().addAll(titleLbl, subLbl);
        return empty;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private Label buildBadge(String text, String textColor, String bgColor, String borderColor) {
        Label badge = new Label(text);
        badge.setStyle(
                "-fx-background-color:" + bgColor + ";-fx-background-radius:20;-fx-padding:2 9;" +
                        "-fx-text-fill:" + textColor + ";-fx-font-size:10px;-fx-font-weight:700;" +
                        "-fx-border-color:" + borderColor + ";-fx-border-radius:20;-fx-border-width:1;"
        );
        return badge;
    }

    private String getStatutColor(String statut) {
        if (statut == null) return "#888888";
        return switch (statut.toLowerCase()) {
            case "actif"              -> "#2E7D32";
            case "complet"            -> "#C62828";
            case "annule", "annulé"   -> "#E65100";
            case "planifie","a_venir" -> "#1565C0";
            default                   -> "#555555";
        };
    }

    private void animateFade(Node node, long delayMs) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), node);
        ft.setDelay(Duration.millis(delayMs));
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void showError(String msg) {
        if (statusLabel == null) return;
        statusLabel.setText("Attention : " + msg);
        statusLabel.setStyle("-fx-text-fill:#C62828;-fx-font-size:12px;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void clearStatus() {
        if (statusLabel == null) return;
        statusLabel.setText("");
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}