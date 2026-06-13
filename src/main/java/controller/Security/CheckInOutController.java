package controller.Security;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
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
    @FXML private ImageView imgStudentPhoto;

    @FXML private FlowPane deviceContainer; // This will now hold our rich photo cards!

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
            this.brand = (brand != null) ? brand : "Unknown";
            this.model = (model != null) ? model : "Device";
            this.serial = (serial != null) ? serial : "N/A";
            this.accessCode = (accessCode != null) ? accessCode : "N/A";
            this.photoPath = (photoPath != null && !photoPath.isEmpty()) ? photoPath : "default_device.png";
        }
    }

    @FXML
    public void initialize() {
        cmbAction.getItems().addAll("Check-In", "Check-Out");
        cmbAction.getSelectionModel().selectFirst();
        cmbLocation.getItems().addAll("Main Gate","Side Gate","Library Entrance");
        cmbLocation.getSelectionModel().selectFirst();
        cmbGuard.getItems().addAll("Guard 01","Guard 02","Guard 03");
        cmbGuard.getSelectionModel().selectFirst();

        updateDateTime();
        startScanner();
    }

    private void updateDateTime() {
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy  hh:mm a")));
    }

    private void startScanner() {
        btnConfirm.setDisable(true);
        btnConfirm.setText("Confirm");
        statusLabel.setText("Scanning for QR...");
        statusLabel.setStyle("-fx-text-fill: #333333;");
        currentScannedStudent = null;
        currentDbStudentId = null;
        isCheckingOut = false;
        activeLogIds.clear();
        devicesInsideIds.clear();
        selectedDeviceIds.clear();
        studentDeviceList.clear();
        deviceContainer.getChildren().clear();
        deviceContainer.setDisable(false);
        if(imgStudentPhoto != null) imgStudentPhoto.setImage(null);

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
            statusLabel.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
            txtDeviceSearch.setText(studentNumber);
        });

        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                checkAccessStatus(conn, studentNumber);
                fetchStudentAndDeviceData(conn, studentNumber);
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("Database Connection Error");
                    statusLabel.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                });
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
        // Restricting the check to CURRENT_DATE to solve Bug 12 & 13!
        String query = "SELECT c.id as log_id, c.device_id FROM check_in_out c " +
                "JOIN students s ON c.student_id = s.id " +
                "WHERE s.school_id = ? AND c.status = 'CHECKED_IN' AND DATE(c.check_in_time) = CURRENT_DATE";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, studentNumber);
            ResultSet rs = pstmt.executeQuery();
            activeLogIds.clear();
            devicesInsideIds.clear();
            while (rs.next()) {
                activeLogIds.add(rs.getObject("log_id"));
                Object devId = rs.getObject("device_id");
                if (devId != null) devicesInsideIds.add(devId);
            }
            isCheckingOut = !activeLogIds.isEmpty();
        }
    }

    private void fetchStudentAndDeviceData(Connection conn, String identifier) throws SQLException {
        String query = "SELECT s.id as student_db_id, s.school_id, s.first_name, s.last_name, s.program_course, s.section, s.photo_path as student_photo, " +
                "d.id as device_id, d.device_brand, d.device_name, d.mac_address, d.unique_code, d.photo_path as device_photo " +
                "FROM students s LEFT JOIN devices d ON s.id = d.student_id WHERE s.school_id = ? OR d.unique_code = ?";

        List<DeviceRecord> foundDevices = new ArrayList<>();
        String fullName = "", studentNum = "", courseSection = "", studentPhoto = "";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, identifier);
            pstmt.setString(2, identifier);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                if (fullName.isEmpty()) {
                    currentDbStudentId = rs.getObject("student_db_id");
                    fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                    studentNum = rs.getString("school_id");
                    courseSection = rs.getString("program_course") + " - " + rs.getString("section");
                    studentPhoto = rs.getString("student_photo");
                }
                if (rs.getObject("device_id") != null) {
                    foundDevices.add(new DeviceRecord(rs.getObject("device_id"), rs.getString("device_brand"), rs.getString("device_name"), rs.getString("mac_address"), rs.getString("unique_code"), rs.getString("device_photo")));
                }
            }
        }

        if (fullName.isEmpty()) {
            Platform.runLater(() -> {
                statusLabel.setText("Unregistered QR Code");
                statusLabel.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
            });
            return;
        }

        studentDeviceList = foundDevices;
        final String fName = fullName, fNum = studentNum, fCourse = courseSection, fPhoto = studentPhoto;

        Platform.runLater(() -> {
            lblStudentName.setText(fName);
            lblStudentID.setText(fNum);
            lblCourseSection.setText(fCourse);
            loadImageToView(imgStudentPhoto, fPhoto);
            deviceContainer.getChildren().clear();
            selectedDeviceIds.clear();

            if (isCheckingOut) {
                lblCurrentStatus.setText("Currently INSIDE");
                cmbAction.getSelectionModel().select("Check-Out");
                for (DeviceRecord d : foundDevices) {
                    if (devicesInsideIds.contains(d.id)) {
                        ToggleButton card = createDeviceCard(d);
                        card.setSelected(true);
                        card.setDisable(true); // Lock selection
                        deviceContainer.getChildren().add(card);
                    }
                }
            } else {
                lblCurrentStatus.setText("Currently OUTSIDE");
                cmbAction.getSelectionModel().select("Check-In");
                for (DeviceRecord d : foundDevices) {
                    deviceContainer.getChildren().add(createDeviceCard(d));
                }
            }
            updateSelectionLabels();
            statusLabel.setText("Verification Ready");
            statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
            btnConfirm.setDisable(false);
        });
    }

    // THE PHASE 3 FIX: Dynamic Photo Grid Cards!
    private ToggleButton createDeviceCard(DeviceRecord device) {
        ToggleButton btn = new ToggleButton();

        // Build the inner visual layout of the card
        VBox cardLayout = new VBox(5);
        cardLayout.setAlignment(Pos.CENTER);
        cardLayout.setPadding(new Insets(10));

        // Inject the Image
        ImageView img = new ImageView();
        img.setFitWidth(100);
        img.setFitHeight(90);
        img.setPreserveRatio(true);
        loadImageToView(img, device.photoPath);

        // Inject the Device Name
        Label lblName = new Label(device.brand + " " + device.model);
        lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #222222;");
        lblName.setWrapText(true);
        lblName.setTextAlignment(TextAlignment.CENTER);

        // Inject the Code
        Label lblDetails = new Label("Token: " + device.accessCode);
        lblDetails.setStyle("-fx-font-size: 9px; -fx-text-fill: #666666;");

        cardLayout.getChildren().addAll(img, lblName, lblDetails);
        btn.setGraphic(cardLayout);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        String defaultStyle = "-fx-background-color: #FFFFFF; -fx-border-color: #DDDDDD; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);";
        String selectedStyle = "-fx-background-color: #FEF0F0; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(80,10,14,0.2), 8, 0, 0, 3);";

        btn.setStyle(defaultStyle);

        btn.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
            btn.setStyle(isNowSelected ? selectedStyle : defaultStyle);
            if (isNowSelected) {
                if (!selectedDeviceIds.contains(device.id)) selectedDeviceIds.add(device.id);
            } else {
                selectedDeviceIds.remove(device.id);
            }
            updateSelectionLabels();
        });

        return btn;
    }

    private void loadImageToView(ImageView imageView, String path) {
        if (imageView == null || path == null || path.isEmpty()) return;
        try {
            if (path.startsWith("http")) {
                imageView.setImage(new Image(path, true));
            } else {
                File imgFile = new File("src/main/resources/" + path);
                if (imgFile.exists()) imageView.setImage(new Image(imgFile.toURI().toString()));
            }
        } catch (Exception ignored) {}
    }

    private void updateSelectionLabels() {
        if (selectedDeviceIds.isEmpty()) {
            lblSerialNumber.setText("No devices selected");
            lblAccessCode.setText("-");
        } else {
            lblSerialNumber.setText(selectedDeviceIds.size() + " Device(s) Selected");
            lblAccessCode.setText("Ready for processing");
        }
    }

    @FXML
    private void handleConfirm() {
        if (lblStudentID.getText().isEmpty() || currentDbStudentId == null) return;
        btnConfirm.setDisable(true);
        btnConfirm.setText("Processing...");
        deviceContainer.setDisable(true);

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
                    showAlert(Alert.AlertType.INFORMATION, (isCheckingOut ? "Check-Out" : "Check-In") + " logged successfully!");
                    handleClear();
                    startScanner();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    btnConfirm.setText("Confirm");
                    btnConfirm.setDisable(false);
                    deviceContainer.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Database error during transaction.");
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
        if(imgStudentPhoto != null) imgStudentPhoto.setImage(null);
        currentDbStudentId = null;
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