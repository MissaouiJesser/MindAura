package tn.esprit.services;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.entities.NotificationItem;
import tn.esprit.entities.reservation_local;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Singleton gérant toutes les notifications in-app.
 *
 * Utilisation dans AfficherReservationController.initialize() :
 *   NotificationService.getInstance()
 *       .demarrer("jdbc:mysql://localhost:3306/mindaura", "root", "");
 *
 * Utilisation lors d'un ajout :
 *   NotificationService.getInstance().notifierNouvelleReservation(reservation);
 */
public class NotificationService {

    private static NotificationService instance;

    // Liste observable — l'UI s'y abonne directement
    private final ObservableList<NotificationItem> notifications =
            FXCollections.observableArrayList();

    // Callback appelé sur le FX thread à chaque nouvelle notif (pour mettre à jour le badge)
    private Consumer<Long> onChangement;

    // Scheduler daemon — s'arrête automatiquement à la fermeture de l'app
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "NotifScheduler");
                t.setDaemon(true);
                return t;
            });

    // Connexion DB pour la vérification 24h
    private String dbUrl, dbUser, dbPassword;

    private static final SimpleDateFormat DATE_FMT  = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat HEURE_FMT = new SimpleDateFormat("HH:mm");

    private NotificationService() {}

    public static synchronized NotificationService getInstance() {
        if (instance == null) instance = new NotificationService();
        return instance;
    }

    // ─── Démarrage ─────────────────────────────────────────────────────────────
    /**
     * Lance la vérification périodique des réservations dans 24h.
     * Appeler une seule fois au démarrage de l'app.
     *
     * ⚠️  Ajouter la colonne SQL avant :
     *   ALTER TABLE reservation_local
     *       ADD COLUMN IF NOT EXISTS notif_rappel_envoye TINYINT(1) DEFAULT 0;
     */
    public void demarrer(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl      = dbUrl;
        this.dbUser     = dbUser;
        this.dbPassword = dbPassword;

        // Vérification immédiate + toutes les heures
        scheduler.scheduleAtFixedRate(
                this::verifier24h, 0, 1, TimeUnit.HOURS
        );
        System.out.println("🔔 NotificationService démarré");
    }

    /** Callback appelé sur le FX thread quand le nombre de notifs change */
    public void setOnChangement(Consumer<Long> callback) {
        this.onChangement = callback;
    }

    // ─── Ajout de notifications ────────────────────────────────────────────────
    public void ajouter(NotificationItem notif) {
        Platform.runLater(() -> {
            notifications.add(0, notif);       // plus récent en premier
            notifierChangement();
        });
    }

    // Raccourcis appelés depuis les controllers
    public void notifierNouvelleReservation(reservation_local r) {
        String date = r.getDate_reservation() != null
                ? DATE_FMT.format(r.getDate_reservation()) : "—";
        ajouter(NotificationItem.nouvelleReservation(
                r.getNom_cl(), r.getPrenom_cl(), date, r.getId_reservation()));
    }

    public void notifierModification(reservation_local r, String champ,
                                     String ancienne, String nouvelle) {
        ajouter(NotificationItem.modification(
                r.getNom_cl() + " " + r.getPrenom_cl(),
                champ, ancienne, nouvelle, r.getId_reservation()));
    }

    public void notifierSuppression(String nomClient, String date) {
        ajouter(NotificationItem.suppression(nomClient, date));
    }

    // ─── Lecture ───────────────────────────────────────────────────────────────
    public ObservableList<NotificationItem> getNotifications() { return notifications; }

    public long getNonLues() {
        return notifications.stream().filter(n -> !n.isLue()).count();
    }

    public void marquerToutesLues() {
        Platform.runLater(() -> {
            notifications.forEach(n -> n.setLue(true));
            notifierChangement();
        });
    }

    public void marquerLue(NotificationItem n) {
        n.setLue(true);
        Platform.runLater(this::notifierChangement);
    }

    public void supprimer(NotificationItem n) {
        Platform.runLater(() -> {
            notifications.remove(n);
            notifierChangement();
        });
    }

    public void supprimerToutes() {
        Platform.runLater(() -> {
            notifications.clear();
            notifierChangement();
        });
    }

    // ─── Vérification 24h (thread background) ─────────────────────────────────
    private void verifier24h() {
        if (dbUrl == null) return;

        String sql =
                "SELECT id_reservation, nom_cl, prenom_cl, " +
                        "       date_reservation, heure_debut_reservation " +
                        "FROM reservation_local " +
                        "WHERE date_reservation > NOW() " +
                        "  AND date_reservation <= DATE_ADD(NOW(), INTERVAL 24 HOUR) " +
                        "  AND COALESCE(notif_rappel_envoye, 0) = 0 " +
                        "  AND status_reservation != 'ANNULEE'";

        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int    id     = rs.getInt("id_reservation");
                String nom    = rs.getString("nom_cl");
                String prenom = rs.getString("prenom_cl");
                Date   date   = rs.getDate("date_reservation");
                Timestamp heure  = rs.getTimestamp("heure_debut_reservation");

                String dateStr  = date  != null ? DATE_FMT.format(date)  : "—";
                String heureStr = heure != null ? HEURE_FMT.format(heure) : "—";

                ajouter(NotificationItem.rappel24h(nom, prenom, dateStr, heureStr, id));
                marquerRappelDB(conn, id);
                System.out.println("🔔 Rappel notif : " + nom + " " + prenom + " le " + dateStr);
            }

        } catch (SQLException e) {
            System.err.println("❌ NotificationService DB : " + e.getMessage());
        }
    }

    private void marquerRappelDB(Connection conn, int id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE reservation_local SET notif_rappel_envoye=1 WHERE id_reservation=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public void reinitialiserRappel(int idReservation) {
        if (dbUrl == null) return;
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE reservation_local SET notif_rappel_envoye=0 WHERE id_reservation=?")) {
            ps.setInt(1, idReservation);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur réinit rappel : " + e.getMessage());
        }
    }

    private void notifierChangement() {
        if (onChangement != null) onChangement.accept(getNonLues());
    }
}