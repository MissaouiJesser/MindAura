package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service qui interroge l'API Overpass (OpenStreetMap) depuis Java
 * pour récupérer les hôtels et restaurants proches d'un lieu.
 *
 * Appelé depuis UserEventController après géocodage,
 * les résultats sont injectés dans le WebView via executeScript().
 *
 * Aucune clé API requise — 100% gratuit.
 */
public class OverpassService {

    private static final String OVERPASS_URL    = "https://overpass-api.de/api/interpreter";
    private static final int    RADIUS_METERS   = 2500; // rayon élargi pour capturer les restaurants lointains
    private static final int    MIN_WALK_METERS = 1400; // 20 min à pied ≈ 1400 m (70 m/min)

    /**
     * Retourne un tableau JSON (String) des POIs proches :
     * hôtels, guest-houses, motels, et restaurants/fast_food à >= 20 min à pied.
     *
     * Règles appliquées :
     * - Les cafés sont exclus.
     * - Les restaurants et fast_food à moins de 1 400 m (< 20 min à pied) sont exclus.
     * - Les hôtels sont tous retournés dans le rayon.
     *
     * Format de chaque élément :
     * {
     *   "type": "hotel" | "restaurant" | "fast_food",
     *   "name": "...",
     *   "lat": 36.xx,
     *   "lng": 10.xx,
     *   "dist": 1750,
     *   "phone": "...",
     *   "website": "...",
     *   "stars": 3,
     *   "cuisine": "..."
     * }
     *
     * @param lat  Latitude du lieu de l'événement
     * @param lng  Longitude du lieu de l'événement
     * @return     JSON array string, ex: "[{...},{...}]"
     */
    public static String fetchPOIs(double lat, double lng) {
        try {
            String query = buildQuery(lat, lng);
            String response = httpPost(query);
            if (response == null) return "[]";
            return parseResponse(response, lat, lng);
        } catch (Exception e) {
            System.err.println("[OverpassService] Erreur : " + e.getMessage());
            return "[]";
        }
    }

    // ── Construction de la requête Overpass QL ─────────────────────────────────
    private static String buildQuery(double lat, double lng) {
        return "[out:json][timeout:20];\n" +
                "(\n" +
                "  node[tourism=hotel](around:" + RADIUS_METERS + "," + lat + "," + lng + ");\n" +
                "  node[tourism=guest_house](around:" + RADIUS_METERS + "," + lat + "," + lng + ");\n" +
                "  node[tourism=motel](around:" + RADIUS_METERS + "," + lat + "," + lng + ");\n" +
                "  node[amenity=restaurant](around:" + RADIUS_METERS + "," + lat + "," + lng + ");\n" +
                "  node[amenity=fast_food](around:" + RADIUS_METERS + "," + lat + "," + lng + ");\n" +
                ");\n" +
                "out body;";
    }

    // ── Parsing de la réponse Overpass → JSON simplifié ───────────────────────
    private static String parseResponse(String response, double evLat, double evLng) {
        JsonArray elements = JsonParser.parseString(response)
                .getAsJsonObject()
                .getAsJsonArray("elements");

        JsonArray result = new JsonArray();

        for (int i = 0; i < elements.size(); i++) {
            JsonObject el = elements.get(i).getAsJsonObject();

            if (!el.has("lat") || !el.has("lon")) continue;
            JsonObject tags = el.has("tags") ? el.getAsJsonObject("tags") : new JsonObject();

            double poiLat = el.get("lat").getAsDouble();
            double poiLng = el.get("lon").getAsDouble();
            int dist = (int) haversine(evLat, evLng, poiLat, poiLng);

            // Déterminer le type
            String tourism  = getTag(tags, "tourism");
            String amenity  = getTag(tags, "amenity");
            String type;
            if ("hotel".equals(tourism) || "guest_house".equals(tourism) || "motel".equals(tourism)) {
                type = "hotel";
            } else if ("restaurant".equals(amenity)) {
                type = "restaurant";
            } else if ("fast_food".equals(amenity)) {
                type = "fast_food";
            } else {
                continue; // cafés et autres types ignorés
            }

            // Restaurants et fast_food : seulement ceux à >= 20 min à pied (>= 1400 m)
            boolean isFood = "restaurant".equals(type) || "fast_food".equals(type);
            if (isFood && dist < MIN_WALK_METERS) {
                continue;
            }

            // Nom : essayer plusieurs tags
            String name = getTag(tags, "name");
            if (name.isEmpty()) name = getTag(tags, "name:fr");
            if (name.isEmpty()) name = getTag(tags, "name:ar");
            if (name.isEmpty()) name = capitalize(type.replace("_", " "));

            // Étoiles
            int stars = 0;
            String starsTag = getTag(tags, "stars");
            if (!starsTag.isEmpty()) {
                try { stars = Integer.parseInt(starsTag.trim()); } catch (NumberFormatException ignored) {}
            }

            // Construction de l'objet POI
            int walkMinutes = (int) Math.round(dist / 70.0); // 70 m/min = vitesse marche moyenne
            JsonObject poi = new JsonObject();
            poi.addProperty("type",         type);
            poi.addProperty("name",         name);
            poi.addProperty("lat",          poiLat);
            poi.addProperty("lng",          poiLng);
            poi.addProperty("dist",         dist);
            poi.addProperty("walkMinutes",  walkMinutes);
            poi.addProperty("phone",        getTag(tags, "phone").isEmpty() ? getTag(tags, "contact:phone") : getTag(tags, "phone"));
            poi.addProperty("website",      getTag(tags, "website").isEmpty() ? getTag(tags, "contact:website") : getTag(tags, "website"));
            poi.addProperty("stars",        stars);
            poi.addProperty("cuisine",      getTag(tags, "cuisine").replace("_", " "));
            poi.addProperty("opening_hours", getTag(tags, "opening_hours"));

            result.add(poi);
        }

        return result.toString();
    }

    // ── HTTP POST vers Overpass ────────────────────────────────────────────────
    private static String httpPost(String query) throws Exception {
        URL url = new URL(OVERPASS_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(20000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "MindAura/1.0");

        String body = "data=" + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            System.err.println("[OverpassService] HTTP " + status);
            return null;
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private static String getTag(JsonObject tags, String key) {
        return tags.has(key) ? tags.get(key).getAsString() : "";
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}