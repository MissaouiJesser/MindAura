package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class EmailRepService {

    // ── Chargement config ─────────────────────────────────────────────────────
    private static final String[] config    = loadConfig();
    private static final String   FROM_EMAIL = config[0];
    private static final String   PASSWORD   = config[1];
    private static final String   FROM_NAME  = "MindAura";

    private static String[] loadConfig() {
        try {
            Properties props = new Properties();
            InputStream is = EmailRepService.class.getResourceAsStream("/config.properties");
            if (is == null) {
                System.err.println("❌ config.properties NON TROUVÉ!");
                return new String[]{"", ""};
            }
            props.load(is);

            // ✅ Lire les clés spécifiques à EmailRepService
            String email    = props.getProperty("gmail.emailRep", "");
            String password = props.getProperty("gmail.passwordRep", "");

            System.out.println("✅ Config Gmail chargée: " + email);
            return new String[]{email, password};

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement config: " + e.getMessage());
            return new String[]{"", ""};
        }
    }

    // ── Session SMTP ──────────────────────────────────────────────────────────
    private static Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    // ── Envoi générique ───────────────────────────────────────────────────────
    private static boolean envoyerEmail(String toEmail, String subject, String htmlContent) {
        if (FROM_EMAIL.isBlank() || PASSWORD.isBlank()) {
            System.err.println("❌ Config Gmail manquante dans config.properties");
            return false;
        }
        try {
            Session session = creerSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);

            // ✅ Sans MimeMultipart — évite le conflit com.sun.mail / angus-mail
            message.setDataHandler(new jakarta.activation.DataHandler(
                    new jakarta.activation.DataSource() {
                        public java.io.InputStream getInputStream() throws java.io.IOException {
                            return new java.io.ByteArrayInputStream(
                                    htmlContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        }
                        public java.io.OutputStream getOutputStream() throws java.io.IOException {
                            throw new java.io.IOException("Read only");
                        }
                        public String getContentType() { return "text/html; charset=UTF-8"; }
                        public String getName()        { return "email"; }
                    }
            ));

            Transport.send(message);
            System.out.println("✅ Email résultats envoyé à: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ── Méthode principale ────────────────────────────────────────────────────
    public static void envoyerResultatsTest(
            String destinataire,
            String nomPatient,
            String nomTest,
            int scoreTotal,
            List<LigneResultat> lignesReponse) {

        new Thread(() -> {
            String subject = "🎯 Vos résultats — " + nomTest;
            String html    = construireCorpsHTML(nomPatient, nomTest, scoreTotal, lignesReponse);
            envoyerEmail(destinataire, subject, html);
        }).start();
    }

    // ── Corps HTML ────────────────────────────────────────────────────────────
    private static String construireCorpsHTML(
            String nomPatient, String nomTest,
            int scoreTotal, List<LigneResultat> lignes) {

        StringBuilder rows = new StringBuilder();
        int num = 1;
        for (LigneResultat ligne : lignes) {
            String rowBg = (num % 2 == 0) ? "#f9fafb" : "#ffffff";
            rows.append(String.format("""
                <tr style="background-color:%s;">
                    <td style="padding:10px 14px;color:#374151;font-size:14px;
                               border-bottom:1px solid #e5e7eb;">
                        <strong>%d.</strong> %s
                    </td>
                    <td style="padding:10px 14px;color:#2D6A4F;font-size:14px;
                               border-bottom:1px solid #e5e7eb;">%s</td>
                    <td style="padding:10px 14px;text-align:center;font-weight:bold;
                               color:#7B5EA7;border-bottom:1px solid #e5e7eb;">%d pt(s)</td>
                </tr>
                """, rowBg, num++,
                    escapeHtml(ligne.question()),
                    escapeHtml(ligne.reponse() != null ? ligne.reponse() : "—"),
                    ligne.score()));
        }

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"></head>
            <body style="margin:0;padding:0;background-color:#f3f4f6;
                         font-family:'Segoe UI',Arial,sans-serif;">
              <div style="background:linear-gradient(135deg,#7B5EA7,#2D6A4F);
                          padding:36px 40px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:26px;">🧠 MindAura</h1>
                <p style="color:rgba(255,255,255,0.85);margin:8px 0 0;font-size:15px;">
                    Plateforme de bien-être psychologique</p>
              </div>
              <div style="max-width:680px;margin:30px auto;background:white;
                          border-radius:12px;box-shadow:0 4px 20px rgba(0,0,0,0.08);">
                <div style="padding:32px 40px 20px;">
                  <h2 style="color:#1A1A2E;margin:0 0 10px;">Bonjour %s 👋</h2>
                  <p style="color:#5A6475;font-size:15px;line-height:1.6;margin:0;">
                      Vous venez de terminer le test
                      <strong style="color:#7B5EA7;">%s</strong>.</p>
                </div>
                <div style="margin:0 40px 28px;padding:20px 28px;
                            background:linear-gradient(135deg,#f0fdf4,#e0f2fe);
                            border-radius:10px;border-left:4px solid #2D6A4F;">
                  <p style="margin:0;color:#6B7280;font-size:13px;">SCORE TOTAL</p>
                  <p style="margin:4px 0 0;font-size:32px;font-weight:900;color:#2D6A4F;">
                      %d points</p>
                </div>
                <div style="padding:0 40px 32px;">
                  <h3 style="color:#1A1A2E;font-size:16px;margin:0 0 14px;">
                      📋 Détail de vos réponses</h3>
                  <table width="100%%" cellpadding="0" cellspacing="0"
                         style="border-collapse:collapse;border:1px solid #e5e7eb;">
                    <thead>
                      <tr style="background-color:#7B5EA7;">
                        <th style="padding:12px 14px;color:white;text-align:left;">Question</th>
                        <th style="padding:12px 14px;color:white;text-align:left;">Réponse</th>
                        <th style="padding:12px 14px;color:white;text-align:center;">Score</th>
                      </tr>
                    </thead>
                    <tbody>%s</tbody>
                  </table>
                </div>
                <div style="margin:0 40px 32px;padding:18px 22px;background:#fef9ff;
                            border-radius:8px;border:1px dashed #7B5EA7;text-align:center;">
                  <p style="margin:0;color:#7B5EA7;font-size:14px;font-style:italic;">
                      💜 Consultez vos objectifs recommandés dans l'application MindAura !</p>
                </div>
              </div>
              <div style="text-align:center;padding:20px;color:#9CA3AF;font-size:12px;">
                <p>© 2025 MindAura — Email généré automatiquement.</p>
              </div>
            </body></html>
            """.formatted(
                escapeHtml(nomPatient), escapeHtml(nomTest),
                scoreTotal, rows.toString());
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    public record LigneResultat(String question, String reponse, int score) {}
}