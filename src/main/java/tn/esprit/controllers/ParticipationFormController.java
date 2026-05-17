package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;
import tn.esprit.services.EvenementService;
import tn.esprit.services.ParticipationService;
import tn.esprit.services.QRCodeService;
import tn.esprit.services.EmailServiceParticipation;
import tn.esprit.utils.SessionManager;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ParticipationFormController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private Label     eventNameLabel;
    @FXML private Label     eventDatesLabel;
    @FXML private Label     periodLabel;
    @FXML private Label     errorLabel;
    @FXML private Label     eventNameErrorLabel;
    @FXML private Label     eventNameOkLabel;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private TextField eventNameField;
    @FXML private TextArea  feedbackField;

    // ── State ─────────────────────────────────────────────────────────────────
    private Evenements currentEvent;
    private Runnable   onSuccessCallback;

    private final ParticipationService      participationService = new ParticipationService();
    private final EvenementService          evenementService     = new EvenementService();
    private final QRCodeService             qrCodeService        = new QRCodeService();
    private final EmailServiceParticipation emailService         = new EmailServiceParticipation();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String LOCKED_STYLE =
            "-fx-background-color: #F1F5F9; -fx-border-color: #CBD5E1;" +
                    "-fx-border-width: 1.5; -fx-border-radius: 9; -fx-background-radius: 9;" +
                    "-fx-font-size: 12.5px; -fx-text-fill: #64748B;" +
                    "-fx-padding: 8 12; -fx-pref-height: 36;";

    // ── API publique ──────────────────────────────────────────────────────────

    public void setOnSuccess(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    public void setEvent(Evenements ev) {
        this.currentEvent = ev;
        if (ev == null) return;

        if (eventNameLabel != null)
            eventNameLabel.setText(ev.getTitreEvenement());

        if (eventNameField != null) {
            eventNameField.setText(ev.getTitreEvenement());
            eventNameField.setEditable(false);
            eventNameField.setStyle(LOCKED_STYLE);
            showEventNameOk();
        }

        Date debut = ev.getDatedebutEvenemnt();
        Date fin   = ev.getDatefinEvenemnt();
        if (debut != null && fin != null) {
            LocalDate ldDebut = toLocalDate(debut);
            LocalDate ldFin   = toLocalDate(fin);
            if (eventDatesLabel != null)
                eventDatesLabel.setText("Du " + ldDebut.format(FMT) + " au " + ldFin.format(FMT));
        } else {
            if (eventDatesLabel != null) eventDatesLabel.setText("Dates non définies");
        }
        if (periodLabel != null) periodLabel.setText("");

        if (nomField != null) {
            nomField.setText(SessionManager.getNomComplet());
            nomField.setEditable(false);
            nomField.setStyle(LOCKED_STYLE);
        }
        if (emailField != null && SessionManager.getCurrentUser() != null) {
            emailField.setText(SessionManager.getCurrentUser().getEmail_utilisateur());
            emailField.setEditable(false);
            emailField.setStyle(LOCKED_STYLE);
        }
    }

    // ── Actions FXML ──────────────────────────────────────────────────────────

    @FXML
    private void saveParticipation() {
        hideErrors();

        String nom          = get(nomField);
        String email        = get(emailField);
        String telephone    = get(telephoneField);
        String nomEvenement = get(eventNameField);

        if (nom.isEmpty()) {
            showError("Le nom complet est obligatoire.");
            return;
        }
        if (!email.isEmpty() && !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("L'adresse e-mail n'est pas valide.");
            return;
        }
        if (nomEvenement.isEmpty()) {
            showEventNameError("Le nom de l'événement est obligatoire.");
            return;
        }

        Evenements evenementTrouve = findEventByName(nomEvenement);
        if (evenementTrouve == null) {
            showEventNameError("Aucun événement trouvé avec ce nom.");
            return;
        }

        Participation p = new Participation();
        p.setNom(nom);
        p.setEmail(email);
        p.setTelephone(telephone);
        p.setDateInscription(new java.sql.Timestamp(System.currentTimeMillis()));
        p.setStatut("confirmee");
        String qrFileName = "qr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + ".png";
        p.setCodeQr(qrFileName);
        p.setEvenementId(evenementTrouve.getId());

        try {
            participationService.add(p);
            genererEtSauvegarderQR(p, evenementTrouve, qrFileName);

            final Evenements   evFinal = evenementTrouve;
            final Participation pFinal = p;
            new Thread(() -> {
                try {
                    emailService.envoyerConfirmationParticipation(pFinal, evFinal);
                    System.out.println("[Email] Confirmation envoyée à : " + pFinal.getEmail());
                } catch (Exception ex) {
                    System.err.println("[Email] Erreur envoi : " + ex.getMessage());
                }
            }).start();

            if (onSuccessCallback != null) onSuccessCallback.run();

            // Fermer le formulaire
            Stage currentStage = (Stage) nomField.getScene().getWindow();
            currentStage.close();

            // Ouvrir la fenêtre QR sur le thread JavaFX
            Platform.runLater(() -> openQRCodeWindow(p, evenementTrouve));

        } catch (SQLException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        clearForm();
        if (nomField != null
                && nomField.getScene() != null
                && nomField.getScene().getWindow() instanceof Stage stage) {
            stage.close();
        }
    }

    // ── Ouverture fenêtre QR ──────────────────────────────────────────────────

    private void openQRCodeWindow(Participation p, Evenements ev) {
        try {
            URL fxmlUrl = resolveFxmlUrl();

            if (fxmlUrl == null) {
                System.err.println("[QR] *** QRCode.fxml introuvable ***");
                System.err.println("[QR] Fichiers .fxml détectés :");
                listFxmlFiles(new File("src/main/resources"), "");
                listFxmlFiles(new File("target/classes"),     "");
                return;
            }

            System.out.println("[QR] Chargement FXML → " + fxmlUrl);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();

            QRCodeController ctrl = loader.getController();
            ctrl.setTicketData(p, ev);

            Stage stage = new Stage();
            stage.setTitle("Votre Ticket QR");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("[QR] Erreur ouverture : " + ex.getMessage());
        }
    }

    /**
     * Trois stratégies pour localiser QRCode.fxml :
     *   1. getClass().getResource() avec slash (classpath absolu)
     *   2. ClassLoader sans slash
     *   3. Recherche récursive sur disque
     */
    private URL resolveFxmlUrl() {

        // ── Stratégie 1 : getClass().getResource ──
        String[] withSlash = {
                "/tn/esprit/fxml/QRCode.fxml",
                "/tn/esprit/fxml/qrcode.fxml",
                "/tn/esprit/fxml/Qrcode.fxml",
                "/fxml/QRCode.fxml",
                "/fxml/qrcode.fxml",
                "/tn/esprit/views/QRCode.fxml",
                "/views/QRCode.fxml",
                "/QRCode.fxml"
        };
        for (String path : withSlash) {
            URL url = getClass().getResource(path);
            if (url != null) {
                System.out.println("[QR] Stratégie 1 → " + path);
                return url;
            }
        }

        // ── Stratégie 2 : ClassLoader ──
        String[] withoutSlash = {
                "tn/esprit/fxml/QRCode.fxml",
                "tn/esprit/fxml/qrcode.fxml",
                "fxml/QRCode.fxml",
                "tn/esprit/views/QRCode.fxml",
                "views/QRCode.fxml",
                "QRCode.fxml"
        };
        for (String path : withoutSlash) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url == null) url = getClass().getClassLoader().getResource(path);
            if (url != null) {
                System.out.println("[QR] Stratégie 2 → " + path);
                return url;
            }
        }

        // ── Stratégie 3 : recherche sur disque ──
        for (String root : new String[]{"src/main/resources", "target/classes", "out/production"}) {
            File found = findFileRecursive(new File(root), "QRCode.fxml");
            if (found != null) {
                try {
                    System.out.println("[QR] Stratégie 3 → " + found.getAbsolutePath());
                    return found.toURI().toURL();
                } catch (Exception ignored) {}
            }
        }

        return null;
    }

    private File findFileRecursive(File dir, String name) {
        if (!dir.exists() || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findFileRecursive(f, name);
                if (found != null) return found;
            } else if (f.getName().equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }

    private void listFxmlFiles(File dir, String indent) {
        if (!dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) listFxmlFiles(f, indent + "  ");
            else if (f.getName().toLowerCase().endsWith(".fxml"))
                System.err.println(indent + "  [FXML] " + f.getAbsolutePath());
        }
    }

    // ── Génération QR sur disque ──────────────────────────────────────────────

    private void genererEtSauvegarderQR(Participation p, Evenements ev, String fileName) {
        try {
            BufferedImage ticket = qrCodeService.genererTicket(p, ev);
            String outPath = "src/main/resources/qr_tickets/" + fileName;
            qrCodeService.sauvegarderTicket(ticket, outPath);
            System.out.println("[QR] Ticket sauvegardé → " + outPath);
        } catch (Exception ex) {
            System.err.println("[QR] Erreur génération : " + ex.getMessage());
        }
    }

    // ── Helpers métier ────────────────────────────────────────────────────────

    private Evenements findEventByName(String titre) {
        try {
            List<Evenements> tous = evenementService.afficherList();
            return tous.stream()
                    .filter(e -> e.getTitreEvenement() != null
                            && e.getTitreEvenement().trim().equalsIgnoreCase(titre))
                    .findFirst()
                    .orElse(null);
        } catch (SQLException ex) {
            showError("Erreur lors de la vérification : " + ex.getMessage());
            return null;
        }
    }

    private LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date)
            return ((java.sql.Date) date).toLocalDate();
        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private String get(TextField f) {
        return (f != null && f.getText() != null) ? f.getText().trim() : "";
    }

    private void showError(String msg) {
        if (errorLabel == null) return;
        errorLabel.setStyle(
                "-fx-background-color:#FEF2F2;-fx-border-color:#FECACA;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1.5;" +
                        "-fx-padding:10 14;-fx-text-fill:#DC2626;-fx-font-size:12.5px;-fx-font-weight:600;");
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        if (errorLabel == null) return;
        errorLabel.setStyle(
                "-fx-background-color:#F0FDF4;-fx-border-color:#BBF7D0;" +
                        "-fx-border-radius:8;-fx-background-radius:8;-fx-border-width:1.5;" +
                        "-fx-padding:10 14;-fx-text-fill:#15803D;-fx-font-size:12.5px;-fx-font-weight:600;");
        errorLabel.setText("✓  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void showEventNameError(String msg) {
        if (eventNameErrorLabel == null) return;
        eventNameErrorLabel.setText("⚠  " + msg);
        eventNameErrorLabel.setVisible(true);
        eventNameErrorLabel.setManaged(true);
        if (eventNameOkLabel != null) {
            eventNameOkLabel.setVisible(false);
            eventNameOkLabel.setManaged(false);
        }
    }

    private void showEventNameOk() {
        if (eventNameOkLabel != null) {
            eventNameOkLabel.setVisible(true);
            eventNameOkLabel.setManaged(true);
        }
        if (eventNameErrorLabel != null) {
            eventNameErrorLabel.setVisible(false);
            eventNameErrorLabel.setManaged(false);
        }
    }

    private void hideEventNameFeedback() {
        if (eventNameErrorLabel != null) { eventNameErrorLabel.setVisible(false); eventNameErrorLabel.setManaged(false); }
        if (eventNameOkLabel    != null) { eventNameOkLabel.setVisible(false);    eventNameOkLabel.setManaged(false); }
    }

    private void hideErrors() {
        if (errorLabel != null) { errorLabel.setVisible(false); errorLabel.setManaged(false); }
        hideEventNameFeedback();
    }

    private void clearForm() {
        if (telephoneField != null) telephoneField.clear();
        if (feedbackField  != null) feedbackField.clear();
        hideErrors();
    }
}