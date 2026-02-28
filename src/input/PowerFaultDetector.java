package input;

public class PowerFaultDetector {

    private boolean isPowerStable;

    public PowerFaultDetector() {
        this.isPowerStable = true;
    }

    /** Returns the current power state (true = stable). */
    public boolean checkPower() {
        return isPowerStable;
    }

    /** Simulates a power fault event for demo purposes. */
    public void simulateFault() {
        isPowerStable = false;
    }

    /** Restores normal power for demo purposes. */
    public void simulateRestore() {
        isPowerStable = true;
    }

    public boolean isPowerStable() { return isPowerStable; }
}
