package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.service.ReliefSystem;

import java.io.IOException;
import java.util.Map;

/**
 * Handles /api/dashboard/stats
 * Provides live consolidated statistics computed directly from backend data managers.
 */
public class DashboardStatsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, Object> stats = ReliefSystem.getInstance().computeDashboardStats();
            CorsHandler.sendJsonResponse(exchange, 200, stats);
        } else {
            CorsHandler.sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
