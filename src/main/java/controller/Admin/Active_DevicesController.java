package controller.Admin;

import com.example.byod.LogEntry;
import com.example.byod.model.Device;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;

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

    private FilteredList<LogEntry> filteredActiveDevices;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colAccessCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colTimeIn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        filteredActiveDevices = new FilteredList<>(DataStore.getInstance().getActiveDevicesList(), p -> true);

        searchBarField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredActiveDevices.setPredicate(activeDevice -> {
                if (newValue == null || newValue.isBlank()) return true;

                String keyword = newValue.toLowerCase();

                if(activeDevice.getStudentName() != null && activeDevice.getStudentName().toLowerCase().contains(keyword));
                if(activeDevice.getStudentId() != null && activeDevice.getStudentId().toLowerCase().contains(keyword));
                if(activeDevice.getDeviceModel() != null && activeDevice.getDeviceModel().toLowerCase().contains(keyword));
                if(activeDevice.getAccessToken() != null && activeDevice.getAccessToken().toLowerCase().contains(keyword));
                if(activeDevice.getTimestamp() != null && activeDevice.getTimestamp().toLowerCase().contains(keyword));
                if(activeDevice.getLocation() != null && activeDevice.getLocation().toLowerCase().contains(keyword));

                return false;

            });

            updateCountLabel();
        });

        SortedList<LogEntry> sortedActiveDevices = new SortedList<>(filteredActiveDevices);
        sortedActiveDevices.comparatorProperty().bind(activeDevicesTableView.comparatorProperty());
        activeDevicesTableView.setItems(sortedActiveDevices);
    }

    private void updateCountLabel() {
        int total = DataStore.getInstance().getActiveDevicesList().size();
        int filtered = filteredActiveDevices.size();
        if (filtered == total) {
            statusSummaryLabel.setText("Showing 1 to " + total + " of " + total + " entries");
        } else {
            statusSummaryLabel.setText("Showing " + filtered + " of " + total + " entries");
        }
    }
}