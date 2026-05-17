package tn.esprit.services;

import tn.esprit.entities.Ressources;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 🧠 SERVICE DE RECOMMANDATION HYBRIDE
 *
 * Combine deux stratégies :
 * ┌─────────────────────────────────────────────────────────┐
 * │  1. CONTENT-BASED  → ressources similaires au contenu   │
 * │     consulté (même tags, catégorie, niveau, type)       │
 * │                                                         │
 * │  2. POPULARITÉ     → ressources les plus vues           │
 * │     pondérées par fraîcheur et score global             │
 * │                                                         │
 * │  SCORE HYBRIDE = 0.65 × content_score + 0.35 × pop_score│
 * └─────────────────────────────────────────────────────────┘
 */
public class RecommandationService {

    private static final double POIDS_CONTENT    = 0.65;
    private static final double POIDS_POPULARITE = 0.35;

    private static final double POIDS_TAG       = 0.40;
    private static final double POIDS_CATEGORIE = 0.25;
    private static final double POIDS_NIVEAU    = 0.20;
    private static final double POIDS_TYPE      = 0.15;

    private static final int MAX_RECOMMANDATIONS = 6;

    // ═════════════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE PRINCIPAL
    // ═════════════════════════════════════════════════════════════════════════

    public List<RessourceScore> recommander(
            Ressources reference,
            List<Ressources> favoris,
            List<Ressources> toutesRessources) {

        Set<Integer> exclusions = new HashSet<>();
        if (reference != null) exclusions.add(reference.getId_ressources());
        favoris.forEach(f -> exclusions.add(f.getId_ressources()));

        List<Ressources> candidats = toutesRessources.stream()
                .filter(r -> !exclusions.contains(r.getId_ressources()))
                .collect(Collectors.toList());

        if (candidats.isEmpty()) return Collections.emptyList();

        double maxVues = toutesRessources.stream()
                .mapToInt(Ressources::getNbr_vues).max().orElse(1);

        ProfilUtilisateur profil = construireProfil(favoris, reference);

        List<RessourceScore> scores = new ArrayList<>();
        for (Ressources candidat : candidats) {
            double scoreContent    = calculerScoreContent(candidat, profil, reference);
            double scorePopularite = calculerScorePopularite(candidat, maxVues);
            double scoreHybride    = POIDS_CONTENT * scoreContent + POIDS_POPULARITE * scorePopularite;
            String raison = genererRaison(candidat, profil, reference, scoreContent, scorePopularite);
            scores.add(new RessourceScore(candidat, scoreHybride, scoreContent, scorePopularite, raison));
        }

        return scores.stream()
                .sorted(Comparator.comparingDouble(RessourceScore::getScore).reversed())
                .limit(MAX_RECOMMANDATIONS)
                .collect(Collectors.toList());
    }

    public List<RessourceScore> recommanderParPopularite(List<Ressources> toutesRessources) {
        if (toutesRessources.isEmpty()) return Collections.emptyList();

        double maxVues = toutesRessources.stream()
                .mapToInt(Ressources::getNbr_vues).max().orElse(1);

        return toutesRessources.stream()
                .map(r -> {
                    double pop = calculerScorePopularite(r, maxVues);
                    return new RessourceScore(r, pop, 0.0, pop, "Populaire dans la communauté");
                })
                .sorted(Comparator.comparingDouble(RessourceScore::getScore).reversed())
                .limit(MAX_RECOMMANDATIONS)
                .collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  PROFIL UTILISATEUR (à partir des favoris + ressource consultée)
    // ═════════════════════════════════════════════════════════════════════════

    private ProfilUtilisateur construireProfil(List<Ressources> favoris, Ressources reference) {
        Map<String, Double> prefTags       = new HashMap<>();
        Map<String, Double> prefCategories = new HashMap<>();
        Map<String, Double> prefNiveaux    = new HashMap<>();
        Map<String, Double> prefTypes      = new HashMap<>();

        List<Ressources> sources = new ArrayList<>(favoris);
        if (reference != null) { sources.add(reference); sources.add(reference); } // double poids

        for (Ressources r : sources) {
            // Tags : on split la chaîne CSV
            extraireTags(r).forEach(t -> prefTags.merge(t.toLowerCase().trim(), 1.0, Double::sum));

            // Catégorie (String)
            if (r.getCategorie() != null && !r.getCategorie().isEmpty())
                prefCategories.merge(r.getCategorie(), 1.0, Double::sum);

            // Niveau (String)
            if (r.getNiveau() != null && !r.getNiveau().isEmpty())
                prefNiveaux.merge(r.getNiveau(), 1.0, Double::sum);

            // Type de contenu (String)
            if (r.getContenu() != null && !r.getContenu().isEmpty())
                prefTypes.merge(r.getContenu(), 1.0, Double::sum);
        }

        normaliser(prefTags);
        normaliser(prefCategories);
        normaliser(prefNiveaux);
        normaliser(prefTypes);

        return new ProfilUtilisateur(prefTags, prefCategories, prefNiveaux, prefTypes);
    }

    /** Convertit le champ tags (String CSV) en liste de tags individuels */
    private List<String> extraireTags(Ressources r) {
        if (r.getTags() == null || r.getTags().isBlank()) return Collections.emptyList();
        return Arrays.stream(r.getTags().split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .collect(Collectors.toList());
    }

    private void normaliser(Map<String, Double> map) {
        double total = map.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total > 0) map.replaceAll((k, v) -> v / total);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CALCUL DES SCORES
    // ═════════════════════════════════════════════════════════════════════════

    private double calculerScoreContent(Ressources r, ProfilUtilisateur profil, Ressources reference) {
        double score = 0.0;

        // Tags
        double scoreTags = 0.0;
        if (r.getTags() != null && !profil.prefTags.isEmpty()) {
            List<String> tagsRessource = extraireTags(r);
            for (String tag : tagsRessource) {
                scoreTags += profil.prefTags.getOrDefault(tag.toLowerCase().trim(), 0.0);
            }
            scoreTags = Math.min(1.0, scoreTags);
        }
        score += POIDS_TAG * scoreTags;

        // Catégorie
        if (r.getCategorie() != null && !r.getCategorie().isEmpty())
            score += POIDS_CATEGORIE * profil.prefCategories.getOrDefault(r.getCategorie(), 0.0);

        // Niveau
        if (r.getNiveau() != null && !r.getNiveau().isEmpty()) {
            double scoreNiveau = profil.prefNiveaux.getOrDefault(r.getNiveau(), 0.0);
            // Bonus si même niveau que la référence consultée
            if (reference != null && reference.getNiveau() != null
                    && reference.getNiveau().equalsIgnoreCase(r.getNiveau())) {
                scoreNiveau = Math.min(1.0, scoreNiveau + 0.2);
            }
            score += POIDS_NIVEAU * scoreNiveau;
        }

        // Type de contenu
        if (r.getContenu() != null && !r.getContenu().isEmpty())
            score += POIDS_TYPE * profil.prefTypes.getOrDefault(r.getContenu(), 0.0);

        return Math.min(1.0, score);
    }

    private double calculerScorePopularite(Ressources r, double maxVues) {
        double scoreVues = maxVues > 0 ? (double) r.getNbr_vues() / maxVues : 0.0;
        double bonusViral = r.getNbr_vues() > 1000 ? 0.1 : 0.0;
        return Math.min(1.0, scoreVues + bonusViral);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RAISON LISIBLE (explication pour l'UI)
    // ═════════════════════════════════════════════════════════════════════════

    private String genererRaison(Ressources r, ProfilUtilisateur profil,
                                 Ressources reference, double scoreContent, double scorePopularite) {
        // Tags communs avec la référence
        if (reference != null && r.getTags() != null && reference.getTags() != null) {
            List<String> tagsRef = extraireTags(reference);
            List<String> tagsCand = extraireTags(r);
            Optional<String> tagCommun = tagsCand.stream()
                    .filter(t -> tagsRef.stream().anyMatch(rt -> rt.equalsIgnoreCase(t)))
                    .findFirst();
            if (tagCommun.isPresent()) return "Tag similaire · " + tagCommun.get();
        }
        // Même catégorie
        if (reference != null && r.getCategorie() != null && reference.getCategorie() != null
                && r.getCategorie().equalsIgnoreCase(reference.getCategorie())) {
            return "Même thématique · " + r.getCategorie().replace("_", " ");
        }
        // Très populaire
        if (scorePopularite > 0.7) {
            return "Très populaire · " + r.getNbr_vues() + " vues";
        }
        // Basé sur les favoris
        if (scoreContent > 0.5) return "Selon vos favoris";
        // Même niveau
        if (reference != null && r.getNiveau() != null && reference.getNiveau() != null
                && r.getNiveau().equalsIgnoreCase(reference.getNiveau())) {
            return "Niveau similaire · " + r.getNiveau();
        }
        return "Recommandé pour vous";
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CLASSES INTERNES
    // ═════════════════════════════════════════════════════════════════════════

    public static class RessourceScore {
        private final Ressources ressource;
        private final double score;
        private final double scoreContent;
        private final double scorePopularite;
        private final String raison;

        public RessourceScore(Ressources r, double score, double sc, double sp, String raison) {
            this.ressource        = r;
            this.score            = score;
            this.scoreContent     = sc;
            this.scorePopularite  = sp;
            this.raison           = raison;
        }

        public Ressources getRessource()       { return ressource; }
        public double     getScore()           { return score; }
        public double     getScoreContent()    { return scoreContent; }
        public double     getScorePopularite() { return scorePopularite; }
        public String     getRaison()          { return raison; }
        public int        getMatchPourcent()   { return (int) Math.round(score * 100); }
    }

    private static class ProfilUtilisateur {
        final Map<String, Double> prefTags;
        final Map<String, Double> prefCategories;
        final Map<String, Double> prefNiveaux;
        final Map<String, Double> prefTypes;

        ProfilUtilisateur(Map<String, Double> tags, Map<String, Double> cats,
                          Map<String, Double> niveaux, Map<String, Double> types) {
            this.prefTags       = tags;
            this.prefCategories = cats;
            this.prefNiveaux    = niveaux;
            this.prefTypes      = types;
        }
    }
}