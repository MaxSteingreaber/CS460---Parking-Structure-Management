package gui;

import controller.MainController;
import model.Floor;
import observer.SystemEvent;
import observer.SystemObserver;

import javax.swing.*;
import java.awt.*;

/**
 * DriverDisplayPanel simulates the real-world LED signage drivers see
 * when entering a parking structure:
 *   - Entrance status    (OPEN / FULL / EMERGENCY)
 *   - Total availability across all floors
 *   - In-Transit vehicle count (vehicles between gate and stall)
 *   - Per-floor vacancy with colour-coded progress bars
 *   - System-recommended space (closest to entrance)
 *
 * Dark monospaced LED aesthetic. Refreshes on every SystemObserver event.
 */
public class DriverDisplayPanel extends JPanel implements SystemObserver {

    private static final Color BG           = new Color(10,  10,  10);
    private static final Color GREEN_BRIGHT = new Color(0,   255, 80);
    private static final Color GREEN_DIM    = new Color(0,   160, 50);
    private static final Color RED_BRIGHT   = new Color(255, 50,  50);
    private static final Color AMBER        = new Color(255, 180, 0);
    private static final Color BLUE_LIGHT   = new Color(100, 180, 255);
    private static final Color WHITE_DIM    = new Color(200, 200, 200);
    private static final Color HEADER_BG    = new Color(20,  20,  20);

    private final JLabel statusLabel;
    private final JLabel totalLabel;
    private final JLabel inTransitLabel;
    private final JLabel suggestedLabel;
    private final JPanel floorPanel;
    private final MainController mainController;

    public DriverDisplayPanel(MainController mainController) {
        this.mainController = mainController;
        setLayout(new BorderLayout());
        setBackground(BG);
        setPreferredSize(new Dimension(230, 0));
        setBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, new Color(40, 40, 40)));

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(HEADER_BG);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        addCentered(header, led("PARKING GUIDANCE", 11, Font.BOLD, WHITE_DIM));
        addCentered(header, led("DRIVER DISPLAY",    9, Font.PLAIN, new Color(100,100,100)));
        header.add(Box.createVerticalStrut(4));

        statusLabel = led("OPEN", 30, Font.BOLD, GREEN_BRIGHT);
        addCentered(header, statusLabel);

        totalLabel = led("-- / -- AVAILABLE", 12, Font.PLAIN, GREEN_DIM);
        addCentered(header, totalLabel);
        header.add(Box.createVerticalStrut(6));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(45, 45, 45));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        header.add(sep);
        header.add(Box.createVerticalStrut(6));

        addCentered(header, led("IN TRANSIT", 9, Font.PLAIN, WHITE_DIM));
        inTransitLabel = led("0 vehicles", 14, Font.BOLD, BLUE_LIGHT);
        addCentered(header, inTransitLabel);

        add(header, BorderLayout.NORTH);

        // ── Floor rows ────────────────────────────────────────────────────────
        floorPanel = new JPanel();
        floorPanel.setLayout(new BoxLayout(floorPanel, BoxLayout.Y_AXIS));
        floorPanel.setBackground(BG);
        floorPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel floorHdr = led("FLOOR AVAILABILITY", 9, Font.BOLD, WHITE_DIM);
        floorHdr.setAlignmentX(CENTER_ALIGNMENT);
        floorHdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        floorPanel.add(floorHdr);

        for (Floor f : mainController.getParkingStructure().getFloors()) {
            floorPanel.add(buildFloorRow(
                    f.getFloorNumber(),
                    f.getCapacity() - f.getOccupiedCount(),
                    f.getCapacity()));
        }
        add(floorPanel, BorderLayout.CENTER);

        // ── Footer: suggested space ───────────────────────────────────────────
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(HEADER_BG);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 8, 12, 8));

        addCentered(footer, led("SUGGESTED SPACE",       9, Font.PLAIN, WHITE_DIM));
        addCentered(footer, led("closest to entrance",   8, Font.PLAIN, new Color(100,100,100)));
        suggestedLabel = led("--", 22, Font.BOLD, AMBER);
        addCentered(footer, suggestedLabel);
        add(footer, BorderLayout.SOUTH);

        refresh();
    }

    // ── Floor row ─────────────────────────────────────────────────────────────

    private JPanel buildFloorRow(int floorNum, int available, int capacity) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));
        row.setName("row-" + floorNum);

        JLabel floorLbl = led("FLOOR " + floorNum, 11, Font.BOLD, WHITE_DIM);

        JLabel countLbl = led(available + " / " + capacity, 14, Font.BOLD, colorFor(available, capacity));
        countLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        countLbl.setName("count-" + floorNum);

        JProgressBar bar = new JProgressBar(0, capacity);
        bar.setValue(available);
        bar.setStringPainted(false);
        bar.setForeground(colorFor(available, capacity));
        bar.setBackground(new Color(30, 30, 30));
        bar.setPreferredSize(new Dimension(0, 5));
        bar.setName("bar-" + floorNum);

        JPanel right = new JPanel(new BorderLayout(0, 2));
        right.setBackground(BG);
        right.add(countLbl, BorderLayout.CENTER);
        right.add(bar,      BorderLayout.SOUTH);

        row.add(floorLbl, BorderLayout.WEST);
        row.add(right,    BorderLayout.EAST);
        return row;
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    private void refresh() {
        SwingUtilities.invokeLater(() -> {
            int total     = mainController.getParkingStructure().getTotalCapacity();
            int occupied  = mainController.getParkingStructure().getTotalOccupancy();
            int avail     = total - occupied;
            int inTransit = mainController.getInTransitCount();
            boolean emerg = mainController.isEmergencyActive();
            boolean full  = avail == 0 && inTransit == 0;

            if (emerg) {
                statusLabel.setText("EMERGENCY");
                statusLabel.setForeground(RED_BRIGHT);
            } else if (full) {
                statusLabel.setText("FULL");
                statusLabel.setForeground(RED_BRIGHT);
            } else {
                statusLabel.setText("OPEN");
                statusLabel.setForeground(GREEN_BRIGHT);
            }

            totalLabel.setText(avail + " / " + total + "  AVAILABLE");
            totalLabel.setForeground(colorFor(avail, total));

            inTransitLabel.setText(inTransit + " vehicle" + (inTransit == 1 ? "" : "s"));
            inTransitLabel.setForeground(inTransit > 0 ? BLUE_LIGHT : new Color(70, 70, 70));

            // Update floor rows
            for (Component c : floorPanel.getComponents()) {
                if (!(c instanceof JPanel)) continue;
                JPanel row = (JPanel) c;
                if (row.getName() == null || !row.getName().startsWith("row-")) continue;
                int fn = Integer.parseInt(row.getName().substring(4));
                Floor f = mainController.getParkingStructure().getFloor(fn);
                if (f == null) continue;
                int fAvail = f.getCapacity() - f.getOccupiedCount();
                int fCap   = f.getCapacity();
                Color col  = colorFor(fAvail, fCap);

                for (Component child : row.getComponents()) {
                    if (child instanceof JPanel) {
                        for (Component inner : ((JPanel) child).getComponents()) {
                            if (inner instanceof JLabel) {
                                ((JLabel) inner).setText(fAvail + " / " + fCap);
                                ((JLabel) inner).setForeground(col);
                            } else if (inner instanceof JProgressBar) {
                                JProgressBar bar = (JProgressBar) inner;
                                bar.setValue(fAvail);
                                bar.setForeground(col);
                            }
                        }
                    }
                }
            }

            // Suggested space
            var suggested = mainController.getSuggestedSpace();
            if (emerg) {
                suggestedLabel.setText("EXIT NOW");
                suggestedLabel.setForeground(RED_BRIGHT);
            } else if (full || suggested == null) {
                suggestedLabel.setText("FULL");
                suggestedLabel.setForeground(RED_BRIGHT);
            } else {
                suggestedLabel.setText(suggested.getSpaceId());
                suggestedLabel.setForeground(AMBER);
            }

            repaint();
        });
    }

    @Override
    public void onSystemEvent(SystemEvent event) { refresh(); }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private JLabel led(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Monospaced", style, size));
        l.setForeground(color);
        return l;
    }

    private void addCentered(JPanel panel, JLabel label) {
        label.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(label);
    }

    private Color colorFor(int available, int capacity) {
        if (capacity == 0 || available == 0)  return RED_BRIGHT;
        if (available <= capacity * 0.25)     return AMBER;
        return GREEN_BRIGHT;
    }
}