package org.rescuedrone.test;

import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;
import org.rescuedrone.service.CostCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Verification test suite for Phase 2: Java OOP and Entity Implementation.
 * Verifies Encapsulation, Inheritance, Polymorphism, Abstraction, and domain rules.
 */
public class Phase2Test {

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
        System.out.println("   RescueDrone Phase 2 OOP & Entity Test Suite   ");
        System.out.println("=================================================");

        testInheritanceAndPolymorphism();
        testEncapsulationAndValidation();
        testPriorityQueueOrdering();
        testDroneEligibilityAndBattery();
        testResourceAllocationAndReservation();
        testDeliveryCostCalculator();
        testCsvSerializationRoundtrip();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    private static void testInheritanceAndPolymorphism() {
        System.out.println("\n-- Testing Inheritance and Polymorphism --");
        List<BaseEntity> entities = new ArrayList<>();

        entities.add(new Disaster("DIS-001", "Flash Flood", DisasterType.FLOOD, "North Zone",
                SeverityLevel.CRITICAL, "River breach", "2026-11-14"));
        entities.add(new Location("LOC-001", "Base Hospital", 28.61, 77.20, "DIS-001", 0, SeverityLevel.LOW));
        entities.add(new Drone("DRN-001", "SkyLifter", 15.0, 90, 40.0, "LOC-001", DroneStatus.AVAILABLE, "Hexacopter"));
        entities.add(new Resource("RES-001", "First Aid Kit", ResourceCategory.FIRST_AID, 100, 10, "kits", 1.2));
        entities.add(new ReliefRequest("REQ-001", "LOC-002", "RES-001", 5, UrgencyLevel.CRITICAL, 120, "Immediate"));
        entities.add(new Route("RTE-001", "LOC-001", "LOC-002", 12.5));
        entities.add(new Delivery("DEL-001", "REQ-001", "DRN-001", "LOC-001", "LOC-002", List.of("LOC-001", "LOC-002"), 12.5, 400.0));

        // Polymorphic validation and CSV generation across all entities
        for (BaseEntity entity : entities) {
            entity.validate(); // Polymorphic method call
            String csv = entity.toCsvRecord();
            assertTrue(csv != null && !csv.isEmpty(), "Polymorphic CSV generation for " + entity.getClass().getSimpleName());
        }
    }

    private static void testEncapsulationAndValidation() {
        System.out.println("\n-- Testing Encapsulation and Validation --");

        // Negative latitude check
        try {
            Location invalidLoc = new Location("LOC-INV", "Invalid", 95.0, 77.0, "DIS-01", 10, SeverityLevel.LOW);
            invalidLoc.validate();
            assertTrue(false, "Should fail on invalid latitude > 90");
        } catch (IllegalArgumentException e) {
            assertTrue(true, "Encapsulation caught invalid latitude (> 90)");
        }

        // Empty disaster name check
        try {
            Disaster invalidDis = new Disaster("DIS-INV", "", DisasterType.FIRE, "Region", SeverityLevel.HIGH, "Desc", "Date");
            invalidDis.validate();
            assertTrue(false, "Should fail on empty disaster name");
        } catch (IllegalArgumentException e) {
            assertTrue(true, "Encapsulation caught empty disaster name");
        }
    }

    private static void testPriorityQueueOrdering() {
        System.out.println("\n-- Testing PriorityQueue Scheduling (Urgency + Population) --");
        PriorityQueue<ReliefRequest> pq = new PriorityQueue<>();

        long now = System.currentTimeMillis();
        ReliefRequest reqLow = new ReliefRequest("REQ-LOW", "LOC-1", "RES-1", 5, UrgencyLevel.LOW, 10, "Low urgency", RequestStatus.PENDING, now, now);
        ReliefRequest reqMed = new ReliefRequest("REQ-MED", "LOC-1", "RES-1", 5, UrgencyLevel.MEDIUM, 50, "Med urgency", RequestStatus.PENDING, now + 1, now + 1);
        ReliefRequest reqCrit1 = new ReliefRequest("REQ-CRIT-SMALL", "LOC-1", "RES-1", 5, UrgencyLevel.CRITICAL, 20, "Crit with 20 pop", RequestStatus.PENDING, now + 2, now + 2);
        ReliefRequest reqCrit2 = new ReliefRequest("REQ-CRIT-LARGE", "LOC-1", "RES-1", 5, UrgencyLevel.CRITICAL, 200, "Crit with 200 pop", RequestStatus.PENDING, now + 3, now + 3);

        pq.add(reqLow);
        pq.add(reqMed);
        pq.add(reqCrit1);
        pq.add(reqCrit2);

        // Dequeue order must be: reqCrit2 (Critical, 200) -> reqCrit1 (Critical, 20) -> reqMed -> reqLow
        ReliefRequest first = pq.poll();
        assertTrue(first.getId().equals("REQ-CRIT-LARGE"), "Highest priority is Critical with larger population");

        ReliefRequest second = pq.poll();
        assertTrue(second.getId().equals("REQ-CRIT-SMALL"), "Second priority is Critical with smaller population");

        ReliefRequest third = pq.poll();
        assertTrue(third.getId().equals("REQ-MED"), "Third priority is Medium urgency");

        ReliefRequest fourth = pq.poll();
        assertTrue(fourth.getId().equals("REQ-LOW"), "Fourth priority is Low urgency");
    }

    private static void testDroneEligibilityAndBattery() {
        System.out.println("\n-- Testing Drone Eligibility, Battery, and Range Checks --");
        Drone drone = new Drone("DRN-001", "HeavyLift-1", 15.0, 80, 50.0, "LOC-001", DroneStatus.AVAILABLE, "Hexacopter");

        // Test payload > capacity (18kg > 15kg) -> Should reject
        assertTrue(!drone.isEligibleFor(18.0, 10.0), "Reject drone if payload exceeds capacity");

        // Test distance exceeding max flight range (roundtrip 60km > 50km max range) -> Should reject
        assertTrue(!drone.isEligibleFor(5.0, 30.0), "Reject drone if round trip exceeds max flight range");

        // Valid payload & distance -> Should accept
        assertTrue(drone.isEligibleFor(10.0, 10.0), "Accept drone when capacity, battery, and range are sufficient");

        // Test battery consumption after flight
        int batBefore = drone.getBatteryPercentage();
        drone.consumeBattery(10.0); // 20km round trip @ 1.5% = 30%
        assertTrue(drone.getBatteryPercentage() < batBefore, "Drone battery percentage deducted after flight");

        // Test recharge
        drone.recharge();
        assertTrue(drone.getBatteryPercentage() == 100, "Drone recharged back to 100%");
    }

    private static void testResourceAllocationAndReservation() {
        System.out.println("\n-- Testing Resource Reservation & Allocation --");
        Resource res = new Resource("RES-001", "Drinking Water", ResourceCategory.WATER, 100, 0, "cans", 2.0);

        // Allocate 30 units
        boolean allocated = res.allocate(30);
        assertTrue(allocated, "Allocate 30 units of stock");
        assertTrue(res.getAvailableStock() == 70, "Available stock reduced to 70");
        assertTrue(res.getReservedStock() == 30, "Reserved stock increased to 30");

        // Attempt to over-allocate (80 units > 70 available) -> Should fail
        boolean overAlloc = res.allocate(80);
        assertTrue(!overAlloc, "Prevent over-allocation beyond available stock");

        // Release 10 units
        res.release(10);
        assertTrue(res.getAvailableStock() == 80, "Released units returned to available stock");
        assertTrue(res.getReservedStock() == 20, "Reserved stock reduced to 20");

        // Consume 20 units (delivery completed)
        res.consume(20);
        assertTrue(res.getReservedStock() == 0, "Reserved stock cleared after consumption");
        assertTrue(res.getAvailableStock() == 80, "Available stock remains intact");
    }

    private static void testDeliveryCostCalculator() {
        System.out.println("\n-- Testing Delivery Cost Calculation Formula --");
        // Example from master prompt: Base (₹100) + 10km * ₹20/km + Emergency (₹50) = ₹350
        CostCalculator.CostBreakdown cost = CostCalculator.calculateCost(10.0, UrgencyLevel.CRITICAL);
        assertTrue(cost.getBaseCharge() == 100.0, "Base charge is ₹100");
        assertTrue(cost.getDistanceCharge() == 200.0, "Distance charge for 10km is ₹200");
        assertTrue(cost.getEmergencyCharge() == 50.0, "Emergency surcharge is ₹50 for Critical request");
        assertTrue(cost.getTotalCost() == 350.0, "Total calculated cost is ₹350 (matches synopsis example)");

        // Non-emergency request (Low urgency) -> Emergency charge should be ₹0
        CostCalculator.CostBreakdown costLow = CostCalculator.calculateCost(15.0, UrgencyLevel.LOW);
        assertTrue(costLow.getEmergencyCharge() == 0.0, "No emergency surcharge for Low urgency request");
        assertTrue(costLow.getTotalCost() == 400.0, "Total cost for 15km Low urgency = 100 + 300 = ₹400");
    }

    private static void testCsvSerializationRoundtrip() {
        System.out.println("\n-- Testing CSV Serialization & Deserialization Fidelity --");
        Disaster d1 = new Disaster("DIS-001", "Flash Flood, Sector 2", DisasterType.FLOOD, "North Valley",
                SeverityLevel.CRITICAL, "Bridges collapsed, needs aid", "2026-11-14");
        String csv = d1.toCsvRecord();
        assertTrue(csv.contains("DIS-001"), "CSV record contains disaster ID");

        // Test parser with comma handling
        String[] tokens = csv.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        Disaster d2 = Disaster.fromCsvTokens(tokens);
        assertTrue(d2.getId().equals(d1.getId()), "Deserialized Disaster ID matches original");
        assertTrue(d2.getType() == d1.getType(), "Deserialized Disaster Type matches original");
        assertTrue(d2.getSeverity() == d1.getSeverity(), "Deserialized Severity matches original");
    }
}
