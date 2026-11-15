package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Service responsible for computing comprehensive operational reports,
 * logistics cost analysis, drone fleet utilization, resource turnover,
 * and system telemetry metrics.
 */
public class AnalyticsService {
    private static AnalyticsService instance;

    private final ReliefSystem system;
    private final long startTimeMillis;

    private AnalyticsService() {
        this.system = ReliefSystem.getInstance();
        this.startTimeMillis = System.currentTimeMillis();
    }

    public static synchronized AnalyticsService getInstance() {
        if (instance == null) {
            instance = new AnalyticsService();
        }
        return instance;
    }

    /**
     * Computes the complete analytical report payload.
     */
    public synchronized Map<String, Object> generateAnalyticsReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        List<Delivery> allDeliveries = system.getDeliveryManager().getAll();
        List<ReliefRequest> allRequests = system.getRequestManager().getAll();
        List<Drone> allDrones = system.getDroneManager().getAll();
        List<Resource> allResources = system.getResourceManager().getAll();
        List<Location> allLocations = system.getLocationManager().getAll();
        List<Disaster> allDisasters = system.getDisasterManager().getAll();

        // 1. Delivery & Cost Statistics
        int totalDeliveries = allDeliveries.size();
        int completedDeliveries = 0;
        int inTransitDeliveries = 0;
        int pendingDeliveries = 0;
        int cancelledDeliveries = 0;
        double totalCost = 0.0;
        double totalDistanceKm = 0.0;
        double totalResponseTimeMinutes = 0.0;
        int responseTimeCount = 0;

        // Map drone ID -> completed delivery count
        Map<String, Integer> droneMissionCounts = new HashMap<>();

        for (Delivery d : allDeliveries) {
            if (d.getStatus() == DeliveryStatus.COMPLETED) {
                completedDeliveries++;
                totalCost += d.getDeliveryCost();
                totalDistanceKm += d.getTotalDistanceKm();

                droneMissionCounts.put(d.getDroneId(), droneMissionCounts.getOrDefault(d.getDroneId(), 0) + 1);

                ReliefRequest req = system.getRequestManager().getById(d.getRequestId());
                if (req != null && d.getCompletionTime() > 0 && req.getCreatedAt() > 0) {
                    double respTime = (d.getCompletionTime() - req.getCreatedAt()) / (1000.0 * 60.0);
                    if (respTime >= 0) {
                        totalResponseTimeMinutes += respTime;
                        responseTimeCount++;
                    }
                }
            } else if (d.getStatus() == DeliveryStatus.IN_TRANSIT) {
                inTransitDeliveries++;
            } else if (d.getStatus() == DeliveryStatus.PENDING) {
                pendingDeliveries++;
            } else if (d.getStatus() == DeliveryStatus.CANCELLED) {
                cancelledDeliveries++;
            }
        }

        double successRate = totalDeliveries > 0 ? ((double) completedDeliveries / totalDeliveries) * 100.0 : 0.0;
        double avgCost = completedDeliveries > 0 ? (totalCost / completedDeliveries) : 0.0;
        double avgDistance = completedDeliveries > 0 ? (totalDistanceKm / completedDeliveries) : 0.0;
        double avgResponseTime = responseTimeCount > 0 ? (totalResponseTimeMinutes / responseTimeCount) : 0.0;

        Map<String, Object> deliveryMetrics = new LinkedHashMap<>();
        deliveryMetrics.put("totalDeliveries", totalDeliveries);
        deliveryMetrics.put("completedDeliveries", completedDeliveries);
        deliveryMetrics.put("inTransitDeliveries", inTransitDeliveries);
        deliveryMetrics.put("pendingDeliveries", pendingDeliveries);
        deliveryMetrics.put("cancelledDeliveries", cancelledDeliveries);
        deliveryMetrics.put("successRatePercent", Math.round(successRate * 10.0) / 10.0);
        deliveryMetrics.put("totalReliefCost", Math.round(totalCost * 100.0) / 100.0);
        deliveryMetrics.put("avgDeliveryCost", Math.round(avgCost * 100.0) / 100.0);
        deliveryMetrics.put("totalDistanceKm", Math.round(totalDistanceKm * 10.0) / 10.0);
        deliveryMetrics.put("avgDistanceKm", Math.round(avgDistance * 10.0) / 10.0);
        deliveryMetrics.put("avgResponseTimeMinutes", Math.round(avgResponseTime * 10.0) / 10.0);
        report.put("deliveryMetrics", deliveryMetrics);

        // 2. Drone Fleet Utilization & Health
        int totalDrones = allDrones.size();
        int availableDrones = 0;
        int assignedDrones = 0;
        int inFlightDrones = 0;
        int maintenanceDrones = 0;
        double totalBattery = 0.0;

        List<Map<String, Object>> droneStatsList = new ArrayList<>();
        for (Drone drone : allDrones) {
            totalBattery += drone.getBatteryPercentage();
            switch (drone.getStatus()) {
                case AVAILABLE -> availableDrones++;
                case ASSIGNED -> assignedDrones++;
                case IN_FLIGHT -> inFlightDrones++;
                case MAINTENANCE -> maintenanceDrones++;
            }

            Location loc = system.getLocationManager().getById(drone.getCurrentLocationId());
            Map<String, Object> dStat = new LinkedHashMap<>();
            dStat.put("id", drone.getId());
            dStat.put("name", drone.getName());
            dStat.put("status", drone.getStatus().name());
            dStat.put("batteryPercentage", drone.getBatteryPercentage());
            dStat.put("capacityKg", drone.getMaxCapacityKg());
            dStat.put("maxFlightRangeKm", drone.getMaxFlightRangeKm());
            dStat.put("type", drone.getDroneType());
            dStat.put("currentLocationId", drone.getCurrentLocationId());
            dStat.put("currentLocationName", loc != null ? loc.getName() : drone.getCurrentLocationId());
            dStat.put("completedMissions", droneMissionCounts.getOrDefault(drone.getId(), 0));
            droneStatsList.add(dStat);
        }

        int busyDrones = assignedDrones + inFlightDrones;
        double fleetUtilization = totalDrones > 0 ? ((double) busyDrones / totalDrones) * 100.0 : 0.0;
        double avgBattery = totalDrones > 0 ? (totalBattery / totalDrones) : 0.0;

        Map<String, Object> droneMetrics = new LinkedHashMap<>();
        droneMetrics.put("totalDrones", totalDrones);
        droneMetrics.put("availableDrones", availableDrones);
        droneMetrics.put("assignedDrones", assignedDrones);
        droneMetrics.put("inFlightDrones", inFlightDrones);
        droneMetrics.put("maintenanceDrones", maintenanceDrones);
        droneMetrics.put("busyDrones", busyDrones);
        droneMetrics.put("fleetUtilizationPercent", Math.round(fleetUtilization * 10.0) / 10.0);
        droneMetrics.put("avgBatteryPercent", Math.round(avgBattery * 10.0) / 10.0);
        droneMetrics.put("drones", droneStatsList);
        report.put("droneMetrics", droneMetrics);

        // 3. Request Urgency & Status Distribution
        Map<String, Integer> urgencyBreakdown = new LinkedHashMap<>();
        for (UrgencyLevel u : UrgencyLevel.values()) {
            urgencyBreakdown.put(u.name(), 0);
        }
        Map<String, Integer> statusBreakdown = new LinkedHashMap<>();
        for (RequestStatus s : RequestStatus.values()) {
            statusBreakdown.put(s.name(), 0);
        }

        int totalAffectedPeople = 0;
        for (ReliefRequest r : allRequests) {
            urgencyBreakdown.put(r.getUrgency().name(), urgencyBreakdown.get(r.getUrgency().name()) + 1);
            statusBreakdown.put(r.getStatus().name(), statusBreakdown.get(r.getStatus().name()) + 1);
            totalAffectedPeople += r.getAffectedPeople();
        }

        Map<String, Object> requestMetrics = new LinkedHashMap<>();
        requestMetrics.put("totalRequests", allRequests.size());
        requestMetrics.put("totalAffectedPeople", totalAffectedPeople);
        requestMetrics.put("urgencyBreakdown", urgencyBreakdown);
        requestMetrics.put("statusBreakdown", statusBreakdown);
        report.put("requestMetrics", requestMetrics);

        // 4. Resource Consumption & Inventory Turnover
        List<Map<String, Object>> resourceBreakdown = new ArrayList<>();
        int totalDistributedUnits = 0;

        for (Resource res : allResources) {
            int deliveredUnits = 0;
            for (Delivery d : allDeliveries) {
                if (d.getStatus() == DeliveryStatus.COMPLETED) {
                    ReliefRequest req = system.getRequestManager().getById(d.getRequestId());
                    if (req != null && res.getId().equalsIgnoreCase(req.getResourceId())) {
                        deliveredUnits += req.getQuantity();
                    }
                }
            }
            totalDistributedUnits += deliveredUnits;

            int totalStock = res.getAvailableStock() + res.getReservedStock() + deliveredUnits;
            double turnoverRate = totalStock > 0 ? ((double) deliveredUnits / totalStock) * 100.0 : 0.0;

            Map<String, Object> rStat = new LinkedHashMap<>();
            rStat.put("id", res.getId());
            rStat.put("name", res.getName());
            rStat.put("category", res.getCategory().name());
            rStat.put("unit", res.getUnit());
            rStat.put("unitWeightKg", res.getUnitWeightKg());
            rStat.put("availableStock", res.getAvailableStock());
            rStat.put("reservedStock", res.getReservedStock());
            rStat.put("deliveredStock", deliveredUnits);
            rStat.put("totalStock", totalStock);
            rStat.put("turnoverRatePercent", Math.round(turnoverRate * 10.0) / 10.0);
            resourceBreakdown.add(rStat);
        }

        Map<String, Object> resourceMetrics = new LinkedHashMap<>();
        resourceMetrics.put("totalResourceTypes", allResources.size());
        resourceMetrics.put("totalDistributedUnits", totalDistributedUnits);
        resourceMetrics.put("resources", resourceBreakdown);
        report.put("resourceMetrics", resourceMetrics);

        // 5. Disaster Logistics & Cost Breakdown
        List<Map<String, Object>> disasterBreakdown = new ArrayList<>();
        for (Disaster dis : allDisasters) {
            double disasterCost = 0.0;
            int disasterDeliveries = 0;
            int disasterRequests = 0;

            for (Location loc : allLocations) {
                if (dis.getId().equalsIgnoreCase(loc.getDisasterId())) {
                    for (ReliefRequest req : allRequests) {
                        if (loc.getId().equalsIgnoreCase(req.getLocationId())) {
                            disasterRequests++;
                            for (Delivery d : allDeliveries) {
                                if (req.getId().equalsIgnoreCase(d.getRequestId()) && d.getStatus() == DeliveryStatus.COMPLETED) {
                                    disasterCost += d.getDeliveryCost();
                                    disasterDeliveries++;
                                }
                            }
                        }
                    }
                }
            }

            Map<String, Object> disStat = new LinkedHashMap<>();
            disStat.put("id", dis.getId());
            disStat.put("name", dis.getName());
            disStat.put("type", dis.getType().name());
            disStat.put("region", dis.getAffectedRegion());
            disStat.put("severity", dis.getSeverity().name());
            disStat.put("totalCost", Math.round(disasterCost * 100.0) / 100.0);
            disStat.put("completedDeliveries", disasterDeliveries);
            disStat.put("totalRequests", disasterRequests);
            disasterBreakdown.add(disStat);
        }
        report.put("disasterBreakdown", disasterBreakdown);

        // 6. System Telemetry & JVM Diagnostics
        Runtime rt = Runtime.getRuntime();
        long totalMemoryMB = rt.totalMemory() / (1024 * 1024);
        long freeMemoryMB = rt.freeMemory() / (1024 * 1024);
        long usedMemoryMB = totalMemoryMB - freeMemoryMB;
        long maxMemoryMB = rt.maxMemory() / (1024 * 1024);

        long uptimeSeconds = (System.currentTimeMillis() - startTimeMillis) / 1000;
        int activeThreads = ManagementFactory.getThreadMXBean().getThreadCount();

        Map<String, Object> systemTelemetry = new LinkedHashMap<>();
        systemTelemetry.put("javaVersion", System.getProperty("java.version"));
        systemTelemetry.put("javaVendor", System.getProperty("java.vendor"));
        systemTelemetry.put("osName", System.getProperty("os.name"));
        systemTelemetry.put("osArch", System.getProperty("os.arch"));
        systemTelemetry.put("availableProcessors", rt.availableProcessors());
        systemTelemetry.put("usedMemoryMB", usedMemoryMB);
        systemTelemetry.put("freeMemoryMB", freeMemoryMB);
        systemTelemetry.put("totalMemoryMB", totalMemoryMB);
        systemTelemetry.put("maxMemoryMB", maxMemoryMB);
        systemTelemetry.put("uptimeSeconds", uptimeSeconds);
        systemTelemetry.put("activeThreads", activeThreads);

        // Storage files telemetry
        List<Map<String, Object>> storageFiles = new ArrayList<>();
        addStorageFileInfo(storageFiles, "disasters.csv", AppConfig.DISASTERS_FILE, system.getDisasterManager().size());
        addStorageFileInfo(storageFiles, "locations.csv", AppConfig.LOCATIONS_FILE, system.getLocationManager().size());
        addStorageFileInfo(storageFiles, "drones.csv", AppConfig.DRONES_FILE, system.getDroneManager().size());
        addStorageFileInfo(storageFiles, "resources.csv", AppConfig.RESOURCES_FILE, system.getResourceManager().size());
        addStorageFileInfo(storageFiles, "relief_requests.csv", AppConfig.REQUESTS_FILE, system.getRequestManager().size());
        addStorageFileInfo(storageFiles, "routes.csv", AppConfig.ROUTES_FILE, system.getRouteManager().size());
        addStorageFileInfo(storageFiles, "deliveries.csv", AppConfig.DELIVERIES_FILE, system.getDeliveryManager().size());
        systemTelemetry.put("storageFiles", storageFiles);

        report.put("systemTelemetry", systemTelemetry);

        return report;
    }

    private void addStorageFileInfo(List<Map<String, Object>> list, String name, String path, int recordCount) {
        File file = new File(path);
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("fileName", name);
        info.put("filePath", path);
        info.put("recordCount", recordCount);
        info.put("fileSizeBytes", file.exists() ? file.length() : 0);
        info.put("lastModified", file.exists() ? file.lastModified() : 0);
        list.add(info);
    }
}
