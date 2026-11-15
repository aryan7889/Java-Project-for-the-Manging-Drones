package org.rescuedrone.test;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;
import org.rescuedrone.persistence.FileManager;
import org.rescuedrone.service.*;

import java.io.File;
import java.util.List;

/**
 * Verification test suite for Phase 3: Data Structures and File Handling.
 * Verifies ArrayList, HashMap, PriorityQueue, CSV persistence, and restart simulation.
 */
public class Phase3Test {

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
        System.out.println(" RescueDrone Phase 3 DS & File Handling Tests   ");
        System.out.println("=================================================");

        testFileManagerQuotesAndCommas();
        testMissingAndEmptyFiles();
        testCorruptedCsvLinesTolerance();
        testReliefSystemInitializationAndSeed();
        testArrayListAndHashMapSynchronisation();
        testPriorityQueueManagement();
        testRecordUpdatingAndPersistence();
        testPersistenceAcrossApplicationRestart();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    private static void testFileManagerQuotesAndCommas() {
        System.out.println("\n-- Testing FileManager CSV Quote & Comma Parsing --");
        String testLine = "REQ-001,LOC-002,RES-001,10,CRITICAL,450,\"Severe flash flood, bridges submerged\",PENDING,1700000000,1700000000";
        String[] tokens = FileManager.parseCsvLine(testLine);

        assertTrue(tokens.length == 10, "Token count is 10 despite comma inside quotes");
        assertTrue(tokens[6].equals("Severe flash flood, bridges submerged"), "Quoted description containing comma was preserved");
    }

    private static void testMissingAndEmptyFiles() {
        System.out.println("\n-- Testing Missing & Empty File Handling --");
        String missingPath = "backend/data/test_missing_" + System.currentTimeMillis() + ".csv";
        List<String> lines = FileManager.readLines(missingPath);
        assertTrue(lines.isEmpty(), "Reading missing file returns empty list without error");

        File f = new File(missingPath);
        assertTrue(f.exists(), "Missing file was safely created on disk");
        f.delete();
    }

    private static void testCorruptedCsvLinesTolerance() {
        System.out.println("\n-- Testing Fault Tolerance for Corrupted Records --");
        String corruptPath = "backend/data/test_corrupt_" + System.currentTimeMillis() + ".csv";
        FileManager.appendLine(corruptPath, "VALID-01,Flood Alpha,Flood,North,CRITICAL,Desc,2026-11-14,1700000000");
        FileManager.appendLine(corruptPath, "CORRUPT_TOO_FEW_TOKENS,junk"); // Missing columns
        FileManager.appendLine(corruptPath, "VALID-02,Fire Beta,Fire,South,HIGH,Desc,2026-11-14,1700000000");

        DisasterManager dm = new DisasterManager(corruptPath);
        dm.loadFromFile();

        assertTrue(dm.size() == 2, "Corrupted CSV line was safely skipped without crashing (loaded 2 valid records)");
        assertTrue(dm.getById("VALID-01") != null, "Valid record 1 was parsed");
        assertTrue(dm.getById("VALID-02") != null, "Valid record 2 was parsed");

        new File(corruptPath).delete();
    }

    private static void testReliefSystemInitializationAndSeed() {
        System.out.println("\n-- Testing ReliefSystem Startup & Data Seeding --");
        ReliefSystem system = ReliefSystem.getInstance();
        system.initialize();

        assertTrue(system.getDisasterManager().size() >= 2, "Disasters seeded & loaded (count >= 2)");
        assertTrue(system.getLocationManager().size() >= 6, "Locations seeded & loaded (count >= 6)");
        assertTrue(system.getDroneManager().size() >= 4, "Drones seeded & loaded (count >= 4)");
        assertTrue(system.getResourceManager().size() >= 5, "Resources seeded & loaded (count >= 5)");
        assertTrue(system.getRequestManager().size() >= 5, "Requests seeded & loaded (count >= 5)");
        assertTrue(system.getRouteManager().size() >= 8, "Routes seeded & loaded (count >= 8)");
        assertTrue(system.getDeliveryManager().size() >= 1, "Deliveries seeded & loaded (count >= 1)");
    }

    private static void testArrayListAndHashMapSynchronisation() {
        System.out.println("\n-- Testing ArrayList (Order) vs HashMap (O(1) Lookup) --");
        ReliefSystem system = ReliefSystem.getInstance();
        LocationManager lm = system.getLocationManager();

        // O(1) HashMap lookup
        Location loc1 = lm.getById("LOC-001");
        assertTrue(loc1 != null, "HashMap O(1) lookup found LOC-001");
        assertTrue(loc1.getName().contains("Hospital"), "LOC-001 details match expected entity");

        // Non-existent ID lookup
        Location nonExistent = lm.getById("LOC-999");
        assertTrue(nonExistent == null, "HashMap returns null for non-existent ID");

        // ArrayList collection integrity
        List<Location> allLocs = lm.getAll();
        assertTrue(allLocs.size() == lm.size(), "ArrayList contains all registered locations");
    }

    private static void testPriorityQueueManagement() {
        System.out.println("\n-- Testing PriorityQueue Scheduling from Seed Data --");
        ReliefSystem system = ReliefSystem.getInstance();
        ReliefRequestManager rm = system.getRequestManager();

        ReliefRequest top = rm.peekNextPriorityRequest();
        assertTrue(top != null, "PriorityQueue has a top pending request");
        assertTrue(top.getUrgency() == UrgencyLevel.CRITICAL, "Top pending request is of CRITICAL urgency");

        // Ordered snapshot verification
        List<ReliefRequest> orderedList = rm.getPriorityOrderedPendingRequests();
        assertTrue(orderedList.size() == rm.getPendingCount(), "Priority-ordered list contains all pending requests");

        // Ensure each item in the ordered list is >= urgency than the subsequent item
        for (int i = 0; i < orderedList.size() - 1; i++) {
            ReliefRequest cur = orderedList.get(i);
            ReliefRequest next = orderedList.get(i + 1);
            int comp = cur.compareTo(next);
            assertTrue(comp <= 0, "PriorityQueue order invariant holds (" + cur.getId() + " <= " + next.getId() + ")");
        }
    }

    private static void testRecordUpdatingAndPersistence() {
        System.out.println("\n-- Testing Record Updating & File Sync --");
        ReliefSystem system = ReliefSystem.getInstance();
        DroneManager dm = system.getDroneManager();

        Drone drone = dm.getById("DRN-001");
        assertTrue(drone != null, "Found drone DRN-001");

        int initialBat = drone.getBatteryPercentage();
        drone.setBatteryPercentage(88);
        dm.update(drone);

        Drone reloaded = dm.getById("DRN-001");
        assertTrue(reloaded.getBatteryPercentage() == 88, "Drone battery updated in memory");

        // Restore battery
        drone.setBatteryPercentage(initialBat);
        dm.update(drone);
    }

    private static void testPersistenceAcrossApplicationRestart() {
        System.out.println("\n-- Testing Persistence Across Simulated Application Restart --");
        String testFile = "backend/data/test_restart_" + System.currentTimeMillis() + ".csv";

        // Step 1: Initialize Manager Instance 1
        DisasterManager instance1 = new DisasterManager(testFile);
        Disaster newDisaster = new Disaster("DIS-TEST-99", "Wildfire Outbreak", DisasterType.FIRE,
                "Southern Hills", SeverityLevel.CRITICAL, "Emergency forest fire evacuation", "2026-11-14");
        instance1.add(newDisaster);
        assertTrue(instance1.size() == 1, "Instance 1 added disaster and saved to file");

        // Step 2: Simulate Application Restart (Instantiate brand new Manager Instance 2)
        DisasterManager instance2 = new DisasterManager(testFile);
        instance2.loadFromFile();

        // Step 3: Verify Persistence
        assertTrue(instance2.size() == 1, "Instance 2 loaded exactly 1 record from CSV");
        Disaster persisted = instance2.getById("DIS-TEST-99");
        assertTrue(persisted != null, "Instance 2 found persisted disaster DIS-TEST-99");
        assertTrue(persisted.getName().equals("Wildfire Outbreak"), "Disaster name persisted with full fidelity");
        assertTrue(persisted.getType() == DisasterType.FIRE, "Disaster type enum persisted");
        assertTrue(persisted.getSeverity() == SeverityLevel.CRITICAL, "Disaster severity persisted");

        // Cleanup test file
        new File(testFile).delete();
    }
}
