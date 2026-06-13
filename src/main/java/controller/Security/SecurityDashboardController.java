package controller.Security;

import utils.DataStore;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

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
        try {
            // Locate the FXML file from the resources folder
            java.net.URL resource = getClass().getResource("/com/example/byod/Security/ReportIncident.fxml");

            // Safety check: Prevent the "Location is not set" crash
            if (resource == null) {
                showAlert(Alert.AlertType.ERROR, "System Error", "Could not find ReportIncident.fxml. Please rebuild the project using Maven Clean and Install.");
                return;
            }

            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // Create and show the popup window
            Stage incidentStage = new Stage();
            incidentStage.setTitle("Report Security Incident");
            incidentStage.setScene(new Scene(root));

            // Block interaction with the dashboard until this report is closed
            incidentStage.initModality(Modality.APPLICATION_MODAL);
            incidentStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error", "An error occurred while opening the Incident Report.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}