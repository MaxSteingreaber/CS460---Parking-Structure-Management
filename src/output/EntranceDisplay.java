package output;

public class EntranceDisplay {

    private String currentMessage;

    public EntranceDisplay() {
        this.currentMessage = "OPEN";
    }

    public void setMessage(String msg) { this.currentMessage = msg; }
    public String getMessage()         { return currentMessage; }
}
