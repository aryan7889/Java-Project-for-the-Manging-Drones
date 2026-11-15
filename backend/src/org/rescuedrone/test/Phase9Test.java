package org.rescuedrone.test;

import org.rescuedrone.model.enums.UrgencyLevel;
import org.rescuedrone.service.AnalyticsService;
import org.rescuedrone.service.ReliefSystem;

import java.util.List;
import java.util.Map;

/**
 * Verification test suite for Phase 9: Reports, Operational Analytics & UI Telemetry.
 */
public class Phase9Test {

    private static int testsPassed = 0;
    private static int testsTotal = 0;

    private static void assertTrue(boolean condition, String message) {
        testsTotal++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + message);
        } else {
            System.err.println("  [FAIL] " + message);
            throw new AssertionError("Test failed: " + message);
        }
    }

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  RescueDrone Phase 9 Reports & Analytics Tests  ");
        System.out.println("=================================================");

        ReliefSystem.getInstance().initialize();

        testAnalyticsServiceDeliveryAndCostCalculations();
        testFleetUtilizationAndBatteryTelemetry();
        testRequestUrgencyAndResourceTurnoverMetrics();
        testJvmAndCsvStorageTelemetry();
        testDashboardConsolidatedRealtimeStatistics();
        testReseedDemoBaselinePersistence();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    @SuppressWarnings("unchecked")
    private static void testAnalyticsServiceDeliveryAndCostCalculations() {
        System.out.println("\n-- Testing Delivery Logistics & Cost Analytics --");
        AnalyticsService analytics = AnalyticsService.getInstance();
        Map<String, Object> report = analytics.generateAnalyticsReport();

        assertTrue(report != null, "Analytics report generated successfully");
        assertTrue(report.containsKey("deliveryMetrics"), "Report contains deliveryMetrics");

        Map<String, Object> del = (Map<String, Object>) report.get("deliveryMetrics");
        int totalDel = (int) del.get("totalDeliveries");
        int compDel = (int) del.get("completedDeliveries");
        double totalCost = (double) del.get("totalReliefCost");
        double avgCost = (double) del.get("avgDeliveryCost");
        double totalDist = (double) del.get("totalDistanceKm");
        double successRate = (double) del.get("successRatePercent");

        assertTrue(totalDel >= 1, "Total deliveries tracked (>= 1): " + totalDel);
        assertTrue(compDel >= 1, "Completed deliveries tracked (>= 1): " + compDel);
        assertTrue(totalCost > 0, "Total relief expenditure calculated (> 0): ₹" + totalCost);
        assertTrue(avgCost > 0, "Average delivery cost calculated (> 0): ₹" + avgCost);
        assertTrue(totalDist > 0, "Total distance flown calculated (> 0): " + totalDist + " km");
        assertTrue(successRate >= 0.0 && successRate <= 100.0, "Success rate bounded [0, 100%]: " + successRate + "%");
    }

    @SuppressWarnings("unchecked")
    private static void testFleetUtilizationAndBatteryTelemetry() {
        System.out.println("\n-- Testing Drone Fleet Utilization & Telemetry --");
        AnalyticsService analytics = AnalyticsService.getInstance();
        Map<String, Object> report = analytics.generateAnalyticsReport();

        assertTrue(report.containsKey("droneMetrics"), "Report contains droneMetrics");
        Map<String, Object> droneMetrics = (Map<String, Object>) report.get("droneMetrics");

        int totalDrones = (int) droneMetrics.get("totalDrones");
        double utilPct = (double) droneMetrics.get("fleetUtilizationPercent");
        double avgBat = (double) droneMetrics.get("avgBatteryPercent");
        List<Map<String, Object>> dronesList = (List<Map<String, Object>>) droneMetrics.get("drones");

        assertTrue(totalDrones == 4, "Total registered drones count matches (4)");
        assertTrue(utilPct >= 0.0 && utilPct <= 100.0, "Fleet utilization rate bounded [0, 100%]: " + utilPct + "%");
        assertTrue(avgBat > 0.0 && avgBat <= 100.0, "Average fleet battery valid: " + avgBat + "%");
        assertTrue(dronesList.size() == 4, "Detailed drone telemetry list contains all 4 aircraft");

        Map<String, Object> d1 = dronesList.get(0);
        assertTrue(d1.containsKey("name") && d1.containsKey("batteryPercentage"), "Individual drone telemetry contains name and battery");
        assertTrue(d1.containsKey("currentLocationName"), "Drone telemetry resolved stationed location name");
    }

    @SuppressWarnings("unchecked")
    private static void testRequestUrgencyAndResourceTurnoverMetrics() {
        System.out.println("\n-- Testing Request Urgency & Inventory Turnover --");
        AnalyticsService analytics = AnalyticsService.getInstance();
        Map<String, Object> report = analytics.generateAnalyticsReport();

        assertTrue(report.containsKey("requestMetrics"), "Report contains requestMetrics");
        Map<String, Object> reqMetrics = (Map<String, Object>) report.get("requestMetrics");
        Map<String, Integer> urgencyBreakdown = (Map<String, Integer>) reqMetrics.get("urgencyBreakdown");

        for (UrgencyLevel u : UrgencyLevel.values()) {
            assertTrue(urgencyBreakdown.containsKey(u.name()), "Urgency breakdown contains tier: " + u.name());
        }

        assertTrue(report.containsKey("resourceMetrics"), "Report contains resourceMetrics");
        Map<String, Object> resMetrics = (Map<String, Object>) report.get("resourceMetrics");
        List<Map<String, Object>> resList = (List<Map<String, Object>>) resMetrics.get("resources");

        assertTrue(resList.size() >= 5, "Resource metrics tracked for all 5 catalog resources");
        Map<String, Object> r1 = resList.get(0);
        assertTrue(r1.containsKey("availableStock") && r1.containsKey("reservedStock") && r1.containsKey("deliveredStock"),
                "Resource contains available, reserved, and delivered stock breakdowns");
    }

    @SuppressWarnings("unchecked")
    private static void testJvmAndCsvStorageTelemetry() {
        System.out.println("\n-- Testing JVM Runtime & CSV Persistence Telemetry --");
        AnalyticsService analytics = AnalyticsService.getInstance();
        Map<String, Object> report = analytics.generateAnalyticsReport();

        assertTrue(report.containsKey("systemTelemetry"), "Report contains systemTelemetry");
        Map<String, Object> telem = (Map<String, Object>) report.get("systemTelemetry");

        assertTrue(telem.get("javaVersion") != null, "Java version detected: " + telem.get("javaVersion"));
        assertTrue(telem.get("osName") != null, "OS detected: " + telem.get("osName"));
        assertTrue((long) telem.get("totalMemoryMB") > 0, "JVM committed memory (> 0 MB): " + telem.get("totalMemoryMB") + " MB");
        assertTrue((int) telem.get("activeThreads") > 0, "Active threads monitored: " + telem.get("activeThreads"));

        List<Map<String, Object>> files = (List<Map<String, Object>>) telem.get("storageFiles");
        assertTrue(files.size() == 7, "All 7 persistence CSV files audited in telemetry");
    }

    @SuppressWarnings("unchecked")
    private static void testDashboardConsolidatedRealtimeStatistics() {
        System.out.println("\n-- Testing Central Dashboard Consolidated Statistics --");
        ReliefSystem system = ReliefSystem.getInstance();
        Map<String, Object> stats = system.computeDashboardStats();

        // 9 Mandatory Section 3 Dashboard counters
        assertTrue(stats.containsKey("totalDisasters"), "Contains totalDisasters");
        assertTrue(stats.containsKey("totalLocations"), "Contains totalLocations");
        assertTrue(stats.containsKey("totalRequests"), "Contains totalRequests");
        assertTrue(stats.containsKey("pendingRequests"), "Contains pendingRequests");
        assertTrue(stats.containsKey("processingRequests"), "Contains processingRequests");
        assertTrue(stats.containsKey("completedDeliveries"), "Contains completedDeliveries");
        assertTrue(stats.containsKey("availableDrones"), "Contains availableDrones");
        assertTrue(stats.containsKey("busyDrones"), "Contains busyDrones");
        assertTrue(stats.containsKey("emergencyRequests"), "Contains emergencyRequests");

        // Priority Queue Preview
        List<Map<String, Object>> pq = (List<Map<String, Object>>) stats.get("priorityQueuePreview");
        assertTrue(pq != null && pq.size() > 0, "Dashboard contains live PriorityQueue preview items");
        assertTrue((int) pq.get(0).get("rank") == 1, "Top PriorityQueue item has rank #1");

        // Drone Preview
        List<Map<String, Object>> drones = (List<Map<String, Object>>) stats.get("droneFleetPreview");
        assertTrue(drones != null && drones.size() == 4, "Dashboard contains live Drone fleet preview");

        // Activities
        List<Map<String, Object>> acts = (List<Map<String, Object>>) stats.get("recentActivities");
        assertTrue(acts != null && acts.size() > 0, "Dashboard contains dynamic recent activities");
    }

    private static void testReseedDemoBaselinePersistence() {
        System.out.println("\n-- Testing Demo Baseline Reseed & Reload Pipeline --");
        ReliefSystem system = ReliefSystem.getInstance();
        system.reseedDemoData();

        assertTrue(system.getDisasterManager().size() >= 2, "Disasters reseeded to baseline (>= 2)");
        assertTrue(system.getLocationManager().size() >= 6, "Locations reseeded to baseline (>= 6)");
        assertTrue(system.getDroneManager().size() >= 4, "Drones reseeded to baseline (>= 4)");
        assertTrue(system.getResourceManager().size() >= 5, "Resources reseeded to baseline (>= 5)");
        assertTrue(system.getRouteManager().size() >= 8, "Routes reseeded to baseline (>= 8)");
        assertTrue(system.getRequestManager().size() >= 5, "Relief requests reseeded to baseline (>= 5)");
        assertTrue(system.getDeliveryManager().size() >= 1, "Deliveries reseeded to baseline (>= 1)");
    }
}
