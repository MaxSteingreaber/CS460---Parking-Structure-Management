package model;

import enums.SpaceState;

import java.util.ArrayList;
import java.util.List;

public class Floor {

    private final int floorNumber;
    private final ParkingSpace[][] spaces;
    private final int rows;
    private final int columns;

    public Floor(int floorNumber, int rows, int columns) {
        this.floorNumber = floorNumber;
        this.rows        = rows;
        this.columns     = columns;
        this.spaces      = new ParkingSpace[rows][columns];
        initializeSpaces();
    }

    private void initializeSpaces() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                String id = String.format("F%d-%d%d", floorNumber, r, c);
                spaces[r][c] = new ParkingSpace(id, floorNumber, r, c);
            }
        }
    }

    public ParkingSpace getSpace(int row, int col) {
        return spaces[row][col];
    }

    public List<ParkingSpace> getAvailableSpaces() {
        List<ParkingSpace> available = new ArrayList<>();
        for (ParkingSpace[] row : spaces) {
            for (ParkingSpace space : row) {
                if (space.getState() == SpaceState.AVAILABLE) {
                    available.add(space);
                }
            }
        }
        return available;
    }

    public int getOccupiedCount() {
        int count = 0;
        for (ParkingSpace[] row : spaces) {
            for (ParkingSpace space : row) {
                if (space.getState() == SpaceState.OCCUPIED) count++;
            }
        }
        return count;
    }

    public int getCapacity()       { return rows * columns; }
    public int getFloorNumber()    { return floorNumber; }
    public int getRows()           { return rows; }
    public int getColumns()        { return columns; }
    public ParkingSpace[][] getSpaces() { return spaces; }
}
