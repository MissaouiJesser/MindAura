package tn.esprit.services;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class GeminiServiceR {

    private static final String API_KEY = loadApiKey();

    // ✅ Groq — rapide, gratuit, accessible depuis la Tunisie
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Modèle Groq — vous pouvez aussi utiliser "mixtral-8x7b-32768"
    private static final String MODEL = "llama-3.3-70b-versatile";

    // ─── Chargement de la clé API ────────────────────────────────────────────

    private static String loadApiKey() {
        try {
            Properties props = new Properties();
            InputStream is = GeminiServiceR.class.getResourceAsStream("/config.properties");

            if (is == null) {
                System.out.println("❌ config.properties NON TROUVÉ!");
                return "";
            }

            props.load(is);
            String key = props.getProperty("gemini.api.key1");

            if (key == null || key.isBlank()) {
                System.out.println("❌ Clé API vide dans config.properties!");
                return "";
            }

            System.out.println("✅ Clé chargée: " + key.substring(0, 8) + "...");
            return key;

        } catch (Exception e) {
            System.out.println("❌ Erreur chargement clé: " + e.getMessage());
            return "";
        }
    }

    // ─── Appel principal (signature identique à l'ancienne version) ──────────

    public static String askGemini(String prompt) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "❌ Clé API manquante. Vérifiez config.properties";
        }

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                System.out.println("📤 Envoi à Groq... (tentative " + attempt + "/" + maxRetries + ")");
                String result = sendRequest(prompt);
                System.out.println("✅ Réponse reçue avec succès.");
                return result;

            } catch (Exception e) {
                System.out.println("⚠️ Tentative " + attempt + " échouée: " + e.getMessage());
                if (attempt == maxRetries) {
                    e.printStackTrace();
                    return "❌ Erreur de connexion à l'IA après " + maxRetries + " tentatives: " + e.getMessage();
                }
                try {
                    Thread.sleep(1000L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return "❌ Erreur inattendue.";
    }

    // ─── Envoi de la requête HTTP ─────────────────────────────────────────────

    private static String sendRequest(String prompt) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        String jsonBody = buildJsonBody(prompt);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + API_KEY)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("📥 Status code: " + response.statusCode());
        System.out.println("📥 Raw response: " + response.body());

        if (response.statusCode() == 429) {
            throw new Exception("Quota API dépassé (429). Réessayez plus tard.");
        }
        if (response.statusCode() != 200) {
            throw new Exception("Erreur API (code " + response.statusCode() + "): " + response.body());
        }

        return parseResponse(response.body());
    }

    // ─── Construction du corps JSON (format OpenAI/Groq) ─────────────────────

    private static String buildJsonBody(String prompt) {
        String safe = prompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");

        return "{\n" +
                "  \"model\": \"" + MODEL + "\",\n" +
                "  \"messages\": [\n" +
                "    {\"role\": \"user\", \"content\": \"" + safe + "\"}\n" +
                "  ],\n" +
                "  \"temperature\": 0.7,\n" +
                "  \"max_tokens\": 1024\n" +
                "}";
    }

    // ─── Parsing de la réponse JSON (format OpenAI/Groq) ─────────────────────

    private static String parseResponse(String json) {
        try {
            // Format Groq : {"choices":[{"message":{"content":"..."}}]}
            int contentIndex = json.indexOf("\"content\":");
            if (contentIndex == -1) {
                System.out.println("⚠️ Champ 'content' non trouvé dans la réponse.");
                return "Aucune réponse reçue.";
            }

            int start = json.indexOf("\"", contentIndex + 10) + 1;
            int end = start;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '"' && json.charAt(end - 1) != '\\') break;
                end++;
            }

            String result = json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\t", "\t");

            System.out.println("✅ Réponse parsée avec succès.");
            return result;

        } catch (Exception e) {
            System.out.println("❌ Erreur parsing: " + e.getMessage());
            return "Erreur de parsing de la réponse.";
        }
    }

    // ─── Test rapide ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("=== TEST GROQ SERVICE ===");
        String response = askGemini("Dis bonjour en français en une phrase.");
        System.out.println("=== RÉPONSE FINALE ===");
        System.out.println(response);
    }
}