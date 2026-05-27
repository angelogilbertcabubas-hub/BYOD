package utils;

import com.example.byod.model.Device;
import com.example.byod.model.Student;
import com.example.byod.model.LogEntry;
import com.example.byod.model.Report;
import com.example.byod.model.SystemUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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

        studentsList.add(new Student("2024-0001", "John Doe", "BSIT", "jdoe@iskolar.edu", "09123456789", "Active"));
        studentsList.add(new Student("2024-00504-SR-0", "Ravin James Masarap", "BSIT 2-1", "ravinmasarappogi@gmail.com", "09987654321", "Active"));

        devicesList.add(new Device("John Doe", "Laptop", "Lenovo ThinkPad", "00:1B:44:11:3A:B7", "TKN-8492"));
        devicesList.add(new Device("Ravin James Masarap", "Smartphone", "Iphone 15 Pro Max", "00.10.FA.63.38.4A", "TKN-6517"));

        monitoringLogsList.add(new LogEntry("LOG-1001", "Ravin James Masarap", "2024-00504-SR-0", "Iphone 15 Pro Max", "TKN-6517", "Check-In", "08:15 AM", "Main Gate"));
        monitoringLogsList.add(new LogEntry("LOG-1002", "John Doe", "2024-0001", "Lenovo ThinkPad", "TKN-8492", "Check-In", "08:30 AM", "Main Gate"));
        monitoringLogsList.add(new LogEntry("LOG-1003", "John Doe", "2024-0001", "Lenovo ThinkPad", "TKN-8492", "Check-Out", "12:00 PM", "Main Gate"));

        activeDevicesList.add(new LogEntry("LOG-1001", "Ravin James Masarap", "2024-00504-SR-0", "Iphone 15 Pro Max", "TKN-6517", "Check-In", "08:15 AM", "Main Gate"));

        reportsList.add(new Report("RPT-2026-001", "Monthly Device Traffic Summary", "2026-05-01", "Admin", "View / Print"));
        reportsList.add(new Report("RPT-2026-002", "Unregistered Asset Violation Audit", "2026-05-15", "Admin", "View / Print"));

        usersList.add(new SystemUser("admin_cayenne", "Princess Cayenne M. Rañeses", "Administrator", "Active", "Edit / Delete"));
        usersList.add(new SystemUser("dan_sosa", "Dan Henry Sosa", "Administrator", "Active", "Edit / Delete"));
        usersList.add(new SystemUser("guard_kyle", "Kyle Garcia", "Security Guard", "Active", "Edit / Delete"));
        usersList.add(new SystemUser("maria_s", "Maria Santos", "Security Guard", "Inactive", "Edit / Delete"));
    }

    public static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }
        return instance;
    }

    public ObservableList<Student> getStudentsList() { return studentsList; }
    public ObservableList<Device> getDevicesList() { return devicesList; }
    public ObservableList<LogEntry> getMonitoringLogsList() { return monitoringLogsList; }
    public ObservableList<LogEntry> getActiveDevicesList() { return activeDevicesList; }
    public ObservableList<Report> getReportsList() { return reportsList; }
    public ObservableList<SystemUser> getUsersList() { return usersList; }
}