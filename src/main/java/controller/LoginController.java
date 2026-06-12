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
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import utils.DatabaseHelper;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // THIS IS THE FIX: Binding the button here instead of the layout
    @FXML private Hyperlink forgotPasswordLink;

    @FXML
    public void initialize() {
        // Bulletproof programmatic binding
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnAction(this::handleForgotPassword);
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorAlert("Login Failed", "Please enter both username and password.");
            return;
        }

        Scene currentScene = ((Node) event.getSource()).getScene();
        currentScene.setCursor(Cursor.WAIT);

        Thread networkWorker = new Thread(() -> {
            try {
                String role = authenticateUser(username, password);

                if (role != null) {
                    Platform.runLater(() -> {
                        String fxmlPath = null;
                        String windowTitle = null;

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
                    Platform.runLater(() -> {
                        currentScene.setCursor(Cursor.DEFAULT);
                        showErrorAlert("Access Denied", "The username or password you entered is incorrect.");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    currentScene.setCursor(Cursor.DEFAULT);
                    e.printStackTrace();
                    showErrorAlert("Connection Error", "Could not connect to the cloud database. Check your internet connection.");
                });
            }
        });

        networkWorker.setDaemon(true);
        networkWorker.start();
    }

    private String authenticateUser(String username, String password) throws Exception {
        String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";

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

    private void handleForgotPassword(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Account Recovery");
        dialog.setHeaderText("Password Reset Request");
        dialog.setContentText("Please enter your registered username or email:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String targetUsername = result.get().trim();
            if (targetUsername.isEmpty()) return;

            new Thread(() -> {
                String query = "SELECT username FROM users WHERE username = ?";
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {

                    pstmt.setString(1, targetUsername);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Recovery Initiated");
                            alert.setHeaderText("System Administrator Notified");
                            alert.setContentText("Your reset request for '" + targetUsername + "' has been verified.\n\nPlease contact your System Administrator. They will securely reset your password from the User Management console.");
                            alert.showAndWait();
                        });
                    } else {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Recovery Failed");
                            alert.setHeaderText("Account Not Found");
                            alert.setContentText("The username '" + targetUsername + "' does not exist in our secure registry.");
                            alert.showAndWait();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showErrorAlert("Connection Error", "Could not reach the database to verify your account."));
                }
            }).start();
        }
    }

    private void loadDashboard(ActionEvent event, String fxmlPath, String windowTitle) {
        try {
            URL dashboardUrl = getClass().getResource(fxmlPath);

            if (dashboardUrl == null) {
                ((Node) event.getSource()).getScene().setCursor(Cursor.DEFAULT);
                showErrorAlert("Navigation Error", "Cannot find the FXML file at: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(dashboardUrl);
            Parent root = loader.load();

            // Safe window retrieval
            Stage stage = null;
            if (event.getSource() instanceof Node) {
                stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            } else if (forgotPasswordLink != null && forgotPasswordLink.getScene() != null) {
                stage = (Stage) forgotPasswordLink.getScene().getWindow();
            }

            if (stage != null) {
                stage.setScene(new Scene(root));
                stage.setTitle(windowTitle);
                stage.centerOnScreen();
                stage.show();
            }

        } catch (Exception e) {
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