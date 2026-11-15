package org.rescuedrone.test;

import org.rescuedrone.algorithm.DijkstraOptimizer;
import org.rescuedrone.algorithm.DijkstraOptimizer.PathResult;
import org.rescuedrone.algorithm.DroneAllocator;
import org.rescuedrone.algorithm.DroneAllocator.AllocationResult;
import org.rescuedrone.algorithm.DroneAllocator.CandidateEvaluation;
import org.rescuedrone.ds.Graph;
import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;
import org.rescuedrone.service.*;

import java.util.List;
import java.util.Map;

/**
 * Phase 10 Master End-to-End Integration Test Suite.
 * Validates the complete Disaster Relief Operational Lifecycle,
 * cross-module data consistency, algorithm correctness, and edge-case handling.
 */
public class Phase10IntegrationTest {

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
        System.out.println("==================================================================");
        System.out.println("  RescueDrone Phase 10 Master End-to-End Integration Test Suite   ");
        System.out.println("==================================================================");

        ReliefSystem system = ReliefSystem.getInstance();
        system.initialize();

        testEndToEndEmergencyPipeline(system);
        testDijkstraMultiHopAndUnreachableEdgeCases(system);
        testDroneAllocationConstraintsAndRejections(system);
        testResourceInventoryStockLifecycle(system);
        testAnalyticsAndConsolidatedReporting(system);

        System.out.println("==================================================================");
        System.out.printf("   Master Integration Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("==================================================================");
    }

    private static void testEndToEndEmergencyPipeline(ReliefSystem system) {
        System.out.println("\n-- 1. Testing Full Disaster -> Request -> Route -> Delivery Pipeline --");

        // 1. Register Disaster
        String disId = "DIS-P10-" + System.currentTimeMillis();
        Disaster disaster = new Disaster(disId, "Cyclone Vardah", DisasterType.CYCLONE, "Coastal Belt",
                SeverityLevel.CRITICAL, "Category 4 tropical storm with severe storm surges", "2026-11-14");
        system.getDisasterManager().add(disaster);
        assertTrue(system.getDisasterManager().getById(disId) != null, "Disaster registered and indexed in HashMap");

        // 2. Register Locations
        String locHub = "LOC-HUB-" + System.currentTimeMillis();
        String locCamp = "LOC-CAMP-" + System.currentTimeMillis();
        String locVillage = "LOC-VILLAGE-" + System.currentTimeMillis();

        Location hub = new Location(locHub, "Relief Staging Hub", 28.50, 77.10, disId, 0, SeverityLevel.LOW);
        Location camp = new Location(locCamp, "Coastal Evacuation Camp", 28.55, 77.18, disId, 600, SeverityLevel.CRITICAL);
        Location village = new Location(locVillage, "Isolated Inundated Hamlet", 28.62, 77.25, disId, 250, SeverityLevel.HIGH);

        system.getLocationManager().add(hub);
        system.getLocationManager().add(camp);
        system.getLocationManager().add(village);
        assertTrue(system.getLocationManager().getById(locHub) != null, "Staging hub location added");
        assertTrue(system.getLocationManager().getById(locCamp) != null, "Evacuation camp added");
        assertTrue(system.getLocationManager().getById(locVillage) != null, "Inundated hamlet added");

        // 3. Register Graph Routes
        // Path 1: Direct Hub -> Village = 35.0 km
        // Path 2: Hub -> Camp (10.0 km) + Camp -> Village (12.0 km) = 22.0 km (Shortest)
        String r1Id = "RTE-1-" + System.currentTimeMillis();
        String r2Id = "RTE-2-" + System.currentTimeMillis();
        String r3Id = "RTE-3-" + System.currentTimeMillis();

        system.getRouteManager().add(new Route(r1Id, locHub, locVillage, 35.0));
        system.getRouteManager().add(new Route(r2Id, locHub, locCamp, 10.0));
        system.getRouteManager().add(new Route(r3Id, locCamp, locVillage, 12.0));

        // 4. Register Drone
        String droneId = "DRN-P10-" + System.currentTimeMillis();
        Drone drone = new Drone(droneId, "Cyclone Lifter X", 18.0, 100, 60.0, locHub, DroneStatus.AVAILABLE, "Hexacopter VTOL");
        system.getDroneManager().add(drone);
        assertTrue(system.getDroneManager().getById(droneId) != null, "Operational relief drone registered");

        // 5. Register Emergency Relief Request
        String resId = "RES-001"; // First Aid Kits from seed
        String reqId = "REQ-P10-" + System.currentTimeMillis();
        ReliefRequest req = new ReliefRequest(reqId, locVillage, resId, 4, UrgencyLevel.CRITICAL, 250, "Urgent first-aid needed for surge injured");
        system.getRequestManager().add(req);
        assertTrue(system.getRequestManager().getById(reqId) != null, "Critical relief request registered");

        // 6. Test Priority Queue Scheduling
        ReliefRequest nextInQueue = system.getRequestManager().peekNextPriorityRequest();
        assertTrue(nextInQueue != null && nextInQueue.getUrgency() == UrgencyLevel.CRITICAL,
                "PriorityQueue correctly serves Critical request first");

        // 7. Dispatch Delivery Mission
        DeliveryService deliveryService = DeliveryService.getInstance();
        Delivery delivery = deliveryService.createDelivery(reqId, droneId);

        assertTrue(delivery != null, "Delivery mission planned and created");
        assertTrue(delivery.getStatus() == DeliveryStatus.PENDING, "Delivery status is PENDING");
        assertTrue(Math.abs(delivery.getTotalDistanceKm() - 22.0) < 0.001,
                "Dijkstra chose shortest multi-hop path via Camp (22.0 km instead of 35.0 km direct)");
        assertTrue(delivery.getPathNodeIds().size() == 3, "Path sequence has exactly 3 nodes [Hub -> Camp -> Village]");
        assertTrue(delivery.getPathNodeIds().get(0).equals(locHub), "Path starts at Hub");
        assertTrue(delivery.getPathNodeIds().get(1).equals(locCamp), "Path transits through Camp");
        assertTrue(delivery.getPathNodeIds().get(2).equals(locVillage), "Path terminates at Village");

        // 8. Execute Mission (Takeoff -> In-Transit)
        deliveryService.startDelivery(delivery.getId());
        assertTrue(delivery.getStatus() == DeliveryStatus.IN_TRANSIT, "Delivery transitioned to IN_TRANSIT");
        assertTrue(system.getDroneManager().getById(droneId).getStatus() == DroneStatus.IN_FLIGHT, "Drone transitioned to IN_FLIGHT");
        assertTrue(system.getRequestManager().getById(reqId).getStatus() == RequestStatus.IN_TRANSIT, "Request transitioned to IN_TRANSIT");

        // 9. Mission Arrival (Complete Delivery)
        deliveryService.completeDelivery(delivery.getId());
        assertTrue(delivery.getStatus() == DeliveryStatus.COMPLETED, "Delivery transitioned to COMPLETED");
        assertTrue(system.getRequestManager().getById(reqId).getStatus() == RequestStatus.DELIVERED, "Request transitioned to DELIVERED");

        Drone updatedDrone = system.getDroneManager().getById(droneId);
        assertTrue(updatedDrone.getCurrentLocationId().equals(locVillage), "Drone relocated to destination village (LOC-VILLAGE)");
        assertTrue(updatedDrone.getBatteryPercentage() < 100, "Drone battery discharged proportionally to distance flown");
        assertTrue(updatedDrone.getStatus() == DroneStatus.AVAILABLE || updatedDrone.getStatus() == DroneStatus.MAINTENANCE,
                "Drone freed back to fleet");

        // Cleanup temporary integration test entities
        system.getDisasterManager().delete(disId);
        system.getLocationManager().delete(locHub);
        system.getLocationManager().delete(locCamp);
        system.getLocationManager().delete(locVillage);
        system.getRouteManager().delete(r1Id);
        system.getRouteManager().delete(r2Id);
        system.getRouteManager().delete(r3Id);
        system.getDroneManager().delete(droneId);
        system.getRequestManager().delete(reqId);
        system.getDeliveryManager().delete(delivery.getId());
    }

    private static void testDijkstraMultiHopAndUnreachableEdgeCases(ReliefSystem system) {
        System.out.println("\n-- 2. Testing Dijkstra Shortest Path & Unreachable Graph Components --");
        Graph graph = new Graph();
        graph.addVertex("V1");
        graph.addVertex("V2");
        graph.addVertex("V3");
        graph.addVertex("V4");
        graph.addVertex("ISOLATED");

        graph.addEdge("V1", "V2", 5.0, "E1", true);
        graph.addEdge("V2", "V3", 7.0, "E2", true);
        graph.addEdge("V1", "V3", 15.0, "E3", true); // Longer direct edge
        graph.addEdge("V3", "V4", 3.0, "E4", true);

        DijkstraOptimizer optimizer = new DijkstraOptimizer();

        // Path V1 -> V4: V1 -> V2 (5) -> V3 (7) -> V4 (3) = 15.0 km
        PathResult res = optimizer.findShortestPath(graph, "V1", "V4");
        assertTrue(res.isFound(), "Shortest path found through multi-hop graph");
        assertTrue(Math.abs(res.getTotalDistanceKm() - 15.0) < 0.001, "Dijkstra relaxed edge correctly (15.0 km)");
        assertTrue(res.getPathNodeIds().size() == 4, "Path contains exactly 4 hops [V1, V2, V3, V4]");

        // Unreachable Destination Test
        PathResult unreachableRes = optimizer.findShortestPath(graph, "V1", "ISOLATED");
        assertTrue(!unreachableRes.isFound(), "Disconnected graph component cleanly recognized as unreachable");
        assertTrue(unreachableRes.getTotalDistanceKm() == 0.0, "Unreachable distance recorded as 0.0 with found=false");
        assertTrue(unreachableRes.getPathNodeIds().isEmpty(), "Unreachable path is empty");
        assertTrue(unreachableRes.getMessage().contains("unreachable"), "Meaningful error message returned for unreachable vertex");
    }

    private static void testDroneAllocationConstraintsAndRejections(ReliefSystem system) {
        System.out.println("\n-- 3. Testing Drone Allocation Constraints & Rejections --");

        // 1. Capacity Exceeded Rejection
        Resource heavyRes = new Resource("RES-HEAVY-P10", "Heavy Medical Scanner", ResourceCategory.EMERGENCY_SUPPLIES, 10, 0, "units", 50.0);
        system.getResourceManager().add(heavyRes);

        ReliefRequest heavyReq = new ReliefRequest("REQ-HEAVY-P10", "LOC-002", "RES-HEAVY-P10", 1, // 50kg exceeds all fleet drones
                UrgencyLevel.HIGH, 100, "Heavy payload request");
        system.getRequestManager().add(heavyReq);

        AllocationResult heavyResult = DroneAllocator.evaluateAndAssign("REQ-HEAVY-P10", 10.0);
        assertTrue(!heavyResult.isSuccess(), "Allocation cleanly rejected when payload exceeds max capacity of all fleet drones");
        assertTrue(heavyResult.getSelectedDrone() == null, "No drone assigned for impossible payload");

        // 2. Battery Insufficient Rejection
        Drone lowBatDrone = new Drone("DRN-LOWBAT", "Low Bat Drone", 20.0, 5, 50.0, "LOC-001", DroneStatus.AVAILABLE, "Quad");
        assertTrue(!lowBatDrone.isEligibleFor(2.0, 15.0), "Drone with 5% battery recognized as ineligible due to insufficient flight reserve");
        assertTrue(lowBatDrone.calculateRequiredBattery(15.0) > lowBatDrone.getBatteryPercentage(),
                "Required battery calculation accounts for round-trip safety buffer");

        system.getRequestManager().delete("REQ-HEAVY-P10");
        system.getResourceManager().delete("RES-HEAVY-P10");
    }

    private static void testResourceInventoryStockLifecycle(ReliefSystem system) {
        System.out.println("\n-- 4. Testing Resource Inventory Allocation, Reservation & Rollback --");
        ResourceManager rm = system.getResourceManager();

        Resource r = new Resource("RES-TEST", "Sterile Bandages", ResourceCategory.FIRST_AID, 100, 0, "rolls", 0.2);
        rm.add(r);

        // Reserve 25 units
        boolean reserved = r.allocate(25);
        assertTrue(reserved, "Successfully reserved 25 units");
        assertTrue(r.getAvailableStock() == 75, "Available stock reduced to 75");
        assertTrue(r.getReservedStock() == 25, "Reserved stock increased to 25");

        // Release 10 units (simulating partial cancel)
        r.release(10);
        assertTrue(r.getAvailableStock() == 85, "Available stock restored to 85");
        assertTrue(r.getReservedStock() == 15, "Reserved stock reduced to 15");

        // Consume remaining 15 units (simulating successful delivery arrival)
        r.consume(15);
        assertTrue(r.getReservedStock() == 0, "Reserved stock cleared after consumption");
        assertTrue(r.getAvailableStock() == 85, "Available stock permanently consumed");

        // Over-allocation protection
        boolean overAlloc = r.allocate(200);
        assertTrue(!overAlloc, "Over-allocation beyond available stock safely prevented");

        rm.delete("RES-TEST");
    }

    @SuppressWarnings("unchecked")
    private static void testAnalyticsAndConsolidatedReporting(ReliefSystem system) {
        System.out.println("\n-- 5. Testing Analytics Engine & Central Telemetry Reporting --");
        AnalyticsService analytics = AnalyticsService.getInstance();
        Map<String, Object> report = analytics.generateAnalyticsReport();

        assertTrue(report != null, "Analytics report object non-null");
        assertTrue(report.containsKey("deliveryMetrics"), "Contains deliveryMetrics");
        assertTrue(report.containsKey("droneMetrics"), "Contains droneMetrics");
        assertTrue(report.containsKey("requestMetrics"), "Contains requestMetrics");
        assertTrue(report.containsKey("resourceMetrics"), "Contains resourceMetrics");
        assertTrue(report.containsKey("disasterBreakdown"), "Contains disasterBreakdown");
        assertTrue(report.containsKey("systemTelemetry"), "Contains systemTelemetry");

        Map<String, Object> sys = system.computeDashboardStats();
        assertTrue(sys.get("totalDisasters") != null, "Dashboard contains totalDisasters count");
        assertTrue(sys.get("availableDrones") != null, "Dashboard contains availableDrones count");
        assertTrue(sys.get("priorityQueuePreview") != null, "Dashboard contains live PriorityQueue preview list");
    }
}
