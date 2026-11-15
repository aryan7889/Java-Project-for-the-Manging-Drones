package org.rescuedrone.service;

import org.rescuedrone.algorithm.DijkstraOptimizer;
import org.rescuedrone.algorithm.DijkstraOptimizer.PathResult;
import org.rescuedrone.algorithm.DroneAllocator;
import org.rescuedrone.algorithm.DroneAllocator.AllocationResult;
import org.rescuedrone.ds.Graph;
import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Service managing the complete lifecycle of disaster relief delivery operations:
 * Mission Planning -> Dispatch -> In-Transit Simulation -> Completion -> Asset Recovery.
 */
public class DeliveryService {
    private static DeliveryService instance;

    private DeliveryService() {}

    public static synchronized DeliveryService getInstance() {
        if (instance == null) {
            instance = new DeliveryService();
        }
        return instance;
    }

    /**
     * Plans and creates a new delivery record for a relief request.
     * Integrates Dijkstra shortest path and drone allocation.
     */
    public synchronized Delivery createDelivery(String requestId, String droneId) {
        ReliefSystem system = ReliefSystem.getInstance();
        ReliefRequest request = system.getRequestManager().getById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("Relief request not found with ID: " + requestId);
        }
        if (request.getStatus() == RequestStatus.DELIVERED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot create delivery for request with status: " + request.getStatus());
        }

        Drone drone;
        if (droneId != null && !droneId.trim().isEmpty()) {
            drone = system.getDroneManager().getById(droneId.trim());
            if (drone == null) {
                throw new IllegalArgumentException("Drone not found with ID: " + droneId);
            }
        } else {
            // Auto-allocate drone via DroneAllocator
            AllocationResult allocRes = DroneAllocator.evaluateAndAssign(requestId, 10.0);
            if (!allocRes.isSuccess()) {
                throw new IllegalStateException("Auto drone allocation failed: " + allocRes.getMessage());
            }
            drone = allocRes.getSelectedDrone();
        }

        String sourceLocId = drone.getCurrentLocationId() != null ? drone.getCurrentLocationId() : "LOC-001";
        String destLocId = request.getLocationId();

        // Build live graph and compute Dijkstra shortest path
        List<String> vertexIds = new ArrayList<>();
        for (Location loc : system.getLocationManager().getAll()) {
            vertexIds.add(loc.getId());
        }
        Graph graph = system.getRouteManager().toGraph(vertexIds);

        PathResult pathResult = DijkstraOptimizer.findShortestPath(graph, sourceLocId, destLocId);
        double distanceKm = pathResult.isFound() ? pathResult.getTotalDistanceKm() : 10.0;
        List<String> pathNodes = pathResult.isFound() ? pathResult.getPathNodeIds() : List.of(sourceLocId, destLocId);

        // Calculate delivery cost
        CostCalculator.CostBreakdown cost = CostCalculator.calculateCost(distanceKm, request.getUrgency());

        // Generate delivery ID
        DeliveryManager dm = system.getDeliveryManager();
        String id = "DEL-" + String.format("%04d", dm.size() + 1);
        int counter = 1;
        while (dm.exists(id)) {
            id = "DEL-" + String.format("%04d", dm.size() + counter++);
        }

        Delivery delivery = new Delivery(
                id,
                request.getId(),
                drone.getId(),
                sourceLocId,
                destLocId,
                pathNodes,
                distanceKm,
                cost.getTotalCost()
        );

        // Update states
        drone.setStatus(DroneStatus.ASSIGNED);
        system.getDroneManager().update(drone);

        request.setStatus(RequestStatus.ASSIGNED);
        system.getRequestManager().update(request);

        // Reserve stock only if custom drone was provided (DroneAllocator already reserves for auto-allocation)
        if (droneId != null && !droneId.trim().isEmpty()) {
            system.getResourceManager().allocateStock(request.getResourceId(), request.getQuantity());
        }

        dm.add(delivery);
        return delivery;
    }

    /**
     * Dispatches a planned delivery mission (IN_TRANSIT).
     */
    public synchronized Delivery startDelivery(String deliveryId) {
        ReliefSystem system = ReliefSystem.getInstance();
        Delivery delivery = system.getDeliveryManager().getById(deliveryId);
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery not found with ID: " + deliveryId);
        }

        delivery.startMission();
        system.getDeliveryManager().update(delivery);

        // Update drone
        Drone drone = system.getDroneManager().getById(delivery.getDroneId());
        if (drone != null) {
            drone.setStatus(DroneStatus.IN_FLIGHT);
            system.getDroneManager().update(drone);
        }

        // Update request
        ReliefRequest req = system.getRequestManager().getById(delivery.getRequestId());
        if (req != null) {
            req.setStatus(RequestStatus.IN_TRANSIT);
            system.getRequestManager().update(req);
        }

        return delivery;
    }

    /**
     * Finalizes delivery completion, consumes inventory, deducts drone battery,
     * updates drone location, and frees drone for subsequent missions.
     */
    public synchronized Delivery completeDelivery(String deliveryId) {
        ReliefSystem system = ReliefSystem.getInstance();
        Delivery delivery = system.getDeliveryManager().getById(deliveryId);
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery not found with ID: " + deliveryId);
        }

        delivery.completeMission();
        system.getDeliveryManager().update(delivery);

        // 1. Update Request
        ReliefRequest req = system.getRequestManager().getById(delivery.getRequestId());
        if (req != null) {
            req.setStatus(RequestStatus.DELIVERED);
            system.getRequestManager().update(req);

            // 2. Permanently consume inventory stock
            system.getResourceManager().consumeStock(req.getResourceId(), req.getQuantity());
        }

        // 3. Update Drone (Location, Battery, Status)
        Drone drone = system.getDroneManager().getById(delivery.getDroneId());
        if (drone != null) {
            drone.consumeBattery(delivery.getTotalDistanceKm());
            drone.setCurrentLocationId(delivery.getDestLocationId());
            // If battery is critically low (< 20%), place in maintenance, otherwise make available
            if (drone.getBatteryPercentage() < 20) {
                drone.setStatus(DroneStatus.MAINTENANCE);
            } else {
                drone.setStatus(DroneStatus.AVAILABLE);
            }
            system.getDroneManager().update(drone);
        }

        return delivery;
    }

    /**
     * Cancels an active or planned delivery mission, releasing reserved assets.
     */
    public synchronized Delivery cancelDelivery(String deliveryId) {
        ReliefSystem system = ReliefSystem.getInstance();
        Delivery delivery = system.getDeliveryManager().getById(deliveryId);
        if (delivery == null) {
            throw new IllegalArgumentException("Delivery not found with ID: " + deliveryId);
        }

        delivery.cancelMission();
        system.getDeliveryManager().update(delivery);

        // 1. Restore Request to PENDING or CANCELLED
        ReliefRequest req = system.getRequestManager().getById(delivery.getRequestId());
        if (req != null) {
            req.setStatus(RequestStatus.CANCELLED);
            system.getRequestManager().update(req);

            // 2. Release reserved stock back to available pool
            system.getResourceManager().releaseStock(req.getResourceId(), req.getQuantity());
        }

        // 3. Restore Drone to AVAILABLE
        Drone drone = system.getDroneManager().getById(delivery.getDroneId());
        if (drone != null) {
            drone.setStatus(DroneStatus.AVAILABLE);
            system.getDroneManager().update(drone);
        }

        return delivery;
    }
}
