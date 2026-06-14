package com.example.byod;

public class LogEntry {
    private String logId;
    private String studentName;
    private String studentId;
    private String deviceModel;
    private String accessToken;
    private String operation;
    private String timestamp;
    private String location;
    private String status; // NEW: The Security State (ACTIVE, COMPROMISED, RECOVERED)

    // Legacy Constructor (Defaults to ACTIVE so old data doesn't break)
    public LogEntry(String logId, String studentName, String studentId, String deviceModel, String accessToken, String operation, String timestamp, String location) {
        this.logId = logId;
        this.studentName = studentName;
        this.studentId = studentId;
        this.deviceModel = deviceModel;
        this.accessToken = accessToken;
        this.operation = operation;
        this.timestamp = timestamp;
        this.location = location;
        this.status = "ACTIVE";
    }

    // NEW Constructor: For injecting strict security alerts
    public LogEntry(String logId, String studentName, String studentId, String deviceModel, String accessToken, String operation, String timestamp, String location, String status) {
        this.logId = logId;
        this.studentName = studentName;
        this.studentId = studentId;
        this.deviceModel = deviceModel;
        this.accessToken = accessToken;
        this.operation = operation;
        this.timestamp = timestamp;
        this.location = location;
        this.status = status;
    }

    // Getters required for JavaFX reflection matching
    public String getLogId() { return logId; }
    public String getStudentName() { return studentName; }
    public String getStudentId() { return studentId; }
    public String getDeviceModel() { return deviceModel; }
    public String getAccessToken() { return accessToken; }
    public String getOperation() { return operation; }
    public String getTimestamp() { return timestamp; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
}