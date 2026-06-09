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

    // --- GETTERS --- (Required for JavaFX TableView)
    public String getStudentId() { return studentId; }
    public String getFullName() { return fullName; }
    public String getCourse() { return course; }
    public String getEmail() { return email; }
    public String getMobile() { return mobile; }
    public String getStatus() { return status; }

    // --- SETTERS --- (Required for Editing/Updating Data)
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}