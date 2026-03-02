package simulation;

import controller.MainController;
import enums.AllocationStrategy;
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

/**
 * SimulationEngine drives a realistic, self-running demo of the MPSMS.
 *
 * ── Entry sequence ────────────────────────────────────────────────────────────
 *   1. Induction Loop detected at entrance
 *   2. Entry Kiosk button pressed
 *   3. Gate actuated (OPEN)
 *   4. In-Transit count incremented and displayed          ← Phase 1 complete
 *   5. Weight Sensor detected → space OCCUPIED (IN_TRANSIT → OCCUPIED on GUI)
 *                                                          ← Phase 2 complete
 *
 * ── Exit sequence (normal) ───────────────────────────────────────────────────
 *   1. Weight Sensor detects car leaving → space AVAILABLE, LED → GREEN
 *   2. In-Transit count incremented
 *   3. Induction Loop detected at exit gate
 *   4. Ticket scanned / fee calculated
 *   5. Payment processed
 *   6. Gate actuated (OPEN)
 *   7. In-Transit count decremented
 *
 * ── Emergency mode ────────────────────────────────────────────────────────────
 *   Entry: gate is permanently open; entry flow is halted
 *   Exit:  steps 4 & 5 (payment) are skipped entirely; gate opens immediately
 *
 * ── Timing knobs ──────────────────────────────────────────────────────────────
 *   CYCLE_SECONDS — seconds between vehicle events   (default: 8)
 *   STEP_MS       — ms between individual log steps  (default: 700)
 */
public class SimulationEngine {

    private static final int    CYCLE_SECONDS = 8;
    private static final long   STEP_MS       = 700;
    private static final double RATE_PER_HOUR = 2.50;
    private static final double FILL_BIAS     = 0.60;

    private final MainController        mainController;
    private final Random                random;
    private ScheduledExecutorService    scheduler;
    private ScheduledFuture<?>          cycleHandle;
    private volatile boolean            running;

    public SimulationEngine(MainController mainController) {
        this.mainController = mainController;
        this.random         = new Random();
        this.running        = false;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void start() {
        if (running) return;
        running     = true;
        scheduler   = Executors.newScheduledThreadPool(4);
        cycleHandle = scheduler.scheduleAtFixedRate(
                this::decisionCycle, 1, CYCLE_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        running = false;
        if (cycleHandle != null) cycleHandle.cancel(false);
        if (scheduler   != null) scheduler.shutdownNow();
    }

    public boolean isRunning() { return running; }

    // ── Decision cycle ────────────────────────────────────────────────────────

    private void decisionCycle() {
        if (!running) return;

        boolean emergency = mainController.isEmergencyActive();
        int active = mainController.getDataStoreDriver()
                .getSessionLogger().getActiveSessions().size();

        // During emergency only drain vehicles out
        if (emergency) {
            if (active > 0) scheduler.submit(this::simulateEmergencyExitFlow);
            return;
        }

        int total    = mainController.getParkingStructure().getTotalCapacity();
        int occupied = mainController.getParkingStructure().getTotalOccupancy();
        boolean canEnter = occupied < total;
        boolean canExit  = active > 0;

        if (!canEnter && !canExit) return;
        if (!canEnter) { scheduler.submit(this::simulateExitFlow);  return; }
        if (!canExit)  { scheduler.submit(this::simulateEntryFlow); return; }

        double fillRatio        = (double) occupied / total;
        double entryProbability = fillRatio < FILL_BIAS ? 0.75 : 0.45;

        if (random.nextDouble() < entryProbability) {
            scheduler.submit(this::simulateEntryFlow);
        } else {
            scheduler.submit(this::simulateExitFlow);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ENTRY FLOW
    // ═════════════════════════════════════════════════════════════════════════

    private void simulateEntryFlow() {

        // 1. Induction Loop detected
        log(EventType.GATE, "LOOP-ENTRY",
                "━━ ENTRY ━━  Induction Loop [LOOP-ENTRY]: Vehicle detected at entrance — signal HIGH");

        // 2. Entry Kiosk button pressed
        sleep(1);
        log(EventType.SESSION, "ENTRY-KIOSK",
                "Entry Kiosk: Ticket request button pressed by driver");

        // Show system suggestion before committing to a space
        ParkingSpace suggested = mainController.getSuggestedSpace();
        if (suggested != null) {
            log(EventType.SESSION, suggested.getSpaceId(),
                    "System Suggestion: Recommended → " + suggested.getSpaceId()
                            + "  (Floor " + suggested.getFloor() + ", closest to entrance)"
                            + " — driver may choose any available space");
        }

        // 3. Gate actuated + 4. In-Transit count updated  [Phase 1]
        sleep(2);
        String sessionId = mainController.startVehicleEntry(AllocationStrategy.RANDOM);
        if (sessionId == null) return;

        String assignedSpace = getSessionSpaceId(sessionId);
        if (assignedSpace != null) {
            boolean tookSuggestion = suggested != null
                    && assignedSpace.equals(suggested.getSpaceId());
            log(EventType.SESSION, assignedSpace,
                    "Driver selected: " + assignedSpace
                            + (tookSuggestion
                            ? " (accepted system suggestion)"
                            : " (chose independently — differs from suggestion)"));
        }

        log(EventType.GATE, "GATE-ENTRY",
                "Gate Actuator [ENTRY]: RAISED — barrier arm up, vehicle proceeding");
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) en route to stall — capacity display adjusted");

        // 5. Weight Sensor detected → space transitions IN_TRANSIT → OCCUPIED  [Phase 2]
        sleep(3);
        log(EventType.SESSION, assignedSpace,
                "Weight Sensor [" + assignedSpace
                        + "]: Weight increase detected — vehicle arriving at stall");

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
        List<Session> active = mainController.getDataStoreDriver()
                .getSessionLogger().getActiveSessions();
        if (active.isEmpty()) return;

        Session session  = active.get(random.nextInt(active.size()));
        String sessionId = session.getSessionId();
        String spaceId   = session.getSpaceId();

        long   minutes = Math.max(session.getDuration().toMinutes(), 1);
        double fee     = Math.ceil(minutes / 60.0) * RATE_PER_HOUR;

        // 1. Weight Sensor detects car leaving stall → space AVAILABLE
        log(EventType.EXIT, sessionId,
                "━━ EXIT ━━  Weight Sensor [" + spaceId
                        + "]: Weight decrease detected — vehicle departing stall");

        sleep(1);
        mainController.startVehicleExit(sessionId);

        log(EventType.SESSION, spaceId,
                "Spot LED Indicator [" + spaceId + "]: Color → GREEN (available)");

        // 2. In-Transit count updated
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) en route to exit");

        // 3. Induction Loop detected at exit gate
        sleep(2);
        log(EventType.GATE, "LOOP-EXIT",
                "Induction Loop [LOOP-EXIT]: Vehicle detected at exit gate — signal HIGH");

        // 4. Ticket scanned / fee calculated
        sleep(3);
        log(EventType.SESSION, sessionId,
                "Exit Kiosk: Ticket scanned — Session ID: " + sessionId
                        + "  |  Space: " + spaceId);

        sleep(4);
        log(EventType.SESSION, sessionId,
                "Exit Kiosk: Duration = " + minutes + " min"
                        + "  |  Fee = $" + String.format("%.2f", fee));

        sleep(5);
        log(EventType.SESSION, sessionId,
                "Payment Gateway: Authorisation request sent — processing card...");

        // 5. Payment processed
        sleep(6);
        log(EventType.SESSION, sessionId,
                "Payment Gateway: APPROVED — transaction authorised ($"
                        + String.format("%.2f", fee) + ")");

        // 6. Gate actuated
        sleep(7);
        mainController.completeVehicleExit(sessionId);

        log(EventType.GATE, "GATE-EXIT",
                "Gate Actuator [EXIT]: RAISED — barrier arm up, vehicle proceeding");

        // 7. In-Transit count decremented
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
    // EMERGENCY EXIT FLOW — no payment, gate already open
    // ═════════════════════════════════════════════════════════════════════════

    private void simulateEmergencyExitFlow() {
        List<Session> active = mainController.getDataStoreDriver()
                .getSessionLogger().getActiveSessions();
        if (active.isEmpty()) return;

        Session session  = active.get(random.nextInt(active.size()));
        String sessionId = session.getSessionId();
        String spaceId   = session.getSpaceId();

        // 1. Weight Sensor detects car leaving
        log(EventType.EXIT, sessionId,
                "━━ EMERGENCY EXIT ━━  Weight Sensor [" + spaceId
                        + "]: Vehicle departing stall");

        sleep(1);
        mainController.startVehicleExit(sessionId);

        log(EventType.SESSION, spaceId,
                "Spot LED Indicator [" + spaceId + "]: Color → GREEN (available)");

        // 2. In-Transit count updated
        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) evacuating");

        // 3. Induction Loop detected
        sleep(2);
        log(EventType.GATE, "LOOP-EXIT",
                "Induction Loop [LOOP-EXIT]: Vehicle detected at exit gate");

        // 4 & 5 SKIPPED — no payment during emergency
        sleep(3);
        log(EventType.SESSION, sessionId,
                "EMERGENCY MODE: Payment steps BYPASSED — no ticket or card required");

        // 6. Gate open (already raised by emergency override)
        log(EventType.GATE, "GATE-EXIT",
                "Gate Actuator [EXIT]: OPEN (emergency override active — barrier permanently raised)");

        // 7. In-Transit count decremented
        sleep(4);
        mainController.completeVehicleExit(sessionId);

        log(EventType.CAPACITY, null,
                "In-Transit Count updated: " + mainController.getInTransitCount()
                        + " vehicle(s) remaining");
        log(EventType.SESSION, sessionId,
                "Data Store: Session " + sessionId + " closed — fee WAIVED (emergency)");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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