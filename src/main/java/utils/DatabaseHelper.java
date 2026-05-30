package utils;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String URL = dotenv.get("DB_URL");
    private static final String USER = dotenv.get("DB_USER");
    private static final String PASSWORD = dotenv.get("DB_PASSWORD");

    /**
     * Establishes and returns a connection to the Supabase PostgreSQL database.
     */
    public static Connection getConnection() throws SQLException {
        if (URL == null || USER == null || PASSWORD == null) {
            throw new SQLException("Database credentials are missing! Please check your .env file.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Tests the database connection on application startup.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("Successfully connected to the Supabase Cloud Database securely!");
        } catch (SQLException e) {
            System.err.println("Critical Error: Could not connect to Supabase.");
            System.err.println("Please check your internet connection and .env file.");
            System.err.println("Error details: " + e.getMessage());
        }
    }
}