package controller.Security;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

public abstract class BaseSecurityController {

    // --- UNIVERSAL NO-ARGUMENT HANDLERS ---
    // JavaFX flawlessly binds to these without caring if you clicked a Mouse or a Button!
    @FXML public void goToDashboard()      { navigate("/com/example/byod/Security/SecurityDashboard.fxml"); }
    @FXML public void goToCheckInOut()     { navigate("/com/example/byod/Security/CheckInOut.fxml"); }
    @FXML public void goToMonitoringLogs() { navigate("/com/example/byod/Security/MonitoringLogs.fxml"); }
    @FXML public void goToActiveDevices()  { navigate("/com/example/byod/Security/ActiveDevices.fxml"); }
    @FXML public void goToReports()        { navigate("/com/example/byod/Security/Reports.fxml"); }
    @FXML public void handleLogout()       { navigate("/com/example/byod/login.fxml"); }

    // --- OVERLOADS FOR INTERNAL CONTROLLER CALLS ---
    // Safely handles the Dashboard's "Quick Action" buttons
    public void goToDashboard(javafx.event.Event e)      { goToDashboard(); }
    public void goToCheckInOut(javafx.event.Event e)     { goToCheckInOut(); }
    public void goToMonitoringLogs(javafx.event.Event e) { goToMonitoringLogs(); }
    public void goToActiveDevices(javafx.event.Event e)  { goToActiveDevices(); }
    public void goToReports(javafx.event.Event e)        { goToReports(); }
    public void handleLogout(javafx.event.Event e)       { handleLogout(); }

    // --- THE MASTER NAVIGATOR ---
    protected void navigate(String fxmlPath) {
        try {
            java.net.URL resource = getClass().getResource(fxmlPath);

            // Safety Check
            if (resource == null) {
                System.err.println("CRITICAL ERROR: Could not find FXML file at path: " + fxmlPath);
                return;
            }

            Parent root = FXMLLoader.load(resource);

            // Automatically grab the currently active application window instead of relying on the click event!
            Stage stage = null;
            for (Window window : Window.getWindows()) {
                if (window.isFocused()) {
                    stage = (Stage) window;
                    break;
                }
            }

            // Fallback if no window is explicitly focused
            if (stage == null && !Window.getWindows().isEmpty()) {
                stage = (Stage) Window.getWindows().get(0);
            }

            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.show();
            } else {
                System.err.println("ERROR: Could not find an active window to change the scene.");
            }
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Failed to navigate to " + fxmlPath);
            e.printStackTrace();
        }
    }
}