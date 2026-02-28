package observer;

import java.time.LocalDateTime;

public class SystemEvent {

    private final EventType eventType;
    private final String targetId;
    private final LocalDateTime timestamp;
    private final String message;

    public SystemEvent(EventType eventType, String targetId, String message) {
        this.eventType = eventType;
        this.targetId = targetId;
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }

    public EventType getEventType() { return eventType; }
    public String getTargetId()     { return targetId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage()      { return message; }

    public String format() {
        return String.format("[%s] %s: %s", timestamp, eventType, message);
    }
}
