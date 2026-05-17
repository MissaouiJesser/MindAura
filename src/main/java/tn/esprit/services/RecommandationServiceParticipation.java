package tn.esprit.services;

import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class RecommandationServiceParticipation {

    private final EvenementService     evenementService     = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();

    private static final double POIDS_TYPE_PREFERE  = 0.40;
    private static final double POIDS_LIEU_PROCHE   = 0.20;
    private static final double POIDS_DISPONIBILITE = 0.25;
    private static final double POIDS_POPULARITE    = 0.15;

    public record EvenementScore(
            Evenements evenement,
            double     score,
            String     raisonPrincipale,
            boolean    dejaInscrit
    ) {
        public EvenementScore(Evenements evenement, double score, String raisonPrincipale) {
            this(evenement, score, raisonPrincipale, false);
        }
    }

    /**
     * Recommandations personnalisées pour un utilisateur identifié par son email.
     * EXCLUT les événements auxquels l'utilisateur est déjà inscrit.
     */
    public List<EvenementScore> recommendationsForUser(String userEmail, int topN) throws SQLException {
        List<Evenements>    tousEvenements = evenementService.getAllEvenements();
        List<Participation> historique     = getHistoriqueUtilisateur(userEmail);

        Map<String, Long> typesPreferes = analyserTypesPreferes(historique, tousEvenements);
        Set<String>       lieuxVisites  = analyserLieuxVisites(historique, tousEvenements);

        Set<Integer> evenementsInscrits = historique.stream()
                .map(Participation::getId_evenemnt)
                .collect(Collectors.toSet());

        List<EvenementScore> scores = new ArrayList<>();

        for (Evenements e : tousEvenements) {
            if ("Annulé".equalsIgnoreCase(e.getStatut_evenemnt())) continue;
            if (e.getCapacite_evenement() <= 0) continue;

            boolean dejaInscrit = evenementsInscrits.contains(e.getId_evenemnt());
            if (dejaInscrit) continue;

            double score = calculerScore(e, typesPreferes, lieuxVisites, tousEvenements);
            String raison = determinerRaison(e, typesPreferes, lieuxVisites);
            scores.add(new EvenementScore(e, score, raison, false));
        }

        if (scores.isEmpty()) return List.of();

        return scores.stream()
                .sorted(Comparator.comparingDouble(EvenementScore::score).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public List<EvenementScore> evenementsSimilaires(Evenements reference, int topN) throws SQLException {
        List<Evenements> tousEvenements = evenementService.getAllEvenements();

        return tousEvenements.stream()
                .filter(e -> e.getId_evenemnt() != reference.getId_evenemnt())
                .filter(e -> e.getCapacite_evenement() > 0)
                .filter(e -> !"Annulé".equalsIgnoreCase(e.getStatut_evenemnt()))
                .map(e -> {
                    double score = 0;
                    List<String> raisons = new ArrayList<>();

                    if (e.getTypeEvenement() != null
                            && e.getTypeEvenement().equalsIgnoreCase(reference.getTypeEvenement())) {
                        score += 50;
                        raisons.add("Même type (" + e.getTypeEvenement() + ")");
                    }
                    if (memeVille(e.getLieu_evenement(), reference.getLieu_evenement())) {
                        score += 30;
                        raisons.add("Même lieu");
                    }
                    if (periodProche(e, reference, 30)) {
                        score += 20;
                        raisons.add("Période proche");
                    }

                    String raison = raisons.isEmpty() ? "Événement disponible" : String.join(", ", raisons);
                    return new EvenementScore(e, score, raison, false);
                })
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingDouble(EvenementScore::score).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public List<EvenementScore> tendances(int topN) throws SQLException {
        List<Evenements>    tousEvenements = evenementService.getAllEvenements();
        List<Participation> toutesParticipations;

        try {
            toutesParticipations = participationService.recuperer();
        } catch (Exception e) {
            toutesParticipations = List.of();
        }

        Map<Integer, Long> countByEvent = toutesParticipations.stream()
                .collect(Collectors.groupingBy(Participation::getId_evenemnt, Collectors.counting()));

        return tousEvenements.stream()
                .filter(e -> !"Annulé".equalsIgnoreCase(e.getStatut_evenemnt()))
                .map(e -> {
                    long   nbPart  = countByEvent.getOrDefault(e.getId_evenemnt(), 0L);
                    int    initial = e.getCapacite_evenement() + (int) nbPart;
                    double taux    = initial > 0 ? (double) nbPart / initial * 100 : 0;
                    String raison  = nbPart > 0
                            ? nbPart + " inscription" + (nbPart > 1 ? "s" : "")
                            + " — " + String.format("%.0f", taux) + "% rempli"
                            : "Nouvel événement";
                    return new EvenementScore(e, taux, raison, false);
                })
                .sorted(Comparator.comparingDouble(EvenementScore::score).reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    /**
     * Profil utilisateur basé sur son email.
     */
    public String profilUtilisateur(String userEmail) throws SQLException {
        List<Participation> historique = getHistoriqueUtilisateur(userEmail);
        List<Evenements>    tous       = evenementService.getAllEvenements();
        Map<String, Long>   types      = analyserTypesPreferes(historique, tous);

        if (historique.isEmpty()) {
            return "Aucune participation — Profil vierge";
        }

        String typePreferee = types.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Divers");

        return String.format("%d participation%s — Type favori : %s",
                historique.size(),
                historique.size() > 1 ? "s" : "",
                typePreferee);
    }

    // --- Méthodes privées ---

    /**
     * Filtre les participations par email de l'utilisateur connecté.
     */
    private List<Participation> getHistoriqueUtilisateur(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) return List.of();
        try {
            return participationService.recuperer().stream()
                    .filter(p -> userEmail.equalsIgnoreCase(p.getEmail()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Long> analyserTypesPreferes(List<Participation> historique,
                                                    List<Evenements> tous) {
        Map<Integer, String> typeByEvent = tous.stream()
                .collect(Collectors.toMap(
                        Evenements::getId_evenemnt,
                        e -> e.getTypeEvenement() != null ? e.getTypeEvenement() : "Autre",
                        (a, b) -> a
                ));
        return historique.stream()
                .map(p -> typeByEvent.getOrDefault(p.getId_evenemnt(), "Autre"))
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));
    }

    private Set<String> analyserLieuxVisites(List<Participation> historique,
                                             List<Evenements> tous) {
        Map<Integer, String> lieuByEvent = tous.stream()
                .collect(Collectors.toMap(
                        Evenements::getId_evenemnt,
                        e -> e.getLieu_evenement() != null ? extraireVille(e.getLieu_evenement()) : "",
                        (a, b) -> a
                ));
        return historique.stream()
                .map(p -> lieuByEvent.getOrDefault(p.getId_evenemnt(), ""))
                .filter(l -> !l.isEmpty())
                .collect(Collectors.toSet());
    }

    private double calculerScore(Evenements e, Map<String, Long> typesPreferes,
                                 Set<String> lieuxVisites, List<Evenements> tous) {
        double score = 0;

        if (e.getTypeEvenement() != null) {
            long countType = typesPreferes.getOrDefault(e.getTypeEvenement(), 0L);
            long totalPref = typesPreferes.values().stream().mapToLong(Long::longValue).sum();
            if (totalPref > 0) {
                score += POIDS_TYPE_PREFERE * ((double) countType / totalPref) * 100;
            }
        }

        if (e.getLieu_evenement() != null) {
            String ville = extraireVille(e.getLieu_evenement());
            if (lieuxVisites.contains(ville)) {
                score += POIDS_LIEU_PROCHE * 100;
            }
        }

        int cap = Math.max(e.getCapacite_evenement(), 1);
        score += POIDS_DISPONIBILITE * Math.min((double) cap / 100, 1.0) * 100;

        if ("Actif".equalsIgnoreCase(e.getStatut_evenemnt())) {
            score += POIDS_POPULARITE * 100;
        } else if ("Planifié".equalsIgnoreCase(e.getStatut_evenemnt())) {
            score += POIDS_POPULARITE * 60;
        }

        return Math.min(score, 100.0);
    }

    private String determinerRaison(Evenements e, Map<String, Long> typesPreferes,
                                    Set<String> lieuxVisites) {
        List<String> raisons = new ArrayList<>();

        if (e.getTypeEvenement() != null && typesPreferes.containsKey(e.getTypeEvenement()))
            raisons.add("Type correspondant à vos préférences");
        if (e.getLieu_evenement() != null
                && lieuxVisites.contains(extraireVille(e.getLieu_evenement())))
            raisons.add("Lieu familier");
        if ("Actif".equalsIgnoreCase(e.getStatut_evenemnt()))
            raisons.add("Événement actif");
        if (raisons.isEmpty())
            raisons.add("Disponible et recommandé");

        return String.join(" · ", raisons);
    }

    private boolean memeVille(String lieu1, String lieu2) {
        if (lieu1 == null || lieu2 == null) return false;
        return extraireVille(lieu1).equalsIgnoreCase(extraireVille(lieu2));
    }

    private String extraireVille(String lieu) {
        if (lieu == null) return "";
        return lieu.split("[,\\-]")[0].trim().toLowerCase();
    }

    private boolean periodProche(Evenements a, Evenements b, int joursMax) {
        if (a.getDatedebut_evenemnt() == null || b.getDatedebut_evenemnt() == null) return false;
        long diffMs     = Math.abs(a.getDatedebut_evenemnt().getTime() - b.getDatedebut_evenemnt().getTime());
        long joursEcart = diffMs / (1000L * 60 * 60 * 24);
        return joursEcart <= joursMax;
    }
}