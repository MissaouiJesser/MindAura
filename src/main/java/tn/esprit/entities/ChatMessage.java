package tn.esprit.entities;

import java.sql.Timestamp;


public class ChatMessage {

    private int       id;
    private int       eventId;
    private String    senderName;
    private String    message;
    private Timestamp sentAt;

    public ChatMessage() {}

    public ChatMessage(int eventId, String senderName, String message) {
        this.eventId    = eventId;
        this.senderName = senderName;
        this.message    = message;
    }



    public int       getId()         { return id; }
    public void      setId(int id)   { this.id = id; }

    public int       getEventId()              { return eventId; }
    public void      setEventId(int eventId)   { this.eventId = eventId; }

    public String    getSenderName()                   { return senderName; }
    public void      setSenderName(String senderName)  { this.senderName = senderName; }

    public String    getMessage()                { return message; }
    public void      setMessage(String message)  { this.message = message; }

    public Timestamp getSentAt()                 { return sentAt; }
    public void      setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }

    @Override
    public String toString() {
        return "[" + sentAt + "] " + senderName + ": " + message;
    }
}
