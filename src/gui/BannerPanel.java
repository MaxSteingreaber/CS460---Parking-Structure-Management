package gui;

import enums.SystemState;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BannerPanel extends JPanel {

    private final JLabel statusLabel;
    private final JLabel timeLabel;
    private Timer clockTimer;

    private static final Color COLOR_NORMAL    = new Color(34, 139, 34);
    private static final Color COLOR_EMERGENCY = Color.RED;
    private static final Color COLOR_DEGRADED  = new Color(200, 130, 0);

    public BannerPanel(String structureName) {
        setLayout(new BorderLayout(10, 0));
        setPreferredSize(new Dimension(0, 55));
        setBackground(COLOR_NORMAL);

        JLabel nameLabel = new JLabel(structureName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        nameLabel.setForeground(Color.WHITE);

        statusLabel = new JLabel("  NORMAL", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);

        timeLabel = new JLabel("", SwingConstants.RIGHT);
        timeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        timeLabel.setForeground(Color.WHITE);

        add(statusLabel, BorderLayout.WEST);
        add(nameLabel,   BorderLayout.CENTER);
        add(timeLabel,   BorderLayout.EAST);
    }

    /** Updates the status label text and the panel background color. */
    public void setStatus(String text, Color color) {
        statusLabel.setText("  " + text);
        setBackground(color);
        repaint();
    }

    /** Begins a Swing Timer that updates the time label every second. */
    public void startClock() {
        clockTimer = new Timer(1000, e -> {
            String time = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss"));
            timeLabel.setText(time + "  ");
        });
        clockTimer.setInitialDelay(0);
        clockTimer.start();
    }

    public void updateState(SystemState state) {
        switch (state) {
            case NORMAL:    setStatus("NORMAL",    COLOR_NORMAL);    break;
            case EMERGENCY: setStatus("EMERGENCY", COLOR_EMERGENCY); break;
            case DEGRADED:  setStatus("DEGRADED",  COLOR_DEGRADED);  break;
        }
    }
}
