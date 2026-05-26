package controller.Admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import java.io.IOException;

public class ReportsController {

    @FXML private TableView<ReportModel> reportsTable;
    @FXML private TableColumn<ReportModel, String> colReportID;
    @FXML private TableColumn<ReportModel, String> colTitle;
    @FXML private TableColumn<ReportModel, String> colGeneratedDate;
    @FXML private TableColumn<ReportModel, String> colGeneratedBy;
    @FXML private TableColumn<ReportModel, String> colActions;
    @FXML private TextField txtSearch;
    @FXML private Label statusEntriesLabel;

    private final ObservableList<ReportModel> reportsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colReportID.setCellValueFactory(new PropertyValueFactory<>("reportID"));
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colGeneratedDate.setCellValueFactory(new PropertyValueFactory<>("generatedDate"));
        colGeneratedBy.setCellValueFactory(new PropertyValueFactory<>("generatedBy"));
        colActions.setCellValueFactory(new PropertyValueFactory<>("actions"));

        reportsTable.setItems(reportsList);

        Label placeholder = new Label("No records available.");
        placeholder.setStyle("-fx-text-fill: #888888; -fx-font-style: italic; -fx-font-size: 13px;");
        reportsTable.setPlaceholder(placeholder);

        statusEntriesLabel.setText("Showing 0 to 0 of 0 entries");
    }

    @FXML private void handleDashboard(MouseEvent event) { navigateTo(event, "/com/example/byod/dashboard.fxml"); }
    @FXML private void handleStudents(MouseEvent event) { navigateTo(event, "/com/example/byod/Students.fxml"); }
    @FXML private void handleDevices(MouseEvent event) { navigateTo(event, "/com/example/byod/Devices.fxml"); }
    @FXML private void handleMonitoringLogs(MouseEvent event) { navigateTo(event, "/com/example/byod/Monitoring_Logs.fxml"); }
    @FXML private void handleActiveDevices(MouseEvent event) { navigateTo(event, "/com/example/byod/Active_Devices.fxml"); }
    @FXML private void handleUserManagement(MouseEvent event) { navigateTo(event, "/com/example/byod/User_Management.fxml"); }
    @FXML private void handleLogout(MouseEvent event) { navigateTo(event, "/com/example/byod/login.fxml"); }

    private void navigateTo(MouseEvent event, String resourcePath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(resourcePath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ReportModel {
        private final String reportID, title, generatedDate, generatedBy, actions;
        public ReportModel(String id, String t, String d, String b, String a) {
            this.reportID = id; this.title = t; this.generatedDate = d; this.generatedBy = b; this.actions = a;
        }
        public String getReportID() { return reportID; }
        public String getTitle() { return title; }
        public String getGeneratedDate() { return generatedDate; }
        public String getGeneratedBy() { return generatedBy; }
        public String getActions() { return actions; }
    }
}