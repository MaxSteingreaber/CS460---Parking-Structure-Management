package input;

public class EntryKiosk {

    private boolean isActive;

    public EntryKiosk() {
        this.isActive = true;
    }

    /** Signals that a vehicle has arrived and requests a parking session. */
    public boolean requestEntry() {
        return isActive;
    }

    /** Disables the kiosk (used during emergency or full capacity). */
    public void disable() {
        isActive = false;
    }

    /** Re-enables the kiosk for normal operations. */
    public void enable() {
        isActive = true;
    }

    public boolean isActive() { return isActive; }
}
