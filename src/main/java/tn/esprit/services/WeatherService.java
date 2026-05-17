package tn.esprit.services;

import com.google.gson.*;
import java.net.URI;
import java.net.http.*;
import java.util.ArrayList;
import java.util.List;

public class WeatherService {

    private static final String BASE_URL = "http://localhost:5000";
    private static final HttpClient client = HttpClient.newHttpClient();

    // =========================================================================
    //  MODÈLE — Prévision d'un jour
    // =========================================================================
    public static class PrevisionJour {
        public String date;
        public String label;        // "Aujourd'hui", "Demain", "Lundi"...
        public double tempMax;
        public double tempMin;
        public double precipitation;
        public double windspeed;
        public String prediction;   // "sunny", "rainy", "cloudy"...
        public double confidence;   // % de confiance

        @Override
        public String toString() {
            return label + " | " + prediction + " (" + confidence + "%) | "
                    + tempMin + "°C - " + tempMax + "°C";
        }
    }

    // =========================================================================
    //  PRÉVISIONS 7 JOURS — GET /api/predict/weather/forecast
    // =========================================================================
    public static List<PrevisionJour> getPrevisions7Jours() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/predict/weather/forecast"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Erreur API: " + response.statusCode());
        }

        return parseForecasts(response.body());
    }

    // =========================================================================
    //  PRÉDICTION MANUELLE — POST /api/predict/weather
    // =========================================================================
    public static PrevisionJour predire(double tempMax, double tempMin,
                                        double precipitation, double windspeed,
                                        int day, int month) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("temp_max",      tempMax);
        body.addProperty("temp_min",      tempMin);
        body.addProperty("precipitation", precipitation);
        body.addProperty("windspeed",     windspeed);
        body.addProperty("day",           day);
        body.addProperty("month",         month);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/predict/weather"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        PrevisionJour p = new PrevisionJour();
        p.prediction = json.get("prediction").getAsString();
        p.confidence = json.get("confidence").getAsDouble();
        return p;
    }

    // =========================================================================
    //  PARSING JSON
    // =========================================================================
    private static List<PrevisionJour> parseForecasts(String json) {
        List<PrevisionJour> list = new ArrayList<>();
        JsonObject root     = JsonParser.parseString(json).getAsJsonObject();
        JsonArray  forecast = root.getAsJsonArray("forecast");

        for (JsonElement el : forecast) {
            JsonObject obj = el.getAsJsonObject();
            PrevisionJour p = new PrevisionJour();
            p.date          = obj.get("date").getAsString();
            p.label         = obj.get("label").getAsString();
            p.tempMax       = obj.get("temp_max").getAsDouble();
            p.tempMin       = obj.get("temp_min").getAsDouble();
            p.precipitation = obj.get("precipitation").getAsDouble();
            p.windspeed     = obj.get("windspeed").getAsDouble();
            p.prediction    = obj.get("prediction").getAsString();
            p.confidence    = obj.get("confidence").getAsDouble();
            list.add(p);
        }
        return list;
    }
}