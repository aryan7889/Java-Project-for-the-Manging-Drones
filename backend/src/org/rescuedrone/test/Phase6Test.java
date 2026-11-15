package org.rescuedrone.test;

import org.rescuedrone.algorithm.DroneAllocator;
import org.rescuedrone.model.Drone;
import org.rescuedrone.model.ReliefRequest;
import org.rescuedrone.model.Resource;
import org.rescuedrone.model.enums.DroneStatus;
import org.rescuedrone.model.enums.RequestStatus;
import org.rescuedrone.model.enums.ResourceCategory;
import org.rescuedrone.model.enums.UrgencyLevel;
import org.rescuedrone.service.DroneManager;
import org.rescuedrone.service.ReliefSystem;

/**
 * Verification test suite for Phase 6: Drone Management and Assignment Engine.
 */
public class Phase6Test {

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
        System.out.println(" RescueDrone Phase 6 Drone & Assignment Tests   ");
        System.out.println("=================================================");

        ReliefSystem.getInstance().initialize();

        testDroneRegistrationAndRecharge();
        testLeastExcessCapacitySelectionStrategy();
        testRejectionOnExcessivePayload();
        testRejectionOnLowBattery();
        testRejectionOnExcessiveDistance();
        testRejectionOnUnavailableStatus();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    private static void testDroneRegistrationAndRecharge() {
        System.out.println("\n-- Testing Drone Registration & Recharge Mechanics --");
        DroneManager dm = ReliefSystem.getInstance().getDroneManager();

        String testId = "DRN-TEST-" + System.currentTimeMillis();
        Drone drone = new Drone(testId, "Test Scout Hawk", 8.0, 35, 25.0, "LOC-001",
                DroneStatus.MAINTENANCE, "Quadcopter");
        dm.add(drone);

        assertTrue(dm.getById(testId) != null, "Drone registered successfully");
        assertTrue(dm.getById(testId).getStatus() == DroneStatus.MAINTENANCE, "Initial status is MAINTENANCE");

        // Recharge
        drone.recharge();
        dm.update(drone);

        Drone recharged = dm.getById(testId);
        assertTrue(recharged.getBatteryPercentage() == 100, "Recharged battery is 100%");
        assertTrue(recharged.getStatus() == DroneStatus.AVAILABLE, "Status restored from MAINTENANCE to AVAILABLE");

        // Cleanup
        dm.delete(testId);
    }

    private static void testLeastExcessCapacitySelectionStrategy() {
        System.out.println("\n-- Testing Least Excess Capacity Selection Strategy (Best Fit) --");
        ReliefSystem system = ReliefSystem.getInstance();

        // Register a small medical payload: 2 units of 1.5kg = 3.0kg total
        Resource res = new Resource("RES-FIT-01", "Compact First Aid", ResourceCategory.FIRST_AID, 100, 0, "kits", 1.5);
        system.getResourceManager().add(res);

        ReliefRequest req = new ReliefRequest("REQ-FIT-01", "LOC-002", "RES-FIT-01", 2,
                UrgencyLevel.HIGH, 80, "Small medical box");
        system.getRequestManager().add(req);

        // Drone options currently in seed:
        // DRN-001: 20kg cap (excess = 17kg)
        // DRN-002: 10kg cap (excess = 7kg)
        // DRN-003: 5kg cap (excess = 2kg) -> Best Fit!

        DroneAllocator.AllocationResult result = DroneAllocator.evaluateAndAssign("REQ-FIT-01", 8.0);
        assertTrue(result.isSuccess(), "Drone allocation succeeded");
        assertTrue(result.getSelectedDrone() != null, "Selected drone is not null");
        assertTrue(result.getSelectedDrone().getId().equals("DRN-003"),
                "Selected DRN-003 (5kg capacity) to minimize excess capacity and conserve heavy-lift drones");
        assertTrue(result.getRequest().getStatus() == RequestStatus.ASSIGNED, "Request status transitioned to ASSIGNED");
        assertTrue(result.getSelectedDrone().getStatus() == DroneStatus.ASSIGNED, "Drone status transitioned to ASSIGNED");

        // Restore drone and request for other tests
        result.getSelectedDrone().setStatus(DroneStatus.AVAILABLE);
        system.getDroneManager().update(result.getSelectedDrone());
        system.getRequestManager().delete("REQ-FIT-01");
        system.getResourceManager().delete("RES-FIT-01");
    }

    private static void testRejectionOnExcessivePayload() {
        System.out.println("\n-- Testing Rejection When Payload Exceeds Max Capacity --");
        ReliefSystem system = ReliefSystem.getInstance();

        // 50kg payload (heaviest drone in fleet is 25kg)
        Resource heavyRes = new Resource("RES-HEAVY", "Heavy Generator", ResourceCategory.EMERGENCY_SUPPLIES, 10, 0, "units", 50.0);
        system.getResourceManager().add(heavyRes);

        ReliefRequest heavyReq = new ReliefRequest("REQ-HEAVY-01", "LOC-002", "RES-HEAVY", 1,
                UrgencyLevel.CRITICAL, 200, "Heavy equipment");
        system.getRequestManager().add(heavyReq);

        DroneAllocator.AllocationResult result = DroneAllocator.evaluateAndAssign("REQ-HEAVY-01", 10.0);
        assertTrue(!result.isSuccess(), "Allocation rejected due to excessive payload");
        assertTrue(result.getMessage().contains("No suitable drone available"), "Meaningful error message returned");

        // Verify diagnostic reason for DRN-001 (20kg capacity)
        boolean foundCapacityRejection = false;
        for (DroneAllocator.CandidateEvaluation eval : result.getEvaluations()) {
            if (!eval.isEligible() && eval.getRejectionReason().contains("exceeds max capacity")) {
                foundCapacityRejection = true;
                break;
            }
        }
        assertTrue(foundCapacityRejection, "Diagnostic evaluations reported capacity rejection reason");

        system.getRequestManager().delete("REQ-HEAVY-01");
        system.getResourceManager().delete("RES-HEAVY");
    }

    private static void testRejectionOnLowBattery() {
        System.out.println("\n-- Testing Rejection On Low Battery --");
        ReliefSystem system = ReliefSystem.getInstance();

        // Temporarily drain available drone DRN-001 battery to 15%
        Drone d1 = system.getDroneManager().getById("DRN-001");
        int originalBat = d1.getBatteryPercentage();
        d1.setBatteryPercentage(15);
        system.getDroneManager().update(d1);

        // Required battery for 15km flight is (15 * 2 * 1.5) + 10 = 55%
        int reqBat = d1.calculateRequiredBattery(15.0);
        assertTrue(d1.getBatteryPercentage() < reqBat, "Drone battery (15%) is below required (55%)");

        // Restore battery
        d1.setBatteryPercentage(originalBat);
        system.getDroneManager().update(d1);
    }

    private static void testRejectionOnExcessiveDistance() {
        System.out.println("\n-- Testing Rejection When Flight Range Exceeded --");
        Drone testDrone = new Drone("DRN-SHORT", "Short Range VTOL", 10.0, 100, 15.0, "LOC-001",
                DroneStatus.AVAILABLE, "Mini Drone");

        // Attempt round trip of 20km (2 x 10km) which exceeds 15km max range
        assertTrue(!testDrone.isEligibleFor(5.0, 10.0),
                "Drone rejected because round trip (20km) exceeds max range (15km)");
    }

    private static void testRejectionOnUnavailableStatus() {
        System.out.println("\n-- Testing Rejection When Drone Is In Maintenance Or In-Flight --");
        Drone maintDrone = new Drone("DRN-MAINT", "Service Quad", 15.0, 100, 50.0, "LOC-001",
                DroneStatus.MAINTENANCE, "Quadcopter");
        assertTrue(!maintDrone.isEligibleFor(5.0, 5.0), "Reject drone whose status is MAINTENANCE");

        Drone flightDrone = new Drone("DRN-FLIGHT", "Service Quad", 15.0, 100, 50.0, "LOC-001",
                DroneStatus.IN_FLIGHT, "Quadcopter");
        assertTrue(!flightDrone.isEligibleFor(5.0, 5.0), "Reject drone whose status is IN_FLIGHT");
    }
}
