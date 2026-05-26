package controller.Security;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ReportsController {

    @FXML private ComboBox<String> cmbReportType;
    @FXML private TextField txtDateFrom;
    @FXML private TextField txtDateTo;
    @FXML private Label lblTotalCheckIn;
    @FXML private Label lblTotalCheckOut;
    @FXML private Label lblCurrentlyInside;
    @FXML private Label lblRegisteredDevices;
    @FXML private Button btnGenerate;
    @FXML private Button btnExport;

    @FXML public void initialize() {
        cmbReportType.getItems().addAll("Daily Summary","Weekly Summary","Monthly Summary","Custom Range");
        cmbReportType.getSelectionModel().selectFirst();
        txtDateFrom.setText("05/12/2025");
        txtDateTo.setText("05/12/2025");
    }

    @FXML private void handleGenerate() {
        if (txtDateFrom.getText().isEmpty() || txtDateTo.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please enter both dates."); return;
        }
        showAlert(Alert.AlertType.INFORMATION, "Report generated for " + cmbReportType.getValue());
    }

    @FXML private void handleExport() { showAlert(Alert.AlertType.INFORMATION, "Export coming soon."); }

    @FXML private void goToDashboard()      { navigateTo("SecurityDashboard.fxml"); }
    @FXML private void goToCheckInOut()     { navigateTo("CheckInOut.fxml"); }
    @FXML private void goToMonitoringLogs() { navigateTo("MonitoringLogs.fxml"); }
    @FXML private void goToActiveDevices()  { navigateTo("ActiveDevices.fxml"); }
    @FXML private void handleLogout()       { System.out.println("Logout"); }

    private void navigateTo(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/" + fxml));
            Stage stage = (Stage) btnGenerate.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1024, 768));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("BYOD"); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}