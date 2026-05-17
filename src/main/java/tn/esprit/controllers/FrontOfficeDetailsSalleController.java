package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import tn.esprit.entities.reservation_local;
import tn.esprit.entities.salle;
import tn.esprit.entities.utilisateurs;
import tn.esprit.enums.MotifReservation;
import tn.esprit.enums.StatutReservation;
import tn.esprit.services.EmailService;               // même package que le local
import tn.esprit.services.PriceCalculatorService;
import tn.esprit.services.ReservationValidationService;
import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.utils.ImageManager;
import tn.esprit.utils.SessionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;

public class FrontOfficeDetailsSalleController implements Initializable {

    // ── Détails ───────────────────────────────────────────────────────────────
    @FXML private ImageView imgSalle;
    @FXML private Label     lblImgPlaceholder;
    @FXML private Label     lblBreadcrumb;
    @FXML private Label     lblNom;
    @FXML private Label     lblType;
    @FXML private Label     lblStatut;
    @FXML private Label     lblCapacite;
    @FXML private Label     lblEtage;
    @FXML private Label     lblLocalParent;
    @FXML private Label     lblEquipements;
    @FXML private Label     lblDisponibilite;

    // ── Formulaire ────────────────────────────────────────────────────────────
    @FXML private TextField              txtNomCl;
    @FXML private TextField              txtPrenomCl;
    @FXML private DatePicker             dateReservation;
    @FXML private Spinner<Integer>       spinnerHeureDebut;
    @FXML private Spinner<Integer>       spinnerMinuteDebut;
    @FXML private Spinner<Integer>       spinnerHeureFin;
    @FXML private Spinner<Integer>       spinnerMinuteFin;
    @FXML private ComboBox<MotifReservation> comboMotif;
    @FXML private Label                  lblNomSalle;
    @FXML private Label                  lblPrixCalcule;
    @FXML private Label                  lblMessage;
    @FXML private Button                 btnReserver;

    // ── Références ────────────────────────────────────────────────────────────
    private salle                          salleSelectionnee;
    private String                         nomLocalParent;
    private FrontOfficeSallesController    sallesController;
    private FrontOfficeAccueilController   parentController;

    private final reservation_local_SERVICE   service           = new reservation_local_SERVICE();
    private final ReservationValidationService validationService = new ReservationValidationService();

    // ── Init ──────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initializeFormControls();
        setupPriceCalculation();
        applyFadeInAnimation();
    }

    private void initializeFormControls() {
        comboMotif.setItems(FXCollections.observableArrayList(MotifReservation.values()));
        comboMotif.setValue(MotifReservation.CONSULTATION_INDIVIDUELLE);

        dateReservation.setValue(LocalDate.now().plusDays(1));
        dateReservation.setDayCellFactory(picker -> new DateCell() {
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isBefore(LocalDate.now().plusDays(1)));
            }
        });

        spinnerHeureDebut .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 9));
        spinnerMinuteDebut.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));
        spinnerHeureFin   .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 20, 11));
        spinnerMinuteFin  .setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));

        spinnerHeureDebut .setEditable(true);
        spinnerMinuteDebut.setEditable(true);
        spinnerHeureFin   .setEditable(true);
        spinnerMinuteFin  .setEditable(true);
    }

    private void setupPriceCalculation() {
        spinnerHeureDebut .valueProperty().addListener((obs, o, n) -> calculerPrix());
        spinnerMinuteDebut.valueProperty().addListener((obs, o, n) -> calculerPrix());
        spinnerHeureFin   .valueProperty().addListener((obs, o, n) -> calculerPrix());
        spinnerMinuteFin  .valueProperty().addListener((obs, o, n) -> calculerPrix());
        dateReservation.valueProperty().addListener((obs, o, n) -> calculerPrix());
    }

    // ── Injection depuis FrontOfficeSallesController ──────────────────────────
    public void setSalle(salle s, String nomLocal) {
        this.salleSelectionnee = s;
        this.nomLocalParent    = nomLocal;
        remplirDetails();
        prefillFromSession();
        calculerPrix();
    }

    public void setParentController(FrontOfficeAccueilController parent) {
        this.parentController = parent;
    }

    public void setSallesController(FrontOfficeSallesController sc) {
        this.sallesController = sc;
    }

    // ── Panneau gauche : détails ──────────────────────────────────────────────
    private void remplirDetails() {
        if (salleSelectionnee == null) return;

        set(lblBreadcrumb,  salleSelectionnee.getNom_salle());
        set(lblNom,         salleSelectionnee.getNom_salle());
        set(lblType,        salleSelectionnee.getType_salle());
        set(lblCapacite,    salleSelectionnee.getCapacite_salle());
        set(lblEtage,       salleSelectionnee.getEtage());
        set(lblLocalParent, nomLocalParent);
        set(lblEquipements, salleSelectionnee.getEquipements());

        if (lblNomSalle != null)
            lblNomSalle.setText(salleSelectionnee.getNom_salle() != null
                    ? salleSelectionnee.getNom_salle() : "—");

        // Image
        String imgPath = salleSelectionnee.getImage_url();
        if (imgPath != null && !imgPath.isBlank()) {
            Image img = ImageManager.loadImage(imgPath, 420, 260, true);
            if (img != null && !img.isError()) {
                imgSalle.setImage(img);
                imgSalle.setVisible(true);
                if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(false);
            } else {
                imgSalle.setVisible(false);
                if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(true);
            }
        } else {
            imgSalle.setVisible(false);
            if (lblImgPlaceholder != null) lblImgPlaceholder.setVisible(true);
        }

        // Statut coloré
        if (lblStatut != null) {
            String st = salleSelectionnee.getStatut_salle();
            lblStatut.setText(st != null ? st : "—");
            if ("Active".equalsIgnoreCase(st))
                lblStatut.setStyle("-fx-text-fill:#059669;-fx-font-size:13px;-fx-font-weight:700;");
            else if ("En maintenance".equalsIgnoreCase(st))
                lblStatut.setStyle("-fx-text-fill:#D97706;-fx-font-size:13px;-fx-font-weight:700;");
            else
                lblStatut.setStyle("-fx-text-fill:#DC2626;-fx-font-size:13px;-fx-font-weight:700;");
        }

        // Disponibilité + désactivation du bouton si non disponible
        if (lblDisponibilite != null) {
            String dispo = salleSelectionnee.getDisponibilite_salle();
            boolean ok   = "Disponible".equalsIgnoreCase(dispo);
            lblDisponibilite.getStyleClass().clear();
            lblDisponibilite.getStyleClass().add("badge");
            if (ok) {
                lblDisponibilite.getStyleClass().add("badge-disponible");
                lblDisponibilite.setText("✓ Disponible");
            } else {
                lblDisponibilite.getStyleClass().add("badge-non-disponible");
                lblDisponibilite.setText("✗ " + (dispo != null ? dispo : "Non disponible"));
                if (btnReserver != null) {
                    btnReserver.setDisable(true);
                    btnReserver.setText("❌ Salle non disponible");
                }
                showMessage("⚠️ Cette salle n'est pas disponible actuellement", "warning");
            }
        }
    }

    // ── Pré-remplissage nom/prénom depuis la session ──────────────────────────
    private void prefillFromSession() {
        if (SessionManager.isLoggedIn()) {
            utilisateurs user = SessionManager.getCurrentUser();
            txtNomCl.setText(user.getNom_utilisateur());
            txtPrenomCl.setText(user.getPrenom_utilisateur());
            txtNomCl.setEditable(false);
            txtPrenomCl.setEditable(false);
            String styleReadOnly =
                    "-fx-background-color: #F1F5F9; -fx-text-fill: #4B5563; " +
                            "-fx-border-color: #E2E8F0; -fx-border-radius: 8px; -fx-background-radius: 8px;";
            txtNomCl.setStyle(styleReadOnly);
            txtPrenomCl.setStyle(styleReadOnly);
        }
    }

    // ── Calcul automatique du prix ────────────────────────────────────────────
    private void calculerPrix() {
        try {
            if (dateReservation.getValue() == null) return;
            Date heureDebut = createDateTime(dateReservation.getValue(),
                    spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
            Date heureFin   = createDateTime(dateReservation.getValue(),
                    spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
            int prix = PriceCalculatorService.calculerPrix(heureDebut, heureFin);
            if (lblPrixCalcule != null) lblPrixCalcule.setText(prix + " DT");
        } catch (Exception e) {
            if (lblPrixCalcule != null) lblPrixCalcule.setText("0 DT");
        }
    }

    // ── Confirmer réservation ─────────────────────────────────────────────────
    @FXML
    private void handleReserver() {
        if (!validateForm()) return;

        try {
            Date heureDebut = createDateTime(dateReservation.getValue(),
                    spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
            Date heureFin   = createDateTime(dateReservation.getValue(),
                    spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());
            Date dateRes    = Date.from(dateReservation.getValue()
                    .atStartOfDay(ZoneId.systemDefault()).toInstant());

            // Vérification conflit
            if (validationService.hasConflict(salleSelectionnee.getId_local(),
                    dateRes, heureDebut, heureFin, null)) {
                showMessage(validationService.getConflictMessage(
                        salleSelectionnee.getId_local(), heureDebut, heureFin), "error");
                return;
            }

            int prix = PriceCalculatorService.calculerPrix(heureDebut, heureFin);

            reservation_local r = new reservation_local();
            r.setId_local(salleSelectionnee.getId_local());
            r.setSalle_id(salleSelectionnee.getId_salle());
            r.setDate_reservation(dateRes);
            r.setHeure_debut_reservation(heureDebut);
            r.setHeure_fin_reservation(heureFin);
            r.setStatus_reservation(StatutReservation.CONFIRMEE);
            r.setMotif_reservation(comboMotif.getValue());
            r.setPrix_reservation(prix);
            r.setNom_cl(txtNomCl.getText().trim());
            r.setPrenom_cl(txtPrenomCl.getText().trim());

            // ID utilisateur depuis la session
            if (SessionManager.isLoggedIn()) {
                try {
                    r.setId_utilisateur(
                            Integer.parseInt(SessionManager.getCurrentUser().getId_utilisateur()));
                } catch (NumberFormatException ex) {
                    r.setId_utilisateur(1);
                }
            } else {
                r.setId_utilisateur(1);
            }

            service.add(r);

            // ── ✉️  Envoi de l'e-mail — même logique que FrontOfficeDetailsLocalController
            if (SessionManager.isLoggedIn()) {
                utilisateurs user     = SessionManager.getCurrentUser();
                String emailUser      = user.getEmail_utilisateur();
                String nomComplet     = user.getPrenom_utilisateur() + " " + user.getNom_utilisateur();

                // dateSeance = date + heure de début (identique au local)
                LocalDateTime dateSeance = dateReservation.getValue()
                        .atTime(spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());

                // lieu = "Nom salle – Étage X – Local parent" (équivalent de l'adresse du local)
                String etage = salleSelectionnee.getEtage() != null
                        ? "Étage " + salleSelectionnee.getEtage() : "";
                String lieu  = salleSelectionnee.getNom_salle()
                        + (etage.isEmpty() ? "" : " – " + etage)
                        + (nomLocalParent != null ? " – " + nomLocalParent : "");

                new Thread(() -> {
                    EmailService.EmailResult result = EmailService.envoyerConfirmation(
                            emailUser,
                            nomComplet,
                            salleSelectionnee.getNom_salle(),
                            dateSeance,
                            comboMotif.getValue().getLibelle(),
                            lieu
                    );
                    System.out.println("[Email] " + result);
                }).start();
            }
            // ─────────────────────────────────────────────────────────────────

            showMessage("✅ Réservation confirmée ! Un email de confirmation vous a été envoyé.", "success");

            if (btnReserver != null) {
                btnReserver.setDisable(true);
                btnReserver.setText("✅ Réservation enregistrée");
            }
            if (parentController != null) parentController.refreshDisplay();

            // Redirection vers Mes Réservations après 2 s
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::redirectionnerVersMesReservations);
                } catch (InterruptedException ignored) {}
            }).start();

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("❌ Erreur lors de la réservation : " + e.getMessage(), "error");
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────
    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNomCl.getText().trim().isEmpty())
            errors.append("- Le nom est obligatoire\n");
        if (txtPrenomCl.getText().trim().isEmpty())
            errors.append("- Le prénom est obligatoire\n");

        if (dateReservation.getValue() == null) {
            errors.append("- La date est obligatoire\n");
            showMessage(errors.toString(), "error");
            return false;
        }

        Date heureDebut = createDateTime(dateReservation.getValue(),
                spinnerHeureDebut.getValue(), spinnerMinuteDebut.getValue());
        Date heureFin   = createDateTime(dateReservation.getValue(),
                spinnerHeureFin.getValue(), spinnerMinuteFin.getValue());

        if (!heureFin.after(heureDebut))
            errors.append("- L'heure de fin doit être après l'heure de début\n");

        if (errors.length() > 0) {
            showMessage(errors.toString(), "error");
            return false;
        }
        return true;
    }

    // ── Redirection vers Mes Réservations ─────────────────────────────────────
    private void redirectionnerVersMesReservations() {
        try {
            if (parentController != null && parentController.getParentHomeController() != null) {
                parentController.getParentHomeController().loadMesReservations();
                return;
            }
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/FrontOfficeMesReservations.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) btnReserver.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL css = getClass().getResource("/style.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setTitle("MindAura – Mes Réservations");
            stage.setWidth(1400);
            stage.setHeight(850);
            stage.centerOnScreen();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Retour à la liste des salles ──────────────────────────────────────────
    @FXML
    private void handleRetour() {
        if (parentController != null) {
            parentController.retourVueSalles();
        } else if (sallesController != null) {
            sallesController.retourListe();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private Date createDateTime(LocalDate date, int heure, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR,         date.getYear());
        cal.set(Calendar.MONTH,        date.getMonthValue() - 1);
        cal.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        cal.set(Calendar.HOUR_OF_DAY,  heure);
        cal.set(Calendar.MINUTE,       minute);
        cal.set(Calendar.SECOND,       0);
        cal.set(Calendar.MILLISECOND,  0);
        return cal.getTime();
    }

    private void showMessage(String msg, String type) {
        if (lblMessage == null) return;
        lblMessage.setText(msg);
        if ("success".equals(type)) {
            lblMessage.setStyle(
                    "-fx-text-fill:#1B4332; -fx-font-weight:bold; -fx-background-color:#D8F3DC;" +
                            "-fx-padding:12px 20px; -fx-background-radius:10px;" +
                            "-fx-border-color:#2D6A4F; -fx-border-width:1px; -fx-border-radius:10px;");
        } else if ("warning".equals(type)) {
            lblMessage.setStyle(
                    "-fx-text-fill:#7B5EA7; -fx-font-weight:bold; -fx-background-color:#FFF4E6;" +
                            "-fx-padding:12px 20px; -fx-background-radius:10px;" +
                            "-fx-border-color:#B794F4; -fx-border-width:1px; -fx-border-radius:10px;");
        } else {
            lblMessage.setStyle(
                    "-fx-text-fill:#7B5EA7; -fx-font-weight:bold; -fx-background-color:#F3E8FF;" +
                            "-fx-padding:12px 20px; -fx-background-radius:10px;" +
                            "-fx-border-color:#9B7DC4; -fx-border-width:1px; -fx-border-radius:10px;");
        }
        FadeTransition fade = new FadeTransition(Duration.millis(300), lblMessage);
        fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
    }

    private void applyFadeInAnimation() {
        if (lblNom != null && lblNom.getParent() != null &&
                lblNom.getParent().getParent() != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(600),
                    lblNom.getParent().getParent());
            fade.setFromValue(0.0); fade.setToValue(1.0); fade.play();
        }
    }

    private void set(Label lbl, String val) {
        if (lbl != null) lbl.setText(val != null && !val.isBlank() ? val : "—");
    }

    private void set(Label lbl, Integer val) {
        set(lbl, val != null ? val.toString() : null);
    }
}