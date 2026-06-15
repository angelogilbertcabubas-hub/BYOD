package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import utils.DatabaseHelper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Hyperlink forgotPasswordLink;

    @FXML private TextField passwordVisible;
    @FXML private Button eyeToggleBtn;
    @FXML private StackPane loadingOverlay;

    private boolean isPasswordVisible = false;

    @FXML
    public void initialize() {
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnAction(this::handleForgotPassword);
        }

        // Phase 1: Enter Key Login Support
        usernameField.setOnKeyPressed(this::handleEnterKey);
        passwordField.setOnKeyPressed(this::handleEnterKey);
        passwordVisible.setOnKeyPressed(this::handleEnterKey);
    }

    private void handleEnterKey(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            // Trigger the login event using the source of the keypress
            handleLogin(new ActionEvent(event.getSource(), event.getTarget()));
        }
    }

    public void togglePasswordVisibility(ActionEvent event){
        isPasswordVisible = !isPasswordVisible;

        if(isPasswordVisible){
            // Copy hidden text to visible field
            passwordVisible.setText(passwordField.getText());
            passwordVisible.setVisible(true);
            passwordVisible.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            eyeToggleBtn.setText("👁 Hide");
        } else {
            // Copy visible text back to hidden field
            passwordField.setText(passwordVisible.getText());
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            eyeToggleBtn.setText("👁 Show");
        }
    }

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = isPasswordVisible ? passwordVisible.getText() : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showCustomAlert("Login Failed", "Please enter both username and password.");
            return;
        }

        setLoading(true);

        Scene currentScene = ((Node) event.getSource()).getScene();
        currentScene.setCursor(Cursor.WAIT);

        Thread networkWorker = new Thread(() -> {
            try {
                String role = authenticateUser(username, password);
                if (role != null) {
                    Platform.runLater(() -> {
                        setLoading(false);
                        String fxmlPath = role.equalsIgnoreCase("admin") ? "/com/example/byod/Admin/dashboard.fxml" : "/com/example/byod/Security/SecurityDashboard.fxml";
                        String windowTitle = role.equalsIgnoreCase("admin") ? "Admin Dashboard" : "Security Dashboard";
                        loadDashboard(event, fxmlPath, windowTitle);
                    });
                } else {
                    Platform.runLater(() -> {
                        setLoading(false);
                        currentScene.setCursor(Cursor.DEFAULT);
                        showCustomAlert("Access Denied", "The username or password you entered is incorrect.");
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    currentScene.setCursor(Cursor.DEFAULT);
                    e.printStackTrace();
                    showCustomAlert("Connection Error", "Could not connect to the cloud database.");
                });
            }
        });
        networkWorker.setDaemon(true);
        networkWorker.start();
    }

    private void setLoading(boolean loading){
        loadingOverlay.setVisible(loading);
        loadingOverlay.setManaged(loading);
    }

    private String authenticateUser(String username, String password) throws Exception {
        String query = "SELECT role FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("role");
            }
        }
        return null;
    }

    private void handleForgotPassword(ActionEvent event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(((Node) event.getSource()).getScene().getWindow());
        dialog.initStyle(StageStyle.TRANSPARENT);

        Label keyIconGfx = new Label("🔑");
        keyIconGfx.setAlignment(Pos.CENTER);
        keyIconGfx.setPrefSize(50, 50);
        keyIconGfx.setMinSize(50, 50);
        keyIconGfx.setMaxSize(50, 50);
        keyIconGfx.setStyle("-fx-background-color: #C49A45; -fx-background-radius: 25; -fx-font-size: 25px; -fx-text-fill: white;");
        Label titleLabel = new Label("Account Recovery");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #500A0E;");
        Label subTitleLabel = new Label("Password Reset Request");
        subTitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555555;");
        VBox headerTextContainer = new VBox(2, titleLabel, subTitleLabel);
        HBox headerContainer = new HBox(15, keyIconGfx, headerTextContainer);
        headerContainer.setPadding(new Insets(20, 25, 10, 25));

        TextField inputField = new TextField();
        inputField.setPromptText("Enter username or email");
        inputField.setStyle("-fx-background-radius: 4; -fx-border-color: #DDD; -fx-border-radius: 4; -fx-padding: 8;");
        VBox inputContainer = new VBox(8, new Label("Registered User/Email:"), inputField);
        inputContainer.setPadding(new Insets(10, 25, 20, 25));
        Button btnSend = new Button("Send Request");
        btnSend.setPrefWidth(120);
        btnSend.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 8;");
        Button btnCancel = new Button("Cancel");
        btnCancel.setPrefWidth(120);
        btnCancel.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #500A0E; -fx-border-color: #500A0E; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8;");
        HBox buttonContainer = new HBox(10, btnSend, btnCancel);
        buttonContainer.setAlignment(Pos.CENTER_RIGHT);
        buttonContainer.setPadding(new Insets(0, 25, 20, 25));

        VBox root = new VBox(headerContainer, inputContainer, buttonContainer);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15;");

        btnCancel.setOnAction(e -> dialog.close());
        btnSend.setOnAction(e -> {
            String targetUsername = inputField.getText().trim();
            if (targetUsername.isEmpty()) return;
            new Thread(() -> {
                String query = "UPDATE users SET status = 'Reset Requested' WHERE username = ? RETURNING username";
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, targetUsername);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        Platform.runLater(() -> {
                            showCustomAlert("Success", "Reset request for '" + targetUsername + "' has been sent.");
                            dialog.close();
                        });
                    } else {
                        Platform.runLater(() -> showCustomAlert("Failed", "User not found."));
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }).start();
        });

        Scene scene = new Scene(root, 450, 250);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
        javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
        dialog.setX((bounds.getWidth() - 450) / 2);
        dialog.setY((bounds.getHeight() - 250) / 2);
        dialog.showAndWait();
    }

    private void loadDashboard(ActionEvent event, String fxmlPath, String windowTitle) {
        try {
            URL dashboardUrl = getClass().getResource(fxmlPath);
            if (dashboardUrl == null) {
                ((Node) event.getSource()).getScene().setCursor(Cursor.DEFAULT);
                showCustomAlert("Navigation Error", "Cannot find the FXML file: " + fxmlPath);
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
            e.printStackTrace(); // Logs it to your console
            ((Node) event.getSource()).getScene().setCursor(Cursor.DEFAULT);

            // Extract the actual root cause of the JavaFX crash
            Throwable cause = e.getCause();
            if (cause != null && cause.getCause() != null) {
                cause = cause.getCause();
            }
            String errorMsg = (cause != null) ? cause.toString() : e.getMessage();

            // Displays the explicit technical error on your screen
            showCustomAlert("System Crash Log", "Error: " + errorMsg);
        }
    }

    private void showCustomAlert(String title, String message) {
        Stage alertStage = new Stage();
        alertStage.initModality(Modality.APPLICATION_MODAL);
        alertStage.initStyle(StageStyle.TRANSPARENT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #500A0E;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333;");
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(350);

        Button btnOk = new Button("OK");
        btnOk.setPrefWidth(90);
        btnOk.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 6;");
        btnOk.setOnAction(e -> alertStage.close());

        VBox box = new VBox(15, titleLabel, msgLabel, btnOk);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: white; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-background-radius: 15; -fx-border-radius: 15;");

        Scene scene = new Scene(box);
        scene.setFill(Color.TRANSPARENT);
        alertStage.setScene(scene);

        alertStage.setOnShown(e -> {
            javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
            javafx.geometry.Rectangle2D bounds = screen.getVisualBounds();
            alertStage.setX(bounds.getMinX() + (bounds.getWidth() - alertStage.getWidth()) / 2);
            alertStage.setY(bounds.getMinY() + (bounds.getHeight() - alertStage.getHeight()) / 2);
        });

        alertStage.showAndWait();
    }
}