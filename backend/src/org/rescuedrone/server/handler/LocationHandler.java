package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.model.Location;
import org.rescuedrone.model.Route;
import org.rescuedrone.model.enums.SeverityLevel;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.server.JsonUtil;
import org.rescuedrone.service.LocationManager;
import org.rescuedrone.service.ReliefSystem;
import org.rescuedrone.service.RouteManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/locations CRUD operations.
 */
public class LocationHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/locations or /api/locations/LOC-001

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
        LocationManager lm = ReliefSystem.getInstance().getLocationManager();
        String query = exchange.getRequestURI().getQuery();

        String[] parts = path.split("/");
        if (parts.length <= 3) {
            // Check for ?disasterId=DIS-001
            if (query != null && query.contains("disasterId=")) {
                String disId = query.split("disasterId=")[1].split("&")[0];
                CorsHandler.sendJsonResponse(exchange, 200, lm.getLocationsByDisaster(disId));
            } else {
                CorsHandler.sendJsonResponse(exchange, 200, lm.getAll());
            }
        } else if (parts.length == 4) {
            String id = parts[3];
            Location loc = lm.getById(id);
            if (loc != null) {
                CorsHandler.sendJsonResponse(exchange, 200, loc);
            } else {
                CorsHandler.sendError(exchange, 404, "Location not found with ID: " + id);
            }
        } else {
            CorsHandler.sendError(exchange, 400, "Invalid URL path");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        LocationManager lm = ReliefSystem.getInstance().getLocationManager();

        String name = (String) data.get("name");
        String disasterId = (String) data.get("disasterId");
        String severityStr = (String) data.get("severity");

        double lat = 0.0;
        double lon = 0.0;
        int population = 0;

        try {
            if (data.containsKey("latitude")) lat = Double.parseDouble(String.valueOf(data.get("latitude")));
            if (data.containsKey("longitude")) lon = Double.parseDouble(String.valueOf(data.get("longitude")));
            if (data.containsKey("affectedPopulation")) population = Integer.parseInt(String.valueOf(data.get("affectedPopulation")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for latitude, longitude, or population");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name is required");
        }

        // Generate next ID
        String id = "LOC-" + String.format("%03d", lm.size() + 1);
        int counter = 1;
        while (lm.exists(id)) {
            id = "LOC-" + String.format("%03d", lm.size() + counter++);
        }

        Location loc = new Location(
                id,
                name.trim(),
                lat,
                lon,
                disasterId != null ? disasterId.trim() : "",
                population,
                SeverityLevel.fromString(severityStr)
        );

        lm.add(loc);
        CorsHandler.sendJsonResponse(exchange, 201, loc);
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing location ID in URL");
            return;
        }
        String id = parts[3];
        LocationManager lm = ReliefSystem.getInstance().getLocationManager();
        Location existing = lm.getById(id);
        if (existing == null) {
            CorsHandler.sendError(exchange, 404, "Location not found with ID: " + id);
            return;
        }

        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        if (data.containsKey("name")) existing.setName((String) data.get("name"));
        if (data.containsKey("latitude")) existing.setLatitude(Double.parseDouble(String.valueOf(data.get("latitude"))));
        if (data.containsKey("longitude")) existing.setLongitude(Double.parseDouble(String.valueOf(data.get("longitude"))));
        if (data.containsKey("disasterId")) existing.setDisasterId((String) data.get("disasterId"));
        if (data.containsKey("affectedPopulation")) existing.setAffectedPopulation(Integer.parseInt(String.valueOf(data.get("affectedPopulation"))));
        if (data.containsKey("severity")) existing.setSeverity(SeverityLevel.fromString((String) data.get("severity")));

        lm.update(existing);
        CorsHandler.sendJsonResponse(exchange, 200, existing);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing location ID in URL");
            return;
        }
        String id = parts[3];
        LocationManager lm = ReliefSystem.getInstance().getLocationManager();
        RouteManager rm = ReliefSystem.getInstance().getRouteManager();

        boolean deleted = lm.delete(id);
        if (deleted) {
            // Clean up associated graph routes connected to this location
            List<Route> routesToRemove = new ArrayList<>();
            for (Route r : rm.getAll()) {
                if (id.equalsIgnoreCase(r.getSourceLocationId()) || id.equalsIgnoreCase(r.getDestLocationId())) {
                    routesToRemove.add(r);
                }
            }
            for (Route r : routesToRemove) {
                rm.delete(r.getId());
            }

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Location " + id + " and associated routes successfully deleted");
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 404, "Location not found with ID: " + id);
        }
    }
}
