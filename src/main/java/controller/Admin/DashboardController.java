package controller.Admin;

import com.example.byod.model.LogEntry;
import utils.DataStore;
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
        loadDashboardData();

        chartPlaceholder.setVisible(true);
        ingressEgressChart.setAnimated(false);

        colMiniName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colMiniAction.setCellValueFactory(new PropertyValueFactory<>("operation"));
        colMiniTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        miniLogsTable.setItems(DataStore.getInstance().getMonitoringLogsList());

        setupSampleData();
    }

    private void loadDashboardData() {
        lblTotalStudents.setText(String.valueOf(DataStore.getInstance().getStudentsList().size()));
        lblRegisteredDevices.setText(String.valueOf(DataStore.getInstance().getDevicesList().size()));
        lblDevicesInside.setText(String.valueOf(DataStore.getInstance().getActiveDevicesList().size()));
        lblTodayLogs.setText(String.valueOf(DataStore.getInstance().getMonitoringLogsList().size()));
    }

    private void setupSampleData() {
    }
}