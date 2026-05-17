package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.entities.ReponseClient;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.ReponseClientService;
import tn.esprit.services.TestPsychoService;
import tn.esprit.utils.PaginationHelper;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class StatistiquesParTypeController implements Initializable, DashboardController.DashboardAware {

    /** Statistiques agrégées par type de test. */
    public static class StatistiqueType {
        private final String typeTest;
        private final int nbTests;
        private final int nbPassations;
        private final int totalReponses;
        private final int scoreTotal;
        private final double scoreMoyen;

        public StatistiqueType(String typeTest, int nbTests, int nbPassations, int totalReponses, int scoreTotal, double scoreMoyen) {
            this.typeTest = typeTest;
            this.nbTests = nbTests;
            this.nbPassations = nbPassations;
            this.totalReponses = totalReponses;
            this.scoreTotal = scoreTotal;
            this.scoreMoyen = scoreMoyen;
        }

        public String getTypeTest() { return typeTest; }
        public int getNbTests() { return nbTests; }
        public int getNbPassations() { return nbPassations; }
        public int getTotalReponses() { return totalReponses; }
        public int getScoreTotal() { return scoreTotal; }
        public double getScoreMoyen() { return scoreMoyen; }
    }

    @FXML
    private TableView<StatistiqueType> tableViewStats;

    @FXML
    private TableColumn<StatistiqueType, String> colType;

    @FXML
    private TableColumn<StatistiqueType, Number> colNbTests;

    @FXML
    private TableColumn<StatistiqueType, Number> colNbPassations;

    @FXML
    private TableColumn<StatistiqueType, Number> colTotalReponses;

    @FXML
    private TableColumn<StatistiqueType, Number> colScoreTotal;

    @FXML
    private TableColumn<StatistiqueType, Number> colScoreMoyen;

    @FXML
    private BarChart<String, Number> barChartPassations;

    @FXML
    private PieChart pieChartTypes;

    @FXML
    private Label lblResume;
    @FXML
    private javafx.scene.layout.HBox paginationContainer;

    private List<StatistiqueType> listeStatsFull = new ArrayList<>();
    private int currentPage = 0;

    private DashboardController dashboardController;

    @Override
    public void setDashboardController(DashboardController dc) {
        this.dashboardController = dc;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colType.setCellValueFactory(new PropertyValueFactory<>("typeTest"));
        colNbTests.setCellValueFactory(new PropertyValueFactory<>("nbTests"));
        colNbPassations.setCellValueFactory(new PropertyValueFactory<>("nbPassations"));
        colTotalReponses.setCellValueFactory(new PropertyValueFactory<>("totalReponses"));
        colScoreTotal.setCellValueFactory(new PropertyValueFactory<>("scoreTotal"));
        colScoreMoyen.setCellValueFactory(new PropertyValueFactory<>("scoreMoyen"));

        chargerStatistiques();
    }

    private void chargerStatistiques() {
        TestPsychoService testService = new TestPsychoService();
        ReponseClientService reponseService = new ReponseClientService();

        try {
            List<TestPsycho> tests = testService.afficherList();
            List<ReponseClient> reponses = reponseService.afficherList();

            // idTest -> typeTest
            Map<Integer, String> idTestToType = new HashMap<>();
            for (TestPsycho t : tests) {
                idTestToType.put(t.getIdTest(), t.getTypeTest() != null ? t.getTypeTest() : "Autre");
            }

            // Par type : nb tests
            Map<String, Long> nbTestsParType = tests.stream()
                    .filter(t -> t.getTypeTest() != null && !t.getTypeTest().isEmpty())
                    .collect(Collectors.groupingBy(TestPsycho::getTypeTest, Collectors.counting()));

            // Types distincts (inclure types sans test pour cohérence)
            Set<String> types = new LinkedHashSet<>(nbTestsParType.keySet());
            for (ReponseClient r : reponses) {
                String type = idTestToType.get(r.getIdTestId());
                if (type != null) types.add(type);
            }

            List<StatistiqueType> stats = new ArrayList<>();

            for (String type : types) {
                int nbTests = nbTestsParType.getOrDefault(type, 0L).intValue();

                Set<Integer> idTestsPasses = new HashSet<>();
                int totalReponses = 0;
                int scoreTotal = 0;

                for (ReponseClient r : reponses) {
                    if (!type.equals(idTestToType.get(r.getIdTestId()))) continue;
                    idTestsPasses.add(r.getIdTestId());
                    totalReponses++;
                    scoreTotal += r.getScoreObtenu();
                }

                int nbPassations = idTestsPasses.size();
                double scoreMoyen = totalReponses > 0 ? (double) scoreTotal / totalReponses : 0;

                stats.add(new StatistiqueType(type, nbTests, nbPassations, totalReponses, scoreTotal, Math.round(scoreMoyen * 100.0) / 100.0));
            }

            // Trier par type
            stats.sort(Comparator.comparing(StatistiqueType::getTypeTest));

            listeStatsFull = stats;
            currentPage = 0;
            applyPagination();

            // Résumé global
            int totalTests = tests.size();
            int totalPassations = reponses.stream().map(ReponseClient::getIdTestId).collect(Collectors.toSet()).size();
            int totalScore = reponses.stream().mapToInt(ReponseClient::getScoreObtenu).sum();
            lblResume.setText(String.format("Total : %d test(s) | %d passation(s) | Score total : %d pts", totalTests, totalPassations, totalScore));

            // BarChart : passations par type
            if (barChartPassations != null) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Nombre de passations");
                for (StatistiqueType s : stats) {
                    series.getData().add(new XYChart.Data<>(s.getTypeTest(), s.getNbPassations()));
                }
                barChartPassations.getData().clear();
                barChartPassations.getData().add(series);
            }

            // PieChart : répartition des réponses par type
            if (pieChartTypes != null) {
                ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                for (StatistiqueType s : stats) {
                    if (s.getTotalReponses() > 0) {
                        pieData.add(new PieChart.Data(s.getTypeTest() + " (" + s.getTotalReponses() + ")", s.getTotalReponses()));
                    }
                }
                pieChartTypes.setData(pieData);
            }

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Erreur lors du chargement des statistiques : " + e.getMessage());
            alert.show();
            e.printStackTrace();
        }
    }

    @FXML
    void retour(ActionEvent event) {
        if (dashboardController != null) {
            dashboardController.navigateTo(
                    "AfficherTest.fxml",
                    "Tests psychologiques",
                    "Gestion des tests psychologiques");
            dashboardController.maintainActiveButton("tests");
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/AfficherTest.fxml"));
            tableViewStats.getScene().setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    void actualiser(ActionEvent event) {
        chargerStatistiques();
    }

    private void applyPagination() {
        int total = listeStatsFull.size();
        int totalPages = PaginationHelper.getTotalPages(total);
        currentPage = Math.min(currentPage, Math.max(0, totalPages - 1));
        List<StatistiqueType> pageItems = PaginationHelper.getPageItems(listeStatsFull, currentPage);
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
}
