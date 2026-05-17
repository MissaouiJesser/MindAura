package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.InputStream;
import java.util.Properties;



public class EmailServiceR {

    private static final String[] config = loadConfig();
    private static final String FROM_EMAIL = config[0];
    private static final String PASSWORD    = config[1];
    private static final String FROM_NAME   = "Système de Réclamations";

    /**
     * Charger email + mot de passe depuis config.properties
     */
    private static String[] loadConfig() {
        try {
            Properties props = new Properties();
            InputStream is = EmailServiceR.class.getResourceAsStream("/config.properties");
            if (is == null) {
                System.out.println("❌ config.properties NON TROUVÉ!");
                return new String[]{"", ""};
            }
            props.load(is);
            String email    = props.getProperty("gmail.email", "");
            String password = props.getProperty("gmail.password", "");
            if (email.isBlank() || password.isBlank()) {
                System.out.println("❌ gmail.email ou gmail.password vide dans config.properties!");
                return new String[]{"", ""};
            }
            System.out.println("✅ Config Gmail chargée: " + email);
            return new String[]{email, password};
        } catch (Exception e) {
            System.out.println("❌ Erreur chargement config: " + e.getMessage());
            return new String[]{"", ""};
        }
    }

    /**
     * Créer la session Gmail SMTP
     */
    private static Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }

    /**
     * Envoyer un email générique
     */
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

            // Contenu HTML
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlContent, "text/html; charset=UTF-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

            Transport.send(message);
            System.out.println("✅ Email envoyé à: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur envoi email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoyer un email de réponse à un client
     */
    public static boolean envoyerReponseClient(String toEmail, String sujetReclamation,
                                               String reponseTexte, float noteReponse) {
        System.out.println("\n📧 Envoi réponse à: " + toEmail);

        String htmlContent = construireHtmlReponse(sujetReclamation, reponseTexte, noteReponse);
        String subject = "🎉 Réponse à votre réclamation: " + sujetReclamation;

        return envoyerEmail(toEmail, subject, htmlContent);
    }

    /**
     * ✅ Envoyer une nouvelle réclamation à l'admin (sujet + description + catégorie)
     */
    public static boolean envoyerNouvelleReclamationAdmin(String adminEmail, String sujet,
                                                          String description, String categorie) {
        System.out.println("\n📧 Notification admin: " + adminEmail);

        String htmlContent = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif;'>"
                + "<div style='max-width:600px;margin:0 auto;padding:20px;'>"
                + "<div style='background-color:#1A1A2E;color:white;padding:20px;border-radius:8px;text-align:center;'>"
                + "<h1>🔔 Nouvelle Réclamation Reçue</h1></div>"
                + "<div style='background-color:#f9fafb;padding:20px;margin:20px 0;border-radius:8px;"
                + "border-left:4px solid #1A1A2E;'>"
                + "<p><strong>📌 Sujet:</strong> " + sujet + "</p>"
                + "<p><strong>🏷️ Catégorie:</strong> " + categorie + "</p>"
                + "<hr style='border:1px solid #E2E8F0;'/>"
                + "<p><strong>📝 Description:</strong></p>"
                + "<div style='background-color:white;padding:15px;border-radius:6px;margin:10px 0;"
                + "border:1px solid #E2E8F0;'>"
                + description.replace("\n", "<br>")
                + "</div>"
                + "<p style='color:#5A6475;font-size:12px;'>Connectez-vous à l'application pour traiter cette réclamation.</p>"
                + "</div></div></body></html>";

        String subject = "🔔 Nouvelle réclamation: " + sujet;
        return envoyerEmail(adminEmail, subject, htmlContent);
    }

    /**
     * Envoyer un email de confirmation d'ajout de réclamation
     */
    public static boolean envoyerConfirmationReclamation(String toEmail, String sujet) {
        System.out.println("\n📧 Envoi confirmation à: " + toEmail);

        String htmlContent = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif;'>"
                + "<div style='max-width:600px;margin:0 auto;padding:20px;'>"
                + "<div style='background-color:#2D6A4F;color:white;padding:20px;border-radius:8px;text-align:center;'>"
                + "<h1>✅ Réclamation Reçue</h1></div>"
                + "<div style='background-color:#f9fafb;padding:20px;margin:20px 0;border-radius:8px;'>"
                + "<p>Bonjour,</p>"
                + "<p>Votre réclamation a été reçue avec succès.</p>"
                + "<p><strong>Sujet:</strong> " + sujet + "</p>"
                + "<p>Notre équipe va traiter votre demande dans les plus brefs délais.</p>"
                + "<p>Merci de votre patience.</p>"
                + "</div></div></body></html>";

        String subject = "✅ Confirmation: Réclamation reçue - " + sujet;
        return envoyerEmail(toEmail, subject, htmlContent);
    }

    /**
     * Construire le contenu HTML de l'email de réponse
     */
    private static String construireHtmlReponse(String sujet, String reponse, float note) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<style>"
                + "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}"
                + ".container{max-width:600px;margin:0 auto;padding:20px;}"
                + ".header{background-color:#2D6A4F;color:white;padding:20px;border-radius:8px;text-align:center;}"
                + ".content{background-color:#f9fafb;padding:20px;margin:20px 0;border-radius:8px;border-left:4px solid #2D6A4F;}"
                + ".footer{text-align:center;font-size:12px;color:#666;margin-top:30px;}"
                + "h2{color:#2D6A4F;}"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'><h1>🎉 Réponse à votre réclamation</h1></div>"
                + "<div class='content'>"
                + "<h2>Sujet: " + sujet + "</h2>"
                + "<p>Bonjour,</p>"
                + "<p>Voici la réponse à votre réclamation:</p>"
                + "<div style='background-color:white;padding:15px;border-radius:6px;margin:15px 0;'>"
                + reponse.replace("\n", "<br>")
                + "</div>"
                + "<p><strong>Note de satisfaction:</strong> " + note + "/5 ⭐</p>"
                + "<p>Si vous avez d'autres questions, n'hésitez pas à nous contacter.</p>"
                + "<p>Cordialement,<br>L'équipe du Système de Réclamations</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>Cet email a été généré automatiquement.</p>"
                + "<p>&copy; 2025 Système de Gestion des Réclamations.</p>"
                + "</div></div></body></html>";
    }

    /**
     * Test
     */
    public static void main(String[] args) {
        System.out.println("=== TEST GMAIL SMTP ===\n");

        boolean test1 = envoyerConfirmationReclamation(
                "benrabehyassinne@gmail.com",
                "Service de mauvaise qualité"
        );
        System.out.println("Test 1 (Confirmation): " + (test1 ? "✅ SUCCÈS" : "❌ ÉCHEC"));

        boolean test2 = envoyerReponseClient(
                "benrabehyassinne@gmail.com",
                "Service de mauvaise qualité",
                "Nous nous excusons pour la mauvaise expérience.",
                4.5f
        );
        System.out.println("Test 2 (Réponse): " + (test2 ? "✅ SUCCÈS" : "❌ ÉCHEC"));
    }
}