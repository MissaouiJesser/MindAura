package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.scene.control.TabPane;
import tn.esprit.entities.Ressources;
import tn.esprit.services.RessourcesService;
import tn.esprit.utils.MyDataBase;

import java.sql.SQLException;
import java.util.*;

public class StatistiquesController {

    @FXML private Label lblTotalRessources;
    @FXML private Label lblTotalVues;
    @FXML private Label lblTotalLikes;
    @FXML private Label lblMoyenneVues;
    @FXML private Label lblPlusVue;
    @FXML private Label lblPlusAimee;

    @FXML private PieChart pieChartTypes;
    @FXML private PieChart pieChartCategories;
    @FXML private BarChart<String, Number> barChartVues;
    @FXML private BarChart<String, Number> barChartLikes;
    @FXML private BarChart<String, Number> barChartNiveaux;

    private RessourcesService ressourceService;
    private List<Ressources> toutesRessources;
    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    @FXML
    public void initialize() {
        ressourceService = new RessourcesService(MyDataBase.getInstance().getConx());
        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            toutesRessources = ressourceService.afficherList();
            calculerCartes();
            remplirPieChartTypes();
            remplirPieChartCategories();
            remplirBarChartVues();
            remplirBarChartLikes();
            remplirBarChartNiveaux();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void calculerCartes() {
        int total = toutesRessources.size();
        int totalVues = 0, totalLikes = 0;
        Ressources plusVue = null, plusAimee = null;
        for (Ressources r : toutesRessources) {
            totalVues  += r.getNbr_vues();
            totalLikes += r.getLikes();
            if (plusVue   == null || r.getNbr_vues() > plusVue.getNbr_vues())   plusVue   = r;
            if (plusAimee == null || r.getLikes()    > plusAimee.getLikes())     plusAimee = r;
        }
        double moyenne = total > 0 ? (double) totalVues / total : 0;
        lblTotalRessources.setText(String.valueOf(total));
        lblTotalVues.setText(formaterNombre(totalVues));
        lblTotalLikes.setText(formaterNombre(totalLikes));
        lblMoyenneVues.setText(String.format("%.0f", moyenne));
        lblPlusVue.setText(plusVue   != null ? plusVue.getTitre()   : "-");
        lblPlusAimee.setText(plusAimee != null ? plusAimee.getTitre() : "-");
    }

    // ✅ Correction 1 : plus de .name()
    private void remplirPieChartTypes() {
        Map<String, Integer> parType = new HashMap<>();
        for (Ressources r : toutesRessources) {
            if (r.getContenu() != null) {
                String type = r.getContenu();   // String directement
                parType.put(type, parType.getOrDefault(type, 0) + 1);
            }
        }
        List<PieChart.Data> data = new ArrayList<>();
        for (Map.Entry<String, Integer> e : parType.entrySet())
            data.add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        pieChartTypes.setData(FXCollections.observableArrayList(data));
        pieChartTypes.setTitle("Répartition par type");
    }

    // ✅ Correction 2 : r.getCategorie() sans .name()
    private void remplirPieChartCategories() {
        Map<String, Integer> parCat = new HashMap<>();
        for (Ressources r : toutesRessources) {
            if (r.getCategorie() != null) {
                String cat = r.getCategorie().replace("_", " ");
                parCat.put(cat, parCat.getOrDefault(cat, 0) + 1);
            }
        }
        List<PieChart.Data> data = new ArrayList<>();
        for (Map.Entry<String, Integer> e : parCat.entrySet())
            data.add(new PieChart.Data(e.getKey(), e.getValue()));
        pieChartCategories.setData(FXCollections.observableArrayList(data));
        pieChartCategories.setTitle("Répartition par catégorie");
    }

    private void remplirBarChartVues() {
        List<Ressources> triees = new ArrayList<>(toutesRessources);
        triees.sort((a, b) -> b.getNbr_vues() - a.getNbr_vues());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre de vues");
        int limite = Math.min(5, triees.size());
        for (int i = 0; i < limite; i++) {
            Ressources r = triees.get(i);
            String titre = r.getTitre() != null ? r.getTitre() : "Sans titre";
            if (titre.length() > 15) titre = titre.substring(0, 15) + "...";
            series.getData().add(new XYChart.Data<>(titre, r.getNbr_vues()));
        }
        barChartVues.getData().clear();
        barChartVues.getData().add(series);
        barChartVues.setTitle("Top 5 — Plus vues");
        barChartVues.setLegendVisible(false);
        for (XYChart.Data<String, Number> d : series.getData())
            d.getNode().setStyle("-fx-bar-fill: #2D6A4F;");
    }

    private void remplirBarChartLikes() {
        List<Ressources> triees = new ArrayList<>(toutesRessources);
        triees.sort((a, b) -> b.getLikes() - a.getLikes());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Likes");
        int limite = Math.min(5, triees.size());
        for (int i = 0; i < limite; i++) {
            Ressources r = triees.get(i);
            String titre = r.getTitre() != null ? r.getTitre() : "Sans titre";
            if (titre.length() > 15) titre = titre.substring(0, 15) + "...";
            series.getData().add(new XYChart.Data<>(titre, r.getLikes()));
        }
        barChartLikes.getData().clear();
        barChartLikes.getData().add(series);
        barChartLikes.setTitle("Top 5 — Plus aimées");
        barChartLikes.setLegendVisible(false);
        for (XYChart.Data<String, Number> d : series.getData())
            d.getNode().setStyle("-fx-bar-fill: #7B5EA7;");
    }

    // ✅ Correction 3 : r.getNiveau() sans .name()
    private void remplirBarChartNiveaux() {
        Map<String, Integer> parNiveau = new HashMap<>();
        for (Ressources r : toutesRessources) {
            if (r.getNiveau() != null) {
                String niveau = r.getNiveau();
                parNiveau.put(niveau, parNiveau.getOrDefault(niveau, 0) + 1);
            }
        }
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ressources");
        for (Map.Entry<String, Integer> e : parNiveau.entrySet())
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        barChartNiveaux.getData().clear();
        barChartNiveaux.getData().add(series);
        barChartNiveaux.setTitle("Répartition par niveau");
        barChartNiveaux.setLegendVisible(false);
    }

    private String formaterNombre(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000)     return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    @FXML
    public void actualiser() {
        chargerDonnees();
    }

    @FXML
    public void retour(ActionEvent actionEvent) {
        if (parentHomeController != null) {
            parentHomeController.loadInCenter("/FrontOffice_COMPLET.fxml");
            return;
        }
        try {
            Stage stage = (Stage) lblTotalRessources.getScene().getWindow();
            if (stage.getScene().getRoot() instanceof TabPane tabPane) {
                tabPane.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}