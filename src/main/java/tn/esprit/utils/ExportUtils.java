package tn.esprit.utils;

import tn.esprit.entities.traitement;
import tn.esprit.entities.utilisateurs;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utilitaire d'export XLSX stylisé et PDF (pur Java).
 * XLSX généré via Apache POI avec mise en forme professionnelle.
 *
 * Dépendance Maven à ajouter dans pom.xml :
 * <dependency>
 *     <groupId>org.apache.poi</groupId>
 *     <artifactId>poi-ooxml</artifactId>
 *     <version>5.2.5</version>
 * </dependency>
 */
public class ExportUtils {

    private static final SimpleDateFormat SDF       = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final SimpleDateFormat SDF_DATE  = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat SDF_FILE  = new SimpleDateFormat("yyyyMMdd_HHmmss");

    // ── Palette MindAura ────────────────────────────────────────────────────
    private static final String COLOR_DARK_GREEN  = "1B4332";   // en-tête colonnes
    private static final String COLOR_MED_GREEN   = "2D6A4F";   // titre rapport
    private static final String COLOR_LIGHT_GREEN = "D1FAE5";   // lignes paires / badge actif
    private static final String COLOR_HEADER_TEXT = "FFFFFF";
    private static final String COLOR_BORDER      = "B7E4C7";
    private static final String COLOR_ROW_ODD     = "FFFFFF";
    private static final String COLOR_ROW_EVEN    = "F0FFF4";
    private static final String COLOR_INACTIVE_BG = "FEE2E2";
    private static final String COLOR_INACTIVE_FG = "991B1B";
    private static final String COLOR_ROLE_BG     = "EDE9FE";
    private static final String COLOR_ROLE_FG     = "6D28D9";

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT XLSX - UTILISATEURS
    // ─────────────────────────────────────────────────────────────────────────

    public static File exportUsersCSV(List<utilisateurs> users, File dest) throws IOException {
        // On génère un vrai .xlsx stylisé (le nom de méthode conservé pour compatibilité)
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Utilisateurs");
            sheet.setDefaultColumnWidth(18);

            // ── Styles ──────────────────────────────────────────────────────
            CellStyle titleStyle   = makeTitleStyle(wb);
            CellStyle metaStyle    = makeMetaStyle(wb);
            CellStyle headerStyle  = makeHeaderStyle(wb, COLOR_DARK_GREEN);
            CellStyle oddStyle     = makeDataStyle(wb, COLOR_ROW_ODD,     false);
            CellStyle evenStyle    = makeDataStyle(wb, COLOR_ROW_EVEN,    false);
            CellStyle actifStyle   = makeBadgeStyle(wb, COLOR_LIGHT_GREEN, "065F46");
            CellStyle inactifStyle = makeBadgeStyle(wb, COLOR_INACTIVE_BG, COLOR_INACTIVE_FG);
            CellStyle roleStyle    = makeBadgeStyle(wb, COLOR_ROLE_BG,     COLOR_ROLE_FG);

            int r = 0;

            // ── Ligne titre ─────────────────────────────────────────────────
            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(32);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("🧠 MindAura — Rapport des Utilisateurs");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

            // ── Méta ─────────────────────────────────────────────────────────
            Row metaRow = sheet.createRow(r++);
            metaRow.setHeightInPoints(18);
            Cell metaCell = metaRow.createCell(0);
            metaCell.setCellValue("Généré le : " + SDF.format(new Date()) + "   |   Nombre total : " + users.size());
            metaCell.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 6));

            r++; // ligne vide

            // ── En-têtes colonnes ────────────────────────────────────────────
            String[] headers = {"Prénom", "Nom", "Email", "Téléphone", "Rôle", "Statut", "Date Inscription"};
            int[] widths      = {18, 18, 30, 16, 16, 12, 18};
            Row hRow = sheet.createRow(r++);
            hRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            // ── Données ──────────────────────────────────────────────────────
            for (int i = 0; i < users.size(); i++) {
                utilisateurs u = users.get(i);
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);
                CellStyle base = (i % 2 == 0) ? oddStyle : evenStyle;

                setCell(row, 0, safe(u.getPrenom_utilisateur()), base);
                setCell(row, 1, safe(u.getNom_utilisateur()),    base);
                setCell(row, 2, safe(u.getEmail_utilisateur()),  base);
                setCell(row, 3, safe(u.getTelephone_utilisateur()), base);

                // Rôle — badge violet
                Cell roleCell = row.createCell(4);
                roleCell.setCellValue(u.getRole_utilisateur() != null ? u.getRole_utilisateur().getLibelle() : "");
                roleCell.setCellStyle(roleStyle);

                // Statut — badge coloré
                boolean actif = u.isEst_actif_utilisateur();
                Cell statCell = row.createCell(5);
                statCell.setCellValue(actif ? "✔ Actif" : "✘ Inactif");
                statCell.setCellStyle(actif ? actifStyle : inactifStyle);

                // Date inscription
                String dateStr = u.getDate_inscription_utilisateur() != null
                        ? SDF_DATE.format(u.getDate_inscription_utilisateur()) : "";
                setCell(row, 6, dateStr, base);
            }

            // ── Ligne de total ───────────────────────────────────────────────
            r++;
            Row totalRow = sheet.createRow(r);
            totalRow.setHeightInPoints(20);
            CellStyle totalStyle = makeTotalStyle(wb);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Total utilisateurs : " + users.size());
            totalLabel.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 6));

            // ── Filtre automatique ───────────────────────────────────────────
            sheet.setAutoFilter(new CellRangeAddress(3, 3 + users.size(), 0, 6));

            // ── Figer la ligne d'en-tête ─────────────────────────────────────
            sheet.createFreezePane(0, 4);

            try (FileOutputStream fos = new FileOutputStream(dest)) {
                wb.write(fos);
            }
        }
        return dest;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT XLSX - TRAITEMENTS
    // ─────────────────────────────────────────────────────────────────────────

    public static File exportTraitementsCSV(List<traitement> traitements, File dest) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Traitements");

            CellStyle titleStyle   = makeTitleStyle(wb);
            CellStyle metaStyle    = makeMetaStyle(wb);
            CellStyle headerStyle  = makeHeaderStyle(wb, COLOR_DARK_GREEN);
            CellStyle oddStyle     = makeDataStyle(wb, COLOR_ROW_ODD,  false);
            CellStyle evenStyle    = makeDataStyle(wb, COLOR_ROW_EVEN, false);
            CellStyle enCoursStyle = makeBadgeStyle(wb, "DBEAFE", "1E40AF");
            CellStyle termineStyle = makeBadgeStyle(wb, COLOR_LIGHT_GREEN, "065F46");
            CellStyle suspStyle    = makeBadgeStyle(wb, "FEF9C3", "854D0E");

            int r = 0;

            Row titleRow = sheet.createRow(r++);
            titleRow.setHeightInPoints(32);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("🧠 MindAura — Rapport des Traitements");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row metaRow = sheet.createRow(r++);
            metaRow.setHeightInPoints(18);
            Cell metaCell = metaRow.createCell(0);
            metaCell.setCellValue("Généré le : " + SDF.format(new Date()) + "   |   Nombre total : " + traitements.size());
            metaCell.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            r++;

            String[] headers = {"Type", "Objectif", "État", "Date Début", "Date Fin", "Description"};
            int[] widths      = {20, 20, 16, 16, 16, 40};
            Row hRow = sheet.createRow(r++);
            hRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            for (int i = 0; i < traitements.size(); i++) {
                traitement t = traitements.get(i);
                Row row = sheet.createRow(r++);
                row.setHeightInPoints(20);
                CellStyle base = (i % 2 == 0) ? oddStyle : evenStyle;

                setCell(row, 0, t.getType_traitement()     != null ? t.getType_traitement().getLibelle()     : "", base);
                setCell(row, 1, t.getObjectif_traitement() != null ? t.getObjectif_traitement().getLibelle() : "", base);

                // État — badge coloré
                String etat = t.getEtat_traitement() != null ? t.getEtat_traitement().getLibelle() : "";
                Cell etatCell = row.createCell(2);
                etatCell.setCellValue(etat);
                if (t.getEtat_traitement() != null) {
                    etatCell.setCellStyle(switch (t.getEtat_traitement()) {
                        case EN_COURS  -> enCoursStyle;
                        case TERMINE   -> termineStyle;
                        case SUSPENDU  -> suspStyle;
                    });
                } else {
                    etatCell.setCellStyle(base);
                }

                setCell(row, 3, t.getDate_debut_traitement() != null ? SDF_DATE.format(t.getDate_debut_traitement()) : "", base);
                setCell(row, 4, t.getDate_fin_traitement()   != null ? SDF_DATE.format(t.getDate_fin_traitement())   : "–", base);
                setCell(row, 5, safe(t.getDescription_traitement()), base);
            }

            r++;
            Row totalRow = sheet.createRow(r);
            totalRow.setHeightInPoints(20);
            CellStyle totalStyle = makeTotalStyle(wb);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Total traitements : " + traitements.size());
            totalLabel.setCellStyle(totalStyle);
            sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 5));

            sheet.setAutoFilter(new CellRangeAddress(3, 3 + traitements.size(), 0, 5));
            sheet.createFreezePane(0, 4);

            try (FileOutputStream fos = new FileOutputStream(dest)) {
                wb.write(fos);
            }
        }
        return dest;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EXPORT PDF (HTML auto-imprimable, inchangé)
    // ─────────────────────────────────────────────────────────────────────────

    public static File exportUsersPDF(List<utilisateurs> users, File dest) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>")
                .append("<title>Rapport Utilisateurs MindAura</title><style>")
                .append(getPdfStyles())
                .append("</style></head><body>")
                .append("<div class='header'><h1>&#129504; MindAura</h1>")
                .append("<h2>Rapport des Utilisateurs</h2>")
                .append("<p class='date'>Généré le : ").append(SDF.format(new Date())).append("</p>")
                .append("<p class='count'>Nombre total : <strong>").append(users.size()).append("</strong></p></div>")
                .append("<table><thead><tr>")
                .append("<th>Photo</th><th>Prénom</th><th>Nom</th><th>Email</th>")
                .append("<th>Téléphone</th><th>Rôle</th><th>Statut</th></tr></thead><tbody>");

        for (utilisateurs u : users) {
            String initials   = getInitials(u.getPrenom_utilisateur(), u.getNom_utilisateur());
            String statusClass = u.isEst_actif_utilisateur() ? "badge-actif" : "badge-inactif";
            String statusTxt   = u.isEst_actif_utilisateur() ? "Actif" : "Inactif";
            sb.append("<tr>")
                    .append("<td><div class='avatar'>").append(initials).append("</div></td>")
                    .append("<td>").append(safe(u.getPrenom_utilisateur())).append("</td>")
                    .append("<td>").append(safe(u.getNom_utilisateur())).append("</td>")
                    .append("<td>").append(safe(u.getEmail_utilisateur())).append("</td>")
                    .append("<td>").append(safe(u.getTelephone_utilisateur())).append("</td>")
                    .append("<td><span class='role'>").append(u.getRole_utilisateur() != null ?
                            u.getRole_utilisateur().getLibelle() : "").append("</span></td>")
                    .append("<td><span class='badge ").append(statusClass).append("'>")
                    .append(statusTxt).append("</span></td>")
                    .append("</tr>");
        }

        sb.append("</tbody></table>")
                .append("<div class='footer'>MindAura — Psychologie &amp; Développement Personnel</div>")
                .append("<script>window.onload=()=>window.print();</script>")
                .append("</body></html>");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(dest), StandardCharsets.UTF_8))) {
            pw.print(sb);
        }
        return dest;
    }

    public static File exportTraitementsPDF(List<traitement> traitements, File dest) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'>")
                .append("<title>Rapport Traitements MindAura</title><style>")
                .append(getPdfStyles())
                .append("</style></head><body>")
                .append("<div class='header'><h1>&#129504; MindAura</h1>")
                .append("<h2>Rapport des Traitements</h2>")
                .append("<p class='date'>Généré le : ").append(SDF.format(new Date())).append("</p>")
                .append("<p class='count'>Nombre total : <strong>").append(traitements.size()).append("</strong></p></div>")
                .append("<table><thead><tr>")
                .append("<th>Type</th><th>Objectif</th><th>État</th><th>Début</th><th>Fin</th><th>Description</th>")
                .append("</tr></thead><tbody>");

        for (traitement t : traitements) {
            String etat = t.getEtat_traitement() != null ? t.getEtat_traitement().getLibelle() : "";
            String etatClass = t.getEtat_traitement() != null ? switch (t.getEtat_traitement()) {
                case EN_COURS -> "badge-encours"; case TERMINE -> "badge-termine"; case SUSPENDU -> "badge-suspendu";
            } : "";
            sb.append("<tr>")
                    .append("<td>").append(t.getType_traitement() != null ? t.getType_traitement().getLibelle() : "").append("</td>")
                    .append("<td>").append(t.getObjectif_traitement() != null ? t.getObjectif_traitement().getLibelle() : "").append("</td>")
                    .append("<td><span class='badge ").append(etatClass).append("'>").append(etat).append("</span></td>")
                    .append("<td>").append(t.getDate_debut_traitement() != null ? SDF_DATE.format(t.getDate_debut_traitement()) : "").append("</td>")
                    .append("<td>").append(t.getDate_fin_traitement() != null ? SDF_DATE.format(t.getDate_fin_traitement()) : "–").append("</td>")
                    .append("<td>").append(safe(t.getDescription_traitement())).append("</td>")
                    .append("</tr>");
        }

        sb.append("</tbody></table>")
                .append("<div class='footer'>MindAura — Psychologie &amp; Développement Personnel</div>")
                .append("<script>window.onload=()=>window.print();</script>")
                .append("</body></html>");

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(dest), StandardCharsets.UTF_8))) {
            pw.print(sb);
        }
        return dest;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS — STYLES POI
    // ─────────────────────────────────────────────────────────────────────────

    private static CellStyle makeTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(COLOR_MED_GREEN), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 16);
        f.setColor(new XSSFColor(hexToBytes(COLOR_HEADER_TEXT), null));
        s.setFont(f);
        return s;
    }

    private static CellStyle makeMetaStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes("95C9B4"), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setItalic(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(hexToBytes("1A3A2A"), null));
        s.setFont(f);
        return s;
    }

    private static CellStyle makeHeaderStyle(XSSFWorkbook wb, String bgHex) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(bgHex), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, COLOR_BORDER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(new XSSFColor(hexToBytes(COLOR_HEADER_TEXT), null));
        s.setFont(f);
        return s;
    }

    private static CellStyle makeDataStyle(XSSFWorkbook wb, String bgHex, boolean bold) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(bgHex), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.LEFT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, COLOR_BORDER);
        XSSFFont f = wb.createFont();
        f.setBold(bold);
        f.setFontHeightInPoints((short) 10);
        s.setFont(f);
        return s;
    }

    private static CellStyle makeBadgeStyle(XSSFWorkbook wb, String bgHex, String fgHex) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(bgHex), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(s, COLOR_BORDER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 10);
        f.setColor(new XSSFColor(hexToBytes(fgHex), null));
        s.setFont(f);
        return s;
    }

    private static CellStyle makeTotalStyle(XSSFWorkbook wb) {
        XSSFCellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(hexToBytes(COLOR_DARK_GREEN), null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.RIGHT);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 11);
        f.setColor(new XSSFColor(hexToBytes(COLOR_HEADER_TEXT), null));
        s.setFont(f);
        return s;
    }

    private static void setBorder(XSSFCellStyle s, String colorHex) {
        XSSFColor c = new XSSFColor(hexToBytes(colorHex), null);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setBottomBorderColor(c);
        s.setTopBorderColor(c);
        s.setLeftBorderColor(c);
        s.setRightBorderColor(c);
    }

    private static void setCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(style);
    }

    private static byte[] hexToBytes(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS — DIVERS
    // ─────────────────────────────────────────────────────────────────────────

    public static String getTimestampedName(String prefix) {
        return prefix + "_" + SDF_FILE.format(new Date());
    }

    private static String safe(String s) { return s != null ? s.replace("\"", "'") : ""; }

    private static String getInitials(String prenom, String nom) {
        String p = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)).toUpperCase() : "";
        String n = (nom    != null && !nom.isEmpty())    ? String.valueOf(nom.charAt(0)).toUpperCase()    : "";
        return p + n;
    }

    private static String getPdfStyles() {
        return "body{font-family:'Segoe UI',Arial,sans-serif;margin:0;padding:20px;background:#F0F4F8;color:#1A1A2E;}"
                + ".header{background:linear-gradient(135deg,#1B4332,#2D6A4F);color:#fff;padding:30px 36px;border-radius:12px;margin-bottom:24px;}"
                + ".header h1{margin:0 0 4px;font-size:28px;} .header h2{margin:0 0 12px;font-weight:400;opacity:.85;}"
                + ".date{margin:0;font-size:12px;opacity:.7;} .count{margin:4px 0 0;font-size:14px;}"
                + "table{width:100%;border-collapse:collapse;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 2px 16px rgba(0,0,0,.07);}"
                + "th{background:#1B4332;color:#fff;padding:12px 14px;text-align:left;font-size:12px;font-weight:600;}"
                + "td{padding:11px 14px;border-bottom:1px solid #EDF1F5;font-size:13px;vertical-align:middle;}"
                + "tr:hover td{background:#F0FFF4;}"
                + ".avatar{width:32px;height:32px;border-radius:50%;background:linear-gradient(135deg,#2D6A4F,#1B4332);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:bold;font-size:13px;margin:auto;}"
                + ".badge{padding:3px 10px;border-radius:20px;font-size:11px;font-weight:600;}"
                + ".badge-actif{background:#d1fae5;color:#065f46;} .badge-inactif{background:#fee2e2;color:#991b1b;}"
                + ".badge-encours{background:#dbeafe;color:#1e40af;} .badge-termine{background:#d1fae5;color:#065f46;} .badge-suspendu{background:#fef9c3;color:#854d0e;}"
                + ".role{background:#ede9fe;color:#6d28d9;padding:2px 8px;border-radius:20px;font-size:11px;font-weight:600;}"
                + ".footer{text-align:center;margin-top:24px;font-size:11px;color:#95C9B4;padding:16px;}"
                + "@media print{body{background:#fff;} .header{-webkit-print-color-adjust:exact;}}";
    }
}