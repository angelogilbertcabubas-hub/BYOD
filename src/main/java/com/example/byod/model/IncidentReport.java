package com.example.byod.model;

public class IncidentReport {
    private String date;
    private String time;
    private String studentNumber;
    private String deviceDetails;
    private String incidentType;
    private String location;
    private String description;

    public IncidentReport(String date, String time, String studentNumber, String deviceDetails, String incidentType, String location, String description) {
        this.date = date;
        this.time = time;
        this.studentNumber = studentNumber;
        this.deviceDetails = deviceDetails;
        this.incidentType = incidentType;
        this.location = location;
        this.description = description;
    }

    public String getDate() { return date; }
    public String getTime() { return time; }
    public String getStudentNumber() { return studentNumber; }
    public String getDeviceDetails() { return deviceDetails; }
    public String getIncidentType() { return incidentType; }
    public String getLocation() { return location; }
    public String getDescription() { return description; }
}