package com.example.byod.model;

public class Device {
    private String ownerName;
    private String deviceType;
    private String brandModel;
    private String macAddress;
    private String accessCode;

    public Device(String ownerName, String deviceType, String brandModel, String macAddress, String accessCode) {
        this.ownerName = ownerName;
        this.deviceType = deviceType;
        this.brandModel = brandModel;
        this.macAddress = macAddress;
        this.accessCode = accessCode;
    }

    // --- NEW GETTERS (Used by the Student Profile Modal) ---
    public String getOwnerName() { return ownerName; }
    public String getDeviceType() { return deviceType; }
    public String getBrandModel() { return brandModel; }
    public String getMacAddress() { return macAddress; }
    public String getAccessCode() { return accessCode; }

    // --- LEGACY ALIAS GETTERS (Fixes the DevicesController crash) ---
    public String getModel() { return brandModel; }
    public String getToken() { return accessCode; }

    // --- SETTERS ---
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    public void setBrandModel(String brandModel) { this.brandModel = brandModel; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
    public void setAccessCode(String accessCode) { this.accessCode = accessCode; }
}