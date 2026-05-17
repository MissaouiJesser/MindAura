package tn.esprit.entities;

import java.util.Date;

/**
 * Représente une notification in-app MindAura.
 */
public class NotificationItem {

    public enum Type { RAPPEL_24H, NOUVELLE_RESERVATION, MODIFICATION, SUPPRESSION }

    private String  titre;
    private String  message;
    private Type    type;
    private Date    dateCreation;
    private boolean lue;
    private int     idReservation;

    public NotificationItem(String titre, String message, Type type, int idReservation) {
        this.titre         = titre;
        this.message       = message;
        this.type          = type;
        this.dateCreation  = new Date();
        this.lue           = false;
        this.idReservation = idReservation;
    }

    // ─── Factories ─────────────────────────────────────────────────────────────
    public static NotificationItem rappel24h(String nom, String prenom,
                                             String date, String heure, int idRes) {
        return new NotificationItem(
                "⏰ Rappel – réservation demain",
                nom + " " + prenom + "\n📅 " + date + " à " + heure,
                Type.RAPPEL_24H, idRes);
    }

    public static NotificationItem nouvelleReservation(String nom, String prenom,
                                                       String date, int idRes) {
        return new NotificationItem(
                "✅ Nouvelle réservation",
                nom + " " + prenom + " — " + date,
                Type.NOUVELLE_RESERVATION, idRes);
    }

    public static NotificationItem modification(String nomClient, String champ,
                                                String ancienne, String nouvelle, int idRes) {
        return new NotificationItem(
                "✏️ Réservation modifiée",
                nomClient + "\n" + champ + " : " + ancienne + " → " + nouvelle,
                Type.MODIFICATION, idRes);
    }

    public static NotificationItem suppression(String nomClient, String date) {
        return new NotificationItem(
                "🗑️ Réservation supprimée",
                nomClient + " — " + date,
                Type.SUPPRESSION, -1);
    }

    // ─── Couleurs selon type ────────────────────────────────────────────────────
    public String getCouleurFond() {
        switch (type) {
            case RAPPEL_24H:           return "#FFF4E6";
            case NOUVELLE_RESERVATION: return "#D8F3DC";
            case MODIFICATION:         return "#EFF6FF";
            case SUPPRESSION:          return "#F3E8FF";
            default:                   return "#F0F4F8";
        }
    }
    public String getCouleurBordure() {
        switch (type) {
            case RAPPEL_24H:           return "#FF8C00";
            case NOUVELLE_RESERVATION: return "#2D6A4F";
            case MODIFICATION:         return "#2563EB";
            case SUPPRESSION:          return "#7B5EA7";
            default:                   return "#9CA3AF";
        }
    }
    public String getIcone() {
        switch (type) {
            case RAPPEL_24H:           return "⏰";
            case NOUVELLE_RESERVATION: return "✅";
            case MODIFICATION:         return "✏️";
            case SUPPRESSION:          return "🗑️";
            default:                   return "ℹ️";
        }
    }

    // ─── Getters / Setters ──────────────────────────────────────────────────────
    public String  getTitre()         { return titre; }
    public String  getMessage()       { return message; }
    public Type    getType()          { return type; }
    public Date    getDateCreation()  { return dateCreation; }
    public void    setDateCreation(Date d) { this.dateCreation = d; }
    public boolean isLue()            { return lue; }
    public void    setLue(boolean b)  { this.lue = b; }
    public int     getIdReservation() { return idReservation; }
}