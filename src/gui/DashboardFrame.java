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
 * Organizes the four-region layout: banner (N), control console (W),
 * structure view (C), and event log (S).
 */
public class DashboardFrame extends JFrame implements SystemObserver {

    private final MainController      mainController;
    private final BannerPanel         bannerPanel;
    private final ControlConsolePanel controlConsole;
    private final StructureViewPanel  structureView;
    private final EventLogPanel       eventLog;

    public DashboardFrame(MainController mainController) {
        this.mainController = mainController;
        this.bannerPanel    = new BannerPanel(mainController.getParkingStructure().getStructureName());
        this.controlConsole = new ControlConsolePanel(mainController);
        this.structureView  = new StructureViewPanel(mainController.getParkingStructure());
        this.eventLog       = new EventLogPanel();

        mainController.addObserver(this);
        mainController.addObserver(controlConsole);
        mainController.addObserver(structureView);
        mainController.addObserver(eventLog);

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
        setMinimumSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);
    }

    public void initializeLayout() {
        setLayout(new BorderLayout());
        add(bannerPanel,    BorderLayout.NORTH);
        add(controlConsole, BorderLayout.WEST);
        add(structureView,  BorderLayout.CENTER);
        add(eventLog,       BorderLayout.SOUTH);
    }

    public void updateBanner(SystemState state) {
        bannerPanel.updateState(state);
    }

    @Override
    public void onSystemEvent(SystemEvent event) {
        SwingUtilities.invokeLater(() -> updateBanner(mainController.getSystemState()));
    }
}
