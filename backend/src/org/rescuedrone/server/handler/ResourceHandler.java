package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.model.Resource;
import org.rescuedrone.model.enums.ResourceCategory;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.server.JsonUtil;
import org.rescuedrone.service.ReliefSystem;
import org.rescuedrone.service.ResourceManager;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles /api/resources inventory and stock allocation operations.
 */
public class ResourceHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/resources or /api/resources/RES-001 or /api/resources/allocate

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path);
            } else if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/allocate")) {
                    handleAllocate(exchange);
                } else if (path.endsWith("/release")) {
                    handleRelease(exchange);
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
        ResourceManager rm = ReliefSystem.getInstance().getResourceManager();
        String query = exchange.getRequestURI().getQuery();

        String[] parts = path.split("/");
        if (parts.length <= 3) {
            if (query != null && query.contains("lowStock=true")) {
                CorsHandler.sendJsonResponse(exchange, 200, rm.getLowStockResources());
            } else {
                CorsHandler.sendJsonResponse(exchange, 200, rm.getAll());
            }
        } else if (parts.length == 4) {
            String id = parts[3];
            Resource res = rm.getById(id);
            if (res != null) {
                CorsHandler.sendJsonResponse(exchange, 200, res);
            } else {
                CorsHandler.sendError(exchange, 404, "Resource not found with ID: " + id);
            }
        } else {
            CorsHandler.sendError(exchange, 400, "Invalid URL path");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        ResourceManager rm = ReliefSystem.getInstance().getResourceManager();

        String name = (String) data.get("name");
        String categoryStr = (String) data.get("category");
        String unit = (String) data.get("unit");

        int stock = 0;
        double weight = 1.0;

        try {
            if (data.containsKey("availableStock")) stock = Integer.parseInt(String.valueOf(data.get("availableStock")));
            if (data.containsKey("unitWeightKg")) weight = Double.parseDouble(String.valueOf(data.get("unitWeightKg")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for stock or unit weight");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name is required");
        }

        // Generate next ID
        String id = "RES-" + String.format("%03d", rm.size() + 1);
        int counter = 1;
        while (rm.exists(id)) {
            id = "RES-" + String.format("%03d", rm.size() + counter++);
        }

        Resource res = new Resource(
                id,
                name.trim(),
                ResourceCategory.fromString(categoryStr),
                stock,
                0,
                unit != null ? unit.trim() : "units",
                weight
        );

        rm.add(res);
        CorsHandler.sendJsonResponse(exchange, 201, res);
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing resource ID in URL");
            return;
        }
        String id = parts[3];
        ResourceManager rm = ReliefSystem.getInstance().getResourceManager();
        Resource existing = rm.getById(id);
        if (existing == null) {
            CorsHandler.sendError(exchange, 404, "Resource not found with ID: " + id);
            return;
        }

        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        // Check if adjusting stock by amount (e.g., adding +50 supplies)
        if (data.containsKey("stockAdjustment")) {
            int adjustment = Integer.parseInt(String.valueOf(data.get("stockAdjustment")));
            int newStock = Math.max(0, existing.getAvailableStock() + adjustment);
            existing.setAvailableStock(newStock);
        } else {
            if (data.containsKey("name")) existing.setName((String) data.get("name"));
            if (data.containsKey("category")) existing.setCategory(ResourceCategory.fromString((String) data.get("category")));
            if (data.containsKey("availableStock")) existing.setAvailableStock(Integer.parseInt(String.valueOf(data.get("availableStock"))));
            if (data.containsKey("unit")) existing.setUnit((String) data.get("unit"));
            if (data.containsKey("unitWeightKg")) existing.setUnitWeightKg(Double.parseDouble(String.valueOf(data.get("unitWeightKg"))));
        }

        rm.update(existing);
        CorsHandler.sendJsonResponse(exchange, 200, existing);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing resource ID in URL");
            return;
        }
        String id = parts[3];
        ResourceManager rm = ReliefSystem.getInstance().getResourceManager();
        boolean deleted = rm.delete(id);

        if (deleted) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Resource " + id + " successfully deleted");
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 404, "Resource not found with ID: " + id);
        }
    }

    private void handleAllocate(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String resourceId = (String) data.get("resourceId");
        int quantity = Integer.parseInt(String.valueOf(data.get("quantity")));

        ResourceManager rm = ReliefSystem.getInstance().getResourceManager();
        boolean success = rm.allocateStock(resourceId, quantity);

        if (success) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("resource", rm.getById(resourceId));
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 400, "Insufficient stock or invalid resource ID for allocation");
        }
    }

    private void handleRelease(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String resourceId = (String) data.get("resourceId");
        int quantity = Integer.parseInt(String.valueOf(data.get("quantity")));

        ResourceManager rm = ReliefSystem.getInstance().getResourceManager();
        rm.releaseStock(resourceId, quantity);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("resource", rm.getById(resourceId));
        CorsHandler.sendJsonResponse(exchange, 200, resp);
    }
}
