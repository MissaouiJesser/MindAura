package tn.esprit.services;

import okhttp3.*;

import java.io.InputStream;
import java.util.Properties;

/**
 * Service de traduction via DeepL API Free
 * ✅ 500 000 caractères/mois gratuits
 * ✅ Meilleure qualité de traduction
 * ✅ Sans Gson - parsing JSON manuel
 */
public class TranslationserviceR {

    private static final String API_KEY = loadApiKey();

    // ⚠️ DeepL Free API utilise api-free.deepl.com (pas api.deepl.com)
    private static final String API_URL = "https://api-free.deepl.com/v2/translate";
    private static final OkHttpClient client = new OkHttpClient();

    /**
     * Charger la clé API depuis config.properties
     */
    private static String loadApiKey() {
        try {
            Properties props = new Properties();
            InputStream is = TranslationserviceR.class.getResourceAsStream("/config.properties");
            if (is == null) {
                System.out.println("❌ config.properties NON TROUVÉ!");
                return "";
            }
            props.load(is);
            String key = props.getProperty("deepl.api.key");
            if (key == null || key.isBlank()) {
                System.out.println("❌ Clé DeepL vide dans config.properties!");
                return "";
            }
            System.out.println("✅ Clé DeepL chargée");
            return key;
        } catch (Exception e) {
            System.out.println("❌ Erreur chargement clé: " + e.getMessage());
            return "";
        }
    }

    /**
     * Traduire un texte avec DeepL
     *
     * @param texte      Le texte à traduire
     * @param langCible  Langue cible ("FR", "EN", "AR", "ES", "DE"...) — majuscules
     * @return Le texte traduit, ou texte original en cas d'erreur
     */
    public static String traduire(String texte, String langCible) {
        if (texte == null || texte.isBlank()) return texte;

        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("❌ Clé API DeepL manquante dans config.properties");
            return texte;
        }

        try {
            // DeepL utilise application/x-www-form-urlencoded
            RequestBody requestBody = new FormBody.Builder()
                    .add("text", texte)
                    .add("target_lang", langCible.toUpperCase())
                    .build();

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(requestBody)
                    .addHeader("Authorization", "DeepL-Auth-Key " + API_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.err.println("❌ Erreur DeepL: " + response.code());
                    return texte;
                }

                String responseBody = response.body().string();
                System.out.println("📥 Réponse DeepL: " + responseBody);

                // Parser manuellement
                // Réponse: {"translations":[{"detected_source_language":"EN","text":"Bonjour"}]}
                String texteTraduit = extraireValeurJson(responseBody, "text");

                if (texteTraduit == null || texteTraduit.isBlank()) {
                    System.err.println("❌ Traduction vide dans la réponse");
                    return texte;
                }

                System.out.println("✅ Traduction DeepL → " + langCible + ": " + texteTraduit);
                return texteTraduit;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur traduction: " + e.getMessage());
            return texte;
        }
    }

    /**
     * Parser JSON simple sans bibliothèque externe
     */
    private static String extraireValeurJson(String json, String cle) {
        try {
            String recherche = "\"" + cle + "\"";
            int index = json.indexOf(recherche);
            if (index == -1) return null;

            int debutValeur = json.indexOf("\"", index + recherche.length() + 1);
            if (debutValeur == -1) return null;

            int finValeur = json.indexOf("\"", debutValeur + 1);
            while (finValeur != -1 && json.charAt(finValeur - 1) == '\\') {
                finValeur = json.indexOf("\"", finValeur + 1);
            }
            if (finValeur == -1) return null;

            return json.substring(debutValeur + 1, finValeur)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Raccourcis pratiques
     */
    public static String versFrancais(String texte) {
        return traduire(texte, "FR");
    }

    public static String versAnglais(String texte) {
        return traduire(texte, "EN");
    }

    public static String versArabe(String texte) {
        return traduire(texte, "AR");
    }

    /**
     * Test
     */
    public static void main(String[] args) {
        System.out.println("=== TEST DeepL API Free ===\n");

        String texte1 = "My coach was very unprofessional and rude.";
        System.out.println("Original (EN): " + texte1);
        System.out.println("Traduit  (FR): " + versFrancais(texte1));
        System.out.println();

        String texte2 = "Je suis très insatisfait du service.";
        System.out.println("Original (FR): " + texte2);
        System.out.println("Traduit  (EN): " + versAnglais(texte2));
        System.out.println();

        String texte3 = "El servicio fue muy malo.";
        System.out.println("Original (ES): " + texte3);
        System.out.println("Traduit  (FR): " + versFrancais(texte3));
    }
}