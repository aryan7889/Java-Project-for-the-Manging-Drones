package org.rescuedrone.service;

import org.rescuedrone.algorithm.DijkstraOptimizer;
import org.rescuedrone.algorithm.DijkstraOptimizer.PathResult;
import org.rescuedrone.algorithm.DroneAllocator;
import org.rescuedrone.algorithm.DroneAllocator.AllocationResult;
import org.rescuedrone.ds.Graph;
import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        DeliveryManager dm = system.getDeliveryManager();

        // Check if an active delivery already exists for this request (idempotent guard)
        for (Delivery d : dm.getActiveDeliveries()) {
            if (d.getRequestId().equalsIgnoreCase(requestId)) {
                return d;
            }
        }

        Drone drone;
        boolean stockAlreadyAllocated = (request.getStatus() == RequestStatus.ASSIGNED);

        if (droneId != null && !droneId.trim().isEmpty()) {
            drone = system.getDroneManager().getById(droneId.trim());
            if (drone == null) {
                throw new IllegalArgumentException("Drone not found with ID: " + droneId);
            }
        } else if (request.getStatus() == RequestStatus.ASSIGNED) {
            // Request was already assigned - look for the assigned drone or an available one
            Set<String> activeDroneIds = new HashSet<>();
            for (Delivery d : dm.getActiveDeliveries()) {
                activeDroneIds.add(d.getDroneId());
            }
            Drone foundDrone = null;
            for (Drone d : system.getDroneManager().getAll()) {
                if (d.getStatus() == DroneStatus.ASSIGNED && !activeDroneIds.contains(d.getId())) {
                    foundDrone = d;
                    break;
                }
            }
            if (foundDrone == null) {
                for (Drone d : system.getDroneManager().getAll()) {
                    if (d.getStatus() == DroneStatus.AVAILABLE) {
                        foundDrone = d;
                        break;
                    }
                }
            }
            if (foundDrone == null) {
                throw new IllegalStateException("No available or assigned drone found for request " + requestId);
            }
            drone = foundDrone;
        } else {
            // Auto-allocate drone via DroneAllocator (Request is PENDING)
            AllocationResult allocRes = DroneAllocator.evaluateAndAssign(requestId, 10.0);
            if (!allocRes.isSuccess()) {
                throw new IllegalStateException("Auto drone allocation failed: " + allocRes.getMessage());
            }
            drone = allocRes.getSelectedDrone();
            stockAlreadyAllocated = true; // DroneAllocator reserves inventory during evaluateAndAssign
        }

        String sourceLocId = drone.getCurrentLocationId() != null && !drone.getCurrentLocationId().isEmpty()
                ? drone.getCurrentLocationId() : "LOC-001";
        String destLocId = request.getLocationId();

        // Build live graph and compute Dijkstra shortest path
        Graph graph = system.getRouteManager().buildGraph();

        PathResult pathResult = DijkstraOptimizer.findShortestPath(graph, sourceLocId, destLocId);
        double distanceKm = (pathResult != null && pathResult.isFound()) ? pathResult.getTotalDistanceKm() : 10.0;
        List<String> pathNodes = (pathResult != null && pathResult.isFound() && pathResult.getPathNodeIds() != null && !pathResult.getPathNodeIds().isEmpty())
                ? pathResult.getPathNodeIds() : List.of(sourceLocId, destLocId);

        // Calculate delivery cost
        CostCalculator.CostBreakdown cost = CostCalculator.calculateCost(distanceKm, request.getUrgency());

        // Generate delivery ID
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

        // Reserve stock if not already reserved
        if (!stockAlreadyAllocated) {
            system.getResourceManager().allocateStock(request.getResourceId(), request.getQuantity());
        }

        dm.add(delivery);
        return delivery;
    }

    /**
     * Creates and registers a Delivery mission record for a request and drone
     * that were just successfully paired by the allocation engine.
     */
    public synchronized Delivery createDeliveryForAssignedRequest(ReliefRequest request, Drone drone, double precomputedDistanceKm) {
        if (request == null || drone == null) return null;

        ReliefSystem system = ReliefSystem.getInstance();
        DeliveryManager dm = system.getDeliveryManager();

        // Check if an active delivery already exists for this request
        for (Delivery d : dm.getActiveDeliveries()) {
            if (d.getRequestId().equalsIgnoreCase(request.getId())) {
                return d;
            }
        }

        String sourceLocId = drone.getCurrentLocationId() != null && !drone.getCurrentLocationId().isEmpty()
                ? drone.getCurrentLocationId() : "LOC-001";
        String destLocId = request.getLocationId();

        // Build live graph and compute Dijkstra shortest path
        Graph graph = system.getRouteManager().buildGraph();
        PathResult pathResult = DijkstraOptimizer.findShortestPath(graph, sourceLocId, destLocId);

        double distanceKm = (pathResult != null && pathResult.isFound()) ? pathResult.getTotalDistanceKm() :
                (precomputedDistanceKm > 0 ? precomputedDistanceKm : 10.0);
        List<String> pathNodes = (pathResult != null && pathResult.isFound() && pathResult.getPathNodeIds() != null && !pathResult.getPathNodeIds().isEmpty())
                ? pathResult.getPathNodeIds() : List.of(sourceLocId, destLocId);

        // Calculate delivery cost
        CostCalculator.CostBreakdown cost = CostCalculator.calculateCost(distanceKm, request.getUrgency());

        // Generate delivery ID
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
