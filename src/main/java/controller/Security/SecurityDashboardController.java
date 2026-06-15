package controller.Security;

import com.example.byod.model.Device;
import utils.DataStore;
import utils.DatabaseHelper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SecurityDashboardController extends BaseSecurityController {

    @FXML private Label lblDevicesCount;
    @FXML private Label lblCheckInCount;
    @FXML private Label lblCheckOutCount;

    @FXML private TextField txtSearch;
    @FXML private Button btnVerify;

    // UI Panel Injections
    @FXML private VBox verificationResultsPanel;
    @FXML private Label lblResultName;
    @FXML private Label lblResultId;
    @FXML private Label lblSystemStatus;
    @FXML private VBox deviceListContainer;
    @FXML private ImageView imgStudentPhoto;

    // Helper class to store device info during the database fetch
    private static class DeviceData {
        String name, mac, status, photoPath;
        DeviceData(String name, String mac, String status, String photoPath) {
            this.name = name; this.mac = mac; this.status = status; this.photoPath = photoPath;
        }
    }

    @FXML
    public void initialize() {
        lblDevicesCount.setText("...");
        lblCheckInCount.setText("...");
        lblCheckOutCount.setText("...");

        Thread dataLoadThread = new Thread(() -> {
            DataStore store = DataStore.getInstance();

            int activeDevices = store.getActiveDevicesList().size();
            long checkIns = store.getMonitoringLogsList().stream()
                    .filter(log -> "Check-In".equals(log.getOperation()))
                    .count();
            long checkOuts = store.getMonitoringLogsList().stream()
                    .filter(log -> "Check-Out".equals(log.getOperation()))
                    .count();

            Platform.runLater(() -> {
                lblDevicesCount.setText(String.valueOf(activeDevices));
                lblCheckInCount.setText(String.valueOf(checkIns));
                lblCheckOutCount.setText(String.valueOf(checkOuts));
            });
        });

        dataLoadThread.setDaemon(true);
        dataLoadThread.start();
    }

    @FXML
    private void handleVerify(ActionEvent event) {
        String query = txtSearch.getText().trim().toLowerCase();

        if (query.isEmpty()) {
            verificationResultsPanel.setVisible(false);
            verificationResultsPanel.setManaged(false);
            showAlert(Alert.AlertType.WARNING, "Input Required", "Please enter a Student ID, Name, or MAC Address.");
            return;
        }

        verificationResultsPanel.setVisible(true);
        verificationResultsPanel.setManaged(true);
        deviceListContainer.getChildren().clear();

        lblSystemStatus.setText("FETCHING DATA...");
        lblSystemStatus.setStyle("-fx-background-color: #E2DDD9; -fx-text-fill: #333333; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");

        // Execute live DB Query to fetch exact photos and devices
        new Thread(() -> {
            String sql = "SELECT s.school_id, s.first_name, s.last_name, s.photo_path AS student_photo, " +
                    "d.device_brand, d.device_name, d.mac_address, d.unique_code, d.status AS device_status, d.photo_path AS device_photo " +
                    "FROM students s " +
                    "LEFT JOIN devices d ON s.id = d.student_id " +
                    "WHERE LOWER(s.school_id) = ? OR LOWER(d.mac_address) = ? OR LOWER(d.unique_code) = ? " +
                    "OR LOWER(s.first_name || ' ' || s.last_name) LIKE ?";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, query);
                pstmt.setString(2, query);
                pstmt.setString(3, query);
                pstmt.setString(4, "%" + query + "%");

                ResultSet rs = pstmt.executeQuery();

                String studentName = null;
                String studentId = null;
                String studentPhoto = null;
                List<DeviceData> devices = new ArrayList<>();

                while (rs.next()) {
                    if (studentName == null) {
                        studentName = rs.getString("first_name") + " " + rs.getString("last_name");
                        studentId = rs.getString("school_id");
                        studentPhoto = rs.getString("student_photo");
                    }
                    if (rs.getString("unique_code") != null) {
                        devices.add(new DeviceData(
                                rs.getString("device_brand") + " " + rs.getString("device_name"),
                                rs.getString("mac_address"),
                                rs.getString("device_status"),
                                rs.getString("device_photo")
                        ));
                    }
                }

                final String fStudentName = studentName;
                final String fStudentId = studentId;
                final String fStudentPhoto = studentPhoto;
                final List<DeviceData> fDevices = devices;

                Platform.runLater(() -> {
                    if (fStudentName == null) {
                        lblResultName.setText("UNREGISTERED / NOT FOUND");
                        lblResultId.setText("No match for: '" + txtSearch.getText() + "'");
                        lblSystemStatus.setText("🚨 ENTRY DENIED");
                        lblSystemStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");

                        loadImageToView(imgStudentPhoto, null);

                        Label noDevice = new Label("Direct the student to the Admin Office for registration.");
                        noDevice.setStyle("-fx-text-fill: #777777; -fx-font-style: italic;");
                        deviceListContainer.getChildren().add(noDevice);
                    } else {
                        lblResultName.setText(fStudentName);
                        lblResultId.setText("Student ID: " + fStudentId);

                        // Load actual student photo
                        loadImageToView(imgStudentPhoto, fStudentPhoto);

                        boolean hasCompromised = false;

                        for (DeviceData d : fDevices) {
                            String status = d.status != null ? d.status.toUpperCase() : "UNKNOWN";
                            boolean isClear = !(status.equals("COMPROMISED") || status.equals("ARCHIVED") || status.equals("INACTIVE"));
                            if (!isClear) hasCompromised = true;

                            String badgeColor = isClear ? "#2E7D32" : "#C62828";
                            String badgeBg = isClear ? "#E8F5E9" : "#FFEBEE";

                            // Inject Actual Device Image
                            StackPane devImgContainer = new StackPane();
                            devImgContainer.setStyle("-fx-border-color: #DDDDDD; -fx-border-radius: 6; -fx-background-color: white; -fx-padding: 3;");
                            ImageView devImg = new ImageView();
                            devImg.setFitWidth(40);
                            devImg.setFitHeight(40);
                            devImg.setPreserveRatio(true);
                            loadImageToView(devImg, d.photoPath);
                            devImgContainer.getChildren().add(devImg);

                            Label lblDeviceName = new Label("💻 " + d.name);
                            lblDeviceName.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333; -fx-font-size: 14px;");

                            Label lblMac = new Label(d.mac);
                            lblMac.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

                            VBox deviceInfo = new VBox(2, lblDeviceName, lblMac);

                            Label lblDeviceStatus = new Label(isClear ? "✅ " + status : "🚨 " + status);
                            lblDeviceStatus.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeColor + "; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-size: 11px;");

                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);

                            HBox deviceRow = new HBox(12, devImgContainer, deviceInfo, spacer, lblDeviceStatus);
                            deviceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            deviceRow.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #EEEEEE; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");

                            deviceListContainer.getChildren().add(deviceRow);
                        }

                        if (fDevices.isEmpty()) {
                            Label noDevice = new Label("No registered devices found for this student.");
                            noDevice.setStyle("-fx-text-fill: #777777; -fx-font-style: italic;");
                            deviceListContainer.getChildren().add(noDevice);
                        }

                        if (hasCompromised) {
                            lblSystemStatus.setText("🚨 SECURITY HOLD");
                            lblSystemStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");
                        } else {
                            lblSystemStatus.setText("✅ VERIFIED CLEAR");
                            lblSystemStatus.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblSystemStatus.setText("DATABASE ERROR");
                    lblSystemStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");
                });
            }
        }).start();

        txtSearch.clear();
    }

    // Unified Image Loader for both Cloud (http) and Local photos
    private void loadImageToView(ImageView imageView, String path) {
        if (imageView == null) return;
        imageView.setImage(null);

        if (path == null || path.isEmpty() || path.contains("default_")) {
            // Apply default avatar if it's the main student profile picture
            if (imageView == imgStudentPhoto) {
                try {
                    java.net.URL defaultUrl = getClass().getResource("/images/default_avatar.png");
                    if (defaultUrl != null) {
                        imageView.setImage(new Image(defaultUrl.toExternalForm()));
                    }
                } catch (Exception ignored) {}
            }
            return;
        }

        try {
            if (path.startsWith("http")) {
                imageView.setImage(new Image(path, true)); // 'true' enables background loading
            } else {
                File imgFile = new File("src/main/resources/" + path);
                if (imgFile.exists()) {
                    imageView.setImage(new Image(imgFile.toURI().toString()));
                }
            }
        } catch (Exception ignored) {}
    }


    // --- NAVIGATION ---

    @FXML
    private void handleQuickCheckIn(ActionEvent event) { goToCheckInOut(); }

    @FXML
    private void handleQuickCheckOut(ActionEvent event) { goToCheckInOut(); }

    @FXML
    private void handleReportIncident(ActionEvent event) {
        try {
            java.net.URL resource = getClass().getResource("/com/example/byod/Security/ReportIncident.fxml");

            if (resource == null) {
                showAlert(Alert.AlertType.ERROR, "System Error", "Could not find ReportIncident.fxml. Please rebuild the project.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Stage incidentStage = new Stage();
            incidentStage.setTitle("Report Security Incident");
            incidentStage.setScene(new Scene(root));
            incidentStage.initModality(Modality.APPLICATION_MODAL);
            incidentStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Loading Error", "An error occurred while opening the Incident Report.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}