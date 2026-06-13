package com.example.byod.model;

public class Device {
    private String studentNumber;
    private String ownerName;
    private String deviceType;
    private String brandModel;
    private String macAddress;
    private String accessCode;

    // 6-Argument Constructor (Used by the updated DataStore for searching)
    public Device(String studentNumber, String ownerName, String deviceType, String brandModel, String macAddress, String accessCode) {
        this.studentNumber = studentNumber;
        this.ownerName = ownerName;
        this.deviceType = deviceType;
        this.brandModel = brandModel;
        this.macAddress = macAddress;
        this.accessCode = accessCode;
    }

    // 5-Argument Constructor (Legacy support for older controllers)
    public Device(String ownerName, String deviceType, String brandModel, String macAddress, String accessCode) {
        this.studentNumber = "N/A";
        this.ownerName = ownerName;
        this.deviceType = deviceType;
        this.brandModel = brandModel;
        this.macAddress = macAddress;
        this.accessCode = accessCode;
    }

    public String getStudentNumber() { return studentNumber; }
    public String getOwnerName() { return ownerName; }
    public String getDeviceType() { return deviceType; }
    public String getBrandModel() { return brandModel; }
    public String getMacAddress() { return macAddress; }
    public String getAccessCode() { return accessCode; }

    public String getModel() { return brandModel; }
    public String getToken() { return accessCode; }

    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public void setBrandModel(String brandModel) { this.brandModel = brandModel; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
}