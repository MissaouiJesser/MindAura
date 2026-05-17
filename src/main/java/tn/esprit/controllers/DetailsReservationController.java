package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.entities.reservation_local;
import tn.esprit.services.local_psychiatrie_SERVICE;

import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

public class DetailsReservationController implements Initializable {

    @FXML private Label lblId;
    @FXML private Label lblNomClient;
    @FXML private Label lblPrenomClient;
    @FXML private Label lblIdLocal;
    @FXML private Label lblIdUtilisateur;
    @FXML private Label lblDateReservation;
    @FXML private Label lblHeureDebut;
    @FXML private Label lblHeureFin;
    @FXML private Label lblDuree;
    @FXML private Label lblStatut;
    @FXML private Label lblMotif;
    @FXML private Label lblPrix;
    @FXML private Label lblNomLocal;
    @FXML private Button btnFermer;

    private reservation_local currentReservation;
    private local_psychiatrie_SERVICE localService;

    // ── Référence au contrôleur parent pour revenir à la liste ──────────────
    private AfficherReservationController parentController;

    public void setParentController(AfficherReservationController parentController) {
        this.parentController = parentController;
    }

    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        localService = new local_psychiatrie_SERVICE();
        applyCSSStyles();
        applyFadeInAnimation();
    }

    private void applyCSSStyles() {
        if (btnFermer != null) {
            btnFermer.getStyleClass().add("btn-annuler");
        }
    }

    public void setReservation(reservation_local reservation) {
        this.currentReservation = reservation;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

        lblId.setText(String.valueOf(reservation.getId_reservation()));
        lblNomClient.setText(reservation.getNom_cl());
        lblPrenomClient.setText(reservation.getPrenom_cl());
        lblIdLocal.setText(String.valueOf(reservation.getId_local()));
        lblIdUtilisateur.setText(String.valueOf(reservation.getId_utilisateur()));
        lblDateReservation.setText(dateFormat.format(reservation.getDate_reservation()));
        lblHeureDebut.setText(timeFormat.format(reservation.getHeure_debut_reservation()));
        lblHeureFin.setText(timeFormat.format(reservation.getHeure_fin_reservation()));

        long dureeMs = reservation.getHeure_fin_reservation().getTime() -
                reservation.getHeure_debut_reservation().getTime();
        long dureeMinutes = dureeMs / (60 * 1000);
        long heures = dureeMinutes / 60;
        long minutes = dureeMinutes % 60;
        lblDuree.setText(heures + "h " + minutes + "min");

        lblMotif.setText(reservation.getMotif_reservation().getLibelle());
        lblPrix.setText(reservation.getPrix_reservation() + " DT");

        setupStatutBadge(reservation);
        chargerEtAfficherNomLocal(reservation.getId_local());
    }

    private void chargerEtAfficherNomLocal(int idLocal) {
        try {
            local_psychiatrie local = localService.afficherList().stream()
                    .filter(l -> l.getId_local() == idLocal)
                    .findFirst()
                    .orElse(null);

            if (local != null && lblNomLocal != null) {
                lblNomLocal.setText("🏥 " + local.getNom_local());
                lblNomLocal.setStyle(
                        "-fx-font-size: 24px; " +
                                "-fx-font-weight: 900; " +
                                "-fx-text-fill: linear-gradient(to right, #1B4332, #2D6A4F); " +
                                "-fx-background-color: linear-gradient(to bottom right, #D8F3DC, #B7E4C7); " +
                                "-fx-background-radius: 15px; " +
                                "-fx-padding: 18px 30px; " +
                                "-fx-border-color: #2D6A4F; " +
                                "-fx-border-width: 3px; " +
                                "-fx-border-radius: 15px; " +
                                "-fx-effect: dropshadow(gaussian, rgba(45, 106, 79, 0.4), 12, 0.5, 0, 4); " +
                                "-fx-alignment: center; " +
                                "-fx-min-height: 70px;"
                );
                FadeTransition fade = new FadeTransition(Duration.millis(800), lblNomLocal);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                fade.play();
            } else if (lblNomLocal != null) {
                lblNomLocal.setText("🏥 Local #" + idLocal);
                lblNomLocal.setStyle(
                        "-fx-font-size: 20px; " +
                                "-fx-font-weight: 700; " +
                                "-fx-text-fill: #6B7280; " +
                                "-fx-background-color: #F3F4F6; " +
                                "-fx-background-radius: 12px; " +
                                "-fx-padding: 15px 25px; " +
                                "-fx-border-color: #9CA3AF; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 12px; " +
                                "-fx-alignment: center;"
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
            if (lblNomLocal != null) {
                lblNomLocal.setText("⚠️ Erreur de chargement");
                lblNomLocal.setStyle(
                        "-fx-font-size: 18px; " +
                                "-fx-font-weight: 600; " +
                                "-fx-text-fill: #7B5EA7; " +
                                "-fx-background-color: #F3E8FF; " +
                                "-fx-background-radius: 10px; " +
                                "-fx-padding: 12px 20px; " +
                                "-fx-border-color: #9B7DC4; " +
                                "-fx-border-width: 2px; " +
                                "-fx-border-radius: 10px; " +
                                "-fx-alignment: center;"
                );
            }
        }
    }

    private void setupStatutBadge(reservation_local reservation) {
        lblStatut.getStyleClass().clear();
        lblStatut.getStyleClass().add("badge");

        switch (reservation.getStatus_reservation()) {
            case CONFIRMEE:
                lblStatut.getStyleClass().add("badge-disponible");
                lblStatut.setText("✓ " + reservation.getStatus_reservation().getLibelle());
                break;
            case EN_ATTENTE:
                lblStatut.setStyle(
                        "-fx-background-color: #FFF4E6; -fx-text-fill: #7B5EA7; " +
                                "-fx-padding: 6px 16px; -fx-background-radius: 20px; -fx-font-weight: 700;"
                );
                lblStatut.setText("⏳ " + reservation.getStatus_reservation().getLibelle());
                break;
            case ANNULEE:
                lblStatut.getStyleClass().add("badge-non-disponible");
                lblStatut.setText("✗ " + reservation.getStatus_reservation().getLibelle());
                break;
            case TERMINEE:
                lblStatut.setStyle(
                        "-fx-background-color: #E5E7EB; -fx-text-fill: #6B7280; " +
                                "-fx-padding: 6px 16px; -fx-background-radius: 20px; -fx-font-weight: 700;"
                );
                lblStatut.setText("✓ " + reservation.getStatus_reservation().getLibelle());
                break;
        }
    }

    /**
     * ✅ MODIFIÉ : Au lieu de fermer un Stage, on revient à la liste via le parent.
     */
    @FXML
    private void handleFermer() {
        if (parentController != null) {
            parentController.showListView();
        }
    }

    private void applyFadeInAnimation() {
        if (lblId != null && lblId.getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(600), lblId.getParent());
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
        }
    }
}