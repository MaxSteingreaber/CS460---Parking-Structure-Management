package simulation;

import controller.MainController;
import enums.AllocationStrategy;
import enums.SpaceState;
import model.Floor;
import model.ParkingSpace;
import model.Session;
import observer.EventType;
import observer.SystemEvent;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * SimulationEngine — self-running demo driver for the MPSMS.
 *
 * Entry and exit run on fully independent schedulers so they can be started
 * and stopped individually or together.
 *
 * Exit safety guarantee: a session is only eligible to exit once its
 * assigned parking space reaches SpaceState.OCCUPIED. Vehicles still
 * travelling from the gate (IN_TRANSIT) are never selected for exit.
 */
public class SimulationEngine {

    private static final int    CYCLE_SECONDS          = 8;
    private static final long   STEP_MS                = 800;
    private static final double SUGGESTION_FOLLOW_RATE = 0.80;
    private static final long   MIN_PARK_SECONDS       = 15;
    private static final double RATE_PER_HOUR          = 2.50;

    private final MainController     mainController;
    private final Random             random;

    // ── Independent entry scheduler ───────────────────────────────────────────
    private ScheduledExecutorService entryScheduler;
    private ScheduledFuture<?>       entryHandle;
    private volatile boolean         entryRunning = false;

    // ── Independent exit scheduler ────────────────────────────────────────────
    private ScheduledExecutorService exitScheduler;
    private ScheduledFuture<?>       exitHandle;
    private volatile boolean         exitRunning  = false;

    public SimulationEngine(MainController mainController) {
        this.mainController = mainController;
        this.random         = new Random();
    }

    // ── Entry lifecycle ───────────────────────────────────────────────────────

    public void startEntry() {
        if (entryRunning) return;
        entryRunning   = true;
        entryScheduler = Executors.newScheduledThreadPool(2);
        entryHandle    = entryScheduler.scheduleAtFixedRate(
                this::entryCycle, 1, CYCLE_SECONDS, TimeUnit.SECONDS);
    }

    public void stopEntry() {
        entryRunning = false;
        if (entryHandle    != null) entryHandle.cancel(false);
        if (entryScheduler != null) entryScheduler.shutdownNow();
    }

    public boolean isEntryRunning() { return entryRunning; }

    // ── Exit lifecycle ────────────────────────────────────────────────────────

    public void startExit() {
        if (exitRunning) return;
        exitRunning   = true;
        exitScheduler = Executors.newScheduledThreadPool(2);
        exitHandle    = exitScheduler.scheduleAtFixedRate(
                this::exitCycle, 1, CYCLE_SECONDS, TimeUnit.SECONDS);
    }

    public void stopExit() {
        exitRunning = false;
        if (exitHandle    != null) exitHandle.cancel(false);
        if (exitScheduler != null) exitScheduler.shutdownNow();
    }

    public boolean isExitRunning() { return exitRunning; }

    // ── Convenience: start / stop both together ───────────────────────────────

    public void startBoth() { startEntry(); startExit(); }
    public void stopBoth()  { stopEntry();  stopExit(); }
    public boolean isRunning() { return entryRunning || exitRunning; }

    // ── Decision cycles ───────────────────────────────────────────────────────

    private void entryCycle() {
        if (!entryRunning) return;
        int total   = mainController.getParkingStructure().getTotalCapacity();
        int occupied = mainController.getParkingStructure().getTotalOccupancy();
        if (occupied >= total) return;   // structure full — skip

        if (mainController.isEmergencyActive())
            entryScheduler.submit(this::simulateEmergencyEntryFlow);
        else
            entryScheduler.submit(this::simulateEntryFlow);
    }

    private void exitCycle() {
        if (!exitRunning) return;
        if (getExitEligibleSessions().isEmpty()) return;   // nothing ready — skip

        if (mainController.isEmergencyActive())
            exitScheduler.submit(this::simulateEmergencyExitFlow);
        else
            exitScheduler.submit(this::simulateExitFlow);
    }

    // ── Exit eligibility helper ───────────────────────────────────────────────

    /**
     * Returns active sessions whose space is fully OCCUPIED and, in normal
     * mode, that have been parked at least MIN_PARK_SECONDS.
     * Vehicles still IN_TRANSIT (en route from the gate) are excluded.
     */
    private List<Session> getExitEligibleSessions() {
        boolean emergency = mainController.isEmergencyActive();
        return mainController.getDataStoreDriver()
                .getSessionLogger().getActiveSessions().stream()
                .filter(s -> {
                    ParkingSpace space = findSpace(s.getSpaceId());
                    return space != null && space.getState() == SpaceState.OCCUPIED;
                })
                .filter(s -> emergency || s.getDuration().getSeconds() >= MIN_PARK_SECONDS)
                .collect(Collectors.toList());
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ENTRY FLOW (normal)
    // ═════════════════════════════════════════════════════════════════════════

    private void simulateEntryFlow() {

        log(EventType.GATE, "LOOP-ENTRY",
                "━━ ENTRY ━━  Induction Loop [LOOP-ENTRY]: Vehicle detected at entrance — signal HIGH");

        sleep(1);
        log(EventType.SESSION, "ENTRY-KIOSK",
                "Entry Kiosk: Ticket request button pressed by driver");

        ParkingSpace suggested = mainController.getSuggestedSpace();
        if (suggested != null) {
            log(EventType.SESSION, suggested.getSpaceId(),
                    "System Suggestion: Recommended → " + suggested.getSpaceId()
                            + "  (Floor " + suggested.getFloor() + ", closest to entrance)"
                            + " — driver may choose any available space");
        }

        boolean followsSuggestion = random.nextDouble() < SUGGESTION_FOLLOW_RATE;
        AllocationStrategy strategy = followsSuggestion
                ? AllocationStrategy.LOWEST_FLOOR_FIRST
                : AllocationStrategy.RANDOM;

        sleep(2);
        String sessionId = mainController.startVehicleEntry(strategy);
        if (sessionId == null) return;

        String assignedSpace = getSessionSpaceId(sessionId);
        if (assignedSpace != null) {
            log(EventType.SESSION, assignedSpace, followsSuggestion
                    ? "Driver accepted suggestion → parked at " + assignedSpace
                    : "Driver chose independently → parked at " + assignedSpace
                    + (suggested != null ? " (suggested was " + suggested.getSpaceId() + ")" : ""));
        }

        log(EventType.GATE, "GATE-ENTRY",
                "Gate Actuator [ENTRY]: RAISED — barrier arm up, vehicle proceeding");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) en route to stall");

        sleep(3);
        log(EventType.SESSION, assignedSpace,
                "Weight Sensor [" + assignedSpace + "]: Weight increase detected — vehicle arriving at stall");

        sleep(4);
        mainController.completeVehicleEntry(sessionId);

        log(EventType.SESSION, assignedSpace,
                "Weight Sensor [" + assignedSpace + "]: OCCUPIED confirmed (~1,450 kg)");
        log(EventType.SESSION, assignedSpace,
                "Spot LED Indicator [" + assignedSpace + "]: Color → RED (occupied)");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) remaining in transit");

        sleep(5);
        log(EventType.GATE, "LOOP-ENTRY",
                "Induction Loop [LOOP-ENTRY]: Loop CLEAR — vehicle has cleared entrance gate");
        log(EventType.GATE, "GATE-ENTRY",
                "Gate Actuator [ENTRY]: LOWERED — barrier arm down, entrance closed");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EXIT FLOW (normal)
    // ═════════════════════════════════════════════════════════════════════════

    private void simulateExitFlow() {
        List<Session> eligible = getExitEligibleSessions();
        if (eligible.isEmpty()) return;

        Session session  = eligible.get(random.nextInt(eligible.size()));
        String sessionId = session.getSessionId();
        String spaceId   = session.getSpaceId();
        long   minutes   = session.getDuration().toMinutes();
        double fee       = Math.ceil(minutes / 60.0) * RATE_PER_HOUR;

        log(EventType.EXIT, sessionId,
                "━━ EXIT ━━  Weight Sensor [" + spaceId
                        + "]: Weight decrease detected — vehicle departing stall");

        sleep(1);
        mainController.startVehicleExit(sessionId);

        log(EventType.SESSION, spaceId,
                "Spot LED Indicator [" + spaceId + "]: Color → GREEN (available)");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) en route to exit");

        sleep(2);
        log(EventType.GATE, "LOOP-EXIT",
                "Induction Loop [LOOP-EXIT]: Vehicle detected at exit gate — signal HIGH");

        sleep(3);
        log(EventType.SESSION, sessionId,
                "Exit Kiosk: Ticket scanned — Session ID: " + sessionId
                        + "  |  Space: " + spaceId);

        sleep(4);
        log(EventType.SESSION, sessionId,
                "Exit Kiosk: Duration = " + minutes + " min  |  Fee = $"
                        + String.format("%.2f", fee));

        sleep(5);
        log(EventType.SESSION, sessionId,
                "Payment Gateway: Authorisation request sent — processing card...");

        sleep(6);
        log(EventType.SESSION, sessionId,
                "Payment Gateway: APPROVED — transaction authorised ($"
                        + String.format("%.2f", fee) + ")");

        sleep(7);
        mainController.completeVehicleExit(sessionId);

        log(EventType.GATE, "GATE-EXIT",
                "Gate Actuator [EXIT]: RAISED — barrier arm up, vehicle proceeding");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) remaining in transit");

        sleep(8);
        log(EventType.GATE, "LOOP-EXIT",
                "Induction Loop [LOOP-EXIT]: Loop CLEAR — vehicle has cleared exit gate");
        log(EventType.GATE, "GATE-EXIT",
                "Gate Actuator [EXIT]: LOWERED — barrier arm down, exit closed");
        log(EventType.SESSION, sessionId,
                "Data Store: Transaction archived — Session " + sessionId + " closed");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EMERGENCY ENTRY FLOW — gates permanently open, no actuation
    // ═════════════════════════════════════════════════════════════════════════

    private void simulateEmergencyEntryFlow() {
        log(EventType.ENTRY, "LOOP-ENTRY",
                "━━ EMERGENCY ENTRY ━━  Vehicle arriving — all gates OPEN (no actuation required)");

        AllocationStrategy strategy = random.nextDouble() < SUGGESTION_FOLLOW_RATE
                ? AllocationStrategy.LOWEST_FLOOR_FIRST
                : AllocationStrategy.RANDOM;

        sleep(1);
        String sessionId = mainController.startVehicleEntryEmergency(strategy);
        if (sessionId == null) return;

        String spaceId = getSessionSpaceId(sessionId);
        log(EventType.SESSION, spaceId,
                "EMERGENCY: Vehicle entering freely — Space " + spaceId
                        + " assigned | No ticket issued, no gate command sent");

        sleep(2);
        mainController.completeVehicleEntry(sessionId);

        log(EventType.SESSION, spaceId,
                "Weight Sensor [" + spaceId + "]: OCCUPIED — vehicle parked (emergency mode)");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) remaining in transit");
    }

    // ═════════════════════════════════════════════════════════════════════════
    // EMERGENCY EXIT FLOW — gates permanently open, no actuation
    // ═════════════════════════════════════════════════════════════════════════

    private void simulateEmergencyExitFlow() {
        List<Session> eligible = getExitEligibleSessions();
        if (eligible.isEmpty()) return;

        Session session  = eligible.get(random.nextInt(eligible.size()));
        String sessionId = session.getSessionId();
        String spaceId   = session.getSpaceId();

        log(EventType.EXIT, sessionId,
                "━━ EMERGENCY EXIT ━━  Weight Sensor [" + spaceId
                        + "]: Vehicle departing stall — gates remain OPEN, no actuation");

        sleep(1);
        mainController.startVehicleExit(sessionId);

        log(EventType.SESSION, spaceId,
                "Spot LED [" + spaceId + "]: GREEN (available) | No gate command issued");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) evacuating");

        sleep(2);
        log(EventType.SESSION, sessionId,
                "EMERGENCY MODE: Payment BYPASSED — vehicle exiting freely, no ticket or card required");

        sleep(3);
        mainController.completeVehicleExit(sessionId);

        log(EventType.SESSION, sessionId,
                "Session " + sessionId + " closed — fee WAIVED (emergency) | Vehicle clear");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) remaining");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ParkingSpace findSpace(String spaceId) {
        for (Floor f : mainController.getParkingStructure().getFloors()) {
            for (int r = 0; r < f.getRows(); r++) {
                for (int c = 0; c < f.getColumns(); c++) {
                    ParkingSpace sp = f.getSpace(r, c);
                    if (sp.getSpaceId().equals(spaceId)) return sp;
                }
            }
        }
        return null;
    }

    private void log(EventType type, String targetId, String message) {
        mainController.notifyObservers(new SystemEvent(type, targetId, message));
    }

    private void sleep(int stepIndex) {
        if (stepIndex == 0) return;
        try {
            Thread.sleep(stepIndex * STEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getSessionSpaceId(String sessionId) {
        return mainController.getDataStoreDriver()
                .getSessionLogger()
                .getSessionById(sessionId)
                .map(Session::getSpaceId)
                .orElse(null);
    }
}