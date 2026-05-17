package tn.esprit.utils;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

/**
 * Système de notifications toast (coin haut-droit).
 * Types : SUCCESS, ERROR, INFO, WARNING
 */
public class NotificationManager {

    public enum Type { SUCCESS, ERROR, INFO, WARNING }

    private static Window ownerWindow;

    public static void setOwnerWindow(Window w) { ownerWindow = w; }

    public static void show(String message, Type type) {
        if (ownerWindow == null) return;

        javafx.application.Platform.runLater(() -> {
            Popup popup = new Popup();

            HBox toast = new HBox(12);
            toast.setAlignment(Pos.CENTER_LEFT);
            toast.setStyle(buildStyle(type));

            Label icon  = new Label(getIcon(type));
            icon.setStyle("-fx-font-size:18px;");

            Label lbl = new Label(message);
            lbl.setStyle("-fx-text-fill:#FFFFFF; -fx-font-size:13px; -fx-font-family:'Segoe UI'; -fx-wrap-text:true; -fx-max-width:300px;");
            lbl.setWrapText(true);

            toast.getChildren().addAll(icon, lbl);
            popup.getContent().add(toast);
            popup.setAutoFix(true);

            // Position : haut-droit
            double x = ownerWindow.getX() + ownerWindow.getWidth() - 360;
            double y = ownerWindow.getY() + 60;
            popup.show(ownerWindow, x, y);

            // Animation entrée + sortie
            toast.setOpacity(0);
            toast.setTranslateX(30);

            ParallelTransition enter = new ParallelTransition(
                fadeAnim(toast, 0, 1, 300),
                translateAnim(toast, 30, 0, 300)
            );
            enter.play();

            // Auto-fermeture après 3.5s
            PauseTransition pause = new PauseTransition(Duration.seconds(3.5));
            pause.setOnFinished(e2 -> {
                FadeTransition exit = new FadeTransition(Duration.millis(300), toast);
                exit.setFromValue(1); exit.setToValue(0);
                exit.setOnFinished(e3 -> popup.hide());
                exit.play();
            });
            enter.setOnFinished(e2 -> pause.play());
        });
    }

    // Surcharges raccourcies
    public static void success(String msg) { show(msg, Type.SUCCESS); }
    public static void error(String msg)   { show(msg, Type.ERROR); }
    public static void info(String msg)    { show(msg, Type.INFO); }
    public static void warning(String msg) { show(msg, Type.WARNING); }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String buildStyle(Type t) {
        String bg = switch(t) {
            case SUCCESS -> "#1B4332";
            case ERROR   -> "#7f1d1d";
            case WARNING -> "#78350f";
            case INFO    -> "#1e3a5f";
        };
        return "-fx-background-color:" + bg + ";"
             + "-fx-border-color:rgba(255,255,255,0);"
             + "-fx-border-width:1;"
             + "-fx-border-radius:10;"
             + "-fx-background-radius:10;"
             + "-fx-padding:14 20 14 20;"
             + "-fx-min-width:280;"
             + "-fx-max-width:360;"
             + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.5),20,0,0,6);";
    }

    private static String getIcon(Type t) {
        return switch(t) {
            case SUCCESS -> "✓";
            case ERROR   -> "✕";
            case WARNING -> "⚠";
            case INFO    -> "ℹ";
        };
    }

    private static FadeTransition fadeAnim(javafx.scene.Node n, double from, double to, int ms) {
        FadeTransition ft = new FadeTransition(Duration.millis(ms), n);
        ft.setFromValue(from); ft.setToValue(to); return ft;
    }
    private static TranslateTransition translateAnim(javafx.scene.Node n, double fromX, double toX, int ms) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(ms), n);
        tt.setFromX(fromX); tt.setToX(toX); return tt;
    }
}
