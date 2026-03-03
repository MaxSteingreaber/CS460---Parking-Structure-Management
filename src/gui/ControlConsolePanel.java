package gui;

import controller.MainController;
import model.Floor;
import model.Session;
import observer.SystemEvent;
import observer.SystemObserver;
import simulation.SimulationEngine;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControlConsolePanel extends JPanel implements SystemObserver {

    private final MainController  mainController;
    private SimulationEngine      simulationEngine;

    // ── Capacity panel — color-coded progress bars ────────────────────────────
    private final List<JProgressBar> floorBars     = new ArrayList<>();
    private JLabel                   capTotalLabel;
    private JLabel                   capInTransitLabel;

    // ── Session table ─────────────────────────────────────────────────────────
    private DefaultTableModel     sessionTableModel;
    /** Session IDs suppressed from the display until they naturally close. */
    private final Set<String>     hiddenSessionIds  = new HashSet<>();

    // ── Transaction table ─────────────────────────────────────────────────────
    private DefaultTableModel     transactionTableModel;
    /** Transactions before this index are hidden after a Clear. */
    private int                   transactionOffset = 0;

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
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 0));
        panel.setBorder(BorderFactory.createTitledBorder("Emergency"));

        JButton activateBtn = new JButton("⚠  ACTIVATE");
        activateBtn.setBackground(new Color(180, 0, 0));
        activateBtn.setForeground(Color.WHITE);
        activateBtn.setFont(new Font("Arial", Font.BOLD, 13));
        activateBtn.setOpaque(true);
        activateBtn.setBorder(new LineBorder(new Color(255, 80, 80), 2));
        activateBtn.setPreferredSize(new Dimension(0, 40));
        activateBtn.addActionListener(e -> {
            int r = JOptionPane.showConfirmDialog(this,
                    "Activate emergency state?", "Confirm Emergency",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (r == JOptionPane.YES_OPTION) mainController.getAdminCommands().triggerEmergency();
        });

        JButton deactivateBtn = new JButton("✔  Deactivate");
        deactivateBtn.setFont(new Font("Arial", Font.BOLD, 12));
        deactivateBtn.setPreferredSize(new Dimension(0, 40));
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

    // ── Capacity ──────────────────────────────────────────────────────────────

    public JPanel buildCapacityPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBorder(BorderFactory.createTitledBorder("Capacity"));
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        outer.setMinimumSize(new Dimension(0, 160));

        // Dark display board background
        JPanel board = new JPanel();
        board.setLayout(new BoxLayout(board, BoxLayout.Y_AXIS));
        board.setBackground(new Color(16, 16, 16));
        board.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        // ── Per-floor rows ────────────────────────────────────────────────────
        for (Floor f : mainController.getParkingStructure().getFloors()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(new Color(16, 16, 16));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

            JLabel lbl = new JLabel("F" + f.getFloorNumber());
            lbl.setFont(new Font("Monospaced", Font.BOLD, 13));
            lbl.setForeground(new Color(190, 190, 190));
            lbl.setPreferredSize(new Dimension(24, 0));

            JProgressBar bar = new JProgressBar(0, f.getCapacity());
            bar.setValue(f.getCapacity());  // all available initially
            bar.setStringPainted(true);
            bar.setFont(new Font("Monospaced", Font.BOLD, 10));
            bar.setBackground(new Color(38, 38, 38));
            bar.setForeground(new Color(0, 210, 80));
            bar.setBorderPainted(false);
            floorBars.add(bar);

            row.add(lbl, BorderLayout.WEST);
            row.add(bar, BorderLayout.CENTER);
            board.add(row);
        }

        // ── Separator ─────────────────────────────────────────────────────────
        board.add(Box.createVerticalStrut(5));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(55, 55, 55));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        board.add(sep);
        board.add(Box.createVerticalStrut(4));

        // ── Totals ────────────────────────────────────────────────────────────
        capTotalLabel = new JLabel("Total: --");
        capTotalLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        capTotalLabel.setForeground(new Color(210, 210, 210));
        capTotalLabel.setAlignmentX(LEFT_ALIGNMENT);

        capInTransitLabel = new JLabel("In-Transit: 0 vehicles");
        capInTransitLabel.setFont(new Font("Monospaced", Font.BOLD, 11));
        capInTransitLabel.setForeground(new Color(80, 80, 80));
        capInTransitLabel.setAlignmentX(LEFT_ALIGNMENT);

        board.add(capTotalLabel);
        board.add(Box.createVerticalStrut(2));
        board.add(capInTransitLabel);

        refreshCapacity();
        outer.add(board, BorderLayout.CENTER);
        return outer;
    }

    /**
     * Updates all per-floor progress bars and summary labels from live data.
     * Bar colour: green (>50% free) → amber (25–50%) → red (<25%).
     * Must be called on the EDT.
     */
    private void refreshCapacity() {
        List<Floor> floors = mainController.getParkingStructure().getFloors();
        int totalCap = 0, totalOcc = 0;

        for (int i = 0; i < floors.size() && i < floorBars.size(); i++) {
            Floor f      = floors.get(i);
            int   cap    = f.getCapacity();
            int   occ    = f.getOccupiedCount();
            int   avail  = cap - occ;
            totalCap += cap;
            totalOcc += occ;

            JProgressBar bar = floorBars.get(i);
            bar.setMaximum(cap);
            bar.setValue(avail);
            bar.setString(avail + " / " + cap + " free");

            double ratio = cap > 0 ? (double) avail / cap : 0;
            if      (ratio > 0.50) bar.setForeground(new Color(0,   210,  80));
            else if (ratio > 0.25) bar.setForeground(new Color(255, 175,   0));
            else                   bar.setForeground(new Color(220,  50,  50));
        }

        if (capTotalLabel != null) {
            int avail = totalCap - totalOcc;
            capTotalLabel.setText(String.format("Total: %d/%d occ  (%d free)",
                    totalOcc, totalCap, avail));
            double ratio = totalCap > 0 ? (double) avail / totalCap : 0;
            if      (ratio > 0.50) capTotalLabel.setForeground(new Color(0,   210,  80));
            else if (ratio > 0.25) capTotalLabel.setForeground(new Color(255, 175,   0));
            else                   capTotalLabel.setForeground(new Color(220,  50,  50));
        }

        if (capInTransitLabel != null) {
            int it = mainController.getInTransitCount();
            capInTransitLabel.setText("In-Transit: " + it + " vehicle" + (it == 1 ? "" : "s"));
            capInTransitLabel.setForeground(it > 0
                    ? new Color(100, 180, 255)
                    : new Color(80, 80, 80));
        }
    }

    // ── Simulation ────────────────────────────────────────────────────────────

    public JPanel buildSimulationPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("Simulation"));

        // ── Manual one-shot buttons ───────────────────────────────────────────
        JButton manualEntryBtn = new JButton("Manual Entry");
        manualEntryBtn.addActionListener(e -> mainController.getAdminCommands().simulateEntry());

        JButton manualExitBtn = new JButton("Manual Exit");
        manualExitBtn.addActionListener(e -> {
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

        // ── Auto Entry toggle ─────────────────────────────────────────────────
        JButton autoEntryBtn = new JButton("▶  Auto Entry");
        autoEntryBtn.setBackground(new Color(20, 100, 170));
        autoEntryBtn.setForeground(Color.WHITE);
        autoEntryBtn.setFont(new Font("Arial", Font.BOLD, 12));
        autoEntryBtn.setOpaque(true);
        autoEntryBtn.setBorder(new LineBorder(new Color(80, 160, 255), 2));
        autoEntryBtn.setPreferredSize(new Dimension(0, 38));

        autoEntryBtn.addActionListener(e -> {
            if (simulationEngine == null)
                simulationEngine = new SimulationEngine(mainController);

            if (simulationEngine.isEntryRunning()) {
                simulationEngine.stopEntry();
                autoEntryBtn.setText("▶  Auto Entry");
                autoEntryBtn.setBackground(new Color(20, 100, 170));
                autoEntryBtn.setBorder(new LineBorder(new Color(80, 160, 255), 2));
            } else {
                simulationEngine.startEntry();
                autoEntryBtn.setText("⏹  Stop Entry");
                autoEntryBtn.setBackground(new Color(160, 20, 20));
                autoEntryBtn.setBorder(new LineBorder(new Color(255, 80, 80), 2));
            }
        });

        // ── Auto Exit toggle ──────────────────────────────────────────────────
        JButton autoExitBtn = new JButton("▶  Auto Exit");
        autoExitBtn.setBackground(new Color(20, 130, 40));
        autoExitBtn.setForeground(Color.WHITE);
        autoExitBtn.setFont(new Font("Arial", Font.BOLD, 12));
        autoExitBtn.setOpaque(true);
        autoExitBtn.setBorder(new LineBorder(new Color(60, 200, 80), 2));
        autoExitBtn.setPreferredSize(new Dimension(0, 38));

        autoExitBtn.addActionListener(e -> {
            if (simulationEngine == null)
                simulationEngine = new SimulationEngine(mainController);

            if (simulationEngine.isExitRunning()) {
                simulationEngine.stopExit();
                autoExitBtn.setText("▶  Auto Exit");
                autoExitBtn.setBackground(new Color(20, 130, 40));
                autoExitBtn.setBorder(new LineBorder(new Color(60, 200, 80), 2));
            } else {
                simulationEngine.startExit();
                autoExitBtn.setText("⏹  Stop Exit");
                autoExitBtn.setBackground(new Color(160, 20, 20));
                autoExitBtn.setBorder(new LineBorder(new Color(255, 80, 80), 2));
            }
        });

        panel.add(manualEntryBtn);
        panel.add(manualExitBtn);
        panel.add(autoEntryBtn);
        panel.add(autoExitBtn);
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

        JButton clearSessionsBtn = new JButton("Clear Display");
        clearSessionsBtn.addActionListener(e -> {
            List<Session> current = mainController.getDataStoreDriver()
                    .getSessionLogger().getActiveSessions();
            for (Session s : current) hiddenSessionIds.add(s.getSessionId());
            sessionTableModel.setRowCount(0);
        });

        JButton clearTransBtn = new JButton("Clear Display");
        clearTransBtn.addActionListener(e -> {
            transactionOffset = mainController.getDataStoreDriver()
                    .getTransactionArchive().getAllTransactions().size();
            transactionTableModel.setRowCount(0);
        });

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

            // ── Capacity bars (always live) ───────────────────────────────────
            refreshCapacity();

            // ── Active sessions (suppress hidden IDs) ─────────────────────────
            sessionTableModel.setRowCount(0);
            for (Session s : mainController.getDataStoreDriver()
                    .getSessionLogger().getActiveSessions()) {
                if (!hiddenSessionIds.contains(s.getSessionId())) {
                    sessionTableModel.addRow(new Object[]{
                            s.getSessionId(), s.getSpaceId(),
                            s.getFloor(), s.getEntryTime()});
                }
            }
            // Prune hidden set — once a session closes it leaves getActiveSessions()
            List<Session> stillActive = mainController.getDataStoreDriver()
                    .getSessionLogger().getActiveSessions();
            Set<String> activeIds = new HashSet<>();
            for (Session s : stillActive) activeIds.add(s.getSessionId());
            hiddenSessionIds.retainAll(activeIds);

            // ── Transactions (only post-offset entries) ───────────────────────
            transactionTableModel.setRowCount(0);
            var allTrans = mainController.getDataStoreDriver()
                    .getTransactionArchive().getAllTransactions();
            for (int i = transactionOffset; i < allTrans.size(); i++) {
                var t = allTrans.get(i);
                transactionTableModel.addRow(new Object[]{
                        t.getSessionId(), t.getExitTime(),
                        String.format("%.2f", t.getFee())});
            }
        });
    }
}