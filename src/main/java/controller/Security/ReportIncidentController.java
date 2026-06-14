package controller.Security;

import com.example.byod.LogEntry;
import com.example.byod.model.Device;
import utils.DataStore;
import utils.DatabaseHelper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ReportIncidentController {

    @FXML private TextField txtStudentNumber;
    @FXML private ComboBox<String> cmbTargetDevice;
    @FXML private ComboBox<String> cmbIncidentType;
    @FXML private DatePicker dpIncidentDate;
    @FXML private TextField txtIncidentTime;
    @FXML private TextField txtLastLocation;
    @FXML private TextArea txtIncidentDescription;

    @FXML
    public void initialize() {
        cmbIncidentType.getItems().addAll("Missing / Lost Device", "Stolen Device", "Found / Recovered Device", "Suspicious Activity", "Physical Hardware Damage");
        dpIncidentDate.setValue(LocalDate.now());
    }

    @FXML
    private void handleSearchStudent(ActionEvent event) {
        String searchInput = txtStudentNumber.getText().trim().toLowerCase();
        if (searchInput.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please enter a Student Number or Name.");
            return;
        }

        cmbTargetDevice.getItems().clear();
        List<Device> studentDevices = DataStore.getInstance().getDevicesList().stream()
                .filter(d -> (d.getStudentNumber() != null && d.getStudentNumber().toLowerCase().contains(searchInput)) ||
                        (d.getOwnerName() != null && d.getOwnerName().toLowerCase().contains(searchInput)))
                .collect(Collectors.toList());

        if (studentDevices.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No devices found for this identity.");
            cmbTargetDevice.setPromptText("No registered devices found.");
        } else {
            for (Device d : studentDevices) {
                String flag = "COMPROMISED".equals(d.getStatus()) ? " [ALREADY LOCKED]" : "";
                cmbTargetDevice.getItems().add(d.getBrandModel() + " (" + d.getMacAddress() + ")" + flag);
            }
            cmbTargetDevice.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleFileReport(ActionEvent event) {
        if (cmbTargetDevice.getValue() == null || cmbIncidentType.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Please select Incident Type and Target Device.");
            return;
        }

        String student = txtStudentNumber.getText();
        String deviceStr = cmbTargetDevice.getValue();
        String type = cmbIncidentType.getValue();
        String time = txtIncidentTime.getText() != null ? txtIncidentTime.getText() : "";
        String location = txtLastLocation.getText();
        String desc = txtIncidentDescription.getText();

        String dateForDb = dpIncidentDate.getValue() != null ? dpIncidentDate.getValue().toString() : "No Date";

        // THE FIX: Using a single-assignment ternary operator ensures this remains "effectively final"
        final String uiFormattedTimestamp = (dpIncidentDate.getValue() != null)
                ? dpIncidentDate.getValue().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) + " " + time
                : time;

        boolean isRecovery = type.equals("Found / Recovered Device");
        String newDbStatus = isRecovery ? "ACTIVE" : "COMPROMISED";

        String logIcon = isRecovery ? "✅ LIFTED" : "🚨 ALERT";
        String logTitle = isRecovery ? "SECURITY LIFTED" : "SECURITY HOLD";
        String logToken = isRecovery ? "RESTORED" : "LOCKED";
        String successMsg = isRecovery ? "Device recovered! Access has been restored to the student." : "Hardware Blacklisted! The perimeter gate will block this device.";

        String macAddress = "";
        try {
            macAddress = deviceStr.substring(deviceStr.lastIndexOf("(") + 1, deviceStr.lastIndexOf(")"));
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Failed to parse device MAC address.");
            return;
        }

        final String targetMac = macAddress;

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {

                String insertQuery = "INSERT INTO incident_reports (date, time, student_number, device_details, incident_type, location, description) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                    pstmt.setString(1, dateForDb);
                    pstmt.setString(2, time);
                    pstmt.setString(3, student);
                    pstmt.setString(4, deviceStr);
                    pstmt.setString(5, type);
                    pstmt.setString(6, location);
                    pstmt.setString(7, desc);
                    pstmt.executeUpdate();
                }

                String updateQuery = "UPDATE devices SET status = ? WHERE mac_address = ?";
                try (PreparedStatement pstmt2 = conn.prepareStatement(updateQuery)) {
                    pstmt2.setString(1, newDbStatus);
                    pstmt2.setString(2, targetMac);
                    pstmt2.executeUpdate();
                }

                Platform.runLater(() -> {
                    DataStore.getInstance().refreshDevices();
                    DataStore.getInstance().refreshIncidents();

                    // Injecting the properly formatted, final date + time string!
                    DataStore.getInstance().getMonitoringLogsList().add(0, new LogEntry(
                            logIcon, logTitle, student, deviceStr, logToken, type.toUpperCase(), uiFormattedTimestamp, location, newDbStatus
                    ));

                    showAlert(Alert.AlertType.INFORMATION, "Security Protocol Updated", successMsg);
                    handleBack(event);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database connection failed. Incident not saved.", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Security Protocol");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Security Protocol");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}