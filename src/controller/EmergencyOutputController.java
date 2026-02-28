package controller;

import output.LEDController;
import output.Siren;

public class EmergencyOutputController {

    private final Siren         siren;
    private final LEDController ledController;
    private boolean isEngaged;

    public EmergencyOutputController(Siren siren, LEDController ledController) {
        this.siren         = siren;
        this.ledController = ledController;
        this.isEngaged     = false;
    }

    /** Activates sirens and switches LEDs to emergency pattern. Returns false if already engaged. */
    public boolean engageEmergency() {
        if (isEngaged) return false;
        isEngaged = true;
        siren.activate();
        ledController.setEmergency();
        return true;
    }

    /** Deactivates sirens and restores LEDs to normal. Returns false if not currently engaged. */
    public boolean disengageEmergency() {
        if (!isEngaged) return false;
        isEngaged = false;
        siren.deactivate();
        ledController.setNormal();
        return true;
    }

    public boolean getStatus() { return isEngaged; }
}
