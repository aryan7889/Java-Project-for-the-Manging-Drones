package org.rescuedrone.test;

import org.rescuedrone.model.ReliefRequest;
import org.rescuedrone.model.enums.RequestStatus;
import org.rescuedrone.model.enums.UrgencyLevel;
import org.rescuedrone.service.ReliefRequestManager;
import org.rescuedrone.service.ReliefSystem;

import java.io.File;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Verification test suite for Phase 5: Relief Request and Priority System.
 * Tests PriorityQueue heap behavior, deterministic tie-breaking, and status updates.
 */
public class Phase5Test {

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
        System.out.println(" RescueDrone Phase 5 Relief Request & PQ Tests  ");
        System.out.println("=================================================");

        testMultiFactorPriorityComparator();
        testPriorityQueueLiveHeapBehavior();
        testStatusTransitionsAndQueueExclusion();
        testRequestPersistenceAndReload();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    private static void testMultiFactorPriorityComparator() {
        System.out.println("\n-- Testing Multi-Factor PriorityQueue Comparator --");

        long baseTime = 1700000000000L;

        // 1. Urgency test: Critical vs High
        ReliefRequest rCrit = new ReliefRequest("R-CRIT", "LOC-001", "RES-001", 5, UrgencyLevel.CRITICAL, 100, "Desc", RequestStatus.PENDING, baseTime, baseTime);
        ReliefRequest rHigh = new ReliefRequest("R-HIGH", "LOC-001", "RES-001", 5, UrgencyLevel.HIGH, 100, "Desc", RequestStatus.PENDING, baseTime, baseTime);
        assertTrue(rCrit.compareTo(rHigh) < 0, "Critical urgency precedes High urgency");
        assertTrue(rHigh.compareTo(rCrit) > 0, "High urgency succeeds Critical urgency");

        // 2. Secondary criterion: Identical urgency (HIGH vs HIGH), different population
        ReliefRequest rPopHigh = new ReliefRequest("R-POP-500", "LOC-001", "RES-001", 5, UrgencyLevel.HIGH, 500, "Desc", RequestStatus.PENDING, baseTime, baseTime);
        ReliefRequest rPopLow = new ReliefRequest("R-POP-50", "LOC-001", "RES-001", 5, UrgencyLevel.HIGH, 50, "Desc", RequestStatus.PENDING, baseTime, baseTime);
        assertTrue(rPopHigh.compareTo(rPopLow) < 0, "Among equal urgency, higher affected population (500 > 50) takes priority");

        // 3. Tertiary criterion: Identical urgency and population, different arrival timestamp
        ReliefRequest rOlder = new ReliefRequest("R-OLD", "LOC-001", "RES-001", 5, UrgencyLevel.MEDIUM, 100, "Desc", RequestStatus.PENDING, baseTime, baseTime);
        ReliefRequest rNewer = new ReliefRequest("R-NEW", "LOC-001", "RES-001", 5, UrgencyLevel.MEDIUM, 100, "Desc", RequestStatus.PENDING, baseTime + 5000, baseTime + 5000);
        assertTrue(rOlder.compareTo(rNewer) < 0, "Among equal urgency & population, earlier timestamp (FIFO) takes priority");
    }

    private static void testPriorityQueueLiveHeapBehavior() {
        System.out.println("\n-- Testing PriorityQueue Live Heap Dynamic Dispatch --");

        String tempFile = "backend/data/test_pq_" + System.currentTimeMillis() + ".csv";
        ReliefRequestManager rm = new ReliefRequestManager(tempFile);

        long now = System.currentTimeMillis();

        // Add out of order
        rm.add(new ReliefRequest("REQ-D", "LOC-001", "RES-001", 5, UrgencyLevel.LOW, 20, "Low", RequestStatus.PENDING, now, now));
        rm.add(new ReliefRequest("REQ-B", "LOC-001", "RES-001", 5, UrgencyLevel.HIGH, 80, "High 80", RequestStatus.PENDING, now + 1, now + 1));
        rm.add(new ReliefRequest("REQ-A", "LOC-001", "RES-001", 5, UrgencyLevel.CRITICAL, 300, "Critical 300", RequestStatus.PENDING, now + 2, now + 2));
        rm.add(new ReliefRequest("REQ-C", "LOC-001", "RES-001", 5, UrgencyLevel.HIGH, 250, "High 250", RequestStatus.PENDING, now + 3, now + 3));

        assertTrue(rm.getPendingCount() == 4, "4 pending requests enqueued in PriorityQueue");

        // PriorityQueue peek must be REQ-A (Critical, 300)
        ReliefRequest top = rm.peekNextPriorityRequest();
        assertTrue(top.getId().equals("REQ-A"), "Top priority request is REQ-A (Critical, 300 people)");

        // Ordered polling verification
        ReliefRequest p1 = rm.pollNextPriorityRequest(); // polls REQ-A
        assertTrue(p1.getId().equals("REQ-A"), "First polled is REQ-A (Critical, 300)");
        assertTrue(p1.getStatus() == RequestStatus.ASSIGNED, "Polled request status transitioned to ASSIGNED");

        ReliefRequest p2 = rm.pollNextPriorityRequest(); // polls REQ-C (High, 250)
        assertTrue(p2.getId().equals("REQ-C"), "Second polled is REQ-C (High, 250)");

        ReliefRequest p3 = rm.pollNextPriorityRequest(); // polls REQ-B (High, 80)
        assertTrue(p3.getId().equals("REQ-B"), "Third polled is REQ-B (High, 80)");

        ReliefRequest p4 = rm.pollNextPriorityRequest(); // polls REQ-D (Low, 20)
        assertTrue(p4.getId().equals("REQ-D"), "Fourth polled is REQ-D (Low, 20)");

        assertTrue(rm.getPendingCount() == 0, "PriorityQueue empty after all requests dispatched");

        new File(tempFile).delete();
    }

    private static void testStatusTransitionsAndQueueExclusion() {
        System.out.println("\n-- Testing Request Status Lifecycle & Queue Exclusion --");

        String tempFile = "backend/data/test_lifecycle_" + System.currentTimeMillis() + ".csv";
        ReliefRequestManager rm = new ReliefRequestManager(tempFile);

        ReliefRequest req = new ReliefRequest("REQ-LIFE", "LOC-001", "RES-001", 10, UrgencyLevel.CRITICAL, 150, "Life test");
        rm.add(req);
        assertTrue(rm.getPendingCount() == 1, "New request in PENDING status enters PriorityQueue");

        // Update status to ASSIGNED -> must leave PriorityQueue
        req.setStatus(RequestStatus.ASSIGNED);
        rm.update(req);
        assertTrue(rm.getPendingCount() == 0, "Assigned request automatically excluded from pending PriorityQueue");

        // Update status to IN_TRANSIT
        req.setStatus(RequestStatus.IN_TRANSIT);
        rm.update(req);
        assertTrue(rm.getById("REQ-LIFE").getStatus() == RequestStatus.IN_TRANSIT, "Status updated to IN_TRANSIT");

        // Update status to DELIVERED
        req.setStatus(RequestStatus.DELIVERED);
        rm.update(req);
        assertTrue(rm.getById("REQ-LIFE").getStatus() == RequestStatus.DELIVERED, "Status updated to DELIVERED");

        new File(tempFile).delete();
    }

    private static void testRequestPersistenceAndReload() {
        System.out.println("\n-- Testing Request Persistence and Queue Rebuild on Reload --");

        String tempFile = "backend/data/test_pq_reload_" + System.currentTimeMillis() + ".csv";
        ReliefRequestManager rm1 = new ReliefRequestManager(tempFile);

        rm1.add(new ReliefRequest("REQ-R1", "LOC-001", "RES-001", 10, UrgencyLevel.HIGH, 100, "Pending req 1"));
        rm1.add(new ReliefRequest("REQ-R2", "LOC-002", "RES-002", 5, UrgencyLevel.CRITICAL, 200, "Pending req 2"));

        // Instantiate new manager instance (reloading from file)
        ReliefRequestManager rm2 = new ReliefRequestManager(tempFile);
        rm2.loadFromFile();

        assertTrue(rm2.size() == 2, "Reloaded 2 requests from CSV");
        assertTrue(rm2.getPendingCount() == 2, "PriorityQueue successfully rebuilt with 2 pending requests");
        assertTrue(rm2.peekNextPriorityRequest().getId().equals("REQ-R2"), "Heap top is REQ-R2 (Critical) upon reload");

        new File(tempFile).delete();
    }
}
