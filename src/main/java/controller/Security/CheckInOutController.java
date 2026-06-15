package controller.Security;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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

    @FXML private HBox deviceContainer;

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
        String brand, model, serial, accessCode, photoPath, status;

        public DeviceRecord(Object id, String brand, String model, String serial, String accessCode, String photoPath, String status) {
            this.id = id;
            this.brand = (brand != null) ? brand : "Unknown";
            this.model = (model != null) ? model : "Device";
            this.serial = (serial != null) ? serial : "N/A";
            this.accessCode = (accessCode != null) ? accessCode : "N/A";
            this.photoPath = (photoPath != null && !photoPath.isEmpty()) ? photoPath : "";
            this.status = (status != null) ? status : "ACTIVE";
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

    // --- Phase 2: Intercepting Navigation to Kill Camera Memory Leak ---
    @FXML @Override public void goToDashboard()      { shutdown(); super.goToDashboard(); }
    @FXML @Override public void goToCheckInOut()     { shutdown(); super.goToCheckInOut(); }
    @FXML @Override public void goToMonitoringLogs() { shutdown(); super.goToMonitoringLogs(); }
    @FXML @Override public void goToActiveDevices()  { shutdown(); super.goToActiveDevices(); }
    @FXML @Override public void goToReports()        { shutdown(); super.goToReports(); }
    @FXML @Override public void handleLogout()       { shutdown(); super.handleLogout(); }

    @Override public void goToDashboard(javafx.event.Event e)      { shutdown(); super.goToDashboard(e); }
    @Override public void goToCheckInOut(javafx.event.Event e)     { shutdown(); super.goToCheckInOut(e); }
    @Override public void goToMonitoringLogs(javafx.event.Event e) { shutdown(); super.goToMonitoringLogs(e); }
    @Override public void goToActiveDevices(javafx.event.Event e)  { shutdown(); super.goToActiveDevices(e); }
    @Override public void goToReports(javafx.event.Event e)        { shutdown(); super.goToReports(e); }
    @Override public void handleLogout(javafx.event.Event e)       { shutdown(); super.handleLogout(e); }
    // --------------------------------------------------------------------

    private void updateDateTime() {
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy  hh:mm a")));
    }

    private void startScanner() {
        btnConfirm.setDisable(true);
        btnConfirm.setText("CONFIRM & LOG ACTIVITY");
        statusLabel.setText("Scanning for QR...");
        statusLabel.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
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
            statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
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
            showAlert(Alert.AlertType.WARNING, "System Warning", "Please enter an Access Code or Student ID.");
            return;
        }
        if (scannerThread != null) scannerThread.stopScanner();
        processScannedCode(txtDeviceSearch.getText().trim());
    }

    private void checkAccessStatus(Connection conn, String studentNumber) throws SQLException {
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
                "d.id as device_id, d.device_brand, d.device_name, d.mac_address, d.unique_code, d.photo_path as device_photo, d.status as device_status " +
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
                    foundDevices.add(new DeviceRecord(
                            rs.getObject("device_id"), rs.getString("device_brand"), rs.getString("device_name"),
                            rs.getString("mac_address"), rs.getString("unique_code"), rs.getString("device_photo"),
                            rs.getString("device_status")
                    ));
                }
            }
        }

        if (fullName.isEmpty()) {
            Platform.runLater(() -> {
                statusLabel.setText("ACCESS DENIED: Unregistered QR");
                statusLabel.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                showAlert(Alert.AlertType.ERROR, "SECURITY ALERT", "The scanned QR code is NOT registered in the system. Access Denied.");
                handleClear();
                startScanner();
            });
            return;
        }

        studentDeviceList = foundDevices;
        final String fName = fullName, fNum = studentNum, fCourse = courseSection, fPhoto = studentPhoto;

        Platform.runLater(() -> {
            lblStudentName.setText(fName);
            lblStudentID.setText(fNum);
            lblCourseSection.setText(fCourse);

            // Photo logic triggers here
            loadImageToView(imgStudentPhoto, fPhoto);

            deviceContainer.getChildren().clear();
            selectedDeviceIds.clear();

            if (isCheckingOut) {
                lblCurrentStatus.setText("Currently INSIDE");
                lblCurrentStatus.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
                cmbAction.getSelectionModel().select("Check-Out");
                for (DeviceRecord d : foundDevices) {
                    if (devicesInsideIds.contains(d.id)) {
                        ToggleButton card = createDeviceCard(d);
                        card.setSelected(true);
                        card.setDisable(true);
                        deviceContainer.getChildren().add(card);
                    }
                }
            } else {
                lblCurrentStatus.setText("Currently OUTSIDE");
                lblCurrentStatus.setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                cmbAction.getSelectionModel().select("Check-In");
                for (DeviceRecord d : foundDevices) {
                    ToggleButton card = createDeviceCard(d);
                    deviceContainer.getChildren().add(card);

                    if (!"COMPROMISED".equalsIgnoreCase(d.status) && !"INACTIVE".equalsIgnoreCase(d.status) && selectedDeviceIds.isEmpty()) {
                        card.setSelected(true);
                    }
                }
            }
            updateSelectionLabels();
            statusLabel.setText("Verification Ready");
            statusLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;");
            btnConfirm.setDisable(false);
        });
    }

    // Phase 2 Fix: Reduced Device Card layout constraints to remove UI clutter
    private ToggleButton createDeviceCard(DeviceRecord device) {
        ToggleButton btn = new ToggleButton();
        btn.setPrefSize(140, 180);
        btn.setMinSize(140, 180);
        btn.setMaxSize(140, 180);

        VBox cardLayout = new VBox(6);
        cardLayout.setAlignment(Pos.CENTER);
        cardLayout.setPadding(new Insets(10));

        StackPane imgContainer = new StackPane();
        imgContainer.setStyle("-fx-border-color: #DDDDDD; -fx-border-radius: 8; -fx-background-color: white; -fx-padding: 5;");
        imgContainer.setMaxSize(80, 80);

        ImageView img = new ImageView();
        img.setFitWidth(70);
        img.setFitHeight(70);
        img.setPreserveRatio(true);
        loadImageToView(img, device.photoPath);
        imgContainer.getChildren().add(img);

        Label lblName = new Label();
        lblName.setWrapText(true);
        lblName.setTextAlignment(TextAlignment.CENTER);
        lblName.setMaxHeight(35);

        Label lblDetails = new Label("Token: " + device.accessCode);
        lblDetails.setStyle("-fx-font-size: 10px; -fx-text-fill: #777777;");

        Label lblCheck = new Label("⚪");
        lblCheck.setStyle("-fx-text-fill: #DDDDDD; -fx-font-size: 14px;");

        cardLayout.getChildren().addAll(imgContainer, lblName, lblDetails, lblCheck);
        btn.setGraphic(cardLayout);
        btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        String defaultStyle = "-fx-background-color: #F9F9F9; -fx-border-color: #DDDDDD; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        String selectedStyle = "-fx-background-color: #FEF0F0; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;";
        String compromisedStyle = "-fx-background-color: #FFCDD2; -fx-border-color: #B71C1C; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;";

        if ("COMPROMISED".equalsIgnoreCase(device.status) || "INACTIVE".equalsIgnoreCase(device.status)) {
            lblName.setText("⚠️ LOCKED\n" + device.brand);
            lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #B71C1C;");
            btn.setStyle(compromisedStyle);
            btn.setDisable(true);
        } else {
            lblName.setText(device.brand + "\n" + device.model);
            lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #222222;");
            btn.setStyle(defaultStyle);

            btn.selectedProperty().addListener((obs, wasSelected, isNowSelected) -> {
                btn.setStyle(isNowSelected ? selectedStyle : defaultStyle);
                lblCheck.setText(isNowSelected ? "🔴" : "⚪");
                lblCheck.setStyle(isNowSelected ? "-fx-text-fill: #500A0E; -fx-font-size: 14px;" : "-fx-text-fill: #DDDDDD; -fx-font-size: 14px;");
                if (isNowSelected) {
                    if (!selectedDeviceIds.contains(device.id)) selectedDeviceIds.add(device.id);
                } else {
                    selectedDeviceIds.remove(device.id);
                }
                updateSelectionLabels();
            });
        }
        return btn;
    }

    // Phase 2 Fix: Asynchronous Cloud Image Handling & Fallbacks
    private void loadImageToView(ImageView imageView, String path) {
        if (imageView == null) return;
        imageView.setImage(null);

        boolean isStudentPhoto = (imageView == imgStudentPhoto);
        String defaultImage = isStudentPhoto ? "/images/default_avatar.png" : "/images/icon-devices.png";

        if (path == null || path.trim().isEmpty() || path.contains("default_")) {
            setFallbackImage(imageView, defaultImage);
            return;
        }

        try {
            if (path.startsWith("http")) {
                Image webImage = new Image(path, true); // true = load asynchronously
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
            if (url != null) {
                imageView.setImage(new Image(url.toExternalForm()));
            } else {
                imageView.setImage(null);
            }
        } catch (Exception e) {
            imageView.setImage(null);
        }
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
                DataStore.getInstance().refreshActiveDevices();

                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.INFORMATION, "Success", (isCheckingOut ? "Check-Out" : "Check-In") + " logged successfully!");
                    handleClear();
                    startScanner();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    btnConfirm.setText("CONFIRM & LOG ACTIVITY");
                    btnConfirm.setDisable(false);
                    deviceContainer.setDisable(false);
                    showAlert(Alert.AlertType.ERROR, "Database Error", "Database error during transaction.");
                });
            }
        }).start();
    }

    @FXML
    private void handleClear() {
        if (scannerThread != null) scannerThread.stopScanner();
        txtDeviceSearch.clear();
        txtRemarks.clear();
        lblStudentName.setText("---");
        lblStudentID.setText("---");
        lblCourseSection.setText("---");
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("BYOD Security Module");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}