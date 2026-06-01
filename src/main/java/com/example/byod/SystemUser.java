package com.example.byod;

public class SystemUser {
    private String username;
    private String fullName;
    private String role;
    private String status;
    private String actionPlaceholder; // Matches colUserActionControls column

    public SystemUser(String username, String fullName, String role, String status, String actionPlaceholder) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.actionPlaceholder = actionPlaceholder;
    }

    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getActionPlaceholder() { return actionPlaceholder; }
}