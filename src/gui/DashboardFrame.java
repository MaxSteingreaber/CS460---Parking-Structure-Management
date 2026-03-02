package gui;

import controller.MainController;
import enums.SystemState;
import observer.SystemEvent;
import observer.SystemObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Top-level Swing JFrame hosting the entire dashboard application.
 * Layout:
 *   NORTH  — BannerPanel         (status + clock)
 *   WEST   — ControlConsolePanel (admin controls)
 *   CENTER — StructureViewPanel  (floor grid)
 *   SOUTH  — EventLogPanel       (sensor event log)
 *   EAST   — DriverDisplayPanel  (LED-style driver guidance display)
 */
public class DashboardFrame extends JFrame implements SystemObserver {

    private final MainController      mainController;
    private final BannerPanel         bannerPanel;
    private final ControlConsolePanel controlConsole;
    private final StructureViewPanel  structureView;
    private final EventLogPanel       eventLog;
    private final DriverDisplayPanel  driverDisplay;

    public DashboardFrame(MainController mainController) {
        this.mainController = mainController;
        this.bannerPanel    = new BannerPanel(
                mainController.getParkingStructure().getStructureName());
        this.controlConsole = new ControlConsolePanel(mainController);
        this.structureView  = new StructureViewPanel(
                mainController.getParkingStructure());
        this.eventLog       = new EventLogPanel();
        this.driverDisplay  = new DriverDisplayPanel(mainController);

        mainController.addObserver(this);
        mainController.addObserver(controlConsole);
        mainController.addObserver(structureView);
        mainController.addObserver(eventLog);
        mainController.addObserver(driverDisplay);

        initializeLayout();
        bannerPanel.startClock();

        setTitle("MPSMS Administrative Dashboard");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                mainController.getDataStoreDriver().close();
                dispose();
                System.exit(0);
            }
        });

        pack();
        setMinimumSize(new Dimension(1400, 800));
        setLocationRelativeTo(null);
    }

    public void initializeLayout() {
        setLayout(new BorderLayout());
        add(bannerPanel,    BorderLayout.NORTH);
        add(controlConsole, BorderLayout.WEST);
        add(structureView,  BorderLayout.CENTER);
        add(eventLog,       BorderLayout.SOUTH);
        add(driverDisplay,  BorderLayout.EAST);
    }

    public void updateBanner(SystemState state) {
        bannerPanel.updateState(state);
    }

    @Override
    public void onSystemEvent(SystemEvent event) {
        SwingUtilities.invokeLater(() -> updateBanner(mainController.getSystemState()));
    }
}