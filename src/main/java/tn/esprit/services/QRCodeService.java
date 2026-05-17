package tn.esprit.services;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class QRCodeService {

    private static final int QR_SIZE  = 250;
    private static final int TICKET_W = 600;
    private static final int TICKET_H = 340;

    // ─────────────────────────────────────────────────────────────────────────
    //  CONTENU QR
    // ─────────────────────────────────────────────────────────────────────────

    public String buildQrContent(Participation p, Evenements e) {
        return "EVENTURA_TICKET"
                + "|ID:"     + p.getId()
                + "|NOM:"    + p.getNom()
                + "|EVENT:"  + e.getTitreEvenement()
                + "|DATE:"   + (p.getDateInscription() != null ? p.getDateInscription() : "?")
                + "|LIEU:"   + (e.getLieu_evenement()  != null ? e.getLieu_evenement()  : "?")
                + "|STATUT:" + p.getStatut();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GÉNÉRATION QR CODE BRUT
    // ─────────────────────────────────────────────────────────────────────────

    public BufferedImage genererQRCode(String content) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

        // Couleurs : modules bleus sur fond sombre
        MatrixToImageConfig config = new MatrixToImageConfig(0xFF4F6EF7, 0xFF0D1526);
        return MatrixToImageWriter.toBufferedImage(matrix, config);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GÉNÉRATION TICKET COMPLET
    // ─────────────────────────────────────────────────────────────────────────

    public BufferedImage genererTicket(Participation p, Evenements e) throws WriterException {
        // 1. QR code intégré dans le ticket
        String content  = buildQrContent(p, e);
        BufferedImage qr = genererQRCode(content);
        BufferedImage qrScaled = scaleImage(qr, 180, 180);

        // 2. Canvas du ticket
        BufferedImage ticket = new BufferedImage(TICKET_W, TICKET_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ticket.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // ── Fond dégradé sombre ──
        GradientPaint bg = new GradientPaint(0, 0, new Color(0x0D1526),
                TICKET_W, TICKET_H, new Color(0x111827));
        g.setPaint(bg);
        g.fillRoundRect(0, 0, TICKET_W, TICKET_H, 20, 20);

        // ── Barre d'accentuation gauche (bleu → violet) ──
        GradientPaint accent = new GradientPaint(0, 0, new Color(0x4F6EF7),
                0, TICKET_H, new Color(0x8B5CF6));
        g.setPaint(accent);
        g.fillRoundRect(0, 0, 6, TICKET_H, 3, 3);

        // ── Bordure extérieure ──
        g.setColor(new Color(0x1E2D45));
        g.setStroke(new BasicStroke(1));
        g.drawRoundRect(0, 0, TICKET_W - 1, TICKET_H - 1, 20, 20);

        // ── Séparateur en pointillés avant la zone QR ──
        g.setColor(new Color(0x1E2D45));
        float[] dash = {6, 4};
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, dash, 0));
        g.drawLine(TICKET_W - 220, 20, TICKET_W - 220, TICKET_H - 20);
        g.setStroke(new BasicStroke(1));

        int lx = 26; // marge gauche texte

        // ── Logo / Marque ──
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.setColor(new Color(0x4F6EF7));
        g.drawString("⚡ EVENTURA", lx, 38);

        // ── Ligne de séparation sous le logo ──
        g.setColor(new Color(0x1E2D45));
        g.fillRoundRect(lx, 46, 170, 1, 1, 1);

        // ── Titre de l'événement ──
        g.setFont(new Font("Segoe UI", Font.BOLD, 17));
        g.setColor(new Color(0xF1F5F9));
        drawStringWrapped(g, e.getTitreEvenement(), lx, 72, 340, 17);

        // ── Participant ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.setColor(new Color(0x8899AA));
        g.drawString("Participant", lx, 112);
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.setColor(new Color(0xF1F5F9));
        g.drawString(p.getNom(), lx, 128);

        // ── Date d'inscription ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.setColor(new Color(0x8899AA));
        g.drawString("Date d'inscription", lx, 158);
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g.setColor(new Color(0xF1F5F9));
        g.drawString(p.getDateInscription() != null ? p.getDateInscription().toString() : "—", lx, 173);

        // ── Lieu ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.setColor(new Color(0x8899AA));
        g.drawString("Lieu", lx, 203);
        g.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g.setColor(new Color(0xF1F5F9));
        g.drawString(e.getLieu_evenement() != null ? e.getLieu_evenement() : "—", lx, 218);

        // ── Période ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.setColor(new Color(0x8899AA));
        g.drawString("Période", lx, 248);
        g.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g.setColor(new Color(0xCBD5E1));
        String debut  = e.getDatedebutEvenemnt() != null ? e.getDatedebutEvenemnt().toString() : "?";
        String fin    = e.getDatefinEvenemnt()   != null ? e.getDatefinEvenemnt().toString()   : "?";
        g.drawString(debut + " → " + fin, lx, 263);

        // ── Badge statut ──
        String statut    = p.getStatut() != null ? p.getStatut() : "En attente";
        Color statusBg   = new Color(0x10B981, true);
        Color statusText = new Color(0x34D399);
        if (statut.toLowerCase().contains("attente")) {
            statusBg   = new Color(0xF59E0B & 0xFFFFFF | (0x22 << 24), true);
            statusText = new Color(0xFCD34D);
        }
        g.setColor(statusBg);
        g.fillRoundRect(lx, 282, 120, 26, 13, 13);
        g.setFont(new Font("Segoe UI", Font.BOLD, 11));
        g.setColor(statusText);
        g.drawString("● " + statut, lx + 12, 300);

        // ── Numéro de ticket ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g.setColor(new Color(0x4B607A));
        g.drawString("Ticket #" + p.getId(), lx, 325);

        // ── Image QR code ──
        int qrX = TICKET_W - 210;
        int qrY = (TICKET_H - 180) / 2;
        g.drawImage(qrScaled, qrX, qrY, null);

        // ── Label sous le QR ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g.setColor(new Color(0x4B607A));
        String scanLabel = "Scanner pour vérifier";
        FontMetrics fm   = g.getFontMetrics();
        int labelX = qrX + (180 - fm.stringWidth(scanLabel)) / 2;
        g.drawString(scanLabel, labelX, qrY + 180 + 14);

        g.dispose();
        return ticket;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAUVEGARDE
    // ─────────────────────────────────────────────────────────────────────────

    public void sauvegarderTicket(BufferedImage ticket, String outPath) throws IOException {
        File out = new File(outPath);
        out.getParentFile().mkdirs();
        ImageIO.write(ticket, "PNG", out);
        System.out.println("[QRCodeService] Ticket sauvegardé → " + outPath);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONVERSION JavaFX
    // ─────────────────────────────────────────────────────────────────────────

    public Image toFxImage(BufferedImage img) {
        return SwingFXUtils.toFXImage(img, null);
    }

    public Image genererTicketFx(Participation p, Evenements e) throws WriterException {
        return toFxImage(genererTicket(p, e));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITAIRES PRIVÉS
    // ─────────────────────────────────────────────────────────────────────────

    private BufferedImage scaleImage(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private void drawStringWrapped(Graphics2D g, String text, int x, int y, int maxWidth, int lineH) {
        if (text == null) return;
        FontMetrics fm    = g.getFontMetrics();
        String[]    words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int currentY = y;
        for (String word : words) {
            String test = line + (line.isEmpty() ? "" : " ") + word;
            if (fm.stringWidth(test) > maxWidth && !line.isEmpty()) {
                g.drawString(line.toString(), x, currentY);
                line     = new StringBuilder(word);
                currentY += lineH + 2;
            } else {
                if (!line.isEmpty()) line.append(" ");
                line.append(word);
            }
        }
        if (!line.isEmpty()) g.drawString(line.toString(), x, currentY);
    }
}