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
 * Central orchestrator of the MPSMS.
 *
 * Entry and exit are split into two phases to model real hardware sequencing.
 * The in-transit counter tracks vehicles between the gate and their stall
 * (entry) or between their stall and the exit gate (exit). The counter is
 * displayed on the DriverDisplayPanel but does NOT affect space colour on the
 * grid — spaces go directly AVAILABLE → OCCUPIED on entry and OCCUPIED →
 * AVAILABLE on exit, with the weight sensor completing each transition.
 *
 *   ENTRY  Phase 1 — startVehicleEntry()    : allocate + occupy space, open gate, +inTransit
 *   ENTRY  Phase 2 — completeVehicleEntry() : weight sensor confirmed, -inTransit
 *
 *   EXIT   Phase 1 — startVehicleExit()     : weight sensor cleared → AVAILABLE, +inTransit
 *   EXIT   Phase 2 — completeVehicleExit()  : payment done, open gate, archive, -inTransit
 */
public class MainController {

    private UserInputController        userInputCtrl;
    private FacilitiesInputController  facilitiesInputCtrl;
    private EmergencyOutputController  emergencyOutputCtrl;
    private FacilitiesOutputController facilitiesOutputCtrl;
    private DataStoreDriver            dataStoreDriver;
    private ParkingStructure           parkingStructure;

    private boolean            isEmergencyActive;
    private AllocationStrategy allocationStrategy;
    private int                inTransitCount;

    private final List<SystemObserver> observers;

    public MainController() {
        this.isEmergencyActive  = false;
        this.allocationStrategy = AllocationStrategy.LOWEST_FLOOR_FIRST;
        this.inTransitCount     = 0;
        this.observers          = new ArrayList<>();
    }

    // ── Initialization ────────────────────────────────────────────────────────

    public void initialize() {
        if (dataStoreDriver != null) dataStoreDriver.initialize();
        if (parkingStructure != null && dataStoreDriver != null) {
            for (Floor floor : parkingStructure.getFloors()) {
                dataStoreDriver.getCapacityMonitor()
                        .initFloor(floor.getFloorNumber(), floor.getCapacity());
            }
        }
    }

    // ── Entry convenience wrappers (manual admin buttons) ─────────────────────

    public void handleVehicleEntry() {
        handleVehicleEntry(allocationStrategy);
    }

    public void handleVehicleEntry(AllocationStrategy strategyOverride) {
        String sessionId = startVehicleEntry(strategyOverride);
        if (sessionId != null) completeVehicleEntry(sessionId);
    }

    // ── Entry Phase 1: space allocated + OCCUPIED, gate opens, inTransit++ ────

    /**
     * Allocates the best available space, immediately marks it OCCUPIED
     * (the grid updates to blue), opens the entry gate, and increments the
     * in-transit counter. The weight sensor confirmation (Phase 2) will
     * decrement the counter once the vehicle is physically in the stall.
     *
     * @return the new sessionId, or null if structure is full / emergency active
     */
    public String startVehicleEntry(AllocationStrategy strategyOverride) {
        if (isEmergencyActive) return null;
        if (userInputCtrl != null && !userInputCtrl.getEntryKiosk().isActive()) return null;

        ParkingSpace space = parkingStructure.findAvailableSpace(strategyOverride);
        if (space == null) {
            facilitiesOutputCtrl.setEntranceMessage("FULL");
            notifyObservers(new SystemEvent(EventType.CAPACITY, null,
                    "Structure is full — no spaces available"));
            return null;
        }

        String sessionId  = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        // Space goes straight to OCCUPIED — grid updates to blue immediately
        space.occupy(sessionId);

        dataStoreDriver.getSessionLogger()
                .createSession(sessionId, space.getSpaceId(), space.getFloor(), now);
        dataStoreDriver.getCapacityMonitor().incrementOccupancy(space.getFloor());

        Ticket ticket = facilitiesOutputCtrl.issueTicket(sessionId, space.getSpaceId(), now);
        facilitiesOutputCtrl.openGate("ENTRY");
        inTransitCount++;

        facilitiesOutputCtrl.updateFloorDisplay(space.getFloor(),
                dataStoreDriver.getCapacityMonitor().getAvailable(space.getFloor()));

        notifyObservers(new SystemEvent(EventType.ENTRY, sessionId,
                "Vehicle entered. Space: " + space.getSpaceId()
                        + " | Ticket: " + ticket.getTicketNumber()
                        + " | In-Transit: " + inTransitCount));

        return sessionId;
    }

    // ── Entry Phase 2: weight sensor confirmed, inTransit-- ───────────────────

    /**
     * Called when the weight sensor at the stall confirms vehicle arrival.
     * The space is already OCCUPIED — this simply decrements the in-transit
     * counter and logs the hardware confirmation.
     */
    public void completeVehicleEntry(String sessionId) {
        dataStoreDriver.getSessionLogger().getSessionById(sessionId).ifPresent(session -> {
            inTransitCount = Math.max(0, inTransitCount - 1);
            notifyObservers(new SystemEvent(EventType.SESSION, sessionId,
                    "Weight sensor confirmed — Space " + session.getSpaceId()
                            + " OCCUPIED | In-Transit: " + inTransitCount));
        });
    }

    // ── Exit convenience wrapper (manual admin buttons) ───────────────────────

    public void handleVehicleExit(String sessionId) {
        startVehicleExit(sessionId);
        completeVehicleExit(sessionId);
    }

    // ── Exit Phase 1: weight sensor clears, space AVAILABLE, inTransit++ ─────

    public void startVehicleExit(String sessionId) {
        dataStoreDriver.getSessionLogger().getSessionById(sessionId).ifPresent(session ->
                findSpaceById(session.getSpaceId()).ifPresent(space -> {
                    space.vacate();
                    inTransitCount++;
                    notifyObservers(new SystemEvent(EventType.EXIT, sessionId,
                            "Weight sensor cleared — Space " + session.getSpaceId()
                                    + " AVAILABLE | Vehicle en route to exit | In-Transit: "
                                    + inTransitCount));
                })
        );
    }

    // ── Exit Phase 2: payment confirmed, gate opens, transaction archived ─────

    public void completeVehicleExit(String sessionId) {
        dataStoreDriver.getSessionLogger().getSessionById(sessionId).ifPresent(session -> {
            LocalDateTime now = LocalDateTime.now();
            session.setExitTime(now);

            double fee = isEmergencyActive ? 0.0
                    : userInputCtrl.getExitKiosk().calculateFee(session);

            dataStoreDriver.getTransactionArchive().archiveTransaction(session, fee);
            dataStoreDriver.getCapacityMonitor().decrementOccupancy(session.getFloor());

            facilitiesOutputCtrl.openGate("EXIT");
            facilitiesOutputCtrl.updateFloorDisplay(session.getFloor(),
                    dataStoreDriver.getCapacityMonitor().getAvailable(session.getFloor()));

            inTransitCount = Math.max(0, inTransitCount - 1);

            String feeStr = isEmergencyActive
                    ? "WAIVED (Emergency)" : "$" + String.format("%.2f", fee);

            notifyObservers(new SystemEvent(EventType.EXIT, sessionId,
                    "Vehicle exited. Session: " + sessionId
                            + " | Fee: " + feeStr
                            + " | In-Transit: " + inTransitCount));
        });
    }

    // ── Suggested space (no side effects) ────────────────────────────────────

    public ParkingSpace getSuggestedSpace() {
        return parkingStructure.findAvailableSpace(AllocationStrategy.LOWEST_FLOOR_FIRST);
    }

    // ── Emergency ─────────────────────────────────────────────────────────────

    public void activateEmergency() {
        if (isEmergencyActive) return;
        isEmergencyActive = true;
        emergencyOutputCtrl.engageEmergency();
        facilitiesOutputCtrl.openAllGates();
        if (userInputCtrl != null) userInputCtrl.getEntryKiosk().disable();
        notifyObservers(new SystemEvent(EventType.EMERGENCY, null,
                "Emergency state ACTIVATED — all gates open, payment waived"));
    }

    public void deactivateEmergency() {
        if (!isEmergencyActive) return;
        isEmergencyActive = false;
        emergencyOutputCtrl.disengageEmergency();
        if (userInputCtrl != null) userInputCtrl.getEntryKiosk().enable();
        notifyObservers(new SystemEvent(EventType.EMERGENCY, null,
                "Emergency state DEACTIVATED — normal operations resumed"));
    }

    // ── Space management ──────────────────────────────────────────────────────

    public void restrictSpace(String spaceId) {
        findSpaceById(spaceId).ifPresent(space -> {
            space.restrict();
            notifyObservers(new SystemEvent(EventType.RESTRICTION, spaceId,
                    "Space restricted: " + spaceId
                            + (space.isPendingRestriction() ? " (pending — space is occupied)" : "")));
        });
    }

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

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    public void addObserver(SystemObserver o) { observers.add(o); }

    public void notifyObservers(SystemEvent event) {
        for (SystemObserver o : observers) o.onSystemEvent(event);
    }

    public AdminCommands getAdminCommands() {
        return userInputCtrl != null ? userInputCtrl.getAdminCommands() : null;
    }

    public UserInputController getUserInputCtrl() { return userInputCtrl; }

    public SystemState getSystemState() {
        return isEmergencyActive ? SystemState.EMERGENCY : SystemState.NORMAL;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public boolean            isEmergencyActive()    { return isEmergencyActive; }
    public int                getInTransitCount()    { return inTransitCount; }
    public DataStoreDriver    getDataStoreDriver()   { return dataStoreDriver; }
    public ParkingStructure   getParkingStructure()  { return parkingStructure; }
    public AllocationStrategy getAllocationStrategy() { return allocationStrategy; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setUserInputCtrl(UserInputController ctrl)             { this.userInputCtrl = ctrl; }
    public void setFacilitiesInputCtrl(FacilitiesInputController ctrl) { this.facilitiesInputCtrl = ctrl; }
    public void setEmergencyOutputCtrl(EmergencyOutputController ctrl) { this.emergencyOutputCtrl = ctrl; }
    public void setFacilitiesOutputCtrl(FacilitiesOutputController ctrl){ this.facilitiesOutputCtrl = ctrl; }
    public void setDataStoreDriver(DataStoreDriver driver)             { this.dataStoreDriver = driver; }
    public void setParkingStructure(ParkingStructure structure)        { this.parkingStructure = structure; }
    public void setAllocationStrategy(AllocationStrategy strategy)     { this.allocationStrategy = strategy; }
}