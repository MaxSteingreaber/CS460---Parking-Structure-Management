package model;

import enums.SpaceOrientation;
import enums.SpaceState;

import java.util.ArrayList;
import java.util.List;

public class Floor {

    private final int floorNumber;
    private ArrayList<ParkingSpace> spaces;
    private int perimeterLength;
    private int perimeterWidth;


    public Floor(int floorNumber, int spacesLen, int spacesWid) {
        this.floorNumber = floorNumber;
        this.perimeterLength = spacesLen;
        this.perimeterWidth = spacesWid;
        this.spaces = new ArrayList<>();
        initializeSpaces();
    }

    private void initializeSpaces() {
        int spaceId = 0;

        // Top corridor - OUTER row (left to right)
        for (int i = 0; i < perimeterLength; i++) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.HORIZONTAL_TOP_OUTER,
                    floorNumber
            ));
        }

        // Top corridor - INNER row (left to right)
        for (int i = 0; i < perimeterLength; i++) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.HORIZONTAL_TOP_INNER,
                    floorNumber
            ));
        }

        // Right corridor - OUTER column (top to bottom)
        for (int i = 0; i < perimeterWidth; i++) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.VERTICAL_RIGHT_OUTER,
                    floorNumber
            ));
        }

        // Right corridor - INNER column (top to bottom)
        for (int i = 0; i < perimeterWidth; i++) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.VERTICAL_RIGHT_INNER,
                    floorNumber
            ));
        }

        // Bottom corridor - OUTER row (right to left)
        for (int i = perimeterLength - 1; i >= 0; i--) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.HORIZONTAL_BOTTOM_OUTER,
                    floorNumber
            ));
        }

        // Bottom corridor - INNER row (right to left)
        for (int i = perimeterLength - 1; i >= 0; i--) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.HORIZONTAL_BOTTOM_INNER,
                    floorNumber
            ));
        }

        // Left corridor - OUTER column (bottom to top)
        for (int i = perimeterWidth - 1; i >= 0; i--) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.VERTICAL_LEFT_OUTER,
                    floorNumber
            ));
        }

        // Left corridor - INNER column (bottom to top)
        for (int i = perimeterWidth - 1; i >= 0; i--) {
            spaces.add(new ParkingSpace(
                    floorNumber + "-" + spaceId++,
                    SpaceOrientation.VERTICAL_LEFT_INNER,
                    floorNumber
            ));
        }
    }

    public ParkingSpace getSpace(int index) {
        return spaces.get(index);
    }

    public List<ParkingSpace> getAvailableSpaces() {
        List<ParkingSpace> available = new ArrayList<>();
        for (ParkingSpace space : spaces) {
            if (space.getState() == SpaceState.AVAILABLE) {
                available.add(space);
            }
        }
        return available;
    }

    public int getOccupiedCount() {
        int count = 0;
        for (ParkingSpace space : spaces) {
            if (space.getState() == SpaceState.OCCUPIED) count++;
        }
        return count;
    }

    public int getCapacity()       { return spaces.size(); }
    public int getFloorNumber()    { return floorNumber; }
    public int getPerimeterLength()           { return perimeterLength; }
    public int getPerimeterWidth()        { return perimeterWidth; }
    public ArrayList<ParkingSpace> getSpaces() { return spaces; }
}
