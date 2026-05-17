package tn.esprit.controllers;

import com.google.gson.*;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;
import tn.esprit.services.EvenementService;
import tn.esprit.services.ParticipationService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


public class ChatbotEventController {

    // ── FXML refs ──────────────────────────────────────────────────────────────
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox       chatMessagesContainer;
    @FXML private TextField  userInputField;
    @FXML private Button     sendButton;
    @FXML private Button     clearButton;
    @FXML private Label      statusLabel;

    // ── Services BD ───────────────────────────────────────────────────────────
    private final EvenementService     evenementService     = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();

    // ── Historique multi-tour ─────────────────────────────────────────────────
    private final List<Map<String, String>> conversationHistory = new ArrayList<>();

    // ── Configuration API Groq ───────────────────────────────────────────────
    private static final String GROQ_API_KEY = "";
    private static final String GROQ_MODEL   = "llama-3.3-70b-versatile";
    private static final String API_URL      = "https://api.groq.com/openai/v1/chat/completions";

    // ═════════════════════════════════════════════════════════════════════════
    //  Initialisation JavaFX
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        sendButton.setOnAction(e -> handleSend());
        clearButton.setOnAction(e -> clearChat());
        userInputField.setOnAction(e -> handleSend());

        chatMessagesContainer.heightProperty().addListener(
                (obs, oldVal, newVal) -> chatScrollPane.setVvalue(1.0)
        );

        showWelcomeMessage();
        statusLabel.setText("En ligne · Prêt à vous aider");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Messages de bienvenue
    // ═════════════════════════════════════════════════════════════════════════

    private void showWelcomeMessage() {
        addBotMessage(
                "Bonjour ! 👋 Je suis votre assistant intelligent MindAura.\n\n" +
                        "Je peux vous aider à :\n" +
                        "• Explorer les événements disponibles\n" +
                        "• Vérifier les places et la capacité\n" +
                        "• Comprendre les types d'événements\n" +
                        "• Vous orienter pour vos participations\n\n" +
                        "Posez-moi n'importe quelle question 🎯"
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Gestion envoi / effacement
    // ═════════════════════════════════════════════════════════════════════════

    @FXML
    private void handleSend() {
        String text = userInputField.getText().trim();
        if (text.isEmpty()) return;

        userInputField.clear();
        addUserMessage(text);

        sendButton.setDisable(true);
        userInputField.setDisable(true);
        statusLabel.setText("💭 En train de réfléchir…");

        Thread t = new Thread(() -> {
            try {
                String context  = buildContextData();
                String response = callGroqAPI(text, context);
                Platform.runLater(() -> {
                    addBotMessage(response);
                    sendButton.setDisable(false);
                    userInputField.setDisable(false);
                    userInputField.requestFocus();
                    statusLabel.setText("En ligne · Prêt à vous aider");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    addBotMessage(
                            "❌ Erreur : " + ex.getMessage() + "\n\n" +
                                    "Conseil : vérifiez que GROQ_API_KEY est correctement renseignée.\n" +
                                    "Obtenez une clé GRATUITE sur : https://console.groq.com"
                    );
                    sendButton.setDisable(false);
                    userInputField.setDisable(false);
                    statusLabel.setText("⚠ Erreur de connexion API");
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void clearChat() {
        chatMessagesContainer.getChildren().clear();
        conversationHistory.clear();
        showWelcomeMessage();
        statusLabel.setText("En ligne · Prêt à vous aider");
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Rendu des bulles de chat
    // ═════════════════════════════════════════════════════════════════════════

    private void addUserMessage(String text) {
        HBox wrapper = new HBox();
        wrapper.setAlignment(Pos.CENTER_RIGHT);
        wrapper.setPadding(new Insets(3, 16, 3, 60));

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(420);
        bubble.setStyle(
                "-fx-background-color: linear-gradient(135deg, #52B788, #2d9b6f);" +
                        "-fx-background-radius: 18 18 4 18;" +
                        "-fx-padding: 10 14;"
        );

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: 500;");

        Label time = new Label(now());
        time.setStyle("-fx-text-fill: rgba(255,255,255,0.60); -fx-font-size: 10px;");
        time.setMaxWidth(Double.MAX_VALUE);
        time.setAlignment(Pos.CENTER_RIGHT);

        bubble.getChildren().addAll(msg, time);
        wrapper.getChildren().add(bubble);
        fadeIn(wrapper);
        chatMessagesContainer.getChildren().add(wrapper);
    }

    private void addBotMessage(String text) {
        HBox wrapper = new HBox(10);
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setPadding(new Insets(3, 60, 3, 16));

        Label avatar = new Label("🤖");
        avatar.setMinSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: rgba(82,183,136,0.16);" +
                        "-fx-background-radius: 18;" +
                        "-fx-font-size: 16px;"
        );

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(460);
        bubble.setStyle(
                "-fx-background-color: rgba(255,255,255,0.055);" +
                        "-fx-border-color: rgba(82,183,136,0.20);" +
                        "-fx-border-radius: 4 18 18 18;" +
                        "-fx-background-radius: 4 18 18 18;" +
                        "-fx-padding: 10 14;"
        );

        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setStyle("-fx-text-fill: rgba(255,255,255,0.88); -fx-font-size: 13px;");

        Label time = new Label("Assistant · " + now());
        time.setStyle("-fx-text-fill: rgba(255,255,255,0.30); -fx-font-size: 10px;");

        bubble.getChildren().addAll(msg, time);
        wrapper.getChildren().addAll(avatar, bubble);
        fadeIn(wrapper);
        chatMessagesContainer.getChildren().add(wrapper);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Construction du contexte depuis la BD
    // ═════════════════════════════════════════════════════════════════════════

    private String buildContextData() {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        try {
            List<Evenements> events = evenementService.getAllEvenements();

            sb.append("=== DONNÉES EN TEMPS RÉEL – Base de données MindAura ===\n\n");
            sb.append("NOMBRE TOTAL D'ÉVÉNEMENTS : ").append(events.size()).append("\n\n");
            sb.append("DÉTAIL DES ÉVÉNEMENTS :\n");

            for (Evenements ev : events) {
                sb.append("───────────────────────────────\n");
                sb.append("Titre      : ").append(ev.getTitreEvenement()).append("\n");
                sb.append("Type       : ").append(ev.getTypeEvenement()).append("\n");
                sb.append("Lieu       : ").append(ev.getLieuEvenement()).append("\n");
                sb.append("Statut     : ").append(ev.getStatutEvenemnt()).append("\n");
                sb.append("Capacité   : ").append(ev.getCapaciteEvenement()).append(" places\n");
                if (ev.getMaxListeAttente() != null)
                    sb.append("Liste att. : ").append(ev.getMaxListeAttente()).append(" max\n");
                if (ev.getDatedebutEvenemnt() != null)
                    sb.append("Début      : ").append(sdf.format(ev.getDatedebutEvenemnt())).append("\n");
                if (ev.getDatefinEvenemnt() != null)
                    sb.append("Fin        : ").append(sdf.format(ev.getDatefinEvenemnt())).append("\n");
                if (ev.getDescriptionEvenement() != null && !ev.getDescriptionEvenement().isBlank())
                    sb.append("Description: ").append(ev.getDescriptionEvenement()).append("\n");
            }

            // Résumé par type
            Map<String, Long> byType = events.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getTypeEvenement() != null ? e.getTypeEvenement() : "Autre",
                            Collectors.counting()
                    ));
            sb.append("\nRÉSUMÉ PAR TYPE :\n");
            byType.forEach((type, cnt) ->
                    sb.append("• ").append(type).append(" : ").append(cnt).append(" événement(s)\n"));

            // Participations
            try {
                List<Participation> parts = participationService.afficherList();
                Map<Integer, Long> countByEvent = parts.stream()
                        .collect(Collectors.groupingBy(Participation::getEvenementId, Collectors.counting()));

                sb.append("\nOCCUPATION DES ÉVÉNEMENTS :\n");
                for (Evenements ev : events) {
                    long inscrits  = countByEvent.getOrDefault(ev.getId(), 0L);
                    long restantes = ev.getCapaciteEvenement() - inscrits;
                    sb.append("• ").append(ev.getTitreEvenement())
                            .append(" → ").append(inscrits).append(" inscrit(s)");
                    if (restantes > 0)
                        sb.append(", ").append(restantes).append(" place(s) restante(s)");
                    else
                        sb.append(" ⚠ COMPLET");
                    sb.append("\n");
                }

                Map<String, Long> byStatut = parts.stream()
                        .collect(Collectors.groupingBy(
                                p -> p.getStatut() != null ? p.getStatut() : "inconnu",
                                Collectors.counting()
                        ));
                sb.append("\nSTATUTS DES PARTICIPATIONS :\n");
                byStatut.forEach((statut, cnt) ->
                        sb.append("• ").append(statut).append(" : ").append(cnt).append("\n"));

            } catch (Exception e) {
                sb.append("\n(Participations non disponibles : ").append(e.getMessage()).append(")\n");
            }

        } catch (SQLException e) {
            sb.append("(Données indisponibles – erreur BD : ").append(e.getMessage()).append(")\n");
        }

        return sb.toString();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Appel à l'API Groq (format OpenAI-compatible)
    // ═════════════════════════════════════════════════════════════════════════

    private String callGroqAPI(String userMessage, String contextData) throws IOException {

        // 1. System prompt
        String systemPrompt =
                "Tu es un assistant intelligent et bienveillant intégré dans MindAura, " +
                        "une plateforme de gestion d'événements de bien-être et de développement personnel.\n\n" +
                        "DONNÉES EN TEMPS RÉEL :\n" + contextData + "\n\n" +
                        "RÈGLES DE RÉPONSE :\n" +
                        "• Réponds toujours en français, avec chaleur et professionnalisme.\n" +
                        "• Appuie-toi exclusivement sur les données fournies ci-dessus pour les faits.\n" +
                        "• Pour chaque événement mentionné, indique : titre, type, lieu, date(s), statut, places restantes.\n" +
                        "• Si l'événement est complet, signale-le clairement et parle de la liste d'attente si disponible.\n" +
                        "• Pour s'inscrire, oriente l'utilisateur vers le bouton « Participer » de la page « Explorer les événements ».\n" +
                        "• Si une question sort du périmètre MindAura, réponds poliment que tu es spécialisé dans les événements.\n" +
                        "• Utilise des listes à puces pour la clarté quand il y a plusieurs éléments.\n" +
                        "• Émojis avec parcimonie (1-2 par réponse max).\n" +
                        "• Ne mentionne jamais les IDs internes de la base de données.\n" +
                        "• Sois concis mais complet. Termine parfois par une courte question d'engagement.\n";

        // 2. Construire le tableau "messages" (format OpenAI)
        JsonArray messages = new JsonArray();

        // Message système
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role",    "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);

        // Ajouter le message utilisateur à l'historique
        Map<String, String> userTurn = new HashMap<>();
        userTurn.put("role", "user");
        userTurn.put("content", userMessage);
        conversationHistory.add(userTurn);

        // Historique (10 derniers tours max)
        int start = Math.max(0, conversationHistory.size() - 10);
        for (int i = start; i < conversationHistory.size(); i++) {
            Map<String, String> turn = conversationHistory.get(i);
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role",    turn.get("role"));
            msgObj.addProperty("content", turn.get("content"));
            messages.add(msgObj);
        }

        // 3. Corps de la requête (format OpenAI standard)
        JsonObject body = new JsonObject();
        body.addProperty("model",       GROQ_MODEL);
        body.addProperty("max_tokens",  1024);
        body.addProperty("temperature", 0.7);
        body.add("messages", messages);

        // 4. Connexion HTTP
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",  "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(30_000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        // 5. Lecture de la réponse
        int code = conn.getResponseCode();
        InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder raw = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) raw.append(line);
        }

        if (code != 200) {
            throw new IOException("Groq API – HTTP " + code + " : " + raw);
        }

        // 6. Parser la réponse (format OpenAI)
        String assistantText = JsonParser.parseString(raw.toString())
                .getAsJsonObject()
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        // 7. Ajouter la réponse à l'historique
        Map<String, String> asstTurn = new HashMap<>();
        asstTurn.put("role",    "assistant");
        asstTurn.put("content", assistantText);
        conversationHistory.add(asstTurn);

        return assistantText;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Utilitaires
    // ═════════════════════════════════════════════════════════════════════════

    private void fadeIn(javafx.scene.Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(280), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private String now() {
        return new SimpleDateFormat("HH:mm").format(new Date());
    }
}