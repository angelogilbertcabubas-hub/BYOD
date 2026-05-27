package com.example.byod.model;

public class Report {
    private String reportId;
    private String title;
    private String generatedDate;
    private String generatedBy;
    private String actions; // Matches colActions column

    public Report(String reportId, String title, String generatedDate, String generatedBy, String actions) {
        this.reportId = reportId;
        this.title = title;
        this.generatedDate = generatedDate;
        this.generatedBy = generatedBy;
        this.actions = actions;
    }

    public String getReportId() { return reportId; }
    public String getTitle() { return title; }
    public String getGeneratedDate() { return generatedDate; }
    public String getGeneratedBy() { return generatedBy; }
    public String getActions() { return actions; }
}