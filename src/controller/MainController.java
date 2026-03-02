package controller;

import datastore.DataStoreDriver;
import enums.AllocationStrategy;
import enums.SystemState;
import input.AdminCommands;
import model.Floor;
import model.ParkingSpace;
import model.ParkingStructure;
import model.Session;
import model.Ticket;
import observer.EventType;
import observer.SystemEvent;
import observer.SystemObserver;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Central orchestrator of the MPSMS. Receives commands from input controllers,
 * delegates to output controllers and the data store, and fires state-change
 * events to observers. All business logic flows through this class.
 */
public class MainController {

    private UserInputController      userInputCtrl;
    private FacilitiesInputController facilitiesInputCtrl;
    private EmergencyOutputController  emergencyOutputCtrl;
    private FacilitiesOutputController facilitiesOutputCtrl;
    private DataStoreDriver            dataStoreDriver;
    private ParkingStructure           parkingStructure;

    private boolean            isEmergencyActive;
    private AllocationStrategy allocationStrategy;
    private final List<SystemObserver> observers;

    public MainController() {
        this.isEmergencyActive  = false;
        this.allocationStrategy = AllocationStrategy.LOWEST_FLOOR_FIRST;
        this.observers          = new ArrayList<>();
    }

    /**
     * Initializes all subsystems. Call after all setters have been invoked.
     * Loads persisted state and registers floor capacities with the CapacityMonitor.
     */
    public void initialize() {
        if (dataStoreDriver != null) {
            dataStoreDriver.initialize();
        }
        if (parkingStructure != null && dataStoreDriver != null) {
            for (Floor floor : parkingStructure.getFloors()) {
                dataStoreDriver.getCapacityMonitor()
                        .initFloor(floor.getFloorNumber(), floor.getCapacity());
            }
        }
    }

    /**
     * Processes a simulated vehicle entry: finds an available space, creates a session,
     * commands gate and ticket dispenser, and fires update events.
     */
    public void handleVehicleEntry() {
        if (isEmergencyActive) return;
        if (userInputCtrl != null && !userInputCtrl.getEntryKiosk().isActive()) return;

        ParkingSpace space = parkingStructure.findAvailableSpace(allocationStrategy);
        if (space == null) {
            facilitiesOutputCtrl.setEntranceMessage("FULL");
            notifyObservers(new SystemEvent(EventType.CAPACITY, null, "Structure is full — no spaces available"));
            return;
        }

        String sessionId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        space.occupy(sessionId);
        dataStoreDriver.getSessionLogger()
                .createSession(sessionId, space.getSpaceId(), space.getFloor(), now);
        dataStoreDriver.getCapacityMonitor().incrementOccupancy(space.getFloor());

        Ticket ticket = facilitiesOutputCtrl.issueTicket(sessionId, space.getSpaceId(), now);
        facilitiesOutputCtrl.openGate("ENTRY");
        facilitiesOutputCtrl.updateFloorDisplay(space.getFloor(),
                dataStoreDriver.getCapacityMonitor().getAvailable(space.getFloor()));

        notifyObservers(new SystemEvent(EventType.ENTRY, sessionId,
                "Vehicle entered. Space: " + space.getSpaceId()
                        + " | Ticket: " + ticket.getTicketNumber()));
    }

    /**
     * Processes a vehicle exit: releases the space, calculates the fee,
     * archives the transaction, commands gate, and fires update events.
     */
    public void handleVehicleExit(String sessionId) {
        dataStoreDriver.getSessionLogger().getSessionById(sessionId).ifPresent(session -> {
            LocalDateTime now = LocalDateTime.now();
            session.setExitTime(now);

            findSpaceById(session.getSpaceId()).ifPresent(ParkingSpace::vacate);

            double fee = userInputCtrl.getExitKiosk().calculateFee(session);
            dataStoreDriver.getTransactionArchive().archiveTransaction(session, fee);
            dataStoreDriver.getCapacityMonitor().decrementOccupancy(session.getFloor());

            facilitiesOutputCtrl.openGate("EXIT");
            facilitiesOutputCtrl.updateFloorDisplay(session.getFloor(),
                    dataStoreDriver.getCapacityMonitor().getAvailable(session.getFloor()));

            notifyObservers(new SystemEvent(EventType.EXIT, sessionId,
                    "Vehicle exited. Session: " + sessionId
                            + " | Fee: $" + String.format("%.2f", fee)));
        });
    }

    /**
     * Transitions the system to emergency state.
     * Engages sirens/LEDs, opens all gates, halts new entries.
     */
    public void activateEmergency() {
        if (isEmergencyActive) return;
        isEmergencyActive = true;
        emergencyOutputCtrl.engageEmergency();
        facilitiesOutputCtrl.openAllGates();
        if (userInputCtrl != null) userInputCtrl.getEntryKiosk().disable();
        notifyObservers(new SystemEvent(EventType.EMERGENCY, null, "Emergency state ACTIVATED"));
    }

    /** Restores normal operations from emergency state. */
    public void deactivateEmergency() {
        if (!isEmergencyActive) return;
        isEmergencyActive = false;
        emergencyOutputCtrl.disengageEmergency();
        if (userInputCtrl != null) userInputCtrl.getEntryKiosk().enable();
        notifyObservers(new SystemEvent(EventType.EMERGENCY, null, "Emergency state DEACTIVATED"));
    }

    /**
     * Marks a space as restricted. If the space is currently occupied,
     * queues the restriction to apply when the occupant departs.
     */
    public void restrictSpace(String spaceId) {
        findSpaceById(spaceId).ifPresent(space -> {
            space.restrict();
            notifyObservers(new SystemEvent(EventType.RESTRICTION, spaceId,
                    "Space restricted: " + spaceId
                            + (space.isPendingRestriction() ? " (pending — space is occupied)" : "")));
        });
    }

    /** Removes the restriction from a space and updates availability. */
    public void unrestrictSpace(String spaceId) {
        findSpaceById(spaceId).ifPresent(space -> {
            space.unrestrict();
            notifyObservers(new SystemEvent(EventType.RESTRICTION, spaceId,
                    "Space unrestricted: " + spaceId));
        });
    }

    public void reserveSpace(String spaceId) {
        findSpaceById(spaceId).ifPresent(space -> {
            space.reserve();
            notifyObservers(new SystemEvent(EventType.RESTRICTION, spaceId,
                    "Space reserved: " + spaceId));
        });
    }

    public void unreserveSpace(String spaceId) {
        findSpaceById(spaceId).ifPresent(space -> {
            space.unreserve();
            notifyObservers(new SystemEvent(EventType.RESTRICTION, spaceId,
                    "Space unreserved: " + spaceId));
        });
    }

    private Optional<ParkingSpace> findSpaceById(String spaceId) {
        for (Floor floor : parkingStructure.getFloors()) {
            for (int r = 0; r < floor.getRows(); r++) {
                for (int c = 0; c < floor.getColumns(); c++) {
                    ParkingSpace space = floor.getSpace(r, c);
                    if (space.getSpaceId().equals(spaceId)) return Optional.of(space);
                }
            }
        }
        return Optional.empty();
    }

    public void addObserver(SystemObserver o) {
        observers.add(o);
    }

    public void notifyObservers(SystemEvent event) {
        for (SystemObserver o : observers) {
            o.onSystemEvent(event);
        }
    }

    /** Convenience accessor so GUI panels can reach AdminCommands without storing a separate reference. */
    public AdminCommands getAdminCommands() {
        return userInputCtrl != null ? userInputCtrl.getAdminCommands() : null;
    }

    public SystemState getSystemState() {
        return isEmergencyActive ? SystemState.EMERGENCY : SystemState.NORMAL;
    }

    // ── Getters ──────────────────────────────────────────────────────────────
    public boolean           isEmergencyActive()    { return isEmergencyActive; }
    public DataStoreDriver   getDataStoreDriver()   { return dataStoreDriver; }
    public ParkingStructure  getParkingStructure()  { return parkingStructure; }
    public AllocationStrategy getAllocationStrategy(){ return allocationStrategy; }

    // ── Setters (called during wiring in Main) ────────────────────────────────
    public void setUserInputCtrl(UserInputController ctrl)           { this.userInputCtrl = ctrl; }
    public void setFacilitiesInputCtrl(FacilitiesInputController ctrl){ this.facilitiesInputCtrl = ctrl; }
    public void setEmergencyOutputCtrl(EmergencyOutputController ctrl){ this.emergencyOutputCtrl = ctrl; }
    public void setFacilitiesOutputCtrl(FacilitiesOutputController ctrl){ this.facilitiesOutputCtrl = ctrl; }
    public void setDataStoreDriver(DataStoreDriver driver)           { this.dataStoreDriver = driver; }
    public void setParkingStructure(ParkingStructure structure)      { this.parkingStructure = structure; }
    public void setAllocationStrategy(AllocationStrategy strategy)   { this.allocationStrategy = strategy; }
}
