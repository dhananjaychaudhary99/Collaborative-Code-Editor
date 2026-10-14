package com.editor.sync;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CleanupScheduler {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Storage-Cleaner-Thread");
        t.setDaemon(true);
        return t;
    });

    public static void startDailyCleanup() {
        // Run check every 24 hours
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("[Retention Manager] Running periodic 30-day expiry check...");
                DocumentStorage.loadLocally(); // Expired hone par andar se hi deleteLocally() trigger karega
            } catch (Exception e) {
                System.err.println("Cleanup error: " + e.getMessage());
            }
        }, 1, 24, TimeUnit.HOURS);
    }
}