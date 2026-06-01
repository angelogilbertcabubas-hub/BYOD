package controller.Admin;

import com.example.byod.SystemUser;
import utils.DataStore;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class User_ManagementController extends BaseAdminController {

    @FXML private TableView<SystemUser> userManagementTableView;
    @FXML private TableColumn<SystemUser, String> colUserIdenticon;
    @FXML private TableColumn<SystemUser, String> colUserFullName;
    @FXML private TableColumn<SystemUser, String> colUserPrivilegeBadge;
    @FXML private TableColumn<SystemUser, String> colUserStateBadge;
    @FXML private TableColumn<SystemUser, String> colUserActionControls;
    @FXML private Label entriesSummaryCountLabel;

    @FXML
    public void initialize() {
        colUserIdenticon.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserPrivilegeBadge.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserStateBadge.setCellValueFactory(new PropertyValueFactory<>("status"));

        colUserActionControls.setCellFactory(param -> new TableCell<SystemUser, String>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnDelete = new Button("Delete");
            private final javafx.scene.layout.HBox pane = new javafx.scene.layout.HBox(10, btnEdit, btnDelete);

            {
                btnEdit.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");
                btnDelete.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 10;");

                btnEdit.setOnAction(event -> {
                    SystemUser user = getTableView().getItems().get(getIndex());
                    System.out.println("Editing user: " + user.getUsername());
                });

                btnDelete.setOnAction(event -> {
                    SystemUser user = getTableView().getItems().get(getIndex());
                    DataStore.getInstance().getUsersList().remove(user);
                    updateCountLabel(); // Refresh the counter
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        userManagementTableView.setItems(DataStore.getInstance().getUsersList());
        updateCountLabel();
    }

    private void updateCountLabel() {
        int count = DataStore.getInstance().getUsersList().size();
        entriesSummaryCountLabel.setText("Showing 1 to " + count + " of " + count + " users");
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

            SystemUser createdUser = dialogController.getNewUser();
            if (createdUser != null) {
                DataStore.getInstance().getUsersList().add(createdUser);
                updateCountLabel();
            }

        } catch (java.io.IOException e) {
            System.err.println("CRITICAL FAULT: Unable to load Add User Modal.");
            e.printStackTrace();
        }
    }
}