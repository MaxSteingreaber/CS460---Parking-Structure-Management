package input;

public class WeightSensor {

    private final String spaceId;
    private double currentWeight;
    private final double threshold;

    public WeightSensor(String spaceId, double threshold) {
        this.spaceId       = spaceId;
        this.currentWeight = 0.0;
        this.threshold     = threshold;
    }

    /** Returns true if the current weight exceeds the occupancy threshold. */
    public boolean isOccupied() {
        return currentWeight >= threshold;
    }

    /** Sets the simulated weight reading for demo purposes. */
    public void setSimulatedWeight(double kg) {
        this.currentWeight = kg;
    }

    public String getSpaceId()       { return spaceId; }
    public double getCurrentWeight() { return currentWeight; }
    public double getThreshold()     { return threshold; }
}
