package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseHelper {

    private static HikariDataSource dataSource;

    static {
        try {
            Dotenv dotenv = Dotenv.load();

            String url = dotenv.get("DB_URL");
            String user = dotenv.get("DB_USER");
            String password = dotenv.get("DB_PASSWORD");

            if (url == null || user == null || password == null) {
                System.err.println("CRITICAL: Database credentials are missing in the .env file.");
            }

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            System.out.println("HikariCP Connection Pool initialized successfully.");

        } catch (Exception e) {
            System.err.println("Critical Error initializing HikariCP Connection Pool: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool is not initialized.");
        }
        return dataSource.getConnection();
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("Successfully tested connection to the Supabase Cloud Database!");
        } catch (SQLException e) {
            System.err.println("Critical Error: Could not connect to Supabase.");
            System.err.println("Please check your internet connection and .env file.");
            System.err.println("Error details: " + e.getMessage());
        }
    }

    public static int getInfractionCount(String schoolId) {
        String query = "SELECT infraction_count FROM students WHERE school_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, schoolId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("infraction_count");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean isStudentRestricted(String schoolId) {
        String query = "SELECT status FROM students WHERE school_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, schoolId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return "RESTRICTED".equalsIgnoreCase(rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void incrementInfraction(String studentDbId) {
        String querySelect = "SELECT infraction_count, email FROM students WHERE id = ?";
        String queryUpdate = "UPDATE students SET infraction_count = ?, status = ? WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmtSelect = conn.prepareStatement(querySelect)) {

            pstmtSelect.setString(1, studentDbId);
            try (ResultSet rs = pstmtSelect.executeQuery()) {
                if (rs.next()) {
                    int currentCount = rs.getInt("infraction_count");
                    String studentEmail = rs.getString("email");
                    int newCount = currentCount + 1;
                    String newStatus = "ACTIVE";

                    if (newCount == 2) {
                        newStatus = "WARNING";
                        if (EmailHelper.isConfigured()) {
                            EmailHelper.sendEmail(studentEmail, "BYOD Final Warning", "You failed to check out your device. This is your final warning.");
                        }
                    } else if (newCount >= 3) {
                        newStatus = "RESTRICTED";
                        if (EmailHelper.isConfigured()) {
                            EmailHelper.sendEmail(studentEmail, "BYOD Disciplinary Hold", "You failed to check out your device 3 times. Please secure clearance from the Dean of Student Affairs.");
                        }
                    }

                    try (PreparedStatement pstmtUpdate = conn.prepareStatement(queryUpdate)) {
                        pstmtUpdate.setInt(1, newCount);
                        pstmtUpdate.setString(2, newStatus);
                        pstmtUpdate.setString(3, studentDbId);
                        pstmtUpdate.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}