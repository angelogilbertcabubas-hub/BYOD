package controller.Admin;

import com.example.byod.LogEntry;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

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

        // --- BATCH 2: UI/UX SECURITY ENHANCEMENTS ---

        // 1. Row Highlighting: Turns the entire row faint red if the device is compromised
        monitoringLogsTableView.setRowFactory(tv -> new TableRow<LogEntry>() {
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

        // 2. Cell Badging: Injects the 🚨 emoji and bold red text into the Device column
        colLogDevice.setCellFactory(column -> new TableCell<LogEntry, String>() {
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

        // --- PHASE 3 FIX: Fetching permanent history instead of daily logs ---
        filteredMonitorLogs = new FilteredList<>(DataStore.getInstance().getAllHistoricalLogsList(), p -> true);

        // FIX: Removed the trailing semicolons so the search actually works!
        searchBarField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredMonitorLogs.setPredicate(monitorLogs -> {
                if(newValue == null || newValue.isBlank()) return true;

                String keyword = newValue.toLowerCase();

                if(monitorLogs.getLogId() != null && monitorLogs.getLogId().toLowerCase().contains(keyword)) return true;
                if(monitorLogs.getStudentName() != null && monitorLogs.getStudentName().toLowerCase().contains(keyword)) return true;
                if(monitorLogs.getDeviceModel() != null && monitorLogs.getDeviceModel().toLowerCase().contains(keyword)) return true;
                if(monitorLogs.getAccessToken() != null && monitorLogs.getAccessToken().toLowerCase().contains(keyword)) return true;
                if(monitorLogs.getOperation() != null && monitorLogs.getOperation().toLowerCase().contains(keyword)) return true;
                if(monitorLogs.getTimestamp() != null && monitorLogs.getTimestamp().toLowerCase().contains(keyword)) return true;

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
        // --- PHASE 3 FIX: Update counter to reflect historical log size ---
        int total = DataStore.getInstance().getAllHistoricalLogsList().size();
        int filtered = filteredMonitorLogs.size();
        if(filtered == total){
            statusSummaryLabel.setText("Showing 1 to " + total + " of " + total + " logged streams");
        }else{
            statusSummaryLabel.setText("Showing " + filtered + " matching streams");
        }
    }
}