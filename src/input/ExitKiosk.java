package input;

import model.Session;

public class ExitKiosk {

    private boolean isActive;
    private static final double RATE_PER_HOUR = 2.50;

    public ExitKiosk() {
        this.isActive = true;
    }

    /** Initiates the exit process for the given session. */
    public boolean requestExit(String sessionId) {
        return isActive && sessionId != null;
    }

    /** Computes the parking fee based on duration and rate schedule. */
    public double calculateFee(Session session) {
        long minutes = session.getDuration().toMinutes();
        double hours = Math.ceil(minutes / 60.0);
        return hours * RATE_PER_HOUR;
    }

    public void disable() { isActive = false; }
    public void enable()  { isActive = true; }
    public boolean isActive() { return isActive; }
}
