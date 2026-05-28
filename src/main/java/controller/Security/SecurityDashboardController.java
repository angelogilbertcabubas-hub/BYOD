package controller.Security;

import utils.DataStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SecurityDashboardController extends BaseSecurityController {

    @FXML private Label lblDevicesCount;
    @FXML private Label lblCheckInCount;
    @FXML private Label lblCheckOutCount;

    @FXML private TextField txtSearch;
    @FXML private Button btnVerify;
    @FXML private Button btnQuickCheckIn;
    @FXML private Button btnQuickCheckOut;
    @FXML private Button btnReportIncident;

    @FXML
    public void initialize() {
        // Automatically populate the dashboard counters with live data from DataStore
        int activeDevices = DataStore.getInstance().getActiveDevicesList().size();
        lblDevicesCount.setText(String.valueOf(activeDevices));

        long checkIns = DataStore.getInstance().getMonitoringLogsList().stream()
                .filter(log -> "Check-In".equals(log.getOperation()))
                .count();
        lblCheckInCount.setText(String.valueOf(checkIns));

        long checkOuts = DataStore.getInstance().getMonitoringLogsList().stream()
                .filter(log -> "Check-Out".equals(log.getOperation()))
                .count();
        lblCheckOutCount.setText(String.valueOf(checkOuts));
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Verification Error", "Please enter an Access Token, Student ID, or Name.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Verification Status", "Searching registry for: " + query + "\n\nRedirecting to Gate Check-In...");

        // Automatically routes the guard to the Check-In terminal after verifying
        goToCheckInOut(event);
    }

    // --- QUICK ACTION DASHBOARD BUTTONS ---
    @FXML
    private void handleQuickCheckIn(ActionEvent event) {
        goToCheckInOut(event); // Inherited seamlessly from BaseSecurityController
    }

    @FXML
    private void handleQuickCheckOut(ActionEvent event) {
        goToCheckInOut(event); // Inherited seamlessly from BaseSecurityController
    }

    @FXML
    private void handleReportIncident(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Incident Report", "Opening Security Incident Logging Protocol...");
    }

    // Alert helper for popups
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}