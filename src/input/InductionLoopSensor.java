package input;

public class InductionLoopSensor {

    private final String sensorId;
    private boolean isVehiclePresent;

    public InductionLoopSensor(String sensorId) {
        this.sensorId       = sensorId;
        this.isVehiclePresent = false;
    }

    /** Returns true if a vehicle is currently over the loop. */
    public boolean detect() {
        return isVehiclePresent;
    }

    /** Simulates a brief vehicle detection event (pulse high then clears). */
    public void simulatePulse() {
        isVehiclePresent = true;
        isVehiclePresent = false;
    }

    public String getSensorId()                   { return sensorId; }
    public boolean isVehiclePresent()             { return isVehiclePresent; }
    public void setVehiclePresent(boolean present){ this.isVehiclePresent = present; }
}
