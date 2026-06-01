package controller.Admin;

import com.example.byod.LogEntry;
import utils.DataStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class DashboardController extends BaseAdminController {

    @FXML private Label lblTotalStudents;
    @FXML private Label lblRegisteredDevices;
    @FXML private Label lblDevicesInside;
    @FXML private Label lblTodayLogs;

    @FXML private AreaChart<String, Number> ingressEgressChart;
    @FXML private CategoryAxis xAxisChart;
    @FXML private NumberAxis yAxisChart;
    @FXML private VBox chartPlaceholder;

    @FXML private TableView<LogEntry> miniLogsTable;
    @FXML private TableColumn<LogEntry, String> colMiniName;
    @FXML private TableColumn<LogEntry, String> colMiniAction;
    @FXML private TableColumn<LogEntry, String> colMiniTime;

    @FXML
    public void initialize() {
        lblTotalStudents.setText("Loading...");
        lblRegisteredDevices.setText("Loading...");
        lblDevicesInside.setText("Loading...");
        lblTodayLogs.setText("Loading...");

        chartPlaceholder.setVisible(true);
        ingressEgressChart.setAnimated(false);

        colMiniName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colMiniAction.setCellValueFactory(new PropertyValueFactory<>("operation"));
        colMiniTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        Thread dataLoadThread = new Thread(() -> {
            DataStore store = DataStore.getInstance();

            Platform.runLater(() -> {
                lblTotalStudents.setText(String.valueOf(store.getStudentsList().size()));
                lblRegisteredDevices.setText(String.valueOf(store.getDevicesList().size()));
                lblDevicesInside.setText(String.valueOf(store.getActiveDevicesList().size()));
                lblTodayLogs.setText(String.valueOf(store.getMonitoringLogsList().size()));
                miniLogsTable.setItems(store.getMonitoringLogsList());
            });
        });

        dataLoadThread.setDaemon(true);
        dataLoadThread.start();

        setupSampleData();
    }

    private void setupSampleData() {
    }
}