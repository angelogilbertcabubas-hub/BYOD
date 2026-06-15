package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MidnightResetService {

    private static ScheduledExecutorService scheduler;

    public static void initialize() {
        // Run a sweep immediately on startup
        executeSystemSweep();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        // Check the clock every 30 minutes
        scheduler.scheduleAtFixedRate(() -> {
            LocalTime now = LocalTime.now();
            // Trigger the reset if the time is between 11:30 PM and 11:59 PM
            if (now.getHour() == 23 && now.getMinute() >= 30) {
                executeSystemSweep();
            }
        }, 0, 30, TimeUnit.MINUTES);
    }

    private static void executeSystemSweep() {
        String findQuery = "SELECT id, student_id FROM check_in_out WHERE status = 'CHECKED_IN'";
        String updateQuery = "UPDATE check_in_out " +
                "SET check_out_time = CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Manila', status = 'UNVERIFIED_EXIT' " +
                "WHERE id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement findStmt = conn.prepareStatement(findQuery);
             ResultSet rs = findStmt.executeQuery()) {

            int orphanedLogsClosed = 0;

            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {

                while (rs.next()) {
                    // FIXED: Force these to be Strings to prevent type mismatch errors
                    String logId = rs.getString("id");
                    String studentDbId = rs.getString("student_id");

                    // Trigger the Disciplinary Matrix
                    DatabaseHelper.incrementInfraction(studentDbId);

                    // Queue the log update to batch them for speed
                    updateStmt.setString(1, logId);
                    updateStmt.addBatch();
                    orphanedLogsClosed++;
                }

                if (orphanedLogsClosed > 0) {
                    updateStmt.executeBatch();
                    System.out.println("[SYSTEM AUTO-ROUTINE] Midnight Reset executed successfully. Auto-closed " + orphanedLogsClosed + " orphaned session(s) and issued infractions.");

                    // Force the UI tables to update with the auto-closed statuses
                    DataStore.getInstance().refreshLogs();
                    DataStore.getInstance().refreshActiveDevices();
                }
            }
        } catch (Exception e) {
            System.err.println("Critical Exception during Midnight Auto-Reset: " + e.getMessage());
        }
    }

    public static void shutdown() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}