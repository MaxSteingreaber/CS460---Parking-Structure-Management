package datastore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CapacityMonitor implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Integer, Integer> floorCapacities;
    private Map<Integer, Integer> floorOccupancy;

    public CapacityMonitor() {
        this.floorCapacities = new HashMap<>();
        this.floorOccupancy  = new HashMap<>();
    }

    /** Registers a floor with its total parkable capacity. */
    public void initFloor(int floor, int capacity) {
        floorCapacities.put(floor, capacity);
        floorOccupancy.put(floor, 0);
    }

    public void incrementOccupancy(int floor) {
        floorOccupancy.merge(floor, 1, Integer::sum);
    }

    public void decrementOccupancy(int floor) {
        floorOccupancy.merge(floor, -1, Integer::sum);
    }

    /** Returns the number of available spaces on the specified floor. */
    public int getAvailable(int floor) {
        int capacity = floorCapacities.getOrDefault(floor, 0);
        int occupied = floorOccupancy.getOrDefault(floor, 0);
        return capacity - occupied;
    }

    /** Returns the aggregate occupancy across all floors. */
    public int getTotalOccupancy() {
        return floorOccupancy.values().stream().mapToInt(Integer::intValue).sum();
    }

    /** Returns true if the specified floor has no available spaces. */
    public boolean isFloorFull(int floor) {
        return getAvailable(floor) <= 0;
    }

    /** Persists the current occupancy state as a historical snapshot for analytics. */
    public void saveSnapshot() {
        // TODO: serialize a point-in-time copy of floorOccupancy for trend analysis
    }

    public Map<Integer, Integer> getFloorCapacities() { return floorCapacities; }
    public Map<Integer, Integer> getFloorOccupancy()  { return floorOccupancy; }
}
