package model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String spaceId;
    private final int floor;
    private final LocalDateTime entryTime;
    private final LocalDateTime exitTime;
    private final double fee;

    public Transaction(Session session, double fee) {
        this.sessionId = session.getSessionId();
        this.spaceId   = session.getSpaceId();
        this.floor     = session.getFloor();
        this.entryTime = session.getEntryTime();
        this.exitTime  = session.getExitTime();
        this.fee       = fee;
    }

    public Duration getDuration() {
        return Duration.between(entryTime, exitTime);
    }

    public String getSessionId()        { return sessionId; }
    public String getSpaceId()          { return spaceId; }
    public int getFloor()               { return floor; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime()  { return exitTime; }
    public double getFee()              { return fee; }
}
