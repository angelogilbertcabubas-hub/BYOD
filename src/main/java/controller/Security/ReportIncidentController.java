package controller.Security;

import com.example.byod.model.Device;
import com.example.byod.model.IncidentReport;
import utils.DataStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
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
        if (cmbIncidentType != null) {
            cmbIncidentType.getItems().addAll(
                    "Missing / Lost Device",
                    "Stolen Device",
                    "Found / Recovered Device",
                    "Suspicious Activity",
                    "Physical Hardware Damage"
            );
        }
        if (dpIncidentDate != null) {
            dpIncidentDate.setValue(LocalDate.now());
        }
    }

    @FXML
    private void handleSearchStudent(ActionEvent event) {
        String searchInput = txtStudentNumber.getText().trim().toLowerCase();
        if (searchInput.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Search Failed", "Please enter a Student Number or Name to search.");
            return;
        }

        cmbTargetDevice.getItems().clear();

        // SMARTER SEARCH: Checks BOTH Student Number AND Owner's Name
        List<Device> studentDevices = DataStore.getInstance().getDevicesList().stream()
                .filter(d ->
                        (d.getStudentNumber() != null && d.getStudentNumber().toLowerCase().contains(searchInput)) ||
                                (d.getOwnerName() != null && d.getOwnerName().toLowerCase().contains(searchInput))
                )
                .collect(Collectors.toList());

        if (studentDevices.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Devices Found", "Could not find any devices. If the Student ID doesn't work, try typing the Student's Name instead!");
            cmbTargetDevice.setPromptText("No registered devices found.");
        } else {
            for (Device d : studentDevices) {
                cmbTargetDevice.getItems().add(d.getBrandModel() + " (" + d.getMacAddress() + ")");
            }
            cmbTargetDevice.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleFileReport(ActionEvent event) {
        if (cmbTargetDevice.getValue() == null || cmbIncidentType.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Incomplete Form", "Please ensure an Incident Type and Target Device are selected.");
            return;
        }

        String student = txtStudentNumber.getText();
        String device = cmbTargetDevice.getValue();
        String type = cmbIncidentType.getValue();
        String date = dpIncidentDate.getValue() != null ? dpIncidentDate.getValue().toString() : "No Date";
        String time = txtIncidentTime.getText();
        String location = txtLastLocation.getText();
        String desc = txtIncidentDescription.getText();

        // Push data to reports page table
        DataStore.getInstance().getIncidentReportsList().add(
                new IncidentReport(date, time, student, device, type, location, desc)
        );

        showAlert(Alert.AlertType.INFORMATION, "System Update",
                "The " + type + " report for " + device + " has been successfully routed to the Security Reports module.");

        handleBack(event);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Incident System");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}