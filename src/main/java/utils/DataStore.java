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
import java.time.format.DateTimeFormatter;

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

        // Automatically fetch real data from Supabase when the app starts
        loadStudentsFromDatabase();
        loadDevicesFromDatabase();
        loadLogsFromDatabase(); // NEW: Load the logs on startup!
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public void refreshLogs() {
        monitoringLogsList.clear();
        loadLogsFromDatabase();
    }

    private void loadLogsFromDatabase() {
        // Fix: Use TO_CHAR to format timestamps exactly at the database level
        String query = "SELECT al.log_id, s.first_name, s.last_name, s.student_number, " +
                "d.brand, d.model, d.access_code, al.status, " +
                "TO_CHAR(al.time_in, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in, " +
                "TO_CHAR(al.time_out, 'MM/DD/YYYY HH12:MI AM') as formatted_time_out " +
                "FROM access_logs al " +
                "JOIN students s ON al.student_number = s.student_number " +
                "LEFT JOIN devices d ON al.device_id = d.device_id " +
                "ORDER BY COALESCE(al.time_out, al.time_in) DESC LIMIT 100";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String logId = String.valueOf(rs.getInt("log_id"));
                String studentName = rs.getString("first_name") + " " + rs.getString("last_name");
                String studentId = rs.getString("student_number");

                String brand = rs.getString("brand");
                String deviceModel = (brand != null) ? brand + " " + rs.getString("model") : "No Device";

                String accessToken = rs.getString("access_code");
                if (accessToken == null) accessToken = "N/A";

                String status = rs.getString("status");
                String operation = "CLEARED".equals(status) ? "Check-Out" : "Check-In";

                // Read the perfectly formatted string directly from the database
                String timestamp = "CLEARED".equals(status) ?
                        rs.getString("formatted_time_out") : rs.getString("formatted_time_in");

                if (timestamp == null) timestamp = "Unknown Time";

                String location = "Main Gate";

                monitoringLogsList.add(new LogEntry(logId, studentName, studentId, deviceModel, accessToken, operation, timestamp, location));
            }
            System.out.println("Successfully loaded " + monitoringLogsList.size() + " logs from Supabase!");

        } catch (Exception e) {
            System.err.println("Failed to fetch logs from database!");
            e.printStackTrace();
        }
    }

    private void loadStudentsFromDatabase() {
        String query = "SELECT student_number, first_name, last_name, course, section, status FROM students";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String studentId = rs.getString("student_number");
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                String course = rs.getString("course");
                String email = rs.getString("section");
                String mobile = "N/A";
                String status = rs.getString("status");
                studentsList.add(new Student(studentId, fullName, course, email, mobile, status));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDevicesFromDatabase() {
        String query = "SELECT s.first_name, s.last_name, d.device_type, d.brand, d.model, d.serial_number, d.access_code " +
                "FROM devices d JOIN students s ON d.student_id = s.student_id";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String ownerName = rs.getString("first_name") + " " + rs.getString("last_name");
                String deviceType = rs.getString("device_type");
                String brandModel = rs.getString("brand") + " " + rs.getString("model");
                String serialNumber = rs.getString("serial_number");
                String accessCode = rs.getString("access_code");
                devicesList.add(new Device(ownerName, deviceType, brandModel, serialNumber, accessCode));
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