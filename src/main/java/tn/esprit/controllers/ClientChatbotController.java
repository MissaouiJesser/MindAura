package tn.esprit.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.esprit.services.GeminiServiceR;

/**
 * Contrôleur Chatbot IA pour les clients
 * Utilise Gemini pour répondre aux questions
 */
public class ClientChatbotController{

    @FXML
    private VBox chatContainer;

    @FXML
    private TextField tfMessage;

    @FXML
    private Button btnEnvoyer;

    @FXML
    private ScrollPane scrollChat;

    @FXML
    private ProgressBar pbChargement;

    private static final String SYSTEM_PROMPT = "Tu es un assistant client professionnel et amical pour un service de gestion de réclamations. " +
            "Réponds aux questions des clients sur:\n" +
            "- Comment déposer une réclamation\n" +
            "- Le suivi de leur réclamation\n" +
            "- Les différentes catégories (COACH, PSYCHOLOGUE, EVENEMENT, AUTRE)\n" +
            "- Les statuts de réclamation (EN_ATTENTE, EN_COURS, TRAITEE)\n" +
            "- Les délais de traitement\n" +
            "- Comment contacter le support\n\n" +
            "Sois courtois, empathique et utile. Réponds en français.";

    @FXML
    public void initialize() {
        System.out.println("🤖 Initialisation du chatbot client...");

        if (pbChargement != null) {
            pbChargement.setVisible(false);
        }

        if (btnEnvoyer != null) {
            btnEnvoyer.setOnAction(e -> envoyerMessage());
        }

        if (tfMessage != null) {
            tfMessage.setOnKeyPressed(e -> {
                if (e.getCode().toString().equals("ENTER")) {
                    envoyerMessage();
                }
            });
        }

        // Message de bienvenue
        afficherMessageBot("Bonjour! 👋 Je suis votre assistant IA. Comment puis-je vous aider?");

        System.out.println("✓ Chatbot client prêt");
    }

    /**
     * Envoyer un message utilisateur
     */
    private void envoyerMessage() {
        String message = tfMessage.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        // Afficher le message de l'utilisateur
        afficherMessageUtilisateur(message);

        // Vider le champ
        tfMessage.clear();

        // Afficher la barre de chargement
        if (pbChargement != null) {
            pbChargement.setVisible(true);
        }

        if (btnEnvoyer != null) {
            btnEnvoyer.setDisable(true);
        }

        // Appeler Gemini en arrière-plan
        new Thread(() -> {
            try {
                String prompt = SYSTEM_PROMPT + "\n\nQuestion du client: " + message;
                String reponse = GeminiServiceR.askGemini(prompt);

                System.out.println("✅ Réponse reçue de Gemini");

                // Mettre à jour l'UI
                javafx.application.Platform.runLater(() -> {
                    afficherMessageBot(reponse);

                    if (pbChargement != null) {
                        pbChargement.setVisible(false);
                    }
                    if (btnEnvoyer != null) {
                        btnEnvoyer.setDisable(false);
                    }

                    // Scroll vers le bas
                    scrollChat.setVvalue(1.0);
                });

            } catch (Exception e) {
                System.err.println("❌ Erreur: " + e.getMessage());
                javafx.application.Platform.runLater(() -> {
                    afficherMessageBot("❌ Erreur: Je n'ai pas pu traiter votre demande. Veuillez réessayer.");

                    if (pbChargement != null) {
                        pbChargement.setVisible(false);
                    }
                    if (btnEnvoyer != null) {
                        btnEnvoyer.setDisable(false);
                    }
                });
            }
        }).start();
    }

    /**
     * Afficher un message de l'utilisateur
     */
    private void afficherMessageUtilisateur(String texte) {
        HBox messageBox = new HBox();
        messageBox.setSpacing(10);
        messageBox.setPadding(new Insets(10));
        messageBox.setStyle("-fx-alignment: CENTER_RIGHT;");

        Label message = new Label(texte);
        message.setWrapText(true);
        message.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-padding: 10; " +
                "-fx-background-radius: 10; -fx-max-width: 600;");

        messageBox.getChildren().add(message);
        chatContainer.getChildren().add(messageBox);
    }

    /**
     * Afficher un message du bot
     */
    private void afficherMessageBot(String texte) {
        HBox messageBox = new HBox();
        messageBox.setSpacing(10);
        messageBox.setPadding(new Insets(10));
        messageBox.setStyle("-fx-alignment: CENTER_LEFT;");

        Label message = new Label(texte);
        message.setWrapText(true);
        message.setStyle("-fx-background-color: #f0f4f8; -fx-text-fill: #1a1a2e; -fx-padding: 10; " +
                "-fx-background-radius: 10; -fx-max-width: 600; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

        messageBox.getChildren().add(message);
        chatContainer.getChildren().add(messageBox);
    }

    /**
     * Nettoyer la conversation
     */
    public void nettoyer() {
        chatContainer.getChildren().clear();
        afficherMessageBot("Conversation réinitialisée. Comment puis-je vous aider?");
    }
}