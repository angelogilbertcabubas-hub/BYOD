package com.example.byod.dao;

import com.example.byod.model.Student;
import utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class StudentDAO {

    public static void insertStudent(Student student) throws SQLException {
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            // Updated to match your original database schema columns
            String sql = "INSERT INTO students (student_number, first_name, last_name, course, status) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {

                // 1. Break the full combined name back into First and Last names
                // Expects format: "LastName, FirstName" or "LastName, FirstName MiddleName"
                String fullName = student.getFullName();
                String lastName = "";
                String firstName = "";

                if (fullName != null && fullName.contains(",")) {
                    String[] parts = fullName.split(",", 2);
                    lastName = parts[0].trim();
                    firstName = parts[1].trim();
                } else {
                    firstName = fullName != null ? fullName.trim() : "";
                }

                // 2. Map fields to your original database columns
                stmt.setString(1, student.getStudentId()); // Maps to student_number
                stmt.setString(2, firstName);              // Maps to first_name
                stmt.setString(3, lastName);               // Maps to last_name
                stmt.setString(4, student.getCourse());    // Maps to course
                stmt.setString(5, student.getStatus().toLowerCase()); // Maps to status ('active')

                stmt.executeUpdate();
                conn.commit(); // 🔑 ensures persistence
            } catch (SQLException e) {
                conn.rollback(); // Rollback if something goes wrong
                throw e;
            }
        }
    }
}