package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_local;
import tn.esprit.enums.StatutReservation;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.services.reservation_local_SERVICE;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ✅ CALENDRIER INTERACTIF DES RÉSERVATIONS
 *
 * Affiche un calendrier mensuel avec :
 *   - Les créneaux réservés (colorés selon le statut)
 *   - Navigation mois par mois
 *   - Clic sur un jour → détail des réservations du jour
 *   - Taux d'occupation par jour (heatmap)
 *   - Bouton "Réserver" depuis le calendrier
 *
 * FXML associé : CalendrierReservation.fxml
 */
public class CalendrierReservationController implements Initializable {

    // ============================================================
    //  FXML
    // ============================================================

    @FXML private GridPane calendrierGrid;          // grille 7x7 du calendrier
    @FXML private Label     lblMoisAnnee;           // "Mars 2025"
    @FXML private Button    btnMoisPrecedent;
    @FXML private Button    btnMoisSuivant;
    @FXML private Button    btnRetour;
    @FXML private VBox      detailJourContainer;    // panneau latéral droit
    @FXML private Label     lblDetailJour;          // titre du détail "Réservations du 12 mars"
    @FXML private VBox      listeDetailReservations;// liste des réservations du jour sélectionné
    @FXML private ComboBox<String> comboFiltreLocal;// filtre par local
    @FXML private Label     lblLegende;             // légende des couleurs

    // ============================================================
    //  Services & données
    // ============================================================

    private reservation_local_SERVICE  reservationService;
    private local_psychiatrie_SERVICE  localService;

    private List<reservation_local>    toutesReservations;
    private List<local_psychiatrie>    tousLocaux;

    // Référence au contrôleur parent UserHome pour la navigation inline
    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    // Mois affiché (navigation)
    private YearMonth moisAffiche;

    // Jour actuellement sélectionné
    private LocalDate jourSelectionne;

    // Couleurs selon statut — palette professionnelle verte/orange/rouge/violet
    private static final String COULEUR_CONFIRMEE  = "#1B4332"; // vert foncé
    private static final String COULEUR_EN_ATTENTE = "#F57C00"; // orange professionnel
    private static final String COULEUR_ANNULEE    = "#C62828"; // rouge foncé
    private static final String COULEUR_TERMINEE   = "#6A1B9A"; // violet foncé

    // ============================================================
    //  Initialisation
    // ============================================================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        reservationService = new reservation_local_SERVICE();
        localService       = new local_psychiatrie_SERVICE();
        moisAffiche        = YearMonth.now();

        chargerDonnees();
        initialiserFiltreLocal();
        dessinerCalendrier();
        afficherLegende();

        applyFadeIn();
    }

    private void chargerDonnees() {
        try {
            toutesReservations = reservationService.afficherList();
            tousLocaux         = localService.afficherList();
        } catch (SQLException e) {
            toutesReservations = new ArrayList<>();
            tousLocaux         = new ArrayList<>();
        }
    }

    private void initialiserFiltreLocal() {
        List<String> items = new ArrayList<>();
        items.add("Tous les locaux");
        tousLocaux.forEach(l -> items.add(l.getNom_local()));
        comboFiltreLocal.getItems().setAll(items);
        comboFiltreLocal.setValue("Tous les locaux");
        comboFiltreLocal.valueProperty().addListener((obs, o, n) -> dessinerCalendrier());
    }

    // ============================================================
    //  Dessin du calendrier
    // ============================================================

    /**
     * Dessine la grille mensuelle.
     */
    private void dessinerCalendrier() {
        calendrierGrid.getChildren().clear();
        calendrierGrid.setHgap(4);
        calendrierGrid.setVgap(4);

        // Titre du mois
        String moisNom = moisAffiche.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblMoisAnnee.setText(capitalize(moisNom) + " " + moisAffiche.getYear());

        // ─── En-têtes des jours ───
        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(jours[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; " +
                    "-fx-text-fill: #1B4332; -fx-alignment: center;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            calendrierGrid.add(lbl, i, 0);
        }

        // ─── Jours du mois ───
        LocalDate premierJour   = moisAffiche.atDay(1);
        int       decalage      = premierJour.getDayOfWeek().getValue() - 1; // Lundi=0
        int       nbJours       = moisAffiche.lengthOfMonth();
        LocalDate aujourd_hui   = LocalDate.now();

        // Filtrage selon le local sélectionné
        List<reservation_local> reservationsFiltrees = filtrerParLocal(toutesReservations);

        // Map jour → liste de réservations
        Map<LocalDate, List<reservation_local>> reservationsParJour = new HashMap<>();
        for (reservation_local r : reservationsFiltrees) {
            if (r.getDate_reservation() == null) continue;
            java.util.Date rawDate = r.getDate_reservation();
            LocalDate date = (rawDate instanceof java.sql.Date)
                    ? ((java.sql.Date) rawDate).toLocalDate()
                    : rawDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            reservationsParJour.computeIfAbsent(date, k -> new ArrayList<>()).add(r);
        }

        int col = decalage;
        int row = 1;

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisAffiche.atDay(jour);
            List<reservation_local> reservationsJour =
                    reservationsParJour.getOrDefault(date, new ArrayList<>());

            VBox cellule = creerCelluleJour(date, reservationsJour, aujourd_hui);
            calendrierGrid.add(cellule, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }

        // Rafraîchir le détail si un jour est sélectionné
        if (jourSelectionne != null) {
            List<reservation_local> res = reservationsParJour.getOrDefault(jourSelectionne, new ArrayList<>());
            afficherDetailJour(jourSelectionne, res);
        }
    }

    /**
     * Crée une cellule de jour avec ses indicateurs de réservations.
     */
    private VBox creerCelluleJour(LocalDate date,
                                  List<reservation_local> reservations,
                                  LocalDate aujourd_hui) {
        VBox cellule = new VBox(3);
        cellule.setAlignment(Pos.TOP_CENTER);
        cellule.setPrefSize(90, 80);
        cellule.setMinSize(90, 80);

        // Style de base
        boolean estAujourdHui  = date.equals(aujourd_hui);
        boolean estSelectionne = date.equals(jourSelectionne);
        boolean estPasse       = date.isBefore(aujourd_hui);

        String bg;
        if (estSelectionne)    bg = "#D8F3DC";        // vert clair sélection
        else if (estAujourdHui) bg = "#FFF8E1";       // crème aujourd'hui
        else if (estPasse)     bg = "#FAFAFA";
        else                   bg = "white";

        String border = estSelectionne ? "#1B4332" : (estAujourdHui ? "#F57C00" : "#E0E0E0");
        int    bw     = estSelectionne ? 2 : 1;

        cellule.setStyle(
                "-fx-background-color: " + bg + "; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: " + border + "; " +
                        "-fx-border-width: " + bw + "px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-padding: 6px; " +
                        "-fx-cursor: hand;"
        );

        // Numéro du jour
        Label lblJour = new Label(String.valueOf(date.getDayOfMonth()));
        lblJour.setStyle(
                "-fx-font-size: " + (estAujourdHui ? "15px" : "13px") + "; " +
                        "-fx-font-weight: " + (estAujourdHui ? "bold" : "normal") + "; " +
                        "-fx-text-fill: " + (estPasse ? "#9CA3AF" : "#1F2937") + ";"
        );
        cellule.getChildren().add(lblJour);

        // Indicateurs de réservations (max 3 puces)
        if (!reservations.isEmpty()) {
            HBox pucesBox = new HBox(3);
            pucesBox.setAlignment(Pos.CENTER);

            int max = Math.min(reservations.size(), 3);
            for (int i = 0; i < max; i++) {
                String couleur = getCouleurStatut(reservations.get(i).getStatus_reservation());
                Label puce = new Label("●");
                puce.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 10px;");
                pucesBox.getChildren().add(puce);
            }

            if (reservations.size() > 3) {
                Label plus = new Label("+" + (reservations.size() - 3));
                plus.setStyle("-fx-font-size: 9px; -fx-text-fill: #F57C00; -fx-font-weight: bold;");
                pucesBox.getChildren().add(plus);
            }

            cellule.getChildren().add(pucesBox);

            // Nombre total
            Label lblNb = new Label(reservations.size() + " rés.");
            lblNb.setStyle("-fx-font-size: 9px; -fx-text-fill: #6B7280;");
            cellule.getChildren().add(lblNb);
        }

        // Hover
        cellule.setOnMouseEntered(e -> cellule.setStyle(cellule.getStyle()
                .replace("-fx-background-color: " + bg, "-fx-background-color: #D0EDD9")));
        cellule.setOnMouseExited(e -> {
            if (!date.equals(jourSelectionne))
                cellule.setStyle(cellule.getStyle()
                        .replace("-fx-background-color: #D0EDD9", "-fx-background-color: " + bg));
        });

        // Clic → sélection du jour
        final List<reservation_local> resJour = reservations;
        cellule.setOnMouseClicked(e -> {
            jourSelectionne = date;
            afficherDetailJour(date, resJour);
            dessinerCalendrier(); // redessine pour mettre à jour la sélection
        });

        return cellule;
    }

    // ============================================================
    //  Panneau de détail du jour
    // ============================================================

    private void afficherDetailJour(LocalDate date, List<reservation_local> reservations) {
        listeDetailReservations.getChildren().clear();

        String dateStr = date.getDayOfMonth() + " " +
                capitalize(date.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH)) +
                " " + date.getYear();
        lblDetailJour.setText("📅 " + dateStr);

        if (reservations.isEmpty()) {
            Label lblVide = new Label("Aucune réservation ce jour.");
            lblVide.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 10px;");
            listeDetailReservations.getChildren().add(lblVide);

            // Bouton ajouter si le jour est futur
            if (!date.isBefore(LocalDate.now())) {
                Button btnAjouter = new Button("➕ Réserver ce créneau");
                btnAjouter.setStyle(
                        "-fx-background-color: #1B4332; -fx-text-fill: white; " +
                                "-fx-padding: 9px 18px; -fx-background-radius: 8px; " +
                                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 2);");
                btnAjouter.setOnAction(e -> ouvrirAjoutReservation(date));
                listeDetailReservations.getChildren().add(btnAjouter);
            }
            return;
        }

        // Afficher chaque réservation du jour
        for (reservation_local res : reservations) {
            VBox item = creerItemDetailReservation(res);
            listeDetailReservations.getChildren().add(item);

            FadeTransition ft = new FadeTransition(Duration.millis(250), item);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }

        // Résumé du jour
        Label lblResume = new Label(
                "Total : " + reservations.size() + " réservation(s) | " +
                        reservations.stream()
                                .filter(r -> r.getStatus_reservation() == StatutReservation.CONFIRMEE)
                                .count() + " confirmée(s)"
        );
        lblResume.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280; -fx-padding: 8px 0 0 0;");
        listeDetailReservations.getChildren().add(lblResume);
    }

    private VBox creerItemDetailReservation(reservation_local res) {
        VBox item = new VBox(5);
        String couleur = getCouleurStatut(res.getStatus_reservation());

        item.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: " + couleur + "; " +
                        "-fx-border-width: 0 0 0 4px; " +
                        "-fx-padding: 10px 10px 10px 14px; " +
                        "-fx-background-radius: 0 8px 8px 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 1);"
        );

        // Nom du local
        String nomLocal = getNomLocal(res.getId_local());
        Label lblLocal = new Label("🏥 " + nomLocal);
        lblLocal.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1F2937;");

        // Horaires
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String heures = "⏰ ";
        if (res.getHeure_debut_reservation() != null) heures += sdf.format(res.getHeure_debut_reservation());
        if (res.getHeure_fin_reservation()   != null) heures += " → " + sdf.format(res.getHeure_fin_reservation());

        Label lblHeures = new Label(heures);
        lblHeures.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");

        // Client
        Label lblClient = new Label("👤 " + res.getNom_cl() + " " + res.getPrenom_cl());
        lblClient.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

        // Statut badge
        Label badgeStatut = new Label(res.getStatus_reservation().getLibelle());
        badgeStatut.setStyle(
                "-fx-background-color: " + getCouleurBackground(res.getStatus_reservation()) + "; " +
                        "-fx-text-fill: " + getCouleurTexte(res.getStatus_reservation()) + "; " +
                        "-fx-padding: 3px 10px; -fx-background-radius: 10px; -fx-font-size: 11px; -fx-font-weight: bold;"
        );

        item.getChildren().addAll(lblLocal, lblHeures, lblClient, badgeStatut);
        return item;
    }

    // ============================================================
    //  Navigation mois
    // ============================================================

    @FXML
    private void handleMoisPrecedent() {
        moisAffiche = moisAffiche.minusMonths(1);
        jourSelectionne = null;
        listeDetailReservations.getChildren().clear();
        lblDetailJour.setText("Sélectionnez un jour");
        dessinerCalendrier();
    }

    @FXML
    private void handleMoisSuivant() {
        moisAffiche = moisAffiche.plusMonths(1);
        jourSelectionne = null;
        listeDetailReservations.getChildren().clear();
        lblDetailJour.setText("Sélectionnez un jour");
        dessinerCalendrier();
    }

    @FXML
    private void handleAujourdHui() {
        moisAffiche = YearMonth.now();
        jourSelectionne = LocalDate.now();
        dessinerCalendrier();
    }

    // ============================================================
    //  Légende
    // ============================================================

    private void afficherLegende() {
        if (lblLegende == null) return;
        lblLegende.setText(
                "● Confirmée   ● En attente   ● Annulée   ● Terminée"
        );
        lblLegende.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
    }

    // ============================================================
    //  Ouverture formulaire réservation
    // ============================================================

    private void ouvrirAjoutReservation(LocalDate datePre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterReservation.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Nouvelle réservation");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setMinWidth(800);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);

            AjouterReservationController ctrl = loader.getController();
            ctrl.setDatePreselectionne(datePre);

            stage.showAndWait();
            chargerDonnees();
            dessinerCalendrier();

        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Erreur : " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleRetour() {
        if (parentHomeController != null) {
            parentHomeController.loadFrontOfficeAccueil();
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FrontOfficeAccueil.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRetour.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Navigation impossible : " + e.getMessage()).showAndWait();
        }
    }

    // ============================================================
    //  Utilitaires
    // ============================================================

    private List<reservation_local> filtrerParLocal(List<reservation_local> liste) {
        String choix = comboFiltreLocal != null ? comboFiltreLocal.getValue() : "Tous les locaux";
        if (choix == null || choix.equals("Tous les locaux")) return liste;

        return liste.stream()
                .filter(r -> getNomLocal(r.getId_local()).equals(choix))
                .collect(Collectors.toList());
    }

    private String getNomLocal(int id) {
        return tousLocaux.stream()
                .filter(l -> l.getId_local() == id)
                .map(local_psychiatrie::getNom_local)
                .findFirst().orElse("Local #" + id);
    }

    private String getCouleurStatut(StatutReservation statut) {
        switch (statut) {
            case CONFIRMEE:  return COULEUR_CONFIRMEE;
            case EN_ATTENTE: return COULEUR_EN_ATTENTE;
            case ANNULEE:    return COULEUR_ANNULEE;
            case TERMINEE:   return COULEUR_TERMINEE;
            default:         return "#94A3B8";
        }
    }

    private String getCouleurBackground(StatutReservation statut) {
        switch (statut) {
            case CONFIRMEE:  return "#D8F3DC"; // vert très clair
            case EN_ATTENTE: return "#FFF3E0"; // orange très clair
            case ANNULEE:    return "#FFEBEE"; // rouge très clair
            case TERMINEE:   return "#F3E5F5"; // violet très clair
            default:         return "#F5F5F5";
        }
    }

    private String getCouleurTexte(StatutReservation statut) {
        switch (statut) {
            case CONFIRMEE:  return "#1B4332"; // vert foncé
            case EN_ATTENTE: return "#E65100"; // orange foncé
            case ANNULEE:    return "#B71C1C"; // rouge foncé
            case TERMINEE:   return "#4A148C"; // violet foncé
            default:         return "#424242";
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void applyFadeIn() {
        if (calendrierGrid != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(700), calendrierGrid);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
        }
    }
}