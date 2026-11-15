package org.rescuedrone.service;

import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.UrgencyLevel;
import org.rescuedrone.persistence.DataSeeder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central system facade coordinating all domain managers,
 * data persistence, and consolidated operational statistics.
 */
public class ReliefSystem {
    private static ReliefSystem instance;

    private final DisasterManager disasterManager;
    private final LocationManager locationManager;
    private final DroneManager droneManager;
    private final ResourceManager resourceManager;
    private final ReliefRequestManager requestManager;
    private final RouteManager routeManager;
    private final DeliveryManager deliveryManager;

    private ReliefSystem() {
        this.disasterManager = new DisasterManager();
        this.locationManager = new LocationManager();
        this.droneManager = new DroneManager();
        this.resourceManager = new ResourceManager();
        this.requestManager = new ReliefRequestManager();
        this.routeManager = new RouteManager();
        this.deliveryManager = new DeliveryManager();
    }

    public static synchronized ReliefSystem getInstance() {
        if (instance == null) {
            instance = new ReliefSystem();
        }
        return instance;
    }

    /**
     * Initializes the system: seeds realistic initial data if empty,
     * and loads records from CSV files into ArrayLists and HashMaps.
     */
    public synchronized void initialize() {
        System.out.println("[ReliefSystem] Initializing data persistence...");
        DataSeeder.seedIfEmpty();

        disasterManager.loadFromFile();
        locationManager.loadFromFile();
        droneManager.loadFromFile();
        resourceManager.loadFromFile();
        requestManager.loadFromFile();
        routeManager.loadFromFile();
        deliveryManager.loadFromFile();

        System.out.println("[ReliefSystem] Loaded records: " +
                disasterManager.size() + " disasters, " +
                locationManager.size() + " locations, " +
                droneManager.size() + " drones, " +
                resourceManager.size() + " resources, " +
                requestManager.size() + " requests (" + requestManager.getPendingCount() + " pending in PriorityQueue), " +
                routeManager.size() + " routes, " +
                deliveryManager.size() + " deliveries.");
    }

    public DisasterManager getDisasterManager() {
        return disasterManager;
    }

    public LocationManager getLocationManager() {
        return locationManager;
    }

    public DroneManager getDroneManager() {
        return droneManager;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public ReliefRequestManager getRequestManager() {
        return requestManager;
    }

    public RouteManager getRouteManager() {
        return routeManager;
    }

    public DeliveryManager getDeliveryManager() {
        return deliveryManager;
    }

    /**
     * Computes real-time dashboard statistics derived strictly from live backend collections.
     */
    public synchronized Map<String, Object> computeDashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        int totalDisasters = disasterManager.size();
        int totalLocations = locationManager.size();
        int totalRequests = requestManager.size();
        int pending = requestManager.getPendingCount();
        int processing = deliveryManager.getActiveDeliveries().size();
        int completed = deliveryManager.getCompletedDeliveries().size();
        int availableDrones = droneManager.getAvailableDrones().size();
        int busyDrones = droneManager.size() - availableDrones;

        int emergencyCount = 0;
        for (ReliefRequest req : requestManager.getAll()) {
            if (req.getUrgency() == UrgencyLevel.CRITICAL || req.getUrgency() == UrgencyLevel.HIGH) {
                emergencyCount++;
            }
        }

        stats.put("totalDisasters", totalDisasters);
        stats.put("totalLocations", totalLocations);
        stats.put("totalRequests", totalRequests);
        stats.put("pendingRequests", pending);
        stats.put("processingRequests", processing);
        stats.put("completedDeliveries", completed);
        stats.put("availableDrones", availableDrones);
        stats.put("busyDrones", busyDrones);
        stats.put("emergencyRequests", emergencyCount);

        // Priority Queue Preview (Top 5 requests strictly from Java PriorityQueue)
        List<Map<String, Object>> pqPreview = new ArrayList<>();
        List<ReliefRequest> orderedQueue = requestManager.getPriorityOrderedPendingRequests();
        int pqLimit = Math.min(5, orderedQueue.size());
        for (int i = 0; i < pqLimit; i++) {
            ReliefRequest req = orderedQueue.get(i);
            Location loc = locationManager.getById(req.getLocationId());
            Resource res = resourceManager.getById(req.getResourceId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", i + 1);
            item.put("id", req.getId());
            item.put("locationId", req.getLocationId());
            item.put("locationName", loc != null ? loc.getName() : req.getLocationId());
            item.put("resourceId", req.getResourceId());
            item.put("resourceName", res != null ? res.getName() : req.getResourceId());
            item.put("quantityRequired", req.getQuantity());
            item.put("urgency", req.getUrgency().name());
            item.put("affectedPeopleCount", req.getAffectedPeople());
            item.put("status", req.getStatus().name());
            item.put("description", req.getDescription());
            pqPreview.add(item);
        }
        stats.put("priorityQueuePreview", pqPreview);

        // Drone Fleet Preview
        List<Map<String, Object>> dronePreview = new ArrayList<>();
        for (Drone d : droneManager.getAll()) {
            Location loc = locationManager.getById(d.getCurrentLocationId());
            Map<String, Object> dm = new LinkedHashMap<>();
            dm.put("id", d.getId());
            dm.put("name", d.getName());
            dm.put("status", d.getStatus().name());
            dm.put("batteryPercentage", d.getBatteryPercentage());
            dm.put("capacityKg", d.getMaxCapacityKg());
            dm.put("maxFlightRangeKm", d.getMaxFlightRangeKm());
            dm.put("currentLocationName", loc != null ? loc.getName() : d.getCurrentLocationId());
            dronePreview.add(dm);
        }
        stats.put("droneFleetPreview", dronePreview);

        // Dynamic Recent Activity feed derived from live entities
        List<Map<String, Object>> activities = new ArrayList<>();
        int actId = 1;

        if (disasterManager.size() > 0) {
            Disaster d0 = disasterManager.getAll().get(0);
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", "ACT-" + (actId++));
            a.put("action", "Disaster Declared");
            a.put("details", d0.getName() + " (" + d0.getSeverity() + " severity) in " + d0.getAffectedRegion());
            a.put("time", "Active");
            activities.add(a);
        }

        ReliefRequest top = requestManager.peekNextPriorityRequest();
        if (top != null) {
            Location loc = locationManager.getById(top.getLocationId());
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", "ACT-" + (actId++));
            a.put("action", "Top Queue Request");
            a.put("details", top.getId() + " at " + (loc != null ? loc.getName() : top.getLocationId()) +
                    " (" + top.getUrgency() + ", " + top.getAffectedPeople() + " people affected)");
            a.put("time", "Pending Dispatch");
            activities.add(a);
        }

        for (Delivery d : deliveryManager.getActiveDeliveries()) {
            Drone drn = droneManager.getById(d.getDroneId());
            Location dest = locationManager.getById(d.getDestLocationId());
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", "ACT-" + (actId++));
            a.put("action", "Mission In-Flight");
            a.put("details", (drn != null ? drn.getName() : d.getDroneId()) + " flying to " +
                    (dest != null ? dest.getName() : d.getDestLocationId()) + " (" + d.getTotalDistanceKm() + " km)");
            a.put("time", "In Transit");
            activities.add(a);
        }

        for (Delivery d : deliveryManager.getCompletedDeliveries()) {
            Location dest = locationManager.getById(d.getDestLocationId());
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", "ACT-" + (actId++));
            a.put("action", "Delivery Successful");
            a.put("details", d.getId() + " reached " + (dest != null ? dest.getName() : d.getDestLocationId()) +
                    " (Fee: ₹" + d.getDeliveryCost() + ")");
            a.put("time", "Delivered");
            activities.add(a);
            if (activities.size() >= 5) break;
        }

        stats.put("recentActivities", activities);
        return stats;
    }

    /**
     * Resets demo data and reloads all domain collections.
     */
    public synchronized void reseedDemoData() {
        System.out.println("[ReliefSystem] Reseeding clean demo data...");
        DataSeeder.seedAll(true);
        initialize();
    }
}
