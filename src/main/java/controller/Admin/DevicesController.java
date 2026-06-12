package controller.Admin;

import com.example.byod.model.Device;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class DevicesController extends BaseAdminController {

    @FXML private TextField searchBarField;
    @FXML private Label statusSummaryLabel;
    @FXML private TableView<Device> devicesTableView;

    @FXML private TableColumn<Device, String> colOwner;
    @FXML private TableColumn<Device, String> colDeviceType;
    @FXML private TableColumn<Device, String> colModel;
    @FXML private TableColumn<Device, String> colMAC;
    @FXML private TableColumn<Device, String> colToken;

    private FilteredList<Device> filteredDevices;

    @FXML
    public void initialize() {
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colDeviceType.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));
        colMAC.setCellValueFactory(new PropertyValueFactory<>("macAddress"));
        colToken.setCellValueFactory(new PropertyValueFactory<>("token"));

        filteredDevices = new FilteredList<>(DataStore.getInstance().getDevicesList(), p -> true);

        searchBarField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredDevices.setPredicate(device -> {
                if(newValue == null || newValue.isBlank()) return true;

                String keyword = newValue.toLowerCase();

                if (device.getOwnerName() != null && device.getOwnerName().toLowerCase().contains(keyword)) return true;
                if (device.getDeviceType() != null && device.getDeviceType().toLowerCase().contains(keyword)) return true;
                if (device.getModel() != null && device.getModel().toLowerCase().contains(keyword)) return true;
                if (device.getMacAddress() != null && device.getMacAddress().toLowerCase().contains(keyword)) return true;
                if (device.getToken() != null && device.getToken().toLowerCase().contains(keyword)) return true;

                return false;
            });

            updateCountLabel();
        });

        SortedList<Device> sortedDevices = new SortedList<>(filteredDevices);
        sortedDevices.comparatorProperty().bind(devicesTableView.comparatorProperty());
        devicesTableView.setItems(sortedDevices);

        // NEW: Make rows clickable to open the Device Profile Modal
        devicesTableView.setRowFactory(tv -> {
            TableRow<Device> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Device clickedDevice = row.getItem();
                    openDeviceProfileModal(clickedDevice, event);
                }
            });
            return row;
        });

        updateCountLabel();
    }

    @FXML
    private void handleRegisterDevice(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/byod/Admin/AddDeviceModal.fxml"));
            javafx.scene.Parent root = loader.load();

            AddDeviceController dialogController = loader.getController();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Register Student Hardware Asset Configuration");

            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));

            dialogStage.showAndWait();

            // FIXED: The "double-dip" manual list addition has been removed here!
            updateCountLabel();

        } catch (java.io.IOException e) {
            System.err.println("CRITICAL FAULT: Unable to compile asset registry pop-up sub-context views.");
            e.printStackTrace();
        }
    }

    // NEW: Method to launch the Device Profile Modal
    private void openDeviceProfileModal(Device targetDevice, javafx.scene.input.MouseEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/byod/Admin/DeviceProfileModal.fxml"));
            javafx.scene.Parent root = loader.load();

            DeviceProfileModalController controller = loader.getController();
            controller.initData(targetDevice);

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Hardware Asset Profile");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));
            dialogStage.showAndWait();

        } catch (Exception e) {
            System.err.println("Failed to load Device Profile Modal.");
            e.printStackTrace();
        }
    }

    private void updateCountLabel() {
        int count = DataStore.getInstance().getDevicesList().size();
        statusSummaryLabel.setText("Showing 1 to " + count + " of " + count + " hardware entries");
    }
}