package tn.esprit.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class QuotableService {

    public static String[] getCitation() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://type.fit/api/quotes"))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();

            // Sépare les objets du tableau
            String[] objects = json.split("\\},\\{");

            // Choisit un aléatoire
            int index = (int)(Math.random() * objects.length);
            String obj = objects[index];

            String text   = extractValue(obj, "text");
            String author = extractValue(obj, "author");

            if (text == null || text.isEmpty()) return fallback();

            return new String[]{
                    text,
                    (author == null || author.isEmpty()) ? "Anonyme" : author
            };

        } catch (Exception e) {
            return fallback();
        }
    }

    private static String[] fallback() {
        String[][] citations = {
                {"La connaissance de soi est le début de toute sagesse.", "Aristote"},
                {"Le bonheur n'est pas quelque chose de prêt à l'emploi.", "Dalaï Lama"},
                {"Sois le changement que tu veux voir.", "Gandhi"},
                {"Comprendre, c'est le début de guérir.", "Freud"}
        };
        return citations[(int)(Math.random() * citations.length)];
    }



    private static String[] fallbackRandom() {
        String[][] citations = {
                {"La connaissance de soi est le début de toute sagesse.", "Aristote"},
                {"Le bonheur n'est pas quelque chose de prêt à l'emploi.", "Dalaï Lama"},
                {"Sois le changement que tu veux voir dans le monde.", "Gandhi"},
                {"La résilience c'est danser sous la pluie.", "Anonyme"},
                {"Comprendre, c'est le début de guérir.", "Sigmund Freud"}
        };
        return citations[(int)(Math.random() * citations.length)];
    }

    private static String extractValue(String json, String key) {
        try {
            String search = "\"" + key + "\":\"";
            int start = json.indexOf(search) + search.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end).trim();
        } catch (Exception e) {
            return "";
        }
    }
}