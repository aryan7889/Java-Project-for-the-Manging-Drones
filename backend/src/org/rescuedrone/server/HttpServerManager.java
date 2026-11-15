package org.rescuedrone.server;

import com.sun.net.httpserver.HttpServer;
import org.rescuedrone.config.AppConfig;
import org.rescuedrone.server.handler.DashboardStatsHandler;
import org.rescuedrone.server.handler.HealthHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Manages the embedded Java HTTP Server and its endpoint routing.
 */
public class HttpServerManager {
    private HttpServer server;
    private final int port;
    private ThreadPoolExecutor threadPool;

    public HttpServerManager(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.setExecutor(threadPool);

        // Register core endpoints
        registerHandlers();

        server.start();
        System.out.println("=================================================");
        System.out.println("  RescueDrone Java REST Server Started Successfully");
        System.out.println("  Port: " + port);
        System.out.println("  Health URL: http://localhost:" + port + "/api/health");
        System.out.println("  Dashboard Stats: http://localhost:" + port + "/api/dashboard/stats");
        System.out.println("=================================================");
    }

    private void registerHandlers() {
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/dashboard/stats", new DashboardStatsHandler());
        server.createContext("/api/disasters", new org.rescuedrone.server.handler.DisasterHandler());
        server.createContext("/api/locations", new org.rescuedrone.server.handler.LocationHandler());
        server.createContext("/api/resources", new org.rescuedrone.server.handler.ResourceHandler());
        server.createContext("/api/requests", new org.rescuedrone.server.handler.RequestHandler());
        server.createContext("/api/drones", new org.rescuedrone.server.handler.DroneHandler());
        server.createContext("/api/routes", new org.rescuedrone.server.handler.RouteHandler());
        server.createContext("/api/deliveries", new org.rescuedrone.server.handler.DeliveryHandler());
        server.createContext("/api/history", new org.rescuedrone.server.handler.DeliveryHandler());
        server.createContext("/api/reports", new org.rescuedrone.server.handler.AnalyticsHandler());
        server.createContext("/api/system/reseed", new org.rescuedrone.server.handler.AnalyticsHandler());
    }

    public HttpServer getServer() {
        return server;
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            if (threadPool != null) {
                threadPool.shutdown();
            }
            System.out.println("RescueDrone Java REST Server stopped.");
        }
    }
}
