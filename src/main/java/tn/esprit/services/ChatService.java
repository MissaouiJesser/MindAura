package tn.esprit.services;

import tn.esprit.entities.ChatMessage;
import tn.esprit.utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatService {

    private static final int POLL_INTERVAL_MS = 1500;

    private Connection conx;

    public ChatService() {
        conx = MyDataBase.getInstance().getConx();
        ensureTableExists();
    }



    private void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    event_id    INT          NOT NULL,
                    sender_name VARCHAR(120) NOT NULL,
                    message     TEXT         NOT NULL,
                    sent_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_event_time (event_id, sent_at)
                )
                """;

        try (Statement st = conx.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.err.println("[ChatService] Erreur création table : " + e.getMessage());
        }
    }



    public void sendMessage(int eventId, String senderName, String message) throws SQLException {

        String sql = "INSERT INTO chat_messages (event_id, sender_name, message) VALUES (?, ?, ?)";

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setString(2, senderName);
            ps.setString(3, message);
            ps.executeUpdate();
        }
    }



    public List<ChatMessage> getLastMessages(int eventId, int limit) throws SQLException {

        String sql = """
                SELECT * FROM (
                    SELECT id, event_id, sender_name, message, sent_at
                    FROM chat_messages
                    WHERE event_id = ?
                    ORDER BY sent_at DESC
                    LIMIT ?
                ) sub ORDER BY sent_at ASC
                """;

        List<ChatMessage> list = new ArrayList<>();

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        }

        return list;
    }



    public List<ChatMessage> getMessagesSince(int eventId, Timestamp since) throws SQLException {

        String sql = """
                SELECT id, event_id, sender_name, message, sent_at
                FROM chat_messages
                WHERE event_id = ? AND sent_at > ?
                ORDER BY sent_at ASC
                """;

        List<ChatMessage> list = new ArrayList<>();

        try (PreparedStatement ps = conx.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            ps.setTimestamp(2, since);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(map(rs));
            }
        }

        return list;
    }



    public Thread startPolling(int eventId,
                               Timestamp lastSeen,
                               java.util.function.Consumer<List<ChatMessage>> onNew) {

        final Timestamp[] cursor = {
                lastSeen != null
                        ? lastSeen
                        : new Timestamp(System.currentTimeMillis())
        };

        Thread t = new Thread(() -> {

            while (!Thread.currentThread().isInterrupted()) {

                try {
                    List<ChatMessage> newMsgs =
                            getMessagesSince(eventId, cursor[0]);

                    if (!newMsgs.isEmpty()) {

                        // ✅ Correction ici (plus de getLast())
                        cursor[0] =
                                newMsgs.get(newMsgs.size() - 1).getSentAt();

                        javafx.application.Platform.runLater(() ->
                                onNew.accept(newMsgs));
                    }

                    Thread.sleep(POLL_INTERVAL_MS);

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();

                } catch (SQLException e) {
                    System.err.println("[Chat] Erreur polling : "
                            + e.getMessage());

                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });

        t.setDaemon(true);
        t.setName("chat-poll-event-" + eventId);
        t.start();

        return t;
    }



    private ChatMessage map(ResultSet rs) throws SQLException {

        ChatMessage m = new ChatMessage();

        m.setId(rs.getInt("id"));
        m.setEventId(rs.getInt("event_id"));
        m.setSenderName(rs.getString("sender_name"));
        m.setMessage(rs.getString("message"));
        m.setSentAt(rs.getTimestamp("sent_at"));

        return m;
    }
}