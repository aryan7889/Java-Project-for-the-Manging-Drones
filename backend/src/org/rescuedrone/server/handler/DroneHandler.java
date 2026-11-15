package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.algorithm.DroneAllocator;
import org.rescuedrone.model.Drone;
import org.rescuedrone.model.enums.DroneStatus;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.server.JsonUtil;
import org.rescuedrone.service.DroneManager;
import org.rescuedrone.service.ReliefSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/drones fleet management and assignment operations.
 */
public class DroneHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/drones, /api/drones/{id}, /api/drones/{id}/recharge, /api/drones/assign

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path);
            } else if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/recharge-all")) {
                    handleRechargeAll(exchange);
                } else if (path.endsWith("/assign")) {
                    handleAssign(exchange);
                } else {
                    handlePost(exchange);
                }
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (path.endsWith("/recharge-all")) {
                    handleRechargeAll(exchange);
                } else if (path.endsWith("/recharge")) {
                    handleRecharge(exchange, path);
                } else {
                    handlePut(exchange, path);
                }
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
        DroneManager dm = ReliefSystem.getInstance().getDroneManager();
        String query = exchange.getRequestURI().getQuery();

        String[] parts = path.split("/");
        if (parts.length <= 3) {
            if (query != null && query.contains("status=AVAILABLE")) {
                CorsHandler.sendJsonResponse(exchange, 200, dm.getAvailableDrones());
            } else {
                CorsHandler.sendJsonResponse(exchange, 200, dm.getAll());
            }
        } else if (parts.length == 4) {
            String id = parts[3];
            Drone drone = dm.getById(id);
            if (drone != null) {
                CorsHandler.sendJsonResponse(exchange, 200, drone);
            } else {
                CorsHandler.sendError(exchange, 404, "Drone not found with ID: " + id);
            }
        } else {
            CorsHandler.sendError(exchange, 400, "Invalid URL path");
        }
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        DroneManager dm = ReliefSystem.getInstance().getDroneManager();

        String name = (String) data.get("name");
        String droneType = (String) data.get("droneType");
        String locId = (String) data.get("currentLocationId");
        String statusStr = (String) data.get("status");

        double capacity = 10.0;
        int battery = 100;
        double range = 30.0;

        try {
            if (data.containsKey("maxCapacityKg")) capacity = Double.parseDouble(String.valueOf(data.get("maxCapacityKg")));
            if (data.containsKey("batteryPercentage")) battery = Integer.parseInt(String.valueOf(data.get("batteryPercentage")));
            if (data.containsKey("maxFlightRangeKm")) range = Double.parseDouble(String.valueOf(data.get("maxFlightRangeKm")));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for capacity, battery, or range");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Drone name is required");
        }

        // Generate next ID
        String id = "DRN-" + String.format("%03d", dm.size() + 1);
        int counter = 1;
        while (dm.exists(id)) {
            id = "DRN-" + String.format("%03d", dm.size() + counter++);
        }

        Drone drone = new Drone(
                id,
                name.trim(),
                capacity,
                battery,
                range,
                locId != null ? locId.trim() : "LOC-001",
                DroneStatus.fromString(statusStr),
                droneType != null ? droneType.trim() : "Quadcopter"
        );

        dm.add(drone);
        CorsHandler.sendJsonResponse(exchange, 201, drone);
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing drone ID in URL");
            return;
        }
        String id = parts[3];
        DroneManager dm = ReliefSystem.getInstance().getDroneManager();
        Drone existing = dm.getById(id);
        if (existing == null) {
            CorsHandler.sendError(exchange, 404, "Drone not found with ID: " + id);
            return;
        }

        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        if (data.containsKey("name")) existing.setName((String) data.get("name"));
        if (data.containsKey("droneType")) existing.setDroneType((String) data.get("droneType"));
        if (data.containsKey("currentLocationId")) existing.setCurrentLocationId((String) data.get("currentLocationId"));
        if (data.containsKey("status")) existing.setStatus(DroneStatus.fromString((String) data.get("status")));
        if (data.containsKey("maxCapacityKg")) existing.setMaxCapacityKg(Double.parseDouble(String.valueOf(data.get("maxCapacityKg"))));
        if (data.containsKey("batteryPercentage")) existing.setBatteryPercentage(Integer.parseInt(String.valueOf(data.get("batteryPercentage"))));
        if (data.containsKey("maxFlightRangeKm")) existing.setMaxFlightRangeKm(Double.parseDouble(String.valueOf(data.get("maxFlightRangeKm"))));

        dm.update(existing);
        CorsHandler.sendJsonResponse(exchange, 200, existing);
    }

    private void handleRecharge(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        String id = parts[3];
        DroneManager dm = ReliefSystem.getInstance().getDroneManager();
        Drone drone = dm.getById(id);
        if (drone == null) {
            CorsHandler.sendError(exchange, 404, "Drone not found with ID: " + id);
            return;
        }

        drone.recharge();
        dm.update(drone);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("message", "Drone " + id + " recharged to 100% and ready for flight");
        resp.put("drone", drone);
        CorsHandler.sendJsonResponse(exchange, 200, resp);
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        String[] parts = path.split("/");
        if (parts.length < 4) {
            CorsHandler.sendError(exchange, 400, "Missing drone ID in URL");
            return;
        }
        String id = parts[3];
        DroneManager dm = ReliefSystem.getInstance().getDroneManager();
        boolean deleted = dm.delete(id);

        if (deleted) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("message", "Drone " + id + " retired from fleet");
            CorsHandler.sendJsonResponse(exchange, 200, resp);
        } else {
            CorsHandler.sendError(exchange, 404, "Drone not found with ID: " + id);
        }
    }

    private void handleRechargeAll(HttpExchange exchange) throws IOException {
        int count = ReliefSystem.getInstance().getDroneManager().rechargeAll();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", true);
        resp.put("rechargedCount", count);
        resp.put("message", "All fleet drones recharged to 100% and restored to AVAILABLE status.");
        CorsHandler.sendJsonResponse(exchange, 200, resp);
    }

    private void handleAssign(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String requestId = (String) data.get("requestId");
        double distance = 10.0;
        if (data.containsKey("distanceKm")) {
            distance = Double.parseDouble(String.valueOf(data.get("distanceKm")));
        }

        DroneAllocator.AllocationResult result = DroneAllocator.evaluateAndAssign(requestId, distance);
        if (result.isSuccess() && result.getSelectedDrone() != null) {
            try {
                org.rescuedrone.service.DeliveryService.getInstance().createDeliveryForAssignedRequest(
                        result.getRequest(), result.getSelectedDrone(), result.getFlightDistanceKm());
            } catch (Exception ex) {
                System.err.println("[DroneHandler] Error auto-creating delivery record: " + ex.getMessage());
            }
        }
        CorsHandler.sendJsonResponse(exchange, 200, result);
    }
}
