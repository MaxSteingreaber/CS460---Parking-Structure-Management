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
 * During EMERGENCY the gates are permanently open and vehicles flow freely
 * in both directions with no kiosk interaction and no payment. Entry and
 * exit are still tracked so occupancy and in-transit counts remain accurate.
 *
 * Normal two-phase sequencing:
 *   ENTRY Phase 1 — startVehicleEntry()          : IN_TRANSIT, gate opens, inTransit++
 *   ENTRY Phase 2 — completeVehicleEntry()        : weight sensor → OCCUPIED (RED), inTransit--
 *   EMERGENCY ENTRY— startVehicleEntryEmergency() : OCCUPIED immediately, inTransit++
 *                  — completeVehicleEntry()        : weight sensor confirmed, inTransit--
 *
 *   EXIT  Phase 1 — startVehicleExit()            : AVAILABLE (GREEN), inTransit++
 *   EXIT  Phase 2 — completeVehicleExit()          : gate opens / fee archived, inTransit--
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

    // ── Entry Phase 1 (normal): space → IN_TRANSIT (GREEN), gate opens ────────

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

        space.inTransit(sessionId);
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

    // ── Entry Phase 1 (emergency): gates already open, no kiosk/ticket ───────

    /**
     * Emergency variant — bypasses the emergency block, kiosk check, and gate
     * command. Space goes straight to IN_TRANSIT; weight sensor (Phase 2) will
     * flip it to OCCUPIED as normal.
     */
    public String startVehicleEntryEmergency(AllocationStrategy strategyOverride) {
        ParkingSpace space = parkingStructure.findAvailableSpace(strategyOverride);
        if (space == null) return null;

        String sessionId  = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        space.inTransit(sessionId);
        dataStoreDriver.getSessionLogger()
                .createSession(sessionId, space.getSpaceId(), space.getFloor(), now);
        dataStoreDriver.getCapacityMonitor().incrementOccupancy(space.getFloor());
        inTransitCount++;

        facilitiesOutputCtrl.updateFloorDisplay(space.getFloor(),
                dataStoreDriver.getCapacityMonitor().getAvailable(space.getFloor()));

        notifyObservers(new SystemEvent(EventType.ENTRY, sessionId,
                "Emergency entry — Space: " + space.getSpaceId()
                        + " | In-Transit: " + inTransitCount));

        return sessionId;
    }

    // ── Entry Phase 2: weight sensor fires → OCCUPIED (RED), inTransit-- ─────

    public void completeVehicleEntry(String sessionId) {
        dataStoreDriver.getSessionLogger().getSessionById(sessionId).ifPresent(session ->
                findSpaceById(session.getSpaceId()).ifPresent(space -> {
                    space.occupy(sessionId);
                    inTransitCount = Math.max(0, inTransitCount - 1);
                    notifyObservers(new SystemEvent(EventType.SESSION, sessionId,
                            "Weight sensor confirmed — Space " + session.getSpaceId()
                                    + " OCCUPIED | In-Transit: " + inTransitCount));
                })
        );
    }

    // ── Exit convenience wrapper (manual admin buttons) ───────────────────────

    public void handleVehicleExit(String sessionId) {
        startVehicleExit(sessionId);
        completeVehicleExit(sessionId);
    }

    // ── Exit Phase 1: weight sensor clears → AVAILABLE (GREEN), inTransit++ ──

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

    // ── Exit Phase 2: payment / gate / archive ────────────────────────────────

    public void completeVehicleExit(String sessionId) {
        dataStoreDriver.getSessionLogger().getSessionById(sessionId).ifPresent(session -> {
            LocalDateTime now = LocalDateTime.now();
            session.setExitTime(now);

            double fee = isEmergencyActive ? 0.0
                    : userInputCtrl.getExitKiosk().calculateFee(session);

            dataStoreDriver.getTransactionArchive().archiveTransaction(session, fee);
            dataStoreDriver.getCapacityMonitor().decrementOccupancy(session.getFloor());

            if (!isEmergencyActive) facilitiesOutputCtrl.openGate("EXIT");

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

    // ── Suggested space ───────────────────────────────────────────────────────

    public ParkingSpace getSuggestedSpace() {
        return parkingStructure.findAvailableSpace(AllocationStrategy.LOWEST_FLOOR_FIRST);
    }

    // ── Emergency ─────────────────────────────────────────────────────────────

    /**
     * Activates emergency state. Gates open permanently. Vehicles continue to
     * flow freely in both directions — the entry kiosk is intentionally NOT
     * disabled so the simulation can still process entries during evacuation.
     */
    public void activateEmergency() {
        if (isEmergencyActive) return;
        isEmergencyActive = true;
        emergencyOutputCtrl.engageEmergency();
        facilitiesOutputCtrl.openAllGates();
        notifyObservers(new SystemEvent(EventType.EMERGENCY, null,
                "Emergency state ACTIVATED — all gates open, vehicles flow freely, payment waived"));
    }

    public void deactivateEmergency() {
        if (!isEmergencyActive) return;
        isEmergencyActive = false;
        emergencyOutputCtrl.disengageEmergency();
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

    public Optional<ParkingSpace> findSpaceById(String spaceId) {
        for (Floor floor : parkingStructure.getFloors()) {
            for (ParkingSpace space: floor.getSpaces()) {
                if (space.getSpaceId().equals(spaceId)) return Optional.of(space);
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
    public void setUserInputCtrl(UserInputController ctrl)              { this.userInputCtrl = ctrl; }
    public void setFacilitiesInputCtrl(FacilitiesInputController ctrl)  { this.facilitiesInputCtrl = ctrl; }
    public void setEmergencyOutputCtrl(EmergencyOutputController ctrl)  { this.emergencyOutputCtrl = ctrl; }
    public void setFacilitiesOutputCtrl(FacilitiesOutputController ctrl){ this.facilitiesOutputCtrl = ctrl; }
    public void setDataStoreDriver(DataStoreDriver driver)              { this.dataStoreDriver = driver; }
    public void setParkingStructure(ParkingStructure structure)         { this.parkingStructure = structure; }
    public void setAllocationStrategy(AllocationStrategy strategy)      { this.allocationStrategy = strategy; }
}