package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.esprit.entities.Categorie;
import tn.esprit.services.CategorieService;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AfficherCategorieController implements DashboardController.DashboardAware {

    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    @FXML private TextField   tfRecherche;
    @FXML private FlowPane    flowCategories;   // grille de cartes
    @FXML private Label       lblTotal;
    @FXML private Label       lblMessage;

    private CategorieService categorieService;
    private List<Categorie>  toutesLesCategories;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        try {
            categorieService = new CategorieService();
        } catch (Exception e) {
            showError("Erreur de connexion à la base de données.");
            return;
        }

        // Recherche en temps réel
        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> filtrer(newVal));
        }

        chargerCategories();
    }

    // ─── Chargement ──────────────────────────────────────────────────────────

    private void chargerCategories() {
        try {
            toutesLesCategories = categorieService.afficherList();
            afficherCartes(toutesLesCategories);
        } catch (SQLException e) {
            showError("Impossible de charger les catégories.");
        }
    }

    // ─── Filtrage ─────────────────────────────────────────────────────────────

    private void filtrer(String terme) {
        if (toutesLesCategories == null) return;
        String t = terme != null ? terme.toLowerCase().trim() : "";
        List<Categorie> filtrees = toutesLesCategories.stream()
                .filter(c -> t.isEmpty()
                        || (c.getNomCategorie()  != null && c.getNomCategorie().toLowerCase().contains(t))
                        || (c.getDescription()   != null && c.getDescription().toLowerCase().contains(t)))
                .toList();
        afficherCartes(filtrees);
    }

    // ─── Affichage des cartes ─────────────────────────────────────────────────

    private void afficherCartes(List<Categorie> liste) {
        flowCategories.getChildren().clear();

        if (liste.isEmpty()) {
            Label vide = new Label("Aucune catégorie trouvée.");
            vide.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 14px; -fx-padding: 30;");
            flowCategories.getChildren().add(vide);
            majCompteur(0);
            return;
        }

        for (Categorie c : liste) {
            flowCategories.getChildren().add(creerCarte(c));
        }
        majCompteur(liste.size());
    }

    /**
     * Crée une carte visuelle pour une catégorie.
     */
    private VBox creerCarte(Categorie c) {
        // ── Nom ──
        Label lblNom = new Label(c.getNomCategorie() != null ? c.getNomCategorie() : "—");
        lblNom.setStyle(
                "-fx-font-size: 15px; -fx-font-weight: bold; " +
                        "-fx-text-fill: #1A1A2E; -fx-wrap-text: true;");
        lblNom.setMaxWidth(240);

        // ── Description ──
        String desc = c.getDescription() != null ? c.getDescription() : "Aucune description.";
        if (desc.length() > 100) desc = desc.substring(0, 100) + "…";
        Label lblDesc = new Label(desc);
        lblDesc.setStyle(
                "-fx-font-size: 12px; -fx-text-fill: #5A6475; " +
                        "-fx-wrap-text: true;");
        lblDesc.setMaxWidth(240);

        // ── Date de création ──
        String dateStr = "—";
        if (c.getDateCreation() != null) {
            dateStr = c.getDateCreation().toLocalDateTime().format(DATE_FMT);
        }
        Label lblDate = new Label("📅  " + dateStr);
        lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: #95A3B0;");

        // ── Badge ID ──
        Label lblId = new Label("#" + c.getIdCategorie());
        lblId.setStyle(
                "-fx-background-color: #E8F5E9; -fx-text-fill: #2D6A4F; " +
                        "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 11px; " +
                        "-fx-font-weight: bold;");

        // ── Carte (VBox) ──
        VBox card = new VBox(10, lblId, lblNom, lblDesc, lblDate);
        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 18; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2); " +
                        "-fx-cursor: hand;");
        card.setPrefWidth(270);
        card.setMaxWidth(270);

        // Hover : légère élévation
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 18; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #7B5EA7; -fx-border-width: 1.5; -fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(123,94,167,0.18), 14, 0, 0, 4); " +
                        "-fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-padding: 18; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #E2E8F0; -fx-border-width: 1; -fx-border-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 8, 0, 0, 2); " +
                        "-fx-cursor: hand;"));

        return card;
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    private void majCompteur(int count) {
        if (lblTotal != null) {
            lblTotal.setText(count + " catégorie" + (count > 1 ? "s" : ""));
        }
    }

    private void showError(String msg) {
        if (lblMessage != null) {
            lblMessage.setText(msg);
            lblMessage.setStyle("-fx-text-fill: #c62828;");
        }
    }
}
