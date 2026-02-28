package datastore;

import model.Session;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SessionLogger implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Session> sessions;

    public SessionLogger() {
        this.sessions = new ArrayList<>();
    }

    /** Inserts a new active session record. */
    public void createSession(String sessionId, String spaceId, int floor, LocalDateTime entryTime) {
        sessions.add(new Session(sessionId, spaceId, floor, entryTime));
    }

    /** Marks a session as completed and records the exit time. */
    public void endSession(String sessionId, LocalDateTime exitTime) {
        getSessionById(sessionId).ifPresent(s -> s.setExitTime(exitTime));
    }

    /** Returns a list of all sessions that have not yet ended. */
    public List<Session> getActiveSessions() {
        List<Session> active = new ArrayList<>();
        for (Session s : sessions) {
            if (s.isActive()) active.add(s);
        }
        return active;
    }

    /** Returns the session record for the given identifier. */
    public Optional<Session> getSessionById(String sessionId) {
        return sessions.stream()
                .filter(s -> s.getSessionId().equals(sessionId))
                .findFirst();
    }

    public List<Session> getAllSessions() { return sessions; }
}
