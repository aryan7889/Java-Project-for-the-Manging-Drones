package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.service.AnalyticsService;
import org.rescuedrone.service.ReliefSystem;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Handler for /api/reports and system operational telemetry.
 */
public class AnalyticsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> report = AnalyticsService.getInstance().generateAnalyticsReport();
                CorsHandler.sendJsonResponse(exchange, 200, report);
            } else if ("POST".equalsIgnoreCase(method) && path.endsWith("/reseed")) {
                ReliefSystem.getInstance().reseedDemoData();
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("message", "Clean simulation demo data reseeded successfully.");
                CorsHandler.sendJsonResponse(exchange, 200, resp);
            } else {
                CorsHandler.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            CorsHandler.sendError(exchange, 500, "Analytics Error: " + e.getMessage());
        }
    }
}
