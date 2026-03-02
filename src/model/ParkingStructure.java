package model;

import enums.AllocationStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ParkingStructure {

    private final List<Floor>  floors;
    private final String       structureName;
    private final Random       random = new Random();

    public ParkingStructure(String structureName, List<Floor> floors) {
        this.structureName = structureName;
        this.floors        = floors;
    }

    public Floor getFloor(int number) {
        for (Floor f : floors) {
            if (f.getFloorNumber() == number) return f;
        }
        return null;
    }

    /**
     * Finds an available space using the specified allocation strategy.
     *
     * LOWEST_FLOOR_FIRST / NEAREST_TO_ENTRANCE — first available space
     *   on the lowest floor, scanning rows top-to-bottom.
     *
     * BALANCED_DISTRIBUTION — first available space on whichever floor
     *   currently has the fewest occupied spaces.
     *
     * RANDOM — picks a random space from all available spaces across the
     *   entire structure, simulating a driver choosing their own spot.
     */
    public ParkingSpace findAvailableSpace(AllocationStrategy strategy) {
        switch (strategy) {

            case LOWEST_FLOOR_FIRST:
            case NEAREST_TO_ENTRANCE:
                for (Floor f : floors) {
                    List<ParkingSpace> available = f.getAvailableSpaces();
                    if (!available.isEmpty()) return available.get(0);
                }
                break;

            case BALANCED_DISTRIBUTION:
                Floor leastOccupied = null;
                for (Floor f : floors) {
                    if (leastOccupied == null
                            || f.getOccupiedCount() < leastOccupied.getOccupiedCount()) {
                        leastOccupied = f;
                    }
                }
                if (leastOccupied != null) {
                    List<ParkingSpace> available = leastOccupied.getAvailableSpaces();
                    if (!available.isEmpty()) return available.get(0);
                }
                break;

            case RANDOM:
                // Collect every available space across all floors, then pick one at random
                List<ParkingSpace> allAvailable = new ArrayList<>();
                for (Floor f : floors) {
                    allAvailable.addAll(f.getAvailableSpaces());
                }
                if (!allAvailable.isEmpty()) {
                    Collections.shuffle(allAvailable, random);
                    return allAvailable.get(0);
                }
                break;
        }
        return null;
    }

    public int getTotalCapacity() {
        return floors.stream().mapToInt(Floor::getCapacity).sum();
    }

    public int getTotalOccupancy() {
        return floors.stream().mapToInt(Floor::getOccupiedCount).sum();
    }

    public String getStructureName() { return structureName; }
    public List<Floor> getFloors()   { return floors; }
}