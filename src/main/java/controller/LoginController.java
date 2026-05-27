package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.net.URL;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        String fxmlPath = null;
        String windowTitle = null;

        // Role-based authentication logic
        if ("Admin".equals(username) && "Admin123".equals(password)) {
            fxmlPath = "/com/example/byod/Admin/dashboard.fxml";
            windowTitle = "Admin Dashboard";
        } else if ("Security".equals(username) && "Security123".equals(password)) {
            fxmlPath = "/com/example/byod/Security/SecurityDashboard.fxml";
            windowTitle = "Security Dashboard";
        }

        if (fxmlPath != null) {
            try {
                URL dashboardUrl = getClass().getResource(fxmlPath);

                if (dashboardUrl == null) {
                    showErrorAlert("Navigation Error",
                            "Cannot find the FXML file at: " + fxmlPath +
                                    "\n\nTroubleshooting Tip: Verify the exact folder name spelling in the resources directory.");
                    return;
                }

                FXMLLoader loader = new FXMLLoader(dashboardUrl);
                Parent root = loader.load();

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

                stage.setScene(new Scene(root));
                stage.setTitle(windowTitle);
                stage.centerOnScreen();
                stage.show();

            } catch (Exception e) {
                System.err.println("CRITICAL: Failed to load the view pane.");
                e.printStackTrace();
                showErrorAlert("UI Load Failure", "An error occurred while building the view: " + e.getMessage());
            }
        } else {
            System.out.println("Invalid Login Credentials Entered.");
            showErrorAlert("Access Denied", "The username or password you entered is incorrect.");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}