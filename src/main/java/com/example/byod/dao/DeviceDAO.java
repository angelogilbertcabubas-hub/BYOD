package com.example.byod.dao;

import com.example.byod.model.Device;
import utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeviceDAO {

    public static void insertDevice(Device device) throws SQLException {
        try (Connection conn = DatabaseHelper.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Resolve the owner name from the UI to an existing database student integer ID
            int resolvedStudentId = getStudentIdByName(conn, device.getOwnerName());

            if (resolvedStudentId == -1) {
                throw new SQLException("Data Integrity Error: No student record found matching the name '" + device.getOwnerName() + "'. Make sure the student is registered first.");
            }

            // 2. Map variables to match your exact structural schema layout for 'devices'
            String sql = "INSERT INTO devices (student_id, device_type, model, serial_number, access_code, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, resolvedStudentId);                     // Maps foreign key relation
                stmt.setString(2, device.getDeviceType());              // Maps to device_type
                stmt.setString(3, device.getModel());                   // Maps to model
                stmt.setString(4, device.getMacAddress());              // Using MAC Address as unique serial_number
                stmt.setString(5, device.getToken());          // Maps to access_code (e.g., TKN-XXXX)
                stmt.setString(6, "active");                            // Default fallback status

                stmt.executeUpdate();
                conn.commit(); // 🔑 ensures persistence across execution instances
            } catch (SQLException e) {
                conn.rollback(); // Rollback complete block transaction if insert processing aborts
                throw e;
            }
        }
    }

    /**
     * Helper method to map text name inputs from the UI into a valid primary key integer
     * matching the PostgreSQL relational structural rules.
     */
    private static int getStudentIdByName(Connection conn, String ownerName) throws SQLException {
        // Fix: Removed 'full_name = ?' to match your original database column structures
        String sql = "SELECT student_id FROM students WHERE " +
                "CONCAT(last_name, ', ', first_name) = ? " +
                "OR student_number = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ownerName != null ? ownerName.trim() : "");
            stmt.setString(2, ownerName != null ? ownerName.trim() : "");

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("student_id");
                }
            }
        }
        return -1; // Fallback indicating key target mismatch
    }
}