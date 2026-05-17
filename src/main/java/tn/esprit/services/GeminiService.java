package tn.esprit.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.InputStream;
import java.util.Properties;

public class GeminiService {

    private static final String API_KEY = loadApiKey();
    private static final String API_URL =
            // ✅ Nouveau — gemini-1.5-flash (gratuit et disponible)
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    private static String loadApiKey() {
        try {
            Properties props = new Properties();
            InputStream is = GeminiService.class.getResourceAsStream("/config.properties");

            if (is == null) {
                System.out.println("❌ config.properties NON TROUVÉ!");
                return "";
            }

            props.load(is);
            String key = props.getProperty("gemini.api.key");

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
    public static void listModels() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models?key=" + API_KEY))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println(response.body());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String askGemini(String prompt) {
        try {
            if (API_KEY == null || API_KEY.isBlank()) {
                return "❌ Clé API manquante. Vérifiez config.properties";
            }

            HttpClient client = HttpClient.newHttpClient();

            // Escape the prompt safely
            String safePrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

            String jsonBody = "{\n" +
                    "  \"contents\": [{\"parts\": [{\"text\": \"" + safePrompt + "\"}]}]\n" +
                    "}";

            System.out.println("📤 Envoi à Gemini...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📥 Status code: " + response.statusCode());
            System.out.println("📥 Raw response: " + response.body());

            if (response.statusCode() != 200) {
                return "❌ Erreur API (code " + response.statusCode() + "). Vérifiez votre clé.";
            }

            return parseResponse(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Erreur de connexion à l'IA: " + e.getMessage();
        }
    }

    private static String parseResponse(String json) {
        try {
            // Find "text": " and extract value
            int textIndex = json.indexOf("\"text\":");
            if (textIndex == -1) {
                System.out.println("⚠️ Champ 'text' non trouvé dans la réponse.");
                return "Aucune réponse reçue.";
            }

            // Move past "text": and the opening quote
            int start = json.indexOf("\"", textIndex + 7) + 1;
            // Find closing quote (not escaped)
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }

            String result = json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");

            System.out.println("✅ Réponse parsée avec succès.");
            return result;

        } catch (Exception e) {
            System.out.println("❌ Erreur parsing: " + e.getMessage());
            return "Erreur de parsing de la réponse.";
        }
    }

    // ✅ Test rapide — lance directement ce main pour tester
    public static void main(String[] args) {
        System.out.println("=== TEST GEMINI SERVICE ===");
        String response = askGemini("Dis bonjour en français en une phrase.");
        System.out.println("=== RÉPONSE FINALE ===");
        System.out.println(response);
    }
}