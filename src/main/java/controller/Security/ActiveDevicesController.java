package controller.Security;

import com.example.byod.model.LogEntry;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class ActiveDevicesController extends BaseSecurityController {

    @FXML private TextField txtSearch;
    @FXML private TableView<LogEntry> activeDevicesTable;
    @FXML private TableColumn<LogEntry, String> colStudentName;
    @FXML private TableColumn<LogEntry, String> colStudentID;
    @FXML private TableColumn<LogEntry, String> colDevice;
    @FXML private TableColumn<LogEntry, String> colAccessCode;
    @FXML private TableColumn<LogEntry, String> colTimeIn;
    @FXML private TableColumn<LogEntry, String> colLocation;

    @FXML private Label lblTotalCount;
    @FXML private Button btnPage1;
    @FXML private Button btnPage2;
    @FXML private Button btnPage3;

    private int currentPage = 1;
    private static final int TOTAL_PAGES = 3;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colAccessCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colTimeIn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        activeDevicesTable.setItems(DataStore.getInstance().getActiveDevicesList());

        lblTotalCount.setText(String.valueOf(DataStore.getInstance().getActiveDevicesList().size()));
    }

    @FXML private void handleSearch() { System.out.println("Searching: " + txtSearch.getText()); }
    @FXML private void handleFilter() { showAlert("Filter logic processing..."); }

    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; updateActivePage(); } }
    @FXML private void handleNextPage() { if (currentPage < TOTAL_PAGES) { currentPage++; updateActivePage(); } }
    @FXML private void handlePage1() { currentPage = 1; updateActivePage(); }
    @FXML private void handlePage2() { currentPage = 2; updateActivePage(); }
    @FXML private void handlePage3() { currentPage = 3; updateActivePage(); }

    private void updateActivePage() {
        String a = "-fx-background-color: #7B0D0D; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;";
        String i = "-fx-background-color: white; -fx-text-fill: #555555; -fx-font-size: 13px; -fx-background-radius: 6; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-border-width: 1.2; -fx-padding: 4 10; -fx-cursor: hand;";
        btnPage1.setStyle(currentPage == 1 ? a : i);
        btnPage2.setStyle(currentPage == 2 ? a : i);
        btnPage3.setStyle(currentPage == 3 ? a : i);
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("BYOD Security"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}