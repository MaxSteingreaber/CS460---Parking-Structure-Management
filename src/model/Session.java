package model;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

public class Session implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final String spaceId;
    private final int floor;
    private final LocalDateTime entryTime;
    private LocalDateTime exitTime;

    public Session(String sessionId, String spaceId, int floor, LocalDateTime entryTime) {
        this.sessionId = sessionId;
        this.spaceId = spaceId;
        this.floor = floor;
        this.entryTime = entryTime;
        this.exitTime = null;
    }

    public Duration getDuration() {
        LocalDateTime end = (exitTime != null) ? exitTime : LocalDateTime.now();
        return Duration.between(entryTime, end);
    }

    public boolean isActive() {
        return exitTime == null;
    }

    public String getSessionId()       { return sessionId; }
    public String getSpaceId()         { return spaceId; }
    public int getFloor()              { return floor; }
    public LocalDateTime getEntryTime(){ return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
}
