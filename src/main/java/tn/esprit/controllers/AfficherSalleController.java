package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.salle;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.salle_SERVICE;
import tn.esprit.utils.ImageManager;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

public class AfficherSalleController implements Initializable {

    // ── SWAP VUES ─────────────────────────────────────────────────────────────
    @FXML private VBox vueListe;
    @FXML private VBox vueFormulaire;
    @FXML private VBox vueDetails;

    // ── TABLE ─────────────────────────────────────────────────────────────────
    @FXML private TableView<salle>             tableSalles;
    @FXML private TableColumn<salle, Void>     colImage;
    @FXML private TableColumn<salle, Integer>  colId;
    @FXML private TableColumn<salle, String>   colNom;
    @FXML private TableColumn<salle, String>   colType;
    @FXML private TableColumn<salle, String>   colCapacite;
    @FXML private TableColumn<salle, String>   colEquipements;
    @FXML private TableColumn<salle, String>   colDisponibilite;
    @FXML private TableColumn<salle, String>   colEtage;
    @FXML private TableColumn<salle, String>   colStatut;
    @FXML private TableColumn<salle, String>   colNomLocal;
    @FXML private TableColumn<salle, Void>     colActions;

    // ── FILTRES & RECHERCHE ───────────────────────────────────────────────────
    @FXML private TextField        txtRecherche;
    @FXML private ComboBox<String> filterTypeComboBox;
    @FXML private ComboBox<String> filterStatutComboBox;

    // ── BOUTONS LISTE ─────────────────────────────────────────────────────────
    @FXML private Button btnAjouter;
    @FXML private Button btnRafraichir;

    // ── KPI ───────────────────────────────────────────────────────────────────
    @FXML private Label lblTotal;
    @FXML private Label lblDisponibles;
    @FXML private Label lblOccupees;
    @FXML private Label lblFooter;

    // ── FORMULAIRE INLINE ─────────────────────────────────────────────────────
    @FXML private Label            lblTitreFormulaire;
    @FXML private Label            lblSousTitreFormulaire;
    @FXML private TextField        txtNom;
    @FXML private TextField        txtType;
    @FXML private TextField        txtCapacite;
    @FXML private TextArea         txtEquipements;
    @FXML private ComboBox<String> comboDisponibilite;
    @FXML private TextField        txtEtage;
    @FXML private TextField        txtImage_url;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<String> comboLocal;
    @FXML private Button           btnParcourir;
    @FXML private Button           btnSauvegarder;
    @FXML private Label            lblMessage;

    // ── VUE DÉTAILS ───────────────────────────────────────────────────────────
    @FXML private Label     detailNom;
    @FXML private Label     detailType;
    @FXML private Label     detailCapacite;
    @FXML private Label     detailEtage;
    @FXML private Label     detailEquipements;
    @FXML private Label     detailDisponibilite;
    @FXML private Label     detailStatut;
    @FXML private Label     detailLocal;
    @FXML private ImageView detailImage;

    // ── SERVICES & ÉTAT ───────────────────────────────────────────────────────
    private final salle_SERVICE             salleService = new salle_SERVICE();
    private final local_psychiatrie_SERVICE localService = new local_psychiatrie_SERVICE();

    private ObservableList<salle> sallesList = FXCollections.observableArrayList();
    private List<local_psychiatrie> locauxList;
    private Map<Integer, String> localNomMap = new HashMap<>();

    private salle  salleEnCours     = null;
    private File   selectedImageFile = null;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ImageManager.initializeImageDirectory();
        chargerMapLocaux();
        setupColumns();
        setupFilters();
        loadData();
        setupSearch();
    }

    // ── MAP ID→NOM DES LOCAUX ─────────────────────────────────────────────────
    private void chargerMapLocaux() {
        try {
            locauxList = localService.afficherList();
            localNomMap.clear();
            for (local_psychiatrie l : locauxList)
                localNomMap.put(l.getId_local(), l.getNom_local());
        } catch (SQLException e) {
            localNomMap = new HashMap<>();
        }
    }

    // ── COLONNES TABLE ────────────────────────────────────────────────────────
    private void setupColumns() {
        colNom        .setCellValueFactory(new PropertyValueFactory<>("nom_salle"));
        colType       .setCellValueFactory(new PropertyValueFactory<>("type_salle"));
        colCapacite   .setCellValueFactory(new PropertyValueFactory<>("capacite_salle"));
        colEquipements.setCellValueFactory(new PropertyValueFactory<>("equipements"));
        colEtage      .setCellValueFactory(new PropertyValueFactory<>("etage"));
        colStatut     .setCellValueFactory(new PropertyValueFactory<>("statut_salle"));

        // ── Colonne IMAGE miniature ────────────────────────────────────────
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            { iv.setFitWidth(55); iv.setFitHeight(42); iv.setPreserveRatio(true); }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                String path = getTableView().getItems().get(getIndex()).getImage_url();
                if (path != null && !path.isBlank()) {
                    Image img = ImageManager.loadImage(path, 55, 42, true);
                    iv.setImage(img);
                    StackPane sp = new StackPane(iv);
                    sp.setStyle("-fx-background-color:#F0F4F8; -fx-background-radius:8px; -fx-padding:3px;");
                    setGraphic(sp);
                } else {
                    Label lbl = new Label("🏠");
                    lbl.setStyle("-fx-font-size:22px;");
                    setGraphic(lbl);
                }
                setText(null);
            }
        });

        // ── Colonne DISPONIBILITÉ badge couleur ────────────────────────────
        colDisponibilite.setCellValueFactory(new PropertyValueFactory<>("disponibilite_salle"));
        colDisponibilite.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                String color, bg, border;
                if ("Disponible".equalsIgnoreCase(item))         { color="#1B4332"; bg="#D8F3DC"; border="#2D6A4F"; }
                else if ("Non disponible".equalsIgnoreCase(item)) { color="#B91C1C"; bg="#FEE2E2"; border="#DC2626"; }
                else                                              { color="#7B5EA7"; bg="#F3E8FF"; border="#9B7DC4"; }
                badge.setStyle("-fx-text-fill:"+color+"; -fx-background-color:"+bg+"; " +
                        "-fx-border-color:"+border+"; -fx-border-width:1px; " +
                        "-fx-background-radius:20px; -fx-border-radius:20px; " +
                        "-fx-padding:3px 10px; -fx-font-size:10px; -fx-font-weight:700;");
                setGraphic(badge); setText(null);
            }
        });

        // ── Colonne NOM DU LOCAL ───────────────────────────────────────────
        colNomLocal.setCellValueFactory(cellData -> {
            int idLocal = cellData.getValue().getId_local();
            return new SimpleStringProperty(localNomMap.getOrDefault(idLocal, "Local #" + idLocal));
        });
        colNomLocal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setStyle("-fx-text-fill:#1B4332; -fx-font-weight:700; -fx-font-size:11px;");
                setGraphic(lbl); setText(null);
            }
        });

        addActionButtons();
    }

    // ── BOUTONS ACTIONS (même style que AfficherLocalController) ──────────────
    private void addActionButtons() {
        colActions.setCellFactory(param -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null); return;
                }
                Button btnDetails   = new Button("Détails");
                Button btnModifier  = new Button("Modifier");
                Button btnSupprimer = new Button("Supprimer");

                btnDetails.setStyle(
                        "-fx-background-color:#1B4332; -fx-text-fill:white; -fx-font-size:11px; " +
                                "-fx-font-weight:600; -fx-padding:6px 10px; -fx-background-radius:8px; -fx-cursor:hand;");
                btnModifier.setStyle(
                        "-fx-background-color:#B45309; -fx-text-fill:white; -fx-font-size:11px; " +
                                "-fx-font-weight:600; -fx-padding:6px 10px; -fx-background-radius:8px; -fx-cursor:hand;");
                btnSupprimer.setStyle(
                        "-fx-background-color:#B91C1C; -fx-text-fill:white; -fx-font-size:11px; " +
                                "-fx-font-weight:600; -fx-padding:6px 10px; -fx-background-radius:8px; -fx-cursor:hand;");

                salle s = getTableView().getItems().get(getIndex());
                btnDetails  .setOnAction(e -> afficherDetails(s));
                btnModifier .setOnAction(e -> ouvrirFormulaireModifier(s));
                btnSupprimer.setOnAction(e -> supprimerSalle(s));

                HBox hBox = new HBox(6, btnDetails, btnModifier, btnSupprimer);
                hBox.setAlignment(Pos.CENTER);
                setGraphic(hBox);
            }
        });
    }

    // ── FILTRES & RECHERCHE ───────────────────────────────────────────────────
    private void setupFilters() {
        filterTypeComboBox.getItems().addAll(
                "Tous", "Consultation", "Thérapie de groupe",
                "Salle de réunion", "Salle de formation", "Autre");
        filterTypeComboBox.setValue("Tous");
        filterStatutComboBox.getItems().addAll("Tous", "Active", "En maintenance", "Hors service");
        filterStatutComboBox.setValue("Tous");
        filterTypeComboBox  .setOnAction(e -> applyFilters());
        filterStatutComboBox.setOnAction(e -> applyFilters());
    }

    private void setupSearch() {
        txtRecherche.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    private void applyFilters() {
        String search = txtRecherche.getText() == null ? "" : txtRecherche.getText().toLowerCase();
        String type   = filterTypeComboBox.getValue();
        String statut = filterStatutComboBox.getValue();
        ObservableList<salle> filtered = FXCollections.observableArrayList();
        for (salle s : sallesList) {
            boolean ms  = search.isEmpty()
                    || (s.getNom_salle()    != null && s.getNom_salle()   .toLowerCase().contains(search))
                    || (s.getType_salle()   != null && s.getType_salle()  .toLowerCase().contains(search))
                    || (s.getStatut_salle() != null && s.getStatut_salle().toLowerCase().contains(search));
            boolean mt  = "Tous".equals(type)   || (s.getType_salle()   != null && s.getType_salle()  .equalsIgnoreCase(type));
            boolean mst = "Tous".equals(statut) || (s.getStatut_salle() != null && s.getStatut_salle().equalsIgnoreCase(statut));
            if (ms && mt && mst) filtered.add(s);
        }
        tableSalles.setItems(filtered);
    }

    // ── CHARGEMENT DONNÉES ────────────────────────────────────────────────────
    public void loadData() {
        try {
            chargerMapLocaux();
            sallesList = FXCollections.observableArrayList(salleService.afficherList());
            tableSalles.setItems(sallesList);
            updateKPI();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les salles : " + e.getMessage());
        }
    }

    public void refreshTable() { loadData(); }

    private void updateKPI() {
        int total = sallesList.size();
        long dispo    = sallesList.stream().filter(s -> "Disponible".equalsIgnoreCase(s.getDisponibilite_salle())).count();
        long occupees = sallesList.stream().filter(s -> {
            String st = s.getStatut_salle();
            return "En maintenance".equalsIgnoreCase(st) || "Hors service".equalsIgnoreCase(st);
        }).count();
        if (lblTotal      != null) lblTotal     .setText(String.valueOf(total));
        if (lblDisponibles!= null) lblDisponibles.setText(String.valueOf(dispo));
        if (lblOccupees   != null) lblOccupees  .setText(String.valueOf(occupees));
        if (lblFooter     != null) lblFooter    .setText(total + " salle(s) au total");
    }

    // ── SWAP VUES ─────────────────────────────────────────────────────────────
    private void showVue(VBox vueActive) {
        for (VBox v : new VBox[]{vueListe, vueFormulaire, vueDetails}) {
            if (v == null) continue;
            boolean actif = v == vueActive;
            v.setVisible(actif); v.setManaged(actif);
        }
        if (vueActive != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(250), vueActive);
            ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
        }
    }

    @FXML private void handleRetourListe() {
        salleEnCours = null; selectedImageFile = null;
        showVue(vueListe); refreshTable();
    }

    @FXML private void handleRetourDepuisDetails() { handleRetourListe(); }

    // ── AJOUTER ───────────────────────────────────────────────────────────────
    @FXML
    private void handleAjouterSalle() {
        salleEnCours = null;
        viderFormulaire(); chargerComboLocaux(); initFormulaireCombos();
        lblTitreFormulaire.setText("➕ Nouvelle Salle");
        lblSousTitreFormulaire.setText("Remplissez les informations de la salle");
        btnSauvegarder.setText("✓ Ajouter la Salle");
        lblMessage.setText("");
        showVue(vueFormulaire);
    }

    // ── MODIFIER ──────────────────────────────────────────────────────────────
    private void ouvrirFormulaireModifier(salle s) {
        salleEnCours = s;
        viderFormulaire(); chargerComboLocaux(); initFormulaireCombos();

        txtNom.setText(s.getNom_salle());
        txtType.setText(s.getType_salle());
        txtCapacite.setText(s.getCapacite_salle());
        txtEquipements.setText(s.getEquipements());
        txtEtage.setText(s.getEtage() != null ? s.getEtage() : "");
        txtImage_url.setText(s.getImage_url() != null ? s.getImage_url() : "");
        comboDisponibilite.setValue(s.getDisponibilite_salle());
        comboStatut.setValue(s.getStatut_salle());
        comboLocal.getItems().stream()
                .filter(item -> item.startsWith(s.getId_local() + " - "))
                .findFirst().ifPresent(comboLocal::setValue);

        lblTitreFormulaire.setText("✏️ Modifier la Salle");
        lblSousTitreFormulaire.setText("Modification de : " + s.getNom_salle());
        btnSauvegarder.setText("✓ Enregistrer les Modifications");
        lblMessage.setText("");
        showVue(vueFormulaire);
    }

    // ── SAUVEGARDER ───────────────────────────────────────────────────────────
    @FXML
    private void handleSauvegarder() {
        if (!validateForm()) return;
        try {
            String imagePath = null;
            if (selectedImageFile != null)
                imagePath = ImageManager.saveImage(selectedImageFile);

            String localSelection = comboLocal.getValue();
            int idLocal = Integer.parseInt(localSelection.split(" - ")[0].trim());

            if (salleEnCours == null) {
                // ── MODE AJOUT ────────────────────────────────────────────
                salle s = new salle(
                        txtNom.getText().trim(), txtType.getText().trim(),
                        txtCapacite.getText().trim(), txtEquipements.getText().trim(),
                        comboDisponibilite.getValue(), txtEtage.getText().trim(),
                        imagePath != null ? imagePath : txtImage_url.getText().trim(),
                        comboStatut.getValue(), idLocal);
                salleService.add(s);

                // ✅ Capacité du local + 1
                incrementerCapaciteLocal(idLocal);
                showMessage("✓ Salle ajoutée avec succès !", "success");

            } else {
                // ── MODE MODIFICATION ─────────────────────────────────────
                salleEnCours.setNom_salle(txtNom.getText().trim());
                salleEnCours.setType_salle(txtType.getText().trim());
                salleEnCours.setCapacite_salle(txtCapacite.getText().trim());
                salleEnCours.setEquipements(txtEquipements.getText().trim());
                salleEnCours.setDisponibilite_salle(comboDisponibilite.getValue());
                salleEnCours.setEtage(txtEtage.getText().trim());
                if (imagePath != null) salleEnCours.setImageURL(imagePath);
                salleEnCours.setStatut_salle(comboStatut.getValue());
                salleEnCours.setId_local(idLocal);
                salleService.modifier(salleEnCours);
                showMessage("✓ Salle modifiée avec succès !", "success");
            }

            new Thread(() -> {
                try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> { refreshTable(); showVue(vueListe); });
            }).start();

        } catch (SQLException e) { showMessage("✗ Erreur BD : " + e.getMessage(), "error"); }
        catch (NumberFormatException e) { showMessage("✗ Format invalide", "error"); }
        catch (java.io.IOException e) { showMessage("x Erreur image : " + e.getMessage(), "error");}
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ✅ LOGIQUE MÉTIER
    // ═════════════════════════════════════════════════════════════════════════

    /** Ajout d'une salle → capacité du local +1 */
    private void incrementerCapaciteLocal(int idLocal) {
        try {
            local_psychiatrie local = trouverLocalParId(idLocal);
            if (local == null) return;
            int cap = parseCapacite(local.getCapacite_local());
            local.setCapacite_local(String.valueOf(cap + 1));
            localService.modifier(local);
            System.out.println("✅ Capacité local '" + local.getNom_local() + "' → " + (cap + 1));
        } catch (SQLException e) { System.err.println("⚠ Erreur +capacité : " + e.getMessage()); }
    }

    /** Suppression d'une salle → capacité du local -1 */
    private void decrementerCapaciteLocal(int idLocal) {
        try {
            local_psychiatrie local = trouverLocalParId(idLocal);
            if (local == null) return;
            int cap = parseCapacite(local.getCapacite_local());
            if (cap > 0) cap--;
            local.setCapacite_local(String.valueOf(cap));
            localService.modifier(local);
            System.out.println("✅ Capacité local '" + local.getNom_local() + "' → " + cap);
        } catch (SQLException e) { System.err.println("⚠ Erreur -capacité : " + e.getMessage()); }
    }

    /**
     * ✅ Appelé depuis AfficherLocalController quand la disponibilité du local change.
     * Met à jour la disponibilité de toutes les salles appartenant à ce local.
     */
    public void synchroniserDisponibiliteSalles(int idLocal, String disponibiliteLocal) {
        try {
            List<salle> sallesDuLocal = salleService.getSallesByLocal(idLocal);
            String nouvelleDispo;
            if ("Non disponible".equalsIgnoreCase(disponibiliteLocal))
                nouvelleDispo = "Non disponible";
            else if ("Disponible".equalsIgnoreCase(disponibiliteLocal))
                nouvelleDispo = "Disponible";
            else
                nouvelleDispo = "Sur reservation";

            for (salle s : sallesDuLocal) {
                s.setDisponibilite_salle(nouvelleDispo);
                salleService.modifier(s);
                System.out.println("✅ Salle '" + s.getNom_salle() + "' → " + nouvelleDispo);
            }
        } catch (SQLException e) {
            System.err.println("⚠ Erreur sync disponibilité salles : " + e.getMessage());
        }
    }

    private local_psychiatrie trouverLocalParId(int idLocal) throws SQLException {
        if (locauxList == null) chargerMapLocaux();
        return locauxList.stream().filter(l -> l.getId_local() == idLocal).findFirst().orElse(null);
    }

    private int parseCapacite(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── SUPPRIMER ─────────────────────────────────────────────────────────────
    private void supprimerSalle(salle s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la salle");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer « " + s.getNom_salle() + " » ?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                int idLocal = s.getId_local();
                salleService.delete(s.getId_salle());
                // ✅ Capacité du local -1
                decrementerCapaciteLocal(idLocal);
                refreshTable();
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Salle supprimée avec succès.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible : " + e.getMessage());
            }
        }
    }

    // ── RAFRAÎCHIR ────────────────────────────────────────────────────────────
    @FXML
    private void handleRafraichir() {
        if (txtRecherche     != null) txtRecherche.clear();
        if (filterTypeComboBox   != null) filterTypeComboBox  .setValue("Tous");
        if (filterStatutComboBox != null) filterStatutComboBox.setValue("Tous");
        loadData();
    }

    // ── IMAGE ─────────────────────────────────────────────────────────────────
    @FXML
    private void handleParcourir() {
        File file = ImageManager.selectImageFile(btnParcourir.getScene().getWindow());
        if (file != null) {
            if (ImageManager.isValidImageFile(file)) {
                selectedImageFile = file;
                txtImage_url.setText(file.getName());
                showMessage("✓ Image sélectionnée : " + file.getName(), "success");
            } else {
                showMessage("✗ Fichier invalide (PNG, JPG, JPEG, GIF)", "error");
            }
        }
    }

    // ── DÉTAILS ───────────────────────────────────────────────────────────────
    private void afficherDetails(salle s) {
        if (detailNom         != null) detailNom        .setText(s.getNom_salle());
        if (detailType        != null) detailType       .setText(s.getType_salle());
        if (detailCapacite    != null) detailCapacite   .setText(s.getCapacite_salle());
        if (detailEtage       != null) detailEtage      .setText(s.getEtage() != null ? s.getEtage() : "-");
        if (detailEquipements != null) detailEquipements.setText(s.getEquipements() != null ? s.getEquipements() : "-");
        if (detailStatut      != null) detailStatut     .setText(s.getStatut_salle());
        if (detailLocal       != null) detailLocal      .setText(localNomMap.getOrDefault(s.getId_local(), "Local #" + s.getId_local()));

        if (detailDisponibilite != null) {
            detailDisponibilite.setText(s.getDisponibilite_salle());
            String color, bg;
            if ("Disponible".equalsIgnoreCase(s.getDisponibilite_salle()))          { color="#1B4332"; bg="#D8F3DC"; }
            else if ("Non disponible".equalsIgnoreCase(s.getDisponibilite_salle())) { color="#B91C1C"; bg="#FEE2E2"; }
            else                                                                    { color="#7B5EA7"; bg="#F3E8FF"; }
            detailDisponibilite.setStyle("-fx-text-fill:"+color+"; -fx-background-color:"+bg+"; " +
                    "-fx-background-radius:20px; -fx-padding:4px 14px; -fx-font-weight:700;");
        }

        if (detailImage != null) {
            String path = s.getImage_url();
            if (path != null && !path.isBlank())
                detailImage.setImage(ImageManager.loadImage(path, 320, 220, true));
            else
                detailImage.setImage(null);
        }

        showVue(vueDetails);
    }

    // ── HELPERS FORMULAIRE ────────────────────────────────────────────────────
    private void initFormulaireCombos() {
        if (comboDisponibilite.getItems().isEmpty())
            comboDisponibilite.setItems(FXCollections.observableArrayList(
                    "Disponible", "Non disponible", "Sur reservation"));
        comboDisponibilite.setValue("Disponible");
        if (comboStatut.getItems().isEmpty())
            comboStatut.setItems(FXCollections.observableArrayList(
                    "Active", "En maintenance", "Hors service"));
        comboStatut.setValue("Active");
    }

    private void chargerComboLocaux() {
        comboLocal.getItems().clear();
        try {
            locauxList = localService.afficherList();
            for (local_psychiatrie l : locauxList)
                comboLocal.getItems().add(l.getId_local() + " - " + l.getNom_local());
            if (!comboLocal.getItems().isEmpty()) comboLocal.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            showMessage("Impossible de charger les locaux : " + e.getMessage(), "warning");
        }
    }

    private void viderFormulaire() {
        txtNom.clear(); txtType.clear(); txtCapacite.clear();
        txtEquipements.clear(); txtEtage.clear(); txtImage_url.clear();
        txtNom.setStyle(""); txtType.setStyle(""); txtCapacite.setStyle("");
        selectedImageFile = null;
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();
        if (txtNom.getText().trim().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
            txtNom.setStyle("-fx-border-color:#B91C1C; -fx-border-width:2px;"); }
        else txtNom.setStyle("");
        if (txtType.getText().trim().isEmpty()) {
            errors.append("- Le type est obligatoire\n");
            txtType.setStyle("-fx-border-color:#B91C1C; -fx-border-width:2px;"); }
        else txtType.setStyle("");
        if (txtCapacite.getText().trim().isEmpty()) {
            errors.append("- La capacité est obligatoire\n");
            txtCapacite.setStyle("-fx-border-color:#B91C1C; -fx-border-width:2px;"); }
        else txtCapacite.setStyle("");
        if (comboLocal.getValue() == null)
            errors.append("- Veuillez sélectionner un local\n");
        if (errors.length() > 0) { showMessage("Erreurs :\n" + errors, "error"); return false; }
        return true;
    }

    private void showMessage(String message, String type) {
        if (lblMessage == null) return;
        lblMessage.setText(message);
        String style;
        if ("success".equals(type))
            style = "-fx-text-fill:#1B4332; -fx-font-weight:bold; -fx-background-color:#D8F3DC; " +
                    "-fx-padding:12px 20px; -fx-background-radius:10px; " +
                    "-fx-border-color:#2D6A4F; -fx-border-width:1px; -fx-border-radius:10px;";
        else if ("warning".equals(type))
            style = "-fx-text-fill:#B45309; -fx-font-weight:bold; -fx-background-color:#FEF3C7; " +
                    "-fx-padding:12px 20px; -fx-background-radius:10px; " +
                    "-fx-border-color:#D97706; -fx-border-width:1px; -fx-border-radius:10px;";
        else
            style = "-fx-text-fill:#B91C1C; -fx-font-weight:bold; -fx-background-color:#FEE2E2; " +
                    "-fx-padding:12px 20px; -fx-background-radius:10px; " +
                    "-fx-border-color:#B91C1C; -fx-border-width:1px; -fx-border-radius:10px;";
        lblMessage.setStyle(style);
        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(content); alert.showAndWait();
    }
}