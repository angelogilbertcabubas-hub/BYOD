package controller.Admin;

import javafx.scene.input.MouseEvent;
import utils.SceneManager;

public abstract class BaseAdminController {

    private final String VIEW_PATH = "/com/example/byod/Admin/";

    public void handleDashboard(MouseEvent event) {
        SceneManager.switchScene(event, VIEW_PATH + "dashboard.fxml");
    }

    public void handleStudents(MouseEvent event) {
        SceneManager.switchScene(event, VIEW_PATH + "Students.fxml");
    }

    public void handleDevices(MouseEvent event) {
        SceneManager.switchScene(event, VIEW_PATH + "Devices.fxml");
    }

    public void handleMonitoringLogs(MouseEvent event) {
        SceneManager.switchScene(event, VIEW_PATH + "Monitoring_Logs.fxml");
    }

    public void handleActiveDevices(MouseEvent event) {
        SceneManager.switchScene(event, VIEW_PATH + "Active_Devices.fxml");
    }

    public void handleReports(MouseEvent event) {
        SceneManager.switchScene(event, VIEW_PATH + "Reports.fxml");
    }

    public void handleUserManagement(MouseEvent event) {
        SceneManager.switchScene(event, VIEW_PATH + "User_Management.fxml");
    }

    public void handleLogout(MouseEvent event) {
        SceneManager.switchScene(event, "/com/example/byod/login.fxml");
    }
}