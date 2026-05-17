package tn.esprit.services;

import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * SERVICE D'ENVOI D'EMAILS
 * - Envoi à n'importe quel email
 * - Liste de destinataires de notification configurables
 * - Email de refus enrichi
 */
public class EmailRessource {

    // ══════════════════════════════════════════════════════════════════════════
    // CONFIGURATION SMTP
    // ══════════════════════════════════════════════════════════════════════════

    private static final String EMAIL_EXPEDITEUR = "minyarguesmi87@gmail.com";
    private static final String MOT_DE_PASSE     = "iqcs ejvq mvok igrz";

    /**
     * Liste des emails qui reçoivent UNE COPIE de TOUTES les notifications.
     * Ajoutez ici n'importe quel email admin/modérateur.
     */
    private static final List<String> EMAILS_NOTIFICATION = new ArrayList<>(Arrays.asList(
            // Ajoutez vos emails ici :
            // "admin@example.com",
            // "moderateur@example.com"
    ));

    // ══════════════════════════════════════════════════════════════════════════
    // GESTION DES DESTINATAIRES DE NOTIFICATION
    // ══════════════════════════════════════════════════════════════════════════

    /** Ajoute un email à la liste de notification globale */
    public static void ajouterEmailNotification(String email) {
        if (email != null && !email.isBlank() && !EMAILS_NOTIFICATION.contains(email)) {
            EMAILS_NOTIFICATION.add(email.trim());
        }
    }

    /** Retire un email de la liste de notification */
    public static void retirerEmailNotification(String email) {
        EMAILS_NOTIFICATION.remove(email);
    }

    /** Retourne la liste actuelle des emails de notification */
    public static List<String> getEmailsNotification() {
        return new ArrayList<>(EMAILS_NOTIFICATION);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SESSION SMTP
    // ══════════════════════════════════════════════════════════════════════════

    private Session creerSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",              "true");
        props.put("mail.smtp.starttls.enable",   "true");
        props.put("mail.smtp.host",              "smtp.gmail.com");
        props.put("mail.smtp.port",              "587");
        props.put("mail.smtp.ssl.trust",         "smtp.gmail.com");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout",           "8000");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_EXPEDITEUR, MOT_DE_PASSE);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ENVOI GÉNÉRIQUE
    // ══════════════════════════════════════════════════════════════════════════

    /** Envoie un email HTML à UN destinataire principal. */
    public boolean envoyerEmail(String destinataire, String sujet, String contenuHtml) {
        return envoyerEmailMultiple(List.of(destinataire), sujet, contenuHtml);
    }

    /** Envoie un email HTML à PLUSIEURS destinataires. */
    public boolean envoyerEmailMultiple(List<String> destinataires, String sujet, String contenuHtml) {
        if (destinataires == null || destinataires.isEmpty()) return false;
        try {
            Session session = creerSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(EMAIL_EXPEDITEUR, "Plateforme Ressources"));

            List<Address> addresses = new ArrayList<>();
            for (String dest : destinataires) {
                if (dest != null && !dest.isBlank()) {
                    try {
                        addresses.add(new InternetAddress(dest.trim()));
                    } catch (AddressException e) {
                        System.err.println("Email invalide ignore : " + dest);
                    }
                }
            }
            if (addresses.isEmpty()) return false;

            message.setRecipients(Message.RecipientType.TO, addresses.toArray(new Address[0]));
            message.setSubject(sujet);
            message.setContent(contenuHtml, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Email envoye a : " + destinataires);
            return true;

        } catch (Exception e) {
            System.err.println("Erreur envoi email : " + e.getMessage());
            return false;
        }
    }

    /** Construit la liste complète : auteur + emails de notification configurés. */
    private List<String> tousDestinataires(String emailAuteur) {
        List<String> tous = new ArrayList<>();
        if (emailAuteur != null && !emailAuteur.isBlank()) tous.add(emailAuteur.trim());
        tous.addAll(EMAILS_NOTIFICATION);
        return tous;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION : Ressource publiée
    // ══════════════════════════════════════════════════════════════════════════

    public boolean notifierNouvelleRessource(String emailAuteur, String titreRessource,
                                             String categorie, String niveau) {
        String sujet = "Votre ressource a ete publiee : " + titreRessource;
        String html  = buildHtmlPublication(titreRessource, categorie, niveau);
        return envoyerEmailMultiple(tousDestinataires(emailAuteur), sujet, html);
    }

    private String buildHtmlPublication(String titre, String categorie, String niveau) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;"
                + "border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,.1);\">"
                + "<div style=\"background:linear-gradient(135deg,#667eea,#764ba2);"
                + "padding:30px;text-align:center;\">"
                + "<h1 style=\"color:white;margin:0;font-size:24px;\">Votre Ressource est en Ligne !</h1>"
                + "</div>"
                + "<div style=\"padding:30px;background:#fff;\">"
                + "<p style=\"color:#555;font-size:16px;\">Bonjour,</p>"
                + "<p style=\"color:#555;font-size:16px;\">"
                + "Felicitations ! Votre ressource a ete publiee avec succes.</p>"
                + "<div style=\"background:#f8f9ff;border-left:4px solid #667eea;"
                + "border-radius:8px;padding:20px;margin:20px 0;\">"
                + "<h2 style=\"color:#333;margin:0 0 10px 0;\">" + titre + "</h2>"
                + "<p style=\"margin:5px 0;color:#666;\"><strong>Categorie :</strong> " + categorie + "</p>"
                + "<p style=\"margin:5px 0;color:#666;\"><strong>Niveau :</strong> " + niveau + "</p>"
                + "</div>"
                + "</div>"
                + "<div style=\"background:#f0f0f0;padding:15px;text-align:center;\">"
                + "<p style=\"color:#999;font-size:12px;margin:0;\">Plateforme de Ressources Bien-etre</p>"
                + "</div>"
                + "</div>";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NOTIFICATION : Contenu refusé
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Notifie l'auteur ET les emails configurés qu'une ressource a été refusée.
     *
     * @param emailAuteur           Email de l'auteur
     * @param titreRessource        Titre de la ressource refusée
     * @param categoriesDetectees   Catégories détectées par la modération
     * @param emailsSupplementaires Emails additionnels à notifier (optionnel)
     */
    public boolean notifierContenuRefuse(String emailAuteur, String titreRessource,
                                         String[] categoriesDetectees,
                                         String... emailsSupplementaires) {
        String sujet = "Votre contenu a ete refuse : " + titreRessource;
        String html  = buildHtmlRefus(titreRessource, categoriesDetectees);

        List<String> destinataires = tousDestinataires(emailAuteur);
        for (String extra : emailsSupplementaires) {
            if (extra != null && !extra.isBlank() && !destinataires.contains(extra.trim())) {
                destinataires.add(extra.trim());
            }
        }
        return envoyerEmailMultiple(destinataires, sujet, html);
    }

    /** Surcharge sans emails supplémentaires (rétrocompatible) */
    public boolean notifierContenuRefuse(String emailAuteur, String titreRessource,
                                         String[] categoriesDetectees) {
        return notifierContenuRefuse(emailAuteur, titreRessource, categoriesDetectees, new String[0]);
    }

    private String buildHtmlRefus(String titre, String[] categories) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;"
                + "border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,.1);\">"
                + "<div style=\"background:linear-gradient(135deg,#ff6b6b,#ee5a24);"
                + "padding:30px;text-align:center;\">"
                + "<h1 style=\"color:white;margin:0;font-size:24px;\">Contenu Refuse</h1>"
                + "</div>"
                + "<div style=\"padding:30px;background:#fff;\">"
                + "<p style=\"color:#555;font-size:16px;\">Bonjour,</p>"
                + "<p style=\"color:#555;font-size:16px;\">Votre ressource <strong>"
                + titre
                + "</strong> a ete refusee par notre systeme de moderation automatique.</p>"
                + "<div style=\"background:#fff5f5;border-left:4px solid #ff6b6b;"
                + "border-radius:8px;padding:20px;margin:20px 0;\">"
                + "<h3 style=\"color:#cc0000;margin:0 0 10px 0;\">Raisons du refus :</h3>"
                + buildCategoriesHtml(categories)
                + "</div>"
                + "<div style=\"background:#f0f8ff;border-radius:8px;padding:20px;margin:20px 0;\">"
                + "<h3 style=\"color:#333;margin:0 0 10px 0;\">Que faire ?</h3>"
                + "<ul style=\"color:#666;padding-left:20px;\">"
                + "<li>Relisez les regles de la communaute</li>"
                + "<li>Modifiez le contenu inapproprie</li>"
                + "<li>Soumettez a nouveau votre ressource</li>"
                + "</ul>"
                + "</div>"
                + "<div style=\"background:#fffbeb;border-radius:8px;padding:15px;"
                + "margin:10px 0;border:1px solid #fde68a;\">"
                + "<p style=\"color:#92400e;font-size:12px;margin:0;\">"
                + "Si vous pensez que cette moderation est incorrecte, contactez un administrateur."
                + "</p>"
                + "</div>"
                + "</div>"
                + "<div style=\"background:#f0f0f0;padding:15px;text-align:center;\">"
                + "<p style=\"color:#999;font-size:12px;margin:0;\">"
                + "Plateforme de Ressources Bien-etre - Moderation automatique"
                + "</p>"
                + "</div>"
                + "</div>";
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMAIL LIBRE — envoyer à n'importe quelle adresse
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Envoie un email personnalisé à n'importe quelle adresse.
     */
    public boolean envoyerEmailPersonnalise(String destinataire, String sujet,
                                            String titreEmail, String corpsMessage,
                                            String couleurHeader) {
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;"
                + "border-radius:12px;overflow:hidden;\">"
                + "<div style=\"background:" + couleurHeader + ";padding:30px;text-align:center;\">"
                + "<h1 style=\"color:white;margin:0;font-size:22px;\">" + titreEmail + "</h1>"
                + "</div>"
                + "<div style=\"padding:30px;background:#fff;\">"
                + "<p style=\"color:#444;font-size:15px;line-height:1.6;\">"
                + corpsMessage.replace("\n", "<br>")
                + "</p>"
                + "</div>"
                + "<div style=\"background:#f0f0f0;padding:15px;text-align:center;\">"
                + "<p style=\"color:#999;font-size:12px;margin:0;\">Plateforme de Ressources Bien-etre</p>"
                + "</div>"
                + "</div>";

        return envoyerEmail(destinataire, sujet, html);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITAIRES PRIVÉS
    // ══════════════════════════════════════════════════════════════════════════

    private String buildCategoriesHtml(String[] categories) {
        if (categories == null || categories.length == 0) {
            return "<p style=\"color:#666;margin:0;\">Contenu inapproprie detecte</p>";
        }
        StringBuilder sb = new StringBuilder("<ul style=\"color:#666;padding-left:20px;margin:0;\">");
        for (String cat : categories) {
            sb.append("<li style=\"margin:4px 0;\">").append(cat).append("</li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }
}