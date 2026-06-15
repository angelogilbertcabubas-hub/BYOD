package controller.Admin;

import com.example.byod.model.Device;
import javafx.application.Platform;
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
import java.util.Optional;

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

        // LOGICAL FIX: Lock down physical hardware identity so it cannot be altered
        cmbEditType.setDisable(true);
        txtEditModel.setEditable(false);
        txtEditMac.setEditable(false);

        // Optional: Keep text visually readable instead of completely grayed out
        cmbEditType.setStyle("-fx-opacity: 1; -fx-background-color: #F0F0F0;");
        txtEditModel.setStyle("-fx-background-color: #F0F0F0;");
        txtEditMac.setStyle("-fx-background-color: #F0F0F0;");

        fetchExtendedDetails();
    }

    private void fetchExtendedDetails() {
        String query = "SELECT d.photo_path, s.first_name, s.last_name, s.school_id, s.program_course " +
                "FROM devices d JOIN students s ON d.student_id = s.id " +
                "WHERE d.unique_code = ?";

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, focusedDevice.getAccessCode());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    currentCloudPhotoPath = rs.getString("photo_path");

                    String ownerSummary = rs.getString("first_name") + " " + rs.getString("last_name") +
                            " | ID: " + rs.getString("school_id") +
                            " | Course: " + rs.getString("program_course");

                    Platform.runLater(() -> {
                        lblStudentOwnerSummary.setText("Owner: " + ownerSummary);
                        loadCloudPhoto(currentCloudPhotoPath);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // FIX: Robust photo loading logic with local fallback
    private void loadCloudPhoto(String path) {
        String defaultImage = "/images/icon-devices.png";

        if (path == null || path.trim().isEmpty() || path.contains("default_")) {
            setFallbackImage(defaultImage);
            return;
        }

        try {
            if (path.startsWith("http")) {
                Image webImage = new Image(path, true); // true = load asynchronously
                webImage.errorProperty().addListener((obs, oldVal, isError) -> {
                    if (isError) setFallbackImage(defaultImage);
                });
                devicePhotoImageView.setImage(webImage);
            } else {
                File imgFile = new File("src/main/resources/" + path);
                if (imgFile.exists()) {
                    devicePhotoImageView.setImage(new Image(imgFile.toURI().toString()));
                } else {
                    setFallbackImage(defaultImage);
                }
            }
        } catch (Exception ignored) {
            setFallbackImage(defaultImage);
        }
    }

    private void setFallbackImage(String resourcePath) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url != null) {
                devicePhotoImageView.setImage(new Image(url.toExternalForm()));
            } else {
                devicePhotoImageView.setImage(null);
            }
        } catch (Exception e) {
            devicePhotoImageView.setImage(null);
        }
    }

    @FXML
    private void handleRemoveDevice(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Security Override");
        confirm.setHeaderText("Revoke Hardware Registration?");
        confirm.setContentText("Are you sure you want to permanently remove this device?\n\nThis will block it from passing the perimeter gates.");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                String updateQuery = "UPDATE devices SET status = 'ARCHIVED' WHERE mac_address = ?";
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

                    pstmt.setString(1, txtEditMac.getText());
                    pstmt.executeUpdate();

                    Platform.runLater(() -> {
                        DataStore.getInstance().refreshDevices();
                        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                        stage.close();

                        Alert success = new Alert(Alert.AlertType.INFORMATION, "Device successfully unregistered.");
                        success.show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> {
                        Alert err = new Alert(Alert.AlertType.ERROR, "Database connection failed.");
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
            new Thread(() -> {
                try {
                    currentCloudPhotoPath = SupabaseStorageHelper.uploadImage(file, "DEV_" + System.currentTimeMillis());
                    Platform.runLater(() -> {
                        loadCloudPhoto(currentCloudPhotoPath);
                        showAlert(Alert.AlertType.INFORMATION, "Photo Updated", "The new cloud photo has been staged. Click Save to finalize.");
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Upload Error", "Could not beam photo to cloud."));
                }
            }).start();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSaveChanges(ActionEvent event) {
        // BUG 7 FIX: Add confirmation dialog before saving device edits
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Update");
        confirm.setHeaderText("Save Device Photo?");
        confirm.setContentText("Are you sure you want to update the visual profile for this device?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            // LOGICAL FIX: ONLY update the photo path, protecting hardware identity
            String query = "UPDATE devices SET photo_path = ? WHERE unique_code = ?";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, currentCloudPhotoPath);
                ps.setString(2, focusedDevice.getAccessCode());

                ps.executeUpdate();

                DataStore.getInstance().refreshDevices();
                showAlert(Alert.AlertType.INFORMATION, "Update Success", "Device photo successfully updated.");

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                stage.close();

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update device: " + e.getMessage());
            }
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