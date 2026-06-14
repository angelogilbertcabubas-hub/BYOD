package controller.Admin;

import com.example.byod.LogEntry;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.DataStore;

public class Active_DevicesController extends BaseAdminController {

    @FXML private TableView<LogEntry> activeDevicesTableView;
    @FXML private TableColumn<LogEntry, String> colStudentName;
    @FXML private TableColumn<LogEntry, String> colStudentID;
    @FXML private TableColumn<LogEntry, String> colDevice;
    @FXML private TableColumn<LogEntry, String> colAccessCode;
    @FXML private TableColumn<LogEntry, String> colTimeIn;
    @FXML private TableColumn<LogEntry, String> colLocation;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colAccessCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colTimeIn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        // --- BATCH 2: UI/UX SECURITY ENHANCEMENTS ---

        // 1. Row Highlighting: Turns the entire row faint red if a compromised device is inside the campus!
        activeDevicesTableView.setRowFactory(tv -> new TableRow<LogEntry>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else {
                    if ("COMPROMISED".equalsIgnoreCase(item.getStatus())) {
                        setStyle("-fx-background-color: #FFEBEE;"); // Faint red alert background
                    } else {
                        setStyle(""); // Default background
                    }
                }
            }
        });

        // 2. Cell Badging: Injects the 🚨 emoji and bold red text into the Device column
        colDevice.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    LogEntry log = getTableRow().getItem();
                    if ("COMPROMISED".equalsIgnoreCase(log.getStatus())) {
                        setText("🚨 [LOCKED] " + item);
                        setStyle("-fx-text-fill: #B71C1C; -fx-font-weight: bold;");
                    } else {
                        setText("🟢 " + item);
                        setStyle("-fx-text-fill: #2E7D32; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // --------------------------------------------

        // By linking to the DataStore, the Admin page instantly updates
        // whenever the Security guard performs a check-in/out!
        activeDevicesTableView.setItems(DataStore.getInstance().getActiveDevicesList());
    }
}