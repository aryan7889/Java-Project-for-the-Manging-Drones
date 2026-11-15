package org.rescuedrone.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.rescuedrone.model.Delivery;
import org.rescuedrone.model.enums.DeliveryStatus;
import org.rescuedrone.server.CorsHandler;
import org.rescuedrone.server.JsonUtil;
import org.rescuedrone.service.DeliveryManager;
import org.rescuedrone.service.DeliveryService;
import org.rescuedrone.service.ReliefSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles /api/deliveries and /api/history mission management endpoints.
 */
public class DeliveryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (CorsHandler.handlePreflight(exchange)) return;

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath(); // /api/deliveries, /api/deliveries/history, /api/deliveries/start, etc.

        try {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(exchange, path);
            } else if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/start")) {
                    handleStart(exchange);
                } else if (path.endsWith("/complete")) {
                    handleComplete(exchange);
                } else if (path.endsWith("/cancel")) {
                    handleCancel(exchange);
                } else {
                    handleCreate(exchange);
                }
            } else {
                CorsHandler.sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (IllegalArgumentException e) {
            CorsHandler.sendError(exchange, 400, e.getMessage());
        } catch (IllegalStateException e) {
            CorsHandler.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            CorsHandler.sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        DeliveryManager dm = ReliefSystem.getInstance().getDeliveryManager();
        String query = exchange.getRequestURI().getQuery();

        String[] parts = path.split("/");

        // Check if requesting history (/api/deliveries/history or /api/history)
        if (path.contains("/history")) {
            List<Delivery> history = new ArrayList<>();
            for (Delivery d : dm.getAll()) {
                if (d.getStatus() == DeliveryStatus.COMPLETED || d.getStatus() == DeliveryStatus.CANCELLED) {
                    history.add(d);
                }
            }

            // Apply query filters
            if (query != null) {
                if (query.contains("droneId=")) {
                    String dId = query.split("droneId=")[1].split("&")[0];
                    history.removeIf(d -> !d.getDroneId().equalsIgnoreCase(dId));
                }
                if (query.contains("status=")) {
                    String s = query.split("status=")[1].split("&")[0];
                    history.removeIf(d -> !d.getStatus().name().equalsIgnoreCase(s));
                }
                if (query.contains("destId=")) {
                    String dest = query.split("destId=")[1].split("&")[0];
                    history.removeIf(d -> !d.getDestLocationId().equalsIgnoreCase(dest));
                }
            }
            CorsHandler.sendJsonResponse(exchange, 200, history);
            return;
        }

        if (parts.length <= 3) {
            // GET /api/deliveries -> returns active deliveries
            CorsHandler.sendJsonResponse(exchange, 200, dm.getActiveDeliveries());
        } else if (parts.length == 4) {
            String id = parts[3];
            Delivery delivery = dm.getById(id);
            if (delivery != null) {
                CorsHandler.sendJsonResponse(exchange, 200, delivery);
            } else {
                CorsHandler.sendError(exchange, 404, "Delivery record not found with ID: " + id);
            }
        } else {
            CorsHandler.sendError(exchange, 400, "Invalid URL path");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String requestId = (String) data.get("requestId");
        String droneId = (String) data.get("droneId");

        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Relief request ID is required to plan a delivery");
        }

        Delivery delivery = DeliveryService.getInstance().createDelivery(requestId.trim(), droneId);
        CorsHandler.sendJsonResponse(exchange, 201, delivery);
    }

    private void handleStart(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String deliveryId = (String) data.get("deliveryId");
        if (deliveryId == null || deliveryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery ID is required to dispatch mission");
        }

        Delivery delivery = DeliveryService.getInstance().startDelivery(deliveryId.trim());
        CorsHandler.sendJsonResponse(exchange, 200, delivery);
    }

    private void handleComplete(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String deliveryId = (String) data.get("deliveryId");
        if (deliveryId == null || deliveryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery ID is required to complete mission");
        }

        Delivery delivery = DeliveryService.getInstance().completeDelivery(deliveryId.trim());
        CorsHandler.sendJsonResponse(exchange, 200, delivery);
    }

    private void handleCancel(HttpExchange exchange) throws IOException {
        String body = CorsHandler.readRequestBody(exchange);
        Map<String, Object> data = JsonUtil.parseJsonObject(body);

        String deliveryId = (String) data.get("deliveryId");
        if (deliveryId == null || deliveryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Delivery ID is required to cancel mission");
        }

        Delivery delivery = DeliveryService.getInstance().cancelDelivery(deliveryId.trim());
        CorsHandler.sendJsonResponse(exchange, 200, delivery);
    }
}
