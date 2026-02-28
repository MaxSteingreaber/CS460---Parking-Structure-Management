package output;

public class GateActuator {

    private final String gateId;
    private boolean isOpen;

    public GateActuator(String gateId) {
        this.gateId = gateId;
        this.isOpen = false;
    }

    /** Raises the barrier gate. No-op if already open. */
    public void open() {
        if (!isOpen) isOpen = true;
    }

    /** Lowers the barrier gate. No-op if already closed. */
    public void close() {
        if (isOpen) isOpen = false;
    }

    public boolean getState() { return isOpen; }
    public String getGateId() { return gateId; }
}
