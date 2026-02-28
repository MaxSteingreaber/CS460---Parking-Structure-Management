import controller.*;
import datastore.DataStoreDriver;
import enums.AllocationStrategy;
import gui.DashboardFrame;
import input.*;
import model.*;
import output.*;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Application entry point. Wires together all subsystems and launches the
 * administrative dashboard. The parking structure is created with default
 * dimensions here; adjust floors/rows/columns as needed.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            // ── Build parking structure: 3 floors, 4 rows × 6 columns ──────────
            List<Floor> floors = new ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                floors.add(new Floor(i, 4, 6));
            }
            ParkingStructure structure = new ParkingStructure("Demo Parking Structure", floors);

            // ── Data store ───────────────────────────────────────────────────────
            DataStoreDriver dataStore = new DataStoreDriver(Paths.get("data"));

            // ── Output devices ───────────────────────────────────────────────────
            Siren            siren          = new Siren(3000);
            LEDController    led            = new LEDController();
            List<GateActuator> gates        = Arrays.asList(
                    new GateActuator("ENTRY"), new GateActuator("EXIT"));
            List<FloorLevelDisplay> floorDisplays = new ArrayList<>();
            for (Floor f : floors) {
                floorDisplays.add(new FloorLevelDisplay(f.getFloorNumber(), f.getCapacity()));
            }
            EntranceDisplay  entranceDisplay = new EntranceDisplay();
            TicketDispenser  ticketDispenser = new TicketDispenser();

            // ── Controller setup ─────────────────────────────────────────────────
            MainController mainController = new MainController();
            mainController.setParkingStructure(structure);
            mainController.setDataStoreDriver(dataStore);
            mainController.setAllocationStrategy(AllocationStrategy.LOWEST_FLOOR_FIRST);

            EmergencyOutputController  emergencyCtrl    = new EmergencyOutputController(siren, led);
            FacilitiesOutputController facilitiesOutCtrl = new FacilitiesOutputController(
                    gates, floorDisplays, entranceDisplay, ticketDispenser);

            // ── Input devices ────────────────────────────────────────────────────
            EntryKiosk       entryKiosk    = new EntryKiosk();
            ExitKiosk        exitKiosk     = new ExitKiosk();
            AdminCommands    adminCmds     = new AdminCommands(mainController);
            UserInputController userInputCtrl = new UserInputController(entryKiosk, exitKiosk, adminCmds);

            EmergencySignal    emergencySignal    = new EmergencySignal();
            PowerFaultDetector powerFaultDetector = new PowerFaultDetector();
            List<InductionLoopSensor> loops       = Arrays.asList(
                    new InductionLoopSensor("LOOP-ENTRY"),
                    new InductionLoopSensor("LOOP-EXIT"));
            FacilitiesInputController facilitiesInCtrl = new FacilitiesInputController(
                    loops, new ArrayList<>(), emergencySignal, powerFaultDetector);

            // ── Wire everything into MainController ──────────────────────────────
            mainController.setEmergencyOutputCtrl(emergencyCtrl);
            mainController.setFacilitiesOutputCtrl(facilitiesOutCtrl);
            mainController.setUserInputCtrl(userInputCtrl);
            mainController.setFacilitiesInputCtrl(facilitiesInCtrl);

            mainController.initialize();

            // ── Launch GUI ───────────────────────────────────────────────────────
            DashboardFrame frame = new DashboardFrame(mainController);
            frame.setVisible(true);
        });
    }
}
