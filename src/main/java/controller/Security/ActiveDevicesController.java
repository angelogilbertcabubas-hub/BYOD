package controller.Security;

import com.example.byod.LogEntry;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import utils.DataStore;

public class ActiveDevicesController extends BaseSecurityController {

    @FXML private TextField txtSearch;
    @FXML private TableView<LogEntry> activeDevicesTable;
    @FXML private TableColumn<LogEntry, String> colStudentName;
    @FXML private TableColumn<LogEntry, String> colStudentID;
    @FXML private TableColumn<LogEntry, String> colDevice;
    @FXML private TableColumn<LogEntry, String> colAccessCode;
    @FXML private TableColumn<LogEntry, String> colTimeIn;
    @FXML private TableColumn<LogEntry, String> colLocation;

    @FXML private Label lblTotalCount;
    @FXML private Button btnPage1;
    @FXML private Button btnPage2;
    @FXML private Button btnPage3;

    private int currentPage = 1;
    private static final int TOTAL_PAGES = 3;

    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colAccessCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colTimeIn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        // --- BATCH 3: UI/UX SECURITY ENHANCEMENTS ---

        activeDevicesTable.setRowFactory(tv -> new TableRow<LogEntry>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if ("COMPROMISED".equalsIgnoreCase(item.getStatus())) {
                        setStyle("-fx-background-color: #FFEBEE;"); // Faint red alert background
                    } else {
                        setStyle(""); // Default background
                    }
                }
            }
        });

        colDevice.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    LogEntry log = getTableRow().getItem();
                    if ("COMPROMISED".equalsIgnoreCase(log.getStatus())) {
                        setText("🚨 [LOCKED] " + item);
                        setStyle("-fx-text-fill: #B71C1C; -fx-font-weight: bold;");
                    } else {
                        setText("🟢 " + item);
                        setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // --------------------------------------------

        activeDevicesTable.setItems(DataStore.getInstance().getActiveDevicesList());
        updateTotalCount();

        // BUG 3 FIX: Start the auto-refresh loop (runs every 5 seconds)
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            new Thread(() -> {
                DataStore.getInstance().refreshActiveDevices();
                Platform.runLater(this::updateTotalCount);
            }).start();
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }

    private void updateTotalCount() {
        if (lblTotalCount != null) {
            lblTotalCount.setText(String.valueOf(DataStore.getInstance().getActiveDevicesList().size()));
        }
    }

    // Intercept sidebar navigation to stop the background loop
    @FXML @Override public void goToDashboard()      { stopAutoRefresh(); super.goToDashboard(); }
    @FXML @Override public void goToCheckInOut()     { stopAutoRefresh(); super.goToCheckInOut(); }
    @FXML @Override public void goToMonitoringLogs() { stopAutoRefresh(); super.goToMonitoringLogs(); }
    @FXML @Override public void goToActiveDevices()  { stopAutoRefresh(); super.goToActiveDevices(); }
    @FXML @Override public void goToReports()        { stopAutoRefresh(); super.goToReports(); }
    @FXML @Override public void handleLogout()       { stopAutoRefresh(); super.handleLogout(); }

    @Override public void goToDashboard(javafx.event.Event e)      { stopAutoRefresh(); super.goToDashboard(e); }
    @Override public void goToCheckInOut(javafx.event.Event e)     { stopAutoRefresh(); super.goToCheckInOut(e); }
    @Override public void goToMonitoringLogs(javafx.event.Event e) { stopAutoRefresh(); super.goToMonitoringLogs(e); }
    @Override public void goToActiveDevices(javafx.event.Event e)  { stopAutoRefresh(); super.goToActiveDevices(e); }
    @Override public void goToReports(javafx.event.Event e)        { stopAutoRefresh(); super.goToReports(e); }
    @Override public void handleLogout(javafx.event.Event e)       { stopAutoRefresh(); super.handleLogout(e); }

    @FXML private void handleSearch() { System.out.println("Searching: " + txtSearch.getText()); }
    @FXML private void handleFilter() { showAlert("Filter logic processing..."); }

    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; updateActivePage(); } }
    @FXML private void handleNextPage() { if (currentPage < TOTAL_PAGES) { currentPage++; updateActivePage(); } }
    @FXML private void handlePage1() { currentPage = 1; updateActivePage(); }
    @FXML private void handlePage2() { currentPage = 2; updateActivePage(); }
    @FXML private void handlePage3() { currentPage = 3; updateActivePage(); }

    private void updateActivePage() {
        String a = "-fx-background-color: #7B0D0D; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;";
        String i = "-fx-background-color: white; -fx-text-fill: #555555; -fx-font-size: 13px; -fx-background-radius: 6; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-border-width: 1.2; -fx-padding: 4 10; -fx-cursor: hand;";
        btnPage1.setStyle(currentPage == 1 ? a : i);
        btnPage2.setStyle(currentPage == 2 ? a : i);
        btnPage3.setStyle(currentPage == 3 ? a : i);
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("BYOD Security"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}