package com.example.byod.model;

public class Device {
    private String studentNumber;
    private String ownerName;
    private String deviceType;
    private String brandModel;
    private String macAddress;
    private String accessCode;
    private String status; // NEW: Security Flag

    public Device(String studentNumber, String ownerName, String deviceType, String brandModel, String macAddress, String accessCode, String status) {
        this.studentNumber = studentNumber;
        this.ownerName = ownerName;
        this.deviceType = deviceType;
        this.brandModel = brandModel;
        this.macAddress = macAddress;
        this.accessCode = accessCode;
        this.status = (status != null) ? status : "ACTIVE";
    }

    // Legacy Support Constructor
    public Device(String ownerName, String deviceType, String brandModel, String macAddress, String accessCode) {
        this.studentNumber = "N/A";
        this.ownerName = ownerName;
        this.deviceType = deviceType;
        this.brandModel = brandModel;
        this.macAddress = macAddress;
        this.accessCode = accessCode;
        this.status = "ACTIVE";
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getStudentNumber() { return studentNumber; }
    public String getOwnerName() { return ownerName; }
    public String getDeviceType() { return deviceType; }
    public String getBrandModel() { return brandModel; }
    public String getMacAddress() { return macAddress; }
    public String getAccessCode() { return accessCode; }
    public String getStatus() { return status; }

    public String getModel() { return brandModel; }
    public String getToken() { return accessCode; }
}