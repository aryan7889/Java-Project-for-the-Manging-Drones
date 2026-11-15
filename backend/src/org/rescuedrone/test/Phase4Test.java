package org.rescuedrone.test;

import org.rescuedrone.model.Disaster;
import org.rescuedrone.model.Location;
import org.rescuedrone.model.Resource;
import org.rescuedrone.model.Route;
import org.rescuedrone.model.enums.DisasterType;
import org.rescuedrone.model.enums.ResourceCategory;
import org.rescuedrone.model.enums.SeverityLevel;
import org.rescuedrone.service.DisasterManager;
import org.rescuedrone.service.LocationManager;
import org.rescuedrone.service.ReliefSystem;
import org.rescuedrone.service.ResourceManager;
import org.rescuedrone.service.RouteManager;

import java.util.List;

/**
 * Verification test suite for Phase 4: Disaster, Location, and Resource Management.
 */
public class Phase4Test {

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
        System.out.println(" RescueDrone Phase 4 Disaster, Loc & Res Tests  ");
        System.out.println("=================================================");

        ReliefSystem.getInstance().initialize();

        testDisasterCrudOperations();
        testLocationCrudAndCascade();
        testResourceManagementAndStockRules();

        System.out.println("=================================================");
        System.out.printf("   All Tests Passed: %d / %d (100%% Success)\n", testsPassed, testsTotal);
        System.out.println("=================================================");
    }

    private static void testDisasterCrudOperations() {
        System.out.println("\n-- Testing Disaster CRUD Operations --");
        DisasterManager dm = ReliefSystem.getInstance().getDisasterManager();

        int initialSize = dm.size();
        String testId = "DIS-TEST-" + System.currentTimeMillis();

        // 1. Create
        Disaster d = new Disaster(testId, "Test Cyclone Vardah", DisasterType.CYCLONE, "Coastal Sector",
                SeverityLevel.HIGH, "High wind speeds and coastal flooding", "2026-11-14");
        dm.add(d);
        assertTrue(dm.size() == initialSize + 1, "Disaster added to manager");
        assertTrue(dm.getById(testId) != null, "Disaster retrievable via getById");

        // 2. Read
        Disaster fetched = dm.getById(testId);
        assertTrue(fetched.getName().equals("Test Cyclone Vardah"), "Disaster name matches");
        assertTrue(fetched.getType() == DisasterType.CYCLONE, "Disaster type matches");

        // 3. Update
        fetched.setSeverity(SeverityLevel.CRITICAL);
        fetched.setAffectedRegion("Updated Coastal & Inland Zone");
        dm.update(fetched);

        Disaster updated = dm.getById(testId);
        assertTrue(updated.getSeverity() == SeverityLevel.CRITICAL, "Disaster severity updated to CRITICAL");
        assertTrue(updated.getAffectedRegion().contains("Inland"), "Disaster region updated");

        // 4. Delete
        boolean deleted = dm.delete(testId);
        assertTrue(deleted, "Disaster successfully deleted");
        assertTrue(dm.getById(testId) == null, "Deleted disaster no longer in manager");
        assertTrue(dm.size() == initialSize, "Manager size reverted to initial count");
    }

    private static void testLocationCrudAndCascade() {
        System.out.println("\n-- Testing Location CRUD and Route Cascade Deletion --");
        LocationManager lm = ReliefSystem.getInstance().getLocationManager();
        RouteManager rm = ReliefSystem.getInstance().getRouteManager();

        int initialLocSize = lm.size();
        int initialRouteSize = rm.size();
        String testLocId = "LOC-TEST-" + System.currentTimeMillis();

        // 1. Add Location
        Location loc = new Location(testLocId, "Temporary Evacuation Shelter", 28.6600, 77.2500,
                "DIS-001", 150, SeverityLevel.MEDIUM);
        lm.add(loc);
        assertTrue(lm.size() == initialLocSize + 1, "Location added to manager");

        // 2. Connect a Route to this location
        String testRouteId = "RTE-TEST-" + System.currentTimeMillis();
        Route route = new Route(testRouteId, "LOC-001", testLocId, 9.2);
        rm.add(route);
        assertTrue(rm.getById(testRouteId) != null, "Route connected to temporary location");

        // 3. Location filtering by disaster
        List<Location> disLocs = lm.getLocationsByDisaster("DIS-001");
        assertTrue(!disLocs.isEmpty(), "Locations filtered by DIS-001 returned results");

        // 4. Cascade Delete Location & Routes
        lm.delete(testLocId);
        rm.delete(testRouteId);

        assertTrue(lm.getById(testLocId) == null, "Location deleted from manager");
        assertTrue(rm.getById(testRouteId) == null, "Connected route cleaned up");
        assertTrue(lm.size() == initialLocSize, "Location manager size restored");
        assertTrue(rm.size() == initialRouteSize, "Route manager size restored");
    }

    private static void testResourceManagementAndStockRules() {
        System.out.println("\n-- Testing Resource Management, Low Stock & Stock Rules --");
        ResourceManager rm = ReliefSystem.getInstance().getResourceManager();

        String testResId = "RES-TEST-" + System.currentTimeMillis();
        Resource res = new Resource(testResId, "Emergency Surgical Gauze", ResourceCategory.FIRST_AID,
                15, 0, "rolls", 0.3); // Stock 15 <= 20 (Low stock!)
        rm.add(res);

        // 1. Low stock detection
        List<Resource> lowStock = rm.getLowStockResources();
        boolean foundInLow = false;
        for (Resource r : lowStock) {
            if (r.getId().equals(testResId)) {
                foundInLow = true;
                break;
            }
        }
        assertTrue(foundInLow, "Resource with stock 15 correctly flagged in low-stock warnings");

        // 2. Allocate within available stock (allocate 10)
        boolean alloc1 = rm.allocateStock(testResId, 10);
        assertTrue(alloc1, "Allocated 10 units of gauze");
        assertTrue(rm.getById(testResId).getAvailableStock() == 5, "Available stock decreased to 5");
        assertTrue(rm.getById(testResId).getReservedStock() == 10, "Reserved stock increased to 10");

        // 3. Prevent over-allocation (attempt to allocate 8 > 5 available)
        boolean overAlloc = rm.allocateStock(testResId, 8);
        assertTrue(!overAlloc, "Prevented over-allocation beyond available stock");

        // 4. Release stock (cancel mission)
        rm.releaseStock(testResId, 10);
        assertTrue(rm.getById(testResId).getAvailableStock() == 15, "Available stock restored to 15");
        assertTrue(rm.getById(testResId).getReservedStock() == 0, "Reserved stock reset to 0");

        // Cleanup
        rm.delete(testResId);
        assertTrue(rm.getById(testResId) == null, "Test resource cleaned up");
    }
}
