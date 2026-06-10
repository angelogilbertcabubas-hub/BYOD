package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utils.DatabaseHelper;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorAlert("Login Failed", "Please enter both username and password.");
            return;
        }

        // 1. Get the current scene and change the mouse cursor to a loading spinner
        Scene currentScene = ((Node) event.getSource()).getScene();
        currentScene.setCursor(Cursor.WAIT);

        // 2. Create a Background Thread strictly for Authentication
        Thread networkWorker = new Thread(() -> {
            try {
                // Authenticate the user against Supabase using the fast Connection Pool
                String role = authenticateUser(username, password);

                if (role != null) {
                    // 3. Authentication successful. Switch UI immediately.
                    Platform.runLater(() -> {
                        String fxmlPath = null;
                        String windowTitle = null;

                        // FIX: Changed .equals to .equalsIgnoreCase to match "ADMIN" from database
                        if (role.equalsIgnoreCase("admin")) {
                            fxmlPath = "/com/example/byod/Admin/dashboard.fxml";
                            windowTitle = "Admin Dashboard";
                        } else if (role.equalsIgnoreCase("security")) {
                            fxmlPath = "/com/example/byod/Security/SecurityDashboard.fxml";
                            windowTitle = "Security Dashboard";
                        } else {
                            currentScene.setCursor(Cursor.DEFAULT);
                            showErrorAlert("Access Error", "Your account role is not recognized by the system. Found: " + role);
                            return;
                        }

                        loadDashboard(event, fxmlPath, windowTitle);
                    });

                } else {
                    // Login failed.
                    Platform.runLater(() -> {
                        currentScene.setCursor(Cursor.DEFAULT);
                        System.out.println("Invalid Login Credentials Entered.");
                        showErrorAlert("Access Denied", "The username or password you entered is incorrect.");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    currentScene.setCursor(Cursor.DEFAULT);
                    System.err.println("Critical network error during login.");
                    e.printStackTrace();
                    showErrorAlert("Connection Error", "Could not connect to the cloud database. Check your internet connection.");
                });
            }
        });

        networkWorker.setDaemon(true);
        networkWorker.start();
    }

    /**
     * Connects to Supabase and verifies the plain-text username and password.
     */
    private String authenticateUser(String username, String password) throws Exception {
        String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";

        // Grabs an instant connection from HikariCP
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        }
        return null;
    }

    /**
     * A helper method to handle the JavaFX window transition.
     */
    private void loadDashboard(ActionEvent event, String fxmlPath, String windowTitle) {
        try {
            URL dashboardUrl = getClass().getResource(fxmlPath);

            if (dashboardUrl == null) {
                ((Node) event.getSource()).getScene().setCursor(Cursor.DEFAULT);
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
            ((Node) event.getSource()).getScene().setCursor(Cursor.DEFAULT);
            System.err.println("CRITICAL: Failed to load the view pane.");
            e.printStackTrace();
            showErrorAlert("UI Load Failure", "An error occurred while building the view: " + e.getMessage());
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