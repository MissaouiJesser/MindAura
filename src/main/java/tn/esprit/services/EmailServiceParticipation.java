package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;

import java.io.UnsupportedEncodingException;
import java.util.Properties;


public class EmailServiceParticipation {


    private static final String SENDER_EMAIL    = "zairirania61@gmail.com";
    private static final String SENDER_PASSWORD = "lsmf xhgt nqnk jvfe";
    private static final String SMTP_HOST       = "smtp.gmail.com";
    private static final int    SMTP_PORT       = 587;

    public void envoyerConfirmationParticipation(Participation participation, Evenements evenement)
            throws MessagingException, UnsupportedEncodingException {

        if (participation.getEmail() == null || participation.getEmail().isBlank()) {
            System.out.println("[EmailServiceParticipation] Pas d'adresse e-mail → envoi ignoré.");
            return;
        }

        Session session = creerSession();

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL, "Eventura — Gestion Événements", "UTF-8"));
        message.setRecipient(Message.RecipientType.TO,
                new InternetAddress(participation.getEmail(), participation.getNom_participation(), "UTF-8"));
        message.setSubject("✅ Confirmation d'inscription — " + evenement.getTitre_evenement());
        message.setContent(buildHtmlBody(participation, evenement), "text/html; charset=UTF-8");

        Transport.send(message);
        System.out.println("[EmailServiceParticipation] Email envoyé à : " + participation.getEmail());
    }


    public void envoyerRappel(Participation participation, Evenements evenement)
            throws MessagingException {

        if (participation.getEmail() == null || participation.getEmail().isBlank()) return;

        Session session = creerSession();
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipient(Message.RecipientType.TO,
                new InternetAddress(participation.getEmail()));
        message.setSubject("⏰ Rappel — " + evenement.getTitre_evenement() + " commence demain !");
        message.setContent(buildRappelBody(participation, evenement), "text/html; charset=UTF-8");

        Transport.send(message);
    }


    public void envoyerAnnulation(Participation participation, Evenements evenement)
            throws MessagingException {

        if (participation.getEmail() == null || participation.getEmail().isBlank()) return;

        Session session = creerSession();
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipient(Message.RecipientType.TO,
                new InternetAddress(participation.getEmail()));
        message.setSubject("❌ Annulation — " + evenement.getTitre_evenement());
        message.setText("Bonjour " + participation.getNom_participation() + ",\n\n"
                + "Nous sommes navrés de vous informer que l'événement « "
                + evenement.getTitre_evenement() + " » a été annulé.\n\n"
                + "Nous espérons vous revoir bientôt.\n\n"
                + "— L'équipe Eventura");

        Transport.send(message);
    }



    private Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
            }
        });
    }

    private String buildHtmlBody(Participation p, Evenements e) {
        String debut = e.getDatedebut_evenemnt() != null ? e.getDatedebut_evenemnt().toString() : "—";
        String fin   = e.getDatefin_evenemnt()   != null ? e.getDatefin_evenemnt().toString()   : "—";
        String lieu  = e.getLieu_evenement()      != null ? e.getLieu_evenement()                : "—";

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head><meta charset="UTF-8"><style>
              body { font-family: 'Segoe UI', Arial, sans-serif; background:#0D1526; color:#CBD5E1; margin:0; padding:0; }
              .container { max-width:560px; margin:30px auto; background:#141E2E;
                           border-radius:16px; border:1px solid #1E2D45; overflow:hidden; }
              .header { background:linear-gradient(135deg,#4F6EF7,#8B5CF6);
                        padding:32px 32px 24px; text-align:center; }
              .header h1 { margin:0; font-size:22px; color:#fff; letter-spacing:-0.5px; }
              .header p  { margin:6px 0 0; color:rgba(255,255,255,0.75); font-size:13px; }
              .body { padding:28px 32px; }
              .badge { display:inline-block; background:rgba(16,185,129,0.15);
                       color:#34D399; border:1px solid rgba(16,185,129,0.3);
                       border-radius:20px; padding:4px 14px; font-size:12px;
                       font-weight:700; margin-bottom:18px; }
              .info-row { display:flex; align-items:center; gap:10px;
                          margin:10px 0; font-size:13px; }
              .info-icon { font-size:16px; width:24px; text-align:center; }
              .info-label { color:#8899AA; min-width:80px; }
              .info-value { color:#F1F5F9; font-weight:600; }
              .divider { border:none; border-top:1px solid #1E2D45; margin:22px 0; }
              .footer { background:#111827; padding:18px 32px; text-align:center;
                        font-size:11px; color:#4B607A; }
              .cta { display:block; text-align:center; background:#4F6EF7; color:#fff;
                     text-decoration:none; border-radius:10px; padding:13px 24px;
                     font-weight:700; font-size:14px; margin:22px 0 0; }
            </style></head>
            <body>
            <div class="container">
              <div class="header">
                <h1>⚡ Eventura</h1>
                <p>Votre inscription est confirmée !</p>
              </div>
              <div class="body">
                <span class="badge">✅ INSCRIPTION CONFIRMÉE</span>
                <p style="font-size:15px; color:#F1F5F9; margin:0 0 20px;">
                  Bonjour <strong>%s</strong>, vous êtes inscrit(e) à :
                </p>
                <h2 style="font-size:20px;color:#F1F5F9;margin:0 0 16px;letter-spacing:-0.3px;">%s</h2>

                <div class="info-row">
                  <span class="info-icon">📅</span>
                  <span class="info-label">Début</span>
                  <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                  <span class="info-icon">🏁</span>
                  <span class="info-label">Fin</span>
                  <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                  <span class="info-icon">📍</span>
                  <span class="info-label">Lieu</span>
                  <span class="info-value">%s</span>
                </div>
                <div class="info-row">
                  <span class="info-icon">🎫</span>
                  <span class="info-label">Statut</span>
                  <span class="info-value" style="color:#FCD34D;">En attente de confirmation</span>
                </div>

                <hr class="divider">
                <p style="font-size:12px; color:#8899AA; margin:0;">
                  Conservez cet e-mail. Votre numéro d'inscription : <strong style="color:#4F6EF7;">#%d</strong>
                </p>
              </div>
              <div class="footer">
                © 2025 Eventura — Tous droits réservés<br>
                Vous recevez cet e-mail car vous vous êtes inscrit(e) à un événement.
              </div>
            </div>
            </body></html>
            """.formatted(
                p.getNom_participation(),
                e.getTitre_evenement(),
                debut, fin, lieu,
                p.getId_participation()
        );
    }

    private String buildRappelBody(Participation p, Evenements e) {
        return """
            <html><body style="font-family:Segoe UI,sans-serif;background:#0D1526;color:#CBD5E1;padding:30px;">
              <div style="max-width:500px;margin:auto;background:#141E2E;border-radius:14px;
                          border:1px solid #1E2D45;padding:28px;">
                <h2 style="color:#F1F5F9;">⏰ Rappel — Dans 24h !</h2>
                <p>Bonjour <strong>%s</strong>,</p>
                <p>L'événement <strong style="color:#4F6EF7;">%s</strong> commence demain.</p>
                <p>📍 Lieu : <strong>%s</strong></p>
                <p>Nous vous attendons !</p>
                <p style="color:#8899AA;font-size:11px;margin-top:20px;">— L'équipe Eventura</p>
              </div>
            </body></html>
            """.formatted(
                p.getNom_participation(),
                e.getTitre_evenement(),
                e.getLieu_evenement() != null ? e.getLieu_evenement() : "—"
        );
    }
}
