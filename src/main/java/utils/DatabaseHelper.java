package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseHelper {

    // This object holds our pool of open, ready-to-use connections
    private static HikariDataSource dataSource;

    // The static block runs once the moment the application starts
    static {
        try {
            Dotenv dotenv = Dotenv.load();

            String url = dotenv.get("DB_URL");
            String user = dotenv.get("DB_USER");
            String password = dotenv.get("DB_PASSWORD");

            if (url == null || user == null || password == null) {
                System.err.println("CRITICAL: Database credentials are missing in the ..env file.");
            }

            // Configure the connection pool
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);

            // Pool Settings for optimal speed
            config.setMaximumPoolSize(10); // Keep up to 10 connections open
            config.setMinimumIdle(2);      // Always keep at least 2 connections ready
            config.setConnectionTimeout(30000); // 30 seconds wait before timing out

            // PostgreSQL/Supabase specific speed optimizations
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            // Initialize the pool
            dataSource = new HikariDataSource(config);
            System.out.println("HikariCP Connection Pool initialized successfully.");

        } catch (Exception e) {
            System.err.println("Critical Error initializing HikariCP Connection Pool: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Instantly returns an already-open connection from the pool.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Connection pool is not initialized.");
        }
        return dataSource.getConnection(); // Grabs a ready connection (0.01 seconds)
    }

    /**
     * Tests the database connection on application startup.
     */
    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            System.out.println("Successfully tested connection to the Supabase Cloud Database!");
        } catch (SQLException e) {
            System.err.println("Critical Error: Could not connect to Supabase.");
            System.err.println("Please check your internet connection and ..env file.");
            System.err.println("Error details: " + e.getMessage());
        }
    }
}