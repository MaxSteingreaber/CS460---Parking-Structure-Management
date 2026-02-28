package input;

public class EmergencySignal {

    private boolean isActive;

    public EmergencySignal() {
        this.isActive = false;
    }

    public void activate()   { isActive = true; }
    public void deactivate() { isActive = false; }
    public boolean getState(){ return isActive; }
}
