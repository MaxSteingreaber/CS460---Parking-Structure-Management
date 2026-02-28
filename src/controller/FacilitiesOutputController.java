package controller;

import model.Ticket;
import output.EntranceDisplay;
import output.FloorLevelDisplay;
import output.GateActuator;
import output.TicketDispenser;

import java.time.LocalDateTime;
import java.util.List;

public class FacilitiesOutputController {

    private final List<GateActuator>      gateActuators;
    private final List<FloorLevelDisplay> floorDisplays;
    private final EntranceDisplay         entranceDisplay;
    private final TicketDispenser         ticketDispenser;

    public FacilitiesOutputController(List<GateActuator> gateActuators,
                                      List<FloorLevelDisplay> floorDisplays,
                                      EntranceDisplay entranceDisplay,
                                      TicketDispenser ticketDispenser) {
        this.gateActuators   = gateActuators;
        this.floorDisplays   = floorDisplays;
        this.entranceDisplay = entranceDisplay;
        this.ticketDispenser = ticketDispenser;
    }

    public void openGate(String gateId) {
        gateActuators.stream()
                .filter(g -> g.getGateId().equals(gateId))
                .findFirst()
                .ifPresent(GateActuator::open);
    }

    public void closeGate(String gateId) {
        gateActuators.stream()
                .filter(g -> g.getGateId().equals(gateId))
                .findFirst()
                .ifPresent(GateActuator::close);
    }

    /** Opens all gates simultaneously (used during emergency). */
    public void openAllGates() {
        gateActuators.forEach(GateActuator::open);
    }

    public void updateFloorDisplay(int floor, int available) {
        floorDisplays.stream()
                .filter(d -> d.getFloorNumber() == floor)
                .findFirst()
                .ifPresent(d -> d.update(available));
    }

    public void setEntranceMessage(String msg) {
        entranceDisplay.setMessage(msg);
    }

    public Ticket issueTicket(String sessionId, String spaceId, LocalDateTime entryTime) {
        return ticketDispenser.issueTicket(sessionId, spaceId, entryTime);
    }

    public List<GateActuator> getGateActuators()      { return gateActuators; }
    public EntranceDisplay    getEntranceDisplay()     { return entranceDisplay; }
    public TicketDispenser    getTicketDispenser()     { return ticketDispenser; }
}
