package tn.esprit.services;

import tn.esprit.entities.reservation_local;
import tn.esprit.entities.local_psychiatrie;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64;

/**
 * ══════════════════════════════════════════════════════════════════
 *  ReservationPDFService — MindAura
 *  Génération PDF via l'API PDFShift (HTML → PDF haute qualité)
 *  https://pdfshift.io  — clé gratuite : 50 conversions/mois
 * ══════════════════════════════════════════════════════════════════
 *
 *  DÉPENDANCES : aucune librairie tierce, uniquement java.net (JDK)
 *  JSON : construit manuellement (pas besoin de Jackson/Gson)
 *
 *  CONFIGURATION :
 *    - Remplacer API_KEY par votre clé PDFShift (inscription gratuite)
 *    - Ou utiliser wkhtmltopdf en local (voir exportWithWkHtml)
 * ══════════════════════════════════════════════════════════════════
 */
public class ReservationPDFService {

    // ─── Configuration API ────────────────────────────────────────────────────
    private static final String PDFSHIFT_API_URL = "https://api.pdfshift.io/v3/convert/pdf";
    private static final String API_KEY          = "sk_2b51c13f4942b8b64ba6d0389bede07bff2b6482";

    // ─── Formatters ───────────────────────────────────────────────────────────
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy");
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");
    private final SimpleDateFormat tsFmt   = new SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH);

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORT LISTE COMPLÈTE
    // ══════════════════════════════════════════════════════════════════════════

    public void exportReservationsToPDF(List<reservation_local> reservations,
                                        String filePath) throws Exception {
        String html = buildListHtml(reservations);
        callPdfShiftApi(html, filePath, true); // landscape=true pour la liste
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  EXPORT RÉSERVATION UNIQUE
    // ══════════════════════════════════════════════════════════════════════════

    public void exportSingleReservationToPDF(reservation_local reservation,
                                             local_psychiatrie local,
                                             String filePath) throws Exception {
        String html = buildSingleHtml(reservation, local);
        callPdfShiftApi(html, filePath, false);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  APPEL API PDFSHIFT
    // ══════════════════════════════════════════════════════════════════════════

    private void callPdfShiftApi(String html,
                                 String filePath,
                                 boolean landscape) throws Exception {

        // Échapper le HTML pour l'insérer dans un JSON string
        String htmlEscaped = html
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n")
                .replace("\t", "\\t");

        // Corps JSON — sans champs vides (css, sandbox) qui causent l'erreur 400
        String json = "{"
                + "\"source\": \"" + htmlEscaped + "\","
                + "\"landscape\": " + landscape + ","
                + "\"format\": \"A4\","
                + "\"margin\": {\"top\": \"10mm\", \"bottom\": \"10mm\","
                +              "\"left\": \"10mm\", \"right\": \"10mm\"}"
                + "}";

        // Préparer la connexion HTTP
        URL url = new URL(PDFSHIFT_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(60_000);

        // Auth : Basic avec "api" comme username et la clé comme password
        String credentials = Base64.getEncoder()
                .encodeToString(("api:" + API_KEY).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + credentials);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        // Envoi du JSON
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status == 200 || status == 201) {
            // Réception du PDF binaire
            try (InputStream is   = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(filePath)) {
                byte[] buf = new byte[4096];
                int read;
                while ((read = is.read(buf)) != -1) fos.write(buf, 0, read);
            }
            System.out.println("✅ PDF généré via PDFShift API : " + filePath);
        } else {
            // Lire le message d'erreur
            StringBuilder err = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) err.append(line);
            }
            throw new RuntimeException("PDFShift API erreur " + status + " : " + err);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TEMPLATE HTML — LISTE DES RÉSERVATIONS
    //  Design : bandeau hero vert, KPIs, tableau stylisé
    // ══════════════════════════════════════════════════════════════════════════

    private String buildListHtml(List<reservation_local> reservations) {
        long confirmees = reservations.stream()
                .filter(r -> "CONFIRMEE".equals(r.getStatus_reservation().name())).count();
        long enAttente  = reservations.stream()
                .filter(r -> "EN_ATTENTE".equals(r.getStatus_reservation().name())).count();
        long annulees   = reservations.stream()
                .filter(r -> "ANNULEE".equals(r.getStatus_reservation().name())).count();
        int  revenu     = reservations.stream()
                .mapToInt(reservation_local::getPrix_reservation).sum();

        // ── KPI cards ─────────────────────────────────────────────────────────
        String kpiCards = kpiCard("Confirmées", confirmees, "#16a34a", "#dcfce7")
                + kpiCard("En attente",  enAttente,  "#d97706", "#fef9c3")
                + kpiCard("Annulées",    annulees,   "#dc2626", "#fee2e2")
                + kpiCard("Revenu total", revenu + " DT", "#2563eb", "#dbeafe");

        // ── Lignes du tableau ─────────────────────────────────────────────────
        StringBuilder rows = new StringBuilder();
        boolean alt = false;
        for (reservation_local r : reservations) {
            String rowBg = alt ? "#f8fafc" : "#ffffff";
            alt = !alt;

            String statusStyle = statusBadgeStyle(r.getStatus_reservation().name());
            String statusText  = r.getStatus_reservation().getLibelle();

            rows.append("<tr style='background:" + rowBg + ";'>")
                    .append("<td style='padding:11px 14px; font-weight:600; color:#1a1a2e;'>")
                    .append(esc(r.getNom_cl() + " " + r.getPrenom_cl())).append("</td>")
                    .append("<td style='padding:11px 14px; color:#5a6475;'>")
                    .append(dateFmt.format(r.getDate_reservation())).append("</td>")
                    .append("<td style='padding:11px 14px; color:#5a6475; text-align:center;'>")
                    .append(timeFmt.format(r.getHeure_debut_reservation())).append("</td>")
                    .append("<td style='padding:11px 14px; color:#5a6475; text-align:center;'>")
                    .append(timeFmt.format(r.getHeure_fin_reservation())).append("</td>")
                    .append("<td style='padding:11px 14px; text-align:center;'>")
                    .append("<span style='").append(statusStyle).append("'>")
                    .append(statusText).append("</span></td>")
                    .append("<td style='padding:11px 14px; color:#5a6475;'>")
                    .append(esc(r.getMotif_reservation().getLibelle())).append("</td>")
                    .append("<td style='padding:11px 14px; text-align:right; font-weight:700; color:#1b4332;'>")
                    .append(r.getPrix_reservation()).append(" DT</td>")
                    .append("</tr>");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>" + CSS_BASE + CSS_LIST + "</style></head><body>"

                // ── Hero banner ──────────────────────────────────────────────────
                + "<div class='hero'>"
                + "  <div class='hero-left'>"
                + "    <span class='brand'>MindAura</span>"
                + "    <h1>Rapport des Réservations</h1>"
                + "    <p class='hero-sub'>Gestion des locaux psychiatriques</p>"
                + "  </div>"
                + "  <div class='hero-right'>"
                + "    <p class='gen-label'>Généré le</p>"
                + "    <p class='gen-date'>" + tsFmt.format(new Date()) + "</p>"
                + "    <div class='hero-count'>" + reservations.size() + "</div>"
                + "    <p class='gen-label'>réservation(s)</p>"
                + "  </div>"
                + "</div>"

                // ── KPI strip ────────────────────────────────────────────────────
                + "<div class='kpi-strip'>" + kpiCards + "</div>"

                // ── Section titre ────────────────────────────────────────────────
                + "<div class='section-header'>"
                + "  <span class='section-dot'></span>"
                + "  <h2>Liste détaillée des réservations</h2>"
                + "</div>"

                // ── Tableau ──────────────────────────────────────────────────────
                + "<table class='data-table'>"
                + "<thead><tr>"
                + "<th>Client</th><th>Date</th><th>Début</th>"
                + "<th>Fin</th><th>Statut</th><th>Motif</th><th>Prix</th>"
                + "</tr></thead>"
                + "<tbody>" + rows + "</tbody>"
                + "</table>"

                // ── Footer ───────────────────────────────────────────────────────
                + "<div class='footer'>"
                + "  <span class='footer-brand'>MindAura</span>"
                + "  — Ce rapport a été généré automatiquement par le système de gestion des réservations."
                + "</div>"
                + "</body></html>";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  TEMPLATE HTML — RÉSERVATION UNIQUE (confirmation)
    // ══════════════════════════════════════════════════════════════════════════

    private String buildSingleHtml(reservation_local r, local_psychiatrie local) {
        // Durée
        long dureeMin = (r.getHeure_fin_reservation().getTime()
                - r.getHeure_debut_reservation().getTime()) / 60000;
        String duree = (dureeMin / 60) + "h " + (dureeMin % 60) + "min";

        // Couleurs statut
        String[] sColors = statusColors(r.getStatus_reservation().name());
        String statusBg = sColors[0], statusFg = sColors[1], statusLabel = r.getStatus_reservation().getLibelle();

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>" + CSS_BASE + CSS_SINGLE + "</style></head><body>"

                // ── Hero ─────────────────────────────────────────────────────────
                + "<div class='hero'>"
                + "  <div class='hero-left'>"
                + "    <span class='brand'>MindAura</span>"
                + "    <h1>Confirmation de Réservation</h1>"
                + "    <p class='hero-sub'>Document officiel — Conservez ce justificatif</p>"
                + "  </div>"
                + "  <div class='hero-right'>"
                + "    <p class='gen-label'>Émis le</p>"
                + "    <p class='gen-date'>" + tsFmt.format(new Date()) + "</p>"
                + "  </div>"
                + "</div>"

                // ── Bandeau statut ───────────────────────────────────────────────
                + "<div class='status-banner' style='background:" + statusBg + "; border-left:5px solid " + statusFg + ";'>"
                + "  <div class='status-left'>"
                + "    <span class='status-badge' style='background:" + statusFg + ";'>" + statusLabel.toUpperCase() + "</span>"
                + "    <span class='status-client'>"
                + esc(r.getNom_cl() + " " + r.getPrenom_cl())
                + "    </span>"
                + "  </div>"
                + "  <div class='status-price'>" + r.getPrix_reservation() + " <span>DT</span></div>"
                + "</div>"

                // ── Deux colonnes ────────────────────────────────────────────────
                + "<div class='two-col'>"

                // Colonne réservation
                + "  <div class='col-card'>"
                + "    <div class='col-header' style='background:#1b4332;'>Détails de la Réservation</div>"
                + "    <table class='detail-table'>"
                + detailRow("Client",       esc(r.getNom_cl() + " " + r.getPrenom_cl()), false)
                + detailRow("Date",         dateFmt.format(r.getDate_reservation()), true)
                + detailRow("Heure début",  timeFmt.format(r.getHeure_debut_reservation()), false)
                + detailRow("Heure fin",    timeFmt.format(r.getHeure_fin_reservation()), true)
                + detailRow("Durée",        duree, false)
                + detailRow("Motif",        esc(r.getMotif_reservation().getLibelle()), true)
                + detailRow("Statut",       statusLabel, false)
                + "    </table>"
                + "  </div>"

                // Colonne local
                + "  <div class='col-card'>"
                + "    <div class='col-header' style='background:#ff8c00;'>Informations du Local</div>"
                + "    <table class='detail-table'>"
                + detailRow("Nom du local", esc(local.getNom_local()), false)
                + detailRow("Type",         esc(local.getType_local().getLibelle()), true)
                + detailRow("Adresse",      esc(local.getAdresse_local()), false)
                + detailRow("Ville",        esc(local.getVille_local()), true)
                + detailRow("Capacité",     local.getCapacite_local() + " personnes", false)
                + detailRow("Téléphone",    formatPhone(String.valueOf(local.getTelephone_local())), true)
                + detailRow("Email",        esc(local.getEmail_local()), false)
                + "    </table>"
                + "  </div>"
                + "</div>"

                // ── Bloc prix ────────────────────────────────────────────────────
                + "<div class='price-block'>"
                + "  <p class='price-label'>Montant total de la réservation</p>"
                + "  <p class='price-value'>" + r.getPrix_reservation() + " DT</p>"
                + "  <p class='price-note'>80 DT pour la première heure · 20 DT par heure supplémentaire</p>"
                + "</div>"

                // ── Footer ───────────────────────────────────────────────────────
                + "<div class='footer'>"
                + "  <span class='footer-brand'>MindAura</span>"
                + "  — Confirmation officielle émise par le système de gestion des réservations."
                + "</div>"
                + "</body></html>";
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS HTML
    // ══════════════════════════════════════════════════════════════════════════

    private String kpiCard(String label, Object value, String color, String bg) {
        return "<div class='kpi-card' style='background:" + bg
                + "; border-top:4px solid " + color + ";'>"
                + "<p class='kpi-label' style='color:" + color + ";'>" + label + "</p>"
                + "<p class='kpi-value'>" + value + "</p>"
                + "</div>";
    }

    private String detailRow(String label, String value, boolean alt) {
        String bg = alt ? "#f8fafc" : "#ffffff";
        return "<tr style='background:" + bg + ";'>"
                + "<td class='dl'>" + label + "</td>"
                + "<td class='dv'>" + value + "</td>"
                + "</tr>";
    }

    private String statusBadgeStyle(String name) {
        String[] colors = statusColors(name);
        return "background:" + colors[0] + "; color:" + colors[1]
                + "; padding:3px 10px; border-radius:12px; font-size:11px;"
                + " font-weight:700; border:1px solid " + colors[1] + ";";
    }

    private String[] statusColors(String name) {
        switch (name) {
            case "CONFIRMEE": return new String[]{"#dcfce7", "#16a34a"};
            case "ANNULEE":   return new String[]{"#fee2e2", "#dc2626"};
            default:          return new String[]{"#fef9c3", "#d97706"};
        }
    }

    private String formatPhone(String phone) {
        if (phone != null && phone.length() == 8)
            return phone.substring(0, 2) + " " + phone.substring(2, 5) + " " + phone.substring(5);
        return phone != null ? phone : "—";
    }

    /** Échappe les caractères HTML spéciaux */
    private String esc(String s) {
        if (s == null) return "—";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  CSS — BASE (commun aux deux templates)
    // ══════════════════════════════════════════════════════════════════════════

    private static final String CSS_BASE =
            "* { margin:0; padding:0; box-sizing:border-box; }"
                    + "body { font-family: 'Segoe UI', Arial, sans-serif; background:#f0f4f8;"
                    + "       color:#1a1a2e; font-size:13px; }"

                    // Hero banner
                    + ".hero { display:flex; background:linear-gradient(135deg,#1b4332 0%,#2d6a4f 60%,#0d2b1f 100%);"
                    + "        padding:32px 40px; gap:30px; }"
                    + ".hero-left { flex:1; }"
                    + ".brand { font-size:22px; font-weight:900; color:#ff8c00; letter-spacing:1px; }"
                    + ".hero h1 { font-size:18px; font-weight:700; color:#fff; margin-top:6px; }"
                    + ".hero-sub { font-size:11px; color:#b7e4c7; margin-top:4px; }"
                    + ".hero-right { text-align:right; }"
                    + ".gen-label { font-size:10px; color:#86efac; text-transform:uppercase; letter-spacing:1px; }"
                    + ".gen-date { font-size:12px; color:#fff; font-weight:600; margin-top:2px; }"
                    + ".hero-count { font-size:40px; font-weight:900; color:#ff8c00; line-height:1; margin-top:8px; }"

                    // Footer
                    + ".footer { padding:16px 40px; background:#1b4332; color:#86efac; font-size:10px;"
                    + "          margin-top:30px; }"
                    + ".footer-brand { font-weight:900; color:#ff8c00; }"

                    // Section header
                    + ".section-header { display:flex; align-items:center; gap:10px;"
                    + "                  padding:18px 40px 8px; }"
                    + ".section-dot { width:6px; height:6px; border-radius:50%; background:#ff8c00;"
                    + "               display:inline-block; }"
                    + ".section-header h2 { font-size:14px; font-weight:700; color:#1b4332; }";

    // ══════════════════════════════════════════════════════════════════════════
    //  CSS — LISTE
    // ══════════════════════════════════════════════════════════════════════════

    private static final String CSS_LIST =
            // KPI strip
            ".kpi-strip { display:flex; gap:0; padding:20px 40px; background:#fff;"
                    + "             border-bottom:2px solid #e2e8f0; }"
                    + ".kpi-card { flex:1; padding:16px 20px; border-right:1px solid #e2e8f0; }"
                    + ".kpi-card:last-child { border-right:none; }"
                    + ".kpi-label { font-size:10px; font-weight:800; text-transform:uppercase; letter-spacing:1px; }"
                    + ".kpi-value { font-size:26px; font-weight:900; color:#1a1a2e; margin-top:4px; }"

                    // Tableau
                    + ".data-table { width:calc(100% - 80px); margin:12px 40px 0; border-collapse:collapse;"
                    + "              border-radius:12px; overflow:hidden;"
                    + "              box-shadow:0 1px 8px rgba(0,0,0,0.07); }"
                    + ".data-table thead tr { background:linear-gradient(to right,#1b4332,#2d6a4f); }"
                    + ".data-table th { padding:12px 14px; color:#fff; font-size:11px; font-weight:700;"
                    + "                 text-transform:uppercase; letter-spacing:0.5px; text-align:left; }"
                    + ".data-table th:nth-child(3), .data-table th:nth-child(4),"
                    + ".data-table th:nth-child(5) { text-align:center; }"
                    + ".data-table th:last-child { text-align:right; }"
                    + ".data-table td { border-bottom:1px solid #e2e8f0; font-size:12px; }"
                    + ".data-table tbody tr:last-child td { border-bottom:none; }"
                    + ".data-table tbody tr:hover { background:#f0fdf4 !important; }";

    // ══════════════════════════════════════════════════════════════════════════
    //  CSS — RÉSERVATION UNIQUE
    // ══════════════════════════════════════════════════════════════════════════

    private static final String CSS_SINGLE =
            // Bandeau statut
            ".status-banner { display:flex; justify-content:space-between; align-items:center;"
                    + "                 margin:20px 40px; padding:16px 24px; border-radius:12px; }"
                    + ".status-left { display:flex; align-items:center; gap:14px; }"
                    + ".status-badge { color:#fff; padding:5px 14px; border-radius:20px; font-size:11px;"
                    + "                font-weight:800; letter-spacing:1px; }"
                    + ".status-client { font-size:16px; font-weight:700; color:#1a1a2e; }"
                    + ".status-price { font-size:32px; font-weight:900; color:#1b4332; }"
                    + ".status-price span { font-size:16px; }"

                    // Deux colonnes
                    + ".two-col { display:flex; gap:20px; padding:0 40px; margin-top:10px; }"
                    + ".col-card { flex:1; border-radius:10px; overflow:hidden;"
                    + "            box-shadow:0 2px 8px rgba(0,0,0,0.08); background:#fff; }"
                    + ".col-header { padding:13px 18px; color:#fff; font-size:12px; font-weight:700;"
                    + "              text-transform:uppercase; letter-spacing:0.5px; }"
                    + ".detail-table { width:100%; border-collapse:collapse; }"
                    + ".detail-table .dl { padding:10px 16px; font-size:11px; font-weight:700;"
                    + "                    color:#6b7280; width:38%; border-bottom:1px solid #e2e8f0; }"
                    + ".detail-table .dv { padding:10px 16px; font-size:12px; color:#1a1a2e;"
                    + "                    border-bottom:1px solid #e2e8f0; }"

                    // Prix
                    + ".price-block { background:linear-gradient(135deg,#1b4332,#2d6a4f);"
                    + "               margin:24px 40px 0; padding:28px; border-radius:14px; text-align:center; }"
                    + ".price-label { color:#86efac; font-size:12px; margin-bottom:8px; }"
                    + ".price-value { font-size:48px; font-weight:900; color:#ff8c00; line-height:1; }"
                    + ".price-note { color:rgba(183,228,199,0.75); font-size:11px; margin-top:10px; font-style:italic; }";
}