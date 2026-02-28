package output;

public class FloorLevelDisplay {

    private final int floorNumber;
    private int availableSpaces;

    public FloorLevelDisplay(int floorNumber, int availableSpaces) {
        this.floorNumber     = floorNumber;
        this.availableSpaces = availableSpaces;
    }

    /** Updates the displayed available space count. */
    public void update(int available) {
        this.availableSpaces = available;
    }

    /** Returns a formatted string representation for rendering. */
    public String getDisplay() {
        return String.format("Floor %d: %d spaces available", floorNumber, availableSpaces);
    }

    public int getFloorNumber()    { return floorNumber; }
    public int getAvailableSpaces(){ return availableSpaces; }
}
