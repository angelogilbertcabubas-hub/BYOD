package controller.Admin;

import com.example.byod.model.LogEntry;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class Monitoring_LogsController extends BaseAdminController {

    @FXML private TextField searchBarField;
    @FXML private Label statusSummaryLabel;
    @FXML private TableView<LogEntry> monitoringLogsTableView;

    @FXML private TableColumn<LogEntry, String> colLogID;
    @FXML private TableColumn<LogEntry, String> colLogName;
    @FXML private TableColumn<LogEntry, String> colLogDevice;
    @FXML private TableColumn<LogEntry, String> colLogCode;
    @FXML private TableColumn<LogEntry, String> colLogType;
    @FXML private TableColumn<LogEntry, String> colLogTimestamp;

    @FXML
    public void initialize() {
        colLogID.setCellValueFactory(new PropertyValueFactory<>("logId"));
        colLogName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colLogDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colLogCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colLogType.setCellValueFactory(new PropertyValueFactory<>("operation"));
        colLogTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        monitoringLogsTableView.setItems(DataStore.getInstance().getMonitoringLogsList());

        int count = DataStore.getInstance().getMonitoringLogsList().size();
        statusSummaryLabel.setText("Showing 1 to " + count + " of " + count + " streams data logged");
    }
}