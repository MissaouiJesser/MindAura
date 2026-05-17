package tn.esprit.services;

import com.google.gson.*;

import java.net.URI;
import java.net.http.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class NewsApiService {

    private static final String API_KEY = "40247464c1ac406cb858825285f38e5e";
    private static final String API_URL = "https://newsapi.org/v2/everything";

    private static final HttpClient client = HttpClient.newHttpClient();

    // =========================================================================
    //  MODÈLE — Article
    // =========================================================================
    public static class Article {
        public String titre;
        public String description;
        public String url;
        public String source;
        public String date;
        public String urlImage;

        @Override
        public String toString() {
            return titre;
        }
    }

    // =========================================================================
    //  THÈMES DISPONIBLES (même logique que le service Symfony)
    // =========================================================================
    public enum Theme {
        TOUS            ("psychologie OR \"santé mentale\" OR \"développement personnel\" OR mindfulness OR résilience",   "🌐 Tous"),
        PSYCHOLOGIE     ("psychologie OR \"santé mentale\" OR psychothérapie OR anxiété OR dépression OR burnout",        "🧠 Psychologie"),
        DEV_PERSONNEL   ("\"développement personnel\" OR motivation OR \"confiance en soi\" OR coaching OR résilience",    "🌱 Développement personnel"),
        MINDFULNESS     ("mindfulness OR méditation OR \"pleine conscience\"",                                             "🧘 Mindfulness"),
        STRESS          ("stress OR burnout OR \"épuisement professionnel\"",                                              "😤 Stress & Burnout"),
        ANXIETE         ("anxiété OR \"trouble anxieux\" OR \"attaque de panique\"",                                       "😰 Anxiété"),
        SOMMEIL         ("sommeil OR insomnie OR \"qualité du sommeil\"",                                                  "😴 Sommeil"),
        RELATIONS       ("\"relations humaines\" OR empathie OR \"intelligence émotionnelle\"",                            "🤝 Relations");

        public final String query;
        public final String label;

        Theme(String query, String label) {
            this.query = query;
            this.label = label;
        }

        @Override
        public String toString() { return label; }
    }

    // =========================================================================
    //  MÉTHODE PRINCIPALE — Récupérer les articles
    // =========================================================================
    public static List<Article> getArticles(Theme theme, int pageSize) {
        try {
            String query = URLEncoder.encode(theme.query, StandardCharsets.UTF_8);
            String urlStr = API_URL
                    + "?apiKey="   + API_KEY
                    + "&q="        + query
                    + "&searchIn=title"
                    + "&language=fr"
                    + "&sortBy=publishedAt"
                    + "&pageSize=" + pageSize;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            return parseArticles(response.body());

        } catch (Exception e) {
            System.err.println("❌ NewsAPI erreur : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =========================================================================
    //  RECHERCHE LIBRE
    // =========================================================================
    public static List<Article> search(String userQuery, int pageSize) {
        try {
            String enriched = "(" + userQuery + ") AND (psychologie OR \"santé mentale\" OR \"développement personnel\")";
            String query    = URLEncoder.encode(enriched, StandardCharsets.UTF_8);
            String urlStr   = API_URL
                    + "?apiKey="   + API_KEY
                    + "&q="        + query
                    + "&searchIn=title"
                    + "&language=fr"
                    + "&sortBy=publishedAt"
                    + "&pageSize=" + pageSize;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());

            return parseArticles(response.body());

        } catch (Exception e) {
            System.err.println("❌ NewsAPI search erreur : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =========================================================================
    //  PARSING JSON
    // =========================================================================
    private static List<Article> parseArticles(String json) {
        List<Article> list = new ArrayList<>();
        try {
            JsonObject root     = JsonParser.parseString(json).getAsJsonObject();
            JsonArray  articles = root.getAsJsonArray("articles");

            for (JsonElement el : articles) {
                JsonObject obj = el.getAsJsonObject();

                String titre = getString(obj, "title");
                String desc  = getString(obj, "description");

                // Filtrer les articles supprimés ou sans description
                if ("[Removed]".equals(titre) || desc.isEmpty()) continue;

                Article a   = new Article();
                a.titre       = titre;
                a.description = desc;
                a.url         = getString(obj, "url");
                a.date        = getString(obj, "publishedAt").replace("T", " ").replace("Z", "").substring(0, Math.min(16, getString(obj, "publishedAt").length()));
                a.urlImage    = getString(obj, "urlToImage");

                if (obj.has("source") && !obj.get("source").isJsonNull()) {
                    a.source = getString(obj.getAsJsonObject("source"), "name");
                }

                list.add(a);
            }
        } catch (Exception e) {
            System.err.println("❌ Parsing NewsAPI : " + e.getMessage());
        }
        return list;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }
}