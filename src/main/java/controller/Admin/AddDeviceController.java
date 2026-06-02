package controller.Admin;

import com.example.byod.model.Device;
import com.example.byod.model.Student;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.DataStore;
import utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;
import java.util.regex.Pattern;

public class AddDeviceController {

    @FXML private ComboBox<String> cmbOwnerName;
    @FXML private ComboBox<String> cmbDeviceType;
    @FXML private TextField txtModel;
    @FXML private TextField txtMacAddress;

    private Device newDevice = null;
    private ObservableList<String> studentRecords;
    private final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    @FXML
    public void initialize() {
        cmbDeviceType.getItems().addAll("Smartphone", "Laptop", "Tablet", "Speaker", "Projector", "Smart Watch", "Others");

        studentRecords = FXCollections.observableArrayList();
        for (Student s : DataStore.getInstance().getStudentsList()) {
            studentRecords.add(s.getFullName() + " [" + s.getStudentId() + "]");
        }

        FilteredList<String> filteredStudents = new FilteredList<>(studentRecords, p -> true);
        cmbOwnerName.setItems(filteredStudents);

        cmbOwnerName.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            final TextField editor = cmbOwnerName.getEditor();
            final String selected = cmbOwnerName.getSelectionModel().getSelectedItem();

            if (selected == null || !selected.equals(editor.getText())) {
                filteredStudents.setPredicate(item -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    return item.toLowerCase().contains(newValue.toLowerCase());
                });
                if (!cmbOwnerName.isShowing()) {
                    cmbOwnerName.show();
                }
            }
        });
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (!isInputValid()) {
            return;
        }

        String rawSelection = cmbOwnerName.getEditor().getText();
        String studentNumber = rawSelection.substring(rawSelection.indexOf("[") + 1, rawSelection.indexOf("]"));
        String ownerName = rawSelection.substring(0, rawSelection.indexOf("[")).trim();

        String[] modelSplit = txtModel.getText().split(" ", 2);
        String brand = modelSplit[0];
        String modelStr = modelSplit.length > 1 ? modelSplit[1] : "Unknown";

        int randomTokenNum = 1000 + new Random().nextInt(9000);
        String generatedToken = "TKN-" + randomTokenNum;

        String getStudentIdQuery = "SELECT student_id FROM students WHERE student_number = ?";
        String insertQuery = "INSERT INTO devices (student_id, device_type, brand, model, serial_number, access_code) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(getStudentIdQuery)) {

            selectStmt.setString(1, studentNumber);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                int dbStudentId = rs.getInt("student_id");

                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    insertStmt.setInt(1, dbStudentId);
                    insertStmt.setString(2, cmbDeviceType.getValue());
                    insertStmt.setString(3, brand);
                    insertStmt.setString(4, modelStr);
                    insertStmt.setString(5, txtMacAddress.getText().trim());
                    insertStmt.setString(6, generatedToken);
                    insertStmt.executeUpdate();
                }

                newDevice = new Device(
                        ownerName,
                        cmbDeviceType.getValue(),
                        txtModel.getText(),
                        txtMacAddress.getText(),
                        generatedToken
                );

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText("Device successfully registered for " + ownerName);
                alert.showAndWait();

                closeStage(event);
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration Error", "Could not locate student ID in database.");
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save device: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        newDevice = null;
        closeStage(event);
    }

    private boolean isInputValid() {
        StringBuilder errorBuilder = new StringBuilder();

        String owner = cmbOwnerName.getEditor().getText();
        if (owner == null || owner.trim().isEmpty() || !owner.contains("[") || !owner.contains("]")) {
            errorBuilder.append("- Please search and select a valid registered student from the dropdown.\n");
        }

        if (cmbDeviceType.getValue() == null) {
            errorBuilder.append("- Device Type selection is required.\n");
        }

        if (txtModel.getText() == null || txtModel.getText().trim().isEmpty()) {
            errorBuilder.append("- Brand and Model description is required.\n");
        }

        String mac = txtMacAddress.getText();
        if (mac == null || mac.trim().isEmpty() || !MAC_PATTERN.matcher(mac).matches()) {
            errorBuilder.append("- Valid MAC Address is required (e.g., 00:1B:44:11:3A:B7).\n");
        }

        if (errorBuilder.length() == 0) {
            return true;
        } else {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Please correct the following:\n\n" + errorBuilder.toString());
            return false;
        }
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Account Provisioning System");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Device getNewDevice() {
        return newDevice;
    }
}