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

    /** Transitions the space to OCCUPIED and records the session. Fires a state-change event. */
    public void occupy(String sessionId) {
        SpaceState old = this.state;
        this.sessionId = sessionId;
        this.state     = SpaceState.OCCUPIED;
        pcs.firePropertyChange("state", old, this.state);
    }

    /** Transitions the space to AVAILABLE (or RESTRICTED if pending). Clears the session. Fires a state-change event. */
    public void vacate() {
        SpaceState old = this.state;
        this.sessionId = null;
        this.state     = pendingRestriction ? SpaceState.RESTRICTED : SpaceState.AVAILABLE;
        this.pendingRestriction = false;
        pcs.firePropertyChange("state", old, this.state);
    }

    /** If available, transitions to RESTRICTED immediately. If occupied, sets pendingRestriction flag. */
    public void restrict() {
        if (state == SpaceState.OCCUPIED) {
            pendingRestriction = true;
        } else {
            SpaceState old = this.state;
            this.state = SpaceState.RESTRICTED;
            pcs.firePropertyChange("state", old, this.state);
        }
    }

    /** Transitions from RESTRICTED to AVAILABLE. Clears pendingRestriction flag. */
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

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public SpaceState getState()           { return state; }
    public String getSpaceId()             { return spaceId; }
    public String getSessionId()           { return sessionId; }
    public int getFloor()                  { return floor; }
    public boolean isPendingRestriction()  { return pendingRestriction; }
}