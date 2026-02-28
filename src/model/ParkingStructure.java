package model;

import enums.AllocationStrategy;

import java.util.List;

public class ParkingStructure {

    private final List<Floor> floors;
    private final String structureName;

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

    /** Finds the best available space using the specified allocation strategy. */
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
                    if (leastOccupied == null || f.getOccupiedCount() < leastOccupied.getOccupiedCount()) {
                        leastOccupied = f;
                    }
                }
                if (leastOccupied != null) {
                    List<ParkingSpace> available = leastOccupied.getAvailableSpaces();
                    if (!available.isEmpty()) return available.get(0);
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
