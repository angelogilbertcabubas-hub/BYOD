package controller.Admin;

import com.example.byod.model.Device;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class DevicesController extends BaseAdminController {

    @FXML private TextField searchBarField;
    @FXML private Label statusSummaryLabel;
    @FXML private TableView<Device> devicesTableView;

    @FXML private TableColumn<Device, String> colOwner;
    @FXML private TableColumn<Device, String> colDeviceType;
    @FXML private TableColumn<Device, String> colModel;

    // FIX: Updated the column ID to match the FXML change
    @FXML private TableColumn<Device, String> colSerialNumber;

    @FXML private TableColumn<Device, String> colToken;

    private FilteredList<Device> filteredDevices;

    @FXML
    @SuppressWarnings("unchecked")
    public void initialize() {
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        colDeviceType.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("model"));

        // FIX: Bound it to the "serialNumber" property of the Device class
        colSerialNumber.setCellValueFactory(new PropertyValueFactory<>("serialNumber"));

        colToken.setCellValueFactory(new PropertyValueFactory<>("token"));

        filteredDevices = new FilteredList<>(DataStore.getInstance().getDevicesList(), p -> true);

        searchBarField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredDevices.setPredicate(device -> {
                if(newValue == null || newValue.isBlank()) return true;

                String keyword = newValue.toLowerCase();

                if (device.getOwnerName() != null && device.getOwnerName().toLowerCase().contains(keyword)) return true;
                if (device.getDeviceType() != null && device.getDeviceType().toLowerCase().contains(keyword)) return true;
                if (device.getModel() != null && device.getModel().toLowerCase().contains(keyword)) return true;

                // FIX: Updated the search functionality to check against the Serial Number
                if (device.getSerialNumber() != null && device.getSerialNumber().toLowerCase().contains(keyword)) return true;

                if (device.getToken() != null && device.getToken().toLowerCase().contains(keyword)) return true;

                return false;
            });

            updateCountLabel();
        });

        TableColumn<Device, Void> actionColumn = null;
        for (TableColumn<Device, ?> col : devicesTableView.getColumns()) {
            if (col.getText() != null && col.getText().equalsIgnoreCase("ACTION")) {
                actionColumn = (TableColumn<Device, Void>) col;
                break;
            }
        }

        if (actionColumn == null) {
            actionColumn = new TableColumn<>("ACTION");
            actionColumn.setPrefWidth(160);
            devicesTableView.getColumns().add(actionColumn);
        }

        // Inject the buttons directly into the identified column
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 5 15;");
                deleteBtn.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 5 10;");
                pane.setAlignment(Pos.CENTER);

                editBtn.setOnAction(event -> {
                    Device device = getTableView().getItems().get(getIndex());
                    openDeviceProfileModal(device);
                });

                deleteBtn.setOnAction(event -> {
                    Device device = getTableView().getItems().get(getIndex());
                    handleDeleteDevice(device);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        SortedList<Device> sortedDevices = new SortedList<>(filteredDevices);
        sortedDevices.comparatorProperty().bind(devicesTableView.comparatorProperty());
        devicesTableView.setItems(sortedDevices);

        devicesTableView.setRowFactory(tv -> {
            TableRow<Device> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Node target = (Node) event.getTarget();
                    boolean clickedOnButton = false;
                    while (target != null) {
                        if (target instanceof Button) {
                            clickedOnButton = true;
                            break;
                        }
                        target = target.getParent();
                    }

                    if (!clickedOnButton) {
                        Device clickedDevice = row.getItem();
                        openDeviceProfileModal(clickedDevice);
                    }
                }
            });
            row.setStyle("-fx-cursor: hand;");
            return row;
        });

        updateCountLabel();
    }

    @FXML
    private void handleRegisterDevice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/Admin/AddDeviceModal.fxml"));
            Parent root = loader.load();

            AddDeviceController dialogController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Register Student Hardware Asset Configuration");

            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            dialogStage.setScene(new Scene(root, 650, 700));
            dialogStage.setResizable(true);

            dialogStage.showAndWait();

            updateCountLabel();

        } catch (IOException e) {
            System.err.println("CRITICAL FAULT: Unable to compile asset registry pop-up sub-context views.");
            e.printStackTrace();
        }
    }

    private void openDeviceProfileModal(Device targetDevice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/Admin/DeviceProfileModal.fxml"));
            Parent root = loader.load();

            DeviceProfileModalController controller = loader.getController();
            controller.initData(targetDevice);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Hardware Asset Profile");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(devicesTableView.getScene().getWindow());
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

            // Refresh table when modal is closed to reflect any edits or deletions
            devicesTableView.refresh();
            updateCountLabel();

        } catch (Exception e) {
            System.err.println("Failed to load Device Profile Modal.");
            e.printStackTrace();
        }
    }

    private void handleDeleteDevice(Device device) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Hardware Asset Record");
        alert.setContentText("Are you sure you want to permanently remove this " + device.getDeviceType() + " (" + device.getModel() + ") belonging to " + device.getOwnerName() + "?\n\nThis action cannot be undone.");

        ButtonType confirmBtn = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        alert.showAndWait().ifPresent(type -> {
            if (type == confirmBtn) {
                DataStore.getInstance().getDevicesList().remove(device);
                devicesTableView.refresh();
                updateCountLabel();
            }
        });
    }

    private void updateCountLabel() {
        int filtered = filteredDevices.size();
        int total = DataStore.getInstance().getDevicesList().size();
        if (filtered == total) {
            statusSummaryLabel.setText("Showing 1 to " + total + " of " + total + " hardware entries");
        } else {
            statusSummaryLabel.setText("Showing " + filtered + " of " + total + " hardware entries");
        }
    }
}