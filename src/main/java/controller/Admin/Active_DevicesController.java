package controller.Admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;

public class Active_DevicesController {

    @FXML private TextField searchBarField;
    @FXML private Button filterBtn;
    @FXML private Label statusSummaryLabel;

    @FXML private TableView<Object> activeDevicesTableView;
    @FXML private TableColumn<Object, String> colStudentName;
    @FXML private TableColumn<Object, String> colStudentID;
    @FXML private TableColumn<Object, String> colDevice;
    @FXML private TableColumn<Object, String> colAccessCode;
    @FXML private TableColumn<Object, String> colTimeIn;
    @FXML private TableColumn<Object, String> colLocation;

    @FXML
    public void initialize() {
        activeDevicesTableView.setItems(FXCollections.observableArrayList());
        statusSummaryLabel.setText("Showing 0 to 0 of 0 active devices inside perimeter bounds");
    }

    @FXML private void handleDashboard(MouseEvent event) { System.out.println("Navigating to Dashboard..."); }
    @FXML private void handleStudents(MouseEvent event) { System.out.println("Navigating to Students..."); }
    @FXML private void handleDevices(MouseEvent event) { System.out.println("Navigating to Devices..."); }
    @FXML private void handleMonitoringLogs(MouseEvent event) { System.out.println("Navigating to Logs..."); }
    @FXML private void handleActiveDevices(MouseEvent event) { /* Currently Here */ }
    @FXML private void handleReports(MouseEvent event) { System.out.println("Navigating to Reports..."); }
    @FXML private void handleUserManagement(MouseEvent event) { System.out.println("Navigating to Users..."); }
    @FXML private void handleLogout(MouseEvent event) { System.out.println("Logging out..."); }
}