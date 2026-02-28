package output;

import enums.LEDState;

public class LEDController {

    private LEDState currentState;

    public LEDController() {
        this.currentState = LEDState.NORMAL;
    }

    /** Sets LED indicators to steady green. */
    public void setNormal()    { currentState = LEDState.NORMAL; }

    /** Sets LED indicators to flashing red. */
    public void setEmergency() { currentState = LEDState.EMERGENCY; }

    /** Turns off LED indicators. */
    public void setOff()       { currentState = LEDState.OFF; }

    public LEDState getCurrentState() { return currentState; }
}
