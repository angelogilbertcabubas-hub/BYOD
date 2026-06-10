package controller.Security;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import utils.DataStore;
import utils.DatabaseHelper;
import utils.QRScannerThread;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CheckInOutController extends BaseSecurityController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private TextField txtDeviceSearch;
    @FXML private Button btnSearch;

    @FXML private Label lblStudentName;
    @FXML private Label lblStudentID;
    @FXML private Label lblCourseSection;

    // NEW: FXML IDs mapped to the visual layout
    @FXML private ImageView imgStudentPhoto;
    @FXML private ImageView imgDevicePhoto;

    @FXML private FlowPane deviceContainer;

    @FXML private Label lblSerialNumber;
    @FXML private Label lblAccessCode;
    @FXML private Label lblCurrentStatus;

    @FXML private ComboBox<String> cmbAction;
    @FXML private ComboBox<String> cmbLocation;
    @FXML private ComboBox<String> cmbGuard;
    @FXML private TextArea txtRemarks;
    @FXML private Label lblDateTime;
    @FXML private Button btnConfirm;
    @FXML private Button btnClear;

    private QRScannerThread scannerThread;
    private String currentScannedStudent;
    private Object currentDbStudentId;
    private String currentStudentPhotoPath;

    private boolean isCheckingOut = false;

    private List<Object> activeLogIds = new ArrayList<>();
    private List<Object> devicesInsideIds = new ArrayList<>();
    private List<Object> selectedDeviceIds = new ArrayList<>();
    private List<DeviceRecord> studentDeviceList = new ArrayList<>();

    private static class DeviceRecord {
        Object id;
        String brand, model, serial, accessCode, photoPath;

        public DeviceRecord(Object id, String brand, String model, String serial, String accessCode, String photoPath) {
            this.id = id;
            this.brand = (brand != null) ? brand : "Unknown Brand";
            this.model = (model != null) ? model : "Unknown Model";
            this.serial = (serial != null) ? serial : "N/A";
            this.accessCode = (accessCode != null) ? accessCode : "N/A";
            this.photoPath = (photoPath != null) ? photoPath : "default_device.png";
        }
    }

    @FXML
    public void initialize() {
        cmbAction.getItems().addAll("Check-In", "Check-Out");
        cmbAction.getSelectionModel().selectFirst();
        cmbLocation.getItems().addAll("Main Gate","Side Gate","Back Gate","Library Entrance","Admin Building");
        cmbLocation.getSelectionModel().selectFirst();
        cmbGuard.getItems().addAll("Guard 01","Guard 02","Guard 03","Guard 04");
        cmbGuard.getSelectionModel().selectFirst();

        updateDateTime();
        startScanner();
    }

    private void updateDateTime() {
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy  hh:mm a")));
    }

    private void startScanner() {
        btnConfirm.setDisable(true);
        statusLabel.setText("Scanning for QR...");
        currentScannedStudent = null;
        currentDbStudentId = null;
        currentStudentPhotoPath = null;
        isCheckingOut = false;

        activeLogIds.clear();
        devicesInsideIds.clear();
        selectedDeviceIds.clear();
        studentDeviceList.clear();
        deviceContainer.getChildren().clear();

        if(imgStudentPhoto != null) imgStudentPhoto.setImage(null);
        if(imgDevicePhoto != null) imgDevicePhoto.setImage(null);

        if (scannerThread != null) scannerThread.stopScanner();

        scannerThread = new QRScannerThread(cameraView, this::processScannedCode);
        scannerThread.setDaemon(true);
        scannerThread.start();
    }

    @FXML
    private void restartScanner(ActionEvent event) {
        handleClear();
        startScanner();
    }

    private void processScannedCode(String studentNumber) {
        currentScannedStudent = studentNumber;

        Platform.runLater(() -> {
            statusLabel.setText("QR Detected! Fetching Data...");
            txtDeviceSearch.setText(studentNumber);
        });

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                checkAccessStatus(conn, studentNumber);
                fetchStudentAndDeviceData(conn, studentNumber);
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> statusLabel.setText("Database Connection Error"));
            }
        }).start();
    }

    @FXML
    private void handleSearch() {
        if (txtDeviceSearch.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please enter an Access Code or Student ID.");
            return;
        }
        if (scannerThread != null) scannerThread.stopScanner();
        processScannedCode(txtDeviceSearch.getText().trim());
    }

    private void checkAccessStatus(Connection conn, String studentNumber) throws SQLException {
        String query = "SELECT c.id as log_id, c.device_id FROM check_in_out c " +
                "JOIN students s ON c.student_id = s.id " +
                "WHERE s.school_id = ? AND c.status = 'CHECKED_IN'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentNumber);
            ResultSet rs = pstmt.executeQuery();

            activeLogIds.clear();
            devicesInsideIds.clear();

            while (rs.next()) {
                activeLogIds.add(rs.getObject("log_id"));
                Object devId = rs.getObject("device_id");
                if (devId != null) {
                    devicesInsideIds.add(devId);
                }
            }
            isCheckingOut = !activeLogIds.isEmpty();
        }
    }

    private void fetchStudentAndDeviceData(Connection conn, String identifier) throws SQLException {
        String query = "SELECT s.id as student_db_id, s.school_id, s.first_name, s.last_name, s.program_course, s.section, s.photo_path as student_photo, " +
                "d.id as device_id, d.device_brand, d.device_name, d.mac_address, d.unique_code, d.photo_path as device_photo " +
                "FROM students s " +
                "LEFT JOIN devices d ON s.id = d.student_id " +
                "WHERE s.school_id = ? OR d.unique_code = ?";

        List<DeviceRecord> foundDevices = new ArrayList<>();
        String fullName = "";
        String studentNum = "";
        String courseSection = "";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, identifier);
            pstmt.setString(2, identifier);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                if (fullName.isEmpty()) {
                    currentDbStudentId = rs.getObject("student_db_id");
                    fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                    studentNum = rs.getString("school_id");
                    String course = rs.getString("program_course");
                    String section = rs.getString("section");
                    courseSection = (course != null ? course : "N/A") + " - " + (section != null ? section : "N/A");
                    currentStudentPhotoPath = rs.getString("student_photo");
                }

                Object dbDeviceId = rs.getObject("device_id");
                if (dbDeviceId != null) {
                    foundDevices.add(new DeviceRecord(
                            dbDeviceId, rs.getString("device_brand"), rs.getString("device_name"),
                            rs.getString("mac_address"), rs.getString("unique_code"), rs.getString("device_photo")
                    ));
                }
            }
        }

        if (fullName.isEmpty()) {
            Platform.runLater(() -> {
                statusLabel.setText("Unregistered QR Code");
                showAlert(Alert.AlertType.ERROR, "No student found for: " + identifier);
            });
            return;
        }

        studentDeviceList = foundDevices;
        final String finalFullName = fullName;
        final String finalStudentNum = studentNum;
        final String finalCourseSec = courseSection;
        final String finalStudentPhoto = currentStudentPhotoPath;

        Platform.runLater(() -> {
            lblStudentName.setText(finalFullName);
            lblStudentID.setText(finalStudentNum);
            lblCourseSection.setText(finalCourseSec);

            loadImageToView(imgStudentPhoto, finalStudentPhoto);

            deviceContainer.getChildren().clear();
            selectedDeviceIds.clear();

            if (isCheckingOut) {
                lblCurrentStatus.setText("Currently INSIDE");
                cmbAction.getSelectionModel().select("Check-Out");

                if (devicesInsideIds.isEmpty()) {
                    Label noDev = new Label("No Device Brought Inside");
                    noDev.setStyle("-fx-text-fill: #999999; -fx-font-style: italic;");
                    deviceContainer.getChildren().add(noDev);
                } else {
                    for (DeviceRecord d : foundDevices) {
                        if (devicesInsideIds.contains(d.id)) {
                            ToggleButton card = createDeviceCard(d);
                            deviceContainer.getChildren().add(card);
                            card.setSelected(true);
                            card.setDisable(true);
                        }
                    }
                }
                btnConfirm.setDisable(false);

            } else {
                lblCurrentStatus.setText("Currently OUTSIDE");
                cmbAction.getSelectionModel().select("Check-In");

                if (foundDevices.isEmpty()) {
                    Label noDev = new Label("No Registered Devices");
                    noDev.setStyle("-fx-text-fill: #999999; -fx-font-style: italic;");
                    deviceContainer.getChildren().add(noDev);
                } else {
                    for (DeviceRecord d : foundDevices) {
                        ToggleButton card = createDeviceCard(d);
                        deviceContainer.getChildren().add(card);
                    }

                    if (!deviceContainer.getChildren().isEmpty()) {
                        ((ToggleButton) deviceContainer.getChildren().get(0)).setSelected(true);
                    }
                }
                btnConfirm.setDisable(false);
            }

            updateSelectionLabels();
            statusLabel.setText("Verification Ready");
        });
    }

    private void loadImageToView(ImageView imageView, String relativePath) {
        if (imageView == null || relativePath == null || relativePath.isEmpty()) return;
        try {
            File imgFile = new File("src/main/resources/" + relativePath);
            if (imgFile.exists()) {
                imageView.setImage(new Image(imgFile.toURI().toString()));
            } else {
                imageView.setImage(null);
            }
        } catch (Exception e) {
            System.err.println("Could not load image: " + relativePath);
        }
    }

    private ToggleButton createDeviceCard(DeviceRecord device) {
        ToggleButton btn = new ToggleButton(device.brand + "\n" + device.model);
        btn.setUserData(device);

        String defaultStyle = "-fx-background-color: #F7F5F5; -fx-border-color: #E2DDD9; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 15; -fx-cursor: hand; -fx-text-alignment: center; -fx-font-size: 11px; -fx-text-fill: #333333;";
        String selectedStyle = "-fx-background-color: #500A0E; -fx-border-color: #500A0E; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 15; -fx-cursor: hand; -fx-text-alignment: center; -fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: bold;";

        btn.setStyle(defaultStyle);

        btn.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            if (isNowSelected) {
                btn.setStyle(selectedStyle);
                if (!selectedDeviceIds.contains(device.id)) {
                    selectedDeviceIds.add(device.id);
                }
            } else {
                btn.setStyle(defaultStyle);
                selectedDeviceIds.remove(device.id);
            }
            updateSelectionLabels();
        });

        return btn;
    }

    private void updateSelectionLabels() {
        if (selectedDeviceIds.isEmpty()) {
            lblSerialNumber.setText("N/A");
            lblAccessCode.setText("N/A");
            if(imgDevicePhoto != null) imgDevicePhoto.setImage(null);
        } else if (selectedDeviceIds.size() == 1) {
            DeviceRecord selected = studentDeviceList.stream()
                    .filter(d -> d.id.equals(selectedDeviceIds.get(0))).findFirst().orElse(null);
            if (selected != null) {
                lblSerialNumber.setText(selected.serial);
                lblAccessCode.setText(selected.accessCode);
                loadImageToView(imgDevicePhoto, selected.photoPath);
            }
        } else {
            StringBuilder serials = new StringBuilder();
            StringBuilder codes = new StringBuilder();

            for (Object id : selectedDeviceIds) {
                DeviceRecord d = studentDeviceList.stream()
                        .filter(x -> x.id.equals(id)).findFirst().orElse(null);
                if (d != null) {
                    serials.append("• ").append(d.brand).append(" ").append(d.model).append(": ").append(d.serial).append("\n");
                    codes.append("• ").append(d.brand).append(" ").append(d.model).append(": ").append(d.accessCode).append("\n");
                }
            }

            lblSerialNumber.setText(serials.toString().trim());
            lblAccessCode.setText(codes.toString().trim());
            if(imgDevicePhoto != null) imgDevicePhoto.setImage(null);
        }
    }

    @FXML
    private void handleConfirm() {
        if (lblStudentID.getText().isEmpty() || currentScannedStudent == null || currentDbStudentId == null) return;
        btnConfirm.setDisable(true);

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (isCheckingOut && !activeLogIds.isEmpty()) {
                    String update = "UPDATE check_in_out SET check_out_time = CURRENT_TIMESTAMP, status = 'CHECKED_OUT' WHERE id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(update)) {
                        for (Object logId : activeLogIds) {
                            pstmt.setObject(1, logId);
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }
                } else if (!isCheckingOut) {
                    String insert = "INSERT INTO check_in_out (student_id, device_id, status) VALUES (?, ?, 'CHECKED_IN')";
                    try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                        if (selectedDeviceIds.isEmpty()) {
                            pstmt.setObject(1, currentDbStudentId);
                            pstmt.setNull(2, Types.OTHER);
                            pstmt.executeUpdate();
                        } else {
                            for (Object devId : selectedDeviceIds) {
                                pstmt.setObject(1, currentDbStudentId);
                                pstmt.setObject(2, devId);
                                pstmt.addBatch();
                            }
                            pstmt.executeBatch();
                        }
                    }
                }

                DataStore.getInstance().refreshLogs();

                Platform.runLater(() -> {
                    String actionStr = isCheckingOut ? "Check-Out" : "Check-In";
                    showAlert(Alert.AlertType.INFORMATION, actionStr + " logged successfully!\n\nStudent: " + lblStudentName.getText());
                    handleClear();
                    startScanner();
                });
            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Database error during transaction.");
                    btnConfirm.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void handleClear() {
        if (scannerThread != null) scannerThread.stopScanner();

        txtDeviceSearch.clear();
        txtRemarks.clear();

        lblStudentName.setText("");
        lblStudentID.setText("");
        lblCourseSection.setText("");

        deviceContainer.getChildren().clear();
        selectedDeviceIds.clear();

        lblSerialNumber.setText("");
        lblAccessCode.setText("");
        lblCurrentStatus.setText("");
        statusLabel.setText("Scanner Stopped");

        if(imgStudentPhoto != null) imgStudentPhoto.setImage(null);
        if(imgDevicePhoto != null) imgDevicePhoto.setImage(null);

        currentDbStudentId = null;
        currentStudentPhotoPath = null;

        cmbAction.getSelectionModel().selectFirst();
        cmbLocation.getSelectionModel().selectFirst();
        cmbGuard.getSelectionModel().selectFirst();

        btnConfirm.setDisable(true);
        updateDateTime();
    }

    public void shutdown() {
        if (scannerThread != null) scannerThread.stopScanner();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("BYOD System");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}