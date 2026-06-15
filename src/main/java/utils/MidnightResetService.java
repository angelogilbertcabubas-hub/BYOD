package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MidnightResetService {

    private static ScheduledExecutorService scheduler;

    public static void initialize() {
        // Run a sweep immediately on startup to catch anything missed while the app was closed
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
        String sweepQuery = "UPDATE check_in_out " +
                "SET check_out_time = CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Manila', status = 'CHECKED_OUT' " +
                "WHERE status = 'CHECKED_IN' AND DATE(check_in_time AT TIME ZONE 'Asia/Manila') < CURRENT_DATE";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement ps = conn.prepareStatement(sweepQuery)) {

            int orphanedLogsClosed = ps.executeUpdate();

            if (orphanedLogsClosed > 0) {
                System.out.println("[SYSTEM AUTO-ROUTINE] Midnight Reset executed successfully. Auto-closed " + orphanedLogsClosed + " orphaned session(s).");
                // Force the UI tables to update with the auto-closed statuses
                DataStore.getInstance().refreshLogs();
                DataStore.getInstance().refreshActiveDevices();
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