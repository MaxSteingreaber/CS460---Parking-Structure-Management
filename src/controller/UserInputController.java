package controller;

import input.AdminCommands;
import input.EntryKiosk;
import input.ExitKiosk;

public class UserInputController {

    private final EntryKiosk    entryKiosk;
    private final ExitKiosk     exitKiosk;
    private final AdminCommands adminCommands;

    public UserInputController(EntryKiosk entryKiosk,
                               ExitKiosk exitKiosk,
                               AdminCommands adminCommands) {
        this.entryKiosk    = entryKiosk;
        this.exitKiosk     = exitKiosk;
        this.adminCommands = adminCommands;
    }

    /** Returns true if a vehicle entry has been requested and the kiosk is active. */
    public boolean pollEntryRequest() {
        return entryKiosk.requestEntry();
    }

    /**
     * Returns a session ID if a vehicle exit has been requested, null otherwise.
     * In the demo this is driven by AdminCommands.simulateExit() rather than polling.
     */
    public String pollExitRequest() {
        return null;
    }

    public AdminCommands getAdminCommands() { return adminCommands; }
    public EntryKiosk    getEntryKiosk()    { return entryKiosk; }
    public ExitKiosk     getExitKiosk()     { return exitKiosk; }
}
