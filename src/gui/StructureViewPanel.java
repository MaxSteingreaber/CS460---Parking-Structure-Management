package gui;

import enums.SpaceState;
import model.Floor;
import model.ParkingSpace;
import model.ParkingStructure;
import observer.SystemEvent;
import observer.SystemObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StructureViewPanel extends JPanel implements SystemObserver {

    private final ParkingStructure parkingStructure;
    private int    selectedFloor;
    private double zoomLevel;
    private Point  panOffset;

    private static final int CELL_SIZE = 40;
    private static final int CELL_GAP  = 3;

    private final DrawingCanvas canvas;

    public StructureViewPanel(ParkingStructure parkingStructure) {
        this.parkingStructure = parkingStructure;
        this.selectedFloor    = parkingStructure.getFloors().get(0).getFloorNumber();
        this.zoomLevel        = 1.0;
        this.panOffset        = new Point(10, 10);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Structure View"));

        add(buildFloorSelector(), BorderLayout.NORTH);

        canvas = new DrawingCanvas();
        add(new JScrollPane(canvas), BorderLayout.CENTER);

        add(buildLegend(), BorderLayout.SOUTH);
    }

    private JPanel buildFloorSelector() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Floor: "));
        for (Floor floor : parkingStructure.getFloors()) {
            JButton btn = new JButton("Floor " + floor.getFloorNumber());
            int fn = floor.getFloorNumber();
            btn.addActionListener(e -> {
                selectedFloor = fn;
                canvas.repaint();
            });
            panel.add(btn);
        }
        return panel;
    }

    private JPanel buildLegend() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        panel.add(legendSwatch("Available",  new Color(100, 200, 100)));   // green
        panel.add(legendSwatch("Occupied",   new Color(220,  50,  50)));   // red
        panel.add(legendSwatch("Reserved",   new Color( 70, 130, 200)));   // blue
        panel.add(legendSwatch("Restricted", Color.LIGHT_GRAY));
        return panel;
    }

    private JPanel legendSwatch(String label, Color color) {
        JPanel swatch = new JPanel();
        swatch.setPreferredSize(new Dimension(14, 14));
        swatch.setBackground(color);
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        wrapper.add(swatch);
        wrapper.add(new JLabel(label));
        return wrapper;
    }

    public void paintFloor(Graphics2D g, int floor) {
        Floor f = parkingStructure.getFloor(floor);
        if (f == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.scale(zoomLevel, zoomLevel);
        g2.translate(panOffset.x, panOffset.y);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ParkingSpace[][] spaces = f.getSpaces();
        for (int r = 0; r < f.getRows(); r++) {
            for (int c = 0; c < f.getColumns(); c++) {
                ParkingSpace space = spaces[r][c];
                int x = c * (CELL_SIZE + CELL_GAP);
                int y = r * (CELL_SIZE + CELL_GAP);

                g2.setColor(getSpaceColor(space.getState()));
                g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);

                if (space.getState() == SpaceState.RESTRICTED) {
                    drawHatching(g2, x, y, CELL_SIZE);
                }

                g2.setColor(Color.DARK_GRAY);
                g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Arial", Font.PLAIN, 8));
                g2.drawString(space.getSpaceId(), x + 2, y + CELL_SIZE - 4);
            }
        }
        g2.dispose();
    }

    private void drawHatching(Graphics2D g, int x, int y, int size) {
        for (int i = y; i < y + size; i += 4) {
            g.setColor((i / 4) % 2 == 0 ? Color.LIGHT_GRAY : Color.GRAY);
            g.fillRect(x, i, size, 2);
        }
    }

    private Color getSpaceColor(SpaceState state) {
        switch (state) {
            case AVAILABLE:  return new Color(100, 200, 100);   // green
            case IN_TRANSIT: return new Color(100, 200, 100);   // green — same as available,
            //  spot not yet physically taken
            case OCCUPIED:   return new Color(220,  50,  50);   // red
            case RESERVED:   return new Color( 70, 130, 200);   // blue
            case RESTRICTED: return Color.LIGHT_GRAY;
            default:         return Color.WHITE;
        }
    }

    public ParkingSpace getSpaceAtPoint(Point p) {
        Floor f = parkingStructure.getFloor(selectedFloor);
        if (f == null) return null;
        int col = (int) ((p.x / zoomLevel - panOffset.x) / (CELL_SIZE + CELL_GAP));
        int row = (int) ((p.y / zoomLevel - panOffset.y) / (CELL_SIZE + CELL_GAP));
        if (row >= 0 && row < f.getRows() && col >= 0 && col < f.getColumns()) {
            return f.getSpace(row, col);
        }
        return null;
    }

    @Override
    public void onSystemEvent(SystemEvent event) {
        SwingUtilities.invokeLater(canvas::repaint);
    }

    // ── Inner drawing canvas ──────────────────────────────────────────────────

    private class DrawingCanvas extends JPanel {

        DrawingCanvas() {
            setPreferredSize(new Dimension(800, 600));
            setBackground(Color.WHITE);
            setToolTipText("");

            addMouseWheelListener(e -> {
                zoomLevel = Math.max(0.5, Math.min(3.0, zoomLevel - e.getWheelRotation() * 0.1));
                repaint();
            });

            final Point[] dragStart = {null};
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)  { dragStart[0] = e.getPoint(); }
                @Override public void mouseReleased(MouseEvent e) { dragStart[0] = null; }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragStart[0] != null) {
                        panOffset.translate(
                                (int) ((e.getX() - dragStart[0].x) / zoomLevel),
                                (int) ((e.getY() - dragStart[0].y) / zoomLevel));
                        dragStart[0] = e.getPoint();
                        repaint();
                    }
                }
            });
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            ParkingSpace space = getSpaceAtPoint(e.getPoint());
            if (space == null) return null;
            String tip = space.getSpaceId() + " — " + space.getState();
            if (space.getSessionId() != null) tip += " | Session: " + space.getSessionId();
            return tip;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintFloor((Graphics2D) g, selectedFloor);
        }
    }
}