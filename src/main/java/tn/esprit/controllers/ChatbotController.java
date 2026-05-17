package tn.esprit.controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Random;

/**
 * Contrôleur du chatbot de soutien psychologique MindAura
 * — Appel API OpenAI + fallback local intelligent (aucune dépendance externe)
 */
public class ChatbotController implements Initializable {


    private static final String OPENAI_API_KEY = "";   // ex: "sk-proj-xxxxxxxxxxxx"
    private static final String OPENAI_MODEL   = "gpt-3.5-turbo";
    private static final String API_URL        = "https://api.openai.com/v1/chat/completions";

    private static final String SYSTEM_PROMPT =
            "Tu es MindAura, un assistant intelligent et polyvalent intégré dans une plateforme de santé mentale en Tunisie. " +
                    "Tu peux répondre à TOUTES les questions : questions générales, culture, science, technologie, histoire, " +
                    "math, cuisine, voyage, actualités, conseils pratiques, et bien sûr les sujets de bien-être et de santé mentale. " +
                    "Tu réponds toujours en français, avec un ton bienveillant, clair et concis. " +
                    "Pour les sujets de santé mentale, tu es particulièrement empathique et tu ne poses pas de diagnostic. " +
                    "En cas de danger immédiat pour la personne, oriente vers le 71 391 111. " +
                    "Adapte la longueur de tes réponses à la complexité de la question.";

    // =====================================================
    //  BASE DE RÉPONSES LOCALES — mode sans API
    // =====================================================
    private static final List<String[]>  MOTS_CLES = new ArrayList<>();
    private static final List<String>    REPONSES  = new ArrayList<>();

    static {
        add(new String[]{"stress","stresse","stressee","pression","deborde","surcharge"},
                "Je comprends que vous vous sentez stressé(e) en ce moment. 💙\n\n" +
                        "Essayez la respiration 4-7-8 : inspirez 4 secondes, retenez 7 secondes, expirez lentement en 8 secondes. Répétez 3 fois.\n\n" +
                        "Qu'est-ce qui vous pèse le plus en ce moment ?"
        );
        add(new String[]{"anxieux","anxieuse","anxiete","angoisse","angoisse","panique","peur","inquiet","angoiser","angoisser","angoisee","angoise"},
                "L'anxiété peut être très éprouvante, mais vous n'êtes pas seul(e). 🌿\n\n" +
                        "Essayez la règle du 5-4-3-2-1 : nommez 5 choses vues, 4 touchées, 3 entendues, 2 senties, 1 goûtée. Cela ancre dans le présent.\n\n" +
                        "Voulez-vous me parler de ce qui déclenche cette anxiété ?"
        );
        add(new String[]{"deprime","deprimee","depression","triste","tristesse","malheureux","malheureuse","vide","pleurs","pleure"},
                "Je suis touché(e) que vous partagiez cela avec moi. 💙\n\n" +
                        "La tristesse profonde mérite d'être prise au sérieux. Il est important de ne pas rester seul(e) avec ces émotions.\n\n" +
                        "Y a-t-il quelqu'un de confiance autour de vous ? Souhaitez-vous que je vous aide à trouver un professionnel ?"
        );
        add(new String[]{"dormir","dors","dort","sommeil","insomnie","nuit","fatigue","fatigue","reveil","reveille","nuit blanche"},
                "Les troubles du sommeil affectent beaucoup de personnes. 🌙\n\n" +
                        "Quelques conseils : éteignez les écrans 1h avant le coucher, maintenez des horaires réguliers, essayez 5 min de respiration profonde au lit.\n\n" +
                        "Depuis combien de temps rencontrez-vous ces difficultés ?"
        );
        add(new String[]{"seul","seule","solitude","isole","isolee","personne","abandonner","abandonne"},
                "Le sentiment de solitude peut être très douloureux. Je suis là pour vous écouter. 🤝\n\n" +
                        "Ressentir de la solitude ne signifie pas que vous êtes moins digne d'être entouré(e) — c'est simplement un besoin de connexion humaine.\n\n" +
                        "Qu'est-ce qui vous a amené à vous sentir ainsi ?"
        );
        add(new String[]{"colere","en colere","enerve","enervee","frustre","frustree","rage","agressif"},
                "La colère est une émotion tout à fait légitime. 💪\n\n" +
                        "Quand vous la sentez monter, essayez : une pause + 5 respirations profondes, nommer l'émotion (\"Je me sens frustré(e) parce que...\"), ou bouger physiquement.\n\n" +
                        "Qu'est-ce qui a déclenché cette colère ?"
        );
        add(new String[]{"travail","boulot","bureau","collegue","patron","emploi","burn-out","burnout","licencie"},
                "Le stress professionnel est l'une des causes les plus fréquentes de souffrance. Vous faites bien d'en parler. 🌱\n\n" +
                        "Avez-vous la possibilité de poser des limites dans votre environnement professionnel ?\n\n" +
                        "Un accompagnement spécialisé peut vraiment aider. Souhaitez-vous des informations sur les centres disponibles ?"
        );
        add(new String[]{"famille","parents","enfants","mari","femme","couple","relation","divorce","rupture"},
                "Les difficultés relationnelles et familiales sont souvent très chargées émotionnellement. 💙\n\n" +
                        "Parler à un thérapeute peut apporter un regard neutre et bienveillant sur ces situations complexes.\n\n" +
                        "Voulez-vous me partager davantage ce que vous traversez ?"
        );
        add(new String[]{"suicid","mourir","mort","en finir","plus envie","envie de mourir"},
                "Je suis très préoccupé(e) par ce que vous partagez. Merci de me faire confiance. ❤️\n\n" +
                        "Vos sentiments méritent une aide immédiate et professionnelle. Contactez dès maintenant :\n\n" +
                        "🚨 Urgences psychiatriques : 71 391 111 (24h/24 — 7j/7)\n\n" +
                        "Vous n'avez pas à traverser cela seul(e). Y a-t-il quelqu'un près de vous en ce moment ?"
        );
        add(new String[]{"medicament","traitement","psychiatre","psychologue","therapie","consultation","rdv","rendez-vous"},
                "C'est très positif que vous pensiez à consulter. 🌟\n\n" +
                        "MindAura vous permet de trouver et réserver des locaux psychiatriques et centres de bien-être près de chez vous, directement depuis l'accueil.\n\n" +
                        "Avez-vous déjà eu un suivi psychologique, ou est-ce une première démarche ?"
        );
        add(new String[]{"bonjour","salut","bonsoir","hello","coucou"},
                "Bonjour ! 👋 Je suis votre assistant psychologique MindAura.\n\n" +
                        "Je suis là pour vous écouter sans jugement, avant votre rendez-vous. 💙\n\n" +
                        "Comment vous sentez-vous aujourd'hui ?"
        );
        add(new String[]{"merci","super","bien","parfait","top","genial","ca va","ça va"},
                "Je suis heureux(se) que vous alliez mieux ! 😊\n\n" +
                        "N'hésitez pas à revenir si vous avez besoin de parler. Prendre soin de sa santé mentale est un acte de courage.\n\n" +
                        "Y a-t-il autre chose dont vous souhaitez discuter ?"
        );
    }

    private static void add(String[] mots, String reponse) {
        MOTS_CLES.add(mots);
        REPONSES.add(reponse);
    }

    // =====================================================

    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private TextArea inputField;
    @FXML private Button btnEnvoyer;
    @FXML private HBox typingIndicator;
    @FXML private Circle statusDot;
    @FXML private Label statusLabel;

    private final List<Map<String, String>> conversationHistory = new ArrayList<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Référence au contrôleur parent UserHome pour la navigation inline
    private UserHomeController parentHomeController;

    public void setParentHomeController(UserHomeController parent) {
        this.parentHomeController = parent;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Entrée = envoyer, Shift+Entrée = nouvelle ligne
        inputField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown()) {
                event.consume();
                handleEnvoyer();
            }
        });

        // Afficher le mode dans le statut
        boolean apiOk = apiConfiguree();
        if (statusLabel != null) {
            statusLabel.setText(apiOk
                    ? "En ligne · Propulsé par ChatGPT"
                    : "En ligne · Mode soutien local");
        }
        if (statusDot != null) {
            statusDot.setStyle("-fx-fill: #52B788;");
        }

        // Message de bienvenue
        afficherMessageBot(
                "Bonjour ! 👋 Je suis MindAura, votre assistant intelligent.\n\n" +
                        "Je peux répondre à toutes vos questions : santé mentale, culture générale, " +
                        "science, cuisine, calculs, et bien plus encore ! 🌿\n\n" +
                        "Comment puis-je vous aider aujourd'hui ?"
        );
    }

    private boolean apiConfiguree() {
        return OPENAI_API_KEY != null
                && !OPENAI_API_KEY.isEmpty()
                && !OPENAI_API_KEY.startsWith("sk-VOTRE")
                && OPENAI_API_KEY.startsWith("sk-");
    }

    // =====================================================
    //  ENVOI
    // =====================================================

    @FXML
    private void handleEnvoyer() {
        String texte = inputField.getText().trim();
        if (texte.isEmpty()) return;

        afficherMessageUtilisateur(texte);
        inputField.clear();
        btnEnvoyer.setDisable(true);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", texte);
        conversationHistory.add(userMsg);

        showTypingIndicator(true);

        final String messageUtilisateur = texte;

        new Thread(() -> {
            // Délai naturel
            try { Thread.sleep(900); } catch (InterruptedException ignored) {}

            String reponse = null;

            // Tentative API si configurée
            if (apiConfiguree()) {
                reponse = appellerOpenAI();
            }

            // Fallback local
            if (reponse == null) {
                reponse = genererReponseLocale(messageUtilisateur);
            }

            final String reponseFinal = reponse;
            Platform.runLater(() -> {
                showTypingIndicator(false);
                btnEnvoyer.setDisable(false);
                afficherMessageBot(reponseFinal);

                Map<String, String> aMsg = new HashMap<>();
                aMsg.put("role", "assistant");
                aMsg.put("content", reponseFinal);
                conversationHistory.add(aMsg);
            });
        }).start();
    }

    // =====================================================
    //  API OPENAI — sans dépendance externe
    // =====================================================

    private String appellerOpenAI() {
        try {
            StringBuilder msgs = new StringBuilder("[");
            msgs.append("{\"role\":\"system\",\"content\":").append(esc(SYSTEM_PROMPT)).append("}");

            int debut = Math.max(0, conversationHistory.size() - 16);
            for (int i = debut; i < conversationHistory.size(); i++) {
                Map<String, String> m = conversationHistory.get(i);
                msgs.append(",{\"role\":\"").append(m.get("role"))
                        .append("\",\"content\":").append(esc(m.get("content"))).append("}");
            }
            msgs.append("]");

            String body = "{\"model\":\"" + OPENAI_MODEL + "\","
                    + "\"messages\":" + msgs + ","
                    + "\"max_tokens\":500,\"temperature\":0.8}";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + OPENAI_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                return extraireContent(resp.body());
            } else {
                System.err.println("OpenAI [" + resp.statusCode() + "]: " + resp.body());
            }
        } catch (Exception e) {
            System.err.println("OpenAI error: " + e.getMessage());
        }
        return null;
    }

    /** Extrait le champ "content" de la réponse JSON OpenAI */
    private String extraireContent(String json) {
        // Cherche le dernier "content" dans les choices
        int idx = json.lastIndexOf("\"content\":");
        if (idx == -1) return null;

        int start = json.indexOf("\"", idx + 10) + 1;
        int end   = start;

        while (end < json.length()) {
            char c = json.charAt(end);
            if (c == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }

        if (start > 0 && end > start) {
            return json.substring(start, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .trim();
        }
        return null;
    }

    /** Échappe une chaîne pour JSON */
    private String esc(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t") + "\"";
    }

    // =====================================================
    //  MOTEUR DE RÉPONSES LOCALES
    // =====================================================
    //  MOTEUR DE COMPRÉHENSION AVANCÉE
    //  Gère : mots incomplets, fautes de frappe, abréviations,
    //         répétitions de lettres, phonétique approchée
    // =====================================================

    private String genererReponseLocale(String message) {
        // Découper le message en tokens (mots individuels)
        String[] tokens = normaliser(message).split("[\\s,;.!?'\"\\-]+");

        int meilleureCategorie = -1;
        double meilleurScore   = 0.0;

        for (int i = 0; i < MOTS_CLES.size(); i++) {
            double scoreCategorie = 0.0;

            for (String motCle : MOTS_CLES.get(i)) {
                String motNorm = normaliser(motCle);

                // 1. Correspondance exacte dans le message complet
                if (normaliser(message).contains(motNorm)) {
                    scoreCategorie = Math.max(scoreCategorie, 1.0);
                    continue;
                }

                // 2. Vérifier chaque token du message
                for (String token : tokens) {
                    if (token.length() < 2) continue;

                    double s = scoreToken(token, motNorm);
                    scoreCategorie = Math.max(scoreCategorie, s);
                }
            }

            if (scoreCategorie > meilleurScore) {
                meilleurScore   = scoreCategorie;
                meilleureCategorie = i;
            }
        }

        // Seuil d'acceptation : 0.62 pour éviter les faux positifs
        if (meilleurScore >= 0.62 && meilleureCategorie >= 0) {
            return REPONSES.get(meilleureCategorie);
        }

        // ── Tentative de réponse à des questions générales ─────────────────────
        String msgNorm = normaliser(message);

        // Questions sur l'identité du bot
        if (msgNorm.contains("qui es") || msgNorm.contains("c'est quoi") || msgNorm.contains("kesako") ||
                msgNorm.contains("que sais") || msgNorm.contains("tu fais quoi") || msgNorm.contains("tu peux")) {
            return "Je suis MindAura, votre assistant intelligent ! 🌿\n\n" +
                    "Je peux vous aider sur une grande variété de sujets :\n" +
                    "• 🧠 Santé mentale et bien-être\n" +
                    "• 🔬 Science, technologie, histoire\n" +
                    "• 🍳 Cuisine, voyage, culture générale\n" +
                    "• 💬 Conseils pratiques du quotidien\n\n" +
                    "Posez-moi n'importe quelle question, je ferai de mon mieux !";
        }

        // Questions mathématiques simples
        if (msgNorm.matches(".*\\d+\\s*[+\\-*/x×÷]\\s*\\d+.*")) {
            return genererReponseMath(message);
        }

        // Questions météo
        if (msgNorm.contains("meteo") || msgNorm.contains("temps") || msgNorm.contains("pluie") ||
                msgNorm.contains("soleil") || msgNorm.contains("temperature")) {
            return "Pour la météo en temps réel, je vous recommande de consulter :\n\n" +
                    "🌐 **meteotunisie.com** ou **weather.com**\n\n" +
                    "Je n'ai pas accès aux données météo en direct, mais je peux vous aider sur d'autres sujets ! 😊";
        }

        // Questions sur l'heure / date
        if (msgNorm.contains("heure") || msgNorm.contains("date") || msgNorm.contains("aujourd") ||
                msgNorm.contains("quel jour") || msgNorm.contains("quelle heure")) {
            return "📅 Nous sommes le " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    " et il est " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + ".\n\n" +
                    "Y a-t-il autre chose que je peux faire pour vous ?";
        }

        // Questions sur la Tunisie
        if (msgNorm.contains("tunisie") || msgNorm.contains("tunis") || msgNorm.contains("tunisien")) {
            return "La Tunisie est un pays d'Afrique du Nord, avec une population d'environ 12 millions d'habitants. 🇹🇳\n\n" +
                    "Capitale : Tunis | Monnaie : Dinar tunisien | Langue officielle : Arabe\n\n" +
                    "C'est un pays riche en histoire, avec notamment Carthage, les médinas classées UNESCO, " +
                    "et une belle côte méditerranéenne.\n\n" +
                    "Avez-vous une question plus spécifique sur la Tunisie ?";
        }

        // Blagues / humour
        if (msgNorm.contains("blague") || msgNorm.contains("fait moi rire") || msgNorm.contains("drole") ||
                msgNorm.contains("joke")) {
            String[] blagues = {
                    "Pourquoi les plongeurs plongent-ils toujours en arrière et jamais en avant ? 🤿\nParce que sinon, ils tomberaient dans le bateau !",
                    "Un homme entre dans une bibliothèque et demande : \"Avez-vous des livres sur la paranoia ?\"\nLa bibliothécaire chuchote : \"Ils sont juste derrière vous...\" 😄",
                    "Qu'est-ce qu'un canif ? 🐱\nUn petit fien ! (petit chien... en verlan félin !)"
            };
            return blagues[new Random().nextInt(blagues.length)] + "\n\nJ'espère que cela vous a fait sourire ! 😊";
        }

        // Recettes / cuisine
        if (msgNorm.contains("recette") || msgNorm.contains("cuisine") || msgNorm.contains("cuire") ||
                msgNorm.contains("preparer") || msgNorm.contains("couscous") || msgNorm.contains("brick")) {
            return "Je peux vous donner des conseils de cuisine ! 🍳\n\n" +
                    "Pour des recettes détaillées avec photos et étapes précises, je recommande :\n" +
                    "• **choumicha.com** — recettes maghrébines\n" +
                    "• **marmiton.org** — cuisine française et internationale\n\n" +
                    "Dites-moi quel plat vous souhaitez préparer et je vous donnerai les grandes lignes !";
        }

        // Réponse générique intelligente pour les questions non reconnues
        if (conversationHistory.size() > 2) {
            return "C'est une belle question ! 💡\n\n" +
                    "Malheureusement, en mode hors-ligne, mes connaissances générales sont limitées. " +
                    "Pour une réponse précise, je vous conseille de :\n" +
                    "• Consulter **Google** ou **Wikipedia**\n" +
                    "• Ou configurer une clé API OpenAI pour activer mon mode intelligent complet\n\n" +
                    "Je reste disponible pour tout ce qui touche à votre bien-être et santé mentale ! 💙";
        }

        return "Bonjour ! Je suis MindAura, votre assistant polyvalent. 👋\n\n" +
                "Je peux répondre à vos questions sur la santé mentale, la culture générale, la science, " +
                "ou simplement discuter avec vous.\n\n" +
                "Que puis-je faire pour vous aujourd'hui ?";
    }

    /** Tente de résoudre une opération mathématique simple dans le message */
    private String genererReponseMath(String message) {
        try {
            String expr = message.replaceAll("[^0-9+\\-*/x×÷().]", " ").trim()
                    .replace("x", "*").replace("×", "*").replace("÷", "/");
            String[] parts = expr.trim().split("\\s+");
            if (parts.length >= 3) {
                double a = Double.parseDouble(parts[0]);
                String op = parts[1];
                double b = Double.parseDouble(parts[2]);
                double result;
                switch (op) {
                    case "+": result = a + b; break;
                    case "-": result = a - b; break;
                    case "*": result = a * b; break;
                    case "/":
                        if (b == 0) return "⚠️ Division par zéro impossible !";
                        result = a / b; break;
                    default: return "Je n'ai pas pu interpréter cette opération. Essayez par exemple : 15 + 27";
                }
                String res = result == (long) result ? String.valueOf((long) result) : String.valueOf(result);
                return "🔢 " + (long)a + " " + op + " " + (long)b + " = **" + res + "**\n\nY a-t-il autre chose que je peux calculer pour vous ?";
            }
        } catch (Exception ignored) {}
        return "Je n'ai pas pu résoudre ce calcul. Essayez d'écrire : 15 + 27 ou 100 / 4 😊";
    }

    /**
     * Combine plusieurs heuristiques pour une compréhension robuste.
     *
     * @return score entre 0.0 (aucun lien) et 1.0 (identique)
     */
    private double scoreToken(String token, String motCle) {
        // ── 1. Identité exacte ──────────────────────────────────────────────
        if (token.equals(motCle)) return 1.0;

        // ── 2. Préfixe : mot incomplet (ex: "anxi" → "anxieux") ─────────────
        //    On accepte si le token couvre ≥ 60% du mot-clé
        if (motCle.startsWith(token) && token.length() >= Math.max(3, motCle.length() * 0.55)) {
            return 0.90;
        }
        if (token.startsWith(motCle)) return 0.88; // saisie plus longue que le mot-clé

        // ── 3. Contenance mutuelle ───────────────────────────────────────────
        if (motCle.contains(token) && token.length() >= 4) return 0.82;
        if (token.contains(motCle) && motCle.length() >= 4) return 0.80;

        // ── 4. Dédoublement de lettres (ex: "stresss" → "stress") ────────────
        String tokenDedouble = dedoublerLettres(token);
        String motDedouble   = dedoublerLettres(motCle);
        if (tokenDedouble.equals(motDedouble)) return 0.92;
        if (motDedouble.startsWith(tokenDedouble)
                && tokenDedouble.length() >= Math.max(3, motDedouble.length() * 0.55)) {
            return 0.85;
        }

        // ── 5. Distance de Levenshtein (fautes de frappe) ────────────────────
        int lenMin = Math.min(token.length(), motCle.length());
        int lenMax = Math.max(token.length(), motCle.length());

        // Seuil de distance acceptable selon la longueur du mot
        int distMax = lenMin <= 3 ? 1 : lenMin <= 5 ? 2 : 3;
        int dist = levenshtein(token, motCle);

        if (dist <= distMax) {
            // Score décroissant avec la distance
            double score = 1.0 - ((double) dist / (lenMax + 1));
            return Math.max(score, 0.63);
        }

        // Idem sur les versions dédoublées
        int distD = levenshtein(tokenDedouble, motDedouble);
        if (distD <= distMax) {
            double score = 1.0 - ((double) distD / (Math.max(tokenDedouble.length(), motDedouble.length()) + 1));
            return Math.max(score, 0.63);
        }

        // ── 6. Phonétique simplifiée (Soundex français) ──────────────────────
        if (soundexFr(token).equals(soundexFr(motCle))) return 0.72;

        // ── 7. Bigrammes communs ──────────────────────────────────────────────
        double jaro = similariteJaro(token, motCle);
        if (jaro >= 0.88) return jaro * 0.90;

        return 0.0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ALGORITHMES DE SIMILARITÉ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Supprime les répétitions consécutives de lettres.
     * Ex: "stresssss" → "stres", "anxxiete" → "anxiete"
     */
    private String dedoublerLettres(String s) {
        if (s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        sb.append(s.charAt(0));
        for (int i = 1; i < s.length(); i++) {
            if (s.charAt(i) != s.charAt(i - 1)) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * Distance de Levenshtein (édition minimale entre deux chaînes).
     * Gère les fautes de frappe : substitution, insertion, suppression.
     */
    private int levenshtein(String a, String b) {
        int la = a.length(), lb = b.length();
        int[][] dp = new int[la + 1][lb + 1];
        for (int i = 0; i <= la; i++) dp[i][0] = i;
        for (int j = 0; j <= lb; j++) dp[0][j] = j;
        for (int i = 1; i <= la; i++) {
            for (int j = 1; j <= lb; j++) {
                int cout = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(dp[i - 1][j - 1] + cout,
                        Math.min(dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1));
            }
        }
        return dp[la][lb];
    }

    /**
     * Similarité de Jaro — efficace pour les petites chaînes.
     */
    private double similariteJaro(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        int l1 = s1.length(), l2 = s2.length();
        if (l1 == 0 || l2 == 0) return 0.0;

        int fenetre = Math.max(l1, l2) / 2 - 1;
        if (fenetre < 0) fenetre = 0;

        boolean[] m1 = new boolean[l1];
        boolean[] m2 = new boolean[l2];
        int matches = 0;

        for (int i = 0; i < l1; i++) {
            int debut = Math.max(0, i - fenetre);
            int fin   = Math.min(i + fenetre + 1, l2);
            for (int j = debut; j < fin; j++) {
                if (!m2[j] && s1.charAt(i) == s2.charAt(j)) {
                    m1[i] = m2[j] = true;
                    matches++;
                    break;
                }
            }
        }
        if (matches == 0) return 0.0;

        int transpositions = 0;
        int k = 0;
        for (int i = 0; i < l1; i++) {
            if (m1[i]) {
                while (!m2[k]) k++;
                if (s1.charAt(i) != s2.charAt(k)) transpositions++;
                k++;
            }
        }
        return (matches / (double) l1 + matches / (double) l2
                + (matches - transpositions / 2.0) / matches) / 3.0;
    }

    /**
     * Soundex français simplifié — regroupe les sons phonétiques proches.
     * Ex: "stresé" et "stresse" donnent le même code.
     */
    private String soundexFr(String s) {
        if (s.isEmpty()) return "";
        s = s.toUpperCase()
                .replace("PH","F").replace("GN","N")
                .replace("QU","K").replace("CH","S")
                .replace("AI","E").replace("AU","O")
                .replace("EAU","O").replace("OU","U")
                .replace("OI","O").replace("EI","E");

        Map<Character, Character> codes = new HashMap<>();
        for (char c : "BPFV".toCharArray())  codes.put(c, '1');
        for (char c : "CGJKQSXYZ".toCharArray()) codes.put(c, '2');
        for (char c : "DT".toCharArray())    codes.put(c, '3');
        for (char c : "L".toCharArray())     codes.put(c, '4');
        for (char c : "MN".toCharArray())    codes.put(c, '5');
        for (char c : "R".toCharArray())     codes.put(c, '6');

        StringBuilder code = new StringBuilder();
        code.append(s.charAt(0));
        char dernierCode = codes.getOrDefault(s.charAt(0), '0');

        for (int i = 1; i < s.length() && code.length() < 4; i++) {
            char c = s.charAt(i);
            char cd = codes.getOrDefault(c, '0');
            if (cd != '0' && cd != dernierCode) {
                code.append(cd);
                dernierCode = cd;
            } else if ("AEIOU".indexOf(c) >= 0) {
                dernierCode = '0'; // voyelle réinitialise
            }
        }
        while (code.length() < 4) code.append('0');
        return code.toString();
    }

    /** Normalise une chaîne : minuscules + suppression des accents */
    private String normaliser(String s) {
        return s.toLowerCase()
                .replace("é","e").replace("è","e").replace("ê","e").replace("ë","e")
                .replace("à","a").replace("â","a").replace("ä","a")
                .replace("î","i").replace("ï","i")
                .replace("ô","o").replace("ö","o")
                .replace("ù","u").replace("û","u").replace("ü","u")
                .replace("ç","c").replace("œ","oe").replace("æ","ae");
    }

    // =====================================================
    //  AFFICHAGE DES MESSAGES
    // =====================================================

    private void afficherMessageUtilisateur(String texte) {
        HBox cont = new HBox();
        cont.setAlignment(Pos.CENTER_RIGHT);
        cont.setPadding(new Insets(0, 0, 0, 80));
        cont.getChildren().add(creerBulle(texte, "#1B4332", "white", "#2D6A4F", true));
        ajouterMessage(cont);
    }

    private void afficherMessageBot(String texte) {
        HBox cont = new HBox(10);
        cont.setAlignment(Pos.CENTER_LEFT);
        cont.setPadding(new Insets(0, 80, 0, 0));

        // Avatar avec logo MindAura
        StackPane avatar = new StackPane();
        avatar.setStyle("-fx-background-color: #2D6A4F; -fx-background-radius: 50px; " +
                "-fx-min-width: 36px; -fx-min-height: 36px; " +
                "-fx-max-width: 36px; -fx-max-height: 36px;");
        avatar.setAlignment(Pos.TOP_CENTER);

        try {
            javafx.scene.image.Image logo = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/mindaura_logo.png"),
                    26, 26, true, true);
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logo);
            logoView.setFitWidth(26);
            logoView.setFitHeight(26);
            logoView.setPreserveRatio(true);
            avatar.getChildren().add(logoView);
        } catch (Exception e) {
            // Fallback texte si l'image est introuvable
            Label ico = new Label("M");
            ico.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: white;");
            avatar.getChildren().add(ico);
        }

        cont.getChildren().addAll(avatar, creerBulle(texte, "white", "#1A1A2E", "#E8F0EC", false));
        ajouterMessage(cont);
    }

    private VBox creerBulle(String texte, String bg, String fg, String border, boolean droite) {
        String radius = droite ? "18px 4px 18px 18px" : "4px 18px 18px 18px";
        VBox bulle = new VBox(4);
        bulle.setMaxWidth(520);
        bulle.setStyle(
                "-fx-background-color:" + bg + ";" +
                        "-fx-background-radius:" + radius + ";" +
                        "-fx-padding:12px 16px;" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-border-width:1px;" +
                        "-fx-border-radius:" + radius + ";" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);"
        );

        Label txt = new Label(texte);
        txt.setStyle("-fx-font-size:13px;-fx-text-fill:" + fg + ";-fx-line-spacing:3px;");
        txt.setWrapText(true);
        txt.setMaxWidth(500);

        Label heure = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        heure.setStyle("-fx-font-size:9px;-fx-text-fill:" +
                (droite ? "rgba(255,255,255,0.6)" : "#9CA3AF") + ";");
        heure.setAlignment(droite ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        bulle.getChildren().addAll(txt, heure);
        return bulle;
    }

    private void ajouterMessage(HBox cont) {
        FadeTransition fade = new FadeTransition(Duration.millis(300), cont);
        fade.setFromValue(0);
        fade.setToValue(1);
        messagesContainer.getChildren().add(cont);
        fade.play();
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void showTypingIndicator(boolean visible) {
        typingIndicator.setVisible(visible);
        typingIndicator.setManaged(visible);
        if (visible) Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    // =====================================================
    //  SUGGESTIONS RAPIDES
    // =====================================================

    @FXML
    private void handleSuggestion(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        // Extraire le texte après l'emoji
        String texte = btn.getText().replaceAll("^\\S+\\s*", "").trim();
        if (texte.isEmpty()) texte = btn.getText().trim();
        inputField.setText(texte);
        handleEnvoyer();
    }

    // =====================================================
    //  NOUVELLE CONVERSATION / FERMETURE
    // =====================================================

    @FXML
    private void handleNouvelleConversation() {
        new Alert(Alert.AlertType.CONFIRMATION,
                "L'historique de cette conversation sera effacé.",
                ButtonType.OK, ButtonType.CANCEL) {{
            setTitle("Nouvelle conversation");
            setHeaderText("Recommencer ?");
        }}.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                conversationHistory.clear();
                messagesContainer.getChildren().clear();
                afficherMessageBot("Bonjour à nouveau ! 👋 Je suis prêt à vous écouter.\nComment puis-je vous aider aujourd'hui ? 💙");
            }
        });
    }

    @FXML
    private void handleFermer() {
        // Si chargé dans UserHome, retourner à FrontOfficeAccueil dans le même centre
        if (parentHomeController != null) {
            parentHomeController.loadFrontOfficeAccueil();
            return;
        }
        ((Stage) btnEnvoyer.getScene().getWindow()).close();
    }
}