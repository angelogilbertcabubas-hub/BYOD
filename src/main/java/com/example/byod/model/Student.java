package com.example.byod.model;

public class Student {

    private String studentId;
    private String fullName;
    private String course;
    private String email;
    private String mobile;
    private String status;
    private int infractionCount;
    private String byodStatus;

    public Student(String studentId, String fullName, String course, String email, String mobile, String status) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.email = email;
        this.mobile = mobile;
        this.status = status;
        this.infractionCount = 0;
        this.byodStatus = "ACTIVE";
    }

    public Student(String studentId, String fullName, String course, String email, String mobile, String status, int infractionCount, String byodStatus) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.email = email;
        this.mobile = mobile;
        this.status = status;
        this.infractionCount = infractionCount;
        this.byodStatus = byodStatus;
    }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getInfractionCount() { return infractionCount; }
    public void setInfractionCount(int infractionCount) { this.infractionCount = infractionCount; }

    public String getByodStatus() { return byodStatus; }
    public void setByodStatus(String byodStatus) { this.byodStatus = byodStatus; }
}