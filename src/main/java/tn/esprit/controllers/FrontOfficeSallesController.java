package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.salle;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.salle_SERVICE;
import tn.esprit.utils.ImageManager;

import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FrontOfficeSallesController implements Initializable {

    @FXML private GridPane          sallesGridPane;
    @FXML private VBox              emptyStateContainer;
    @FXML private Label             lblStatistiques;
    @FXML private TextField         searchField;
    @FXML private ComboBox<String>  filterTypeComboBox;
    @FXML private ComboBox<String>  filterDispoComboBox;

    private final salle_SERVICE              salleService = new salle_SERVICE();
    private final local_psychiatrie_SERVICE  localService = new local_psychiatrie_SERVICE();

    private List<salle>         allSalles;
    private Map<Integer,String> localNomMap = new HashMap<>();

    // Référence au contrôleur parent (Accueil) pour la navigation inline
    private FrontOfficeAccueilController parentAccueilController;

    public void setParentAccueilController(FrontOfficeAccueilController parent) {
        this.parentAccueilController = parent;
    }

    // Nombre de colonnes dans la grille
    private static final int COLS = 3;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ImageManager.initializeImageDirectory();
        chargerMapLocaux();
        setupFilters();
        loadData();
        setupSearch();
    }

    // ── MAP ID → NOM DU LOCAL ─────────────────────────────────────────────────
    private void chargerMapLocaux() {
        try {
            localService.afficherList().forEach(l ->
                    localNomMap.put(l.getId_local(), l.getNom_local()));
        } catch (SQLException e) {
            localNomMap = new HashMap<>();
        }
    }

    // ── FILTRES ───────────────────────────────────────────────────────────────
    private void setupFilters() {
        filterTypeComboBox.getItems().addAll(
                "Tous", "Consultation", "Thérapie de groupe",
                "Salle de réunion", "Salle de formation", "Autre");
        filterTypeComboBox.setValue("Tous");

        filterDispoComboBox.getItems().addAll(
                "Tous", "Disponible", "Non disponible", "Sur reservation");
        filterDispoComboBox.setValue("Tous");

        filterTypeComboBox .setOnAction(e -> applyFilters());
        filterDispoComboBox.setOnAction(e -> applyFilters());
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    // ── CHARGEMENT DONNÉES ────────────────────────────────────────────────────
    public void loadData() {
        try {
            allSalles = salleService.afficherList();
            renderCards(allSalles);
            updateStats(allSalles);
        } catch (SQLException e) {
            showAlert("Impossible de charger les salles : " + e.getMessage());
        }
    }

    // ── FILTRAGE ──────────────────────────────────────────────────────────────
    private void applyFilters() {
        if (allSalles == null) return;

        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        String type   = filterTypeComboBox.getValue();
        String dispo  = filterDispoComboBox.getValue();

        List<salle> filtered = allSalles.stream()
                .filter(s -> search.isEmpty()
                        || (s.getNom_salle()  != null && s.getNom_salle() .toLowerCase().contains(search))
                        || (s.getType_salle() != null && s.getType_salle().toLowerCase().contains(search))
                        || (s.getEtage()      != null && s.getEtage()     .toLowerCase().contains(search)))
                .filter(s -> "Tous".equals(type)
                        || (s.getType_salle() != null && s.getType_salle().equalsIgnoreCase(type)))
                .filter(s -> "Tous".equals(dispo)
                        || (s.getDisponibilite_salle() != null && s.getDisponibilite_salle().equalsIgnoreCase(dispo)))
                .collect(Collectors.toList());

        renderCards(filtered);
        updateStats(filtered);
    }

    // ── RENDU GRILLE DE CARTES ────────────────────────────────────────────────
    private void renderCards(List<salle> salles) {
        sallesGridPane.getChildren().clear();

        boolean isEmpty = salles == null || salles.isEmpty();
        emptyStateContainer.setVisible(isEmpty);
        emptyStateContainer.setManaged(isEmpty);
        sallesGridPane.setVisible(!isEmpty);
        sallesGridPane.setManaged(!isEmpty);

        if (isEmpty) return;

        for (int i = 0; i < salles.size(); i++) {
            VBox card = createSalleCard(salles.get(i));
            sallesGridPane.add(card, i % COLS, i / COLS);
        }
    }

    // ── CRÉATION D'UNE CARTE SALLE ────────────────────────────────────────────
    private VBox createSalleCard(salle s) {

        // ── Image ──────────────────────────────────────────────────────────
        StackPane imagePane = new StackPane();
        imagePane.setPrefHeight(180);
        imagePane.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 14px 14px 0 0;");

        String imgPath = s.getImage_url();
        if (imgPath != null && !imgPath.isBlank()) {
            try {
                Image img = ImageManager.loadImage(imgPath, 420, 180, true);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(420); iv.setFitHeight(180); iv.setPreserveRatio(false);
                iv.setStyle("-fx-background-radius: 14px 14px 0 0;");
                imagePane.getChildren().add(iv);
            } catch (Exception ignored) {}
        } else {
            Label icone = new Label("🏠");
            icone.setStyle("-fx-font-size: 52px;");
            imagePane.getChildren().add(icone);
        }

        // ── Badge disponibilité (superposé sur l'image) ───────────────────
        String dispo = s.getDisponibilite_salle();
        Label badgeDispo = new Label(dispo != null ? dispo : "?");
        String badgeColor, badgeBg;
        if ("Disponible".equalsIgnoreCase(dispo))          { badgeColor="#1B4332"; badgeBg="#D8F3DC"; }
        else if ("Non disponible".equalsIgnoreCase(dispo)) { badgeColor="#B91C1C"; badgeBg="#FEE2E2"; }
        else                                               { badgeColor="#7B5EA7"; badgeBg="#F3E8FF"; }
        badgeDispo.setStyle("-fx-text-fill:" + badgeColor + "; -fx-background-color:" + badgeBg + ";" +
                "-fx-font-weight:700; -fx-font-size:10px; -fx-background-radius:20px;" +
                "-fx-padding:3px 10px;");
        StackPane.setAlignment(badgeDispo, Pos.TOP_RIGHT);
        StackPane.setMargin(badgeDispo, new Insets(10, 10, 0, 0));
        imagePane.getChildren().add(badgeDispo);

        // ── Contenu texte ─────────────────────────────────────────────────
        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 18, 16, 18));

        // Nom
        Label lblNom = new Label(s.getNom_salle() != null ? s.getNom_salle() : "—");
        lblNom.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #1A1A2E;" +
                " -fx-font-family: 'Poppins';");
        lblNom.setWrapText(true);

        // Type
        Label lblType = new Label("🏷  " + (s.getType_salle() != null ? s.getType_salle() : "—"));
        lblType.setStyle("-fx-font-size: 12px; -fx-text-fill: #5A6475;");

        // Capacité + Étage
        HBox rowCapEtage = new HBox(18);
        rowCapEtage.setAlignment(Pos.CENTER_LEFT);
        Label lblCap  = new Label("👥  " + (s.getCapacite_salle() != null ? s.getCapacite_salle() : "—"));
        lblCap .setStyle("-fx-font-size: 12px; -fx-text-fill: #5A6475;");
        Label lblEtage = new Label("🏢  " + (s.getEtage() != null ? s.getEtage() : "—"));
        lblEtage.setStyle("-fx-font-size: 12px; -fx-text-fill: #5A6475;");
        rowCapEtage.getChildren().addAll(lblCap, lblEtage);

        // Nom du local parent
        String nomLocal = localNomMap.getOrDefault(s.getId_local(), "Local #" + s.getId_local());
        Label lblLocal = new Label("📍  " + nomLocal);
        lblLocal.setStyle("-fx-font-size: 12px; -fx-text-fill: #2D6A4F; -fx-font-weight: 700;");

        // Statut badge
        String statut = s.getStatut_salle();
        Label lblStatut = new Label(statut != null ? statut : "—");
        String statColor, statBg;
        if ("Active".equalsIgnoreCase(statut))                { statColor="#1B4332"; statBg="#D8F3DC"; }
        else if ("En maintenance".equalsIgnoreCase(statut))   { statColor="#B45309"; statBg="#FEF3C7"; }
        else                                                   { statColor="#B91C1C"; statBg="#FEE2E2"; }
        lblStatut.setStyle("-fx-text-fill:" + statColor + "; -fx-background-color:" + statBg + ";" +
                "-fx-font-weight:700; -fx-font-size:10px; -fx-background-radius:20px;" +
                "-fx-padding:3px 10px;");

        // Séparateur
        Separator sep = new Separator();
        sep.setStyle("-fx-opacity:0.3;");

        // Équipements (tronqués)
        String equip = s.getEquipements();
        Label lblEquip = new Label();
        if (equip != null && !equip.isBlank()) {
            lblEquip.setText("🔧  " + (equip.length() > 60 ? equip.substring(0, 57) + "…" : equip));
            lblEquip.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
            lblEquip.setWrapText(true);
        }

        content.getChildren().addAll(lblNom, lblType, rowCapEtage, lblLocal, lblStatut);
        if (equip != null && !equip.isBlank()) {
            content.getChildren().addAll(sep, lblEquip);
        }

        // ── Bouton Réserver ───────────────────────────────────────────────
        String dsp = s.getDisponibilite_salle();
        boolean disponible = "Disponible".equalsIgnoreCase(dsp);
        Button btnReserver = new Button("📅  Réserver");
        btnReserver.setMaxWidth(Double.MAX_VALUE);
        if (disponible) {
            btnReserver.setStyle(
                    "-fx-background-color: linear-gradient(to right, #1B4332, #2D6A4F);" +
                            "-fx-text-fill: white; -fx-font-weight:700; -fx-font-size:13px;" +
                            "-fx-background-radius:10px; -fx-padding:10px 20px; -fx-cursor:hand;" +
                            "-fx-max-width: Infinity;");
            btnReserver.setOnAction(e -> {
                if (parentAccueilController != null) {
                    String nom_Local = localNomMap.getOrDefault(s.getId_local(), "Local #" + s.getId_local());
                    parentAccueilController.ouvrirDetailsSalle(s, nomLocal);
                }
            });
        } else {
            btnReserver.setStyle(
                    "-fx-background-color: #D1D5DB;" +
                            "-fx-text-fill: #6B7280; -fx-font-weight:700; -fx-font-size:13px;" +
                            "-fx-background-radius:10px; -fx-padding:10px 20px;" +
                            "-fx-max-width: Infinity; -fx-opacity:0.6;");
            btnReserver.setDisable(true);
        }
        content.getChildren().add(btnReserver);

        // ── Assemblage de la carte ────────────────────────────────────────
        VBox card = new VBox(0, imagePane, content);
        card.setPrefWidth(400);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 16, 0, 0, 4);" +
                "-fx-cursor: hand;");

        // Animation survol
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(27,67,50,0.22), 22, 0, 0, 7);" +
                        "-fx-cursor: hand; -fx-translate-y: -3px;"));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 16, 0, 0, 4);" +
                        "-fx-cursor: hand; -fx-translate-y: 0;"));

        // Fade-in à l'apparition
        FadeTransition fade = new FadeTransition(Duration.millis(350), card);
        fade.setFromValue(0); fade.setToValue(1); fade.play();

        return card;
    }

    // ── STATS ─────────────────────────────────────────────────────────────────
    private void updateStats(List<salle> salles) {
        if (lblStatistiques == null || salles == null) return;
        long dispo = salles.stream()
                .filter(s -> "Disponible".equalsIgnoreCase(s.getDisponibilite_salle()))
                .count();
        lblStatistiques.setText(salles.size() + " salle(s) · " + dispo + " disponible(s)");
    }

    // ── RAFRAÎCHIR ────────────────────────────────────────────────────────────
    @FXML
    private void handleRafraichir() {
        if (searchField        != null) searchField.clear();
        if (filterTypeComboBox  != null) filterTypeComboBox .setValue("Tous");
        if (filterDispoComboBox != null) filterDispoComboBox.setValue("Tous");
        loadData();
    }

    /**
     * Appelé par FrontOfficeDetailsSalleController pour revenir à la liste des salles.
     */
    public void retourListe() {
        if (parentAccueilController != null) {
            parentAccueilController.retourVueSalles();
        }
    }

    // ── ALERTE ────────────────────────────────────────────────────────────────
    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}