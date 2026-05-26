package controller.Admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.io.IOException;

public class DashboardController {

    @FXML private Label lblTotalStudents;
    @FXML private Label lblRegisteredDevices;
    @FXML private Label lblDevicesInside;
    @FXML private Label lblTodayLogs;

    @FXML private AreaChart<String, Number> ingressEgressChart;
    @FXML private CategoryAxis xAxisChart;
    @FXML private NumberAxis yAxisChart;
    @FXML private VBox chartPlaceholder;

    @FXML private TableView<Object> miniLogsTable;

    @FXML
    public void initialize() {
        // Initialize dashboard data
        loadDashboardData();

        // Setup chart placeholder
        chartPlaceholder.setVisible(true);
        ingressEgressChart.setAnimated(false);

        // Setup sample data for demonstration
        setupSampleData();
    }

    private void loadDashboardData() {
        // TODO: Replace with actual database queries
        lblTotalStudents.setText("156");
        lblRegisteredDevices.setText("203");
        lblDevicesInside.setText("89");
        lblTodayLogs.setText("47");
    }

    private void setupSampleData() {
        // Add sample data to the chart if needed
        // This will be populated from your actual data source
    }

    @FXML
    private void handleStudents(MouseEvent event) {
        navigateTo(event, "/view/Students.fxml");
    }

    @FXML
    private void handleDevices(MouseEvent event) {
        navigateTo(event, "/view/Devices.fxml");
    }

    @FXML
    private void handleMonitoringLogs(MouseEvent event) {
        navigateTo(event, "/view/Monitoring_Logs.fxml");
    }

    @FXML
    private void handleActiveDevices(MouseEvent event) {
        navigateTo(event, "/view/Active_Devices.fxml");
    }

    @FXML
    private void handleReports(MouseEvent event) {
        navigateTo(event, "/view/Reports.fxml");
    }

    @FXML
    private void handleUserManagement(MouseEvent event) {
        navigateTo(event, "/view/User_Management.fxml");
    }

    @FXML
    private void handleLogout(MouseEvent event) {
        try {
            // Load the login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent loginRoot = loader.load();

            // Get the current stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Set the login scene
            Scene loginScene = new Scene(loginRoot);
            stage.setScene(loginScene);
            stage.setTitle("BYOD Registration & Monitoring System - Login");
            stage.show();

        } catch (IOException e) {
            System.err.println("CRITICAL NAVIGATION ERROR: Unable to load login page");
            e.printStackTrace();
        }
    }

    private void navigateTo(MouseEvent event, String fxmlPath) {
        try {
            // Load the FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Get the current stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Set the new scene
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.err.println("CRITICAL NAVIGATION ERROR: Unable to load: " + fxmlPath);
            e.printStackTrace();
        }
    }
}