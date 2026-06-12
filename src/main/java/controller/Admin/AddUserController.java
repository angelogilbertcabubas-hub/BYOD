package controller.Admin;

import com.example.byod.SystemUser;
import utils.DatabaseHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddUserController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtRole;

    private SystemUser newUser = null;

    @FXML
    private void handleSave(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        String role = txtRole.getText().trim();

        if (username.isEmpty() || role.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Username and Role parameters are strictly required.");
            return;
        }

        // Beam the new user directly to the Supabase Cloud
        String insertQuery = "INSERT INTO users (username, role, password_hash) VALUES (?, ?, 'PUP-123456')";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, username);
            pstmt.setString(2, role.toUpperCase()); // Normalize role to ADMIN or SECURITY
            pstmt.executeUpdate();

            // Store the verified object to pass back to the main table
            newUser = new SystemUser(username, fullName, role, "Active", "");

            showAlert(Alert.AlertType.INFORMATION, "User Provisioned",
                    "The account for " + username + " has been securely created in the cloud.\n\n" +
                            "Initial Temporary Password: PUP-123456\n\n" +
                            "The user can now log in. You can securely update this password later using the Reset Password feature.");

            closeStage(event);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to provision user. The username might already exist in the system.");
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        newUser = null;
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public SystemUser getNewUser() {
        return newUser;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Account Provisioning Security");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}