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
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

public class AddDeviceController {

    @FXML private ComboBox<String> cmbOwnerName;
    @FXML private ComboBox<String> cmbDeviceType;
    @FXML private TextField txtModel;
    @FXML private TextField txtMacAddress;

    @FXML private Button btnUploadDevicePhoto;
    @FXML private Label lblDevicePhotoName;
    @FXML private ImageView devicePhotoPreview;

    private String devicePhotoPath = "default_device.png";
    private boolean isPhotoUpdated = false;

    private Device newDevice = null;
    private Device editingDevice = null;

    private ObservableList<String> studentRecords;
    private final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    @FXML
    public void initialize() {
        cmbDeviceType.getItems().addAll("Smartphone", "Laptop", "Tablet", "Speaker", "Projector", "Smart Watch", "Others");

        if (lblDevicePhotoName != null) {
            lblDevicePhotoName.setText("No file selected");
        }

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
                    if (newValue == null || newValue.isEmpty()) return true;
                    return item.toLowerCase().contains(newValue.toLowerCase());
                });
                if (!cmbOwnerName.isShowing()) cmbOwnerName.show();
            }
        });
    }

    // Edit Mode Handoff
    public void setDeviceForEdit(Device device) {
        this.editingDevice = device;

        Platform.runLater(() -> {
            try {
                // Populate Fields
                if (device.getOwnerName() != null) {
                    for (String studentStr : studentRecords) {
                        if (studentStr.toLowerCase().contains(device.getOwnerName().toLowerCase())) {
                            cmbOwnerName.getSelectionModel().select(studentStr);
                            cmbOwnerName.getEditor().setText(studentStr);
                            break;
                        }
                    }
                }

                if (device.getDeviceType() != null) cmbDeviceType.setValue(device.getDeviceType());
                if (device.getModel() != null) txtModel.setText(device.getModel());
                if (device.getMacAddress() != null) txtMacAddress.setText(device.getMacAddress());

                // LOGICAL FIX: Lock down physical hardware details so they cannot be altered
                cmbOwnerName.setDisable(true);
                cmbDeviceType.setDisable(true);
                txtModel.setDisable(true);
                txtMacAddress.setDisable(true);

                if (lblDevicePhotoName != null) {
                    lblDevicePhotoName.setText("Loading Current Photo...");
                }

                // Fetch and load the existing device photo from the database
                fetchAndLoadExistingPhoto(device.getToken());

            } catch (Exception e) {
                System.err.println("Failed to inject device data into form: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void fetchAndLoadExistingPhoto(String deviceToken) {
        new Thread(() -> {
            String photoPath = "default_device.png";
            String query = "SELECT photo_path FROM devices WHERE unique_code = ?";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, deviceToken);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String fetchedPath = rs.getString("photo_path");
                    if (fetchedPath != null && !fetchedPath.isEmpty()) {
                        photoPath = fetchedPath;
                        devicePhotoPath = photoPath; // Retain existing path if no new upload occurs
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            final String finalPath = photoPath;
            Platform.runLater(() -> {
                loadImageToView(devicePhotoPreview, finalPath);
                if (lblDevicePhotoName != null) lblDevicePhotoName.setText("Current Device Photo");
            });
        }).start();
    }

    private void loadImageToView(ImageView imageView, String path) {
        if (imageView == null) return;
        String defaultImage = "/images/icon-devices.png";

        if (path == null || path.trim().isEmpty() || path.contains("default_")) {
            setFallbackImage(imageView, defaultImage);
            return;
        }

        try {
            if (path.startsWith("http")) {
                Image webImage = new Image(path, true); // Load async
                webImage.errorProperty().addListener((obs, oldVal, isError) -> {
                    if (isError) setFallbackImage(imageView, defaultImage);
                });
                imageView.setImage(webImage);
            } else {
                File imgFile = new File("src/main/resources/" + path);
                if (imgFile.exists()) {
                    imageView.setImage(new Image(imgFile.toURI().toString()));
                } else {
                    setFallbackImage(imageView, defaultImage);
                }
            }
        } catch (Exception ignored) {
            setFallbackImage(imageView, defaultImage);
        }
    }

    private void setFallbackImage(ImageView imageView, String resourcePath) {
        try {
            java.net.URL url = getClass().getResource(resourcePath);
            if (url != null) imageView.setImage(new Image(url.toExternalForm()));
        } catch (Exception e) {
            imageView.setImage(null);
        }
    }

    @FXML
    private void handleUploadDevicePhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Device Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            devicePhotoPreview.setImage(new Image(file.toURI().toString()));
            if (lblDevicePhotoName != null) lblDevicePhotoName.setText(file.getName());

            new Thread(() -> {
                try {
                    String cloudUrl = SupabaseStorageHelper.uploadImage(file, "DEV_" + System.currentTimeMillis());
                    if (cloudUrl != null) {
                        devicePhotoPath = cloudUrl;
                        isPhotoUpdated = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        // If we are editing, we don't need to validate input since everything is locked.
        if (editingDevice == null && !isInputValid()) return;

        if (editingDevice != null) {
            processDeviceUpdate(event);
        } else {
            String rawSelection = cmbOwnerName.getEditor().getText();
            String studentNumber = rawSelection.substring(rawSelection.indexOf("[") + 1, rawSelection.indexOf("]"));
            String ownerName = rawSelection.substring(0, rawSelection.indexOf("[")).trim();

            String[] modelSplit = txtModel.getText().split(" ", 2);
            String brand = modelSplit[0];
            String modelStr = modelSplit.length > 1 ? modelSplit[1] : "Unknown";

            processNewDeviceRegistration(event, studentNumber, ownerName, brand, modelStr);
        }
    }

    private void processDeviceUpdate(ActionEvent event) {
        if (!isPhotoUpdated) {
            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "No Changes", "No new photo was uploaded. The device profile remains unchanged.");
                closeStage(event);
            });
            return;
        }

        // ONLY update the photo path, protecting hardware identity
        String updateQuery = "UPDATE devices SET photo_path = ? WHERE unique_code = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

            updateStmt.setString(1, devicePhotoPath);
            updateStmt.setString(2, editingDevice.getToken());
            updateStmt.executeUpdate();

            Platform.runLater(() -> {
                showAlert(Alert.AlertType.INFORMATION, "Update Successful", "Device photo successfully updated.");
                closeStage(event);
            });

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update device photo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processNewDeviceRegistration(ActionEvent event, String studentNumber, String ownerName, String brand, String modelStr) {
        String generatedToken = "TKN-" + (1000 + new Random().nextInt(9000));
        String getStudentIdQuery = "SELECT id FROM students WHERE school_id = ?";
        String insertQuery = "INSERT INTO devices (student_id, device_type, device_brand, device_name, mac_address, unique_code, status, photo_path) VALUES (?, ?, ?, ?, ?, ?, 'REGISTERED', ?)";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(getStudentIdQuery)) {

            selectStmt.setString(1, studentNumber);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                UUID dbStudentId = (UUID) rs.getObject("id");

                try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                    insertStmt.setObject(1, dbStudentId);
                    insertStmt.setString(2, cmbDeviceType.getValue());
                    insertStmt.setString(3, brand);
                    insertStmt.setString(4, modelStr);
                    insertStmt.setString(5, txtMacAddress.getText().trim());
                    insertStmt.setString(6, generatedToken);
                    insertStmt.setString(7, devicePhotoPath);
                    insertStmt.executeUpdate();
                }

                newDevice = new Device(ownerName, cmbDeviceType.getValue(), txtModel.getText(), txtMacAddress.getText(), generatedToken);
                DataStore.getInstance().refreshDevices();

                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Device successfully registered to cloud for " + ownerName);
                    closeStage(event);
                });

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
        editingDevice = null;
        closeStage(event);
    }

    private boolean isInputValid() {
        StringBuilder errorBuilder = new StringBuilder();

        String owner = cmbOwnerName.getEditor().getText();
        if (owner == null || owner.trim().isEmpty() || !owner.contains("[") || !owner.contains("]")) {
            errorBuilder.append("- Please search and select a valid registered student from the dropdown.\n");
        }
        if (cmbDeviceType.getValue() == null) errorBuilder.append("- Device Type selection is required.\n");
        if (txtModel.getText() == null || txtModel.getText().trim().isEmpty()) errorBuilder.append("- Brand and Model description is required.\n");
        String mac = txtMacAddress.getText();
        if (mac == null || mac.trim().isEmpty() || (!mac.equalsIgnoreCase("N/A") && !MAC_PATTERN.matcher(mac).matches())) {
            errorBuilder.append("- Valid MAC Address is required (e.g., 00:1B:44:11:3A:B7 or N/A).\n");
        }

        if (errorBuilder.length() == 0) return true;

        showAlert(Alert.AlertType.WARNING, "Validation Error", "Please correct the following:\n\n" + errorBuilder.toString());
        return false;
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

    public Device getNewDevice() { return newDevice; }
}