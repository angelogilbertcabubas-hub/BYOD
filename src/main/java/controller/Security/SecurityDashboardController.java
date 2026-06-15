package controller.Security;

import com.example.byod.model.Device;
import utils.DataStore;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

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

        List<Device> initialMatches = DataStore.getInstance().getDevicesList().stream()
                .filter(d -> (d.getStudentNumber() != null && d.getStudentNumber().toLowerCase().contains(query)) ||
                        (d.getOwnerName() != null && d.getOwnerName().toLowerCase().contains(query)) ||
                        (d.getMacAddress() != null && d.getMacAddress().toLowerCase().contains(query)))
                .collect(Collectors.toList());

        if (initialMatches.isEmpty()) {
            lblResultName.setText("UNREGISTERED / NOT FOUND");
            lblResultId.setText("No match for: '" + txtSearch.getText() + "'");

            lblSystemStatus.setText("🚨 ENTRY DENIED");
            lblSystemStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");

            // Safe fallback if student doesn't exist
            loadStudentPhoto(null);

            Label noDevice = new Label("Direct the student to the Admin Office for registration.");
            noDevice.setStyle("-fx-text-fill: #777777; -fx-font-style: italic;");
            deviceListContainer.getChildren().add(noDevice);

            txtSearch.clear();
            return;
        }

        Device firstMatch = initialMatches.get(0);
        String targetId = firstMatch.getStudentNumber();
        String targetName = firstMatch.getOwnerName();

        // Dynamically load the correct student photo from Supabase
        loadStudentPhoto(targetId);

        List<Device> allStudentDevices = DataStore.getInstance().getDevicesList().stream()
                .filter(d -> (targetId != null && targetId.equals(d.getStudentNumber())) ||
                        (targetName != null && targetName.equals(d.getOwnerName())))
                .collect(Collectors.toList());

        lblResultName.setText(targetName != null ? targetName : "Unknown Student");
        lblResultId.setText("Student ID: " + (targetId != null ? targetId : "N/A"));

        boolean hasCompromised = false;

        for (Device d : allStudentDevices) {
            String status = d.getStatus() != null ? d.getStatus().toUpperCase() : "UNKNOWN";
            boolean isClear = !(status.equals("COMPROMISED") || status.equals("ARCHIVED") || status.equals("INACTIVE"));
            if (!isClear) hasCompromised = true;

            String badgeColor = isClear ? "#2E7D32" : "#C62828";
            String badgeBg = isClear ? "#E8F5E9" : "#FFEBEE";

            Label lblDeviceName = new Label("💻 " + d.getBrandModel());
            lblDeviceName.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333; -fx-font-size: 14px;");

            Label lblMac = new Label(d.getMacAddress());
            lblMac.setStyle("-fx-text-fill: #888888; -fx-font-size: 11px;");

            VBox deviceInfo = new VBox(2, lblDeviceName, lblMac);

            Label lblDeviceStatus = new Label(isClear ? "🟢 " + status : "🚨 " + status);
            lblDeviceStatus.setStyle("-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeColor + "; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-size: 11px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox deviceRow = new HBox(10, deviceInfo, spacer, lblDeviceStatus);
            deviceRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            deviceRow.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #EEEEEE; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 10;");

            deviceListContainer.getChildren().add(deviceRow);
        }

        if (hasCompromised) {
            lblSystemStatus.setText("🚨 SECURITY HOLD");
            lblSystemStatus.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");
        } else {
            lblSystemStatus.setText("🟢 VERIFIED CLEAR");
            lblSystemStatus.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 6;");
        }

        txtSearch.clear();
    }

    // --- SMART SUPABASE PHOTO LOADING LOGIC ---
    private void loadStudentPhoto(String studentId) {
        // ALWAYS clear previous photo to prevent ghosting
        imgStudentPhoto.setImage(null);

        if (studentId == null || studentId.trim().isEmpty()) {
            setDefaultAvatar();
            return;
        }

        try {
            // IMPORTANT: Replace these with your actual Supabase details!
            String supabaseProjectUrl = "https://YOUR_PROJECT_REF.supabase.co";
            String bucketName = "YOUR_BUCKET_NAME";

            // Constructs the public URL for the image
            String photoUrl = supabaseProjectUrl + "/storage/v1/object/public/" + bucketName + "/" + studentId + ".png";

            // Create the image (The 'true' means load in background so app doesn't freeze)
            Image webImage = new Image(photoUrl, true);

            // If Supabase returns an error, fallback to the default avatar
            webImage.errorProperty().addListener((observable, oldValue, isError) -> {
                if (isError) {
                    setDefaultAvatar();
                }
            });

            imgStudentPhoto.setImage(webImage);

        } catch (Exception e) {
            setDefaultAvatar();
        }
    }

    private void setDefaultAvatar() {
        try {
            // Look for a proper blank silhouette image named "default_avatar.png"
            java.net.URL defaultUrl = getClass().getResource("/images/default_avatar.png");

            if (defaultUrl != null) {
                imgStudentPhoto.setImage(new Image(defaultUrl.toExternalForm()));
            } else {
                imgStudentPhoto.setImage(null);
            }
        } catch (Exception e) {
            imgStudentPhoto.setImage(null);
        }
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