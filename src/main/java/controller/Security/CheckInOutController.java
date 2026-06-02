package controller.Security;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import utils.DataStore;
import utils.DatabaseHelper;
import utils.QRScannerThread;

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

    private boolean isCheckingOut = false;

    // Lists to track multiple device selections and multiple active database logs
    private List<Integer> activeLogIds = new ArrayList<>();
    private List<Integer> devicesInsideIds = new ArrayList<>();
    private List<Integer> selectedDeviceIds = new ArrayList<>();
    private List<DeviceRecord> studentDeviceList = new ArrayList<>();

    private static class DeviceRecord {
        int id;
        String brand, model, serial, accessCode;

        public DeviceRecord(int id, String brand, String model, String serial, String accessCode) {
            this.id = id;
            this.brand = (brand != null) ? brand : "Unknown Brand";
            this.model = (model != null) ? model : "Unknown Model";
            this.serial = (serial != null) ? serial : "N/A";
            this.accessCode = (accessCode != null) ? accessCode : "N/A";
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
        isCheckingOut = false;

        activeLogIds.clear();
        devicesInsideIds.clear();
        selectedDeviceIds.clear();
        studentDeviceList.clear();
        deviceContainer.getChildren().clear();

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
        String query = "SELECT log_id, device_id FROM access_logs WHERE student_number = ? AND status = 'INSIDE'";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentNumber);
            ResultSet rs = pstmt.executeQuery();

            activeLogIds.clear();
            devicesInsideIds.clear();

            while (rs.next()) {
                activeLogIds.add(rs.getInt("log_id"));
                int devId = rs.getInt("device_id");
                if (!rs.wasNull()) {
                    devicesInsideIds.add(devId);
                }
            }
            isCheckingOut = !activeLogIds.isEmpty();
        }
    }

    private void fetchStudentAndDeviceData(Connection conn, String identifier) throws SQLException {
        String query = "SELECT s.student_number, s.first_name, s.last_name, s.course, s.section, " +
                "d.device_id, d.brand, d.model, d.serial_number, d.access_code " +
                "FROM students s " +
                "LEFT JOIN devices d ON s.student_id = d.student_id " +
                "WHERE s.student_number = ? OR d.access_code = ?";

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
                    fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                    studentNum = rs.getString("student_number");
                    String course = rs.getString("course");
                    String section = rs.getString("section");
                    courseSection = (course != null ? course : "N/A") + " - " + (section != null ? section : "N/A");
                }

                int dbDeviceId = rs.getInt("device_id");
                if (!rs.wasNull()) {
                    foundDevices.add(new DeviceRecord(
                            dbDeviceId, rs.getString("brand"), rs.getString("model"),
                            rs.getString("serial_number"), rs.getString("access_code")
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

        Platform.runLater(() -> {
            lblStudentName.setText(finalFullName);
            lblStudentID.setText(finalStudentNum);
            lblCourseSection.setText(finalCourseSec);

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
                            card.setDisable(true); // Locked. They must check out with what they brought in.
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

                    // Auto-select the first device by default just to save the guard a click
                    if (!deviceContainer.getChildren().isEmpty()) {
                        ((ToggleButton) deviceContainer.getChildren().get(0)).setSelected(true);
                    }
                }
                btnConfirm.setDisable(false);
            }

            // EXPLICIT FORCE UPDATE OF LABELS UPON LOAD
            updateSelectionLabels();
            statusLabel.setText("Verification Ready");
        });
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
                selectedDeviceIds.remove(Integer.valueOf(device.id));
            }
            updateSelectionLabels();
        });

        return btn;
    }

    // Cleanly updates the labels if multiple devices are clicked with a bulleted list
    private void updateSelectionLabels() {
        if (selectedDeviceIds.isEmpty()) {
            lblSerialNumber.setText("N/A");
            lblAccessCode.setText("N/A");
        } else if (selectedDeviceIds.size() == 1) {
            DeviceRecord selected = studentDeviceList.stream()
                    .filter(d -> d.id == selectedDeviceIds.get(0)).findFirst().orElse(null);
            if (selected != null) {
                lblSerialNumber.setText(selected.serial);
                lblAccessCode.setText(selected.accessCode);
            }
        } else {
            // Multiple selected - build a dynamic, bulleted multi-line list
            StringBuilder serials = new StringBuilder();
            StringBuilder codes = new StringBuilder();

            for (int id : selectedDeviceIds) {
                DeviceRecord d = studentDeviceList.stream()
                        .filter(x -> x.id == id).findFirst().orElse(null);
                if (d != null) {
                    serials.append("• ").append(d.brand).append(" ").append(d.model).append(": ").append(d.serial).append("\n");
                    codes.append("• ").append(d.brand).append(" ").append(d.model).append(": ").append(d.accessCode).append("\n");
                }
            }

            // .trim() removes the extra invisible newline at the very end
            lblSerialNumber.setText(serials.toString().trim());
            lblAccessCode.setText(codes.toString().trim());
        }
    }

    @FXML
    private void handleConfirm() {
        if (lblStudentID.getText().isEmpty() || currentScannedStudent == null) return;
        btnConfirm.setDisable(true);

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                if (isCheckingOut && !activeLogIds.isEmpty()) {
                    String update = "UPDATE access_logs SET time_out = CURRENT_TIMESTAMP, status = 'CLEARED' WHERE log_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(update)) {
                        for (int logId : activeLogIds) {
                            pstmt.setInt(1, logId);
                            pstmt.addBatch();
                        }
                        pstmt.executeBatch();
                    }
                } else if (!isCheckingOut) {
                    String insert = "INSERT INTO access_logs (student_number, device_id, status) VALUES (?, ?, 'INSIDE')";
                    try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                        if (selectedDeviceIds.isEmpty()) {
                            pstmt.setString(1, currentScannedStudent);
                            pstmt.setNull(2, Types.INTEGER);
                            pstmt.executeUpdate();
                        } else {
                            for (int devId : selectedDeviceIds) {
                                pstmt.setString(1, currentScannedStudent);
                                pstmt.setInt(2, devId);
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