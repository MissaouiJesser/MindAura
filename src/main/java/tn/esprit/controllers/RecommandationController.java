package tn.esprit.controllers;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_local;
import tn.esprit.enums.StatutReservation;
import tn.esprit.enums.typeL;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.reservation_local_SERVICE;

/**
 * ✅ SYSTÈME DE RECOMMANDATION INTELLIGENT
 *
 * Ce contrôleur analyse l'historique des réservations de l'utilisateur
 * et recommande les locaux les plus adaptés selon :
 *   - Le type de local le plus réservé (HISTORIQUE)
 *   - La ville préférée (LOCALISATION)
 *   - La disponibilité en temps réel
 *   - Un score de pertinence calculé automatiquement
 *
 * FXML associé : Recommandation.fxml
 */
public class RecommandationController implements Initializable {

    // ============================================================
    //  FXML – éléments de l'interface
    // ============================================================

    @FXML private VBox containerRecommandations;   // conteneur principal des cartes
    @FXML private Label lblTitreSection;           // titre dynamique
    @FXML private Label lblSousTitle;              // sous-titre avec le profil détecté
    @FXML private ComboBox<String> comboProfil;    // profil manuel (optionnel)
    @FXML private Button btnAnalyser;              // bouton déclencher l'analyse
    @FXML private Button btnRetour;                // retour à l'accueil
    @FXML private ProgressBar progressAnalyse;     // barre de chargement
    @FXML private Label lblStatuts;                // "X recommandations trouvées"
    @FXML private ScrollPane scrollPane;           // scroll autour du container

    // ============================================================
    //  Services
    // ============================================================

    private local_psychiatrie_SERVICE localService;
    private reservation_local_SERVICE reservationService;

    // Référence au contrôleur parent UserHome pour la navigation inline
    private UserHomeController parentHomeController;

    /**
     * Injecté par UserHomeController.loadInCenter() pour permettre
     * la navigation retour sans créer de nouvelle scène.
     */
    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    // ============================================================
    //  Données
    // ============================================================

    private List<local_psychiatrie> tousLesLocaux;
    private List<reservation_local> historiqueReservations;

    // Identifiant de l'utilisateur connecté (à adapter selon votre système d'auth)
    private static final int ID_UTILISATEUR = 1;

    // ============================================================
    //  Initialisation
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        localService     = new local_psychiatrie_SERVICE();
        reservationService = new reservation_local_SERVICE();

        // Profils disponibles pour le filtre manuel
        comboProfil.setItems(FXCollections.observableArrayList(
                "🤖 Automatique (basé sur l'historique)",
                "🏥 Hôpital / Clinique",
                "🛋️ Cabinet privé",
                "🌿 Centre de bien-être",
                "🧠 Centre de santé mentale",
                "💆 Espace de thérapie"
        ));
        comboProfil.setValue("🤖 Automatique (basé sur l'historique)");

        // Charger et analyser automatiquement au démarrage
        chargerDonnees();
        lancerAnalyse();

        applyFadeIn();
    }

    // ============================================================
    //  Chargement des données
    // ============================================================

    private void chargerDonnees() {
        try {
            tousLesLocaux          = localService.afficherList();
            historiqueReservations = reservationService.afficherList(); // toutes réservations
        } catch (SQLException e) {
            tousLesLocaux          = new ArrayList<>();
            historiqueReservations = new ArrayList<>();
            showError("Impossible de charger les données : " + e.getMessage());
        }
    }

    // ============================================================
    //  Analyse et calcul des recommandations
    // ============================================================

    /**
     * Analyse l'historique et affiche les recommandations triées par score.
     */
    @FXML
    private void lancerAnalyse() {
        containerRecommandations.getChildren().clear();

        if (progressAnalyse != null) {
            progressAnalyse.setVisible(true);
            progressAnalyse.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }

        // Simuler un court délai d'analyse (UX)
        new Thread(() -> {
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}

            javafx.application.Platform.runLater(() -> {
                // --- 1. Profil utilisateur détecté ---
                ProfilUtilisateur profil = analyserProfil();

                // --- 2. Calcul des scores ---
                List<LocalScore> scores = calculerScores(profil);

                // --- 3. Top 5 recommandations ---
                List<LocalScore> top5 = scores.stream()
                        .filter(s -> s.getScore() > 0)
                        .sorted(Comparator.comparingDouble(LocalScore::getScore).reversed())
                        .limit(5)
                        .collect(Collectors.toList());

                // --- 4. Affichage ---
                afficherRecommandations(top5, profil);

                if (progressAnalyse != null) progressAnalyse.setVisible(false);
                if (lblStatuts != null)
                    lblStatuts.setText("✨ " + top5.size() + " recommandations personnalisées trouvées");
            });
        }).start();
    }

    /**
     * Analyse le profil de l'utilisateur depuis son historique.
     */
    private ProfilUtilisateur analyserProfil() {
        ProfilUtilisateur profil = new ProfilUtilisateur();

        // Filtrer les réservations confirmées ou terminées de cet utilisateur
        List<reservation_local> mesReservations = historiqueReservations.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.CONFIRMEE
                        || r.getStatus_reservation() == StatutReservation.TERMINEE)
                .collect(Collectors.toList());

        profil.setNbReservations(mesReservations.size());

        if (mesReservations.isEmpty()) {
            // Pas d'historique → profil généraliste
            profil.setTypePreferee(null);
            profil.setVillePreferee(null);
            profil.setDescription("Nouvel utilisateur – recommandations générales");
            return profil;
        }

        // Trouver le local le plus réservé
        Map<Integer, Long> freqLocal = mesReservations.stream()
                .collect(Collectors.groupingBy(reservation_local::getId_local, Collectors.counting()));

        int idLocalPlusReserve = freqLocal.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        // Ville préférée
        Map<String, Long> freqVille = new HashMap<>();
        for (reservation_local r : mesReservations) {
            tousLesLocaux.stream()
                    .filter(l -> l.getId_local() == r.getId_local())
                    .map(local_psychiatrie::getVille_local)
                    .findFirst()
                    .ifPresent(v -> freqVille.merge(v, 1L, Long::sum));
        }
        String villePref = freqVille.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Type préféré
        typeL typePref = null;
        if (idLocalPlusReserve != -1) {
            int finalId = idLocalPlusReserve;
            typePref = tousLesLocaux.stream()
                    .filter(l -> l.getId_local() == finalId)
                    .map(local_psychiatrie::getType_local)
                    .findFirst()
                    .orElse(null);
        }

        profil.setTypePreferee(typePref);
        profil.setVillePreferee(villePref);
        profil.setDescription(construireDescriptionProfil(typePref, villePref, mesReservations.size()));

        // Surcharger si l'utilisateur a choisi un profil manuel
        String choixManuel = comboProfil != null ? comboProfil.getValue() : null;
        if (choixManuel != null && !choixManuel.startsWith("🤖")) {
            typePref = parseTypeManuel(choixManuel);
            profil.setTypePreferee(typePref);
        }

        return profil;
    }

    private typeL parseTypeManuel(String choix) {
        if (choix.contains("Hôpital"))       return typeL.HOPITAL;
        if (choix.contains("Cabinet"))       return typeL.CABINET_PRIVE;
        if (choix.contains("bien-être"))     return typeL.CENTRE_DE_BIEN_ETRE;
        if (choix.contains("santé mentale")) return typeL.CENTRE_DE_SANTE_MENTALE;
        if (choix.contains("thérapie"))      return typeL.ESPACE_DE_THERAPIE;
        return null;
    }

    private String construireDescriptionProfil(typeL type, String ville, int nbRes) {
        StringBuilder sb = new StringBuilder("Profil détecté : ");
        if (type != null)  sb.append(type.getLibelle()).append(" • ");
        if (ville != null) sb.append("Préférence pour ").append(ville).append(" • ");
        sb.append(nbRes).append(" réservation(s) passée(s)");
        return sb.toString();
    }

    /**
     * Calcule un score de pertinence pour chaque local.
     *
     * Critères et poids :
     *   +40 pts  → même type que le type préféré
     *   +30 pts  → même ville que la ville préférée
     *   +20 pts  → local "Disponible"
     *   +10 pts  → local jamais essayé (découverte)
     *   - 5 pts  → local "Non disponible"
     */
    private List<LocalScore> calculerScores(ProfilUtilisateur profil) {
        Set<Integer> locauxDejaPris = historiqueReservations.stream()
                .map(reservation_local::getId_local)
                .collect(Collectors.toSet());

        List<LocalScore> scores = new ArrayList<>();

        for (local_psychiatrie local : tousLesLocaux) {
            double score = 0;
            List<String> raisons = new ArrayList<>();

            // Critère 1 : type préféré
            if (profil.getTypePreferee() != null && profil.getTypePreferee() == local.getType_local()) {
                score += 40;
                raisons.add("✅ Type correspondant à votre profil");
            }

            // Critère 2 : ville préférée
            if (profil.getVillePreferee() != null && profil.getVillePreferee().equals(local.getVille_local())) {
                score += 30;
                raisons.add("📍 Dans votre ville préférée");
            }

            // Critère 3 : disponibilité
            if ("Disponible".equals(local.getDisponibilite_local())) {
                score += 20;
                raisons.add("🟢 Disponible maintenant");
            } else if ("Non disponible".equals(local.getDisponibilite_local())) {
                score -= 5;
            }

            // Critère 4 : nouveau lieu (découverte)
            if (!locauxDejaPris.contains(local.getId_local())) {
                score += 10;
                raisons.add("✨ Nouvelle découverte pour vous");
            } else {
                raisons.add("🔁 Déjà visité");
            }

            // Bonus si pas d'historique → tout est recommandé
            if (profil.getNbReservations() == 0) {
                score += 30;
                raisons.add("🌟 Recommandation populaire");
            }

            scores.add(new LocalScore(local, score, raisons));
        }

        return scores;
    }

    // ============================================================
    //  Affichage des recommandations
    // ============================================================

    private void afficherRecommandations(List<LocalScore> top5, ProfilUtilisateur profil) {
        containerRecommandations.getChildren().clear();

        // Mettre à jour le sous-titre
        if (lblSousTitle != null)
            lblSousTitle.setText(profil.getDescription());

        if (top5.isEmpty()) {
            Label lblVide = new Label("Aucune recommandation disponible pour le moment.");
            lblVide.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 14px; -fx-padding: 20px;");
            containerRecommandations.getChildren().add(lblVide);
            return;
        }

        int rang = 1;
        for (LocalScore ls : top5) {
            VBox carte = creerCarteRecommandation(ls, rang);
            containerRecommandations.getChildren().add(carte);

            // Animation décalée pour chaque carte
            int finalRang = rang;
            FadeTransition ft = new FadeTransition(Duration.millis(400), carte);
            ft.setDelay(Duration.millis(finalRang * 120L));
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();

            rang++;
        }
    }

    /**
     * Crée une carte visuelle pour une recommandation.
     */
    private VBox creerCarteRecommandation(LocalScore ls, int rang) {
        local_psychiatrie local = ls.getLocal();

        VBox carte = new VBox(10);
        carte.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 16px; " +
                        "-fx-padding: 20px; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3); " +
                        "-fx-border-color: " + (rang == 1 ? "#7B5EA7" : "#E5E7EB") + "; " +
                        "-fx-border-width: " + (rang == 1 ? "2px" : "1px") + "; " +
                        "-fx-border-radius: 16px; " +
                        "-fx-cursor: hand;"
        );
        carte.setPrefWidth(700);

        // ─── Ligne 1 : rang + nom ───
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // Badge de rang
        Label lblRang = new Label(getMedaille(rang));
        lblRang.setStyle("-fx-font-size: 28px;");

        // Infos du local
        VBox infosBox = new VBox(4);

        Label lblNom = new Label(local.getNom_local());
        lblNom.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1F2937;");

        Label lblVilleType = new Label("📍 " + local.getVille_local() + "  •  🏷️ " + local.getType_local().getLibelle());
        lblVilleType.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        Label lblAdresse = new Label("🗺️ " + local.getAdresse_local());
        lblAdresse.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");

        infosBox.getChildren().addAll(lblNom, lblVilleType, lblAdresse);

        // Score
        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);
        Label lblScore = new Label(String.format("%.0f pts", ls.getScore()));
        lblScore.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7B5EA7;");
        Label lblScoreLib = new Label("Score");
        lblScoreLib.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");
        scoreBox.getChildren().addAll(lblScore, lblScoreLib);

        HBox.setHgrow(infosBox, Priority.ALWAYS);
        headerBox.getChildren().addAll(lblRang, infosBox, scoreBox);

        // ─── Ligne 2 : raisons ───
        FlowPane raisonsPane = new FlowPane(8, 6);
        for (String raison : ls.getRaisons()) {
            Label chip = new Label(raison);
            chip.setStyle(
                    "-fx-background-color: #F3F4F6; " +
                            "-fx-text-fill: #374151; " +
                            "-fx-font-size: 11px; " +
                            "-fx-padding: 4px 10px; " +
                            "-fx-background-radius: 20px;"
            );
            raisonsPane.getChildren().add(chip);
        }

        // ─── Ligne 3 : badge disponibilité + bouton réserver ───
        HBox footerBox = new HBox(15);
        footerBox.setAlignment(Pos.CENTER_LEFT);

        Label badgeDispo = new Label();
        if ("Disponible".equals(local.getDisponibilite_local())) {
            badgeDispo.setText("🟢 Disponible");
            badgeDispo.setStyle("-fx-background-color: #D8F3DC; -fx-text-fill: #1B4332; " +
                    "-fx-padding: 6px 14px; -fx-background-radius: 20px; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            badgeDispo.setText("🔴 " + local.getDisponibilite_local());
            badgeDispo.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #7F1D1D; " +
                    "-fx-padding: 6px 14px; -fx-background-radius: 20px; -fx-font-size: 12px;");
        }

        Label lblTel = new Label("📞 " + local.getTelephone_local());
        lblTel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        HBox.setHgrow(new Region(), Priority.ALWAYS);

        Button btnReserver = new Button("📅 Réserver");
        btnReserver.setStyle(
                "-fx-background-color: linear-gradient(to right, #7B5EA7, #9B7DC4); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 20px; " +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-font-size: 13px;"
        );
        btnReserver.setDisable(!"Disponible".equals(local.getDisponibilite_local()));

        btnReserver.setOnAction(e -> ouvrirReservation(local));

        // Hover sur le bouton
        btnReserver.setOnMouseEntered(e -> btnReserver.setStyle(
                "-fx-background-color: linear-gradient(to right, #5A3A7B, #7B5EA7); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 20px; " +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-font-size: 13px;"));
        btnReserver.setOnMouseExited(e -> btnReserver.setStyle(
                "-fx-background-color: linear-gradient(to right, #7B5EA7, #9B7DC4); " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 20px; " +
                        "-fx-background-radius: 10px; -fx-cursor: hand; -fx-font-size: 13px;"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerBox.getChildren().addAll(badgeDispo, lblTel, spacer, btnReserver);

        // Hover sur la carte entière
        carte.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), carte);
            st.setToX(1.01); st.setToY(1.01); st.play();
        });
        carte.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(150), carte);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        carte.getChildren().addAll(headerBox, new Separator(), raisonsPane, footerBox);
        return carte;
    }

    private String getMedaille(int rang) {
        switch (rang) {
            case 1: return "🥇";
            case 2: return "🥈";
            case 3: return "🥉";
            default: return "⭐";
        }
    }

    // ============================================================
    //  Navigation
    // ============================================================

    private void ouvrirReservation(local_psychiatrie local) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterReservation.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Réserver – " + local.getNom_local());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setMinWidth(800);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);

            // Pré-remplir le local dans le formulaire
            AjouterReservationController ctrl = loader.getController();
            ctrl.setLocalPreselectionne(local);

            stage.showAndWait();
            lancerAnalyse(); // rafraîchir après réservation

        } catch (IOException e) {
            showError("Impossible d'ouvrir le formulaire de réservation : " + e.getMessage());
        }
    }

    // ─── Navigation navbar UserHome (Accueil / Services / Equipe / Contact) ──────────────────

    @FXML
    private void handleNavigateToAccueil() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToAccueil(); return; }
    }

    @FXML
    private void handleNavigateToServices() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToServices(); return; }
    }

    @FXML
    private void handleNavigateToEquipe() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToEquipe(); return; }
    }

    @FXML
    private void handleNavigateToContact() {
        if (parentHomeController != null) { parentHomeController.restoreHomeAndScrollToContact(); return; }
    }

    @FXML
    private void handleRetour() {
        // Si chargé dans UserHome, retourner à FrontOfficeAccueil dans le même centre
        if (parentHomeController != null) {
            parentHomeController.loadFrontOfficeAccueil();
            return;
        }
        // Sinon comportement par défaut : nouvelle scène
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOfficeAccueil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Navigation impossible : " + e.getMessage());
        }
    }

    // ============================================================
    //  Utilitaires
    // ============================================================

    private void applyFadeIn() {
        if (containerRecommandations != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(700), containerRecommandations);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur"); alert.setHeaderText(null); alert.setContentText(msg);
        alert.showAndWait();
    }

    // ============================================================
    //  Classes internes
    // ============================================================

    /** Représente le profil de l'utilisateur détecté automatiquement. */
    private static class ProfilUtilisateur {
        private typeL    typePreferee;
        private String   villePreferee;
        private int      nbReservations;
        private String   description = "";

        public typeL  getTypePreferee()   { return typePreferee; }
        public String getVillePreferee()  { return villePreferee; }
        public int    getNbReservations() { return nbReservations; }
        public String getDescription()    { return description; }

        public void setTypePreferee(typeL t)     { this.typePreferee   = t; }
        public void setVillePreferee(String v)   { this.villePreferee  = v; }
        public void setNbReservations(int n)     { this.nbReservations = n; }
        public void setDescription(String d)     { this.description    = d; }
    }

    /** Associe un local à son score et aux raisons de recommandation. */
    private static class LocalScore {
        private final local_psychiatrie local;
        private final double            score;
        private final List<String>      raisons;

        public LocalScore(local_psychiatrie local, double score, List<String> raisons) {
            this.local   = local;
            this.score   = score;
            this.raisons = raisons;
        }

        public local_psychiatrie getLocal()   { return local;   }
        public double            getScore()   { return score;   }
        public List<String>      getRaisons() { return raisons; }
    }
}