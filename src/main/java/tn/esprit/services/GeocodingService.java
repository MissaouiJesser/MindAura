package tn.esprit.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.CompletableFuture;

import tn.esprit.utils.MyDataBase;

/**
 * Service de géocodage : convertit un nom de lieu en coordonnées GPS.
 *
 * Supporte deux APIs au choix (configurez via la constante API_PROVIDER) :
 *   - MAPBOX  : gratuit jusqu'à 100 000 requêtes/mois
 *   - GOOGLE  : payant mais plus précis pour la Tunisie
 *
 * Utilisation :
 *   double[] coords = GeocodingService.geocode("Sidi Bou Saïd, Tunis");
 *   // → [36.8704, 10.3412]
 *
 * Cache BDD : les coordonnées sont stockées dans la table `evenement`
 * (colonnes latitude, longitude) pour éviter les appels API répétés.
 *
 * ═══════════════════════════════════════════════════════════════════
 * MIGRATION BDD REQUISE — exécutez ce SQL une seule fois :
 * ═══════════════════════════════════════════════════════════════════
 *
 *   ALTER TABLE evenement
 *     ADD COLUMN latitude  DOUBLE NULL,
 *     ADD COLUMN longitude DOUBLE NULL;
 *
 * ═══════════════════════════════════════════════════════════════════
 */
public class GeocodingService {

    // ── Configuration ─────────────────────────────────────────────────────────
    public enum ApiProvider { MAPBOX, GOOGLE, NOMINATIM }

    /** Choisissez votre provider ici */
    private static final ApiProvider API_PROVIDER = ApiProvider.NOMINATIM;

    /**
     * Votre clé Mapbox (obtenez-la sur https://account.mapbox.com)
     * Format : pk.eyJ1IjoiVk9U...
     */
    private static final String MAPBOX_TOKEN = "VOTRE_TOKEN_MAPBOX_ICI";

    /**
     * Votre clé Google Maps (obtenez-la sur https://console.cloud.google.com)
     * Activez l'API "Geocoding API" dans votre projet Google Cloud.
     */
    private static final String GOOGLE_KEY = "VOTRE_CLE_GOOGLE_ICI";

    // ── API Nominatim (OpenStreetMap) — GRATUIT, sans clé ─────────────────────
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/search?format=json&limit=1&q=";

    // ── Coordonnées par défaut (centre de Tunis) ──────────────────────────────
    private static final double[] DEFAULT_COORDS = {36.8188, 10.1657};

    private static final Connection conx = MyDataBase.getInstance().getConx();

    // ═════════════════════════════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Géocode un lieu et met à jour la BDD si eventId > 0.
     *
     * @param lieu    Adresse ou nom du lieu (ex: "La Marsa, Tunis")
     * @param eventId ID de l'événement (0 = pas de cache BDD)
     * @return tableau [latitude, longitude]
     */
    public static double[] geocode(String lieu, int eventId) {
        if (lieu == null || lieu.isBlank()) return DEFAULT_COORDS;

        // 1. Vérifier le cache BDD
        if (eventId > 0) {
            double[] cached = getCachedCoords(eventId);
            if (cached != null) return cached;
        }

        // 2. Appel API
        double[] coords = switch (API_PROVIDER) {
            case MAPBOX     -> geocodeMapbox(lieu);
            case GOOGLE     -> geocodeGoogle(lieu);
            case NOMINATIM  -> geocodeNominatim(lieu);
        };

        // 3. Sauvegarder dans la BDD
        if (eventId > 0 && coords != DEFAULT_COORDS) {
            saveCoordsToDb(eventId, coords[0], coords[1]);
        }

        return coords;
    }

    /** Version sans cache BDD (juste le géocodage) */
    public static double[] geocode(String lieu) {
        return geocode(lieu, 0);
    }

    /** Version asynchrone (ne bloque pas le thread JavaFX) */
    public static CompletableFuture<double[]> geocodeAsync(String lieu, int eventId) {
        return CompletableFuture.supplyAsync(() -> geocode(lieu, eventId));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GÉOCODAGE NOMINATIM (OpenStreetMap) — GRATUIT, aucune clé requise
    // Limite : 1 requête/seconde. Pour plus de volume → Mapbox ou Google.
    // ═════════════════════════════════════════════════════════════════════════

    private static double[] geocodeNominatim(String lieu) {
        try {
            // Enrichir avec "Tunisie" si absent pour améliorer la précision
            String query = lieu.toLowerCase().contains("tunis") ? lieu : lieu + ", Tunisie";
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlStr = NOMINATIM_URL + encoded;

            String response = httpGet(urlStr, "MindAura/1.0 (contact@mindaura.tn)");
            if (response == null || response.equals("[]")) return DEFAULT_COORDS;

            JsonArray arr = JsonParser.parseString(response).getAsJsonArray();
            if (arr.isEmpty()) return DEFAULT_COORDS;

            JsonObject first = arr.get(0).getAsJsonObject();
            double lat = first.get("lat").getAsDouble();
            double lon = first.get("lon").getAsDouble();
            return new double[]{lat, lon};

        } catch (Exception e) {
            System.err.println("[GeocodingService] Nominatim error: " + e.getMessage());
            return DEFAULT_COORDS;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GÉOCODAGE MAPBOX — 100k requêtes/mois gratuites
    // Doc : https://docs.mapbox.com/api/search/geocoding/
    // ═════════════════════════════════════════════════════════════════════════

    private static double[] geocodeMapbox(String lieu) {
        try {
            String encoded = URLEncoder.encode(lieu, StandardCharsets.UTF_8);
            String urlStr = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + encoded + ".json"
                    + "?access_token=" + MAPBOX_TOKEN
                    + "&country=TN"          // limiter à la Tunisie
                    + "&limit=1"
                    + "&language=fr";

            String response = httpGet(urlStr, null);
            if (response == null) return DEFAULT_COORDS;

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            JsonArray features = json.getAsJsonArray("features");
            if (features.isEmpty()) return DEFAULT_COORDS;

            JsonArray center = features.get(0).getAsJsonObject()
                    .getAsJsonArray("center");
            // Mapbox retourne [lng, lat]
            double lng = center.get(0).getAsDouble();
            double lat = center.get(1).getAsDouble();
            return new double[]{lat, lng};

        } catch (Exception e) {
            System.err.println("[GeocodingService] Mapbox error: " + e.getMessage());
            return DEFAULT_COORDS;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GÉOCODAGE GOOGLE MAPS — Payant mais très précis
    // Doc : https://developers.google.com/maps/documentation/geocoding
    // Activez "Geocoding API" dans Google Cloud Console
    // ═════════════════════════════════════════════════════════════════════════

    private static double[] geocodeGoogle(String lieu) {
        try {
            String encoded = URLEncoder.encode(lieu + ", Tunisie", StandardCharsets.UTF_8);
            String urlStr = "https://maps.googleapis.com/maps/api/geocode/json"
                    + "?address=" + encoded
                    + "&key=" + GOOGLE_KEY
                    + "&region=tn"
                    + "&language=fr";

            String response = httpGet(urlStr, null);
            if (response == null) return DEFAULT_COORDS;

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String status = json.get("status").getAsString();
            if (!"OK".equals(status)) return DEFAULT_COORDS;

            JsonObject location = json.getAsJsonArray("results")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("geometry")
                    .getAsJsonObject("location");

            double lat = location.get("lat").getAsDouble();
            double lng = location.get("lng").getAsDouble();
            return new double[]{lat, lng};

        } catch (Exception e) {
            System.err.println("[GeocodingService] Google error: " + e.getMessage());
            return DEFAULT_COORDS;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CACHE BDD
    // ═════════════════════════════════════════════════════════════════════════

    private static double[] getCachedCoords(int eventId) {
        try {
            String sql = "SELECT latitude, longitude FROM evenement WHERE id=? " +
                    "AND latitude IS NOT NULL AND longitude IS NOT NULL";
            try (PreparedStatement ps = conx.prepareStatement(sql)) {
                ps.setInt(1, eventId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new double[]{rs.getDouble("latitude"), rs.getDouble("longitude")};
                }
            }
        } catch (Exception e) {
            // Colonnes pas encore créées → migration SQL non exécutée
            System.err.println("[GeocodingService] Cache miss (migration SQL requise?) : " + e.getMessage());
        }
        return null;
    }

    private static void saveCoordsToDb(int eventId, double lat, double lng) {
        try {
            String sql = "UPDATE evenement SET latitude=?, longitude=? WHERE id=?";
            try (PreparedStatement ps = conx.prepareStatement(sql)) {
                ps.setDouble(1, lat);
                ps.setDouble(2, lng);
                ps.setInt(3, eventId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[GeocodingService] Impossible de sauvegarder: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HTTP HELPER
    // ═════════════════════════════════════════════════════════════════════════

    private static String httpGet(String urlStr, String userAgent) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(8000);
        if (userAgent != null) conn.setRequestProperty("User-Agent", userAgent);
        conn.setRequestProperty("Accept", "application/json");

        int status = conn.getResponseCode();
        if (status != 200) {
            System.err.println("[GeocodingService] HTTP " + status + " for: " + urlStr);
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

    // ═════════════════════════════════════════════════════════════════════════
    // GÉOCODAGE EN MASSE (utilitaire de migration)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * À appeler une seule fois pour géocoder tous les événements existants
     * sans coordonnées GPS dans la BDD.
     *
     * Exemple d'appel :
     *   GeocodingService.geocodeAllExisting();
     */
    public static void geocodeAllExisting() throws SQLException {
        String sql = "SELECT id, lieu_evenement FROM evenement " +
                "WHERE latitude IS NULL OR longitude IS NULL";
        try (PreparedStatement ps = conx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                String lieu = rs.getString("lieu_evenement");
                System.out.println("[GeocodingService] Géocodage: " + lieu);

                double[] coords = geocode(lieu, id);
                System.out.println("  → " + coords[0] + ", " + coords[1]);
                count++;

                // Respecter la limite de Nominatim (1 req/sec)
                if (API_PROVIDER == ApiProvider.NOMINATIM) {
                    try { Thread.sleep(1100); } catch (InterruptedException ignored) {}
                }
            }
            System.out.println("[GeocodingService] " + count + " événements géocodés.");
        }
    }
}