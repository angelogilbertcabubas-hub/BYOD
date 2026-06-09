package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
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
            System.out.println("Successfully tested connection to the Local PostgreSQL Database!");
        } catch (SQLException e) {
            System.err.println("Critical Error: Could not connect to Local PostgreSQL.");
            System.err.println("Please check your database service and .env file.");
            System.err.println("Error details: " + e.getMessage());
        }
    }
}