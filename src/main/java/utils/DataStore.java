package utils;

import com.example.byod.model.Device;
import com.example.byod.model.Student;
import com.example.byod.LogEntry;
import com.example.byod.Report;
import com.example.byod.SystemUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class DataStore {
    private static DataStore instance;

    private ObservableList<Student> studentsList;
    private ObservableList<Device> devicesList;
    private ObservableList<LogEntry> monitoringLogsList;
    private ObservableList<LogEntry> activeDevicesList;
    private ObservableList<Report> reportsList;
    private ObservableList<SystemUser> usersList;

    private DataStore() {
        studentsList = FXCollections.observableArrayList();
        devicesList = FXCollections.observableArrayList();
        monitoringLogsList = FXCollections.observableArrayList();
        activeDevicesList = FXCollections.observableArrayList();
        reportsList = FXCollections.observableArrayList();
        usersList = FXCollections.observableArrayList();

        // Initial load on application startup
        loadStudentsFromDatabase();
        loadDevicesFromDatabase();
        loadLogsFromDatabase();
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    // NEW REFRESH METHODS FOR REAL-TIME UI UPDATES
    public void refreshStudents() {
        studentsList.clear();
        loadStudentsFromDatabase();
    }

    public void refreshDevices() {
        devicesList.clear();
        loadDevicesFromDatabase();
    }

    public void refreshLogs() {
        monitoringLogsList.clear();
        loadLogsFromDatabase();
    }

    private void loadLogsFromDatabase() {
        String query = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, " +
                "d.device_brand, d.device_name, d.unique_code, c.status, " +
                "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in, " +
                "TO_CHAR(c.check_out_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_out " +
                "FROM check_in_out c " +
                "JOIN students s ON c.student_id = s.id " +
                "JOIN devices d ON c.device_id = d.id " +
                "ORDER BY COALESCE(c.check_out_time, c.check_in_time) DESC LIMIT 100";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String logId = String.valueOf(rs.getObject("log_id"));
                String studentName = rs.getString("first_name") + " " + rs.getString("last_name");
                String studentId = rs.getString("school_id");

                String brand = rs.getString("device_brand");
                String deviceModel = (brand != null) ? brand + " " + rs.getString("device_name") : "No Device";

                String accessToken = rs.getString("unique_code");
                if (accessToken == null) accessToken = "N/A";

                String status = rs.getString("status");
                String operation = "CHECKED_OUT".equals(status) ? "Check-Out" : "Check-In";

                String timestamp = "CHECKED_OUT".equals(status) ?
                        rs.getString("formatted_time_out") : rs.getString("formatted_time_in");

                if (timestamp == null) timestamp = "Unknown Time";

                monitoringLogsList.add(new LogEntry(logId, studentName, studentId, deviceModel, accessToken, operation, timestamp, "Main Gate"));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadStudentsFromDatabase() {
        String query = "SELECT school_id, first_name, last_name, middle_initial, program_course, email_address, mobile_number, status FROM students";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String studentId = rs.getString("school_id");
                String mName = rs.getString("middle_initial");
                String fullName = rs.getString("first_name") + " " + (mName != null && !mName.isEmpty() ? mName + " " : "") + rs.getString("last_name");
                String course = rs.getString("program_course");
                String email = rs.getString("email_address");
                String mobile = rs.getString("mobile_number");
                String status = rs.getString("status");

                studentsList.add(new Student(studentId, fullName, course, email, mobile, status));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDevicesFromDatabase() {
        String query = "SELECT s.first_name, s.last_name, s.middle_initial, d.device_type, d.device_brand, d.device_name, d.mac_address, d.unique_code " +
                "FROM devices d JOIN students s ON d.student_id = s.id";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String mName = rs.getString("middle_initial");
                String ownerName = rs.getString("first_name") + " " + (mName != null && !mName.isEmpty() ? mName + " " : "") + rs.getString("last_name");
                String deviceType = rs.getString("device_type");
                String brandModel = rs.getString("device_brand") + " " + rs.getString("device_name");
                String macAddress = rs.getString("mac_address");
                String accessCode = rs.getString("unique_code");

                devicesList.add(new Device(ownerName, deviceType, brandModel, macAddress, accessCode));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public ObservableList<Student> getStudentsList() { return studentsList; }
    public ObservableList<Device> getDevicesList() { return devicesList; }
    public ObservableList<LogEntry> getMonitoringLogsList() { return monitoringLogsList; }
    public ObservableList<LogEntry> getActiveDevicesList() { return activeDevicesList; }
    public ObservableList<Report> getReportsList() { return reportsList; }
    public ObservableList<SystemUser> getUsersList() { return usersList; }
}