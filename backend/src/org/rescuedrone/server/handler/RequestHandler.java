package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.model.Location;
import org.rescuedrone.model.ReliefRequest;
import org.rescuedrone.model.Resource;
import org.rescuedrone.model.enums.RequestStatus;
import org.rescuedrone.model.enums.UrgencyLevel;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.server.JsonUtil;
import org.rescuedrone.service.LocationManager;
import org.rescuedrone.service.ReliefRequestManager;
import org.rescuedrone.service.ReliefSystem;
import org.rescuedrone.service.ResourceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/requests endpoints and PriorityQueue dispatch operations.
 */
public class RequestHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/requests, /api/requests/queue, /api/requests/{id}

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path);
            } else if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/assign")) {
                    handleAssign(exchange);
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
        ReliefRequestManager rm = ReliefSystem.getInstance().getRequestManager();
        String query = exchange.getRequestURI().getQuery();

        String[] parts = path.split("/");

        if (parts.length <= 3) {
            // GET /api/requests (with optional ?status=... or ?urgency=...)
            List<ReliefRequest> all = rm.getAll();
            if (query != null) {
                if (query.contains("status=")) {
                    String statusStr = query.split("status=")[1].split("&")[0];
                    RequestStatus status = RequestStatus.fromString(statusStr);
                    List<ReliefRequest> filtered = new ArrayList<>();
                    for (ReliefRequest r : all) {
                        if (r.getStatus() == status) filtered.add(r);
                    }
                    CorsHandler.sendJsonResponse(exchange, 200, filtered);
                    return;
                }
            }
            CorsHandler.sendJsonResponse(exchange, 200, all);
        } else if (parts.length == 4 && "queue".equalsIgnoreCase(parts[3])) {
            // GET /api/requests/queue -> Returns PriorityQueue ordered pending requests
            List<ReliefRequest> queue = rm.getPriorityOrderedPendingRequests();
            CorsHandler.sendJsonResponse(exchange, 200, queue);
        } else if (parts.length == 4 && "next".equalsIgnoreCase(parts[3])) {
            // GET /api/requests/next -> Peek highest priority pending request
            ReliefRequest next = rm.peekNextPriorityRequest();
            if (next != null) {
                CorsHandler.sendJsonResponse(exchange, 200, next);
            } else {
                CorsHandler.sendJsonResponse(exchange, 200, "null");
            }
        } else if (parts.length == 4) {
            // GET /api/requests/{id}
            String id = parts[3];
            ReliefRequest req = rm.getById(id);
            if (req != null) {
                CorsHandler.sendJsonResponse(exchange, 200, req);
            } else {
                CorsHandler.sendError(exchange, 404, "Relief request not found with ID: " + id);
            }
        } else {
            CorsHandler.sendError(exchange, 400, "Invalid URL path");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        ReliefRequestManager rm = ReliefSystem.getInstance().getRequestManager();
        LocationManager lm = ReliefSystem.getInstance().getLocationManager();
        ResourceManager resMgr = ReliefSystem.getInstance().getResourceManager();

        String locationId = (String) data.get("locationId");
        String resourceId = (String) data.get("resourceId");
        String urgencyStr = (String) data.get("urgency");
        String description = (String) data.get("description");

        int quantity = 1;
        int people = 0;

        try {
            if (data.containsKey("quantity")) quantity = Integer.parseInt(String.valueOf(data.get("quantity")));
            if (data.containsKey("affectedPeople")) people = Integer.parseInt(String.valueOf(data.get("affectedPeople")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for quantity or affected people");
        }

        if (locationId == null || locationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Target location is required");
        }
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Required resource is required");
        }

        // Verify location exists
        Location loc = lm.getById(locationId);
        if (loc == null) {
            throw new IllegalArgumentException("Location not found with ID: " + locationId);
        }

        // Verify resource exists
        Resource res = resMgr.getById(resourceId);
        if (res == null) {
            throw new IllegalArgumentException("Resource not found with ID: " + resourceId);
        }

        // Generate next ID
        String id = "REQ-" + String.format("%03d", rm.size() + 1);
        int counter = 1;
        while (rm.exists(id)) {
            id = "REQ-" + String.format("%03d", rm.size() + counter++);
        }

        long now = System.currentTimeMillis();
        ReliefRequest req = new ReliefRequest(
                id,
                locationId.trim(),
                resourceId.trim(),
                quantity,
                UrgencyLevel.fromString(urgencyStr),
                people,
                description != null ? description.trim() : "",
                RequestStatus.PENDING,
                now,
                now
        );

        rm.add(req);
        CorsHandler.sendJsonResponse(exchange, 201, req);
    }

    private void handleAssign(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String requestId = (String) data.get("requestId");
        double distance = 10.0;
        if (data.containsKey("distanceKm")) {
            distance = Double.parseDouble(String.valueOf(data.get("distanceKm")));
        }

        org.rescuedrone.algorithm.DroneAllocator.AllocationResult result =
                org.rescuedrone.algorithm.DroneAllocator.evaluateAndAssign(requestId, distance);

        if (result.isSuccess() && result.getSelectedDrone() != null) {
            try {
                org.rescuedrone.service.DeliveryService.getInstance().createDeliveryForAssignedRequest(
                        result.getRequest(), result.getSelectedDrone(), result.getFlightDistanceKm());
            } catch (Exception ex) {
                System.err.println("[RequestHandler] Error auto-creating delivery record: " + ex.getMessage());
            }
        }

        CorsHandler.sendJsonResponse(exchange, 200, result);
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing request ID in URL");
            return;
        }
        String id = parts[3];
        ReliefRequestManager rm = ReliefSystem.getInstance().getRequestManager();
        ReliefRequest existing = rm.getById(id);
        if (existing == null) {
            CorsHandler.sendError(exchange, 404, "Relief request not found with ID: " + id);
            return;
        }

        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        if (data.containsKey("status")) {
            existing.setStatus(RequestStatus.fromString((String) data.get("status")));
        }
        if (data.containsKey("urgency")) {
            existing.setUrgency(UrgencyLevel.fromString((String) data.get("urgency")));
        }
        if (data.containsKey("quantity")) {
            existing.setQuantity(Integer.parseInt(String.valueOf(data.get("quantity"))));
        }
        if (data.containsKey("affectedPeople")) {
            existing.setAffectedPeople(Integer.parseInt(String.valueOf(data.get("affectedPeople"))));
        }
        if (data.containsKey("description")) {
            existing.setDescription((String) data.get("description"));
        }

        rm.update(existing);
        CorsHandler.sendJsonResponse(exchange, 200, existing);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing request ID in URL");
            return;
        }
        String id = parts[3];
        ReliefRequestManager rm = ReliefSystem.getInstance().getRequestManager();
        boolean deleted = rm.delete(id);

        if (deleted) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Relief request " + id + " deleted");
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 404, "Relief request not found with ID: " + id);
        }
    }
}
