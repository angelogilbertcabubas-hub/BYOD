package controller.Admin;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.event.ActionEvent;

public class StudentsController {

    @FXML private TextField searchBarField;
    @FXML private Label statusSummaryLabel;
    @FXML private TableView<Object> studentsTableView;

    @FXML
    public void initialize() {
        studentsTableView.setItems(FXCollections.observableArrayList());
        statusSummaryLabel.setText("Showing 0 to 0 of 0 entries");
    }

    @FXML private void handleAddStudent(ActionEvent event) { System.out.println("Opening Student Registration Dialog Window..."); }
    @FXML private void handleDashboard(MouseEvent event) { System.out.println("Navigating to Dashboard..."); }
    @FXML private void handleDevices(MouseEvent event) { System.out.println("Navigating to Devices..."); }
    @FXML private void handleMonitoringLogs(MouseEvent event) { System.out.println("Navigating to Logs..."); }
    @FXML private void handleActiveDevices(MouseEvent event) { System.out.println("Navigating to Active Devices..."); }
    @FXML private void handleReports(MouseEvent event) { System.out.println("Navigating to Reports..."); }
    @FXML private void handleUserManagement(MouseEvent event) { System.out.println("Navigating to Users..."); }
    @FXML private void handleLogout(MouseEvent event) { System.out.println("Logging out..."); }
}