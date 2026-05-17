package tn.esprit.services;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * SERVICE DE MODÉRATION DE CONTENU
 *
 * Deux niveaux de modération :
 * 1. API OpenAI (omni-moderation-latest) — si disponible
 * 2. Modération locale (liste de mots interdits) — fallback si API indisponible ou quota dépassé
 *
 * Ainsi le contenu inapproprié est TOUJOURS bloqué même sans API.
 */
public class ModerationService {

    // ══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION API
    // ══════════════════════════════════════════════════════════════════════════

    private static final String API_KEY = System.getenv("OPENAI_API_KEY") != null
            ? System.getenv("OPENAI_API_KEY")
            : "";

    private static final String   MODERATION_URL  = "https://api.openai.com/v1/moderations";
    private static final Duration REQUEST_TIMEOUT  = Duration.ofSeconds(8);
    private static final int      MAX_RETRIES      = 2;
    private static final long     RETRY_DELAY_MS   = 1000;

    // ══════════════════════════════════════════════════════════════════════════
    // MODÉRATION LOCALE — mots et expressions interdits (fallback)
    // ══════════════════════════════════════════════════════════════════════════

    private static final String[][] MOTS_INTERDITS = {
            // Violence
            {"tuer", "Violence"},
            {"je vais te tuer", "Violence"},
            {"assassiner", "Violence"},
            {"frapper", "Violence"},
            {"blesser", "Violence"},
            {"mort", "Violence"},
            {"massacrer", "Violence"},
            {"brutaliser", "Violence"},
            {"torturer", "Violence"},
            {"crever", "Violence"},
            {"sang", "Violence"},
            {"couteau", "Violence"},
            {"pistolet", "Violence"},
            {"arme", "Violence"},
            {"bombe", "Violence"},
            {"exploser", "Violence"},
            {"viol", "Violence"},

            // Insultes / Harcèlement
            {"idiot", "Harcelement"},
            {"espece d'idiot", "Harcelement"},
            {"imbecile", "Harcelement"},
            {"connard", "Harcelement"},
            {"batard", "Harcelement"},
            {"salaud", "Harcelement"},
            {"ordure", "Harcelement"},
            {"dechet", "Harcelement"},
            {"abruti", "Harcelement"},
            {"cretin", "Harcelement"},
            {"nul", "Harcelement"},
            {"ta gueule", "Harcelement"},
            {"ferme la", "Harcelement"},
            {"pourriture", "Harcelement"},

            // Discours haineux
            {"raciste", "Discours haineux"},
            {"nazi", "Discours haineux"},
            {"xenophobe", "Discours haineux"},
            {"islamophobie", "Discours haineux"},
            {"antisemite", "Discours haineux"},
            {"discrimination", "Discours haineux"},

            // Auto-mutilation
            {"suicide", "Auto-mutilation"},
            {"se suicider", "Auto-mutilation"},
            {"se tuer", "Auto-mutilation"},
            {"se blesser", "Auto-mutilation"},
            {"automutilation", "Auto-mutilation"},
            {"se couper", "Auto-mutilation"},
            {"mourir", "Auto-mutilation"},
            {"en finir", "Auto-mutilation"},

            // Contenu sexuel explicite
            {"pornographie", "Contenu sexuel"},
            {"pornographique", "Contenu sexuel"},
            {"sexuel explicite", "Contenu sexuel"},
    };

    // ══════════════════════════════════════════════════════════════════════════
    // RÉSULTAT DE MODÉRATION
    // ══════════════════════════════════════════════════════════════════════════

    public static class ResultatModeration {
        public final boolean  estInapproprie;
        public final String[] categoriesDetectees;
        public final String   messageDetails;
        public final boolean  apiDisponible;
        /** true = résultat vient de la modération locale */
        public final boolean  moderationLocale;

        public ResultatModeration(boolean inapproprie, String[] categories,
                                  String details, boolean apiDisponible,
                                  boolean moderationLocale) {
            this.estInapproprie      = inapproprie;
            this.categoriesDetectees = categories;
            this.messageDetails      = details;
            this.apiDisponible       = apiDisponible;
            this.moderationLocale    = moderationLocale;
        }

        // Constructeurs de compatibilité
        public ResultatModeration(boolean inapproprie, String[] categories, String details, boolean apiDispo) {
            this(inapproprie, categories, details, apiDispo, false);
        }
        public ResultatModeration(boolean inapproprie, String[] categories, String details) {
            this(inapproprie, categories, details, true, false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Analyse le contenu :
     * 1. Essaie l'API OpenAI (avec retry)
     * 2. Si API indisponible → fallback modération locale
     * 3. La modération locale BLOQUE toujours le contenu inapproprié
     */
    public ResultatModeration analyserContenu(String texte) throws Exception {
        if (texte == null || texte.trim().isEmpty()) {
            return new ResultatModeration(false, new String[0],
                    "Texte vide, aucune moderation necessaire", true, false);
        }

        // ── Tentatives API ────────────────────────────────────────────────────
        Exception derniereErreur = null;
        for (int tentative = 1; tentative <= MAX_RETRIES; tentative++) {
            try {
                return appelAPI(texte);
            } catch (Exception e) {
                derniereErreur = e;
                System.err.println("Moderation tentative " + tentative + "/" + MAX_RETRIES
                        + " echouee : " + e.getMessage());
                if (tentative < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ignored) {}
                }
            }
        }

        // ── Fallback : modération locale ──────────────────────────────────────
        System.out.println("API indisponible — moderation locale activee.");
        return moderationLocale(texte);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MODÉRATION LOCALE (sans API)
    // ══════════════════════════════════════════════════════════════════════════

    private ResultatModeration moderationLocale(String texte) {
        String texteLower = texte.toLowerCase()
                .replace("'", "'")
                .replace("é", "e").replace("è", "e").replace("ê", "e")
                .replace("à", "a").replace("â", "a")
                .replace("ù", "u").replace("û", "u")
                .replace("î", "i").replace("ï", "i")
                .replace("ô", "o").replace("ö", "o")
                .replace("ç", "c");

        List<String> categoriesDetectees = new ArrayList<>();
        StringBuilder details = new StringBuilder();

        for (String[] entree : MOTS_INTERDITS) {
            String mot      = entree[0];
            String categorie = entree[1];

            if (texteLower.contains(mot) && !categoriesDetectees.contains(categorie)) {
                categoriesDetectees.add(categorie);
                details.append("- ").append(categorie)
                        .append(" (mot detecte : \"").append(mot).append("\")\n");
            }
        }

        boolean inapproprie = !categoriesDetectees.isEmpty();
        String message = inapproprie
                ? details.toString()
                : "Aucun contenu inapproprie detecte (moderation locale)";

        System.out.println(inapproprie
                ? "Moderation locale : REFUSE — " + String.join(", ", categoriesDetectees)
                : "Moderation locale : APPROUVE");

        return new ResultatModeration(
                inapproprie,
                categoriesDetectees.toArray(new String[0]),
                message,
                false,  // apiDisponible = false
                true    // moderationLocale = true
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // APPEL API OPENAI
    // ══════════════════════════════════════════════════════════════════════════

    private ResultatModeration appelAPI(String texte) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("input", texte);
        requestBody.put("model", "omni-moderation-latest");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MODERATION_URL))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        int code = response.statusCode();
        if (code == 401) throw new Exception("Cle API invalide (401).");
        if (code == 429) throw new Exception("Quota API depasse (429).");
        if (code == 500 || code == 503) throw new Exception("Serveur OpenAI indisponible (" + code + ").");
        if (code != 200) throw new Exception("Erreur API (code " + code + ") : " + response.body());

        JSONObject jsonResponse = new JSONObject(response.body());
        JSONArray  results      = jsonResponse.getJSONArray("results");
        JSONObject result       = results.getJSONObject(0);
        boolean    flagged      = result.getBoolean("flagged");

        JSONObject categories     = result.getJSONObject("categories");
        JSONObject categoryScores = result.getJSONObject("category_scores");

        List<String> liste          = new ArrayList<>();
        StringBuilder detailsBuilder = new StringBuilder();

        String[] toutesCategories = {
                "hate", "hate/threatening",
                "harassment", "harassment/threatening",
                "self-harm", "self-harm/intent", "self-harm/instructions",
                "sexual", "sexual/minors",
                "violence", "violence/graphic"
        };

        for (String categorie : toutesCategories) {
            try {
                if (categories.getBoolean(categorie)) {
                    liste.add(traduireCategorie(categorie));
                    double score = categoryScores.getDouble(categorie);
                    detailsBuilder.append(String.format("- %s (score: %.2f)%n",
                            traduireCategorie(categorie), score));
                }
            } catch (Exception ignored) {}
        }

        return new ResultatModeration(
                flagged,
                liste.toArray(new String[0]),
                flagged ? detailsBuilder.toString() : "Aucun contenu inapproprie detecte",
                true,
                false
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VERSION SIMPLE
    // ══════════════════════════════════════════════════════════════════════════

    public boolean estContenuInapproprie(String texte) {
        try {
            return analyserContenu(texte).estInapproprie;
        } catch (Exception e) {
            System.err.println("Erreur moderation : " + e.getMessage());
            return moderationLocale(texte).estInapproprie;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ══════════════════════════════════════════════════════════════════════════

    private String traduireCategorie(String cat) {
        switch (cat) {
            case "hate":                   return "Discours haineux";
            case "hate/threatening":       return "Discours haineux avec menaces";
            case "harassment":             return "Harcelement";
            case "harassment/threatening": return "Harcelement avec menaces";
            case "self-harm":              return "Auto-mutilation";
            case "self-harm/intent":       return "Intention d'auto-mutilation";
            case "self-harm/instructions": return "Instructions d'auto-mutilation";
            case "sexual":                 return "Contenu sexuel";
            case "sexual/minors":          return "Contenu sexuel impliquant des mineurs";
            case "violence":               return "Violence";
            case "violence/graphic":       return "Violence graphique";
            default:                       return cat;
        }
    }

    public boolean estCleApiConfiguree() {
        return API_KEY != null && !API_KEY.isEmpty() && API_KEY.startsWith("sk-");
    }
}