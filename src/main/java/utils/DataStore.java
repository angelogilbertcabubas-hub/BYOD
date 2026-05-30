package utils;

import com.example.byod.model.Device;
import com.example.byod.model.Student;
import com.example.byod.model.LogEntry;
import com.example.byod.model.Report;
import com.example.byod.model.SystemUser;
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

        // Automatically fetch real data from Supabase when the app starts
        loadStudentsFromDatabase();
        loadDevicesFromDatabase();
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
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
            System.out.println("Successfully loaded " + studentsList.size() + " students from Supabase!");

        } catch (Exception e) {
            System.err.println("Failed to fetch students from database!");
            e.printStackTrace();
        }
    }

    private void loadDevicesFromDatabase() {
        // We use a JOIN here to get the Student's first and last name along with the device info
        String query = "SELECT s.first_name, s.last_name, d.device_type, d.brand, d.model, d.serial_number, d.access_code " +
                "FROM devices d " +
                "JOIN students s ON d.student_id = s.student_id";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String ownerName = rs.getString("first_name") + " " + rs.getString("last_name");
                String deviceType = rs.getString("device_type");
                String brandModel = rs.getString("brand") + " " + rs.getString("model");
                String serialNumber = rs.getString("serial_number");
                String accessCode = rs.getString("access_code");

                // Add the device straight from the cloud to your JavaFX list
                devicesList.add(new Device(ownerName, deviceType, brandModel, serialNumber, accessCode));
            }
            System.out.println("Successfully loaded " + devicesList.size() + " devices from Supabase!");

        } catch (Exception e) {
            System.err.println("Failed to fetch devices from database!");
            e.printStackTrace();
        }
    }

    // Getters for your JavaFX Controllers
    public ObservableList<Student> getStudentsList() { return studentsList; }
    public ObservableList<Device> getDevicesList() { return devicesList; }
    public ObservableList<LogEntry> getMonitoringLogsList() { return monitoringLogsList; }
    public ObservableList<LogEntry> getActiveDevicesList() { return activeDevicesList; }
    public ObservableList<Report> getReportsList() { return reportsList; }
    public ObservableList<SystemUser> getUsersList() { return usersList; }
}