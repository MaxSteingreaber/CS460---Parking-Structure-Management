package model;

import enums.SpaceOrientation;
import enums.SpaceState;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ParkingSpace {

    private final String spaceId;
    private SpaceState state;
    private String sessionId;
    private final int floor;
    private SpaceOrientation ori;
    private boolean pendingRestriction;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public ParkingSpace(String spaceId, SpaceOrientation ori, int floor) {
        this.spaceId   = spaceId;
        this.ori       = ori;
        this.floor     = floor;
        this.state     = SpaceState.AVAILABLE;
        this.sessionId = null;
        this.pendingRestriction = false;
    }

    /**
     * Marks the space as IN_TRANSIT: the vehicle has been assigned here and
     * is travelling from the entry gate. The space is locked for this session
     * so it cannot be double-allocated, but it still renders GREEN on the grid
     * until the weight sensor confirms arrival (occupy() is called).
     */
    public void inTransit(String sessionId) {
        SpaceState old = this.state;
        this.sessionId = sessionId;
        this.state     = SpaceState.IN_TRANSIT;
        pcs.firePropertyChange("state", old, this.state);
    }

    /**
     * Weight sensor confirmed — transitions to OCCUPIED (renders RED).
     * Works from both AVAILABLE and IN_TRANSIT states.
     */
    public void occupy(String sessionId) {
        SpaceState old = this.state;
        this.sessionId = sessionId;
        this.state     = SpaceState.OCCUPIED;
        pcs.firePropertyChange("state", old, this.state);
    }

    /** Frees the space back to AVAILABLE (or RESTRICTED if pending). */
    public void vacate() {
        SpaceState old = this.state;
        this.sessionId = null;
        this.state     = pendingRestriction ? SpaceState.RESTRICTED : SpaceState.AVAILABLE;
        this.pendingRestriction = false;
        pcs.firePropertyChange("state", old, this.state);
    }

    public void restrict() {
        if (state == SpaceState.OCCUPIED) {
            pendingRestriction = true;
        } else {
            SpaceState old = this.state;
            this.state = SpaceState.RESTRICTED;
            pcs.firePropertyChange("state", old, this.state);
        }
    }

    public void unrestrict() {
        pendingRestriction = false;
        if (state == SpaceState.RESTRICTED) {
            SpaceState old = this.state;
            this.state = SpaceState.AVAILABLE;
            pcs.firePropertyChange("state", old, this.state);
        }
    }

    public void reserve() {
        if (state == SpaceState.OCCUPIED) return;
        SpaceState old = this.state;
        this.state = SpaceState.RESERVED;
        pcs.firePropertyChange("state", old, this.state);
    }

    public void unreserve() {
        if (state == SpaceState.RESERVED) {
            SpaceState old = this.state;
            this.state = SpaceState.AVAILABLE;
            pcs.firePropertyChange("state", old, this.state);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l)    { pcs.addPropertyChangeListener(l); }
    public void removePropertyChangeListener(PropertyChangeListener l) { pcs.removePropertyChangeListener(l); }

    public SpaceState getState()           { return state; }
    public String getSpaceId()             { return spaceId; }
    public String getSessionId()           { return sessionId; }
    public int getFloor()                  { return floor; }
    public boolean isPendingRestriction()  { return pendingRestriction; }
}