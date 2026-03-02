package input;

import controller.MainController;

/**
 * Bridges Swing action listeners on the dashboard to MainController business logic.
 * Serves as the single entry point for all administrator-initiated commands.
 */
public class AdminCommands {

    private final MainController mainController;

    public AdminCommands(MainController mainController) {
        this.mainController = mainController;
    }

    public void triggerEmergency() {
        mainController.activateEmergency();
    }

    public void cancelEmergency() {
        mainController.deactivateEmergency();
    }

    public void restrictSpace(String spaceId) {
        mainController.restrictSpace(spaceId);
    }

    public void unrestrictSpace(String spaceId) {
        mainController.unrestrictSpace(spaceId);
    }

    public void reserveSpace(String spaceId) { mainController.reserveSpace(spaceId); }

    public void unreserveSpace(String spaceId) { mainController.unreserveSpace(spaceId); }

    public void simulateEntry() {
        mainController.handleVehicleEntry();
    }

    public void simulateExit(String sessionId) {
        mainController.handleVehicleExit(sessionId);
    }

    /** Exports all transaction records to the specified CSV file path. */
    public void exportData(String filePath) {
        mainController.getDataStoreDriver().getTransactionArchive().exportToCsv(filePath);
    }
}
