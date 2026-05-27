package com.example.byod.model;

public class Student {
    private String studentId;
    private String fullName;
    private String course;
    private String email;
    private String mobile;
    private String status;

    public Student(String studentId, String fullName, String course, String email, String mobile, String status) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.email = email;
        this.mobile = mobile;
        this.status = status;
    }

    // Getters required for JavaFX TableView to read the data
    public String getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getCourse() { return course; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getStatus() { return status; }
}