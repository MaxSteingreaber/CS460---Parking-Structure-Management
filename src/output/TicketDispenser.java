package output;

import model.Ticket;

import java.time.LocalDateTime;

public class TicketDispenser {

    private int ticketCounter;

    public TicketDispenser() {
        this.ticketCounter = 1;
    }

    /** Creates and returns a Ticket with the provided session details. */
    public Ticket issueTicket(String sessionId, String spaceId, LocalDateTime entryTime) {
        String ticketNumber = String.format("TKT-%05d", ticketCounter++);
        return new Ticket(ticketNumber, sessionId, spaceId, entryTime);
    }
}
