package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import tn.esprit.entities.QuestionReponse;
import tn.esprit.entities.ReponseClient;
import tn.esprit.entities.TestPsycho;
import tn.esprit.services.GeminiService;
import tn.esprit.services.QuestionReponseService;
import tn.esprit.services.ReponseClientService;
import tn.esprit.services.TestPsychoService;
import tn.esprit.utils.NavigationManager;
import tn.esprit.utils.PaginationHelper;
import tn.esprit.utils.SessionManager;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class MesResultatsController implements Initializable {

    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    // =========================================================================
    //  INNER CLASSES
    // =========================================================================

    /** Données d'un test pour l'affichage dans "Mes Réponses". */
    public static class ResultatTest {
        private final String titreTest;
        private final Date datePassation;
        private final int scoreTotal;
        private final List<LigneReponse> lignes;

        public ResultatTest(String titreTest, Date datePassation, int scoreTotal, List<LigneReponse> lignes) {
            this.titreTest    = titreTest;
            this.datePassation = datePassation;
            this.scoreTotal   = scoreTotal;
            this.lignes       = lignes != null ? lignes : new ArrayList<>();
        }

        public String getTitreTest()        { return titreTest; }
        public Date getDatePassation()      { return datePassation; }
        public int getScoreTotal()          { return scoreTotal; }
        public List<LigneReponse> getLignes() { return lignes; }
    }

    /** Une question/réponse à afficher sous un test. */
    public static class LigneReponse {
        private final String texteQuestion;
        private final String reponseTexte;
        private final int score;

        public LigneReponse(String texteQuestion, String reponseTexte, int score) {
            this.texteQuestion = texteQuestion;
            this.reponseTexte  = reponseTexte != null ? reponseTexte : "—";
            this.score         = score;
        }

        public String getTexteQuestion() { return texteQuestion; }
        public String getReponseTexte()  { return reponseTexte; }
        public int getScore()            { return score; }
    }

    // =========================================================================
    //  FXML
    // =========================================================================

    @FXML private ListView<ResultatTest> listViewReponses;
    @FXML private Label userNameLabel;
    @FXML private javafx.scene.layout.HBox paginationContainer;

    // ── Champs IA ────────────────────────────────────────
    @FXML private TextArea        aiAnalysisArea;
    @FXML private ProgressIndicator aiLoading;
    @FXML private Button          analyzeBtn;
    @FXML private Label           selectedTestLabel;

    // ─────────────────────────────────────────────────────
    private List<ResultatTest> listeResultatsFull = new ArrayList<>();
    private int currentPage = 0;

    private String testName = "";
    private int score       = 0;
    private int maxScore    = 0;

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        if (!SessionManager.isLoggedIn() || SessionManager.getCurrentUser() == null) {
            NavigationManager.navigateTo("Login.fxml", "MindAura – Connexion",
                    NavigationManager.getStage(listViewReponses), false);
            return;
        }

        listViewReponses.setPlaceholder(new Label(
                "Aucune réponse trouvée.\nPassez un test pour voir vos résultats ici !"));

        listViewReponses.setCellFactory(param -> new ListCell<ResultatTest>() {
            @Override
            protected void updateItem(ResultatTest resultat, boolean empty) {
                super.updateItem(resultat, empty);
                if (empty || resultat == null) { setText(null); setGraphic(null); }
                else { setText(null); setGraphic(buildCard(resultat)); }
            }
        });

        // Sélection d'un test → prépare l'analyse IA
        listViewReponses.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, selected) -> {
                    if (selected != null) {
                        testName = selected.getTitreTest();
                        score    = selected.getScoreTotal();
                        maxScore = selected.getLignes().size() * 4;
                        if (maxScore == 0) maxScore = 20;

                        selectedTestLabel.setText("✅ Test sélectionné : " + testName +
                                " (" + score + "/" + maxScore + " pts)");
                        aiAnalysisArea.clear();
                        aiAnalysisArea.setPromptText(
                                "Cliquez sur 'Analyser avec l'IA' pour obtenir votre analyse...");
                    }
                }
        );

        chargerReponses();
    }

    public void initData(String testName, int score, int maxScore) {
        this.testName = testName;
        this.score    = score;
        this.maxScore = maxScore;
    }

    // =========================================================================
    //  ANALYSE IA
    // =========================================================================

    @FXML
    public void analyserAvecIA() {
        if (testName == null || testName.isEmpty()) {
            aiAnalysisArea.setText("⚠️ Veuillez d'abord sélectionner un test dans la liste.");
            return;
        }

        aiLoading.setVisible(true);
        analyzeBtn.setDisable(true);
        aiAnalysisArea.setText("⏳ Analyse en cours, veuillez patienter...");

        double pourcentage = maxScore > 0 ? (score * 100.0 / maxScore) : 0;

        String prompt = "Tu es un psychologue bienveillant et professionnel. " +
                "Un utilisateur a passé le test psychologique \"" + testName + "\". " +
                "Score obtenu : " + score + " sur " + maxScore +
                " (soit " + String.format("%.0f", pourcentage) + "%). " +
                "1. Donne une interprétation empathique et professionnelle en 3-4 phrases. " +
                "2. Identifie les points forts de ce résultat. " +
                "3. Suggère 2-3 axes d'amélioration concrets. " +
                "Réponds en français, de façon claire, encourageante et structurée.";

        new Thread(() -> {
            String result = GeminiService.askGemini(prompt);
            javafx.application.Platform.runLater(() -> {
                aiAnalysisArea.setText(result);
                aiLoading.setVisible(false);
                analyzeBtn.setDisable(false);
            });
        }).start();
    }

    // =========================================================================
    //  CHARGEMENT DES RÉPONSES
    // =========================================================================

    private void chargerReponses() {
        ReponseClientService reponseService  = new ReponseClientService();
        QuestionReponseService questionService = new QuestionReponseService();
        TestPsychoService      testService     = new TestPsychoService();

        try {
            if (!SessionManager.isLoggedIn() || SessionManager.getCurrentUser() == null) return;

            // ✅ userId est un int (utilisateur_id dans la DB est int(11))
            int userId = Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur());

            List<ReponseClient> toutesReponses = reponseService.afficherList();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            Map<String, List<ReponseClient>> reponsesParTest = new LinkedHashMap<>();

            for (ReponseClient reponse : toutesReponses) {
                // ✅ getUtilisateurId() retourne int — comparaison directe
                if (reponse.getUtilisateurId() != userId) continue;

                // ✅ getIdTestId() — nouveau getter FK
                String cle = reponse.getIdTestId() + "_" + sdf.format(reponse.getDateReponse());
                reponsesParTest.computeIfAbsent(cle, k -> new ArrayList<>()).add(reponse);
            }

            List<ResultatTest> resultats = new ArrayList<>();

            for (Map.Entry<String, List<ReponseClient>> entry : reponsesParTest.entrySet()) {
                // ✅ getIdTestId() — nouveau getter
                int idTest = entry.getValue().get(0).getIdTestId();
                List<ReponseClient> reponses = entry.getValue();

                TestPsycho test  = testService.getTestById(idTest);
                String nomTest   = (test != null) ? test.getTitreTest() : "Test #" + idTest;

                int scoreTotal       = 0;
                Date datePassation   = new Date();
                List<LigneReponse> lignes = new ArrayList<>();

                for (ReponseClient reponse : reponses) {
                    scoreTotal    += reponse.getScoreObtenu();
                    datePassation  = reponse.getDateReponse();

                    // ✅ getIdQuestionReponseId() — nouveau getter FK
                    QuestionReponse question = questionService.getQuestionById(
                            reponse.getIdQuestionReponseId());

                    String texteQuestion = (question != null)
                            ? question.getTexteQuestion()
                            : "Question #" + reponse.getIdQuestionReponseId();

                    String reponseAffichee = reponse.getOptionChoisie() != null
                            ? reponse.getOptionChoisie()
                            : reponse.getReponseTexteLibre();

                    lignes.add(new LigneReponse(texteQuestion, reponseAffichee, reponse.getScoreObtenu()));
                }

                resultats.add(new ResultatTest(nomTest, datePassation, scoreTotal, lignes));
            }

            listeResultatsFull = resultats;
            currentPage = 0;
            applyPagination();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  PAGINATION
    // =========================================================================

    private void applyPagination() {
        int total      = listeResultatsFull.size();
        int totalPages = PaginationHelper.getTotalPages(total);
        currentPage    = Math.min(currentPage, Math.max(0, totalPages - 1));

        List<ResultatTest> pageItems = PaginationHelper.getPageItems(listeResultatsFull, currentPage);
        listViewReponses.setItems(FXCollections.observableArrayList(pageItems));

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

    // =========================================================================
    //  UI — CARTE D'UN RÉSULTAT
    // =========================================================================

    private VBox buildCard(ResultatTest resultat) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm");

        VBox header = new VBox(6);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: #7B5EA7; -fx-background-radius: 10 10 0 0;");
        header.setMaxWidth(Double.MAX_VALUE);

        Label titre = new Label(resultat.getTitreTest());
        titre.setFont(Font.font("System", FontWeight.BOLD, 18));
        titre.setStyle("-fx-text-fill: white; -fx-wrap-text: true;");

        Label infos = new Label("📅 " + dateFormat.format(resultat.getDatePassation()) +
                "  •  ⭐ Score total : " + resultat.getScoreTotal() + " pts");
        infos.setStyle("-fx-text-fill: rgba(255,255,255,0.95); -fx-font-size: 13px;");
        header.getChildren().addAll(titre, infos);

        VBox blocReponses = new VBox(12);
        blocReponses.setPadding(new Insets(16, 20, 20, 20));
        blocReponses.setStyle("-fx-background-color: white; -fx-background-radius: 0 0 10 10; " +
                "-fx-border-color: #e8ecf0; -fx-border-width: 0 1 1 1; -fx-border-radius: 0 0 10 10;");

        int num = 1;
        for (LigneReponse ligne : resultat.getLignes()) {
            VBox ligneBox = new VBox(4);
            ligneBox.setPadding(new Insets(12, 14, 12, 14));
            ligneBox.setStyle("-fx-background-color: #F8FAF9; -fx-background-radius: 8; " +
                    "-fx-border-color: #e8ecf0; -fx-border-width: 1; -fx-border-radius: 8;");

            Label question = new Label("Question " + num + " : " + ligne.getTexteQuestion());
            question.setFont(Font.font("System", FontWeight.BOLD, 14));
            question.setStyle("-fx-text-fill: #1A1A2E; -fx-wrap-text: true;");
            question.setWrapText(true);

            Label reponse = new Label("➤ Réponse : " + ligne.getReponseTexte());
            reponse.setStyle("-fx-text-fill: #5A6475; -fx-font-size: 13px; -fx-wrap-text: true;");
            reponse.setWrapText(true);

            Label scoreLabel = new Label("Score : " + ligne.getScore() + " pt(s)");
            scoreLabel.setStyle("-fx-text-fill: #2D6A4F; -fx-font-size: 12px; -fx-font-weight: bold;");

            ligneBox.getChildren().addAll(question, reponse, scoreLabel);
            blocReponses.getChildren().add(ligneBox);
            num++;
        }

        VBox card = new VBox(0);
        card.setStyle("-fx-background-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");
        card.getChildren().addAll(header, blocReponses);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // =========================================================================
    //  NAVIGATION
    // =========================================================================

    @FXML
    void retour(ActionEvent event) {
        if (parentHomeController != null) { parentHomeController.loadChoisirTest(); return; }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ChoisirTest.fxml"));
            listViewReponses.getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    void handleNavigateToChoisirTest(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.loadChoisirTest();
    }

    @FXML
    void handleNavigateToMesObjectifs(ActionEvent event) {
        if (parentHomeController != null) parentHomeController.loadMesObjectifs();
    }

    // =========================================================================
    //  EXPORT PDF
    // =========================================================================

    @FXML
    void telechargerPDF(ActionEvent event) {
        List<ResultatTest> resultats = new ArrayList<>(listeResultatsFull);
        if (resultats.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucune réponse à exporter. Passez un test pour avoir des résultats.").showAndWait();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer les résultats en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (.pdf)", "*.pdf"));
        fc.setInitialFileName("mes_resultats_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf");

        File file = fc.showSaveDialog(listViewReponses.getScene().getWindow());
        if (file == null) return;

        try {
            exporterEnPDF(resultats, file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION,
                    "Les résultats ont été enregistrés dans :\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de créer le PDF : " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    private void exporterEnPDF(List<ResultatTest> resultats, String cheminFichier)
            throws DocumentException, IOException {

        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(document, new FileOutputStream(cheminFichier));
        document.open();

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy à HH:mm");

        com.lowagie.text.Font fontTitre    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
        com.lowagie.text.Font fontSousTitre = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY);
        com.lowagie.text.Font fontNormal   = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);
        com.lowagie.text.Font fontInfo     = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY);

        document.add(new Paragraph("Mes Réponses aux Tests", fontTitre));
        document.add(new Paragraph(" "));

        for (ResultatTest r : resultats) {
            document.add(new Paragraph(r.getTitreTest(), fontSousTitre));
            document.add(new Paragraph("Date : " + dateFormat.format(r.getDatePassation()) +
                    "  |  Score total : " + r.getScoreTotal() + " pts", fontInfo));
            document.add(new Paragraph(" "));
            int num = 1;
            for (LigneReponse ligne : r.getLignes()) {
                document.add(new Paragraph("Question " + num + " : " + ligne.getTexteQuestion(), fontNormal));
                document.add(new Paragraph("   Réponse : " + ligne.getReponseTexte(), fontInfo));
                document.add(new Paragraph("   Score : " + ligne.getScore() + " pt(s)", fontInfo));
                document.add(new Paragraph(" "));
                num++;
            }
            document.add(new Paragraph("————————————————————————————————————"));
            document.add(new Paragraph(" "));
        }
        document.close();
    }

    // =========================================================================
    //  EXPORT EXCEL
    // =========================================================================

    @FXML
    void telechargerExcel(ActionEvent event) {
        List<ResultatTest> resultats = new ArrayList<>(listeResultatsFull);
        if (resultats.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucune réponse à exporter. Passez un test pour avoir des résultats.").showAndWait();
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer les résultats en Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (.xlsx)", "*.xlsx"));
        fc.setInitialFileName("mes_resultats_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx");

        File file = fc.showSaveDialog(listViewReponses.getScene().getWindow());
        if (file == null) return;

        try {
            exporterEnExcel(resultats, file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION,
                    "Les résultats ont été enregistrés dans :\n" + file.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Impossible de créer le fichier Excel : " + e.getMessage()).showAndWait();
            e.printStackTrace();
        }
    }

    private void exporterEnExcel(List<ResultatTest> resultats, String cheminFichier) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Mes Réponses");

            CellStyle styleHeader = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fontHeader = workbook.createFont();
            fontHeader.setBold(true);
            styleHeader.setFont(fontHeader);

            CellStyle styleTest = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fontTest = workbook.createFont();
            fontTest.setBold(true);
            fontTest.setFontHeightInPoints((short) 12);
            styleTest.setFont(fontTest);

            int rowNum = 0;
            for (ResultatTest r : resultats) {
                Row rowTest = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell cellTest = rowTest.createCell(0);
                cellTest.setCellValue(r.getTitreTest());
                cellTest.setCellStyle(styleTest);

                Row rowInfos = sheet.createRow(rowNum++);
                rowInfos.createCell(0).setCellValue("Date : " +
                        new SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.getDatePassation()));
                rowInfos.createCell(1).setCellValue("Score total : " + r.getScoreTotal() + " pts");

                Row rowEntetes = sheet.createRow(rowNum++);
                org.apache.poi.ss.usermodel.Cell c0 = rowEntetes.createCell(0); c0.setCellValue("N°");  c0.setCellStyle(styleHeader);
                org.apache.poi.ss.usermodel.Cell c1 = rowEntetes.createCell(1); c1.setCellValue("Question"); c1.setCellStyle(styleHeader);
                org.apache.poi.ss.usermodel.Cell c2 = rowEntetes.createCell(2); c2.setCellValue("Réponse"); c2.setCellStyle(styleHeader);
                org.apache.poi.ss.usermodel.Cell c3 = rowEntetes.createCell(3); c3.setCellValue("Score"); c3.setCellStyle(styleHeader);

                int num = 1;
                for (LigneReponse ligne : r.getLignes()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(num++);
                    row.createCell(1).setCellValue(ligne.getTexteQuestion());
                    row.createCell(2).setCellValue(ligne.getReponseTexte());
                    row.createCell(3).setCellValue(ligne.getScore());
                }
                rowNum++;
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            sheet.autoSizeColumn(3);

            try (FileOutputStream out = new FileOutputStream(cheminFichier)) {
                workbook.write(out);
            }
        }
    }
}