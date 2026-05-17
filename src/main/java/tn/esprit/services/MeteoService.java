package tn.esprit.services;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MeteoService {

    // Coordonnées de Tunis (adapte selon ta ville)
    private static final String URL =
            "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=36.8065&longitude=10.1815" +
                    "&current=temperature_2m,weathercode" +
                    "&timezone=Africa%2FTunis";

    public static String[] getMeteo() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            String json = response.body();

            // Chercher dans "current":{...} directement
            int currentIndex = json.indexOf("\"current\":{");
            String currentJson = json.substring(currentIndex);

            String tempStr = extractValue(currentJson, "temperature_2m");
            String codeStr = extractValue(currentJson, "weathercode");

            double temp = Double.parseDouble(tempStr);
            int code    = Integer.parseInt(codeStr);

            String emoji   = getEmoji(code);
            String desc    = getDescription(code);
            String message = getMessageMotivant(code, temp);

            return new String[]{
                    emoji + " " + desc + " — " + temp + "°C",
                    message
            };

        } catch (Exception e) {
            System.out.println("Meteo error: " + e.getMessage());
            return new String[]{"🌤 Météo indisponible", "Bonne journée !"};
        }
    }

    private static String getEmoji(int code) {
        if (code == 0)              return "☀️";
        if (code <= 2)              return "🌤";
        if (code <= 3)              return "☁️";
        if (code <= 48)             return "🌫";
        if (code <= 67)             return "🌧";
        if (code <= 77)             return "❄️";
        if (code <= 82)             return "🌦";
        if (code <= 99)             return "⛈";
        return "🌡";
    }

    private static String getDescription(int code) {
        if (code == 0)  return "Ciel dégagé";
        if (code <= 2)  return "Partiellement nuageux";
        if (code <= 3)  return "Nuageux";
        if (code <= 48) return "Brouillard";
        if (code <= 67) return "Pluie";
        if (code <= 77) return "Neige";
        if (code <= 82) return "Averses";
        if (code <= 99) return "Orage";
        return "Météo variable";
    }

    private static String getMessageMotivant(int code, double temp) {
        if (code == 0 && temp > 20)
            return "🌞 Magnifique journée ! Profitez-en pour une promenade méditative.";
        if (code == 0)
            return "✨ Ciel dégagé, esprit clair ! Parfait pour se fixer de nouveaux objectifs.";
        if (code <= 2)
            return "🌤 Belle journée ! Un peu d'air frais fait du bien à l'esprit.";
        if (code <= 3)
            return "☁️ Journée couverte, idéale pour la réflexion et la méditation intérieure.";
        if (code <= 67)
            return "🌧 Journée pluvieuse ? Parfait pour lire, méditer et prendre soin de soi.";
        if (code <= 77)
            return "❄️ Il neige ! Restez au chaud et prenez soin de votre bien-être.";
        if (code <= 99)
            return "⛈ Orage dehors, mais restez serein(e) — la tempête passe toujours.";
        return "🌡 Prenez soin de vous aujourd'hui !";
    }

    private static String extractValue(String json, String key) {
        try {
            String search = "\"" + key + "\":";
            int start = json.indexOf(search) + search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim().replace("\"", "");
        } catch (Exception e) {
            return "0";
        }
    }
}