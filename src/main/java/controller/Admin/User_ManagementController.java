package controller.Admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class User_ManagementController {

    // Table view components matched to fx:id fields
    @FXML private TableView<User> userManagementTableView;
    @FXML private TableColumn<User, String> colUserIdenticon;
    @FXML private TableColumn<User, String> colUserFullName;
    @FXML private TableColumn<User, String> colUserPrivilegeBadge;
    @FXML private TableColumn<User, String> colUserStateBadge;
    @FXML private TableColumn<User, String> colUserActionControls;
    @FXML private Label entriesSummaryCountLabel;

    // Local data structure list
    private final ObservableList<User> systemUsersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set up cell value properties mapping fields to data models
        colUserIdenticon.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserPrivilegeBadge.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserStateBadge.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserActionControls.setCellValueFactory(new PropertyValueFactory<>("actionPlaceholder"));

        // Populate mock table data records
        loadMockSystemUserData();

        // Bind raw dataset array to the visual layout table structure
        userManagementTableView.setItems(systemUsersList);

        // Update total counter summary label matching mock length
        entriesSummaryCountLabel.setText("Showing 1 to " + systemUsersList.size() + " of " + systemUsersList.size() + " users");
    }

    private void loadMockSystemUserData() {
        systemUsersList.add(new User("admin_cayenne", "Princess Cayenne M. Rañeses", "Administrator", "Active"));
        systemUsersList.add(new User("dan_sosa", "Dan Henry Sosa", "Administrator", "Active"));
        systemUsersList.add(new User("guard_kyle", "Kyle Garcia", "Security Guard", "Active"));
        systemUsersList.add(new User("maria_s", "Maria Santos", "Security Guard", "Inactive"));
    }

    // Top Right Configuration Button Trigger
    @FXML
    void handleLabelAddUser(ActionEvent event) {
        System.out.println("Add User button clicked! Open modal pane here.");
    }

    // --- SIDEBAR SCENE SWITCHING UTILITY WRAPPERS ---
    private void navigateToView(MouseEvent event, String fxmlResourcePath, String targetWindowTitle) {
        try {
            URL targetLayoutUrl = getClass().getResource(fxmlResourcePath);
            if (targetLayoutUrl == null) {
                System.err.println("Navigation Configuration Error: Layout location path missing -> " + fxmlResourcePath);
                return;
            }
            Parent targetRootNode = FXMLLoader.load(targetLayoutUrl);
            Stage windowFrameStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            windowFrameStage.setTitle(targetWindowTitle);
            windowFrameStage.setScene(new Scene(targetRootNode));
            windowFrameStage.show();
        } catch (IOException ex) {
            System.err.println("Critical failure loading view engine stage layout definition mapping targets.");
            ex.printStackTrace();
        }
    }

    @FXML void handleDashboard(MouseEvent event) { navigateToView(event, "/com.example.byod/Admin/dashboard.fxml", "Admin Dashboard"); }
    @FXML void handleStudents(MouseEvent event) { navigateToView(event, "/com.example.byod/Admin/Students.fxml", "Student Records Directory"); }
    @FXML void handleDevices(MouseEvent event) { navigateToView(event, "/com.example.byod/Admin/Devices.fxml", "Device Asset Inventories"); }
    @FXML void handleMonitoringLogs(MouseEvent event) { navigateToView(event, "/com.example.byod/Admin/Monitoring_Logs.fxml", "System Monitoring Logs Dashboard"); }
    @FXML void handleActiveDevices(MouseEvent event) { navigateToView(event, "/com.example.byod/Admin/Active_Devices.fxml", "Active On-Premises Devices"); }
    @FXML void handleReports(MouseEvent event) { navigateToView(event, "/com.example.byod/Admin/Reports.fxml", "System Insight Reports"); }
    @FXML void handleLogout(MouseEvent event) { navigateToView(event, "/com.example.byod/login.fxml", "BYOD System Login Access Screen"); }

    // --- INNER CLASS REPRESENTATION MODEL STRUCT ---
    public static class User {
        private final String username;
        private final String fullName;
        private final String role;
        private final String status;
        private final String actionPlaceholder;

        public User(String username, String fullName, String role, String status) {
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.status = status;
            this.actionPlaceholder = "Edit / Delete";
        }

        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getRole() { return role; }
        public String getStatus() { return status; }
        public String getActionPlaceholder() { return actionPlaceholder; }
    }
}