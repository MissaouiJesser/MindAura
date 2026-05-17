package tn.esprit.services;

import tn.esprit.entities.Evenements;
import tn.esprit.entities.Participation;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


public class StatistiquesService {

    private final Connection conx;
    private final EvenementService   evenementService   = new EvenementService();
    private final ParticipationService participationService = new ParticipationService();

    public StatistiquesService() {
        conx = MyDataBase.getInstance().getConx();
    }


    public record StatEvenement(
            int    idEvenement,
            String titre,
            String type,
            String statut,
            int    capaciteInitiale,
            int    capaciteRestante,
            long   nbParticipants,
            long   nbConfirmes,
            double tauxRemplissage
    ) {}


    public record RapportGlobal(
            int    totalEvenements,
            int    totalParticipants,
            int    totalConfirmes,
            double tauxConfirmation,
            double tauxRemplissageMoyen,
            Map<String, Long>   participantParType,
            Map<String, Long>   participantParMois,
            List<StatEvenement> top5Evenements,
            List<StatEvenement> evenementsComplets,
            int    totalPlacesDisponibles
    ) {}


    public StatEvenement getStatsEvenement(Evenements e) {
        long nbPart = 0, nbConf = 0;
        try {
            nbPart = countParticipantsByEvent(e.getId_evenemnt());
            nbConf = countConfirmesByEvent(e.getId_evenemnt());
        } catch (SQLException ex) { ex.printStackTrace(); }


        int initial = e.getCapacite_evenement() + (int) nbPart;
        double taux = (initial > 0) ? (double) nbPart / initial : 0.0;

        return new StatEvenement(
                e.getId_evenemnt(),
                e.getTitre_evenement(),
                e.getTypeEvenement(),
                e.getStatut_evenemnt(),
                initial,
                e.getCapacite_evenement(),
                nbPart,
                nbConf,
                taux
        );
    }


    public RapportGlobal genererRapportGlobal() throws SQLException {
        List<Evenements>    evenements    = evenementService.getAllEvenements();
        List<Participation> participations;
        try {
            participations = participationService.recuperer();
        } catch (Exception ex) {
            participations = List.of();
        }

        int totalEv   = evenements.size();
        int totalPart = participations.size();
        long totalConf = participations.stream()
                .filter(p -> p.getStatutParticipation() != null
                          && p.getStatutParticipation().toLowerCase().contains("confirm"))
                .count();

        double tauxConf = totalPart > 0 ? (double) totalConf / totalPart * 100 : 0;


        List<StatEvenement> statsEvs = evenements.stream()
                .map(this::getStatsEvenement)
                .collect(Collectors.toList());

        double tauxRemplissageMoyen = statsEvs.stream()
                .mapToDouble(StatEvenement::tauxRemplissage)
                .average().orElse(0) * 100;


        List<StatEvenement> top5 = statsEvs.stream()
                .sorted(Comparator.comparingLong(StatEvenement::nbParticipants).reversed())
                .limit(5)
                .collect(Collectors.toList());


        List<StatEvenement> complets = statsEvs.stream()
                .filter(s -> s.capaciteRestante() <= 0)
                .collect(Collectors.toList());


        Map<String, Long> parType = participations.stream()
                .collect(Collectors.groupingBy(
                        p -> getTypeForParticipation(evenements, p.getId_evenemnt()),
                        Collectors.counting()
                ));


        Map<String, Long> parMois = participations.stream()
                .filter(p -> p.getDateInscription() != null)
                .collect(Collectors.groupingBy(
                        p -> {
                            Calendar c = Calendar.getInstance();
                            c.setTime(p.getDateInscription());
                            return c.get(Calendar.YEAR) + "-"
                                 + String.format("%02d", c.get(Calendar.MONTH) + 1);
                        },
                        TreeMap::new,
                        Collectors.counting()
                ));

        int totalPlaces = evenements.stream().mapToInt(Evenements::getCapacite_evenement).sum();

        return new RapportGlobal(
                totalEv, totalPart, (int) totalConf, tauxConf,
                tauxRemplissageMoyen, parType, parMois,
                top5, complets, totalPlaces
        );
    }


    public String exporterRapportTexte() throws SQLException {
        RapportGlobal r = genererRapportGlobal();
        StringBuilder sb = new StringBuilder();

        sb.append("═══════════════════════════════════════════════\n");
        sb.append("        RAPPORT EVENTURA — ").append(new java.util.Date()).append("\n");
        sb.append("═══════════════════════════════════════════════\n\n");

        sb.append("── RÉSUMÉ GLOBAL ──────────────────────────────\n");
        sb.append(String.format("  Événements total    : %d\n", r.totalEvenements()));
        sb.append(String.format("  Participants total  : %d\n", r.totalParticipants()));
        sb.append(String.format("  Confirmés           : %d (%.1f %%)\n", r.totalConfirmes(), r.tauxConfirmation()));
        sb.append(String.format("  Taux remplissage moy: %.1f %%\n", r.tauxRemplissageMoyen()));
        sb.append(String.format("  Places disponibles  : %d\n\n", r.totalPlacesDisponibles()));

        sb.append("── TOP 5 ÉVÉNEMENTS ────────────────────────────\n");
        for (int i = 0; i < r.top5Evenements().size(); i++) {
            StatEvenement s = r.top5Evenements().get(i);
            sb.append(String.format("  %d. %-35s %d participants (%.0f %% plein)\n",
                    i + 1, s.titre(), s.nbParticipants(), s.tauxRemplissage() * 100));
        }

        sb.append("\n── ÉVÉNEMENTS COMPLETS ─────────────────────────\n");
        if (r.evenementsComplets().isEmpty()) {
            sb.append("  Aucun événement complet.\n");
        } else {
            r.evenementsComplets().forEach(s ->
                sb.append("  • ").append(s.titre()).append("\n")
            );
        }

        sb.append("\n── PARTICIPANTS PAR TYPE ───────────────────────\n");
        r.participantParType().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("  %-20s : %d\n", e.getKey(), e.getValue())));

        sb.append("\n── ÉVOLUTION MENSUELLE ─────────────────────────\n");
        r.participantParMois().forEach((mois, count) ->
                sb.append(String.format("  %s : %d inscriptions\n", mois, count)));

        sb.append("\n═══════════════════════════════════════════════\n");
        return sb.toString();
    }



    private long countParticipantsByEvent(int idEvenement) throws SQLException {
        PreparedStatement ps = conx.prepareStatement(
                "SELECT COUNT(*) FROM mindaura.participation WHERE id_evenemnt=?");
        ps.setInt(1, idEvenement);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getLong(1) : 0;
    }

    private long countConfirmesByEvent(int idEvenement) throws SQLException {
        PreparedStatement ps = conx.prepareStatement(
                "SELECT COUNT(*) FROM mindaura.participation WHERE id_evenemnt=? AND statutParticipation LIKE ?");
        ps.setInt(1, idEvenement);
        ps.setString(2, "%confirm%");
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getLong(1) : 0;
    }

    private String getTypeForParticipation(List<Evenements> evs, int idEvenement) {
        return evs.stream()
                .filter(e -> e.getId_evenemnt() == idEvenement)
                .map(e -> e.getTypeEvenement() != null ? e.getTypeEvenement() : "Autre")
                .findFirst()
                .orElse("Inconnu");
    }
}
