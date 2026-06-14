package controller.Admin;

import com.example.byod.SystemUser;
import utils.DatabaseHelper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox; // Import ComboBox
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class AddUserController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private ComboBox<String> cmbRole; // Changed from TextField to ComboBox

    private SystemUser newUser = null;

    @FXML
    public void initialize() {
        // This populates the dropdown when the modal opens
        cmbRole.getItems().addAll("Administrator", "Security Guard");
        cmbRole.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSave(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String fullName = txtFullName.getText().trim();
        // Use cmbRole.getValue() to get the selection from the dropdown
        String role = (cmbRole.getValue() != null) ? cmbRole.getValue().trim() : "";

        if (username.isEmpty() || role.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Username and Role parameters are strictly required.");
            return;
        }

        String insertQuery = "INSERT INTO users (username, role, password_hash) VALUES (?, ?, 'PUP-123456')";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, username);
            pstmt.setString(2, role.toUpperCase());
            pstmt.executeUpdate();

            newUser = new SystemUser(username, fullName, role, "Active", "");

            showAlert(Alert.AlertType.INFORMATION, "User Provisioned",
                    "The account for " + username + " has been securely created in the cloud.\n\n" +
                            "Initial Temporary Password: PUP-123456");

            closeStage(event);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to provision user. Username might already exist.");
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