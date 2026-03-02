package gui;

import controller.MainController;
import model.Session;
import observer.SystemEvent;
import observer.SystemObserver;
import simulation.SimulationEngine;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ControlConsolePanel extends JPanel implements SystemObserver {

    private final MainController     mainController;
    private DefaultTableModel        sessionTableModel;
    private DefaultTableModel        transactionTableModel;
    private SimulationEngine         simulationEngine;

    public ControlConsolePanel(MainController mainController) {
        this.mainController = mainController;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(300, 0));
        setBorder(BorderFactory.createTitledBorder("Control Console"));

        add(buildEmergencyPanel());
        add(buildSpacePanel());
        add(buildCapacityPanel());
        add(buildSimulationPanel());
        add(buildSessionPanel());
    }

    // ── Emergency ─────────────────────────────────────────────────────────────

    public JPanel buildEmergencyPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 4, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Emergency"));

        JButton activateBtn = new JButton("Activate");
        activateBtn.setBackground(Color.RED);
        activateBtn.setForeground(Color.WHITE);
        activateBtn.setOpaque(true);
        activateBtn.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this,
                    "Activate emergency state?", "Confirm Emergency",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.YES_OPTION) mainController.getAdminCommands().triggerEmergency();
        });

        JButton deactivateBtn = new JButton("Deactivate");
        deactivateBtn.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this,
                    "Deactivate emergency state?", "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (r == JOptionPane.YES_OPTION) mainController.getAdminCommands().cancelEmergency();
        });

        panel.add(activateBtn);
        panel.add(deactivateBtn);
        return panel;
    }

    // ── Space management ──────────────────────────────────────────────────────

    public JPanel buildSpacePanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Space Management"));

        JTextField spaceField = new JTextField();
        JButton restrictBtn   = new JButton("Restrict");
        JButton unrestrictBtn = new JButton("Unrestrict");
        JButton reserveBtn    = new JButton("Reserve");
        JButton unreserveBtn  = new JButton("Unreserve");

        restrictBtn.addActionListener(e -> {
            String id = spaceField.getText().trim();
            if (!id.isEmpty()) mainController.getAdminCommands().restrictSpace(id);
        });
        unrestrictBtn.addActionListener(e -> {
            String id = spaceField.getText().trim();
            if (!id.isEmpty()) mainController.getAdminCommands().unrestrictSpace(id);
        });
        reserveBtn.addActionListener(e -> {
            String id = spaceField.getText().trim();
            if (!id.isEmpty()) mainController.getAdminCommands().reserveSpace(id);
        });
        unreserveBtn.addActionListener(e -> {
            String id = spaceField.getText().trim();
            if (!id.isEmpty()) mainController.getAdminCommands().unreserveSpace(id);
        });

        panel.add(new JLabel("Space ID:"));
        panel.add(spaceField);
        panel.add(restrictBtn);
        panel.add(unrestrictBtn);
        panel.add(reserveBtn);
        panel.add(unreserveBtn);
        return panel;
    }

    // ── Capacity summary ──────────────────────────────────────────────────────

    public JPanel buildCapacityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Capacity"));

        JTextArea summary = new JTextArea(4, 20);
        summary.setEditable(false);
        summary.setFont(new Font("Monospaced", Font.PLAIN, 11));

        StringBuilder sb = new StringBuilder();
        for (var floor : mainController.getParkingStructure().getFloors()) {
            int cap   = floor.getCapacity();
            int occ   = floor.getOccupiedCount();
            int avail = cap - occ;
            sb.append(String.format("Floor %d: %d/%d occupied (%d avail)%n",
                    floor.getFloorNumber(), occ, cap, avail));
        }
        summary.setText(sb.toString());
        panel.add(new JScrollPane(summary), BorderLayout.CENTER);
        return panel;
    }

    // ── Simulation ────────────────────────────────────────────────────────────

    public JPanel buildSimulationPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Simulation"));

        JButton entryBtn = new JButton("Simulate Entry");
        entryBtn.addActionListener(e -> mainController.getAdminCommands().simulateEntry());

        JButton exitBtn = new JButton("Simulate Exit");
        exitBtn.addActionListener(e -> {
            List<Session> active = mainController.getDataStoreDriver()
                    .getSessionLogger().getActiveSessions();
            if (active.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No active sessions.");
                return;
            }
            String[] ids = active.stream().map(Session::getSessionId).toArray(String[]::new);
            String selected = (String) JOptionPane.showInputDialog(this,
                    "Select session to exit:", "Simulate Exit",
                    JOptionPane.PLAIN_MESSAGE, null, ids, ids[0]);
            if (selected != null) mainController.getAdminCommands().simulateExit(selected);
        });

        JButton autoBtn = new JButton("▶  Auto Simulate");
        autoBtn.setBackground(new Color(34, 139, 34));
        autoBtn.setForeground(Color.WHITE);
        autoBtn.setOpaque(true);
        autoBtn.setFont(autoBtn.getFont().deriveFont(Font.BOLD));
        autoBtn.addActionListener(e -> {
            if (simulationEngine == null) {
                simulationEngine = new SimulationEngine(mainController);
            }
            if (simulationEngine.isRunning()) {
                simulationEngine.stop();
                autoBtn.setText("▶  Auto Simulate");
                autoBtn.setBackground(new Color(34, 139, 34));
            } else {
                simulationEngine.start();
                autoBtn.setText("⏹  Stop Simulation");
                autoBtn.setBackground(new Color(180, 30, 30));
            }
        });

        panel.add(entryBtn);
        panel.add(exitBtn);
        panel.add(autoBtn);
        panel.add(new JLabel());   // empty fourth cell
        return panel;
    }

    // ── Sessions & Transactions ───────────────────────────────────────────────

    public JPanel buildSessionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Sessions & Transactions"));

        sessionTableModel = new DefaultTableModel(
                new String[]{"Session ID", "Space", "Floor", "Entry Time"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        transactionTableModel = new DefaultTableModel(
                new String[]{"Session ID", "Exit Time", "Fee ($)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable sessionTable     = new JTable(sessionTableModel);
        JTable transactionTable = new JTable(transactionTableModel);

        // ── Clear buttons ─────────────────────────────────────────────────────
        JButton clearSessionsBtn = new JButton("Clear");
        clearSessionsBtn.setToolTipText("Clear active sessions display");
        clearSessionsBtn.addActionListener(e -> sessionTableModel.setRowCount(0));

        JButton clearTransBtn = new JButton("Clear");
        clearTransBtn.setToolTipText("Clear transactions display");
        clearTransBtn.addActionListener(e -> transactionTableModel.setRowCount(0));

        // Wrap each table with its clear button in a titled sub-panel
        JPanel sessionTab = new JPanel(new BorderLayout());
        sessionTab.add(new JScrollPane(sessionTable), BorderLayout.CENTER);
        sessionTab.add(clearSessionsBtn, BorderLayout.SOUTH);

        JPanel transactionTab = new JPanel(new BorderLayout());
        transactionTab.add(new JScrollPane(transactionTable), BorderLayout.CENTER);
        transactionTab.add(clearTransBtn, BorderLayout.SOUTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Active Sessions", sessionTab);
        tabs.add("Transactions",    transactionTab);

        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                mainController.getAdminCommands()
                        .exportData(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        panel.add(tabs,      BorderLayout.CENTER);
        panel.add(exportBtn, BorderLayout.SOUTH);
        return panel;
    }

    // ── SystemObserver ────────────────────────────────────────────────────────

    @Override
    public void onSystemEvent(SystemEvent event) {
        SwingUtilities.invokeLater(() -> {
            sessionTableModel.setRowCount(0);
            for (Session s : mainController.getDataStoreDriver()
                    .getSessionLogger().getActiveSessions()) {
                sessionTableModel.addRow(new Object[]{
                        s.getSessionId(), s.getSpaceId(), s.getFloor(), s.getEntryTime()});
            }
            transactionTableModel.setRowCount(0);
            for (var t : mainController.getDataStoreDriver()
                    .getTransactionArchive().getAllTransactions()) {
                transactionTableModel.addRow(new Object[]{
                        t.getSessionId(), t.getExitTime(),
                        String.format("%.2f", t.getFee())});
            }
        });
    }
}