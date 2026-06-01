package controller.Admin;

import com.example.byod.LogEntry;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;

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

    private FilteredList<LogEntry> filteredMonitorLogs;

    @FXML
    public void initialize() {
        colLogID.setCellValueFactory(new PropertyValueFactory<>("logId"));
        colLogName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colLogDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colLogCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colLogType.setCellValueFactory(new PropertyValueFactory<>("operation"));
        colLogTimestamp.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        filteredMonitorLogs = new FilteredList<>(DataStore.getInstance().getMonitoringLogsList(), p -> true);

        searchBarField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMonitorLogs.setPredicate(monitorLogs -> {
                if(newValue == null || newValue.isBlank()) return true;

                String keyword = newValue.toLowerCase();

                if(monitorLogs.getLogId() != null && monitorLogs.getLogId().toLowerCase().contains(keyword));
                if(monitorLogs.getStudentName() != null && monitorLogs.getStudentName().toLowerCase().contains(keyword));
                if(monitorLogs.getDeviceModel() != null && monitorLogs.getDeviceModel().toLowerCase().contains(keyword));
                if(monitorLogs.getAccessToken() != null && monitorLogs.getAccessToken().toLowerCase().contains(keyword));
                if(monitorLogs.getOperation() != null && monitorLogs.getOperation().toLowerCase().contains(keyword));
                if(monitorLogs.getTimestamp() != null && monitorLogs.getTimestamp().toLowerCase().contains(keyword));

                return false;
            });

            updateCountLabel();
        });

        SortedList<LogEntry> sortedMonitorLogs = new SortedList<>(filteredMonitorLogs);
        sortedMonitorLogs.comparatorProperty().bind(monitoringLogsTableView.comparatorProperty());
        monitoringLogsTableView.setItems(sortedMonitorLogs);

        updateCountLabel();
    }

    private void updateCountLabel(){
        int total = DataStore.getInstance().getMonitoringLogsList().size();
        int filtered = filteredMonitorLogs.size();
        if(filtered == total){
            statusSummaryLabel.setText("Showing 1" +
                    total + " of " + total + "streams data logged");
        }else{
            statusSummaryLabel.setText("Showing" + filtered + " of " +
                    total + "streams data logged");
        }
    }
}