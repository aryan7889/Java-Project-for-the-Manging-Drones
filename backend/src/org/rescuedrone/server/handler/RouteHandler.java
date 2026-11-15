package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.algorithm.DijkstraOptimizer;
import org.rescuedrone.algorithm.DijkstraOptimizer.PathResult;
import org.rescuedrone.ds.Graph;
import org.rescuedrone.model.Location;
import org.rescuedrone.model.Route;
import org.rescuedrone.model.enums.UrgencyLevel;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.server.JsonUtil;
import org.rescuedrone.service.CostCalculator;
import org.rescuedrone.service.LocationManager;
import org.rescuedrone.service.ReliefSystem;
import org.rescuedrone.service.RouteManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/routes management and /api/routes/optimize Dijkstra shortest-path calculations.
 */
public class RouteHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/routes, /api/routes/optimize, /api/routes/{id}

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path);
            } else if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/optimize")) {
                    handleOptimize(exchange);
                } else {
                    handlePost(exchange);
                }
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
        RouteManager rm = ReliefSystem.getInstance().getRouteManager();

        String[] parts = path.split("/");
        if (parts.length <= 3) {
            CorsHandler.sendJsonResponse(exchange, 200, rm.getAll());
        } else if (parts.length == 4) {
            String id = parts[3];
            Route route = rm.getById(id);
            if (route != null) {
                CorsHandler.sendJsonResponse(exchange, 200, route);
            } else {
                CorsHandler.sendError(exchange, 404, "Route corridor not found with ID: " + id);
            }
        } else {
            CorsHandler.sendError(exchange, 400, "Invalid URL path");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        RouteManager rm = ReliefSystem.getInstance().getRouteManager();
        LocationManager lm = ReliefSystem.getInstance().getLocationManager();

        String src = (String) data.get("sourceLocationId");
        String dst = (String) data.get("destLocationId");
        double distance = 0.0;

        try {
            if (data.containsKey("distanceKm")) {
                distance = Double.parseDouble(String.valueOf(data.get("distanceKm")));
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for route distance");
        }

        if (src == null || dst == null || src.trim().equalsIgnoreCase(dst.trim())) {
            throw new IllegalArgumentException("Source and destination must be distinct non-empty locations");
        }
        if (lm.getById(src) == null || lm.getById(dst) == null) {
            throw new IllegalArgumentException("Both source (" + src + ") and destination (" + dst + ") locations must exist");
        }
        if (distance <= 0.0) {
            throw new IllegalArgumentException("Distance must be positive");
        }

        // Generate next ID
        String id = "RTE-" + String.format("%03d", rm.size() + 1);
        int counter = 1;
        while (rm.exists(id)) {
            id = "RTE-" + String.format("%03d", rm.size() + counter++);
        }

        Route route = new Route(id, src.trim(), dst.trim(), distance);
        rm.add(route);
        CorsHandler.sendJsonResponse(exchange, 201, route);
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing route ID in URL");
            return;
        }
        String id = parts[3];
        RouteManager rm = ReliefSystem.getInstance().getRouteManager();
        Route existing = rm.getById(id);
        if (existing == null) {
            CorsHandler.sendError(exchange, 404, "Route corridor not found with ID: " + id);
            return;
        }

        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        if (data.containsKey("sourceLocationId")) existing.setSourceLocationId((String) data.get("sourceLocationId"));
        if (data.containsKey("destLocationId")) existing.setDestLocationId((String) data.get("destLocationId"));
        if (data.containsKey("distanceKm")) existing.setDistanceKm(Double.parseDouble(String.valueOf(data.get("distanceKm"))));

        rm.update(existing);
        CorsHandler.sendJsonResponse(exchange, 200, existing);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing route ID in URL");
            return;
        }
        String id = parts[3];
        RouteManager rm = ReliefSystem.getInstance().getRouteManager();
        boolean deleted = rm.delete(id);

        if (deleted) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Route corridor " + id + " removed");
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 404, "Route corridor not found with ID: " + id);
        }
    }

    private void handleOptimize(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String sourceId = (String) data.get("sourceId");
        String destId = (String) data.get("destId");

        if (sourceId == null || destId == null) {
            CorsHandler.sendError(exchange, 400, "Both sourceId and destId are required");
            return;
        }

        LocationManager lm = ReliefSystem.getInstance().getLocationManager();
        RouteManager rm = ReliefSystem.getInstance().getRouteManager();

        // Build live graph
        List<String> vertexIds = new ArrayList<>();
        for (Location loc : lm.getAll()) {
            vertexIds.add(loc.getId());
        }
        Graph graph = rm.toGraph(vertexIds);

        // Run Dijkstra
        PathResult result = DijkstraOptimizer.findShortestPath(graph, sourceId, destId);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("found", result.isFound());
        resp.put("sourceId", result.getSourceId());
        resp.put("destId", result.getDestId());
        resp.put("totalDistanceKm", result.getTotalDistanceKm());
        resp.put("pathNodeIds", result.getPathNodeIds());
        resp.put("hopCount", result.getHopCount());
        resp.put("message", result.getMessage());

        if (result.isFound()) {
            // Include cost breakdown (default emergency level for simulation display)
            CostCalculator.CostBreakdown cost = CostCalculator.calculateCost(result.getTotalDistanceKm(), UrgencyLevel.HIGH);
            Map<String, Object> costMap = new LinkedHashMap<>();
            costMap.put("baseCharge", cost.getBaseCharge());
            costMap.put("distanceCharge", cost.getDistanceCharge());
            costMap.put("emergencyCharge", cost.getEmergencyCharge());
            costMap.put("totalCost", cost.getTotalCost());
            resp.put("costBreakdown", costMap);
        }

        CorsHandler.sendJsonResponse(exchange, result.isFound() ? 200 : 404, resp);
    }
}
