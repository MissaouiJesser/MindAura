import org.junit.jupiter.api.*;
import tn.esprit.enums.StatutReservation;
import tn.esprit.enums.MotifReservation;
import tn.esprit.enums.typeL;
import tn.esprit.services.reservation_local_SERVICE;
import tn.esprit.services.local_psychiatrie_SERVICE;
import tn.esprit.entities.reservation_local;
import tn.esprit.entities.local_psychiatrie;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TESTS UNITAIRES - SERVICE RÉSERVATION
 *
 * Teste les opérations CRUD sur l'entité reservation_local :
 * - Ajout d'une réservation
 * - Affichage de la liste
 * - Modification d'une réservation
 * - Suppression d'une réservation
 *
 * @author ESPRIT
 * @version 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceReservationTest {

    static reservation_local_SERVICE reservationService;
    static local_psychiatrie_SERVICE localService;
    static int idReservationTest;
    static int idLocalTest;

    @BeforeAll
    static void setup() throws SQLException {
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║   INITIALISATION DES TESTS - SERVICE RÉSERVATION      ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        reservationService = new reservation_local_SERVICE();
        localService = new local_psychiatrie_SERVICE();

        // Créer un local de test pour les réservations
        creerLocalDeTest();

        System.out.println("Services initialisés avec succès");
        System.out.println(" Local de test créé (ID: " + idLocalTest + ")");
    }

    /**
     * Crée un local de test pour les réservations
     */
    private static void creerLocalDeTest() throws SQLException {
        local_psychiatrie localTest = new local_psychiatrie(
                "Centre Test Réservations",
                "123 Rue de Test",
                "Tunis",
                "Local créé pour les tests unitaires",
                "20",
                typeL.HOPITAL,
                71234567,
                "test@reservation.tn",
                "Disponible",
                ""
        );

        localService.add(localTest);

        // Récupérer l'ID du local créé
        List<local_psychiatrie> locaux = localService.afficherList();
        idLocalTest = locaux.stream()
                .filter(l -> l.getNom_local().equals("Centre Test Réservations"))
                .findFirst()
                .get()
                .getId_local();
    }

    // ═══════════════════════════════════════════════════════════
    // TEST 1 : AJOUT D'UNE RÉSERVATION
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Test 1 : Ajout d'une réservation")
    void testAjouterReservation() throws SQLException {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│  TEST 1 : Ajout d'une réservation                      │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");

        // ARRANGE - Préparer les données
        LocalDate dateRes = LocalDate.now().plusDays(1);
        LocalTime heureDebut = LocalTime.of(9, 0);
        LocalTime heureFin = LocalTime.of(11, 0);

        Date dateReservation = Date.from(dateRes.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date heureDebutDate = Date.from(dateRes.atTime(heureDebut).atZone(ZoneId.systemDefault()).toInstant());
        Date heureFinDate = Date.from(dateRes.atTime(heureFin).atZone(ZoneId.systemDefault()).toInstant());

        reservation_local reservation = new reservation_local(
                0,                                    // ID (auto-généré)
                1,                                    // ID utilisateur
                idLocalTest,                          // ID local de test
                dateReservation,
                heureDebutDate,
                heureFinDate,
                StatutReservation.EN_ATTENTE,
                MotifReservation.CONSULTATION_INDIVIDUELLE,
                160,                                  // Prix (2h × 80 DT)
                "Dupont",                            // Nom client
                "Jean"                               // Prénom client
        );

        System.out.println(" Données de la réservation :");
        System.out.println("   Client : " + reservation.getNom_cl() + " " + reservation.getPrenom_cl());
        System.out.println("   Date : " + dateRes);
        System.out.println("   Horaire : " + heureDebut + " - " + heureFin);
        System.out.println("   Motif : " + reservation.getMotif_reservation().getLibelle());
        System.out.println("   Statut : " + reservation.getStatus_reservation().getLibelle());
        System.out.println("   Prix : " + reservation.getPrix_reservation() + " DT");

        // ACT - Ajouter la réservation
        reservationService.add(reservation);

        // ASSERT - Vérifier que la réservation a été ajoutée
        List<reservation_local> reservations = reservationService.afficherList();

        assertFalse(reservations.isEmpty(),
                "La liste des réservations ne devrait pas être vide après l'ajout");

        boolean reservationAjoutee = reservations.stream()
                .anyMatch(r -> r.getNom_cl().equals("Dupont") &&
                        r.getPrenom_cl().equals("Jean") &&
                        r.getId_local() == idLocalTest);

        assertTrue(reservationAjoutee,
                "La réservation de Jean Dupont devrait être dans la liste");

        // Récupérer l'ID de la réservation ajoutée
        idReservationTest = reservations.stream()
                .filter(r -> r.getNom_cl().equals("Dupont") &&
                        r.getPrenom_cl().equals("Jean") &&
                        r.getId_local() == idLocalTest)
                .findFirst()
                .get()
                .getId_reservation();

        System.out.println("\nRéservation ajoutée avec succès !");
        System.out.println("   ID Réservation : #" + idReservationTest);
        System.out.println("   Total réservations : " + reservations.size());
    }

    // ═══════════════════════════════════════════════════════════
    // TEST 2 : AFFICHAGE DE LA LISTE DES RÉSERVATIONS
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(2)
    @DisplayName("Test 2 : Affichage de la liste des réservations")
    void testAfficherListeReservations() throws SQLException {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│  TEST 2 : Affichage de la liste des réservations       │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");

        // ACT - Récupérer la liste complète
        List<reservation_local> reservations = reservationService.afficherList();

        // ASSERT - Vérifications
        assertNotNull(reservations, "La liste ne devrait pas être null");
        assertFalse(reservations.isEmpty(), "La liste ne devrait pas être vide");
        assertTrue(reservations.size() > 0, "Il devrait y avoir au moins 1 réservation");

        // Vérifier que la réservation de test est présente
        boolean reservationPresente = reservations.stream()
                .anyMatch(r -> r.getId_reservation() == idReservationTest);

        assertTrue(reservationPresente,
                "La réservation de test devrait être dans la liste");

        // Afficher les informations
        System.out.println("Liste affichée avec succès");
        System.out.println("Total de réservations : " + reservations.size());
        System.out.println("\nListe des réservations :\n");

        // Statistiques par statut
        long confirmees = reservations.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.CONFIRMEE)
                .count();
        long enAttente = reservations.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.EN_ATTENTE)
                .count();
        long annulees = reservations.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.ANNULEE)
                .count();
        long terminees = reservations.stream()
                .filter(r -> r.getStatus_reservation() == StatutReservation.TERMINEE)
                .count();

        System.out.println("    Confirmées : " + confirmees);
        System.out.println("    En attente : " + enAttente);
        System.out.println("    Annulées : " + annulees);
        System.out.println("    Terminées : " + terminees);

        System.out.println("\nDétail des réservations :\n");
        for (int i = 0; i < Math.min(5, reservations.size()); i++) {
            reservation_local r = reservations.get(i);
            System.out.println("   " + (i + 1) + ". Client : " + r.getNom_cl() + " " + r.getPrenom_cl() +
                    " | Statut : " + r.getStatus_reservation().getLibelle() +
                    " | Prix : " + r.getPrix_reservation() + " DT");
        }

        if (reservations.size() > 5) {
            System.out.println("   ... et " + (reservations.size() - 5) + " autres");
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TEST 3 : MODIFICATION D'UNE RÉSERVATION
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("Test 3 : Modification d'une réservation")
    void testModifierReservation() throws SQLException {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│  TEST 3 : Modification d'une réservation               │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");

        // ARRANGE - Récupérer la réservation à modifier
        List<reservation_local> reservations = reservationService.afficherList();
        reservation_local reservationAModifier = reservations.stream()
                .filter(r -> r.getId_reservation() == idReservationTest)
                .findFirst()
                .orElseThrow(() -> new AssertionError("La réservation à modifier n'a pas été trouvée"));

        System.out.println("    AVANT modification :");
        System.out.println("   Client : " + reservationAModifier.getNom_cl() + " " + reservationAModifier.getPrenom_cl());
        System.out.println("   Statut : " + reservationAModifier.getStatus_reservation().getLibelle());
        System.out.println("   Motif : " + reservationAModifier.getMotif_reservation().getLibelle());
        System.out.println("   Prix : " + reservationAModifier.getPrix_reservation() + " DT");

        // Modifier les données
        reservationAModifier.setNom_cl("Martin");
        reservationAModifier.setPrenom_cl("Sophie");
        reservationAModifier.setStatus_reservation(StatutReservation.CONFIRMEE);
        reservationAModifier.setMotif_reservation(MotifReservation.THERAPIE_DE_GROUPE);
        reservationAModifier.setPrix_reservation(200);

        // ACT - Modifier la réservation dans la base de données
        reservationService.modifier(reservationAModifier);

        // ASSERT - Vérifier que les modifications ont été appliquées
        List<reservation_local> reservationsApres = reservationService.afficherList();
        reservation_local reservationModifiee = reservationsApres.stream()
                .filter(r -> r.getId_reservation() == idReservationTest)
                .findFirst()
                .orElseThrow(() -> new AssertionError("La réservation modifiée n'a pas été trouvée"));

        assertEquals("Martin", reservationModifiee.getNom_cl(),
                "Le nom devrait être modifié");
        assertEquals("Sophie", reservationModifiee.getPrenom_cl(),
                "Le prénom devrait être modifié");
        assertEquals(StatutReservation.CONFIRMEE, reservationModifiee.getStatus_reservation(),
                "Le statut devrait être modifié");
        assertEquals(MotifReservation.THERAPIE_DE_GROUPE, reservationModifiee.getMotif_reservation(),
                "Le motif devrait être modifié");
        assertEquals(200, reservationModifiee.getPrix_reservation(),
                "Le prix devrait être modifié");

        // Vérifier que les données non modifiées restent identiques
        assertEquals(idLocalTest, reservationModifiee.getId_local(),
                "L'ID du local ne devrait pas changer");
        assertEquals(1, reservationModifiee.getId_utilisateur(),
                "L'ID utilisateur ne devrait pas changer");

        System.out.println("\n Réservation modifiée avec succès !");
        System.out.println("\n APRÈS modification :");
        System.out.println("   Client : " + reservationModifiee.getNom_cl() + " " + reservationModifiee.getPrenom_cl());
        System.out.println("   Statut : " + reservationModifiee.getStatus_reservation().getLibelle());
        System.out.println("   Motif : " + reservationModifiee.getMotif_reservation().getLibelle());
        System.out.println("   Prix : " + reservationModifiee.getPrix_reservation() + " DT");
    }

    // ═══════════════════════════════════════════════════════════
    // TEST 4 : CHANGEMENT DE STATUT
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(4)
    @DisplayName("Test 4 : Changement de statut d'une réservation")
    void testChangerStatutReservation() throws SQLException {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│  TEST 4 : Changement de statut                         │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");

        // ARRANGE - Récupérer la réservation
        List<reservation_local> reservations = reservationService.afficherList();
        reservation_local reservation = reservations.stream()
                .filter(r -> r.getId_reservation() == idReservationTest)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Réservation non trouvée"));

        StatutReservation ancienStatut = reservation.getStatus_reservation();
        System.out.println("Statut actuel : " + ancienStatut.getLibelle());

        // Test tous les changements de statut
        StatutReservation[] statuts = {
                StatutReservation.EN_ATTENTE,
                StatutReservation.CONFIRMEE,
                StatutReservation.TERMINEE,
                StatutReservation.ANNULEE
        };

        for (StatutReservation nouveauStatut : statuts) {
            System.out.println("\n  Changement : " + reservation.getStatus_reservation().getLibelle() +
                    " → " + nouveauStatut.getLibelle());

            reservation.setStatus_reservation(nouveauStatut);
            reservationService.modifier(reservation);

            // Vérifier le changement
            reservation_local reservationVerif = reservationService.afficherList().stream()
                    .filter(r -> r.getId_reservation() == idReservationTest)
                    .findFirst()
                    .get();

            assertEquals(nouveauStatut, reservationVerif.getStatus_reservation(),
                    "Le statut devrait être : " + nouveauStatut.getLibelle());

            System.out.println("  Statut mis à jour : " + reservationVerif.getStatus_reservation().getLibelle());
        }

        System.out.println("\n Tous les changements de statut fonctionnent correctement !");
    }

    // ═══════════════════════════════════════════════════════════
    // TEST 5 : CALCUL DU PRIX
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("Test 5 : Vérification du calcul de prix")
    void testCalculPrix() throws SQLException {
        System.out.println("\n┌─────────────────────────────────────────────────────────┐");
        System.out.println("│  TEST 5 : Vérification du calcul de prix               │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");

        // Récupérer la réservation de test
        List<reservation_local> reservations = reservationService.afficherList();
        reservation_local reservation = reservations.stream()
                .filter(r -> r.getId_reservation() == idReservationTest)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Réservation non trouvée"));

        // Calculer la durée
        long dureeMs = reservation.getHeure_fin_reservation().getTime() -
                reservation.getHeure_debut_reservation().getTime();
        long dureeHeures = dureeMs / (1000 * 60 * 60);

        System.out.println(" Durée de la réservation : " + dureeHeures + " heure(s)");
        System.out.println("Prix enregistré : " + reservation.getPrix_reservation() + " DT");

        // Vérifier que le prix est cohérent
        int prixAttendu = (int) (dureeHeures * 80);

        System.out.println("Prix attendu (base) : " + prixAttendu + " DT");

        assertTrue(reservation.getPrix_reservation() >= prixAttendu,
                "Le prix devrait être au moins égal au prix de base");

        System.out.println("\nCalcul de prix cohérent !");
    }

    // ═══════════════════════════════════════════════════════════
    // TEST 6 : SUPPRESSION D'UNE RÉSERVATION
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("Test 6 : Suppression d'une réservation")
    void testSupprimerReservation() throws SQLException {
        System.out.println("\n┌────────────────────────────────────────────────────────┐");
        System.out.println("│  TEST 6 : Suppression d'une réservation                │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");

        // ARRANGE - Récupérer le nombre de réservations avant suppression
        List<reservation_local> reservationsAvant = reservationService.afficherList();
        int nombreAvant = reservationsAvant.size();

        // Récupérer la réservation à supprimer
        reservation_local reservationASupprimer = reservationsAvant.stream()
                .filter(r -> r.getId_reservation() == idReservationTest)
                .findFirst()
                .orElseThrow(() -> new AssertionError("La réservation à supprimer n'a pas été trouvée"));

        System.out.println("  Réservation à supprimer :");
        System.out.println("   ID : #" + reservationASupprimer.getId_reservation());
        System.out.println("   Client : " + reservationASupprimer.getNom_cl() + " " +
                reservationASupprimer.getPrenom_cl());
        System.out.println("   Statut : " + reservationASupprimer.getStatus_reservation().getLibelle());
        System.out.println("\nNombre de réservations avant : " + nombreAvant);

        // ACT - Supprimer la réservation
        reservationService.delete(reservationASupprimer);

        // ASSERT - Vérifier que la réservation a été supprimée
        List<reservation_local> reservationsApres = reservationService.afficherList();
        int nombreApres = reservationsApres.size();

        // Vérifier que le nombre a diminué
        assertEquals(nombreAvant - 1, nombreApres,
                "Le nombre de réservations devrait diminuer de 1");

        // Vérifier que la réservation n'existe plus
        boolean reservationExiste = reservationsApres.stream()
                .anyMatch(r -> r.getId_reservation() == idReservationTest);

        assertFalse(reservationExiste,
                "La réservation ne devrait plus être dans la liste");

        System.out.println("\nRéservation supprimée avec succès !");
        System.out.println("Nombre de réservations après : " + nombreApres);
        System.out.println("Différence : " + (nombreAvant - nombreApres) + " réservation(s) supprimée(s)");
    }

    // ═══════════════════════════════════════════════════════════
    // NETTOYAGE APRÈS TOUS LES TESTS
    // ═══════════════════════════════════════════════════════════

    @AfterAll
    static void cleanup() throws SQLException {
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║  NETTOYAGE APRÈS LES TESTS                            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");

        // Supprimer le local de test
        List<local_psychiatrie> locaux = localService.afficherList();
        local_psychiatrie localTest = locaux.stream()
                .filter(l -> l.getId_local() == idLocalTest)
                .findFirst()
                .orElse(null);

        if (localTest != null) {
            localService.delete(localTest);
            System.out.println("Local de test supprimé (ID: " + idLocalTest + ")");
        }

        System.out.println("Nettoyage terminé\n");

        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║  TOUS LES TESTS TERMINÉS AVEC SUCCÈS                  ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
    }
}