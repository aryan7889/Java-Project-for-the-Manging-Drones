package org.rescuedrone;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.server.HttpServerManager;
import org.rescuedrone.service.ReliefSystem;

import java.io.File;
import java.io.IOException;

/**
 * Entry point for RescueDrone disaster management backend.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Initializing " + AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION + "...");

        // Ensure data directory exists
        File dataDir = new File(AppConfig.DATA_DIR);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("Created data directory at: " + dataDir.getAbsolutePath());
            }
        }

        // Initialize Relief System: File Persistence, Collections & PriorityQueue
        ReliefSystem.getInstance().initialize();

        int port = AppConfig.SERVER_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port parameter, falling back to default " + port);
            }
        }

        try {
            HttpServerManager serverManager = new HttpServerManager(port);
            serverManager.start();

            // Register shutdown hook for graceful exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down RescueDrone server...");
                serverManager.stop();
            }));

        } catch (IOException e) {
            System.err.println("Fatal: Failed to start HTTP server on port " + port + ": " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
