package controller.Security;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SecurityDashboardController {

    @FXML private Label lblDevicesCount;
    @FXML private Label lblCheckInCount;
    @FXML private Label lblCheckOutCount;
    @FXML private TextField txtSearch;
    @FXML private Button btnDashboard;
    @FXML private Button btnCheckInOut;
    @FXML private Button btnMonitoring;
    @FXML private Button btnActiveDevices;
    @FXML private Button btnReports;
    @FXML private Button btnLogout;
    @FXML private Button btnVerify;
    @FXML private Button btnQuickCheckIn;
    @FXML private Button btnQuickCheckOut;
    @FXML private Button btnReportIncident;

    @FXML public void initialize() { }

    @FXML private void handleVerify() {
        String query = txtSearch.getText().trim();
        if (query.isEmpty()) { showAlert("Please enter a Student ID, name, or device name."); return; }
        System.out.println("Verifying: " + query);
    }

    @FXML private void handleQuickCheckIn()   { navigateTo("CheckInOut.fxml"); }
    @FXML private void handleQuickCheckOut()  { navigateTo("CheckInOut.fxml"); }
    @FXML private void handleReportIncident() { System.out.println("Report Incident"); }
    @FXML private void goToCheckInOut()       { navigateTo("CheckInOut.fxml"); }
    @FXML private void goToMonitoringLogs()   { navigateTo("MonitoringLogs.fxml"); }
    @FXML private void goToActiveDevices()    { navigateTo("ActiveDevices.fxml"); }
    @FXML private void goToReports()          { navigateTo("Reports.fxml"); }
    @FXML private void handleLogout()         { System.out.println("Logout"); }

    private void navigateTo(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/" + fxml));
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1024, 768));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("BYOD"); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}