package controller.Admin;

import com.example.byod.SystemUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddUserController {

    @FXML private TextField txtUsername;
    @FXML private TextField txtFullName;
    @FXML private TextField txtRole;

    private SystemUser newUser = null;

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            newUser = new SystemUser(
                    txtUsername.getText(),
                    txtFullName.getText(),
                    txtRole.getText(),
                    "Active",
                    ""
            );
            closeStage(event);
        } catch (Exception e) {
            e.printStackTrace();
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
}