package controller.Admin;

import com.example.byod.model.Device;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.util.Random;

public class AddDeviceController {

    @FXML private TextField txtOwnerName;
    @FXML private TextField txtDeviceType;
    @FXML private TextField txtModel;
    @FXML private TextField txtMacAddress;

    private Device newDevice = null;

    @FXML
    private void handleSave(ActionEvent event) {
        int randomTokenNum = 1000 + new Random().nextInt(9000);
        String generatedToken = "TKN-" + randomTokenNum;

        newDevice = new Device(
                txtOwnerName.getText(),
                txtDeviceType.getText(),
                txtModel.getText(),
                txtMacAddress.getText(),
                generatedToken
        );
        closeStage(event);
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        newDevice = null;
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public Device getNewDevice() {
        return newDevice;
    }
}