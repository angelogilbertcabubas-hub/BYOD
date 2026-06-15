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

    private ObservableList<Student> studentsList = FXCollections.observableArrayList();
    private ObservableList<Device> devicesList = FXCollections.observableArrayList();
    private ObservableList<LogEntry> monitoringLogsList = FXCollections.observableArrayList();
    private ObservableList<LogEntry> allHistoricalLogsList = FXCollections.observableArrayList();
    private ObservableList<LogEntry> activeDevicesList = FXCollections.observableArrayList();
    private ObservableList<Report> reportsList = FXCollections.observableArrayList();
    private ObservableList<SystemUser> usersList = FXCollections.observableArrayList();
    private ObservableList<IncidentReport> incidentReportsList = FXCollections.observableArrayList();

    private DataStore() {
        loadStudentsFromDatabase();
        loadDevicesFromDatabase();
        loadLogsFromDatabase();
        loadActiveDevicesFromDatabase();
        loadIncidentReportsFromDatabase();
    }

    public static DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    public void refreshAll(){
        refreshStudents();
        refreshDevices();
        refreshLogs();
        refreshActiveDevices();
        refreshIncidents();
    }

    public void refreshStudents() { studentsList.clear(); loadStudentsFromDatabase(); }
    public void refreshDevices() { devicesList.clear(); loadDevicesFromDatabase(); }
    public void refreshLogs() {
        monitoringLogsList.clear();
        allHistoricalLogsList.clear();
        loadLogsFromDatabase();
    }
    public void refreshActiveDevices() { activeDevicesList.clear(); loadActiveDevicesFromDatabase(); }
    public void refreshIncidents() { incidentReportsList.clear(); loadIncidentReportsFromDatabase(); }

    private void loadActiveDevicesFromDatabase() {
        String query = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, d.device_brand, d.device_name, d.unique_code, " +
                "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in FROM check_in_out c " +
                "JOIN students s ON c.student_id = s.id JOIN devices d ON c.device_id = d.id " +
                "WHERE c.status = 'CHECKED_IN' AND c.check_out_time IS NULL AND DATE(c.check_in_time) = CURRENT_DATE";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                activeDevicesList.add(new LogEntry(
                        String.valueOf(rs.getObject("log_id")), rs.getString("first_name") + " " + rs.getString("last_name"),
                        rs.getString("school_id"), rs.getString("device_brand") + " " + rs.getString("device_name"),
                        rs.getString("unique_code") != null ? rs.getString("unique_code") : "N/A", "Check-In",
                        rs.getString("formatted_time_in") != null ? rs.getString("formatted_time_in") : "Unknown", "Main Gate"
                ));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadLogsFromDatabase() {
        String todayQuery = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, d.device_brand, d.device_name, d.unique_code, c.status, " +
                "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in, TO_CHAR(c.check_out_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_out " +
                "FROM check_in_out c JOIN students s ON c.student_id = s.id JOIN devices d ON c.device_id = d.id " +
                "WHERE DATE(c.check_in_time) = CURRENT_DATE ORDER BY c.check_in_time DESC LIMIT 150";

        String historyQuery = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, d.device_brand, d.device_name, d.unique_code, c.status, " +
                "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_in, TO_CHAR(c.check_out_time, 'MM/DD/YYYY HH12:MI AM') as formatted_time_out " +
                "FROM check_in_out c JOIN students s ON c.student_id = s.id JOIN devices d ON c.device_id = d.id " +
                "ORDER BY c.check_in_time DESC LIMIT 1500";

        try (Connection conn = DatabaseHelper.getConnection()) {
            try(PreparedStatement pstmt1 = conn.prepareStatement(todayQuery); ResultSet rs = pstmt1.executeQuery()) {
                while (rs.next()) {
                    String logId = String.valueOf(rs.getObject("log_id"));
                    String studentName = rs.getString("first_name") + " " + rs.getString("last_name");
                    String deviceModel = rs.getString("device_brand") + " " + rs.getString("device_name");
                    String accessToken = rs.getString("unique_code") != null ? rs.getString("unique_code") : "N/A";
                    monitoringLogsList.add(new LogEntry(logId + "-IN", studentName, rs.getString("school_id"), deviceModel, accessToken, "Check-In", rs.getString("formatted_time_in"), "Main Gate"));
                    if ("CHECKED_OUT".equals(rs.getString("status")) && rs.getString("formatted_time_out") != null) {
                        monitoringLogsList.add(new LogEntry(logId + "-OUT", studentName, rs.getString("school_id"), deviceModel, accessToken, "Check-Out", rs.getString("formatted_time_out"), "Main Gate"));
                    }
                }
            }
            try(PreparedStatement pstmt2 = conn.prepareStatement(historyQuery); ResultSet rs = pstmt2.executeQuery()) {
                while (rs.next()) {
                    String logId = String.valueOf(rs.getObject("log_id"));
                    String studentName = rs.getString("first_name") + " " + rs.getString("last_name");
                    String deviceModel = rs.getString("device_brand") + " " + rs.getString("device_name");
                    String accessToken = rs.getString("unique_code") != null ? rs.getString("unique_code") : "N/A";
                    allHistoricalLogsList.add(new LogEntry(logId + "-IN", studentName, rs.getString("school_id"), deviceModel, accessToken, "Check-In", rs.getString("formatted_time_in"), "Main Gate"));
                    if ("CHECKED_OUT".equals(rs.getString("status")) && rs.getString("formatted_time_out") != null) {
                        allHistoricalLogsList.add(new LogEntry(logId + "-OUT", studentName, rs.getString("school_id"), deviceModel, accessToken, "Check-Out", rs.getString("formatted_time_out"), "Main Gate"));
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadStudentsFromDatabase() {
        // PERMANENT FIX: Hide archived students from the UI
        String query = "SELECT school_id, first_name, last_name, middle_initial, program_course, email_address, mobile_number, status " +
                "FROM students WHERE status IS NULL OR status != 'ARCHIVED'";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String mName = rs.getString("middle_initial");
                String fullName = rs.getString("first_name") + " " + (mName != null && !mName.isEmpty() ? mName + " " : "") + rs.getString("last_name");
                studentsList.add(new Student(rs.getString("school_id"), fullName, rs.getString("program_course"), rs.getString("email_address"), rs.getString("mobile_number"), rs.getString("status")));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadDevicesFromDatabase() {
        // PERMANENT FIX: Hide devices belonging to archived students
        String query = "SELECT s.school_id, s.first_name, s.last_name, s.middle_initial, d.device_type, d.device_brand, d.device_name, d.mac_address, d.unique_code, d.status " +
                "FROM devices d JOIN students s ON d.student_id = s.id " +
                "WHERE (s.status IS NULL OR s.status != 'ARCHIVED') AND (d.status IS NULL OR d.status != 'ARCHIVED')";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String mName = rs.getString("middle_initial");
                String ownerName = rs.getString("first_name") + " " + (mName != null && !mName.isEmpty() ? mName + " " : "") + rs.getString("last_name");
                String status = rs.getString("status");
                if (status == null) status = "ACTIVE";
                devicesList.add(new Device(rs.getString("school_id"), ownerName, rs.getString("device_type"),
                        rs.getString("device_brand") + " " + rs.getString("device_name"),
                        rs.getString("mac_address"), rs.getString("unique_code"), status));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadIncidentReportsFromDatabase() {
        String query = "SELECT * FROM incident_reports ORDER BY created_at DESC";
        try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                incidentReportsList.add(new IncidentReport(
                        rs.getString("date"), rs.getString("time"), rs.getString("student_number"),
                        rs.getString("device_details"), rs.getString("incident_type"),
                        rs.getString("location"), rs.getString("description")
                ));
            }
        } catch (Exception e) { /* Safely ignore */ }
    }

    public ObservableList<Student> getStudentsList() { return studentsList; }
    public ObservableList<Device> getDevicesList() { return devicesList; }
    public ObservableList<LogEntry> getMonitoringLogsList() { return monitoringLogsList; } // Today
    public ObservableList<LogEntry> getAllHistoricalLogsList() { return allHistoricalLogsList; } // All-Time
    public ObservableList<LogEntry> getActiveDevicesList() { return activeDevicesList; }
    public ObservableList<Report> getReportsList() { return reportsList; }
    public ObservableList<SystemUser> getUsersList() { return usersList; }
    public ObservableList<IncidentReport> getIncidentReportsList() { return incidentReportsList; }
}