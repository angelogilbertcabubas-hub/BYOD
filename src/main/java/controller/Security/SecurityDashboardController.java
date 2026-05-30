package controller.Security;

import utils.DataStore;
import javafx.application.Platform;
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
        lblDevicesCount.setText("...");
        lblCheckInCount.setText("...");
        lblCheckOutCount.setText("...");

        Thread dataLoadThread = new Thread(() -> {
            DataStore store = DataStore.getInstance();

            int activeDevices = store.getActiveDevicesList().size();

            long checkIns = store.getMonitoringLogsList().stream()
                    .filter(log -> "Check-In".equals(log.getOperation()))
                    .count();

            long checkOuts = store.getMonitoringLogsList().stream()
                    .filter(log -> "Check-Out".equals(log.getOperation()))
                    .count();

            Platform.runLater(() -> {
                lblDevicesCount.setText(String.valueOf(activeDevices));
                lblCheckInCount.setText(String.valueOf(checkIns));
                lblCheckOutCount.setText(String.valueOf(checkOuts));
            });
        });

        dataLoadThread.setDaemon(true);
        dataLoadThread.start();
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Verification Error", "Please enter an Access Token, Student ID, or Name.");
            return;
        }

        showAlert(Alert.AlertType.INFORMATION, "Verification Status", "Searching registry for: " + query + "\n\nRedirecting to Gate Check-In...");
        goToCheckInOut();
    }

    @FXML
    private void handleQuickCheckIn(ActionEvent event) {
        goToCheckInOut();
    }

    @FXML
    private void handleQuickCheckOut(ActionEvent event) {
        goToCheckInOut();
    }

    @FXML
    private void handleReportIncident(ActionEvent event) {
        showAlert(Alert.AlertType.INFORMATION, "Incident Report", "Opening Security Incident Logging Protocol...");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}