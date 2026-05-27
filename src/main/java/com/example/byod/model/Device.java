package com.example.byod.model;

public class Device {
    private String ownerName;
    private String deviceType;
    private String model;
    private String macAddress;
    private String token;

    public Device(String ownerName, String deviceType, String model, String macAddress, String token) {
        this.ownerName = ownerName;
        this.deviceType = deviceType;
        this.model = model;
        this.macAddress = macAddress;
        this.token = token;
    }

    // Getters required for JavaFX TableView
    public String getOwnerName() { return ownerName; }
    public String getDeviceType() { return deviceType; }
    public String getModel() { return model; }
    public String getMacAddress() { return macAddress; }
    public String getToken() { return token; }
}