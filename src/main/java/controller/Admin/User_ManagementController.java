package controller.Admin;

import com.example.byod.SystemUser;
import utils.DatabaseHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;

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
            String query = "SELECT username, role FROM users";
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                cloudUsersList.clear();

                while (rs.next()) {
                    String username = rs.getString("username");
                    String role = rs.getString("role");

                    SystemUser user = new SystemUser(username, username, role, "Active", "");
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
        Stage ownerStage = (Stage) userManagementTableView.getScene().getWindow();
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
        dialog.initStyle(StageStyle.UNDECORATED);

        Label keyIconGfx = new Label("🔑");
        keyIconGfx.setAlignment(Pos.CENTER);
        keyIconGfx.setPrefSize(42, 42);
        keyIconGfx.setMinSize(42, 42);
        keyIconGfx.setMaxSize(42, 42);
        keyIconGfx.setStyle("-fx-background-color: #C49A45; -fx-background-radius: 21; -fx-font-size: 20px; -fx-text-fill: white;");

        Label titleLabel = new Label("Secure Password Reset");
        titleLabel.setStyle("-fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #500A0E;");

        Label subTitleLabel = new Label("Provision a new secure credential for: " + user.getUsername());
        subTitleLabel.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px; -fx-text-fill: #555555;");

        VBox headerTextContainer = new VBox(2, titleLabel, subTitleLabel);
        headerTextContainer.setAlignment(Pos.CENTER_LEFT);

        HBox headerContainer = new HBox(15, keyIconGfx, headerTextContainer);
        headerContainer.setAlignment(Pos.CENTER_LEFT);
        headerContainer.setPadding(new Insets(20, 25, 10, 25));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 25, 10, 25));

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password");
        newPasswordField.setPrefWidth(295);
        newPasswordField.setStyle("-fx-background-radius: 4; -fx-border-color: #DDD; -fx-border-radius: 4; -fx-padding: 5;");

        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm new password");
        confirmPasswordField.setPrefWidth(295);
        confirmPasswordField.setStyle("-fx-background-radius: 4; -fx-border-color: #DDD; -fx-border-radius: 4; -fx-padding: 5;");

        Label lblNew = new Label("New Password:");
        lblNew.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");
        Label lblConf = new Label("Confirm Password:");
        lblConf.setStyle("-fx-font-weight: bold; -fx-text-fill: #333333;");

        grid.add(lblNew, 0, 0);
        grid.add(newPasswordField, 1, 0);
        grid.add(lblConf, 0, 1);
        grid.add(confirmPasswordField, 1, 1);

        Label rulesLabel = new Label("Required: Min 8 chars, 1 uppercase, 1 lowercase, 1 number");
        rulesLabel.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #D32F2F; -fx-font-size: 11px; -fx-font-weight: bold;");

        VBox validationContainer = new VBox(4, rulesLabel, errorLabel);
        validationContainer.setPadding(new Insets(0, 25, 10, 25));

        Button btnSave = new Button("Update Password");
        btnSave.setDisable(true);
        btnSave.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12px;");

        btnSave.setOnMouseEntered(e -> {
            if (!btnSave.isDisabled()) {
                btnSave.setStyle("-fx-background-color: #C49A45; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12px;");
            }
        });

        btnSave.setOnMouseExited(e -> {
            if (!btnSave.isDisabled()) {
                btnSave.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12px;");
            }
        });

        Button btnCancel = new Button("Cancel");
        btnCancel.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #500A0E; -fx-border-color: #500A0E; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12px;");

        HBox buttonContainer = new HBox(10, btnSave, btnCancel);
        buttonContainer.setAlignment(Pos.BOTTOM_RIGHT);
        buttonContainer.setPadding(new Insets(10, 25, 20, 25));

        Pattern passwordPattern = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");
        javafx.beans.value.ChangeListener<String> validator = (observable, oldValue, newValue) -> {
            String pwd = newPasswordField.getText();
            String conf = confirmPasswordField.getText();

            if (pwd.isEmpty()) {
                errorLabel.setText("");
                btnSave.setDisable(true);
            } else if (!passwordPattern.matcher(pwd).matches()) {
                errorLabel.setText("Password does not meet complexity requirements.");
                btnSave.setDisable(true);
            } else if (!pwd.equals(conf)) {
                errorLabel.setText("Passwords do not match.");
                btnSave.setDisable(true);
            } else {
                errorLabel.setText("Secure configuration verified.");
                errorLabel.setStyle("-fx-text-fill: #27AE60; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnSave.setDisable(false);
            }
        };

        newPasswordField.textProperty().addListener(validator);
        confirmPasswordField.textProperty().addListener(validator);

        VBox root = new VBox(5, headerContainer, grid, validationContainer, buttonContainer);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        btnCancel.setOnAction(e -> dialog.close());

        btnSave.setOnAction(e -> {
            String newSecurePassword = newPasswordField.getText();
            dialog.close();

            new Thread(() -> {
                String query = "UPDATE users SET password_hash = ? WHERE username = ?";
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {

                    pstmt.setString(1, newSecurePassword);
                    pstmt.setString(2, user.getUsername());
                    pstmt.executeUpdate();

                    Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, "Reset Successful", "The secure password for " + user.getUsername() + " has been officially updated in the cloud."));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Sync Failed", "Could not reach the cloud database to update the credentials."));
                }
            }).start();
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.setWidth(500);
        dialog.setHeight(300);

        dialog.setOnShown(e -> {
            dialog.setX(ownerStage.getX() + (ownerStage.getWidth() - dialog.getWidth()) / 2);
            dialog.setY(ownerStage.getY() + (ownerStage.getHeight() - dialog.getHeight()) / 2);
        });

        dialog.showAndWait();
    }

    private void handleDeleteUser(SystemUser user) {
        Stage ownerStage = (Stage) userManagementTableView.getScene().getWindow();

        if (showCustomDeleteConfirmation(ownerStage, user.getUsername())) {
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

    private boolean showCustomDeleteConfirmation(Stage ownerStage, String username) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(ownerStage);
        dialog.initStyle(StageStyle.UNDECORATED);

        Label systemIconGfx = new Label("?");
        systemIconGfx.setAlignment(Pos.CENTER);
        systemIconGfx.setPrefSize(42, 42);
        systemIconGfx.setMinSize(42, 42);
        systemIconGfx.setMaxSize(42, 42);
        systemIconGfx.setStyle(
                "-fx-background-color: #C49A45; " +
                        "-fx-background-radius: 21; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: 'Segoe UI', 'System'; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 22px;"
        );

        Label titleLabel = new Label("Confirmation");
        titleLabel.setStyle("-fx-font-family: 'System'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #500A0E;");

        Label messageLabel = new Label("Are you sure you want to permanently delete the account: " + username + "?");
        messageLabel.setWrapText(true);
        messageLabel.setPrefWidth(320);
        messageLabel.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px; -fx-text-fill: #444444;");

        VBox textContainer = new VBox(4, titleLabel, messageLabel);
        textContainer.setAlignment(Pos.CENTER_LEFT);

        HBox bodyContainer = new HBox(18, systemIconGfx, textContainer);
        bodyContainer.setAlignment(Pos.CENTER_LEFT);
        bodyContainer.setPadding(new Insets(25, 25, 15, 25));

        Button btnYes = new Button("Yes");
        btnYes.setPrefWidth(75);
        btnYes.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 12px;");

        Button btnNo = new Button("No");
        btnNo.setPrefWidth(75);
        btnNo.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #500A0E; -fx-border-color: #500A0E; -fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 6 12; -fx-font-size: 12px;");

        HBox buttonContainer = new HBox(10, btnYes, btnNo);
        buttonContainer.setAlignment(Pos.BOTTOM_RIGHT);
        buttonContainer.setPadding(new Insets(0, 25, 20, 25));

        VBox root = new VBox(bodyContainer, buttonContainer);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-border-radius: 12; -fx-background-radius: 12;");

        final boolean[] userResponse = {false};

        btnYes.setOnAction(e -> {
            userResponse[0] = true;
            dialog.close();
        });

        btnNo.setOnAction(e -> {
            userResponse[0] = false;
            dialog.close();
        });

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setWidth(430);
        dialog.setHeight(165);

        dialog.setOnShown(e -> {
            dialog.setX(ownerStage.getX() + (ownerStage.getWidth() - dialog.getWidth()) / 2);
            dialog.setY(ownerStage.getY() + (ownerStage.getHeight() - dialog.getHeight()) / 2);
        });

        dialog.showAndWait();
        return userResponse[0];
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