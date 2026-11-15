package org.rescuedrone.test;

import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;
import org.rescuedrone.service.DeliveryService;
import org.rescuedrone.service.ReliefSystem;

/**
 * Verification test suite for Phase 8: Delivery Management, Cost Calculation & Simulation Workflow.
 * Tests the complete end-to-end disaster relief operational pipeline.
 */
public class Phase8Test {

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
        System.out.println(" RescueDrone Phase 8 Delivery & Workflow Tests  ");
        System.out.println("=================================================");

        ReliefSystem.getInstance().initialize();

        testEndToEndDeliveryLifecycle();
        testDeliveryMissionCancellationAndAssetRestoration();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    private static void testEndToEndDeliveryLifecycle() {
        System.out.println("\n-- Testing Complete End-to-End Delivery Mission Lifecycle --");
        ReliefSystem system = ReliefSystem.getInstance();
        DeliveryService deliveryService = DeliveryService.getInstance();

        // Ensure clean fleet state for test
        Drone d1 = system.getDroneManager().getById("DRN-001");
        d1.setCurrentLocationId("LOC-001");
        d1.setStatus(DroneStatus.AVAILABLE);
        d1.setBatteryPercentage(95);
        system.getDroneManager().update(d1);

        // 1. Setup Request for North Valley Camp A (LOC-002) needing 5 First Aid Kits
        Resource res = system.getResourceManager().getById("RES-001");
        int initialAvailStock = res.getAvailableStock();
        int initialReservedStock = res.getReservedStock();

        String reqId = "REQ-P8-" + System.currentTimeMillis();
        ReliefRequest req = new ReliefRequest(reqId, "LOC-002", "RES-001", 5,
                UrgencyLevel.CRITICAL, 200, "Phase 8 end-to-end mission");
        system.getRequestManager().add(req);

        assertTrue(system.getRequestManager().getById(reqId) != null, "Step 1: Emergency relief request created");

        // 2. Plan and Create Delivery (Auto-selects optimal drone & computes Dijkstra route)
        Delivery delivery = deliveryService.createDelivery(reqId, null);
        assertTrue(delivery != null, "Step 2: Delivery mission record planned and created");
        assertTrue(delivery.getStatus() == DeliveryStatus.PENDING, "Delivery initial status is PENDING");
        assertTrue(delivery.getTotalDistanceKm() > 0, "Dijkstra calculated positive distance: " + delivery.getTotalDistanceKm() + " km");
        assertTrue(delivery.getPathNodeIds().size() >= 2, "Path contains source and destination nodes");
        assertTrue(delivery.getDeliveryCost() > 0, "Delivery fee computed: ₹" + delivery.getDeliveryCost());

        // Verify drone & request states
        Drone drone = system.getDroneManager().getById(delivery.getDroneId());
        assertTrue(drone.getStatus() == DroneStatus.ASSIGNED, "Drone status transitioned to ASSIGNED");
        assertTrue(system.getRequestManager().getById(reqId).getStatus() == RequestStatus.ASSIGNED, "Request status transitioned to ASSIGNED");
        assertTrue(res.getReservedStock() == initialReservedStock + 5, "Stock reserved for mission (+5 kits)");

        // 3. Dispatch Delivery Mission (start delivery)
        int batBeforeFlight = drone.getBatteryPercentage();
        deliveryService.startDelivery(delivery.getId());

        assertTrue(delivery.getStatus() == DeliveryStatus.IN_TRANSIT, "Step 3: Delivery marked IN_TRANSIT");
        assertTrue(drone.getStatus() == DroneStatus.IN_FLIGHT, "Drone status transitioned to IN_FLIGHT");
        assertTrue(system.getRequestManager().getById(reqId).getStatus() == RequestStatus.IN_TRANSIT, "Request status transitioned to IN_TRANSIT");

        // 4. Complete Delivery Mission
        deliveryService.completeDelivery(delivery.getId());

        assertTrue(delivery.getStatus() == DeliveryStatus.COMPLETED, "Step 4: Delivery marked COMPLETED");
        assertTrue(system.getRequestManager().getById(reqId).getStatus() == RequestStatus.DELIVERED, "Request status transitioned to DELIVERED");

        // Verify Drone after completion
        assertTrue(drone.getStatus() == DroneStatus.AVAILABLE || drone.getStatus() == DroneStatus.MAINTENANCE, "Drone freed back to operational fleet");
        assertTrue(drone.getBatteryPercentage() < batBeforeFlight, "Drone battery consumed for distance flown (" + drone.getBatteryPercentage() + "% vs " + batBeforeFlight + "%)");
        assertTrue(drone.getCurrentLocationId().equals("LOC-002"), "Drone location updated to destination location LOC-002");

        // Verify Inventory after completion
        assertTrue(res.getReservedStock() == initialReservedStock, "Reserved stock cleared upon successful delivery");
        assertTrue(res.getAvailableStock() == initialAvailStock - 5, "Available stock permanently consumed (-5 kits)");

        // Cleanup
        system.getRequestManager().delete(reqId);
        system.getDeliveryManager().delete(delivery.getId());
        res.setAvailableStock(initialAvailStock);
        system.getResourceManager().update(res);
        drone.recharge();
        system.getDroneManager().update(drone);
    }

    private static void testDeliveryMissionCancellationAndAssetRestoration() {
        System.out.println("\n-- Testing Delivery Cancellation & Asset Restoration --");
        ReliefSystem system = ReliefSystem.getInstance();
        DeliveryService deliveryService = DeliveryService.getInstance();

        Resource res = system.getResourceManager().getById("RES-002");
        int initialAvailStock = res.getAvailableStock();
        int initialReservedStock = res.getReservedStock();

        String reqId = "REQ-CANCEL-" + System.currentTimeMillis();
        ReliefRequest req = new ReliefRequest(reqId, "LOC-004", "RES-002", 2,
                UrgencyLevel.HIGH, 100, "Cancel test request");
        system.getRequestManager().add(req);

        Delivery delivery = deliveryService.createDelivery(reqId, null);
        Drone drone = system.getDroneManager().getById(delivery.getDroneId());

        assertTrue(res.getReservedStock() == initialReservedStock + 2, "Stock reserved (+2 units)");
        assertTrue(drone.getStatus() == DroneStatus.ASSIGNED, "Drone status is ASSIGNED");

        // Cancel Mission
        deliveryService.cancelDelivery(delivery.getId());

        assertTrue(delivery.getStatus() == DeliveryStatus.CANCELLED, "Delivery status transitioned to CANCELLED");
        assertTrue(system.getRequestManager().getById(reqId).getStatus() == RequestStatus.CANCELLED, "Request status transitioned to CANCELLED");
        assertTrue(drone.getStatus() == DroneStatus.AVAILABLE, "Drone restored to AVAILABLE");
        assertTrue(res.getReservedStock() == initialReservedStock, "Reserved stock restored");
        assertTrue(res.getAvailableStock() == initialAvailStock, "Available stock restored intact");

        // Cleanup
        system.getRequestManager().delete(reqId);
        system.getDeliveryManager().delete(delivery.getId());
    }
}
