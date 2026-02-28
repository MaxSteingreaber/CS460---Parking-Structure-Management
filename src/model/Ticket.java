package model;

import java.time.LocalDateTime;

public class Ticket {

    private final String ticketNumber;
    private final String sessionId;
    private final String spaceId;
    private final LocalDateTime entryTime;

    public Ticket(String ticketNumber, String sessionId, String spaceId, LocalDateTime entryTime) {
        this.ticketNumber = ticketNumber;
        this.sessionId    = sessionId;
        this.spaceId      = spaceId;
        this.entryTime    = entryTime;
    }

    public String format() {
        return String.format("Ticket #%s | Session: %s | Space: %s | Entry: %s",
                ticketNumber, sessionId, spaceId, entryTime);
    }

    public String getTicketNumber()     { return ticketNumber; }
    public String getSessionId()        { return sessionId; }
    public String getSpaceId()          { return spaceId; }
    public LocalDateTime getEntryTime() { return entryTime; }
}
