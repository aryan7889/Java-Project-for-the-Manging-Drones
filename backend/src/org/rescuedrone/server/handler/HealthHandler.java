package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.config.AppConfig;
import org.rescuedrone.server.CorsHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check handler responding to /api/health
 */
public class HealthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("status", "UP");
            resp.put("application", AppConfig.APP_NAME);
            resp.put("version", AppConfig.APP_VERSION);
            resp.put("timestamp", System.currentTimeMillis());
            resp.put("javaVersion", System.getProperty("java.version"));
            resp.put("os", System.getProperty("os.name"));
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 405, "Method Not Allowed");
        }
    }
}
