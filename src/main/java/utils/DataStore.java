package utils;

import com.example.byod.model.Device;
import com.example.byod.model.Student;
import com.example.byod.model.IncidentReport;
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
    private ObservableList<LogEntry> activeDevicesList; // The list we are fixing!
    private ObservableList<Report> reportsList;
    private ObservableList<SystemUser> usersList;
    private ObservableList<IncidentReport> incidentReportsList;

    private DataStore() {
        studentsList = FXCollections.observableArrayList();
        devicesList = FXCollections.observableArrayList();
        monitoringLogsList = FXCollections.observableArrayList();
        activeDevicesList = FXCollections.observableArrayList();
        reportsList = FXCollections.observableArrayList();
        usersList = FXCollections.observableArrayList();
        incidentReportsList = FXCollections.observableArrayList();

        loadStudentsFromDatabase();
        loadDevicesFromDatabase();
        loadLogsFromDatabase();
        loadActiveDevicesFromDatabase(); // Automatically load active devices on startup!
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public void refreshStudents() { studentsList.clear(); loadStudentsFromDatabase(); }
    public void refreshDevices() { devicesList.clear(); loadDevicesFromDatabase(); }
    public void refreshLogs() { monitoringLogsList.clear(); loadLogsFromDatabase(); }

    // NEW: Triggers a fresh pull of Active Devices
    public void refreshActiveDevices() { activeDevicesList.clear(); loadActiveDevicesFromDatabase(); }

    // NEW: The missing query that grabs ONLY devices inside the campus today
    private void loadActiveDevicesFromDatabase() {
        String query = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, " +
                "d.device_brand, d.device_name, d.unique_code, " +
                "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in " +
                "FROM check_in_out c " +
                "JOIN students s ON c.student_id = s.id " +
                "JOIN devices d ON c.device_id = d.id " +
                "WHERE c.status = 'CHECKED_IN' AND c.check_out_time IS NULL " +
                "AND DATE(c.check_in_time) = CURRENT_DATE";

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

                activeDevicesList.add(new LogEntry(
                        logId, studentName, studentId, deviceModel, accessCode,
                        "Check-In", timeIn, "Main Gate"
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadLogsFromDatabase() {
        String query = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, " +
                "d.device_brand, d.device_name, d.unique_code, c.status, " +
                "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in, " +
                "TO_CHAR(c.check_out_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_out " +
                "FROM check_in_out c " +
                "JOIN students s ON c.student_id = s.id " +
                "JOIN devices d ON c.device_id = d.id " +
                "WHERE DATE(c.check_in_time) = CURRENT_DATE " +
                "ORDER BY c.check_in_time DESC LIMIT 150";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String logId = String.valueOf(rs.getObject("log_id"));
                String studentName = rs.getString("first_name") + " " + rs.getString("last_name");
                String studentId = rs.getString("school_id");
                String brand = rs.getString("device_brand");
                String deviceModel = (brand != null) ? brand + " " + rs.getString("device_name") : "No Device";
                String accessToken = rs.getString("unique_code") != null ? rs.getString("unique_code") : "N/A";
                String status = rs.getString("status");
                String timeIn = rs.getString("formatted_time_in");
                String timeOut = rs.getString("formatted_time_out");

                monitoringLogsList.add(new LogEntry(
                        logId + "-IN", studentName, studentId, deviceModel, accessToken,
                        "Check-In", (timeIn != null ? timeIn : "Unknown Time"), "Main Gate"
                ));

                if ("CHECKED_OUT".equals(status) && timeOut != null) {
                    monitoringLogsList.add(new LogEntry(
                            logId + "-OUT", studentName, studentId, deviceModel, accessToken,
                            "Check-Out", timeOut, "Main Gate"
                    ));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadStudentsFromDatabase() {
        String query = "SELECT school_id, first_name, last_name, middle_initial, program_course, email_address, mobile_number, status FROM students";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String mName = rs.getString("middle_initial");
                String fullName = rs.getString("first_name") + " " + (mName != null && !mName.isEmpty() ? mName + " " : "") + rs.getString("last_name");
                studentsList.add(new Student(rs.getString("school_id"), fullName, rs.getString("program_course"), rs.getString("email_address"), rs.getString("mobile_number"), rs.getString("status")));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDevicesFromDatabase() {
        String query = "SELECT s.school_id, s.first_name, s.last_name, s.middle_initial, d.device_type, d.device_brand, d.device_name, d.mac_address, d.unique_code " +
                "FROM devices d JOIN students s ON d.student_id = s.id";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String studentNumber = rs.getString("school_id");
                String mName = rs.getString("middle_initial");
                String ownerName = rs.getString("first_name") + " " + (mName != null && !mName.isEmpty() ? mName + " " : "") + rs.getString("last_name");

                devicesList.add(new Device(studentNumber, ownerName, rs.getString("device_type"),
                        rs.getString("device_brand") + " " + rs.getString("device_name"),
                        rs.getString("mac_address"), rs.getString("unique_code")));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public ObservableList<Student> getStudentsList() { return studentsList; }
    public ObservableList<Device> getDevicesList() { return devicesList; }
    public ObservableList<LogEntry> getMonitoringLogsList() { return monitoringLogsList; }
    public ObservableList<LogEntry> getActiveDevicesList() { return activeDevicesList; } // Now this actually has data!
    public ObservableList<Report> getReportsList() { return reportsList; }
    public ObservableList<SystemUser> getUsersList() { return usersList; }
    public ObservableList<IncidentReport> getIncidentReportsList() { return incidentReportsList; }
}