package tn.esprit.controllers;

import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.NotificationItem;
import tn.esprit.services.NotificationService;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ResourceBundle;

/**
 * Contrôleur du panneau de notifications latéral GAUCHE.
 * Intégré dans le layout (HBox) — pas un overlay flottant.
 * L'animation slide-in/slide-out est gérée par le conteneur parent (notifPanelContainer).
 */
public class NotificationPanelController implements Initializable {

    @FXML private VBox       panelRoot;
    @FXML private VBox       listeBox;
    @FXML private Label      lblNonLues;
    @FXML private Button     btnFermer;
    @FXML private ScrollPane scroll;

    private final NotificationService service = NotificationService.getInstance();
    private final SimpleDateFormat    dtFmt   = new SimpleDateFormat("dd/MM HH:mm");

    /**
     * Référence vers le VBox conteneur dans AfficherReservation
     * (notifPanelContainer). Utilisé pour le fermer depuis le bouton ✕.
     */
    private VBox parentContainer;

    /** Callback appelé quand l'utilisateur ferme le panneau via le bouton ✕ */
    private Runnable onFermerCallback;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        rafraichir();

        // Écouter les changements en temps réel
        service.getNotifications().addListener(
                (ListChangeListener<NotificationItem>) c -> rafraichir()
        );
    }

    /**
     * Injecte le conteneur parent VBox et le callback de fermeture.
     * Appelé par AfficherReservationController après le chargement du FXML.
     */
    public void setParentContainer(VBox container, Runnable onFermer) {
        this.parentContainer   = container;
        this.onFermerCallback  = onFermer;
    }

    /**
     * Conservé pour compatibilité si d'autres contrôleurs utilisent encore
     * l'ancienne signature avec StackPane. Ne fait rien de particulier ici
     * car le mode est désormais intégré (pas overlay).
     */
    public void setParentStack(javafx.scene.layout.StackPane stack) {
        // Non utilisé en mode drawer intégré gauche — méthode conservée pour compatibilité
    }

    // ─── Rafraîchissement complet de la liste ──────────────────────────────────
    private void rafraichir() {
        listeBox.getChildren().clear();
        var list = service.getNotifications();

        if (list.isEmpty()) {
            listeBox.getChildren().add(creerVide());
        } else {
            for (NotificationItem n : list) {
                listeBox.getChildren().add(creerCarte(n));
            }
        }

        long nonLues = service.getNonLues();
        lblNonLues.setText(nonLues > 0
                ? nonLues + " non lue" + (nonLues > 1 ? "s" : "")
                : "Tout est lu");
        lblNonLues.setStyle(nonLues > 0
                ? "-fx-font-size:11px; -fx-text-fill:#FF8C00; -fx-font-weight:700;"
                : "-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.6); -fx-font-weight:500;");
    }

    // ─── Carte vide ────────────────────────────────────────────────────────────
    private VBox creerVide() {
        VBox vide = new VBox(10);
        vide.setAlignment(Pos.CENTER);
        vide.setPadding(new Insets(50));
        Label ico = new Label("🔕");
        ico.setStyle("-fx-font-size: 40px;");
        Label txt = new Label("Aucune notification");
        txt.setStyle("-fx-font-size: 13px; -fx-text-fill: #9CA3AF; -fx-font-weight: 600;");
        vide.getChildren().addAll(ico, txt);
        return vide;
    }

    // ─── Création d'une carte notification ─────────────────────────────────────
    private VBox creerCarte(NotificationItem notif) {
        VBox carte = new VBox(8);
        carte.setPadding(new Insets(14, 16, 12, 16));
        appliquerStyleCarte(carte, notif);

        // ── Ligne 1 : icône + titre + heure ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label ico = new Label(notif.getIcone());
        ico.setStyle("-fx-font-size: 20px;");

        Label titre = new Label(notif.getTitre());
        titre.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1A1A2E;");
        titre.setWrapText(true);
        HBox.setHgrow(titre, Priority.ALWAYS);

        Label heure = new Label(notif.getDateCreation() != null
                ? dtFmt.format(notif.getDateCreation()) : "");
        heure.setStyle("-fx-font-size: 10px; -fx-text-fill: #9CA3AF;");

        header.getChildren().addAll(ico, titre, heure);

        // ── Ligne 2 : message ──
        Label msg = new Label(notif.getMessage());
        msg.setStyle("-fx-font-size: 11px; -fx-text-fill: #5A6475; -fx-padding: 0 0 0 30px;");
        msg.setWrapText(true);

        // ── Ligne 3 : badge lu + boutons ──
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_RIGHT);

        if (!notif.isLue()) {
            Label dot = new Label("● Non lue");
            dot.setStyle("-fx-font-size: 9px; -fx-text-fill: " +
                    notif.getCouleurBordure() + "; -fx-font-weight: 700;");
            HBox.setHgrow(dot, Priority.ALWAYS);
            footer.getChildren().add(dot);

            Button btnLu = new Button("✓ Lu");
            btnLu.setStyle(
                    "-fx-background-color: " + notif.getCouleurBordure() + "; -fx-text-fill: white;" +
                            "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 4px 12px;" +
                            "-fx-background-radius: 8px; -fx-cursor: hand;");
            btnLu.setOnAction(e -> { service.marquerLue(notif); rafraichir(); });
            footer.getChildren().add(btnLu);
        }

        Button btnDel = new Button("✕");
        btnDel.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #9CA3AF;" +
                        "-fx-font-size: 13px; -fx-padding: 2px 6px; -fx-cursor: hand;");
        btnDel.setOnAction(e -> service.supprimer(notif));
        footer.getChildren().add(btnDel);

        carte.getChildren().addAll(header, msg, footer);

        // Marquer lu au clic sur la carte
        carte.setOnMouseClicked(e -> { service.marquerLue(notif); rafraichir(); });

        return carte;
    }

    private void appliquerStyleCarte(VBox carte, NotificationItem notif) {
        String opacity = notif.isLue() ? "0.6" : "1.0";
        carte.setStyle(
                "-fx-background-color: " + notif.getCouleurFond() + ";" +
                        "-fx-background-radius: 12px;" +
                        "-fx-border-color: " + notif.getCouleurBordure() + ";" +
                        "-fx-border-width: 0 0 0 4px;" +
                        "-fx-border-radius: 0 12px 12px 0;" +
                        "-fx-opacity: " + opacity + ";" +
                        "-fx-cursor: hand;");
    }

    // ─── Actions header ─────────────────────────────────────────────────────────
    @FXML private void handleToutLu()      { service.marquerToutesLues(); }
    @FXML private void handleToutEffacer() { service.supprimerToutes(); }

    /**
     * Ferme le panneau en slide-out vers la gauche.
     * Réduit la largeur du conteneur parent puis appelle le callback.
     */
    @FXML
    private void handleFermer() {
        if (parentContainer == null) {
            // Fallback : masquer directement
            if (onFermerCallback != null) onFermerCallback.run();
            return;
        }

        // Slide-out vers la gauche sur le panelRoot
        TranslateTransition slide = new TranslateTransition(Duration.millis(260), panelRoot);
        slide.setFromX(0);
        slide.setToX(-370);
        slide.setOnFinished(e -> {
            parentContainer.getChildren().clear();
            parentContainer.setPrefWidth(0);
            parentContainer.setMinWidth(0);
            parentContainer.setMaxWidth(0);
            panelRoot.setTranslateX(0);
            if (onFermerCallback != null) onFermerCallback.run();
        });
        slide.play();
    }

    /**
     * Anime l'entrée du panneau (slide depuis la gauche).
     * Appelé par AfficherReservationController après injection dans le conteneur.
     */
    public void animerEntree() {
        panelRoot.setTranslateX(-370);
        TranslateTransition slide = new TranslateTransition(Duration.millis(280), panelRoot);
        slide.setFromX(-370);
        slide.setToX(0);
        slide.play();
    }
}