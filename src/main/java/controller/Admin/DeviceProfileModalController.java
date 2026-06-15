package controller.Admin;

import com.example.byod.model.Device;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.DataStore;
import utils.DatabaseHelper;
import utils.SupabaseStorageHelper;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DeviceProfileModalController {

    @FXML private Label lblDeviceHeader;
    @FXML private Label lblStudentOwnerSummary;

    @FXML private ComboBox<String> cmbEditType;
    @FXML private TextField txtEditModel;
    @FXML private TextField txtEditMac;

    @FXML private ImageView devicePhotoImageView;
    private String currentCloudPhotoPath = null;
    private Device focusedDevice;

    @FXML
    public void initialize() {
        cmbEditType.getItems().addAll("Smartphone", "Laptop", "Tablet", "Speaker", "Projector", "Smart Watch", "Others");
    }

    public void initData(Device device) {
        this.focusedDevice = device;

        lblDeviceHeader.setText(device.getBrandModel() + " (" + device.getAccessCode() + ")");
        cmbEditType.setValue(device.getDeviceType());

        String[] split = device.getBrandModel().split(" ", 2);
        txtEditModel.setText(split.length > 1 ? split[1] : device.getBrandModel());
        txtEditMac.setText(device.getMacAddress());

        fetchExtendedDetails();
    }

    private void fetchExtendedDetails() {
        String query = "SELECT d.photo_path, s.first_name, s.last_name, s.school_id, s.program_course " +
                "FROM devices d JOIN students s ON d.student_id = s.id " +
                "WHERE d.unique_code = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, focusedDevice.getAccessCode());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                currentCloudPhotoPath = rs.getString("photo_path");

                String ownerSummary = rs.getString("first_name") + " " + rs.getString("last_name") +
                        " | ID: " + rs.getString("school_id") +
                        " | Course: " + rs.getString("program_course");
                lblStudentOwnerSummary.setText("Owner: " + ownerSummary);

                loadCloudPhoto(currentCloudPhotoPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCloudPhoto(String url) {
        if (url != null && url.startsWith("http")) {
            devicePhotoImageView.setImage(new Image(url, true));
        }
    }
    @FXML
    private void handleRemoveDevice(javafx.event.ActionEvent event) {
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Security Override");
        confirm.setHeaderText("Revoke Hardware Registration?");
        confirm.setContentText("Are you sure you want to permanently remove this device?\n\nThis will block it from passing the perimeter gates.");

        java.util.Optional<javafx.scene.control.ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            new Thread(() -> {
                // Perform Soft Delete to preserve historical logs
                String updateQuery = "UPDATE devices SET status = 'ARCHIVED' WHERE mac_address = ?";
                try (java.sql.Connection conn = utils.DatabaseHelper.getConnection();
                     java.sql.PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

                    // IMPORTANT: We use the MAC address from the text field to target the specific device
                    pstmt.setString(1, txtEditMac.getText());
                    pstmt.executeUpdate();

                    javafx.application.Platform.runLater(() -> {
                        // 1. Refresh the main dashboard table so it disappears instantly
                        utils.DataStore.getInstance().refreshDevices();

                        // 2. Close the modal
                        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
                        stage.close();

                        // 3. Show Success Message
                        javafx.scene.control.Alert success = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION, "Device successfully unregistered.");
                        success.show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert err = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Database connection failed.");
                        err.show();
                    });
                }
            }).start();
        }
    }
    @FXML
    private void handleUpdatePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select New Device Photo");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                // Instantly upload the new photo to the cloud
                currentCloudPhotoPath = SupabaseStorageHelper.uploadImage(file, "DEV");
                loadCloudPhoto(currentCloudPhotoPath);

                showAlert(Alert.AlertType.INFORMATION, "Photo Updated", "The new cloud photo has been staged. Click Save to finalize.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Upload Error", "Could not beam photo to cloud.");
            }
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        // This retrieves the current window (Stage) and closes it
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSaveChanges(ActionEvent event) {
        String newType = cmbEditType.getValue();
        String newModel = txtEditModel.getText().trim();
        String newMac = txtEditMac.getText().trim();

        if (newType == null || newModel.isEmpty() || newMac.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Data", "Fields cannot be empty.");
            return;
        }

        String query = "UPDATE devices SET device_type = ?, device_name = ?, mac_address = ?, photo_path = ? WHERE unique_code = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, newType);
            ps.setString(2, newModel); // Assuming brand is kept, or you can merge brand/model logic here
            ps.setString(3, newMac);
            ps.setString(4, currentCloudPhotoPath);
            ps.setString(5, focusedDevice.getAccessCode());

            ps.executeUpdate();

            DataStore.getInstance().refreshDevices();
            showAlert(Alert.AlertType.INFORMATION, "Update Success", "Device parameters saved.");

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update device: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("System Dialog");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}