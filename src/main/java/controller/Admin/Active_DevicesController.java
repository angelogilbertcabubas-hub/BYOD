package controller.Admin;

import com.example.byod.LogEntry;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import utils.DataStore;

public class Active_DevicesController extends BaseAdminController {

    @FXML private TableView<LogEntry> activeDevicesTableView;
    @FXML private TableColumn<LogEntry, String> colStudentName;
    @FXML private TableColumn<LogEntry, String> colStudentID;
    @FXML private TableColumn<LogEntry, String> colDevice;
    @FXML private TableColumn<LogEntry, String> colAccessCode;
    @FXML private TableColumn<LogEntry, String> colTimeIn;
    @FXML private TableColumn<LogEntry, String> colLocation;

    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colAccessCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colTimeIn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        activeDevicesTableView.setRowFactory(tv -> new TableRow<LogEntry>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if ("COMPROMISED".equalsIgnoreCase(item.getStatus())) {
                        setStyle("-fx-background-color: #FFEBEE;");
                    } else {
                        setStyle("");
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

        activeDevicesTableView.setItems(DataStore.getInstance().getActiveDevicesList());
        startAutoRefresh();
    }

    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            new Thread(() -> DataStore.getInstance().refreshActiveDevices()).start();
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
    }

    // NAVIGATION OVERRIDES (Now correctly matching the methods in BaseAdminController)
    @Override public void handleDashboard(MouseEvent e)      { stopAutoRefresh(); super.handleDashboard(e); }
    @Override public void handleStudents(MouseEvent e)       { stopAutoRefresh(); super.handleStudents(e); }
    @Override public void handleDevices(MouseEvent e)        { stopAutoRefresh(); super.handleDevices(e); }
    @Override public void handleMonitoringLogs(MouseEvent e)  { stopAutoRefresh(); super.handleMonitoringLogs(e); }
    @Override public void handleActiveDevices(MouseEvent e)   { stopAutoRefresh(); super.handleActiveDevices(e); }
    @Override public void handleReports(MouseEvent e)         { stopAutoRefresh(); super.handleReports(e); }
    @Override public void handleUserManagement(MouseEvent e)  { stopAutoRefresh(); super.handleUserManagement(e); }
    @Override public void handleLogout(MouseEvent e)          { stopAutoRefresh(); super.handleLogout(e); }
}