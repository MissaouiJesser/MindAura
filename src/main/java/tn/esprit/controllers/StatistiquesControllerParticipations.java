package tn.esprit.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.util.Duration;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.graphics.color.*;
import tn.esprit.services.StatistiquesService;
import tn.esprit.services.StatistiquesService.*;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StatistiquesControllerParticipations implements DashboardController.DashboardAware {


    @FXML private HBox  kpiRow;
    @FXML private HBox  typeDonutRow;
    @FXML private HBox  statutDonutRow;
    @FXML private HBox  barChartRow;
    @FXML private Label trendLabel;
    @FXML private VBox  top5Container;
    @FXML private VBox  completsContainer;
    @FXML private Label statusLabel;

    private final StatistiquesService statService = new StatistiquesService();
    private DashboardController dashboardController;
    private RapportGlobal rapport;


    private static final String[] TYPE_COLORS = {
            "#10B981", "#4F6EF7", "#F59E0B", "#8B5CF6", "#22D3EE",
            "#EC4899", "#14B8A6", "#F97316", "#6366F1"
    };
    private static final String[] STATUT_COLORS = {
            "#10B981", "#22D3EE", "#F59E0B", "#EF4444", "#8899AA"
    };
    private static final String[] BAR_GRADIENT_FROM = {
            "#4F6EF7", "#4F6EF7", "#4F6EF7", "#4F6EF7", "#4F6EF7",
            "#52B788", "#52B788"
    };



    @FXML
    public void initialize() throws SQLException { chargerStats(); }

    @Override
    public void setDashboardController(DashboardController c) { this.dashboardController = c; }



    @FXML
    public void chargerStats() throws SQLException {
        setStatus("Chargement…", false);
        rapport = statService.genererRapportGlobal();
        renderKPIs();
        renderTypeDonut();
        renderStatutDonut();
        renderBarChart();
        renderTop5();
        renderComplets();
        setStatus("✅ Données actualisées", true);
    }



    @FXML
    public void exporterRapport() {
        try {
            rapport = statService.genererRapportGlobal();
            String path = System.getProperty("user.home") + "/mindura_rapport_"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";
            generatePdf(path);
            showInfo("✅ PDF exporté", "Rapport sauvegardé :\n" + path);
            setStatus("Rapport PDF → " + path, true);
        } catch (Exception e) {
            showInfo("Erreur", "Impossible d'exporter : " + e.getMessage());
        }
    }



    private void renderKPIs() {
        kpiRow.getChildren().clear();
        addKpi("📅", String.valueOf(rapport.totalEvenements()),         "Événements",        "#4F6EF7");
        addKpi("👥", String.valueOf(rapport.totalParticipants()),       "Participants",       "#22D3EE");
        addKpi("✅", String.valueOf(rapport.totalConfirmes()),          "Confirmés",          "#10B981");
        addKpi("📊", String.format("%.0f%%", rapport.tauxConfirmation()), "Taux confirmation","#F59E0B");
        addKpi("🏟", String.valueOf(rapport.totalPlacesDisponibles()),  "Places disponibles", "#8B5CF6");
    }

    private void addKpi(String icon, String value, String label, String color) {
        HBox card = new HBox(14);
        card.setStyle("-fx-background-color:#141E2E;-fx-background-radius:12;" +
                "-fx-padding:16 18;-fx-border-color:#1E2D45;-fx-border-width:1;-fx-border-radius:12;");
        card.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(0);

        StackPane iconBox = new StackPane();
        iconBox.setStyle("-fx-background-color:" + color + "22;-fx-background-radius:10;" +
                "-fx-min-width:42;-fx-min-height:42;-fx-max-width:42;-fx-max-height:42;");
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:18px;");
        iconBox.getChildren().add(ico);

        VBox info = new VBox(3);
        Label val = new Label(value);
        val.setStyle("-fx-font-size:24px;-fx-font-weight:800;-fx-text-fill:" + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill:#8899AA;-fx-font-size:11px;-fx-font-weight:500;");
        info.getChildren().addAll(val, lbl);
        card.getChildren().addAll(iconBox, info);

        FadeTransition ft = new FadeTransition(Duration.millis(380), card);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        kpiRow.getChildren().add(card);
    }



    private void renderTypeDonut() {
        if (typeDonutRow == null) return;
        typeDonutRow.getChildren().clear();

        Map<String, Long> data = rapport.participantParType();
        if (data.isEmpty()) {
            typeDonutRow.getChildren().add(emptyLabel());
            return;
        }

        long total = data.values().stream().mapToLong(Long::longValue).sum();
        List<Map.Entry<String, Long>> entries = new ArrayList<>(data.entrySet());
        entries.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        StackPane donut = buildDonutChart(entries, TYPE_COLORS, total, String.valueOf(total), "total");
        VBox legend = buildLegend(entries, TYPE_COLORS, total);
        typeDonutRow.getChildren().addAll(donut, legend);
    }

    private void renderStatutDonut() {
        if (statutDonutRow == null) return;
        statutDonutRow.getChildren().clear();


        Map<String, Long> statutData = new LinkedHashMap<>();

        long actifs = rapport.top5Evenements().stream()
                .filter(s -> "Actif".equalsIgnoreCase(s.statut())).count();
        long planifies = rapport.top5Evenements().stream()
                .filter(s -> "Planifié".equalsIgnoreCase(s.statut())).count();
        long complets = rapport.evenementsComplets().size();
        long total = rapport.totalEvenements();
        long annules = Math.max(0, total - actifs - planifies - complets);

        if (actifs   > 0) statutData.put("Actif",    actifs);
        if (planifies> 0) statutData.put("Planifié", planifies);
        if (complets > 0) statutData.put("Complet",  complets);
        if (annules  > 0) statutData.put("Annulé",   annules);

        if (statutData.isEmpty()) {
            statutDonutRow.getChildren().add(emptyLabel());
            return;
        }

        long sum = statutData.values().stream().mapToLong(Long::longValue).sum();
        List<Map.Entry<String, Long>> entries = new ArrayList<>(statutData.entrySet());

        StackPane donut = buildDonutChart(entries, STATUT_COLORS, sum,
                String.valueOf(total), "événements");
        VBox legend = buildLegend(entries, STATUT_COLORS, sum);
        statutDonutRow.getChildren().addAll(donut, legend);
    }


    private StackPane buildDonutChart(List<Map.Entry<String, Long>> entries,
                                      String[] colors, long total,
                                      String centerVal, String centerSub) {
        int SIZE = 110;
        int OUTER = SIZE / 2 - 2;
        int INNER  = (int)(OUTER * 0.60);

        StackPane sp = new StackPane();
        sp.setMinSize(SIZE, SIZE);
        sp.setMaxSize(SIZE, SIZE);


        Pane arcPane = new Pane();
        arcPane.setMinSize(SIZE, SIZE);
        arcPane.setMaxSize(SIZE, SIZE);

        double startAngle = 90; // top
        for (int i = 0; i < entries.size(); i++) {
            double pct = (double) entries.get(i).getValue() / total;
            double sweep = pct * 360;
            String color = i < colors.length ? colors[i] : "#8899AA";


            Arc arc = new Arc();
            arc.setCenterX(SIZE / 2.0);
            arc.setCenterY(SIZE / 2.0);
            arc.setRadiusX(OUTER);
            arc.setRadiusY(OUTER);
            arc.setStartAngle(startAngle);
            arc.setLength(-sweep);
            arc.setType(ArcType.ROUND);
            arc.setFill(Color.TRANSPARENT);
            arc.setStroke(Color.web(color));
            arc.setStrokeWidth(OUTER - INNER);


            int idx = i;
            arc.setOnMouseEntered(e -> arc.setStroke(Color.web(color).brighter()));
            arc.setOnMouseExited(e  -> arc.setStroke(Color.web(color)));
            Tooltip.install(arc, new Tooltip(entries.get(idx).getKey() +
                    " : " + entries.get(idx).getValue() +
                    " (" + String.format("%.0f%%", (double) entries.get(idx).getValue() / total * 100) + ")"));

            arcPane.getChildren().add(arc);


            startAngle -= (sweep + 1.5);
        }


        VBox center = new VBox(1);
        center.setAlignment(Pos.CENTER);
        Label valLbl = new Label(centerVal);
        valLbl.setStyle("-fx-font-size:18px;-fx-font-weight:800;-fx-text-fill:#F1F5F9;");
        Label subLbl = new Label(centerSub);
        subLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#8899AA;");
        center.getChildren().addAll(valLbl, subLbl);

        sp.getChildren().addAll(arcPane, center);
        return sp;
    }

    private VBox buildLegend(List<Map.Entry<String, Long>> entries, String[] colors, long total) {
        VBox legend = new VBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < entries.size(); i++) {
            String color = i < colors.length ? colors[i] : "#8899AA";
            double pct = (double) entries.get(i).getValue() / total * 100;

            HBox item = new HBox(9);
            item.setAlignment(Pos.CENTER_LEFT);


            Circle dot = new Circle(5);
            dot.setFill(Color.web(color));

            VBox info = new VBox(1);
            Label nameLbl = new Label(entries.get(i).getKey());
            nameLbl.setStyle("-fx-text-fill:#C0CFDF;-fx-font-size:11.5px;-fx-font-weight:600;");
            Label valLbl = new Label(entries.get(i).getValue() + "  ·  " + String.format("%.0f%%", pct));
            valLbl.setStyle("-fx-text-fill:#8899AA;-fx-font-size:10.5px;");
            info.getChildren().addAll(nameLbl, valLbl);

            item.getChildren().addAll(dot, info);
            legend.getChildren().add(item);
        }
        return legend;
    }


    private void renderBarChart() {
        if (barChartRow == null) return;
        barChartRow.getChildren().clear();

        Map<String, Long> data = rapport.participantParMois();
        if (data.isEmpty()) {
            barChartRow.getChildren().add(emptyLabel());
            return;
        }

        long maxVal = data.values().stream().mapToLong(Long::longValue).max().orElse(1);
        double BAR_MAX_H = 100.0;
        double BAR_W    = 46.0;


        List<Long> vals = new ArrayList<>(data.values());
        if (vals.size() >= 2) {
            long last  = vals.get(vals.size() - 1);
            long prev  = vals.get(vals.size() - 2);
            if (last > prev) trendLabel.setText("↑ En hausse ce mois");
            else if (last < prev) trendLabel.setText("↓ En baisse ce mois");
            else trendLabel.setText("→ Stable");
        }

        List<Map.Entry<String, Long>> entries = new ArrayList<>(data.entrySet());
        int size = entries.size();

        for (int i = 0; i < size; i++) {
            String mois  = entries.get(i).getKey();
            long   count = entries.get(i).getValue();
            double ratio = (maxVal > 0) ? (double) count / maxVal : 0;
            double barH  = Math.max(ratio * BAR_MAX_H, 4);
            boolean isLast = (i == size - 1);


            VBox col = new VBox(4);
            col.setAlignment(Pos.BOTTOM_CENTER);
            col.setPrefWidth(BAR_W);
            col.setMinWidth(BAR_W);


            Label valLbl = new Label(String.valueOf(count));
            valLbl.setStyle("-fx-font-size:10px;-fx-font-weight:700;" +
                    "-fx-text-fill:" + (isLast ? "#34D399" : "#8899AA") + ";");


            Rectangle bar = new Rectangle(BAR_W - 8, barH);
            bar.setArcWidth(5); bar.setArcHeight(5);

            if (isLast) {
                bar.setFill(Color.web("#52B788"));

                bar.setEffect(new javafx.scene.effect.DropShadow(8, 0, 2, Color.web("#52B78880")));
            } else {

                double brightness = 0.5 + 0.5 * ((double) i / Math.max(size - 2, 1));
                bar.setFill(Color.web("#4F6EF7").interpolate(Color.web("#6B8FF8"), brightness));
            }


            bar.setOnMouseEntered(e -> bar.setOpacity(0.8));
            bar.setOnMouseExited(e  -> bar.setOpacity(1.0));
            Tooltip.install(bar, new Tooltip(mois + " : " + count + " inscription(s)"));


            String shortMois = mois.length() > 7 ? mois.substring(5) : mois;
            Label moisLbl = new Label(shortMois);
            moisLbl.setStyle("-fx-font-size:9.5px;-fx-text-fill:" +
                    (isLast ? "#52B788" : "#4B607A") + ";" +
                    (isLast ? "-fx-font-weight:800;" : ""));

            col.getChildren().addAll(valLbl, bar, moisLbl);


            ScaleTransition st = new ScaleTransition(Duration.millis(500), bar);
            bar.setScaleY(0); bar.setTranslateY(barH / 2);
            st.setDelay(Duration.millis(i * 60));
            st.setToY(1);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
            st.setOnFinished(ev -> bar.setTranslateY(0));

            barChartRow.getChildren().add(col);
        }
    }


    private void renderTop5() {
        if (top5Container == null) return;
        top5Container.getChildren().clear();

        Label header = new Label("🏆  Top 5 Événements par Participants");
        header.setStyle("-fx-text-fill:#F1F5F9;-fx-font-size:14px;-fx-font-weight:800;");
        top5Container.getChildren().add(header);

        if (rapport.top5Evenements().isEmpty()) {
            top5Container.getChildren().add(emptyLabel());
            return;
        }

        String[] rankColors = {"#EF4444", "#F59E0B", "#8B5CF6", "#22D3EE", "#8899AA"};

        for (int i = 0; i < rapport.top5Evenements().size(); i++) {
            StatEvenement s = rapport.top5Evenements().get(i);
            String rankColor = i < rankColors.length ? rankColors[i] : "#8899AA";
            top5Container.getChildren().add(buildTop5Row(i + 1, s, rankColor));
        }
    }

    private HBox buildTop5Row(int rank, StatEvenement s, String rankColor) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#111827;-fx-background-radius:10;" +
                "-fx-padding:12 14;-fx-border-color:#1E2D45;-fx-border-width:1;-fx-border-radius:10;");


        StackPane rankBox = new StackPane();
        rankBox.setStyle("-fx-background-color:" + rankColor + "22;-fx-background-radius:8;" +
                "-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;");
        Label rankLbl = new Label("#" + rank);
        rankLbl.setStyle("-fx-text-fill:" + rankColor + ";-fx-font-size:12px;-fx-font-weight:900;");
        rankBox.getChildren().add(rankLbl);


        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label titre = new Label(s.titre() != null ? s.titre() : "—");
        titre.setStyle("-fx-text-fill:#F1F5F9;-fx-font-size:13px;-fx-font-weight:700;");
        titre.setMaxWidth(240);
        titre.setEllipsisString("…");
        HBox metaRow = new HBox(12);
        Label meta = new Label(s.nbParticipants() + " participants");
        meta.setStyle("-fx-text-fill:#8899AA;-fx-font-size:11px;");
        if (s.type() != null) {
            Label typeLbl = new Label(s.type());
            typeLbl.setStyle("-fx-background-color:" + rankColor + "22;-fx-background-radius:20;" +
                    "-fx-padding:2 8;-fx-text-fill:" + rankColor + ";-fx-font-size:10px;-fx-font-weight:700;" +
                    "-fx-border-color:" + rankColor + "44;-fx-border-width:1;-fx-border-radius:20;");
            metaRow.getChildren().addAll(meta, typeLbl);
        } else {
            metaRow.getChildren().add(meta);
        }
        info.getChildren().addAll(titre, metaRow);


        VBox progressBox = new VBox(4);
        progressBox.setAlignment(Pos.CENTER_RIGHT);
        double pct = s.tauxRemplissage() * 100;
        Label pctLbl = new Label(String.format("%.0f%%", pct));
        pctLbl.setStyle("-fx-text-fill:" + rankColor + ";-fx-font-size:12px;-fx-font-weight:800;");

        StackPane barBg = new StackPane();
        barBg.setStyle("-fx-background-color:#1E2D45;-fx-background-radius:4;");
        barBg.setMinWidth(100); barBg.setMaxWidth(100);
        barBg.setMinHeight(6);  barBg.setMaxHeight(6);

        Rectangle fill = new Rectangle(Math.max(pct, 4), 6);
        fill.setArcWidth(4); fill.setArcHeight(4);
        fill.setFill(Color.web(rankColor));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        barBg.getChildren().add(fill);

        progressBox.getChildren().addAll(pctLbl, barBg);
        row.getChildren().addAll(rankBox, info, progressBox);

        FadeTransition ft = new FadeTransition(Duration.millis(300), row);
        ft.setDelay(Duration.millis(rank * 60));
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        return row;
    }

    private void renderComplets() {
        if (completsContainer == null) return;
        completsContainer.getChildren().clear();

        Label header = new Label("🔴  Événements Complets");
        header.setStyle("-fx-text-fill:#F1F5F9;-fx-font-size:14px;-fx-font-weight:800;");
        completsContainer.getChildren().add(header);

        if (rapport.evenementsComplets().isEmpty()) {
            Label none = new Label("Aucun événement complet pour l'instant.");
            none.setStyle("-fx-text-fill:#4B607A;-fx-font-size:12px;-fx-font-style:italic;");
            completsContainer.getChildren().add(none);
            return;
        }

        for (StatEvenement s : rapport.evenementsComplets()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color:#111827;-fx-background-radius:9;" +
                    "-fx-padding:10 12;-fx-border-color:#2A1215;-fx-border-width:1;-fx-border-radius:9;");

            Label dot = new Label("●");
            dot.setStyle("-fx-text-fill:#EF4444;-fx-font-size:9px;");

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);
            Label titre = new Label(s.titre() != null ? s.titre() : "—");
            titre.setStyle("-fx-text-fill:#F1F5F9;-fx-font-size:12.5px;-fx-font-weight:700;");
            titre.setMaxWidth(200);
            Label sub = new Label(s.nbParticipants() + " inscrits · 0 place dispo");
            sub.setStyle("-fx-text-fill:#8899AA;-fx-font-size:10.5px;");
            info.getChildren().addAll(titre, sub);

            Label badge = new Label("Complet");
            badge.setStyle("-fx-background-color:rgba(239,68,68,0.12);-fx-background-radius:20;" +
                    "-fx-padding:3 9;-fx-text-fill:#EF4444;-fx-font-size:10px;-fx-font-weight:700;" +
                    "-fx-border-color:rgba(239,68,68,0.30);-fx-border-width:1;-fx-border-radius:20;");

            row.getChildren().addAll(dot, info, badge);
            completsContainer.getChildren().add(row);
        }
    }



    private void generatePdf(String outputPath) throws IOException, SQLException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page1 = new PDPage(PDRectangle.A4);
            doc.addPage(page1);
            float W = page1.getMediaBox().getWidth();
            float H = page1.getMediaBox().getHeight();
            float margin = 45f;
            float y = H - margin;

            PDFont fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fontReg     = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                // Header band
                cs.setNonStrokingColor(new PDColor(new float[]{27f/255,67f/255,50f/255}, PDDeviceRGB.INSTANCE));
                cs.addRect(0, H - 80, W, 80); cs.fill();

                cs.beginText(); cs.setFont(fontBold, 22);
                cs.setNonStrokingColor(new PDColor(new float[]{1,1,1}, PDDeviceRGB.INSTANCE));
                cs.newLineAtOffset(margin, H - 52); cs.showText("MindUra"); cs.endText();

                String dateNow = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                cs.beginText(); cs.setFont(fontReg, 9);
                cs.setNonStrokingColor(new PDColor(new float[]{0.65f,0.85f,0.75f}, PDDeviceRGB.INSTANCE));
                cs.newLineAtOffset(W - margin - 120, H - 52);
                cs.showText("Généré le " + dateNow); cs.endText();

                y = H - 80 - 28;
                y = drawSectionTitle(cs, fontBold, "Indicateurs Clés", y, margin, W);

                String[][] kpis = {
                        {"Événements",          String.valueOf(rapport.totalEvenements())},
                        {"Participants",         String.valueOf(rapport.totalParticipants())},
                        {"Confirmés",            String.valueOf(rapport.totalConfirmes())},
                        {"Taux confirmation",    String.format("%.1f%%", rapport.tauxConfirmation())},
                        {"Places disponibles",   String.valueOf(rapport.totalPlacesDisponibles())}
                };
                float cardW = (W - 2 * margin - 10) / 2f, cardH = 58f, cardX = margin;
                for (int i = 0; i < kpis.length; i++) {
                    float cardY = y - (i / 2) * (cardH + 10) - cardH;
                    cardX = (i % 2 == 1) ? margin + cardW + 10 : margin;
                    cs.setNonStrokingColor(new PDColor(new float[]{0.95f,0.97f,0.98f}, PDDeviceRGB.INSTANCE));
                    cs.addRect(cardX, cardY, cardW, cardH); cs.fill();
                    cs.beginText(); cs.setFont(fontBold, 20);
                    cs.setNonStrokingColor(new PDColor(new float[]{27f/255,67f/255,50f/255}, PDDeviceRGB.INSTANCE));
                    cs.newLineAtOffset(cardX+14, cardY+30); cs.showText(kpis[i][1]); cs.endText();
                    cs.beginText(); cs.setFont(fontReg, 10);
                    cs.setNonStrokingColor(new PDColor(new float[]{0.35f,0.39f,0.46f}, PDDeviceRGB.INSTANCE));
                    cs.newLineAtOffset(cardX+14, cardY+13); cs.showText(kpis[i][0]); cs.endText();
                }
                y -= ((kpis.length+1)/2) * (cardH+10) + 20;

                y = drawSectionTitle(cs, fontBold, "Top 5 Événements", y, margin, W);
                for (int i = 0; i < rapport.top5Evenements().size(); i++) {
                    StatEvenement s = rapport.top5Evenements().get(i);
                    float rowY = y - (i * 28f) - 18f;
                    cs.beginText(); cs.setFont(fontBold, 11);
                    cs.setNonStrokingColor(new PDColor(new float[]{27f/255,67f/255,50f/255}, PDDeviceRGB.INSTANCE));
                    cs.newLineAtOffset(margin, rowY);
                    cs.showText((i+1) + ". " + truncate(s.titre(), 45)); cs.endText();
                    cs.beginText(); cs.setFont(fontReg, 10);
                    cs.setNonStrokingColor(new PDColor(new float[]{0.35f,0.39f,0.46f}, PDDeviceRGB.INSTANCE));
                    cs.newLineAtOffset(W - margin - 100, rowY);
                    cs.showText(s.nbParticipants() + " participants  " +
                            String.format("%.0f%%", s.tauxRemplissage()*100) + " plein"); cs.endText();
                }
            }
            doc.save(outputPath);
        }
    }

    private float drawSectionTitle(PDPageContentStream cs, PDFont font,
                                   String title, float y, float margin, float W) throws IOException {
        cs.beginText(); cs.setFont(font, 13);
        cs.setNonStrokingColor(new PDColor(new float[]{0.1f,0.12f,0.18f}, PDDeviceRGB.INSTANCE));
        cs.newLineAtOffset(margin, y); cs.showText(title); cs.endText();
        cs.setStrokingColor(new PDColor(new float[]{27f/255,67f/255,50f/255}, PDDeviceRGB.INSTANCE));
        cs.setLineWidth(1.5f);
        cs.moveTo(margin, y-4); cs.lineTo(W-margin, y-4); cs.stroke();
        return y - 20;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max-1) + "…" : s;
    }



    private Label emptyLabel() {
        Label l = new Label("Aucune donnée disponible");
        l.setStyle("-fx-text-fill:#4B607A;-fx-font-size:12px;-fx-font-style:italic;");
        return l;
    }

    private void setStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle(success
                ? "-fx-text-fill:#34D399;-fx-font-size:12px;-fx-font-weight:600;"
                : "-fx-text-fill:#8899AA;-fx-font-size:12px;");
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(message);
        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color:#141E2E;-fx-font-family:'Segoe UI';" +
                "-fx-border-color:#1E2D45;-fx-border-width:1;-fx-border-radius:12;-fx-background-radius:12;");
        try { dp.lookup(".content.label").setStyle("-fx-text-fill:#34D399;-fx-font-size:13px;"); }
        catch (Exception ignored) {}
        alert.showAndWait();
    }
}