package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Service d'envoi d'e-mails (confirmation + rappel) via SMTP Gmail.
 *
 * ⚠️  CONFIGURATION REQUISE :
 *   1. Remplacez SENDER_EMAIL par votre adresse Gmail.
 *   2. Remplacez SENDER_PASSWORD par un "mot de passe d'application" Gmail
 *      (Compte Google → Sécurité → Mots de passe des applications).
 *   3. Dépendance pom.xml :
 *        <dependency>
 *            <groupId>com.sun.mail</groupId>
 *            <artifactId>jakarta.mail</artifactId>
 *            <version>2.0.1</version>
 *        </dependency>
 */
public class EmailService {

    private static final String SMTP_HOST       = "smtp.gmail.com";
    private static final int    SMTP_PORT       = 587;
    private static final String SENDER_EMAIL    = "votre.email@gmail.com";  // ← à changer
    private static final String SENDER_PASSWORD = "xxxx xxxx xxxx xxxx";    // ← mot de passe d'application Gmail

    private static final DateTimeFormatter FMT_DATE  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    //  CORRECTION #1 : EmailResult est maintenant une classe avec champ succes
    //  (avant c'était un enum sans ce champ, ce qui causait une erreur de compilation
    //   dans EmailController qui utilisait r.succes)
    // ─────────────────────────────────────────────────────────────────────────
    public static class EmailResult {
        public final boolean succes;
        private final String message;

        private EmailResult(boolean succes, String message) {
            this.succes  = succes;
            this.message = message;
        }

        public static final EmailResult SUCCESS       = new EmailResult(true,  "E-mail envoyé avec succès.");
        public static final EmailResult MISSING_EMAIL = new EmailResult(false, "Adresse e-mail manquante.");
        public static final EmailResult SEND_ERROR    = new EmailResult(false, "Erreur lors de l'envoi.");

        @Override
        public String toString() {
            return message;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CORRECTION #2 : extraction de la config SMTP pour éviter la duplication
    // ─────────────────────────────────────────────────────────────────────────
    private static Properties buildSmtpProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);
        return props;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Méthode 1 — Confirmation de réservation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie un e-mail HTML de confirmation de réservation.
     *
     * @param destinataire  adresse e-mail du destinataire
     * @param nomComplet    prénom + nom de l'utilisateur
     * @param nomLocal      nom du local OU de la salle réservée
     * @param dateSeance    date + heure de début (LocalDateTime)
     * @param motif         libellé du motif de réservation
     * @param lieu          adresse complète du lieu
     * @return              EmailResult.SUCCESS | MISSING_EMAIL | SEND_ERROR
     */
    public static EmailResult envoyerConfirmation(
            String        destinataire,
            String        nomComplet,
            String        nomLocal,
            LocalDateTime dateSeance,
            String        motif,
            String        lieu
    ) {
        if (destinataire == null || destinataire.isBlank()) {
            System.err.println("[EmailService] Adresse e-mail manquante.");
            return EmailResult.MISSING_EMAIL;
        }

        Session session = Session.getInstance(buildSmtpProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SENDER_EMAIL, "MindAura – Réservations"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            msg.setSubject("✅ Confirmation de votre réservation – " + nomLocal);
            msg.setContent(
                    buildHtmlConfirmation(nomComplet, nomLocal, dateSeance, motif, lieu),
                    "text/html; charset=UTF-8"
            );
            Transport.send(msg);
            System.out.println("[EmailService] Confirmation envoyée à : " + destinataire);
            return EmailResult.SUCCESS;

        } catch (Exception e) {
            System.err.println("[EmailService] Erreur d'envoi confirmation : " + e.getMessage());
            return EmailResult.SEND_ERROR;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CORRECTION #3 : Méthode envoyerRappel ajoutée (manquait totalement)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie un e-mail HTML de rappel de séance.
     *
     * @param destinataire  adresse e-mail du destinataire
     * @param nomComplet    prénom + nom de l'utilisateur
     * @param nomPsy        nom du praticien
     * @param dateSeance    date + heure de la séance (LocalDateTime)
     * @param heuresAvant   délai avant la séance (1, 2, 6, 12 ou 24)
     * @param lienVisio     lien de visioconférence (peut être vide)
     * @return              EmailResult.SUCCESS | MISSING_EMAIL | SEND_ERROR
     */
    public static EmailResult envoyerRappel(
            String        destinataire,
            String        nomComplet,
            String        nomPsy,
            LocalDateTime dateSeance,
            int           heuresAvant,
            String        lienVisio
    ) {
        if (destinataire == null || destinataire.isBlank()) {
            System.err.println("[EmailService] Adresse e-mail manquante.");
            return EmailResult.MISSING_EMAIL;
        }

        Session session = Session.getInstance(buildSmtpProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SENDER_EMAIL, "MindAura – Rappels"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
            msg.setSubject("⏰ Rappel de séance dans " + heuresAvant + "h – MindAura");
            msg.setContent(
                    buildHtmlRappel(nomComplet, nomPsy, dateSeance, heuresAvant, lienVisio),
                    "text/html; charset=UTF-8"
            );
            Transport.send(msg);
            System.out.println("[EmailService] Rappel envoyé à : " + destinataire);
            return EmailResult.SUCCESS;

        } catch (Exception e) {
            System.err.println("[EmailService] Erreur d'envoi rappel : " + e.getMessage());
            return EmailResult.SEND_ERROR;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Corps HTML — Confirmation
    // ─────────────────────────────────────────────────────────────────────────
    private static String buildHtmlConfirmation(
            String nomComplet,
            String nomLocal,
            LocalDateTime dateSeance,
            String motif,
            String lieu
    ) {
        String dateStr  = dateSeance.format(FMT_DATE);
        String heureStr = dateSeance.format(FMT_HEURE);

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/></head>"
                + "<body style='margin:0;padding:0;background:#F3F4F6;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"
                + "<tr><td align='center' style='padding:40px 16px;'>"
                + "<table width='580' cellpadding='0' cellspacing='0' style='background:#fff;"
                + "border-radius:14px;overflow:hidden;box-shadow:0 4px 18px rgba(0,0,0,.10);'>"

                // En-tête
                + "<tr><td style='background:linear-gradient(135deg,#6D28D9,#4F46E5);"
                + "padding:32px 36px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0 0 6px;font-size:24px;letter-spacing:.6px;'>MindAura</h1>"
                + "<p style='color:#DDD6FE;margin:0;font-size:13px;'>Confirmation de réservation</p>"
                + "</td></tr>"

                // Corps
                + "<tr><td style='padding:36px;'>"
                + "<p style='font-size:15px;color:#374151;margin:0 0 8px;'>Bonjour <strong>"
                + nomComplet + "</strong>,</p>"
                + "<p style='color:#6B7280;font-size:14px;line-height:1.7;margin:0 0 24px;'>"
                + "Votre réservation a bien été enregistrée. Voici le récapitulatif :</p>"

                // Carte récapitulatif
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F5F3FF;"
                + "border-left:4px solid #7C3AED;border-radius:8px;margin-bottom:28px;'>"
                + "<tr><td style='padding:20px 24px;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"
                + row("🏢 Local / Salle", nomLocal)
                + row("📅 Date",          dateStr)
                + row("🕐 Heure",         heureStr)
                + row("📋 Motif",         motif)
                + row("📍 Lieu",          lieu)
                + "</table></td></tr></table>"

                + "<p style='color:#6B7280;font-size:13px;line-height:1.7;margin:0 0 20px;'>"
                + "En cas de question, n'hésitez pas à nous contacter.<br/>Merci de votre confiance !</p>"
                + "<p style='font-size:14px;color:#374151;margin:0;'>L'équipe <strong>MindAura</strong></p>"
                + "</td></tr>"

                // Pied de page
                + "<tr><td style='background:#F9FAFB;padding:16px 36px;"
                + "border-top:1px solid #E5E7EB;text-align:center;'>"
                + "<p style='font-size:11px;color:#9CA3AF;margin:0;'>"
                + "© 2025 MindAura – Cet e-mail est envoyé automatiquement, merci de ne pas y répondre.</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Corps HTML — Rappel
    // ─────────────────────────────────────────────────────────────────────────
    private static String buildHtmlRappel(
            String nomComplet,
            String nomPsy,
            LocalDateTime dateSeance,
            int heuresAvant,
            String lienVisio
    ) {
        String dateStr  = dateSeance.format(FMT_DATE);
        String heureStr = dateSeance.format(FMT_HEURE);
        String lienHtml = (lienVisio != null && !lienVisio.isBlank())
                ? "<a href='" + lienVisio + "' style='color:#6D28D9;font-weight:700;'>Rejoindre la visio</a>"
                : "—";

        return "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/></head>"
                + "<body style='margin:0;padding:0;background:#F3F4F6;font-family:Arial,sans-serif;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"
                + "<tr><td align='center' style='padding:40px 16px;'>"
                + "<table width='580' cellpadding='0' cellspacing='0' style='background:#fff;"
                + "border-radius:14px;overflow:hidden;box-shadow:0 4px 18px rgba(0,0,0,.10);'>"

                // En-tête
                + "<tr><td style='background:linear-gradient(135deg,#6D28D9,#4F46E5);"
                + "padding:32px 36px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0 0 6px;font-size:24px;letter-spacing:.6px;'>MindAura</h1>"
                + "<p style='color:#DDD6FE;margin:0;font-size:13px;'>Rappel de séance</p>"
                + "</td></tr>"

                // Corps
                + "<tr><td style='padding:36px;'>"
                + "<p style='font-size:15px;color:#374151;margin:0 0 8px;'>Bonjour <strong>"
                + nomComplet + "</strong>,</p>"
                + "<p style='color:#6B7280;font-size:14px;line-height:1.7;margin:0 0 24px;'>"
                + "Votre séance aura lieu dans <strong>" + heuresAvant
                + "h</strong>. Voici le récapitulatif :</p>"

                // Carte récapitulatif
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F5F3FF;"
                + "border-left:4px solid #7C3AED;border-radius:8px;margin-bottom:28px;'>"
                + "<tr><td style='padding:20px 24px;'>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"
                + row("👨‍⚕️ Praticien", nomPsy)
                + row("📅 Date",        dateStr)
                + row("🕐 Heure",       heureStr)
                + row("🔗 Lien visio",  lienHtml)
                + "</table></td></tr></table>"

                + "<p style='color:#6B7280;font-size:13px;line-height:1.7;margin:0 0 20px;'>"
                + "Pensez à vous connecter quelques minutes avant le début de la séance.</p>"
                + "<p style='font-size:14px;color:#374151;margin:0;'>L'équipe <strong>MindAura</strong></p>"
                + "</td></tr>"

                // Pied de page
                + "<tr><td style='background:#F9FAFB;padding:16px 36px;"
                + "border-top:1px solid #E5E7EB;text-align:center;'>"
                + "<p style='font-size:11px;color:#9CA3AF;margin:0;'>"
                + "© 2025 MindAura – Cet e-mail est envoyé automatiquement, merci de ne pas y répondre.</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Utilitaire commun
    // ─────────────────────────────────────────────────────────────────────────
    private static String row(String label, String value) {
        return "<tr>"
                + "<td style='padding:6px 0;color:#6B7280;font-size:13px;width:150px;'>" + label + "</td>"
                + "<td style='padding:6px 0;color:#111827;font-size:13px;font-weight:700;'>"
                + (value != null && !value.isBlank() ? value : "—") + "</td>"
                + "</tr>";
    }
}