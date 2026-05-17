package tn.esprit.controllers;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.bytedeco.javacv.*;
import tn.esprit.entities.utilisateurs;
import tn.esprit.utils.FaceRecognitionService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FaceAuthController — Intégré dans le panneau droit du Login.
 * ─────────────────────────────────────────────────────────────
 * PLUS DE FENÊTRE EXTERNE : ce contrôleur s'affiche dans rightPanel
 * exactement comme MotDePasseOublieController, VerificationCodeController, etc.
 *
 * Pour fermer la vue et revenir au login : on appelle loginController.showLoginPanel()
 * au lieu de stage.close().
 *
 * 3 phases d'animation conservées :
 *   PHASE 1 : DÉTECTION   → anneau pointillé pulsant + coins de visée
 *   PHASE 2 : SCAN        → ligne scanner + traits biométriques Canvas
 *   PHASE 3 : COMPARAISON → points orbitaux + lignes DNA Canvas
 */
public class FaceAuthController implements Initializable {

    // ─── FXML ─────────────────────────────────────────────────────────────────
    @FXML private ImageView         cameraView;
    @FXML private StackPane         cameraPane;

    // Overlays de phase
    @FXML private StackPane         overlayDetection;
    @FXML private StackPane         overlayScan;
    @FXML private StackPane         overlayCompare;

    // Phase 1 — détection
    @FXML private Circle            detectionRing;
    @FXML private Circle            targetDot;

    // Phase 2 — scan
    @FXML private Canvas            biometricCanvas;
    @FXML private Rectangle         scanLine;
    @FXML private Rectangle         scanLineGlow;
    @FXML private Label             bioLabel1, bioLabel2, bioLabel3, bioLabel4;

    // Phase 3 — comparaison
    @FXML private Canvas            compareCanvas;
    @FXML private Circle            compareRingOuter, compareRingInner;
    @FXML private Circle            orbitDot1, orbitDot2, orbitDot3, orbitDot4;
    @FXML private Label             aiLabel, matchProgress;

    // Contrôles communs
    @FXML private Circle            faceRingOuter;
    @FXML private ProgressIndicator progressRing;
    @FXML private ProgressBar       timeProgressBar;
    @FXML private Label             statusLabel, statusSub, phaseChip;
    @FXML private Label             similarityLabel, facePositionHint, attemptsLabel;
    @FXML private StackPane         btnRetry;
    @FXML private Region            attemptIndicator1, attemptIndicator2, attemptIndicator3;

    // ─── ÉTAT ─────────────────────────────────────────────────────────────────
    private FrameGrabber             grabber;
    private Java2DFrameConverter     converter;
    private ScheduledExecutorService cameraExecutor;
    private ExecutorService          authExecutor;
    private final AtomicBoolean      cameraRunning = new AtomicBoolean(false);
    private final AtomicBoolean      capturing     = new AtomicBoolean(false);
    private int attemptsLeft = 3;
    private int frameCount   = 0;

    private utilisateurs    userToVerify;
    private Runnable        onAuthSuccess, onNoPhoto, onCancel;

    /**
     * Référence au LoginController parent.
     * Injectée par LoginController.showFaceAuth() via setLoginController().
     * Utilisée pour revenir au panneau login (à la place de stage.close()).
     */
    private LoginController loginController;

    // ─── ANIMATIONS ───────────────────────────────────────────────────────────
    private Timeline        detectionPulse;
    private Timeline        scanAnimation;
    private AnimationTimer  compareTimer;
    private Timeline        orbitTimeline;
    private double          orbitAngle   = 0;
    private double          scanY        = -140;
    private int             matchPercent = 0;

    // Constantes
    private static final long   FACE_VERIFY_TIMEOUT_SECONDS = 120;
    private static final int    CAPTURE_DELAY_FRAMES        = 60;
    private static final double CAM_CX = 250, CAM_CY = 170;  // centre exact du cameraPane 500×340

    // ─── Landmarks biométriques simulés ──────────────────────────────────────
    // Landmarks centrés sur (CAM_CX=250, CAM_CY=170) = centre exact du cameraPane 500×340
    private static final double[][] LANDMARK_GROUPS = {
            // Contour visage
            {250,79, 220,89, 195,114, 188,149, 190,184, 198,214, 212,239, 235,256, 250,262,
                    265,256, 288,239, 302,214, 312,184, 312,149, 305,114, 280,89},
            // Sourcil gauche
            {200,122, 210,118, 222,117, 233,120, 240,124},
            // Sourcil droit
            {260,124, 267,120, 278,117, 290,118, 300,122},
            // Oeil gauche
            {200,142, 210,136, 222,136, 232,142, 222,148, 210,148},
            // Oeil droit
            {268,142, 278,136, 290,136, 300,142, 290,148, 278,148},
            // Nez
            {250,152, 244,172, 240,189, 242,196, 250,199, 258,196, 260,189, 256,172},
            // Bouche
            {220,222, 232,216, 244,214, 250,215, 256,214, 268,216, 280,222,
                    268,230, 256,234, 250,236, 244,234, 232,230},
    };

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        converter    = new Java2DFrameConverter();
        authExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "face-auth");
            t.setPriority(Thread.MAX_PRIORITY);
            t.setDaemon(true);
            return t;
        });

        updateAttemptIndicators();
        startPhase1Detection();
        startCamera();
    }

    // =========================================================================
    //  PHASE 1 — DÉTECTION
    // =========================================================================

    private void startPhase1Detection() {
        showOverlay(1);
        setStatus("🔍 Recherche d'un visage...", "Placez-vous face à la caméra");
        setPhaseChip("● DÉTECTION", "#2D6A4F", "rgba(27,67,50,0.12)");

        detectionPulse = new Timeline(
                new KeyFrame(Duration.ZERO,       new KeyValue(detectionRing.rotateProperty(), 0)),
                new KeyFrame(Duration.seconds(4), new KeyValue(detectionRing.rotateProperty(), 360))
        );
        detectionPulse.setCycleCount(Animation.INDEFINITE);
        detectionPulse.play();

        if (targetDot != null) {
            ScaleTransition st = new ScaleTransition(Duration.seconds(1.2), targetDot);
            st.setFromX(0.6); st.setToX(1.4);
            st.setFromY(0.6); st.setToY(1.4);
            st.setCycleCount(Animation.INDEFINITE);
            st.setAutoReverse(true);
            st.play();
        }
    }

    // =========================================================================
    //  PHASE 2 — SCAN
    // =========================================================================

    private void startPhase2Scan() {
        if (detectionPulse != null) detectionPulse.stop();
        showOverlay(2);
        setStatus("🧬 Analyse biométrique...", "Extraction des traits du visage");
        setPhaseChip("● SCAN", "#7B5EA7", "rgba(123,94,167,0.12)");

        clearCanvas(biometricCanvas);
        scanY = -150;

        scanAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scanLine.translateYProperty(), -148),
                        new KeyValue(scanLineGlow.translateYProperty(), -148)),
                new KeyFrame(Duration.seconds(2.5),
                        new KeyValue(scanLine.translateYProperty(), 148),
                        new KeyValue(scanLineGlow.translateYProperty(), 148))
        );
        scanAnimation.setCycleCount(1);

        AnimationTimer scanDrawer = new AnimationTimer() {
            private long lastNs    = 0;
            private int  groupIndex = 0;
            private int  labelShown = 0;

            @Override
            public void handle(long now) {
                if (lastNs == 0) { lastNs = now; return; }
                double elapsed      = (now - lastNs) / 1_000_000_000.0;
                double progress     = Math.min(elapsed / 2.5, 1.0);
                double currentScanY = CAM_CY - 145 + progress * 290;

                GraphicsContext gc = biometricCanvas.getGraphicsContext2D();
                for (int g = groupIndex; g < LANDMARK_GROUPS.length; g++) {
                    double[] pts    = LANDMARK_GROUPS[g];
                    double   groupY = 0;
                    for (int i = 1; i < pts.length; i += 2) groupY += pts[i];
                    groupY /= (pts.length / 2.0);
                    if (groupY <= currentScanY) {
                        drawLandmarkGroup(gc, pts, g);
                        groupIndex = g + 1;
                    }
                }

                if (progress > 0.2 && labelShown < 1) { showLabel(bioLabel1); labelShown = 1; }
                if (progress > 0.45 && labelShown < 2) { showLabel(bioLabel4); labelShown = 2; }
                if (progress > 0.65 && labelShown < 3) { showLabel(bioLabel2); labelShown = 3; }
                if (progress > 0.85 && labelShown < 4) { showLabel(bioLabel3); labelShown = 4; }

                if (progress >= 1.0) {
                    stop();
                    PauseTransition p = new PauseTransition(Duration.millis(800));
                    p.setOnFinished(e -> startPhase3Compare());
                    p.play();
                }
            }
        };

        scanAnimation.play();
        scanDrawer.start();
    }

    private void drawLandmarkGroup(GraphicsContext gc, double[] pts, int groupIdx) {
        boolean isViolet = (groupIdx == 1 || groupIdx == 2 || groupIdx == 3 || groupIdx == 4);
        Color lineColor  = isViolet ? Color.rgb(123,94,167,0.7)  : Color.rgb(45,106,79,0.75);
        Color dotColor   = isViolet ? Color.rgb(180,140,220,0.9) : Color.rgb(80,180,120,0.9);

        gc.setStroke(lineColor);
        gc.setLineWidth(1.2);
        gc.setLineDashes(3, 3);
        gc.beginPath();
        gc.moveTo(pts[0], pts[1]);
        for (int i = 2; i < pts.length - 1; i += 2) gc.lineTo(pts[i], pts[i+1]);
        if (groupIdx == 0) gc.closePath();
        gc.stroke();

        gc.setFill(dotColor);
        gc.setLineDashes(0);
        for (int i = 0; i < pts.length - 1; i += 2) gc.fillOval(pts[i]-2, pts[i+1]-2, 4, 4);

        if (groupIdx == 3 || groupIdx == 4) {
            gc.setStroke(Color.rgb(123,94,167,0.4));
            gc.setLineWidth(0.8);
            gc.setLineDashes(2, 4);
            for (int i = 0; i < pts.length - 1; i += 4)
                gc.strokeLine(pts[i], pts[i+1]-8, pts[i], pts[i+1]+8);
        }
    }

    private void showLabel(Label lbl) {
        if (lbl == null) return;
        lbl.setVisible(true);
        lbl.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), lbl);
        ft.setToValue(1.0);
        ft.play();
    }

    // =========================================================================
    //  PHASE 3 — COMPARAISON
    // =========================================================================

    private void startPhase3Compare() {
        showOverlay(3);
        setStatus("🤖 Comparaison en cours...", "Intelligence artificielle en action");
        setPhaseChip("● COMPARAISON IA", "#7B5EA7", "rgba(123,94,167,0.12)");

        matchPercent = 0;
        orbitAngle   = 0;
        clearCanvas(compareCanvas);

        compareTimer = new AnimationTimer() {
            private long startNs    = 0;
            private long lastLabelNs = 0;

            @Override
            public void handle(long now) {
                if (startNs == 0) startNs = now;
                double elapsed = (now - startNs) / 1_000_000_000.0;
                orbitAngle = elapsed * 90;

                double r  = 162;
                double a1 = Math.toRadians(orbitAngle);
                double a2 = Math.toRadians(orbitAngle + 90);
                double a3 = Math.toRadians(orbitAngle + 180);
                double a4 = Math.toRadians(orbitAngle + 270);

                if (orbitDot1 != null) { orbitDot1.setTranslateX(Math.cos(a1)*r); orbitDot1.setTranslateY(Math.sin(a1)*r); }
                if (orbitDot2 != null) { orbitDot2.setTranslateX(Math.cos(a2)*r); orbitDot2.setTranslateY(Math.sin(a2)*r); }
                if (orbitDot3 != null) { orbitDot3.setTranslateX(Math.cos(a3)*r); orbitDot3.setTranslateY(Math.sin(a3)*r); }
                if (orbitDot4 != null) { orbitDot4.setTranslateX(Math.cos(a4)*r); orbitDot4.setTranslateY(Math.sin(a4)*r); }

                if (compareRingOuter != null) compareRingOuter.setRotate( elapsed * 30);
                if (compareRingInner != null) compareRingInner.setRotate(-elapsed * 45);

                drawDnaLines(compareCanvas.getGraphicsContext2D(), elapsed);

                if (now - lastLabelNs > 120_000_000L && matchPercent < 97) {
                    matchPercent += 3;
                    if (matchProgress != null) matchProgress.setText(matchPercent + "%");
                    lastLabelNs = now;
                }
            }
        };
        compareTimer.start();

        PauseTransition triggerDelay = new PauseTransition(Duration.seconds(1.5));
        triggerDelay.setOnFinished(e -> triggerCapture());
        triggerDelay.play();
    }

    private void drawDnaLines(GraphicsContext gc, double elapsed) {
        gc.clearRect(0, 0, compareCanvas.getWidth(), compareCanvas.getHeight());
        double cx = compareCanvas.getWidth() / 2;
        double cy = compareCanvas.getHeight() / 2;

        gc.setLineWidth(1.0);
        gc.setLineDashes(0);
        for (int strand = 0; strand < 3; strand++) {
            double phase = strand * (Math.PI * 2 / 3) + elapsed * 1.5;
            gc.setStroke(strand % 2 == 0
                    ? Color.rgb(45,106,79,0.3) : Color.rgb(123,94,167,0.3));
            gc.beginPath();
            for (int x = -120; x <= 120; x += 2) {
                double y = Math.sin((x * 0.08) + phase) * 28;
                if (x == -120) gc.moveTo(cx+x, cy+y);
                else           gc.lineTo(cx+x, cy+y);
            }
            gc.stroke();
            gc.setFill(strand % 2 == 0
                    ? Color.rgb(80,180,100,0.5) : Color.rgb(180,140,220,0.5));
            for (int x = -120; x <= 120; x += 20) {
                double y = Math.sin((x * 0.08) + phase) * 28;
                gc.fillOval(cx+x-2, cy+y-2, 4, 4);
            }
        }
        gc.setLineWidth(0.7);
        gc.setLineDashes(3, 5);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45 + elapsed * 20);
            double r1 = 30, r2 = 80 + Math.sin(elapsed * 3 + i) * 15;
            gc.setStroke(Color.rgb(45,106,79,0.2));
            gc.strokeLine(cx+Math.cos(angle)*r1, cy+Math.sin(angle)*r1,
                    cx+Math.cos(angle)*r2, cy+Math.sin(angle)*r2);
        }
    }

    // =========================================================================
    //  GESTION DES OVERLAYS
    // =========================================================================

    private void showOverlay(int phase) {
        if (overlayDetection != null) overlayDetection.setVisible(phase == 1);
        if (overlayScan      != null) overlayScan.setVisible(phase == 2);
        if (overlayCompare   != null) overlayCompare.setVisible(phase == 3);
    }

    private void clearCanvas(Canvas c) {
        if (c != null) c.getGraphicsContext2D().clearRect(0, 0, c.getWidth(), c.getHeight());
    }

    private void setPhaseChip(String text, String textColor, String bgColor) {
        if (phaseChip == null) return;
        phaseChip.setText(text);
        phaseChip.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                        "-fx-text-fill:" + textColor + ";" +
                        "-fx-font-size:9px; -fx-font-weight:bold;" +
                        "-fx-font-family:'Consolas',monospace;" +
                        "-fx-background-radius:12; -fx-padding:3 10 3 10;"
        );
    }

    // =========================================================================
    //  WEBCAM
    // =========================================================================

    private void startCamera() {
        cameraExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "webcam");
            t.setDaemon(true);
            return t;
        });

        cameraExecutor.execute(() -> {
            try {
                grabber = new OpenCVFrameGrabber(0);
                grabber.setImageWidth(320);
                grabber.setImageHeight(240);
                grabber.setFrameRate(30);
                grabber.start();
                cameraRunning.set(true);

                frameCount = 0;
                while (cameraRunning.get()) {
                    Frame frame = grabber.grab();
                    if (frame == null || frame.image == null) { Thread.sleep(10); continue; }

                    BufferedImage bi = converter.convert(frame);
                    if (bi == null) { Thread.sleep(10); continue; }

                    final WritableImage fxImg = SwingFXUtils.toFXImage(bi, null);
                    Platform.runLater(() -> {
                        if (cameraView != null) {
                            cameraView.setImage(fxImg);
                            updateFacePositionHint(bi);
                        }
                    });

                    frameCount++;
                    if (frameCount == CAPTURE_DELAY_FRAMES && !capturing.get())
                        Platform.runLater(this::startPhase2Scan);

                    Thread.sleep(33);
                }

            } catch (FrameGrabber.Exception e) {
                Platform.runLater(() -> {
                    setStatus("❌ Caméra inaccessible", "Vérifiez votre webcam");
                    if (btnRetry != null) btnRetry.setVisible(true);
                });
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void updateFacePositionHint(BufferedImage image) {
        if (image == null || capturing.get() || facePositionHint == null) return;
        try {
            int rgb        = image.getRGB(image.getWidth() / 2, image.getHeight() / 2);
            int brightness = ((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF);
            if (brightness < 100) {
                Platform.runLater(() -> {
                    facePositionHint.setText("💡 Améliorez l'éclairage");
                    facePositionHint.setStyle(facePositionHint.getStyle() + "; -fx-text-fill:#F1C40F;");
                });
            } else {
                Platform.runLater(() -> facePositionHint.setText(""));
            }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  CAPTURE ET VÉRIFICATION
    // =========================================================================

    private void triggerCapture() {
        if (capturing.get()) return;
        capturing.set(true);

        authExecutor.submit(() -> {
            try {
                String photoPath = (userToVerify != null)
                        ? userToVerify.getPhoto_profil_utilisateur() : null;

                if (photoPath == null || photoPath.isBlank()) {
                    Platform.runLater(this::handleNoPhoto);
                    return;
                }

                Frame frame = (grabber != null) ? grabber.grab() : null;
                if (frame == null || frame.image == null) {
                    Platform.runLater(() -> showAuthError("Capture impossible — réessayez"));
                    return;
                }

                BufferedImage bi = converter.convert(frame);
                if (bi == null) {
                    Platform.runLater(() -> showAuthError("Erreur image — réessayez"));
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bi, "jpg", baos);
                byte[] capturedBytes = baos.toByteArray();

                Platform.runLater(() -> setStatus("🤖 Analyse IA en cours...",
                        "⏳ Modèle ArcFace — 30-60s au 1er lancement"));

                Future<FaceRecognitionService.FaceCompareResult> future =
                        Executors.newSingleThreadExecutor().submit(() ->
                                FaceRecognitionService.compareFaces(photoPath, capturedBytes));

                FaceRecognitionService.FaceCompareResult result =
                        future.get(FACE_VERIFY_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                Platform.runLater(() -> handleAuthResult(result));

            } catch (TimeoutException e) {
                Platform.runLater(() -> showAuthError(
                        "Délai dépassé (" + FACE_VERIFY_TIMEOUT_SECONDS + "s). Vérifiez Python et DeepFace."));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAuthError("Erreur : " + e.getMessage()));
            }
        });
    }

    // =========================================================================
    //  RÉSULTATS
    // =========================================================================

    private void handleAuthResult(FaceRecognitionService.FaceCompareResult result) {
        stopAllAnimations();
        capturing.set(false);

        if (result.isSuccess()) {
            showAuthSuccess(result.similarity);
        } else {
            attemptsLeft--;
            updateAttemptIndicators();
            updateAttemptsLabel();
            showAuthError(result.message);
        }
    }

    private void showAuthSuccess(double similarity) {
        cameraRunning.set(false);
        showOverlay(0);

        if (faceRingOuter != null) {
            faceRingOuter.setStroke(Color.web("#2D6A4F"));
            faceRingOuter.setStrokeWidth(5);
            faceRingOuter.setEffect(new javafx.scene.effect.Glow(0.6));
        }

        setStatus("✅ Identité confirmée !", "Bienvenue !");
        setPhaseChip("✓ SUCCÈS", "#2D6A4F", "rgba(27,67,50,0.15)");

        if (similarityLabel != null) {
            similarityLabel.setText(String.format("%.0f%%", similarity));
            similarityLabel.setStyle("-fx-text-fill:#2D6A4F; -fx-font-size:26px;" +
                    "-fx-font-weight:bold; -fx-padding:8 0 2 0;");
        }

        if (cameraPane != null) {
            FadeTransition flash = new FadeTransition(Duration.millis(200), cameraPane);
            flash.setFromValue(1.0); flash.setToValue(0.7);
            flash.setCycleCount(2); flash.setAutoReverse(true);
            flash.setOnFinished(e -> {
                PauseTransition successDelay = new PauseTransition(Duration.millis(600));
                // ── NOUVEAU : notifier le parent au lieu de fermer un Stage ──
                successDelay.setOnFinished(ev -> notifySuccess());
                successDelay.play();
            });
            flash.play();
        } else {
            notifySuccess();
        }
    }

    private void showAuthError(String message) {
        stopAllAnimations();
        capturing.set(false);
        showOverlay(0);

        if (faceRingOuter != null) {
            faceRingOuter.setStroke(Color.web("#C0392B"));
            faceRingOuter.setStrokeWidth(4);
        }

        if (attemptsLeft <= 0) {
            setStatus("🔒 Accès refusé", "Tentatives épuisées");
            setPhaseChip("✗ REFUSÉ", "#C0392B", "rgba(192,57,43,0.1)");
            PauseTransition denied = new PauseTransition(Duration.millis(1500));
            denied.setOnFinished(e -> notifyCancel());
            denied.play();
        } else {
            setStatus("❌ " + message, "Tentatives restantes : " + attemptsLeft);
            setPhaseChip("✗ ÉCHEC", "#C0392B", "rgba(192,57,43,0.1)");
            if (btnRetry != null) btnRetry.setVisible(true);
            if (cameraPane != null) shakeNode(cameraPane);
        }
    }

    private void handleNoPhoto() {
        stopAllAnimations();
        cameraRunning.set(false);
        setStatus("📷 Photo requise", "Ajoutez une photo de profil");
        setPhaseChip("⚠ PHOTO MANQUANTE", "#5A6475", "rgba(90,100,117,0.1)");
        PauseTransition delay = new PauseTransition(Duration.millis(1200));
        delay.setOnFinished(e -> notifyNoPhoto());
        delay.play();
    }

    // =========================================================================
    //  NOTIFICATION DU PARENT — remplace closeWindow() + callback
    // =========================================================================

    /**
     * Arrête la webcam et notifie le LoginController du succès.
     * Remplace : stage.close() + onAuthSuccess.run()
     */
    private void notifySuccess() {
        stopCamera();
        if (onAuthSuccess != null) onAuthSuccess.run();
    }

    /**
     * Arrête la webcam et notifie le LoginController d'un annulation/échec.
     * Remplace : stage.close() + onCancel.run()
     */
    private void notifyCancel() {
        stopCamera();
        if (onCancel != null) onCancel.run();
    }

    /**
     * Arrête la webcam et notifie le LoginController qu'une photo est nécessaire.
     * Remplace : stage.close() + onNoPhoto.run()
     */
    private void notifyNoPhoto() {
        stopCamera();
        if (onNoPhoto != null) onNoPhoto.run();
    }

    // =========================================================================
    //  ANIMATIONS UTILITAIRES
    // =========================================================================

    private void stopAllAnimations() {
        if (detectionPulse != null) detectionPulse.stop();
        if (scanAnimation   != null) scanAnimation.stop();
        if (compareTimer    != null) compareTimer.stop();
        if (orbitTimeline   != null) orbitTimeline.stop();
        if (progressRing    != null) progressRing.setVisible(false);
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), node);
        tt.setCycleCount(6); tt.setAutoReverse(true); tt.setByX(8);
        tt.play();
    }

    private void setStatus(String main, String sub) {
        if (statusLabel != null) statusLabel.setText(main);
        if (statusSub   != null) statusSub.setText(sub);
    }

    private void updateAttemptIndicators() {
        if (attemptIndicator1 == null) return;
        String active   = "-fx-background-color:#1B4332; -fx-background-radius:4; -fx-min-width:32; -fx-min-height:5;";
        String inactive = "-fx-background-color:#C8D8CE; -fx-background-radius:4; -fx-min-width:32; -fx-min-height:5;";
        attemptIndicator1.setStyle(attemptsLeft >= 1 ? active : inactive);
        attemptIndicator2.setStyle(attemptsLeft >= 2 ? active : inactive);
        attemptIndicator3.setStyle(attemptsLeft >= 3 ? active : inactive);
    }

    private void updateAttemptsLabel() {
        if (attemptsLabel != null)
            attemptsLabel.setText(attemptsLeft + " tentative"
                    + (attemptsLeft > 1 ? "s" : "") + " restante"
                    + (attemptsLeft > 1 ? "s" : ""));
    }

    // =========================================================================
    //  ACTIONS FXML
    // =========================================================================

    @FXML
    private void handleRetry() {
        if (attemptsLeft <= 0) return;

        if (faceRingOuter != null) {
            faceRingOuter.setStroke(Color.TRANSPARENT);
            faceRingOuter.setStrokeWidth(0);
            faceRingOuter.setEffect(null);
        }

        if (btnRetry != null) btnRetry.setVisible(false);
        if (similarityLabel != null) similarityLabel.setText("");

        frameCount = 0;
        capturing.set(false);
        hideBioLabels();
        startPhase1Detection();

        PauseTransition retryDelay = new PauseTransition(Duration.seconds(2));
        retryDelay.setOnFinished(e -> { if (!capturing.get()) startPhase2Scan(); });
        retryDelay.play();
    }

    /**
     * Bouton "Annuler" : arrête la webcam et retourne au panneau login.
     * Remplace l'ancien : stage.close() + onCancel.run()
     */
    @FXML
    private void handleCancel() {
        notifyCancel();
    }

    private void hideBioLabels() {
        Label[] labels = {bioLabel1, bioLabel2, bioLabel3, bioLabel4};
        for (Label l : labels) { if (l != null) { l.setVisible(false); l.setOpacity(0); } }
    }

    // =========================================================================
    //  NETTOYAGE WEBCAM
    // =========================================================================

    /**
     * Arrête proprement la webcam et les threads associés.
     * Appelé par notifySuccess / notifyCancel / notifyNoPhoto
     * (à la place de closeWindow() qui fermait le Stage).
     */
    private void stopCamera() {
        stopAllAnimations();
        cameraRunning.set(false);
        if (cameraExecutor != null) {
            cameraExecutor.shutdownNow();
            try { cameraExecutor.awaitTermination(500, TimeUnit.MILLISECONDS); }
            catch (InterruptedException ignored) {}
        }
        if (authExecutor != null) authExecutor.shutdownNow();
        try {
            if (grabber != null) { grabber.stop(); grabber.release(); }
        } catch (Exception ignored) {}
    }

    // =========================================================================
    //  SETTERS
    // =========================================================================

    public void setUserToVerify(utilisateurs user)  { this.userToVerify  = user; }
    public void setOnAuthSuccess(Runnable r)         { this.onAuthSuccess = r; }
    public void setOnNoPhoto(Runnable r)             { this.onNoPhoto     = r; }
    public void setOnCancel(Runnable r)              { this.onCancel      = r; }

    /**
     * Injecté par LoginController.showFaceAuth().
     * Permet à FaceAuthController de revenir au login sans fermer un Stage.
     */
    public void setLoginController(LoginController lc) { this.loginController = lc; }
}