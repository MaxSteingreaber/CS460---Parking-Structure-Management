package gui;

import observer.SystemEvent;
import observer.SystemObserver;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class EventLogPanel extends JPanel implements SystemObserver {

    private final JTextArea logArea;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public EventLogPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Event Log"));
        setPreferredSize(new Dimension(0, 150));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clear());
        add(clearButton, BorderLayout.EAST);
    }

    /** Adds a new timestamped entry to the log and scrolls to the bottom. */
    public void appendEntry(String timestamp, String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(String.format("[%s] %s%n", timestamp, message));
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    @Override
    public void onSystemEvent(SystemEvent event) {
        appendEntry(event.getTimestamp().format(FMT), event.getMessage());
    }

    public void clear() {
        logArea.setText("");
    }
}
