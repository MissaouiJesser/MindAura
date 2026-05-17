package tn.esprit.controllers;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.chart.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import tn.esprit.entities.ReponseClient;
import tn.esprit.entities.TestPsycho;
import tn.esprit.entities.traitement;
import tn.esprit.entities.utilisateurs;
import tn.esprit.entities.reservation_historique;
import tn.esprit.enums.Etat;
import tn.esprit.enums.Role;
import tn.esprit.enums.TypeTraitement;
import tn.esprit.services.*;
import tn.esprit.utils.ConnexionHistorique;
import tn.esprit.utils.PaginationHelper;
import tn.esprit.utils.SessionManager;

// ─── Imports module Rania — Événements & Participations ──────────────────────
import tn.esprit.entities.Evenements;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MERGED DASHBOARD HOME CONTROLLER
 *
 * Combines:
 * - Azer's User & Treatment Statistics (KPIs, Charts, Recent Tables, Connection History)
 * - Ichrak's Reservation & Locaux Management (KPIs, Occupation Charts, Historique Tables)
 * - Tests Psychologiques Statistics (KPIs, BarChart, PieChart, Recap Table)
 * - Yassine's Réclamations Statistics (KPIs, PieChart statuts, BarChart catégories)
 */
public class DashboardHomeController implements Initializable, DashboardController.DashboardAware {

    // ═════════════════════════════════════════════════════════════════════════
    //  ANIMATION SECTION ANCHORS
    // ═════════════════════════════════════════════════════════════════════════

    @FXML private VBox  rootContentVBox;
    @FXML private HBox  welcomeCard;
    @FXML private HBox  kpiUsersRow;
    @FXML private HBox  userChartsRow;
    @FXML private HBox  kpiTraitementsRow;
    @FXML private HBox  traitementChartsRow;
    @FXML private VBox  connexionSection;
    @FXML private HBox  ichrakHeader;
    @FXML private HBox  kpiLocauxRow;
    @FXML private HBox  kpiRevenuRow;
    @FXML private HBox  ichrakChartsRow;
    @FXML private HBox  ichrakHistoRow;
    @FXML private HBox  testsHeader;
    @FXML private HBox  kpiTestsRow;
    @FXML private VBox  testsTableCard;
    @FXML private HBox  testsChartsRow;
    @FXML private HBox  reclamationsHeader;
    @FXML private HBox  kpiReclamationsRow;
    @FXML private HBox  reclamationsChartsRow;
    @FXML private HBox  ressourcesHeader;
    @FXML private HBox  kpiRessourcesRow;
    @FXML private HBox  ressourcesTop2Row;
    @FXML private HBox  ressourcesCharts1Row;
    @FXML private HBox  ressourcesCharts2Row;
    @FXML private VBox  ressourcesNiveauxCard;
    @FXML private HBox  ranieHeader;
    @FXML private HBox  kpiEvenementsRow;
    @FXML private HBox  kpiParticipationsRow;
    @FXML private HBox  evenementsChartsRow;
    @FXML private VBox  evenementsTop5Card;

    // ═════════════════════════════════════════════════════════════════════════
    //  AZER'S COMPONENTS — User & Treatment Management
    // ═════════════════════════════════════════════════════════════════════════

    @FXML private Label welcomeLabel, dateLabel;

    @FXML private Label totalUsersLabel, actifsLabel, inactifsLabel, adminsLabel;
    @FXML private Label totalTraitementsLabel, enCoursLabel, terminesLabel, suspendusLabel;

    @FXML private VBox rolesChartBox;
    @FXML private VBox typeChartBox;
    @FXML private VBox etatChartBox;

    @FXML private TableView<utilisateurs>           recentUsersTable;
    @FXML private TableColumn<utilisateurs, String> colUserNom, colUserEmail, colUserRole, colUserStatut;
    @FXML private TableView<traitement>             recentTraitementsTable;
    @FXML private TableColumn<traitement, String>   colTraitType, colTraitObj, colTraitEtat;

    @FXML private TableView<Map<String, String>>           connexionTable;
    @FXML private TableColumn<Map<String, String>, String> colCxNom, colCxEmail, colCxRole, colCxDate, colCxStatut;
    @FXML private Label connexionCountLabel;

    // ═════════════════════════════════════════════════════════════════════════
    //  ICHRAK'S COMPONENTS — Reservations & Locaux Management
    // ═════════════════════════════════════════════════════════════════════════

    @FXML private Label kpiTotalLocaux;
    @FXML private Label kpiDisponibles;
    @FXML private Label kpiTotalReservations;
    @FXML private Label kpiConfirmees;
    @FXML private Label kpiRevenuTotal;
    @FXML private Label kpiRevenuMoyen;

    @FXML private Canvas canvasChart;
    @FXML private Label  lblChartVide;
    @FXML private Canvas canvasPie;
    @FXML private Label  lblPieVide;

    @FXML private TableView<reservation_historique>            tableHistorique;
    @FXML private TableColumn<reservation_historique, Integer> colHistReservation;
    @FXML private TableColumn<reservation_historique, String>  colHistChamp;
    @FXML private TableColumn<reservation_historique, String>  colHistAncienne;
    @FXML private TableColumn<reservation_historique, String>  colHistNouvelle;
    @FXML private TableColumn<reservation_historique, String>  colHistPar;
    @FXML private TableColumn<reservation_historique, java.util.Date> colHistDate;

    @FXML private TableView<TopModifItem>            tableTopModif;
    @FXML private TableColumn<TopModifItem, String>  colTopLabel;
    @FXML private TableColumn<TopModifItem, Integer> colTopNb;

    @FXML private TextField txtFiltreReservation;

    // ═════════════════════════════════════════════════════════════════════════
    //  TESTS PSYCHOLOGIQUES COMPONENTS
    // ═════════════════════════════════════════════════════════════════════════

    // KPI Tests
    @FXML private Label kpiTotalTests;
    @FXML private Label kpiTotalPassations;
    @FXML private Label kpiTotalReponsesTests;
    @FXML private Label kpiScoreTotalTests;

    // Résumé texte
    @FXML private Label lblResume;

    // Tableau récapitulatif
    @FXML private TableView<StatistiquesParTypeController.StatistiqueType> tableViewStats;
    @FXML private TableColumn<StatistiquesParTypeController.StatistiqueType, String> colType;
    @FXML private TableColumn<StatistiquesParTypeController.StatistiqueType, Number> colNbTests;
    @FXML private TableColumn<StatistiquesParTypeController.StatistiqueType, Number> colNbPassations;
    @FXML private TableColumn<StatistiquesParTypeController.StatistiqueType, Number> colTotalReponses;
    @FXML private TableColumn<StatistiquesParTypeController.StatistiqueType, Number> colScoreTotal;
    @FXML private TableColumn<StatistiquesParTypeController.StatistiqueType, Number> colScoreMoyen;

    // Charts Tests
    @FXML private BarChart<String, Number> barChartPassations;
    @FXML private PieChart                 pieChartTypes;

    // Pagination Tests
    @FXML private HBox paginationContainer;

    // État pagination
    private List<StatistiquesParTypeController.StatistiqueType> listeStatsFull = new ArrayList<>();
    private int currentPage = 0;

    // ═════════════════════════════════════════════════════════════════════════
    //  YASSINE'S COMPONENTS — Statistiques des Réclamations
    // ═════════════════════════════════════════════════════════════════════════

    // KPI Réclamations
    @FXML private Label kpiTotalReclamations;
    @FXML private Label kpiTraitees;
    @FXML private Label kpiEnCours;
    @FXML private Label kpiEnAttente;
    @FXML private Label kpiTauxResolution;
    @FXML private Label kpiNoteMoyenne;

    // Conteneurs des graphiques (injectés dynamiquement)
    @FXML private VBox pieStatutsBox;
    @FXML private VBox barCategoriesBox;

    // ─── Rania — Ressources ───────────────────────────────────────────────────
    @FXML private Label lblTotalRessources;    @FXML private Label lblTotalVues;
    @FXML private Label lblTotalLikes;
    @FXML private Label lblMoyenneVues;
    @FXML private Label lblPlusVue;
    @FXML private Label lblPlusAimee;
    @FXML private javafx.scene.chart.PieChart                    rPieChartTypes;
    @FXML private javafx.scene.chart.PieChart                    rPieChartCategories;
    @FXML private javafx.scene.chart.BarChart<String, Number>    rBarChartVues;
    @FXML private javafx.scene.chart.BarChart<String, Number>    rBarChartLikes;
    @FXML private javafx.scene.chart.BarChart<String, Number>    rBarChartNiveaux;

    // ─── Rania — Événements & Participations ─────────────────────────────────
    @FXML private Label evTotalLabel;
    @FXML private Label evActifsLabel;
    @FXML private Label evAnnulesLabel;
    @FXML private Label evPlanifiesLabel;
    @FXML private Label partTotalLabel;
    @FXML private Label partConfirmeesLabel;
    @FXML private Label partAttenteLabel;
    @FXML private Label evPlacesDispoLabel;
    @FXML private VBox  evBarTypesBox;
    @FXML private VBox  evPieStatutsBox;
    @FXML private VBox  evBarTop5Box;

    // ═════════════════════════════════════════════════════════════════════════
    //  SERVICES & STATE
    // ═════════════════════════════════════════════════════════════════════════

    private DashboardController dashboardController;
    private final utilisateurs_service         userService       = new utilisateurs_service();
    private final traitement_service           traitService      = new traitement_service();
    private final ReservationHistoriqueService historiqueService = new ReservationHistoriqueService();
    private final TestPsychoService            testService       = new TestPsychoService();
    private final ReponseClientService reponseService    = new ReponseClientService();
    private       StatistiqueService           statistiqueService;

    // Services module Rania
    private final EvenementService   evenementService   = new EvenementService();
    private final ParticipationService participationServiceRania = new ParticipationService();

    private ObservableList<reservation_historique> historiqueListe;
    private ObservableList<TopModifItem>           topModifListe;

    private static final Color[] PIE_COLORS = {
            Color.web("#2D6A4F"), Color.web("#FF8C00"), Color.web("#7B5EA7"),
            Color.web("#1B4332"), Color.web("#E63946"), Color.web("#457B9D")
    };

    // ═════════════════════════════════════════════════════════════════════════
    //  INITIALIZATION
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        historiqueListe = FXCollections.observableArrayList();
        topModifListe   = FXCollections.observableArrayList();

        welcomeLabel.setText("Bonjour, " + SessionManager.getNomComplet() + " 👋");
        if (dateLabel != null)
            dateLabel.setText(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", new Locale("fr", "FR"))));

        setupTables();
        setupConnexionTable();
        initHistoriqueTable();
        initTopModifTable();
        initTestsTable();

        loadAzerData();
        loadIchrakData();
        loadConnexionHistory();
        loadTestsData();

        try {
            statistiqueService = new StatistiqueService();
        } catch (Exception e) {
            e.printStackTrace();
        }
        loadReclamationsData();
        loadRessourcesData();
        loadEvenementsData();

        applyFadeIn();
    }

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  AZER'S DATA LOADING — Users & Treatments
    // ═════════════════════════════════════════════════════════════════════════

    private void loadAzerData() {
        try {
            List<utilisateurs> users  = userService.afficherList();
            List<traitement>   traits = traitService.afficherList();

            animateCount(totalUsersLabel, users.size());
            long actifs   = users.stream().filter(utilisateurs::isEst_actif_utilisateur).count();
            long inactifs = users.size() - actifs;
            long admins   = users.stream().filter(u -> u.getRole_utilisateur() == Role.ROLE_ADMIN).count();
            animateCount(actifsLabel,   (int) actifs);
            animateCount(inactifsLabel, (int) inactifs);
            animateCount(adminsLabel,   (int) admins);

            animateCount(totalTraitementsLabel, traits.size());
            long enCours   = traits.stream().filter(t -> t.getEtat_traitement() == Etat.EN_COURS).count();
            long termines  = traits.stream().filter(t -> t.getEtat_traitement() == Etat.TERMINE).count();
            long suspendus = traits.stream().filter(t -> t.getEtat_traitement() == Etat.SUSPENDU).count();
            animateCount(enCoursLabel,   (int) enCours);
            animateCount(terminesLabel,  (int) termines);
            animateCount(suspendusLabel, (int) suspendus);

            buildRolesPieChart(users);
            buildTypeBarChart(traits);
            buildEtatBarChart(traits);

            int uSz = users.size();
            ObservableList<utilisateurs> rec = FXCollections.observableArrayList(
                    users.subList(Math.max(0, uSz - 5), uSz));
            FXCollections.reverse(rec);
            recentUsersTable.setItems(rec);

            int tSz = traits.size();
            ObservableList<traitement> recT = FXCollections.observableArrayList(
                    traits.subList(Math.max(0, tSz - 5), tSz));
            FXCollections.reverse(recT);
            recentTraitementsTable.setItems(recT);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buildRolesPieChart(List<utilisateurs> users) {
        if (rolesChartBox == null) return;
        rolesChartBox.getChildren().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Role r : Role.values()) {
            long count = users.stream().filter(u -> u.getRole_utilisateur() == r).count();
            if (count > 0)
                pieData.add(new PieChart.Data(r.getLibelle() + "  (" + count + ")", count));
        }

        PieChart chart = new PieChart(pieData);
        chart.setLegendSide(Side.RIGHT);
        chart.setLabelsVisible(true);
        chart.setStartAngle(90);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.getStyleClass().add("dashboard-pie-chart");
        chart.setTitle(null);

        rolesChartBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    private void buildTypeBarChart(List<traitement> traits) {
        if (typeChartBox == null) return;
        typeChartBox.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel("Nombre");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(260);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setBarGap(4);
        chart.setCategoryGap(24);
        chart.getStyleClass().add("dashboard-bar-chart");
        chart.setTitle(null);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (TypeTraitement tt : TypeTraitement.values()) {
            long count = traits.stream().filter(t -> t.getType_traitement() == tt).count();
            series.getData().add(new XYChart.Data<>(tt.getLibelle(), count));
        }
        chart.getData().add(series);
        applyBarColors(series, new String[]{"#1B4332", "#40916C", "#95D5B2"});

        typeChartBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    private void buildEtatBarChart(List<traitement> traits) {
        if (etatChartBox == null) return;
        etatChartBox.getChildren().clear();

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel("Nombre");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(260);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setBarGap(4);
        chart.setCategoryGap(24);
        chart.getStyleClass().add("dashboard-bar-chart");
        chart.setTitle(null);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Etat e : Etat.values()) {
            long count = traits.stream().filter(t -> t.getEtat_traitement() == e).count();
            series.getData().add(new XYChart.Data<>(e.getLibelle(), count));
        }
        chart.getData().add(series);
        applyBarColors(series, new String[]{"#1E40AF", "#065F46", "#92400E"});

        etatChartBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    private void applyBarColors(XYChart.Series<String, Number> series, String[] colors) {
        javafx.application.Platform.runLater(() -> {
            int i = 0;
            for (XYChart.Data<String, Number> data : series.getData()) {
                if (data.getNode() != null)
                    data.getNode().setStyle("-fx-bar-fill: " + colors[i % colors.length] + ";");
                i++;
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ICHRAK'S DATA LOADING — Reservations & Locaux
    // ═════════════════════════════════════════════════════════════════════════

    private void loadIchrakData() {
        chargerKPIs();
        chargerGraphiqueOccupation();
        chargerGraphiqueMotifs();
        chargerHistorique(null);
        chargerTopModifications();
    }

    private void chargerKPIs() {
        try {
            kpiTotalLocaux.setText(String.valueOf(historiqueService.getTotalLocaux()));
            kpiDisponibles.setText(String.valueOf(historiqueService.getLocauxDisponibles()));
            kpiTotalReservations.setText(String.valueOf(historiqueService.getTotalReservations()));
            kpiConfirmees.setText(String.valueOf(historiqueService.getReservationsConfirmees()));
            kpiRevenuTotal.setText(String.format("%.0f DT", historiqueService.getRevenuTotal()));
            kpiRevenuMoyen.setText(String.format("%.1f DT", historiqueService.getRevenuMoyen()));
        } catch (SQLException e) {
            e.printStackTrace();
            for (Label l : new Label[]{kpiTotalLocaux, kpiDisponibles,
                    kpiTotalReservations, kpiConfirmees, kpiRevenuTotal, kpiRevenuMoyen})
                l.setText("?");
        }
    }

    private void chargerGraphiqueOccupation() {
        try {
            dessinerBarres(historiqueService.getTauxOccupationParLocal());
        } catch (SQLException e) {
            e.printStackTrace();
            if (lblChartVide != null) lblChartVide.setVisible(true);
        }
    }

    private void chargerGraphiqueMotifs() {
        try {
            dessinerCamembert(historiqueService.getReservationsParMotif());
        } catch (SQLException e) {
            e.printStackTrace();
            if (lblPieVide != null) lblPieVide.setVisible(true);
        }
    }

    private void dessinerBarres(Map<String, Integer> data) {
        GraphicsContext gc = canvasChart.getGraphicsContext2D();
        double W = canvasChart.getWidth(), H = canvasChart.getHeight();
        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.WHITE); gc.fillRect(0, 0, W, H);

        if (data == null || data.isEmpty()) { lblChartVide.setVisible(true); return; }
        lblChartVide.setVisible(false);

        double mL = 220, mR = 50, mT = 20, mB = 40;
        int    maxV = data.values().stream().mapToInt(v -> v).max().orElse(1);
        int    nb   = data.size();
        double barH = Math.min(34, ((H - mT - mB) / nb) - 8);
        double gap  = ((H - mT - mB) - barH * nb) / (nb + 1);
        double cW   = W - mL - mR;

        gc.setStroke(Color.web("#F0F4F8")); gc.setLineWidth(1);
        for (int i = 0; i <= 5; i++) {
            double x = mL + (cW / 5) * i;
            gc.strokeLine(x, mT, x, H - mB);
            gc.setFill(Color.web("#9CA3AF")); gc.setFont(Font.font("Arial", 10));
            gc.fillText(String.valueOf((int) Math.round((double) maxV / 5 * i)), x - 6, H - mB + 14);
        }
        gc.setFill(Color.web("#5A6475")); gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        gc.fillText("Nombre de réservations", mL + cW / 2 - 60, H - 5);

        int i = 0;
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            double y     = mT + gap * (i + 1) + barH * i;
            double ratio = maxV > 0 ? (double) e.getValue() / maxV : 0;
            gc.setFill(Color.web("#F0F4F8")); gc.fillRoundRect(mL, y, cW, barH, 8, 8);
            if (e.getValue() > 0) {
                gc.setFill(interpoler(Color.web("#D8F3DC"), Color.web("#2D6A4F"), ratio));
                gc.fillRoundRect(mL, y, cW * ratio, barH, 8, 8);
            }
            gc.setFill(Color.web("#1A1A2E")); gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
            gc.fillText(String.valueOf(e.getValue()),
                    Math.min(mL + cW * ratio + 6, W - mR - 20), y + barH / 2 + 4);
            gc.setFill(Color.web("#374151")); gc.setFont(Font.font("Arial", 11));
            String nom = e.getKey().length() > 28 ? e.getKey().substring(0, 25) + "..." : e.getKey();
            gc.fillText(nom, 5, y + barH / 2 + 4);
            i++;
        }
    }

    private void dessinerCamembert(Map<String, Integer> data) {
        if (canvasPie == null) return;
        GraphicsContext gc = canvasPie.getGraphicsContext2D();
        double W = canvasPie.getWidth(), H = canvasPie.getHeight();
        gc.clearRect(0, 0, W, H);
        gc.setFill(Color.WHITE); gc.fillRect(0, 0, W, H);

        if (data == null || data.isEmpty()) {
            if (lblPieVide != null) lblPieVide.setVisible(true); return;
        }
        if (lblPieVide != null) lblPieVide.setVisible(false);

        int total = data.values().stream().mapToInt(v -> v).sum();
        if (total == 0) return;

        double pieSize = Math.min(W * 0.46, H - 40);
        double cx = 20 + pieSize / 2, cy = H / 2, r = pieSize / 2;

        List<String>  labels = new ArrayList<>(data.keySet());
        List<Integer> vals   = new ArrayList<>(data.values());

        double angle = -90;
        for (int i = 0; i < vals.size(); i++) {
            double sweep = 360.0 * vals.get(i) / total;
            Color  color = PIE_COLORS[i % PIE_COLORS.length];
            gc.setFill(color);
            gc.fillArc(cx - r, cy - r, pieSize, pieSize, angle, sweep, ArcType.ROUND);
            gc.setStroke(Color.WHITE); gc.setLineWidth(2);
            gc.strokeArc(cx - r, cy - r, pieSize, pieSize, angle, sweep, ArcType.ROUND);
            double pct = 100.0 * vals.get(i) / total;
            if (pct >= 5) {
                double mid = Math.toRadians(angle + sweep / 2);
                double tx  = cx + r * 0.62 * Math.cos(mid);
                double ty  = cy + r * 0.62 * Math.sin(mid);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("Arial", FontWeight.BOLD, 11));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText(String.format("%.0f%%", pct), tx, ty + 4);
            }
            angle += sweep;
        }

        double lx = cx + r + 22;
        double ly = cy - (vals.size() * 24) / 2.0;
        gc.setTextAlign(TextAlignment.LEFT);
        for (int i = 0; i < labels.size(); i++) {
            double lineY = ly + i * 26;
            gc.setFill(PIE_COLORS[i % PIE_COLORS.length]);
            gc.fillRoundRect(lx, lineY, 14, 14, 4, 4);
            gc.setFill(Color.web("#374151")); gc.setFont(Font.font("Arial", 11));
            String txt = formatMotif(labels.get(i)) + " (" + vals.get(i) + ")";
            if (txt.length() > 30) txt = txt.substring(0, 27) + "...";
            gc.fillText(txt, lx + 20, lineY + 11);
        }
    }

    private String formatMotif(String raw) {
        if (raw == null) return "Inconnu";
        StringBuilder sb = new StringBuilder();
        for (String w : raw.toLowerCase().split("_"))
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }

    private void chargerHistorique(Integer idFiltre) {
        try {
            historiqueListe.setAll(idFiltre != null
                    ? historiqueService.getHistoriqueParReservation(idFiltre)
                    : historiqueService.getToutHistorique());
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void chargerTopModifications() {
        try {
            topModifListe.clear();
            int rang = 1;
            for (Map.Entry<String, Integer> e : historiqueService.getModificationsParReservation().entrySet())
                topModifListe.add(new TopModifItem(rang++, e.getKey(), e.getValue()));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TESTS PSYCHOLOGIQUES DATA LOADING
    // ═════════════════════════════════════════════════════════════════════════

    private void loadTestsData() {
        try {
            List<TestPsycho>    tests    = testService.afficherList();
            List<ReponseClient> reponses = reponseService.afficherList();

            // Map idTest -> typeTest
            Map<Integer, String> idTestToType = new HashMap<>();
            for (TestPsycho t : tests)
                idTestToType.put(t.getIdTest(), t.getTypeTest() != null ? t.getTypeTest() : "Autre");

            // Par type : nb tests
            Map<String, Long> nbTestsParType = tests.stream()
                    .filter(t -> t.getTypeTest() != null && !t.getTypeTest().isEmpty())
                    .collect(Collectors.groupingBy(TestPsycho::getTypeTest, Collectors.counting()));

            // Types distincts
            Set<String> types = new LinkedHashSet<>(nbTestsParType.keySet());
            for (ReponseClient r : reponses) {
                String type = idTestToType.get(r.getIdTestId());
                if (type != null) types.add(type);
            }

            List<StatistiquesParTypeController.StatistiqueType> stats = new ArrayList<>();
            int totalReponsesGlobal = 0;
            int totalScoreGlobal    = 0;

            for (String type : types) {
                int nbTests = nbTestsParType.getOrDefault(type, 0L).intValue();
                Set<Integer> idTestsPasses = new HashSet<>();
                int totalRep = 0, scoreTotal = 0;

                for (ReponseClient r : reponses) {
                    if (!type.equals(idTestToType.get(r.getIdTestId()))) continue;
                    idTestsPasses.add(r.getIdTestId());
                    totalRep++;
                    scoreTotal += r.getScoreObtenu();
                }

                totalReponsesGlobal += totalRep;
                totalScoreGlobal    += scoreTotal;

                int    nbPassations = idTestsPasses.size();
                double scoreMoyen   = totalRep > 0 ? (double) scoreTotal / totalRep : 0;
                stats.add(new StatistiquesParTypeController.StatistiqueType(
                        type, nbTests, nbPassations, totalRep, scoreTotal,
                        Math.round(scoreMoyen * 100.0) / 100.0));
            }

            stats.sort(Comparator.comparing(StatistiquesParTypeController.StatistiqueType::getTypeTest));

            // KPIs Tests
            int totalPassationsGlobal = reponses.stream()
                    .map(ReponseClient::getIdTestId)
                    .collect(Collectors.toSet()).size();

            if (kpiTotalTests != null)          animateCount(kpiTotalTests,          tests.size());
            if (kpiTotalPassations != null)      animateCount(kpiTotalPassations,     totalPassationsGlobal);
            if (kpiTotalReponsesTests != null)   animateCount(kpiTotalReponsesTests,  totalReponsesGlobal);
            if (kpiScoreTotalTests != null)
                kpiScoreTotalTests.setText(totalScoreGlobal + " pts");

            // Résumé
            if (lblResume != null)
                lblResume.setText(String.format(
                        "Total : %d test(s)  |  %d passation(s)  |  Score total : %d pts",
                        tests.size(), totalPassationsGlobal, totalScoreGlobal));

            // Pagination + tableau
            listeStatsFull = stats;
            currentPage    = 0;
            applyPagination();

            // BarChart passations par type
            if (barChartPassations != null) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Passations");
                for (StatistiquesParTypeController.StatistiqueType s : stats)
                    series.getData().add(new XYChart.Data<>(s.getTypeTest(), s.getNbPassations()));
                barChartPassations.getData().clear();
                barChartPassations.getData().add(series);
                applyBarColors(series, new String[]{"#0F3460", "#16213E", "#1A5276", "#2874A6", "#5DADE2"});
            }

            // PieChart répartition réponses
            if (pieChartTypes != null) {
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                for (StatistiquesParTypeController.StatistiqueType s : stats)
                    if (s.getTotalReponses() > 0)
                        pieData.add(new PieChart.Data(
                                s.getTypeTest() + " (" + s.getTotalReponses() + ")",
                                s.getTotalReponses()));
                pieChartTypes.setData(pieData);
            }

        } catch (SQLException e) {
            if (lblResume != null)
                lblResume.setText("Erreur chargement des statistiques tests.");
            e.printStackTrace();
        }
    }

    /** Applique la pagination sur le tableau des stats tests. */
    private void applyPagination() {
        int total      = listeStatsFull.size();
        int totalPages = PaginationHelper.getTotalPages(total);
        currentPage    = Math.min(currentPage, Math.max(0, totalPages - 1));

        List<StatistiquesParTypeController.StatistiqueType> pageItems =
                PaginationHelper.getPageItems(listeStatsFull, currentPage);
        tableViewStats.setItems(FXCollections.observableArrayList(pageItems));

        if (paginationContainer != null) {
            paginationContainer.getChildren().clear();
            if (totalPages > 1) {
                PaginationHelper.PaginationBar bar = PaginationHelper.createPaginationBarWithRef(
                        currentPage, totalPages, total,
                        () -> { currentPage--; applyPagination(); },
                        () -> { currentPage++; applyPagination(); }
                );
                paginationContainer.getChildren().add(bar.container);
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  YASSINE'S DATA LOADING — Statistiques Réclamations
    // ═════════════════════════════════════════════════════════════════════════

    private void loadReclamationsData() {
        if (statistiqueService == null) return;
        try {
            int total = statistiqueService.getTotalReclamations();
            Map<String, Integer> parStatut = statistiqueService.getNombreParStatut();
            double taux = statistiqueService.getTauxResolution();
            double note = statistiqueService.getNoteMoyenne();

            int traitees  = parStatut.getOrDefault("TRAITEE", 0);
            int enCours   = parStatut.getOrDefault("EN_COURS", 0);
            int enAttente = parStatut.getOrDefault("EN_ATTENTE", 0);

            if (kpiTotalReclamations != null) animateCount(kpiTotalReclamations, total);
            if (kpiTraitees != null)          animateCount(kpiTraitees, traitees);
            if (kpiEnCours != null)           animateCount(kpiEnCours, enCours);
            if (kpiEnAttente != null)         animateCount(kpiEnAttente, enAttente);
            if (kpiTauxResolution != null)    kpiTauxResolution.setText(taux + "%");
            if (kpiNoteMoyenne != null)       kpiNoteMoyenne.setText(note + " / 5");

            buildReclamationsPieChart(traitees, enCours, enAttente, total);
            buildReclamationsBarChart();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buildReclamationsPieChart(int traitees, int enCours, int enAttente, int total) {
        if (pieStatutsBox == null) return;
        pieStatutsBox.getChildren().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (total == 0) {
            pieData.add(new PieChart.Data("Aucune donnée", 1));
        } else {
            if (traitees > 0)  pieData.add(new PieChart.Data("Traitées (" + traitees + ")", traitees));
            if (enCours > 0)   pieData.add(new PieChart.Data("En cours (" + enCours + ")", enCours));
            if (enAttente > 0) pieData.add(new PieChart.Data("En attente (" + enAttente + ")", enAttente));
        }

        PieChart chart = new PieChart(pieData);
        chart.setLegendSide(Side.RIGHT);
        chart.setLabelsVisible(true);
        chart.setStartAngle(90);
        chart.setAnimated(true);
        chart.setPrefHeight(280);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.getStyleClass().add("dashboard-pie-chart");
        chart.setTitle(null);

        javafx.application.Platform.runLater(() ->
                chart.getData().forEach(data -> {
                    String color;
                    if (data.getName().contains("Traitées"))   color = "#2D6A4F";
                    else if (data.getName().contains("cours")) color = "#F59E0B";
                    else                                       color = "#EF4444";
                    if (data.getNode() != null)
                        data.getNode().setStyle("-fx-pie-color: " + color + ";");
                })
        );

        pieStatutsBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    private void buildReclamationsBarChart() throws SQLException {
        if (barCategoriesBox == null || statistiqueService == null) return;
        barCategoriesBox.getChildren().clear();

        Map<String, Integer> parCategorie = statistiqueService.getNombreParCategorie();
        if (parCategorie == null || parCategorie.isEmpty()) return;

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel("Nombre");
        yAxis.setTickUnit(1);
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(true);
        chart.setPrefHeight(260);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.setBarGap(4);
        chart.setCategoryGap(24);
        chart.getStyleClass().add("dashboard-bar-chart");
        chart.setTitle(null);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        parCategorie.forEach((cat, count) ->
                series.getData().add(new XYChart.Data<>(cat, count))
        );
        chart.getData().add(series);
        applyBarColors(series, new String[]{"#2D6A4F", "#40916C", "#95D5B2", "#1B4332", "#52B788"});

        barCategoriesBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TABLE INITIALIZATION
    // ═════════════════════════════════════════════════════════════════════════

    /** Initialise les colonnes du tableau récapitulatif des tests. */
    private void initTestsTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("typeTest"));
        colNbTests.setCellValueFactory(new PropertyValueFactory<>("nbTests"));
        colNbPassations.setCellValueFactory(new PropertyValueFactory<>("nbPassations"));
        colTotalReponses.setCellValueFactory(new PropertyValueFactory<>("totalReponses"));
        colScoreTotal.setCellValueFactory(new PropertyValueFactory<>("scoreTotal"));
        colScoreMoyen.setCellValueFactory(new PropertyValueFactory<>("scoreMoyen"));
    }

    private void setupTables() {
        colUserNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getPrenom_utilisateur() + " " + d.getValue().getNom_utilisateur()));
        colUserEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getEmail_utilisateur()));
        colUserRole.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colUserRole.setCellFactory(col -> new TableCell<utilisateurs, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                utilisateurs u = getTableRow().getItem();
                Label b = new Label(u.getRole_utilisateur().getLibelle());
                String s = switch (u.getRole_utilisateur()) {
                    case ROLE_ADMIN       -> "badge-admin";
                    case ROLE_PSYCHOLOGUE -> "badge-psy";
                    case ROLE_COACH       -> "badge-coach";
                    default               -> "badge-patient";
                };
                b.getStyleClass().addAll("badge", s); setGraphic(b);
            }
        });
        colUserStatut.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colUserStatut.setCellFactory(col -> new TableCell<utilisateurs, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                utilisateurs u = getTableRow().getItem();
                Label b = new Label(u.isEst_actif_utilisateur() ? "Actif" : "Inactif");
                b.getStyleClass().addAll("badge", u.isEst_actif_utilisateur() ? "badge-actif" : "badge-inactif");
                setGraphic(b);
            }
        });

        colTraitType.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getType_traitement() != null ? d.getValue().getType_traitement().getLibelle() : ""));
        colTraitObj.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getObjectif_traitement() != null ? d.getValue().getObjectif_traitement().getLibelle() : ""));
        colTraitEtat.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colTraitEtat.setCellFactory(col -> new TableCell<traitement, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                traitement t = getTableRow().getItem();
                if (t.getEtat_traitement() == null) { setGraphic(null); return; }
                String css = switch (t.getEtat_traitement()) {
                    case EN_COURS -> "badge-encours";
                    case TERMINE  -> "badge-termine";
                    case SUSPENDU -> "badge-suspendu";
                };
                Label b = new Label(t.getEtat_traitement().getLibelle());
                b.getStyleClass().addAll("badge", css); setGraphic(b);
            }
        });
    }

    private void setupConnexionTable() {
        colCxNom.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getOrDefault("nom_complet", "")));
        colCxEmail.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getOrDefault("email", "")));
        colCxRole.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colCxRole.setCellFactory(col -> new TableCell<Map<String, String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                String role = getTableRow().getItem().getOrDefault("role", "");
                if (role.isBlank()) { setText("–"); setGraphic(null); return; }
                Label b = new Label(role.replace("ROLE_", "").replace("_", " "));
                b.getStyleClass().addAll("badge", "badge-patient");
                setGraphic(b); setText(null);
            }
        });
        colCxDate.setCellValueFactory(d -> {
            String raw = d.getValue().getOrDefault("date", "");
            try {
                LocalDateTime ldt = LocalDateTime.parse(raw,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                return new javafx.beans.property.SimpleStringProperty(
                        ldt.format(DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm", new Locale("fr", "FR"))));
            } catch (Exception ex) {
                return new javafx.beans.property.SimpleStringProperty(raw);
            }
        });
        colCxStatut.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(""));
        colCxStatut.setCellFactory(col -> new TableCell<Map<String, String>, String>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                Label b = new Label("Connecté");
                b.getStyleClass().addAll("badge", "badge-actif");
                setGraphic(b); setText(null);
            }
        });
    }

    private void initHistoriqueTable() {
        colHistReservation.setCellValueFactory(new PropertyValueFactory<>("id_reservation"));
        colHistChamp.setCellValueFactory(new PropertyValueFactory<>("champ_modifie"));
        colHistAncienne.setCellValueFactory(new PropertyValueFactory<>("ancienne_valeur"));
        colHistNouvelle.setCellValueFactory(new PropertyValueFactory<>("nouvelle_valeur"));
        colHistPar.setCellValueFactory(new PropertyValueFactory<>("modifie_par"));

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        colHistDate.setCellValueFactory(cellData -> {
            Object val = cellData.getValue().getDate_modification();
            java.util.Date utilDate = null;
            if (val instanceof java.sql.Timestamp)
                utilDate = new java.util.Date(((java.sql.Timestamp) val).getTime());
            else if (val instanceof java.sql.Date)
                utilDate = new java.util.Date(((java.sql.Date) val).getTime());
            return new javafx.beans.property.ReadOnlyObjectWrapper<>(utilDate);
        });
        colHistDate.setCellFactory(col -> new TableCell<reservation_historique, java.util.Date>() {
            @Override protected void updateItem(java.util.Date d, boolean empty) {
                super.updateItem(d, empty);
                setText(empty || d == null ? null : fmt.format(d));
            }
        });

        tableHistorique.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(reservation_historique it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) { setStyle(""); return; }
                String c = it.getChamp_modifie().toLowerCase();
                if (c.contains("statut"))     setStyle("-fx-background-color: rgba(255,140,0,0.06);");
                else if (c.contains("local")) setStyle("-fx-background-color: rgba(45,106,79,0.06);");
                else if (c.contains("prix"))  setStyle("-fx-background-color: rgba(123,94,167,0.06);");
                else setStyle("");
            }
        });
        tableHistorique.setItems(historiqueListe);
    }

    private void initTopModifTable() {
        colTopLabel.setCellValueFactory(new PropertyValueFactory<>("label"));
        colTopNb.setCellValueFactory(new PropertyValueFactory<>("nbModifications"));

        colTopNb.setCellFactory(col -> new TableCell<TopModifItem, Integer>() {
            @Override protected void updateItem(Integer nb, boolean empty) {
                super.updateItem(nb, empty);
                if (empty || nb == null) { setText(null); setStyle(""); return; }
                setText(nb + " modif.");
                int rang = getIndex() + 1;
                if      (rang == 1) setStyle("-fx-font-weight: 900; -fx-text-fill: #FF8C00;");
                else if (rang == 2) setStyle("-fx-font-weight: 700; -fx-text-fill: #2D6A4F;");
                else if (rang == 3) setStyle("-fx-font-weight: 700; -fx-text-fill: #7B5EA7;");
                else                setStyle("-fx-text-fill: #374151;");
            }
        });

        colTopLabel.setCellFactory(col -> new TableCell<TopModifItem, String>() {
            @Override protected void updateItem(String label, boolean empty) {
                super.updateItem(label, empty);
                if (empty || label == null) { setText(null); return; }
                int rang = getIndex() + 1;
                String prefix = rang == 1 ? "🥇 " : rang == 2 ? "🥈 " : rang == 3 ? "🥉 " : rang + ". ";
                setText(prefix + label);
            }
        });

        tableTopModif.setItems(topModifListe);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HANDLERS
    // ═════════════════════════════════════════════════════════════════════════

    private void loadConnexionHistory() {
        List<Map<String, String>> hist = ConnexionHistorique.getHistorique(50);
        connexionTable.setItems(FXCollections.observableArrayList(hist));
        if (connexionCountLabel != null)
            connexionCountLabel.setText("(" + hist.size() + " enregistrements)");
    }

    @FXML private void goToUtilisateurs() {
        if (dashboardController != null)
            dashboardController.navigateTo("ListeUtilisateurs.fxml",
                    "Gestion des Utilisateurs", "Liste et gestion des comptes");
    }

    @FXML private void goToTraitements() {
        if (dashboardController != null)
            dashboardController.navigateTo("ListeTraitements.fxml",
                    "Gestion des Traitements", "Programmes thérapeutiques");
    }

    @FXML private void handleRafraichir() { txtFiltreReservation.clear(); loadIchrakData(); }

    @FXML private void handleFiltrerHistorique() {
        String t = txtFiltreReservation.getText().trim();
        if (t.isEmpty()) { chargerHistorique(null); return; }
        try { chargerHistorique(Integer.parseInt(t)); } catch (NumberFormatException ignored) {}
    }

    @FXML private void handleToutHistorique() { txtFiltreReservation.clear(); chargerHistorique(null); }

    /** Handler bouton "Actualiser" de la section Tests. */
    @FXML private void actualiserStatsTests() { loadTestsData(); }

    /** Handler bouton "Actualiser" de la section Réclamations. */
    @FXML private void actualiserStatsReclamations() { loadReclamationsData(); }

    /** Handler bouton "Actualiser" de la section Ressources. */
    @FXML private void actualiserStatsRessources() { loadRessourcesData(); }

    // ─────────────────────────────────────────────────────────────────────────
    //  RANIA — Ressources  (logique reprise de StatistiquesController)
    // ─────────────────────────────────────────────────────────────────────────

    private void loadRessourcesData() {
        try {
            tn.esprit.services.RessourcesService svc =
                    new tn.esprit.services.RessourcesService(tn.esprit.utils.MyDataBase.getInstance().getConx());
            java.util.List<tn.esprit.entities.Ressources> list = svc.afficherList();

            // ── KPI chiffrés ──────────────────────────────────────────────────
            int total = list.size();
            int totalVues = 0, totalLikes = 0;
            tn.esprit.entities.Ressources plusVue = null, plusAimee = null;

            for (tn.esprit.entities.Ressources r : list) {
                totalVues  += r.getNbr_vues();
                totalLikes += r.getLikes();
                if (plusVue   == null || r.getNbr_vues() > plusVue.getNbr_vues())   plusVue   = r;
                if (plusAimee == null || r.getLikes()    > plusAimee.getLikes())     plusAimee = r;
            }

            double moyenne = total > 0 ? (double) totalVues / total : 0;
            lblTotalRessources.setText(String.valueOf(total));
            lblTotalVues.setText(formaterNombreRessources(totalVues));
            lblTotalLikes.setText(formaterNombreRessources(totalLikes));
            lblMoyenneVues.setText(String.format("%.0f", moyenne));
            lblPlusVue.setText(plusVue     != null ? plusVue.getTitre()   : "-");
            lblPlusAimee.setText(plusAimee != null ? plusAimee.getTitre() : "-");

            // ── PieChart Types ────────────────────────────────────────────────
            java.util.Map<String, Integer> parType = new java.util.HashMap<>();
            for (tn.esprit.entities.Ressources r : list) {
                if (r.getContenu() != null) {
                    String t = r.getContenu();
                    parType.put(t, parType.getOrDefault(t, 0) + 1);
                }
            }
            java.util.List<javafx.scene.chart.PieChart.Data> dataType = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Integer> e : parType.entrySet())
                dataType.add(new javafx.scene.chart.PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
            rPieChartTypes.setData(javafx.collections.FXCollections.observableArrayList(dataType));
            rPieChartTypes.setTitle("Répartition par type");

            // ── PieChart Catégories ───────────────────────────────────────────
            java.util.Map<String, Integer> parCat = new java.util.HashMap<>();
            for (tn.esprit.entities.Ressources r : list) {
                if (r.getCategorie() != null) {
                    String c = r.getCategorie().replace("_", " ");
                    parCat.put(c, parCat.getOrDefault(c, 0) + 1);
                }
            }
            java.util.List<javafx.scene.chart.PieChart.Data> dataCat = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, Integer> e : parCat.entrySet())
                dataCat.add(new javafx.scene.chart.PieChart.Data(e.getKey(), e.getValue()));
            rPieChartCategories.setData(javafx.collections.FXCollections.observableArrayList(dataCat));
            rPieChartCategories.setTitle("Répartition par catégorie");

            // ── BarChart Top 5 Vues ───────────────────────────────────────────
            java.util.List<tn.esprit.entities.Ressources> trieesVues = new java.util.ArrayList<>(list);
            trieesVues.sort((a, b) -> b.getNbr_vues() - a.getNbr_vues());
            javafx.scene.chart.XYChart.Series<String, Number> seriesVues = new javafx.scene.chart.XYChart.Series<>();
            seriesVues.setName("Nombre de vues");
            int limVues = Math.min(5, trieesVues.size());
            for (int i = 0; i < limVues; i++) {
                tn.esprit.entities.Ressources r = trieesVues.get(i);
                String titre = r.getTitre() != null ? r.getTitre() : "Sans titre";
                if (titre.length() > 15) titre = titre.substring(0, 15) + "...";
                seriesVues.getData().add(new javafx.scene.chart.XYChart.Data<>(titre, r.getNbr_vues()));
            }
            rBarChartVues.getData().clear();
            rBarChartVues.getData().add(seriesVues);
            rBarChartVues.setLegendVisible(false);
            javafx.application.Platform.runLater(() -> {
                for (javafx.scene.chart.XYChart.Data<String, Number> d : seriesVues.getData())
                    if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #2D6A4F;");
            });

            // ── BarChart Top 5 Likes ──────────────────────────────────────────
            java.util.List<tn.esprit.entities.Ressources> trieesLikes = new java.util.ArrayList<>(list);
            trieesLikes.sort((a, b) -> b.getLikes() - a.getLikes());
            javafx.scene.chart.XYChart.Series<String, Number> seriesLikes = new javafx.scene.chart.XYChart.Series<>();
            seriesLikes.setName("Likes");
            int limLikes = Math.min(5, trieesLikes.size());
            for (int i = 0; i < limLikes; i++) {
                tn.esprit.entities.Ressources r = trieesLikes.get(i);
                String titre = r.getTitre() != null ? r.getTitre() : "Sans titre";
                if (titre.length() > 15) titre = titre.substring(0, 15) + "...";
                seriesLikes.getData().add(new javafx.scene.chart.XYChart.Data<>(titre, r.getLikes()));
            }
            rBarChartLikes.getData().clear();
            rBarChartLikes.getData().add(seriesLikes);
            rBarChartLikes.setLegendVisible(false);
            javafx.application.Platform.runLater(() -> {
                for (javafx.scene.chart.XYChart.Data<String, Number> d : seriesLikes.getData())
                    if (d.getNode() != null) d.getNode().setStyle("-fx-bar-fill: #7B5EA7;");
            });

            // ── BarChart Niveaux ──────────────────────────────────────────────
            java.util.Map<String, Integer> parNiveau = new java.util.HashMap<>();
            for (tn.esprit.entities.Ressources r : list) {
                if (r.getNiveau() != null) {
                    String n = r.getNiveau();
                    parNiveau.put(n, parNiveau.getOrDefault(n, 0) + 1);
                }
            }
            javafx.scene.chart.XYChart.Series<String, Number> seriesNiv = new javafx.scene.chart.XYChart.Series<>();
            seriesNiv.setName("Ressources");
            for (java.util.Map.Entry<String, Integer> e : parNiveau.entrySet())
                seriesNiv.getData().add(new javafx.scene.chart.XYChart.Data<>(e.getKey(), e.getValue()));
            rBarChartNiveaux.getData().clear();
            rBarChartNiveaux.getData().add(seriesNiv);
            rBarChartNiveaux.setLegendVisible(false);

        } catch (Exception e) {
            System.err.println("[Ressources] Erreur chargement stats : " + e.getMessage());
        }
    }

    private String formaterNombreRessources(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RANIA — ÉVÉNEMENTS & PARTICIPATIONS
    // ═════════════════════════════════════════════════════════════════════════

    /** Appelé par le bouton "↺ Actualiser" de la section Événements. */
    @FXML private void actualiserStatsEvenements() { loadEvenementsData(); }

    private void loadEvenementsData() {
        try {
            List<Evenements> evenements = evenementService.getAllEvenements();

            // ── KPI Événements ─────────────────────────────────────────────
            long actifs    = evenements.stream().filter(e -> "Actif".equalsIgnoreCase(e.getStatut_evenemnt())).count();
            long annules   = evenements.stream().filter(e -> "Annulé".equalsIgnoreCase(e.getStatut_evenemnt())).count();
            long planifies = evenements.stream().filter(e -> "Planifié".equalsIgnoreCase(e.getStatut_evenemnt())).count();
            int  placesTotal = evenements.stream().mapToInt(Evenements::getCapacite_evenement).sum();

            if (evTotalLabel    != null) animateCount(evTotalLabel,    evenements.size());
            if (evActifsLabel   != null) animateCount(evActifsLabel,   (int) actifs);
            if (evAnnulesLabel  != null) animateCount(evAnnulesLabel,  (int) annules);
            if (evPlanifiesLabel!= null) animateCount(evPlanifiesLabel,(int) planifies);
            if (evPlacesDispoLabel != null) animateCount(evPlacesDispoLabel, placesTotal);

            // ── KPI Participations ─────────────────────────────────────────
            int totalPart     = participationServiceRania.countTotal();
            int confirmesPart = participationServiceRania.countConfirmees();
            int attentePart   = totalPart - confirmesPart;

            if (partTotalLabel     != null) animateCount(partTotalLabel,      totalPart);
            if (partConfirmeesLabel!= null) animateCount(partConfirmeesLabel, confirmesPart);
            if (partAttenteLabel   != null) animateCount(partAttenteLabel,    Math.max(0, attentePart));

            // ── BarChart : Participations par type d'événement ────────────
            buildEvBarTypesChart(evenements);

            // ── PieChart : Répartition statuts ────────────────────────────
            buildEvPieStatutsChart(actifs, annules, planifies,
                    evenements.stream().filter(e -> "Complet".equalsIgnoreCase(e.getStatut_evenemnt())).count(),
                    evenements.size());

            // ── BarChart : Top 5 événements par participants ───────────────
            buildEvTop5Chart(evenements);

        } catch (Exception e) {
            System.err.println("[Événements] Erreur chargement stats : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void buildEvBarTypesChart(List<Evenements> evenements) {
        if (evBarTypesBox == null) return;
        evBarTypesBox.getChildren().clear();

        // Compter participations par type via les événements
        Map<String, Long> parType = evenements.stream()
                .filter(e -> e.getTypeEvenement() != null && !e.getTypeEvenement().isEmpty())
                .collect(Collectors.groupingBy(Evenements::getTypeEvenement, Collectors.counting()));

        if (parType.isEmpty()) return;

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        yAxis.setLabel("Nombre"); yAxis.setTickUnit(1); yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false); chart.setAnimated(true);
        chart.setPrefHeight(260); chart.setMaxWidth(Double.MAX_VALUE);
        chart.setBarGap(4); chart.setCategoryGap(20);
        chart.getStyleClass().add("dashboard-bar-chart"); chart.setTitle(null);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        parType.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> series.getData().add(
                        new XYChart.Data<>(entry.getKey(), entry.getValue())));
        chart.getData().add(series);
        applyBarColors(series, new String[]{"#4F6EF7", "#8B5CF6", "#22D3EE", "#10B981", "#F59E0B", "#EF4444"});

        evBarTypesBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    private void buildEvPieStatutsChart(long actifs, long annules, long planifies, long complets, int total) {
        if (evPieStatutsBox == null) return;
        evPieStatutsBox.getChildren().clear();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (total == 0) {
            pieData.add(new PieChart.Data("Aucune donnée", 1));
        } else {
            if (actifs   > 0) pieData.add(new PieChart.Data("Actifs ("   + actifs   + ")", actifs));
            if (planifies> 0) pieData.add(new PieChart.Data("Planifiés (" + planifies + ")", planifies));
            if (complets > 0) pieData.add(new PieChart.Data("Complets ("  + complets  + ")", complets));
            if (annules  > 0) pieData.add(new PieChart.Data("Annulés ("   + annules   + ")", annules));
        }

        PieChart chart = new PieChart(pieData);
        chart.setLegendSide(Side.RIGHT); chart.setLabelsVisible(true);
        chart.setStartAngle(90); chart.setAnimated(true);
        chart.setPrefHeight(280); chart.setMaxWidth(Double.MAX_VALUE);
        chart.getStyleClass().add("dashboard-pie-chart"); chart.setTitle(null);

        javafx.application.Platform.runLater(() ->
                chart.getData().forEach(data -> {
                    String color;
                    if (data.getName().startsWith("Actifs"))    color = "#10B981";
                    else if (data.getName().startsWith("Plan")) color = "#4F6EF7";
                    else if (data.getName().startsWith("Com"))  color = "#F59E0B";
                    else                                        color = "#EF4444";
                    if (data.getNode() != null)
                        data.getNode().setStyle("-fx-pie-color: " + color + ";");
                })
        );

        evPieStatutsBox.getChildren().add(chart);
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    private void buildEvTop5Chart(List<Evenements> evenements) {
        if (evBarTop5Box == null) return;
        evBarTop5Box.getChildren().clear();

        try {
            // Calculer nb participants par événement
            Map<String, Long> top5 = new LinkedHashMap<>();
            evenements.stream()
                    .sorted(Comparator.comparingInt(e -> {
                        try { return -participationServiceRania.countByEvent(e.getId_evenemnt()); }
                        catch (Exception ex) { return 0; }
                    }))
                    .limit(5)
                    .forEach(e -> {
                        try {
                            int nb = participationServiceRania.countByEvent(e.getId_evenemnt());
                            String titre = e.getTitre_evenement() != null ? e.getTitre_evenement() : "—";
                            if (titre.length() > 20) titre = titre.substring(0, 18) + "…";
                            top5.put(titre, (long) nb);
                        } catch (Exception ex) { /* skip */ }
                    });

            if (top5.isEmpty()) return;

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis   yAxis = new NumberAxis();
            yAxis.setLabel("Participants"); yAxis.setTickUnit(1); yAxis.setMinorTickVisible(false);

            BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
            chart.setLegendVisible(false); chart.setAnimated(true);
            chart.setPrefHeight(240); chart.setMaxWidth(Double.MAX_VALUE);
            chart.setBarGap(4); chart.setCategoryGap(28);
            chart.getStyleClass().add("dashboard-bar-chart"); chart.setTitle(null);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            top5.forEach((titre, nb) -> series.getData().add(new XYChart.Data<>(titre, nb)));
            chart.getData().add(series);
            applyBarColors(series, new String[]{"#4F6EF7", "#8B5CF6", "#22D3EE", "#10B981", "#F59E0B"});

            evBarTop5Box.getChildren().add(chart);
            VBox.setVgrow(chart, Priority.ALWAYS);

        } catch (Exception e) {
            System.err.println("[Événements Top5] " + e.getMessage());
        }
    }
    //  UTILITIES
    // ═════════════════════════════════════════════════════════════════════════

    private void animateCount(Label label, int target) {
        if (target == 0) { label.setText("0"); return; }
        int step = Math.max(1, target / 25);
        final int[] cur = {0};
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(40), e -> {
            cur[0] = Math.min(cur[0] + step, target);
            label.setText(String.valueOf(cur[0]));
        }));
        tl.setCycleCount((int) Math.ceil((double) target / step));
        tl.setOnFinished(e -> label.setText(String.valueOf(target)));
        tl.play();
    }

    private Color interpoler(Color c1, Color c2, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                c1.getRed()   + t * (c2.getRed()   - c1.getRed()),
                c1.getGreen() + t * (c2.getGreen() - c1.getGreen()),
                c1.getBlue()  + t * (c2.getBlue()  - c1.getBlue()), 1.0);
    }

    // ─── Full staggered entrance animation for every section ─────────────────
    private void applyFadeIn() {

        // Collect all animated nodes in order of appearance on screen
        javafx.scene.Node[] sections = {
                welcomeCard,
                kpiUsersRow,
                userChartsRow,
                kpiTraitementsRow,
                traitementChartsRow,
                connexionSection,
                ichrakHeader,
                kpiLocauxRow,
                kpiRevenuRow,
                ichrakChartsRow,
                ichrakHistoRow,
                testsHeader,
                kpiTestsRow,
                testsTableCard,
                testsChartsRow,
                reclamationsHeader,
                kpiReclamationsRow,
                reclamationsChartsRow,
                ressourcesHeader,
                kpiRessourcesRow,
                ressourcesTop2Row,
                ressourcesCharts1Row,
                ressourcesCharts2Row,
                ressourcesNiveauxCard,
                ranieHeader,
                kpiEvenementsRow,
                kpiParticipationsRow,
                evenementsChartsRow,
                evenementsTop5Card
        };

        for (int i = 0; i < sections.length; i++) {
            javafx.scene.Node node = sections[i];
            if (node == null) continue;

            node.setOpacity(0);
            node.setTranslateY(22);

            // Stagger: 60ms between each section, start after 80ms initial delay
            long delayMs = 80L + (i * 60L);

            FadeTransition fade = new FadeTransition(Duration.millis(420), node);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            TranslateTransition slide = new TranslateTransition(Duration.millis(420), node);
            slide.setFromY(22);
            slide.setToY(0);
            slide.setInterpolator(javafx.animation.Interpolator.SPLINE(0.25, 0.1, 0.25, 1.0));

            ParallelTransition entrance = new ParallelTransition(fade, slide);
            entrance.setDelay(Duration.millis(delayMs));
            entrance.play();
        }

        // Also keep canvas parent fade for safety
        if (canvasChart != null && canvasChart.getParent() != null
                && !isAlreadyAnimated(canvasChart.getParent(), sections)) {
            FadeTransition ft = new FadeTransition(Duration.millis(500), canvasChart.getParent());
            ft.setFromValue(0.0); ft.setToValue(1.0);
            ft.setDelay(Duration.millis(800));
            ft.play();
        }
    }

    /** Check if a node is already in the staggered list (avoid double animation). */
    private boolean isAlreadyAnimated(javafx.scene.Node node, javafx.scene.Node[] arr) {
        for (javafx.scene.Node n : arr) if (n == node) return true;
        return false;
    }

    /**
     * Animate a single node entrance (useful to call from external controllers
     * when a section is loaded lazily).
     */
    public void animateNode(javafx.scene.Node node, long delayMs) {
        if (node == null) return;
        node.setOpacity(0);
        node.setTranslateY(18);
        FadeTransition fade = new FadeTransition(Duration.millis(380), node);
        fade.setFromValue(0.0); fade.setToValue(1.0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(380), node);
        slide.setFromY(18); slide.setToY(0);
        slide.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.setDelay(Duration.millis(delayMs));
        pt.play();
    }

    // ─── Inner class for Top Modif Item ───────────────────────────────────────
    public static class TopModifItem {
        private final int    rang;
        private final String label;
        private final int    nbModifications;

        public TopModifItem(int rang, String label, int nbModifications) {
            this.rang = rang; this.label = label; this.nbModifications = nbModifications;
        }
        public int    getRang()            { return rang; }
        public String getLabel()           { return label; }
        public int    getNbModifications() { return nbModifications; }
    }
}