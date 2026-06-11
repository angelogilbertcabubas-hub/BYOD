package controller.Admin;

import com.example.byod.LogEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class Active_DevicesController extends BaseAdminController {

    // Matches the fx:id attributes exactly as defined in Active_Devices.fxml
    @FXML private TableView<LogEntry> activeDevicesTableView;
    @FXML private TableColumn<LogEntry, String> colStudentName;
    @FXML private TableColumn<LogEntry, String> colStudentID;
    @FXML private TableColumn<LogEntry, String> colDevice;
    @FXML private TableColumn<LogEntry, String> colAccessCode;
    @FXML private TableColumn<LogEntry, String> colTimeIn;
    @FXML private TableColumn<LogEntry, String> colLocation;

    private ObservableList<LogEntry> activeList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Link the columns to the properties inside the LogEntry model
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colDevice.setCellValueFactory(new PropertyValueFactory<>("deviceModel"));
        colAccessCode.setCellValueFactory(new PropertyValueFactory<>("accessToken"));
        colTimeIn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

        activeDevicesTableView.setItems(activeList);
        loadActiveDevices();
    }

    private void loadActiveDevices() {
        activeList.clear();

        // This query specifically looks for devices that are CHECKED_IN and have no check_out_time
        String query = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, " +
                "d.device_brand, d.device_name, d.unique_code, " +
                "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in " +
                "FROM check_in_out c " +
                "JOIN students s ON c.student_id = s.id " +
                "JOIN devices d ON c.device_id = d.id " +
                "WHERE c.status = 'CHECKED_IN' AND c.check_out_time IS NULL";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String logId = String.valueOf(rs.getObject("log_id"));
                String studentName = rs.getString("first_name") + " " + rs.getString("last_name");
                String studentId = rs.getString("school_id");

                String brand = rs.getString("device_brand");
                String deviceModel = (brand != null) ? brand + " " + rs.getString("device_name") : "No Device";

                String accessCode = rs.getString("unique_code");
                if (accessCode == null) accessCode = "N/A";

                String timeIn = rs.getString("formatted_time_in");
                if (timeIn == null) timeIn = "Unknown Time";

                // Populate the LogEntry model using the live database data
                activeList.add(new LogEntry(logId, studentName, studentId, deviceModel, accessCode, "Check-In", timeIn, "Main Gate"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}