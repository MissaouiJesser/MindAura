package tn.esprit.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Service de traduction utilisant l'API gratuite MyMemory.
 * Inclut un cache local pour éviter les appels répétés.
 *
 * MyMemory : https://mymemory.translated.net/doc/spec.php
 * Limite gratuite : 5 000 mots/jour
 */
public class TranslationService {

    // ── Constantes ─────────────────────────────────────────────────────────────
    private static final String API_URL = "https://api.mymemory.translated.net/get";

    // Langues supportées : code MyMemory
    public static final String LANG_FR = "fr";
    public static final String LANG_EN = "en";
    public static final String LANG_AR = "ar";
    public static final String LANG_ES = "es";

    // ── Singleton ───────────────────────────────────────────────────────────────
    private static TranslationService instance;

    public static TranslationService getInstance() {
        if (instance == null) {
            instance = new TranslationService();
        }
        return instance;
    }

    // ── Cache : "texte|sourceLang|targetLang" → traduction ─────────────────────
    private final Map<String, String> cache = new HashMap<>();

    // Langue courante de l'application
    private String currentLanguage = LANG_FR;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TranslationService() {}

    // ── API publique ────────────────────────────────────────────────────────────

    /**
     * Définit la langue courante de l'application.
     */
    public void setCurrentLanguage(String langCode) {
        this.currentLanguage = langCode;
    }

    /**
     * Retourne la langue courante.
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Traduit un texte du français vers la langue cible courante.
     * Retourne le texte original si la langue cible est le français.
     *
     * @param texteFr Texte source en français
     * @return Texte traduit (ou texte original si FR)
     */
    public String traduire(String texteFr) {
        return traduire(texteFr, LANG_FR, currentLanguage);
    }

    /**
     * Traduit un texte d'une langue source vers une langue cible.
     * Utilise le cache pour éviter les appels API redondants.
     *
     * @param texte      Texte à traduire
     * @param langSource Code langue source (ex: "fr")
     * @param langCible  Code langue cible  (ex: "en")
     * @return Texte traduit, ou texte original en cas d'erreur
     */
    public String traduire(String texte, String langSource, String langCible) {
        if (texte == null || texte.trim().isEmpty()) return texte;
        if (langSource.equals(langCible)) return texte;

        // Clé de cache
        String cacheKey = texte + "|" + langSource + "|" + langCible;
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        try {
            String encoded = URLEncoder.encode(texte, StandardCharsets.UTF_8);
            String langPair = langSource + "|" + langCible;
            String urlStr = API_URL + "?q=" + encoded + "&langpair=" + langPair;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8);
                StringBuilder response = new StringBuilder();
                while (scanner.hasNextLine()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();

                JsonNode root = objectMapper.readTree(response.toString());
                int status = root.path("responseStatus").asInt();

                if (status == 200) {
                    String translated = root
                            .path("responseData")
                            .path("translatedText")
                            .asText();

                    if (translated != null && !translated.isEmpty()) {
                        cache.put(cacheKey, translated);
                        return translated;
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("⚠️ Erreur API traduction: " + e.getMessage());
        }

        // En cas d'erreur : retourner le texte original
        return texte;
    }

    /**
     * Traduction asynchrone (pour ne pas bloquer le thread JavaFX).
     * Le résultat est fourni via un callback.
     */
    public void traduireAsync(String texteFr, TraductionCallback callback) {
        if (LANG_FR.equals(currentLanguage)) {
            callback.onResult(texteFr);
            return;
        }

        new Thread(() -> {
            String result = traduire(texteFr);
            javafx.application.Platform.runLater(() -> callback.onResult(result));
        }).start();
    }

    /**
     * Vide le cache de traduction.
     */
    public void viderCache() {
        cache.clear();
    }

    /**
     * Retourne le nom affiché d'une langue à partir de son code.
     */
    public static String getNomLangue(String code) {
        switch (code) {
            case LANG_FR: return "🇫🇷 Français";
            case LANG_EN: return "🇬🇧 English";
            case LANG_AR: return "🇸🇦 العربية";
            case LANG_ES: return "🇪🇸 Español";
            default:      return code;
        }
    }

    // ── Interface callback ──────────────────────────────────────────────────────
    @FunctionalInterface
    public interface TraductionCallback {
        void onResult(String texteTraduit);
    }
}
