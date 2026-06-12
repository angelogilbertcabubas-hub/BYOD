package controller.Admin;

import com.example.byod.SystemUser;
import utils.DatabaseHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.regex.Pattern;

public class User_ManagementController extends BaseAdminController {

    @FXML private TableView<SystemUser> userManagementTableView;
    @FXML private TableColumn<SystemUser, String> colUserIdenticon;
    @FXML private TableColumn<SystemUser, String> colUserFullName;
    @FXML private TableColumn<SystemUser, String> colUserPrivilegeBadge;
    @FXML private TableColumn<SystemUser, String> colUserStateBadge;
    @FXML private TableColumn<SystemUser, String> colUserActionControls;
    @FXML private Label entriesSummaryCountLabel;

    private ObservableList<SystemUser> cloudUsersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colUserIdenticon.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserPrivilegeBadge.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserStateBadge.setCellValueFactory(new PropertyValueFactory<>("status"));

        // UI UPGRADE: Dynamic Table Cell Coloring based on Status
        colUserStateBadge.setCellFactory(column -> new TableCell<SystemUser, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("Reset Requested")) {
                        setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;"); // Red Warning
                    } else {
                        setStyle("-fx-text-fill: #27AE60; -fx-font-weight: bold;"); // Green Active
                    }
                }
            }
        });

        colUserActionControls.setCellFactory(param -> new TableCell<SystemUser, String>() {
            private final Button btnReset = new Button("Reset Password");
            private final Button btnDelete = new Button("Delete");
            private final HBox pane = new HBox(10, btnReset, btnDelete);

            {
                btnReset.setStyle("-fx-background-color: #E67E22; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4; -fx-font-weight: bold;");
                btnDelete.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10; -fx-background-radius: 4; -fx-font-weight: bold;");

                btnReset.setOnAction(event -> {
                    SystemUser user = getTableView().getItems().get(getIndex());
                    handleSecurePasswordReset(user);
                });

                btnDelete.setOnAction(event -> {
                    SystemUser user = getTableView().getItems().get(getIndex());
                    handleDeleteUser(user);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        userManagementTableView.setItems(cloudUsersList);
        fetchUsersFromCloud();
    }

    private void fetchUsersFromCloud() {
        entriesSummaryCountLabel.setText("Syncing users with cloud...");

        new Thread(() -> {
            // UI FIX: Pulling the actual status from the cloud instead of hardcoding "Active"
            String query = "SELECT username, role, COALESCE(status, 'Active') as current_status FROM users";
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                cloudUsersList.clear();

                while (rs.next()) {
                    String username = rs.getString("username");
                    String role = rs.getString("role");
                    String status = rs.getString("current_status");

                    SystemUser user = new SystemUser(username, username, role, status, "");
                    cloudUsersList.add(user);
                }

                Platform.runLater(this::updateCountLabel);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> entriesSummaryCountLabel.setText("Failed to connect to cloud database."));
            }
        }).start();
    }

    private void handleSecurePasswordReset(SystemUser user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Secure Password Reset");
        dialog.setHeaderText("Provision a new secure credential for: " + user.getUsername());

        ButtonType saveButtonType = new ButtonType("Update Credentials", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");

        Label rulesLabel = new Label("Required: Min 8 chars, 1 uppercase, 1 lowercase, 1 number");
        rulesLabel.setStyle("-fx-text-fill: #555555; -fx-font-size: 11px;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 11px; -fx-font-weight: bold;");

        grid.add(new Label("New Password:"), 0, 0);
        grid.add(newPasswordField, 1, 0);
        grid.add(new Label("Confirm Password:"), 0, 1);
        grid.add(confirmPasswordField, 1, 1);
        grid.add(rulesLabel, 1, 2);
        grid.add(errorLabel, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Pattern passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");

        javafx.beans.value.ChangeListener<String> validator = (observable, oldValue, newValue) -> {
            String pwd = newPasswordField.getText();
            String conf = confirmPasswordField.getText();

            if (pwd.isEmpty()) {
                errorLabel.setText("");
                saveButton.setDisable(true);
            } else if (!passwordPattern.matcher(pwd).matches()) {
                errorLabel.setText("Password does not meet complexity requirements.");
                saveButton.setDisable(true);
            } else if (!pwd.equals(conf)) {
                errorLabel.setText("Passwords do not match.");
                saveButton.setDisable(true);
            } else {
                errorLabel.setText("Secure configuration verified.");
                errorLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11px; -fx-font-weight: bold;");
                saveButton.setDisable(false);
            }
        };

        newPasswordField.textProperty().addListener(validator);
        confirmPasswordField.textProperty().addListener(validator);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return newPasswordField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(newSecurePassword -> {
            new Thread(() -> {
                // UI FIX: Clear the 'Reset Requested' status back to 'Active' upon save
                String query = "UPDATE users SET password_hash = ?, status = 'Active' WHERE username = ?";
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {

                    pstmt.setString(1, newSecurePassword);
                    pstmt.setString(2, user.getUsername());
                    pstmt.executeUpdate();

                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.INFORMATION, "Reset Successful", "The secure password for " + user.getUsername() + " has been updated, and their status is cleared.");
                        fetchUsersFromCloud(); // Refresh the table to paint it green again!
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Sync Failed", "Could not reach the cloud database to update the credentials."));
                }
            }).start();
        });
    }

    private void handleDeleteUser(SystemUser user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you absolutely sure you want to permanently delete the account: " + user.getUsername() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Account");

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            new Thread(() -> {
                String query = "DELETE FROM users WHERE username = ?";
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {

                    pstmt.setString(1, user.getUsername());
                    pstmt.executeUpdate();

                    Platform.runLater(() -> {
                        cloudUsersList.remove(user);
                        updateCountLabel();
                        showAlert(Alert.AlertType.INFORMATION, "Account Deleted", "User access has been permanently revoked.");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Could not remove user from cloud."));
                }
            }).start();
        }
    }

    private void updateCountLabel() {
        int count = cloudUsersList.size();
        entriesSummaryCountLabel.setText("Showing 1 to " + count + " of " + count + " authorized users");
    }

    @FXML
    void handleLabelAddUser(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/byod/Admin/AddUserModal.fxml"));
            javafx.scene.Parent root = loader.load();
            AddUserController dialogController = loader.getController();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Provision New System User");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));

            dialogStage.showAndWait();

            if (dialogController.getNewUser() != null) {
                fetchUsersFromCloud();
            }

        } catch (java.io.IOException e) {
            System.err.println("CRITICAL FAULT: Unable to load Add User Modal.");
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Account Provisioning Security");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}