package controller.Admin;

import com.example.byod.model.Device;
import com.example.byod.model.Student;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.DataStore;
import utils.DatabaseHelper;
import utils.QRCodeGenerator;
import utils.SupabaseStorageHelper; // NEW CLOUD HELPER IMPORT

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;

public class StudentProfileModalController {

    @FXML private Label lblFullNameHeader;
    @FXML private Label lblStudentNumberSub;
    @FXML private Label txtProfileId;
    @FXML private Label txtProfileCourse;
    @FXML private Label txtProfileEmail;
    @FXML private Label txtProfileMobile;

    @FXML private ImageView qrCodeImageView;
    @FXML private ImageView devicePhotoImageView;

    @FXML private TableView<Device> deviceMatrixTable;
    @FXML private TableColumn<Device, String> colType;
    @FXML private TableColumn<Device, String> colModel;
    @FXML private TableColumn<Device, String> colMac;
    @FXML private TableColumn<Device, String> colToken;

    @FXML private ComboBox<String> quickTypeBox;
    @FXML private TextField quickModelField;
    @FXML private TextField quickMacField;

    private Student focusedStudent;
    private UUID databaseStudentUuid;
    private ObservableList<Device> isolatedDevicesList = FXCollections.observableArrayList();

    private Map<String, String> devicePhotoMap = new HashMap<>();
    private String quickDevicePhotoPath = "default_device.png";

    private final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    @FXML
    public void initialize() {
        colType.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("brandModel"));
        colMac.setCellValueFactory(new PropertyValueFactory<>("macAddress"));
        colToken.setCellValueFactory(new PropertyValueFactory<>("accessCode"));

        deviceMatrixTable.setItems(isolatedDevicesList);
        quickTypeBox.getItems().addAll("Smartphone", "Laptop", "Tablet", "Speaker", "Projector", "Smart Watch", "Others");

        deviceMatrixTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String path = devicePhotoMap.get(newSelection.getAccessCode());
                loadDevicePhoto(path);
            } else {
                devicePhotoImageView.setImage(null);
            }
        });
    }

    public void initData(Student targetStudent) {
        this.focusedStudent = targetStudent;

        lblFullNameHeader.setText(targetStudent.getFullName());
        lblStudentNumberSub.setText("Dossier Node File Record Reference ID: " + targetStudent.getStudentId());
        txtProfileId.setText(targetStudent.getStudentId());
        txtProfileCourse.setText(targetStudent.getCourse());
        txtProfileEmail.setText(targetStudent.getEmail());
        txtProfileMobile.setText(targetStudent.getMobile());

        loadOrRegenerateQRCode();
        fetchInternalUuidAndDevices();
    }

    // FIX #1: Teach the UI how to read Cloud URLs
    private void loadDevicePhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            devicePhotoImageView.setImage(null);
            return;
        }
        try {
            if (photoPath.startsWith("http")) {
                // It is a cloud image! The 'true' flag loads it in the background without freezing the app.
                devicePhotoImageView.setImage(new Image(photoPath, true));
            } else {
                // Fallback for your old local images
                File imgFile = new File("src/main/resources/" + photoPath);
                if (imgFile.exists()) {
                    devicePhotoImageView.setImage(new Image(imgFile.toURI().toString()));
                } else {
                    devicePhotoImageView.setImage(null);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load device photo: " + photoPath);
        }
    }

    private void loadOrRegenerateQRCode() {
        try {
            String safeName = focusedStudent.getFullName().replaceAll("\\s+", "_");
            String fileName = focusedStudent.getStudentId() + "_" + safeName + ".png";
            String filePath = "src/main/resources/qrcodes/" + fileName;
            File qrFile = new File(filePath);

            if (!qrFile.exists()) {
                QRCodeGenerator.generateStudentQRCode(focusedStudent.getStudentId(), focusedStudent.getFullName());
            }

            if (qrFile.exists()) {
                qrCodeImageView.setImage(new Image(qrFile.toURI().toString()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchInternalUuidAndDevices() {
        isolatedDevicesList.clear();
        devicePhotoMap.clear();

        String fetchUuidSql = "SELECT id FROM students WHERE school_id = ?";
        String fetchDevicesSql = "SELECT device_type, device_brand, device_name, mac_address, unique_code, photo_path FROM devices WHERE student_id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmtUuid = conn.prepareStatement(fetchUuidSql)) {

            stmtUuid.setString(1, focusedStudent.getStudentId());
            try (ResultSet rsUuid = stmtUuid.executeQuery()) {
                if (rsUuid.next()) {
                    databaseStudentUuid = (UUID) rsUuid.getObject("id");
                }
            }

            if (databaseStudentUuid != null) {
                try (PreparedStatement stmtDev = conn.prepareStatement(fetchDevicesSql)) {
                    stmtDev.setObject(1, databaseStudentUuid);
                    try (ResultSet rsDev = stmtDev.executeQuery()) {
                        while (rsDev.next()) {
                            String type = rsDev.getString("device_type");
                            String brandModel = rsDev.getString("device_brand") + " " + rsDev.getString("device_name");
                            String mac = rsDev.getString("mac_address");
                            String token = rsDev.getString("unique_code");
                            String photo = rsDev.getString("photo_path");

                            isolatedDevicesList.add(new Device(focusedStudent.getFullName(), type, brandModel, mac, token));
                            devicePhotoMap.put(token, photo);
                        }
                    }
                }
            }

            if (!isolatedDevicesList.isEmpty()) {
                deviceMatrixTable.getSelectionModel().select(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // FIX #2: Make the "Quick Add" photo button use the Cloud Helper
    @FXML
    private void handleQuickUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Device Photo");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                // Upload directly to Supabase
                quickDevicePhotoPath = SupabaseStorageHelper.uploadImage(file, "DEV");
                showAlert(Alert.AlertType.INFORMATION, "Cloud Photo Attached", "The image was successfully staged for the new device.");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Upload Failed", "Could not reach cloud storage.");
            }
        }
    }

    @FXML
    private void handleQuickAddDevice(ActionEvent event) {
        String type = quickTypeBox.getValue();
        String rawModel = quickModelField.getText().trim();
        String mac = quickMacField.getText().trim().toUpperCase();

        if (type == null || rawModel.isEmpty() || mac.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Form Empty", "Please fill up all asset attachment parameters.");
            return;
        }

        if (!mac.equalsIgnoreCase("N/A") && !MAC_PATTERN.matcher(mac).matches()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Parameter", "MAC registration layout fail. Standardized layout: 00:1B:44:11:3A:B7 or N/A");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String insertSql = "INSERT INTO devices (student_id, device_type, device_brand, device_name, mac_address, unique_code, status, photo_path) VALUES (?, ?, ?, ?, ?, ?, 'REGISTERED', ?)";

            if (mac.equals("N/A")) mac = "N/A-" + (100 + new Random().nextInt(900));

            String[] modelSplit = rawModel.split(" ", 2);
            String brand = modelSplit[0];
            String modelStr = modelSplit.length > 1 ? modelSplit[1] : "Unknown";
            String generatedToken = "TKN-" + (1000 + new Random().nextInt(9000));

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setObject(1, databaseStudentUuid);
                ps.setString(2, type);
                ps.setString(3, brand);
                ps.setString(4, modelStr);
                ps.setString(5, mac);
                ps.setString(6, generatedToken);
                ps.setString(7, quickDevicePhotoPath);
                ps.executeUpdate();
            }

            DataStore.getInstance().refreshDevices();
            fetchInternalUuidAndDevices();

            quickModelField.clear();
            quickMacField.clear();
            quickTypeBox.setValue(null);
            quickDevicePhotoPath = "default_device.png";

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Linkage Aborted", "Asset mapping error: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveSelectedDevice(ActionEvent event) {
        Device selectedDevice = deviceMatrixTable.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) {
            showAlert(Alert.AlertType.WARNING, "Zero Selection", "Please choose a hardware object partition to drop.");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String deleteSql = "DELETE FROM devices WHERE unique_code = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, selectedDevice.getAccessCode());
                ps.executeUpdate();
            }

            DataStore.getInstance().refreshDevices();
            fetchInternalUuidAndDevices();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Drop Interrupted", "Storage update execution dropped: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveStudent(ActionEvent event) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Destructive Sequence Triggered");
        confirmation.setHeaderText("Wipe target student profile entity: " + focusedStudent.getFullName());
        confirmation.setContentText("Proceeding will invoke foreign reference CASCADE dropping structural logs and mapped network asset links. Are you sure?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try (Connection conn = DatabaseHelper.getConnection()) {
                String purgeSql = "DELETE FROM students WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(purgeSql)) {
                    ps.setObject(1, databaseStudentUuid);
                    ps.executeUpdate();
                }

                DataStore.getInstance().getStudentsList().remove(focusedStudent);
                DataStore.getInstance().refreshStudents();
                DataStore.getInstance().refreshDevices();
                DataStore.getInstance().refreshLogs();

                handleCloseModal(event);

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Purge Execution Dropped", "Target entity dropping transaction terminated: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleCloseModal(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String header, String body) {
        Alert alert = new Alert(type);
        alert.setTitle("Account Provisioning System Core");
        alert.setHeaderText(header);
        alert.setContentText(body);
        alert.showAndWait();
    }
}