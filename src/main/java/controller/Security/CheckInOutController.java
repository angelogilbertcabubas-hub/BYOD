package controller.Security;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CheckInOutController extends BaseSecurityController {

    @FXML private TextField txtDeviceSearch;
    @FXML private Button btnSearch;

    @FXML private Label lblStudentName;
    @FXML private Label lblStudentID;
    @FXML private Label lblDevice;
    @FXML private Label lblSerialNumber;
    @FXML private Label lblAccessCode;
    @FXML private Label lblCurrentStatus;

    @FXML private ComboBox<String> cmbAction;
    @FXML private ComboBox<String> cmbLocation;
    @FXML private ComboBox<String> cmbGuard;
    @FXML private TextArea txtRemarks;
    @FXML private Label lblDateTime;
    @FXML private Button btnConfirm;
    @FXML private Button btnClear;

    @FXML
    public void initialize() {
        cmbAction.getItems().addAll("Check-In", "Check-Out");
        cmbAction.getSelectionModel().selectFirst();

        cmbLocation.getItems().addAll("Main Gate","Side Gate","Back Gate","Library Entrance","Admin Building");
        cmbLocation.getSelectionModel().selectFirst();

        cmbGuard.getItems().addAll("Guard 01","Guard 02","Guard 03","Guard 04");
        cmbGuard.getSelectionModel().selectFirst();

        updateDateTime();
    }

    private void updateDateTime() {
        lblDateTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy  hh:mm a")));
    }

    @FXML
    private void handleSearch() {
        if (txtDeviceSearch.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please enter an Access Code, Student ID, or Serial Number.");
            return;
        }

        // Mock data populating the UI
        lblStudentName.setText("Juan Dela Cruz");
        lblStudentID.setText("2025-0001");
        lblDevice.setText("Lenovo Ideapad 5");
        lblSerialNumber.setText("SN123456789");
        lblAccessCode.setText("AC2026003");
        lblCurrentStatus.setText("Currently OUTSIDE");

        updateDateTime();
    }

    @FXML
    private void handleConfirm() {
        if (lblStudentID.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Please search for a device/student first.");
            return;
        }
        showAlert(Alert.AlertType.INFORMATION, cmbAction.getValue() + " logged!\n\nStudent: " + lblStudentName.getText());
        handleClear();
    }

    @FXML
    private void handleClear() {
        txtDeviceSearch.clear();
        txtRemarks.clear();

        lblStudentName.setText("");
        lblStudentID.setText("");
        lblDevice.setText("");
        lblSerialNumber.setText("");
        lblAccessCode.setText("");
        lblCurrentStatus.setText("");

        cmbAction.getSelectionModel().selectFirst();
        cmbLocation.getSelectionModel().selectFirst();
        cmbGuard.getSelectionModel().selectFirst();

        updateDateTime();
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("BYOD");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}