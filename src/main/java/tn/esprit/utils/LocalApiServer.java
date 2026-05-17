package tn.esprit.utils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import tn.esprit.entities.local_psychiatrie;
import tn.esprit.services.local_psychiatrie_SERVICE;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Serveur HTTP léger embarqué dans l'application JavaFX.
 * Expose une page HTML à l'adresse http://<IP>:<PORT>/locaux
 * qui affiche tous les locaux DISPONIBLES.
 *
 * Utilise com.sun.net.httpserver (inclus dans le JDK, aucune dépendance externe).
 *
 * Usage :
 *   LocalApiServer server = LocalApiServer.getInstance();
 *   server.start();
 *   String url = server.getUrl();   // à encoder dans le QR code
 *   server.stop();
 */
public class LocalApiServer {

    private static final int PORT = 8100;
    private static LocalApiServer instance;

    private HttpServer httpServer;
    private boolean running = false;

    private LocalApiServer() {}

    public static LocalApiServer getInstance() {
        if (instance == null) instance = new LocalApiServer();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DÉMARRAGE / ARRÊT
    // ─────────────────────────────────────────────────────────────────────────

    public void start() throws IOException {
        if (running) return;

        httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // Route principale : liste des locaux disponibles (HTML)
        httpServer.createContext("/locaux", this::handleLocauxHtml);

        // Route JSON (optionnelle, utile pour d'autres applis)
        httpServer.createContext("/api/locaux", this::handleLocauxJson);

        httpServer.setExecutor(Executors.newFixedThreadPool(4));
        httpServer.start();
        running = true;
        System.out.println("✅ Serveur QR démarré sur http://localhost:" + PORT + "/locaux");
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            running = false;
            System.out.println("🛑 Serveur QR arrêté.");
        }
    }

    public boolean isRunning() { return running; }

    /**
     * Retourne l'URL à encoder dans le QR code.
     * Utilise l'IP locale de la machine pour être accessible depuis un téléphone.
     */
    public String getUrl() {
        String ip = getLocalIpAddress();
        return "http://" + ip + ":" + PORT + "/locaux";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HANDLERS HTTP
    // ─────────────────────────────────────────────────────────────────────────

    /** Renvoie une page HTML responsive listant les locaux disponibles */
    private void handleLocauxHtml(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "text/plain", "Method Not Allowed");
            return;
        }

        try {
            local_psychiatrie_SERVICE service = new local_psychiatrie_SERVICE();
            List<local_psychiatrie> locaux = service.afficherList();

            StringBuilder html = new StringBuilder();
            html.append(buildHtmlHeader());

            // Filtrer uniquement les disponibles
            List<local_psychiatrie> disponibles = locaux.stream()
                    .filter(l -> "Disponible".equals(l.getDisponibilite_local()))
                    .toList();

            html.append("<div class='container'>");
            html.append("<header>");
            html.append("<h1>🏥 Locaux Disponibles</h1>");
            html.append("<p class='subtitle'>").append(disponibles.size())
                    .append(" local(aux) disponible(s)</p>");
            html.append("</header>");

            if (disponibles.isEmpty()) {
                html.append("<div class='empty'>Aucun local disponible pour le moment.</div>");
            } else {
                html.append("<div class='grid'>");
                for (local_psychiatrie local : disponibles) {
                    html.append(buildLocalCard(local));
                }
                html.append("</div>");
            }

            html.append("</div>");
            html.append(buildHtmlFooter());

            sendResponse(exchange, 200, "text/html; charset=UTF-8", html.toString());

        } catch (SQLException e) {
            String errorPage = "<html><body><h2>Erreur serveur</h2><p>" + e.getMessage() + "</p></body></html>";
            sendResponse(exchange, 500, "text/html; charset=UTF-8", errorPage);
        }
    }

    /** Renvoie un JSON des locaux disponibles */
    private void handleLocauxJson(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        // Autoriser l'accès depuis n'importe quelle origine (CORS)
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        try {
            local_psychiatrie_SERVICE service = new local_psychiatrie_SERVICE();
            List<local_psychiatrie> locaux = service.afficherList();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            for (local_psychiatrie l : locaux) {
                if (!"Disponible".equals(l.getDisponibilite_local())) continue;
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(l.getId_local()).append(",")
                        .append("\"nom\":\"").append(escape(l.getNom_local())).append("\",")
                        .append("\"adresse\":\"").append(escape(l.getAdresse_local())).append("\",")
                        .append("\"ville\":\"").append(escape(l.getVille_local())).append("\",")
                        .append("\"type\":\"").append(escape(l.getType_local().getLibelle())).append("\",")
                        .append("\"telephone\":").append(l.getTelephone_local()).append(",")
                        .append("\"email\":\"").append(escape(l.getEmail_local())).append("\",")
                        .append("\"capacite\":\"").append(escape(l.getCapacite_local())).append("\",")
                        .append("\"disponibilite\":\"").append(escape(l.getDisponibilite_local())).append("\",")
                        .append("\"description\":\"").append(escape(l.getDescription_local())).append("\"")
                        .append("}");
                first = false;
            }
            json.append("]");

            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.toString());

        } catch (SQLException e) {
            sendResponse(exchange, 500, "application/json", "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HTML BUILDER
    // ─────────────────────────────────────────────────────────────────────────

    private String buildHtmlHeader() {
        return """
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Locaux Disponibles</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                            background: linear-gradient(135deg, #e8f5e9 0%, #f3e5f5 100%);
                            min-height: 100vh;
                            padding: 20px;
                        }
                        .container { max-width: 900px; margin: 0 auto; }
                        header {
                            text-align: center;
                            padding: 30px 0 20px;
                        }
                        header h1 {
                            font-size: 2rem;
                            color: #1B4332;
                            margin-bottom: 8px;
                        }
                        .subtitle {
                            color: #2D6A4F;
                            font-size: 1rem;
                            background: #D8F3DC;
                            display: inline-block;
                            padding: 4px 14px;
                            border-radius: 20px;
                        }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                            gap: 20px;
                            margin-top: 20px;
                        }
                        .card {
                            background: #fff;
                            border-radius: 16px;
                            padding: 20px;
                            box-shadow: 0 4px 15px rgba(0,0,0,0.08);
                            border-top: 4px solid #2D6A4F;
                            transition: transform 0.2s;
                        }
                        .card:hover { transform: translateY(-4px); }
                        .card h2 {
                            font-size: 1.1rem;
                            color: #1B4332;
                            margin-bottom: 12px;
                            border-bottom: 1px solid #e0f2e9;
                            padding-bottom: 8px;
                        }
                        .card .badge {
                            display: inline-block;
                            background: #D8F3DC;
                            color: #1B4332;
                            font-size: 0.75rem;
                            font-weight: bold;
                            padding: 3px 10px;
                            border-radius: 12px;
                            margin-bottom: 12px;
                        }
                        .info-row {
                            display: flex;
                            align-items: flex-start;
                            gap: 8px;
                            margin-bottom: 8px;
                            font-size: 0.9rem;
                            color: #444;
                        }
                        .info-row .icon { font-size: 1rem; min-width: 20px; }
                        .info-row .label { color: #888; font-size: 0.78rem; display: block; }
                        .empty {
                            text-align: center;
                            color: #666;
                            padding: 60px 20px;
                            font-size: 1.1rem;
                        }
                        footer {
                            text-align: center;
                            color: #999;
                            font-size: 0.8rem;
                            margin-top: 40px;
                            padding-bottom: 20px;
                        }
                        @media (max-width: 480px) {
                            header h1 { font-size: 1.4rem; }
                            .grid { grid-template-columns: 1fr; }
                        }
                    </style>
                </head>
                <body>
                """;
    }

    private String buildLocalCard(local_psychiatrie l) {
        String phone = String.valueOf(l.getTelephone_local());
        if (phone.length() == 8)
            phone = phone.substring(0, 2) + " " + phone.substring(2, 5) + " " + phone.substring(5);

        return "<div class='card'>"
                + "<div class='badge'>✓ Disponible</div>"
                + "<h2>" + escapeHtml(l.getNom_local()) + "</h2>"
                + "<div class='info-row'><span class='icon'>📍</span><div>"
                + "<span class='label'>Adresse</span>"
                + escapeHtml(l.getAdresse_local()) + ", " + escapeHtml(l.getVille_local())
                + "</div></div>"
                + "<div class='info-row'><span class='icon'>🏷️</span><div>"
                + "<span class='label'>Type</span>"
                + escapeHtml(l.getType_local().getLibelle())
                + "</div></div>"
                + "<div class='info-row'><span class='icon'>👥</span><div>"
                + "<span class='label'>Capacité</span>"
                + escapeHtml(l.getCapacite_local()) + " personnes"
                + "</div></div>"
                + "<div class='info-row'><span class='icon'>📞</span><div>"
                + "<span class='label'>Téléphone</span>"
                + "<a href='tel:" + l.getTelephone_local() + "'>" + phone + "</a>"
                + "</div></div>"
                + "<div class='info-row'><span class='icon'>✉️</span><div>"
                + "<span class='label'>Email</span>"
                + "<a href='mailto:" + escapeHtml(l.getEmail_local()) + "'>" + escapeHtml(l.getEmail_local()) + "</a>"
                + "</div></div>"
                + (l.getDescription_local() != null && !l.getDescription_local().isEmpty()
                ? "<div class='info-row'><span class='icon'>📝</span><div>"
                + "<span class='label'>Description</span>"
                + escapeHtml(l.getDescription_local()) + "</div></div>"
                : "")
                + "</div>";
    }

    private String buildHtmlFooter() {
        return """
                <footer>Généré par l'application de gestion des locaux psychiatriques</footer>
                </body>
                </html>
                """;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private void sendResponse(HttpExchange exchange, int code, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getLocalIpAddress() {
        try {
            java.net.InetAddress inetAddress = java.net.InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }

    /** Échappe les caractères spéciaux JSON */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /** Échappe les caractères spéciaux HTML */
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
