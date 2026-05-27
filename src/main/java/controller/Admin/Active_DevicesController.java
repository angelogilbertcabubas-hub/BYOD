package controller.Admin;

import com.example.byod.model.LogEntry;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class Active_DevicesController extends BaseAdminController {

    @FXML private TextField searchBarField;
    @FXML private Label statusSummaryLabel;
    @FXML private TableView<LogEntry> activeDevicesTableView;

    @FXML private TableColumn<LogEntry, String> colStudentName;
    @FXML private TableColumn<LogEntry, String> colStudentID;
    @FXML private TableColumn<LogEntry, String> colDevice;
    @FXML private TableColumn<LogEntry, String> colAccessCode;
    @FXML private TableColumn<LogEntry, String> colTimeIn;
    @FXML private TableColumn<LogEntry, String> colLocation;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colAccessCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colTimeIn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        activeDevicesTableView.setItems(DataStore.getInstance().getActiveDevicesList());

        int count = DataStore.getInstance().getActiveDevicesList().size();
        statusSummaryLabel.setText("Showing 1 to " + count + " of " + count + " active devices inside perimeter bounds");
    }
}