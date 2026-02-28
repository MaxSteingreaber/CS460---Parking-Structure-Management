package output;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Siren {

    private boolean isActive;
    private int intervalMs;
    private ScheduledExecutorService scheduler;

    public Siren(int intervalMs) {
        this.isActive   = false;
        this.intervalMs = intervalMs;
    }

    /** Begins sounding the siren at the configured interval. */
    public void activate() {
        if (isActive) return;
        isActive  = true;
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
                () -> java.awt.Toolkit.getDefaultToolkit().beep(),
                0, intervalMs, TimeUnit.MILLISECONDS);
    }

    /** Silences the siren. */
    public void deactivate() {
        isActive = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public boolean isActive()   { return isActive; }
    public int getIntervalMs()  { return intervalMs; }
    public void setIntervalMs(int intervalMs) { this.intervalMs = intervalMs; }
}
