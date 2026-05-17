package tn.esprit.utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailService {

    private static final String EMAIL_FROM     = "laifiazer0@gmail.com";
    private static final String EMAIL_PASSWORD = "hzhg wffz bykj jdvo";
    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final String SMTP_PORT      = "587";
    private static final String APP_NAME       = "MindAura";

    /** Pool de threads pour l'envoi asynchrone (ne bloque pas l'interface JavaFX) */
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "EmailThread");
        t.setDaemon(true); // thread démon : se termine avec l'application
        return t;
    });

    private EmailService() {}

    // =========================================================================
    //  API PUBLIQUE
    // =========================================================================

    /**
     * Envoie un email de confirmation d'inscription à un nouvel utilisateur.
     * Envoi asynchrone — ne bloque pas le thread JavaFX.
     *
     * @param destinataireEmail email du nouvel utilisateur
     * @param prenom            prénom pour personnaliser le message
     * @param nom               nom de famille
     * @param role              libellé du rôle attribué (ex: "Patient")
     * @param onSuccess         callback exécuté si succès (peut être null)
     * @param onError           callback exécuté si erreur (peut être null)
     */
    public static void envoyerConfirmationInscription(
            String destinataireEmail,
            String prenom,
            String nom,
            String role,
            Runnable onSuccess,
            java.util.function.Consumer<String> onError) {

        executor.submit(() -> {
            try {
                String sujet = "✅ Bienvenue sur " + APP_NAME + " !";
                String corps = buildInscriptionHtml(prenom, nom, destinataireEmail, role);
                send(destinataireEmail, sujet, corps);
                System.out.println("[Email] Confirmation envoyée → " + destinataireEmail);
                if (onSuccess != null)
                    javafx.application.Platform.runLater(onSuccess);
            } catch (Exception e) {
                System.err.println("[Email] Erreur confirmation : " + e.getMessage());
                if (onError != null)
                    javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    /**
     * Envoie un rappel de traitement à un patient.
     * Envoi asynchrone — ne bloque pas le thread JavaFX.
     *
     * @param destinataireEmail email du patient
     * @param prenom            prénom du patient
     * @param typeTraitement    libellé du type (ex: "Psychologique")
     * @param objectif          libellé de l'objectif (ex: "Gestion Du Stress")
     * @param dateDebut         date de début du traitement
     * @param dateFin           date de fin du traitement (peut être null)
     * @param description       description du traitement (peut être null)
     * @param nomCoach          nom du coach/thérapeute (peut être null)
     * @param onSuccess         callback si succès
     * @param onError           callback si erreur
     */
    public static void envoyerRappelTraitement(
            String destinataireEmail,
            String prenom,
            String typeTraitement,
            String objectif,
            Date dateDebut,
            Date dateFin,
            String description,
            String nomCoach,
            Runnable onSuccess,
            java.util.function.Consumer<String> onError) {

        executor.submit(() -> {
            try {
                String sujet = "📋 " + APP_NAME + " — Rappel de votre programme : " + typeTraitement;
                String corps = buildRappelTraitementHtml(
                        prenom, typeTraitement, objectif, dateDebut, dateFin, description, nomCoach);
                send(destinataireEmail, sujet, corps);
                System.out.println("[Email] Rappel traitement envoyé → " + destinataireEmail);
                if (onSuccess != null)
                    javafx.application.Platform.runLater(onSuccess);
            } catch (Exception e) {
                System.err.println("[Email] Erreur rappel : " + e.getMessage());
                if (onError != null)
                    javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    /**
     * Version simplifiée sans callbacks (fire-and-forget).
     * Utile quand on n'a pas besoin de retour UI.
     */
    public static void envoyerConfirmationInscription(String email, String prenom, String nom, String role) {
        envoyerConfirmationInscription(email, prenom, nom, role, null, null);
    }

    public static void envoyerRappelTraitement(
            String email, String prenom, String type, String objectif,
            Date debut, Date fin, String description, String coach) {
        envoyerRappelTraitement(email, prenom, type, objectif, debut, fin, description, coach, null, null);
    }

    // =========================================================================
    //  RÉINITIALISATION MOT DE PASSE  — À AJOUTER dans EmailService.java
    //  Collez cette méthode AVANT la méthode send() dans la classe EmailService
    // =========================================================================

    /**
     * Envoie un code de vérification à 6 chiffres pour la réinitialisation
     * du mot de passe. Design HTML élégant avec palette MindAura.
     *
     * @param destinataireEmail  email de l'utilisateur
     * @param prenom             prénom pour personnaliser le message
     * @param code               code à 6 chiffres généré
     * @param onSuccess          callback si envoi réussi
     * @param onError            callback si erreur
     */
    public static void envoyerCodeReinitialisation(
            String destinataireEmail,
            String prenom,
            String code,
            Runnable onSuccess,
            java.util.function.Consumer<String> onError) {

        executor.submit(() -> {
            try {
                String sujet = "🔐 " + APP_NAME + " — Code de vérification : " + code;
                String corps = buildCodeReinitialisationHtml(prenom, code);
                send(destinataireEmail, sujet, corps);
                System.out.println("[Email] Code de réinitialisation envoyé → " + destinataireEmail);
                if (onSuccess != null)
                    javafx.application.Platform.runLater(onSuccess);
            } catch (Exception e) {
                System.err.println("[Email] Erreur code réinitialisation : " + e.getMessage());
                if (onError != null)
                    javafx.application.Platform.runLater(() -> onError.accept(e.getMessage()));
            }
        });
    }

    /**
     * Construit le template HTML de l'email de réinitialisation du mot de passe.
     */
    private static String buildCodeReinitialisationHtml(String prenom, String code) {
        // Formater le code avec des espaces pour meilleure lisibilité : "1 2 3 4 5 6"
        String codeFormate = String.join(" ", code.split(""));

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Réinitialisation du mot de passe MindAura</title>
            </head>
            <body style="margin:0; padding:0; background-color:#F0F4F8;
                         font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">

              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                     style="background-color:#F0F4F8; padding:40px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="560" cellpadding="0" cellspacing="0"
                           style="background:#FFFFFF; border-radius:20px; overflow:hidden;
                                  box-shadow:0 8px 32px rgba(27,67,50,0.12);">

                      <!-- ══ EN-TÊTE ══ -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#1B4332 0%%,#2D6A4F 100%%);
                                   padding:44px 48px 36px 48px; text-align:center;">

                          <!-- Icône cadenas -->
                          <div style="display:inline-block; width:72px; height:72px;
                                      background:rgba(255,255,255,0.15); border-radius:50%%;
                                      line-height:72px; font-size:36px; margin-bottom:16px;">
                            🔐
                          </div>

                          <h1 style="color:#FFFFFF; font-size:26px; font-weight:800;
                                     letter-spacing:0.5px; margin:0 0 6px 0;">MindAura</h1>
                          <p style="color:rgba(255,255,255,0.75); font-size:13px;
                                    margin:0; letter-spacing:0.3px;">
                            Psychologie &amp; Développement Personnel
                          </p>
                        </td>
                      </tr>

                      <!-- ══ CORPS ══ -->
                      <tr>
                        <td style="padding:44px 48px 36px 48px;">

                          <!-- Titre -->
                          <h2 style="color:#1A1A2E; font-size:20px; font-weight:700;
                                     margin:0 0 10px 0; text-align:center;">
                            Réinitialisation du mot de passe
                          </h2>
                          <p style="color:#5A6475; font-size:14px; text-align:center;
                                    margin:0 0 32px 0; line-height:1.6;">
                            Bonjour <strong style="color:#1B4332;">%s</strong>,<br>
                            Vous avez demandé à réinitialiser votre mot de passe.<br>
                            Utilisez ce code de vérification à 6 chiffres :
                          </p>

                          <!-- ═══ BLOC CODE ═══ -->
                          <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                                 style="margin-bottom:32px;">
                            <tr>
                              <td align="center">
                                <div style="display:inline-block;
                                            background:linear-gradient(135deg,#F0FFF4,#E6F4ED);
                                            border:2px solid #95D5B2;
                                            border-radius:16px;
                                            padding:28px 48px;
                                            text-align:center;">
                                  <p style="margin:0 0 8px 0; color:#5A6475; font-size:11px;
                                             font-weight:700; text-transform:uppercase;
                                             letter-spacing:1.5px;">
                                    Code de vérification
                                  </p>
                                  <span style="font-size:42px; font-weight:900; letter-spacing:12px;
                                               color:#1B4332; font-family:monospace;
                                               display:block; line-height:1.2;">
                                    %s
                                  </span>
                                  <p style="margin:10px 0 0 0; color:#5A6475; font-size:11px;">
                                    ⏱ Expire dans <strong>10 minutes</strong>
                                  </p>
                                </div>
                              </td>
                            </tr>
                          </table>

                          <!-- Avertissement sécurité -->
                          <div style="background:#FFF8F0; border-left:4px solid #F39C12;
                                      border-radius:8px; padding:16px 20px; margin-bottom:28px;">
                            <p style="margin:0; color:#7D5A00; font-size:13px; line-height:1.6;">
                              ⚠️ <strong>Si vous n'avez pas demandé cette réinitialisation</strong>,
                              ignorez cet email. Votre mot de passe ne sera pas modifié.
                            </p>
                          </div>

                          <!-- Message -->
                          <div style="background:linear-gradient(135deg,#F0FFF4,#E8F5E9);
                                      border-radius:12px; padding:20px 24px; margin-bottom:28px;
                                      border:1px solid #C8E6C9;">
                            <p style="margin:0; color:#1B4332; font-size:14px; line-height:1.7;
                                       text-align:center;">
                              🛡️ Pour votre sécurité, ne partagez jamais ce code.<br>
                              L'équipe MindAura ne vous demandera jamais votre code.
                            </p>
                          </div>

                          <p style="color:#5A6475; font-size:13px; margin:0; line-height:1.6;">
                            Cordialement,<br>
                            <strong style="color:#1B4332;">L'équipe MindAura 🌿</strong>
                          </p>

                        </td>
                      </tr>

                      <!-- ══ PIED DE PAGE ══ -->
                      <tr>
                        <td style="background:#F0F4F8; padding:20px 48px 24px 48px;
                                   text-align:center; border-top:1px solid #E5E9ED;">
                          <p style="color:#9CA8B3; font-size:11px; margin:0; line-height:1.7;">
                            Cet email a été envoyé automatiquement par MindAura.<br>
                            © 2026 MindAura – Psychologie &amp; Développement Personnel.<br>
                            <span style="color:#95C9B4;">Ne pas répondre à cet email.</span>
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(prenom, codeFormate);
    }

    // =========================================================================
    //  ENVOI SMTP INTERNE
    // =========================================================================

    /**
     * Méthode d'envoi SMTP via Gmail TLS.
     * Lancée dans le thread du pool (jamais dans le thread JavaFX).
     */
    private static void send(String to, String subject, String htmlBody) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth",                "true");
        props.put("mail.smtp.starttls.enable",     "true");
        props.put("mail.smtp.host",                SMTP_HOST);
        props.put("mail.smtp.port",                SMTP_PORT);
        props.put("mail.smtp.ssl.trust",           SMTP_HOST);
        props.put("mail.smtp.connectiontimeout",   "10000");
        props.put("mail.smtp.timeout",             "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);

        // ✅ Gestion de UnsupportedEncodingException levée par InternetAddress(addr, personal)
        try {
            message.setFrom(new InternetAddress(EMAIL_FROM, APP_NAME, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            // Fallback sans nom d'affichage si l'encodage échoue
            message.setFrom(new InternetAddress(EMAIL_FROM));
        }

        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        message.setSentDate(new Date());

        Transport.send(message);
    }

    // =========================================================================
    //  TEMPLATES HTML
    // =========================================================================

    /**
     * Template HTML — Confirmation d'inscription.
     * Design professionnel cohérent avec la charte MindAura (violet/lavande).
     */
    private static String buildInscriptionHtml(String prenom, String nom, String email, String role) {
        String dateInscription = new java.text.SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("fr", "FR"))
                .format(new Date());

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Bienvenue sur MindAura</title>
            </head>
            <body style="margin:0; padding:0; background-color:#F3F0FA; font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">

              <!-- Conteneur principal -->
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                     style="background-color:#F3F0FA; padding:40px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0"
                           style="background:#FFFFFF; border-radius:16px; overflow:hidden;
                                  box-shadow:0 4px 24px rgba(107,70,193,0.10);">

                      <!-- EN-TÊTE -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#6B46C1 0%%,#553C9A 100%%);
                                   padding:40px 48px; text-align:center;">
                          <h1 style="color:#FFFFFF; font-size:28px; font-weight:700;
                                     letter-spacing:1px; margin:0;">🧠 MindAura</h1>
                          <p style="color:rgba(255,255,255,0.80); font-size:14px; margin:8px 0 0 0;">
                            Psychologie &amp; Développement Personnel
                          </p>
                        </td>
                      </tr>

                      <!-- CORPS -->
                      <tr>
                        <td style="padding:48px 48px 32px 48px;">

                          <!-- Icône de bienvenue -->
                          <div style="text-align:center; margin-bottom:24px;">
                            <span style="font-size:56px;">🎉</span>
                          </div>

                          <h2 style="color:#2D3748; font-size:22px; font-weight:700;
                                     margin:0 0 16px 0; text-align:center;">
                            Bienvenue, %s !
                          </h2>

                          <p style="color:#4A5568; font-size:15px; line-height:1.7; margin:0 0 24px 0;">
                            Votre compte <strong>MindAura</strong> a été créé avec succès.
                            Nous sommes ravis de vous accueillir dans notre communauté dédiée
                            au bien-être psychologique et au développement personnel.
                          </p>

                          <!-- Carte informations compte -->
                          <div style="background:#F7F5FF; border-left:4px solid #6B46C1;
                                      border-radius:8px; padding:20px 24px; margin:0 0 28px 0;">
                            <p style="margin:0 0 10px 0; color:#2D3748; font-size:14px; font-weight:600;">
                              📋 Informations de votre compte
                            </p>
                            <table cellpadding="0" cellspacing="0" width="100%%">
                              <tr>
                                <td style="color:#718096; font-size:13px; padding:3px 0;">Nom complet :</td>
                                <td style="color:#2D3748; font-size:13px; font-weight:600;
                                           padding:3px 0; padding-left:12px;">%s %s</td>
                              </tr>
                              <tr>
                                <td style="color:#718096; font-size:13px; padding:3px 0;">Email :</td>
                                <td style="color:#6B46C1; font-size:13px; font-weight:600;
                                           padding:3px 0; padding-left:12px;">%s</td>
                              </tr>
                              <tr>
                                <td style="color:#718096; font-size:13px; padding:3px 0;">Rôle :</td>
                                <td style="padding:3px 0; padding-left:12px;">
                                  <span style="background:#EDE9F8; color:#6B46C1; font-size:12px;
                                               font-weight:700; padding:3px 10px; border-radius:20px;">
                                    %s
                                  </span>
                                </td>
                              </tr>
                              <tr>
                                <td style="color:#718096; font-size:13px; padding:3px 0;">Date d'inscription :</td>
                                <td style="color:#2D3748; font-size:13px; font-weight:600;
                                           padding:3px 0; padding-left:12px;">%s</td>
                              </tr>
                            </table>
                          </div>

                          <!-- Étape suivante : Face ID -->
                          <div style="background:#FFF7ED; border:1px solid #FED7AA;
                                      border-radius:8px; padding:16px 20px; margin:0 0 28px 0;">
                            <p style="margin:0; color:#92400E; font-size:13px; line-height:1.6;">
                              <strong>🔐 Sécurité Face ID :</strong> Pour activer l'authentification
                              faciale, veuillez vous connecter et ajouter une photo de profil.
                              Cette étape est obligatoire pour accéder à l'application.
                            </p>
                          </div>

                          <p style="color:#4A5568; font-size:15px; line-height:1.7; margin:0 0 8px 0;">
                            Si vous avez des questions, n'hésitez pas à contacter notre équipe.
                          </p>

                          <p style="color:#718096; font-size:14px; margin:0;">
                            Avec nos meilleures salutations,<br>
                            <strong style="color:#6B46C1;">L'équipe MindAura 💜</strong>
                          </p>
                        </td>
                      </tr>

                      <!-- PIED DE PAGE -->
                      <tr>
                        <td style="background:#F7F5FF; padding:24px 48px; text-align:center;
                                   border-top:1px solid #E9E4F5;">
                          <p style="color:#A0AEC0; font-size:12px; margin:0; line-height:1.6;">
                            Cet email a été envoyé automatiquement par MindAura.<br>
                            © 2026 MindAura – Psychologie &amp; Développement Personnel.<br>
                            Si vous n'êtes pas à l'origine de cette inscription, ignorez cet email.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(prenom, prenom, nom, email, role, dateInscription);
    }

    /**
     * Template HTML — Rappel de traitement.
     * Design cohérent avec la charte MindAura.
     */
    private static String buildRappelTraitementHtml(
            String prenom,
            String typeTraitement,
            String objectif,
            Date dateDebut,
            Date dateFin,
            String description,
            String nomCoach) {

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy", new java.util.Locale("fr","FR"));
        String debutStr = dateDebut != null ? sdf.format(dateDebut) : "—";
        String finStr   = dateFin   != null ? sdf.format(dateFin)   : "Non définie";
        String descStr  = (description != null && !description.isBlank()) ? description : "Aucune description fournie.";
        String coachStr = (nomCoach   != null && !nomCoach.isBlank())   ? nomCoach    : "Non assigné";

        // Couleur de badge selon le type
        String badgeColor = switch (typeTraitement != null ? typeTraitement.toLowerCase() : "") {
            case "psychologique"  -> "#6B46C1";
            case "comportemental" -> "#2B6CB0";
            default               -> "#276749"; // mixte
        };
        String badgeBg = switch (typeTraitement != null ? typeTraitement.toLowerCase() : "") {
            case "psychologique"  -> "#EDE9F8";
            case "comportemental" -> "#EBF8FF";
            default               -> "#F0FFF4";
        };

        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Rappel de traitement MindAura</title>
            </head>
            <body style="margin:0; padding:0; background-color:#F3F0FA;
                         font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">

              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"
                     style="background-color:#F3F0FA; padding:40px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0"
                           style="background:#FFFFFF; border-radius:16px; overflow:hidden;
                                  box-shadow:0 4px 24px rgba(107,70,193,0.10);">

                      <!-- EN-TÊTE -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#6B46C1 0%%,#553C9A 100%%);
                                   padding:40px 48px; text-align:center;">
                          <h1 style="color:#FFFFFF; font-size:28px; font-weight:700;
                                     letter-spacing:1px; margin:0;">🧠 MindAura</h1>
                          <p style="color:rgba(255,255,255,0.80); font-size:14px; margin:8px 0 0 0;">
                            Psychologie &amp; Développement Personnel
                          </p>
                        </td>
                      </tr>

                      <!-- CORPS -->
                      <tr>
                        <td style="padding:48px 48px 32px 48px;">

                          <div style="text-align:center; margin-bottom:20px;">
                            <span style="font-size:48px;">📋</span>
                          </div>

                          <h2 style="color:#2D3748; font-size:21px; font-weight:700;
                                     margin:0 0 8px 0; text-align:center;">
                            Rappel de votre programme de suivi
                          </h2>
                          <p style="color:#718096; font-size:14px; text-align:center; margin:0 0 32px 0;">
                            Bonjour <strong>%s</strong>, voici un récapitulatif de votre traitement en cours.
                          </p>

                          <!-- Type + Objectif -->
                          <div style="display:flex; gap:12px; margin-bottom:24px; text-align:center;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td width="48%%" style="text-align:center; padding:16px;
                                                        background:#F7F5FF; border-radius:10px;
                                                        border:1px solid #E9E4F5;">
                                  <p style="margin:0 0 6px 0; color:#718096; font-size:12px; font-weight:600;
                                             text-transform:uppercase; letter-spacing:0.5px;">Type</p>
                                  <span style="background:%s; color:%s; font-size:13px;
                                               font-weight:700; padding:5px 14px; border-radius:20px;">
                                    %s
                                  </span>
                                </td>
                                <td width="4%%"></td>
                                <td width="48%%" style="text-align:center; padding:16px;
                                                        background:#F0FFF4; border-radius:10px;
                                                        border:1px solid #C6F6D5;">
                                  <p style="margin:0 0 6px 0; color:#718096; font-size:12px; font-weight:600;
                                             text-transform:uppercase; letter-spacing:0.5px;">Objectif</p>
                                  <span style="background:#C6F6D5; color:#276749; font-size:13px;
                                               font-weight:700; padding:5px 14px; border-radius:20px;">
                                    %s
                                  </span>
                                </td>
                              </tr>
                            </table>
                          </div>

                          <!-- Dates + Coach -->
                          <div style="background:#F7F5FF; border-radius:10px; padding:20px 24px;
                                      margin:0 0 24px 0; border:1px solid #E9E4F5;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="padding:6px 0;">
                                  <span style="color:#718096; font-size:13px;">📅 Date de début :</span>
                                  <strong style="color:#2D3748; font-size:13px; margin-left:8px;">%s</strong>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:6px 0;">
                                  <span style="color:#718096; font-size:13px;">🏁 Date de fin :</span>
                                  <strong style="color:#2D3748; font-size:13px; margin-left:8px;">%s</strong>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:6px 0;">
                                  <span style="color:#718096; font-size:13px;">👨‍⚕️ Coach / Thérapeute :</span>
                                  <strong style="color:#6B46C1; font-size:13px; margin-left:8px;">%s</strong>
                                </td>
                              </tr>
                            </table>
                          </div>

                          <!-- Description -->
                          <div style="background:#FAFAFA; border-left:4px solid #9F7AEA;
                                      border-radius:8px; padding:16px 20px; margin:0 0 28px 0;">
                            <p style="margin:0 0 6px 0; color:#2D3748; font-size:13px;
                                       font-weight:700; text-transform:uppercase; letter-spacing:0.5px;">
                              📝 Description du programme
                            </p>
                            <p style="margin:0; color:#4A5568; font-size:14px; line-height:1.7;">%s</p>
                          </div>

                          <!-- Message motivation -->
                          <div style="background:linear-gradient(135deg,#EDE9F8,#E0E7FF);
                                      border-radius:10px; padding:20px 24px; margin:0 0 24px 0;
                                      text-align:center;">
                            <p style="margin:0; color:#553C9A; font-size:15px; font-weight:600;
                                       line-height:1.6; font-style:italic;">
                              💪 Continuez vos efforts ! Chaque séance vous rapproche de votre objectif.
                              Votre bien-être est notre priorité.
                            </p>
                          </div>

                          <p style="color:#718096; font-size:14px; margin:0;">
                            Avec nos meilleures salutations,<br>
                            <strong style="color:#6B46C1;">L'équipe MindAura 💜</strong>
                          </p>
                        </td>
                      </tr>

                      <!-- PIED DE PAGE -->
                      <tr>
                        <td style="background:#F7F5FF; padding:24px 48px; text-align:center;
                                   border-top:1px solid #E9E4F5;">
                          <p style="color:#A0AEC0; font-size:12px; margin:0; line-height:1.6;">
                            Cet email a été envoyé automatiquement par MindAura.<br>
                            © 2026 MindAura – Psychologie &amp; Développement Personnel.<br>
                            Pour toute question, contactez votre thérapeute via l'application.
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                prenom,
                badgeBg, badgeColor, typeTraitement != null ? typeTraitement : "—",
                objectif != null ? objectif : "—",
                debutStr, finStr, coachStr,
                descStr
        );
    }
}