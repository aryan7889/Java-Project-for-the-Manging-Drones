package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.model.Disaster;
import org.rescuedrone.model.Location;
import org.rescuedrone.model.enums.DisasterType;
import org.rescuedrone.model.enums.SeverityLevel;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.server.JsonUtil;
import org.rescuedrone.service.DisasterManager;
import org.rescuedrone.service.LocationManager;
import org.rescuedrone.service.ReliefSystem;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/disasters CRUD operations.
 */
public class DisasterHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // e.g., /api/disasters or /api/disasters/DIS-001 or /api/disasters/DIS-001/locations

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(exchange);
            } else if ("PUT".equalsIgnoreCase(method)) {
                handlePut(exchange, path);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                handleDelete(exchange, path);
            } else {
                CorsHandler.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (IllegalArgumentException e) {
            CorsHandler.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            CorsHandler.sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        DisasterManager dm = ReliefSystem.getInstance().getDisasterManager();
        LocationManager lm = ReliefSystem.getInstance().getLocationManager();

        String[] parts = path.split("/");
        if (parts.length <= 3) {
            // GET /api/disasters
            CorsHandler.sendJsonResponse(exchange, 200, dm.getAll());
        } else if (parts.length == 4) {
            // GET /api/disasters/{id}
            String id = parts[3];
            Disaster disaster = dm.getById(id);
            if (disaster != null) {
                CorsHandler.sendJsonResponse(exchange, 200, disaster);
            } else {
                CorsHandler.sendError(exchange, 404, "Disaster not found with ID: " + id);
            }
        } else if (parts.length == 5 && "locations".equalsIgnoreCase(parts[4])) {
            // GET /api/disasters/{id}/locations
            String id = parts[3];
            List<Location> locs = lm.getLocationsByDisaster(id);
            CorsHandler.sendJsonResponse(exchange, 200, locs);
        } else {
            CorsHandler.sendError(exchange, 400, "Invalid URL path");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        DisasterManager dm = ReliefSystem.getInstance().getDisasterManager();

        String name = (String) data.get("name");
        String typeStr = (String) data.get("type");
        String region = (String) data.get("affectedRegion");
        String severityStr = (String) data.get("severity");
        String description = (String) data.get("description");
        String date = (String) data.get("date");

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Disaster name is required");
        }
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("Affected region is required");
        }

        // Generate next ID
        String id = "DIS-" + String.format("%03d", dm.size() + 1);
        int counter = 1;
        while (dm.exists(id)) {
            id = "DIS-" + String.format("%03d", dm.size() + counter++);
        }

        Disaster disaster = new Disaster(
                id,
                name.trim(),
                DisasterType.fromString(typeStr),
                region.trim(),
                SeverityLevel.fromString(severityStr),
                description != null ? description.trim() : "",
                date != null ? date.trim() : java.time.LocalDate.now().toString()
        );

        dm.add(disaster);
        CorsHandler.sendJsonResponse(exchange, 201, disaster);
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing disaster ID in URL");
            return;
        }
        String id = parts[3];
        DisasterManager dm = ReliefSystem.getInstance().getDisasterManager();
        Disaster existing = dm.getById(id);
        if (existing == null) {
            CorsHandler.sendError(exchange, 404, "Disaster not found with ID: " + id);
            return;
        }

        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        if (data.containsKey("name")) existing.setName((String) data.get("name"));
        if (data.containsKey("type")) existing.setType(DisasterType.fromString((String) data.get("type")));
        if (data.containsKey("affectedRegion")) existing.setAffectedRegion((String) data.get("affectedRegion"));
        if (data.containsKey("severity")) existing.setSeverity(SeverityLevel.fromString((String) data.get("severity")));
        if (data.containsKey("description")) existing.setDescription((String) data.get("description"));
        if (data.containsKey("date")) existing.setDate((String) data.get("date"));

        dm.update(existing);
        CorsHandler.sendJsonResponse(exchange, 200, existing);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing disaster ID in URL");
            return;
        }
        String id = parts[3];
        DisasterManager dm = ReliefSystem.getInstance().getDisasterManager();
        boolean deleted = dm.delete(id);

        if (deleted) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Disaster " + id + " successfully deleted");
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 404, "Disaster not found with ID: " + id);
        }
    }
}
