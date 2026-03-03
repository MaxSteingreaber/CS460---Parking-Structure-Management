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
import java.util.ArrayList;

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
        panel.add(legendSwatch("Available",  new Color(100, 200, 100)));
        panel.add(legendSwatch("Occupied",   new Color( 70, 130, 200)));
        panel.add(legendSwatch("Restricted", Color.LIGHT_GRAY));
        panel.add(legendSwatch("Reserved",   new Color(220,  80,  80)));
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

        final int SPACE_WIDTH = 40;      // Width of parking space
        final int SPACE_DEPTH = 60;      // Depth of parking space
        final int CORRIDOR_WIDTH = 80;   // Width of driving corridor
        final int GAP = 80;              // Gap between left and top (for ramp transition)

        ArrayList<ParkingSpace> spaces = f.getSpaces();
        int index = 0;
        int corridorLength = f.getPerimeterLength();
        int corridorWidth = f.getPerimeterWidth();

        // Calculate dimensions
        int outerSpaceDepth = SPACE_DEPTH;
        int innerSpaceDepth = SPACE_DEPTH;

        // Key Y positions
        int topStartY = GAP;
        int topCorridorY = topStartY + outerSpaceDepth;
        int topInnerY = topCorridorY + CORRIDOR_WIDTH;
        int sideCorridorStartY = topInnerY + innerSpaceDepth;
        int sideCorridorEndY = sideCorridorStartY + corridorWidth * SPACE_WIDTH;
        int bottomInnerY = sideCorridorEndY;
        int bottomCorridorY = bottomInnerY + innerSpaceDepth;
        int bottomOuterY = bottomCorridorY + CORRIDOR_WIDTH;

        // Key X positions
        int leftCorridorX = outerSpaceDepth;
        int leftInnerX = leftCorridorX + CORRIDOR_WIDTH;
        int topBottomSpacesStartX = leftInnerX + innerSpaceDepth;
        int topBottomSpacesEndX = topBottomSpacesStartX + corridorLength * SPACE_WIDTH;
        int rightInnerX = topBottomSpacesEndX;
        int rightCorridorX = rightInnerX + innerSpaceDepth;
        int rightOuterX = rightCorridorX + CORRIDOR_WIDTH;


        // ============ DRAW ALL DRIVING LANES (CONTINUOUS ROAD) ============
        g2.setColor(new Color(200, 200, 200));

        // Top corridor driving lane
        g2.fillRect(leftCorridorX, topCorridorY,
                rightCorridorX - leftCorridorX + CORRIDOR_WIDTH, CORRIDOR_WIDTH);

        // Right corridor driving lane
        g2.fillRect(rightCorridorX, topCorridorY,
                CORRIDOR_WIDTH, bottomCorridorY - topCorridorY + CORRIDOR_WIDTH);

        // Bottom corridor driving lane
        g2.fillRect(leftCorridorX, bottomCorridorY,
                rightCorridorX - leftCorridorX + CORRIDOR_WIDTH, CORRIDOR_WIDTH);

        // Left corridor driving lane (ramp) with gradient - draw over the gray
        if (f.getFloorNumber() != parkingStructure.getFloors().size()) {
            GradientPaint rampGradient = new GradientPaint(
                    0, sideCorridorEndY, new Color(200, 200, 200),
                    0, sideCorridorStartY, new Color(180, 180, 200));
            g2.setPaint(rampGradient);
            g2.fillRect(leftCorridorX, topCorridorY,
                    CORRIDOR_WIDTH, bottomCorridorY - topCorridorY + CORRIDOR_WIDTH);
        }
        else {
            g2.setColor(new Color(200, 200, 200));
            g2.fillRect(leftCorridorX, topCorridorY,
                    CORRIDOR_WIDTH, bottomCorridorY - topCorridorY + CORRIDOR_WIDTH);
        }

        // ============ DRAW PARKING SPACES ============

        // TOP CORRIDOR - Outer row
        for (int i = 0; i < corridorLength + 3; i++) {
            ParkingSpace space = spaces.get(index++);
            int x = topBottomSpacesStartX + i * SPACE_WIDTH;
            int y = topStartY;
            drawParkingSpace(g2, space, x, y, SPACE_WIDTH, outerSpaceDepth, true);
        }

        // TOP CORRIDOR - Inner row
        for (int i = 0; i < corridorLength; i++) {
            ParkingSpace space = spaces.get(index++);
            int x = topBottomSpacesStartX + i * SPACE_WIDTH;
            int y = topInnerY;
            drawParkingSpace(g2, space, x, y, SPACE_WIDTH, innerSpaceDepth, true);
        }

        // RIGHT CORRIDOR - Outer column
        for (int i = 0; i < corridorWidth + 6; i++) {
            ParkingSpace space = spaces.get(index++);
            int x = rightOuterX;
            int y = (sideCorridorStartY - 3 * SPACE_WIDTH)  + i * SPACE_WIDTH;
            drawParkingSpace(g2, space, x, y, outerSpaceDepth, SPACE_WIDTH, false);
        }

        // RIGHT CORRIDOR - Inner column
        for (int i = 0; i < corridorWidth; i++) {
            ParkingSpace space = spaces.get(index++);
            int x = rightInnerX;
            int y = sideCorridorStartY + i * SPACE_WIDTH;
            drawParkingSpace(g2, space, x, y, innerSpaceDepth, SPACE_WIDTH, false);
        }

        // BOTTOM CORRIDOR - Outer row
        for (int i = corridorLength - 1 + 6; i >= 0; i--) {
            ParkingSpace space = spaces.get(index++);
            int x = (topBottomSpacesStartX- 3 * SPACE_WIDTH) + i * SPACE_WIDTH;
            int y = bottomOuterY;
            drawParkingSpace(g2, space, x, y, SPACE_WIDTH, outerSpaceDepth, true);
        }

        // BOTTOM CORRIDOR - Inner row
        for (int i = corridorLength - 1; i >= 0; i--) {
            ParkingSpace space = spaces.get(index++);
            int x = topBottomSpacesStartX + i * SPACE_WIDTH;
            int y = bottomInnerY;
            drawParkingSpace(g2, space, x, y, SPACE_WIDTH, innerSpaceDepth, true);
        }

        // LEFT CORRIDOR (RAMP) - Outer column
        for (int i = corridorWidth - 1 + 3; i >= 0; i--) {
            ParkingSpace space = spaces.get(index++);
            int x = 0;
            int y = (sideCorridorStartY) + i * SPACE_WIDTH;
            drawParkingSpace(g2, space, x, y, outerSpaceDepth, SPACE_WIDTH, false);
        }

        // LEFT CORRIDOR (RAMP) - Inner column
        for (int i = corridorWidth - 1; i >= 0; i--) {
            ParkingSpace space = spaces.get(index++);
            int x = leftInnerX;
            int y = sideCorridorStartY + i * SPACE_WIDTH;
            drawParkingSpace(g2, space, x, y, innerSpaceDepth, SPACE_WIDTH, false);
        }

        // ============ DRAW RAMP INDICATORS ============
        if (f.getFloorNumber() != parkingStructure.getFloors().size()) {
            g2.setColor(Color.DARK_GRAY);
            int rampCenterX = leftCorridorX + CORRIDOR_WIDTH / 2;
            int rampCenterY = sideCorridorStartY + (corridorWidth * SPACE_WIDTH) / 2;

            // Draw arrows pointing up (direction of travel to next floor)
            g2.setStroke(new BasicStroke(2));
            for (int arrowY = rampCenterY + 60; arrowY > rampCenterY - 80; arrowY -= 50) {
                g2.drawLine(rampCenterX, arrowY, rampCenterX, arrowY - 30);
                g2.drawLine(rampCenterX, arrowY - 30, rampCenterX - 8, arrowY - 20);
                g2.drawLine(rampCenterX, arrowY - 30, rampCenterX + 8, arrowY - 20);
            }

            // Draw "UP" text
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("UP", rampCenterX - 10, rampCenterY - 100);
        }

        g2.dispose();
    }

    private void drawParkingSpace(Graphics2D g2, ParkingSpace space,
                                  int x, int y, int width, int height,
                                  boolean isHorizontal) {
        // Fill space with color based on state
        g2.setColor(getSpaceColor(space.getState()));
        g2.fillRect(x, y, width, height);

        // Draw hatching if restricted
        if (space.getState() == SpaceState.RESTRICTED) {
            drawHatching(g2, x, y, width, height);
        }

        // Draw border
        g2.setColor(Color.DARK_GRAY);
        g2.drawRect(x, y, width, height);

        // Draw space ID
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.PLAIN, 8));

        if (isHorizontal) {
            g2.drawString(space.getSpaceId(), x + 2, y + height - 4);
        } else {
            g2.drawString(space.getSpaceId(), x + 2, y + 12);
        }
    }

    private void drawHatching(Graphics2D g, int x, int y, int width, int height) {
        for (int i = y; i < y + height; i += 4) {
            g.setColor((i / 4) % 2 == 0 ? Color.LIGHT_GRAY : Color.GRAY);
            g.fillRect(x, i, width, 2);
        }
    }

    private Color getSpaceColor(SpaceState state) {
        switch (state) {
            case AVAILABLE:  return new Color(100, 200, 100);
            case OCCUPIED:   return new Color( 70, 130, 200);
            case RESTRICTED: return Color.LIGHT_GRAY;
            case RESERVED:   return new Color(220,  80,  80);
            default:         return Color.WHITE;
        }
    }

    public ParkingSpace getSpaceAtPoint(Point p) {
        Floor f = parkingStructure.getFloor(selectedFloor);
        if (f == null) return null;

        // Transform point based on zoom and pan
        int x = (int) ((p.x / zoomLevel - panOffset.x));
        int y = (int) ((p.y / zoomLevel - panOffset.y));

        final int SPACE_WIDTH = 40;
        final int SPACE_DEPTH = 60;
        final int CORRIDOR_WIDTH = 80;
        final int GAP = 80;

        int corridorLength = f.getPerimeterLength();
        int corridorWidth = f.getPerimeterWidth();
        int outerSpaceDepth = SPACE_DEPTH;
        int innerSpaceDepth = SPACE_DEPTH;

        ArrayList<ParkingSpace> spaces = f.getSpaces();
        int index = 0;

        // Check TOP CORRIDOR - Outer row
        for (int i = 0; i < corridorLength; i++) {
            int spaceX = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            int spaceY = 0;
            if (isPointInRect(x, y, spaceX, spaceY, SPACE_WIDTH, outerSpaceDepth)) {
                return spaces.get(index);
            }
            index++;
        }

        // Check TOP CORRIDOR - Inner row
        for (int i = 0; i < corridorLength; i++) {
            int spaceX = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            int spaceY = outerSpaceDepth + CORRIDOR_WIDTH;
            if (isPointInRect(x, y, spaceX, spaceY, SPACE_WIDTH, innerSpaceDepth)) {
                return spaces.get(index);
            }
            index++;
        }

        // Check RIGHT CORRIDOR - Outer column
        int rampGapX = outerSpaceDepth + corridorLength * SPACE_WIDTH + CORRIDOR_WIDTH + innerSpaceDepth;
        int rightOuterX = rampGapX + GAP;
        for (int i = 0; i < corridorWidth; i++) {
            int spaceX = rightOuterX;
            int spaceY = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            if (isPointInRect(x, y, spaceX, spaceY, outerSpaceDepth, SPACE_WIDTH)) {
                return spaces.get(index);
            }
            index++;
        }

        // Check RIGHT CORRIDOR - Inner column
        int rightCorridorX = rightOuterX - CORRIDOR_WIDTH - innerSpaceDepth;
        for (int i = 0; i < corridorWidth; i++) {
            int spaceX = rightCorridorX - innerSpaceDepth;
            int spaceY = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            if (isPointInRect(x, y, spaceX, spaceY, innerSpaceDepth, SPACE_WIDTH)) {
                return spaces.get(index);
            }
            index++;
        }

        // Check BOTTOM CORRIDOR - Outer row
        int bottomOuterY = outerSpaceDepth + CORRIDOR_WIDTH + corridorWidth * SPACE_WIDTH + CORRIDOR_WIDTH + innerSpaceDepth;
        for (int i = corridorLength - 1; i >= 0; i--) {
            int spaceX = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            int spaceY = bottomOuterY;
            if (isPointInRect(x, y, spaceX, spaceY, SPACE_WIDTH, outerSpaceDepth)) {
                return spaces.get(index);
            }
            index++;
        }

        // Check BOTTOM CORRIDOR - Inner row
        int bottomCorridorY = bottomOuterY - CORRIDOR_WIDTH - innerSpaceDepth;
        for (int i = corridorLength - 1; i >= 0; i--) {
            int spaceX = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            int spaceY = bottomCorridorY - innerSpaceDepth;
            if (isPointInRect(x, y, spaceX, spaceY, SPACE_WIDTH, innerSpaceDepth)) {
                return spaces.get(index);
            }
            index++;
        }

        // Check LEFT CORRIDOR - Outer column
        for (int i = corridorWidth - 1; i >= 0; i--) {
            int spaceX = 0;
            int spaceY = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            if (isPointInRect(x, y, spaceX, spaceY, outerSpaceDepth, SPACE_WIDTH)) {
                return spaces.get(index);
            }
            index++;
        }

        // Check LEFT CORRIDOR - Inner column
        for (int i = corridorWidth - 1; i >= 0; i--) {
            int spaceX = outerSpaceDepth + CORRIDOR_WIDTH;
            int spaceY = outerSpaceDepth + CORRIDOR_WIDTH + i * SPACE_WIDTH;
            if (isPointInRect(x, y, spaceX, spaceY, innerSpaceDepth, SPACE_WIDTH)) {
                return spaces.get(index);
            }
            index++;
        }

        return null;
    }

    private boolean isPointInRect(int px, int py, int rectX, int rectY, int rectWidth, int rectHeight) {
        return px >= rectX && px <= rectX + rectWidth &&
                py >= rectY && py <= rectY + rectHeight;
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
            String base = space.getSpaceId() + " — " + space.getState();
            if (space.getSessionId() != null) base += " | Session: " + space.getSessionId();
            return base;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            paintFloor((Graphics2D) g, selectedFloor);
        }
    }
}