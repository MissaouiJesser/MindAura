package tn.esprit.controllers;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

public final class RatingUtils {

    private static final String STYLE_BUTTON = "-fx-background-color: transparent; -fx-text-fill: #FFB800; -fx-font-size: 18; -fx-cursor: hand; -fx-padding: 0 2;";
    private static final String STYLE_EMPTY = "-fx-background-color: transparent; -fx-text-fill: #E2E8F0; -fx-font-size: 18; -fx-cursor: hand; -fx-padding: 0 2;";

    private RatingUtils() {
    }

    /**
     * Convertit une note numérique (0-5) en une chaîne d'étoiles "★★★★★".
     */
    public static String toStars(float rate) {
        int rounded = Math.round(rate);
        if (rounded < 0) rounded = 0;
        if (rounded > 5) rounded = 5;
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(i < rounded ? '★' : '☆');
        }
        return sb.toString();
    }

    /**
     * Crée un HBox de 5 étoiles cliquables (1 à 5). Au clic, appelle onRateSelected avec la valeur 1..5.
     * currentRate peut être 0-5 pour l'affichage initial.
     */
    public static HBox createClickableStars(float currentRate, Consumer<Integer> onRateSelected) {
        HBox box = new HBox(2);
        box.setStyle("-fx-alignment: CENTER_LEFT;");
        int value = Math.max(0, Math.min(5, Math.round(currentRate)));
        Button[] stars = new Button[5];
        for (int i = 0; i < 5; i++) {
            final int starValue = i + 1;
            Button btn = new Button(i < value ? "★" : "☆");
            btn.setStyle(i < value ? STYLE_BUTTON : STYLE_EMPTY);
            btn.setOnAction(e -> {
                if (onRateSelected != null) {
                    onRateSelected.accept(starValue);
                }
            });
            stars[i] = btn;
            box.getChildren().add(btn);
        }
        return box;
    }
}

