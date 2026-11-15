package org.rescuedrone.persistence;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.*;
import org.rescuedrone.model.enums.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Seeds realistic default simulation records if CSV storage files are missing or empty.
 */
public class DataSeeder {

    public static synchronized void seedIfEmpty() {
        seedAll(false);
    }

    public static synchronized void seedAll(boolean force) {
        if (force) {
            new File(AppConfig.DISASTERS_FILE).delete();
            new File(AppConfig.LOCATIONS_FILE).delete();
            new File(AppConfig.DRONES_FILE).delete();
            new File(AppConfig.RESOURCES_FILE).delete();
            new File(AppConfig.ROUTES_FILE).delete();
            new File(AppConfig.REQUESTS_FILE).delete();
            new File(AppConfig.DELIVERIES_FILE).delete();
        }
        seedDisasters();
        seedLocations();
        seedDrones();
        seedResources();
        seedRoutes();
        seedRequests();
        seedDeliveries();
    }

    private static boolean isFileEmptyOrMissing(String path) {
        File file = new File(path);
        if (!file.exists() || file.length() == 0) return true;
        List<String> lines = FileManager.readLines(path);
        return lines.isEmpty();
    }

    private static void seedDisasters() {
        if (!isFileEmptyOrMissing(AppConfig.DISASTERS_FILE)) return;
        List<String> records = new ArrayList<>();
        records.add(new Disaster("DIS-001", "Monsoon Flash Flood", DisasterType.FLOOD, "North Valley Region",
                SeverityLevel.CRITICAL, "Flash flood breached embankment submerging road bridges", "2026-11-14").toCsvRecord());
        records.add(new Disaster("DIS-002", "Eastern Ridge Landslide", DisasterType.LANDSLIDE, "Sector 4 Hills",
                SeverityLevel.HIGH, "Severe landslide blocked ground transport corridors", "2026-11-13").toCsvRecord());
        FileManager.writeLines(AppConfig.DISASTERS_FILE, records);
        System.out.println("[DataSeeder] Seeded default disasters.");
    }

    private static void seedLocations() {
        if (!isFileEmptyOrMissing(AppConfig.LOCATIONS_FILE)) return;
        List<String> records = new ArrayList<>();
        records.add(new Location("LOC-001", "Central Hospital Base", 28.6139, 77.2090, "DIS-001", 0, SeverityLevel.LOW).toCsvRecord());
        records.add(new Location("LOC-002", "North Valley Camp A", 28.6350, 77.2250, "DIS-001", 450, SeverityLevel.CRITICAL).toCsvRecord());
        records.add(new Location("LOC-003", "East Ridge Village", 28.6500, 77.2400, "DIS-002", 280, SeverityLevel.HIGH).toCsvRecord());
        records.add(new Location("LOC-004", "Sector 2 Relief Post", 28.6250, 77.2350, "DIS-001", 180, SeverityLevel.HIGH).toCsvRecord());
        records.add(new Location("LOC-005", "Riverbank Shelter B", 28.6420, 77.2180, "DIS-001", 320, SeverityLevel.CRITICAL).toCsvRecord());
        records.add(new Location("LOC-006", "South Sector Clinic", 28.6050, 77.2150, "DIS-001", 110, SeverityLevel.MEDIUM).toCsvRecord());
        FileManager.writeLines(AppConfig.LOCATIONS_FILE, records);
        System.out.println("[DataSeeder] Seeded default locations.");
    }

    private static void seedDrones() {
        if (!isFileEmptyOrMissing(AppConfig.DRONES_FILE)) return;
        List<String> records = new ArrayList<>();
        records.add(new Drone("DRN-001", "SkyLifter Alpha", 20.0, 95, 45.0, "LOC-001", DroneStatus.AVAILABLE, "Heavy-Lift Hexacopter").toCsvRecord());
        records.add(new Drone("DRN-002", "AeroRelief Beta", 10.0, 75, 30.0, "LOC-001", DroneStatus.AVAILABLE, "Medium Quadcopter").toCsvRecord());
        records.add(new Drone("DRN-003", "SwiftDrop Gamma", 5.0, 100, 60.0, "LOC-001", DroneStatus.AVAILABLE, "Fast Medical VTOL").toCsvRecord());
        records.add(new Drone("DRN-004", "RescueTitan Delta", 25.0, 40, 50.0, "LOC-001", DroneStatus.MAINTENANCE, "Heavy Cargo Octocopter").toCsvRecord());
        FileManager.writeLines(AppConfig.DRONES_FILE, records);
        System.out.println("[DataSeeder] Seeded default drones.");
    }

    private static void seedResources() {
        if (!isFileEmptyOrMissing(AppConfig.RESOURCES_FILE)) return;
        List<String> records = new ArrayList<>();
        records.add(new Resource("RES-001", "Emergency First Aid Kits", ResourceCategory.FIRST_AID, 120, 10, "kits", 1.5).toCsvRecord());
        records.add(new Resource("RES-002", "Potable Drinking Water (5L)", ResourceCategory.WATER, 85, 25, "cans", 5.0).toCsvRecord());
        records.add(new Resource("RES-003", "Emergency Food Rations", ResourceCategory.FOOD, 150, 0, "packs", 1.0).toCsvRecord());
        records.add(new Resource("RES-004", "Trauma Medicine & Antiseptics", ResourceCategory.MEDICINES, 60, 5, "boxes", 0.8).toCsvRecord());
        records.add(new Resource("RES-005", "Thermal Emergency Blankets", ResourceCategory.EMERGENCY_SUPPLIES, 200, 0, "blankets", 0.5).toCsvRecord());
        FileManager.writeLines(AppConfig.RESOURCES_FILE, records);
        System.out.println("[DataSeeder] Seeded default resources.");
    }

    private static void seedRoutes() {
        if (!isFileEmptyOrMissing(AppConfig.ROUTES_FILE)) return;
        List<String> records = new ArrayList<>();
        records.add(new Route("RTE-001", "LOC-001", "LOC-002", 8.5).toCsvRecord());
        records.add(new Route("RTE-002", "LOC-001", "LOC-004", 6.0).toCsvRecord());
        records.add(new Route("RTE-003", "LOC-001", "LOC-006", 4.2).toCsvRecord());
        records.add(new Route("RTE-004", "LOC-002", "LOC-005", 5.1).toCsvRecord());
        records.add(new Route("RTE-005", "LOC-004", "LOC-003", 7.8).toCsvRecord());
        records.add(new Route("RTE-006", "LOC-002", "LOC-003", 5.7).toCsvRecord());
        records.add(new Route("RTE-007", "LOC-005", "LOC-003", 6.4).toCsvRecord());
        records.add(new Route("RTE-008", "LOC-006", "LOC-004", 4.5).toCsvRecord());
        FileManager.writeLines(AppConfig.ROUTES_FILE, records);
        System.out.println("[DataSeeder] Seeded default routes.");
    }

    private static void seedRequests() {
        if (!isFileEmptyOrMissing(AppConfig.REQUESTS_FILE)) return;
        List<String> records = new ArrayList<>();
        long now = System.currentTimeMillis();
        records.add(new ReliefRequest("REQ-001", "LOC-002", "RES-001", 10, UrgencyLevel.CRITICAL, 450,
                "Urgent trauma kits required immediately", RequestStatus.PENDING, now - 600000, now - 600000).toCsvRecord());
        records.add(new ReliefRequest("REQ-002", "LOC-005", "RES-002", 15, UrgencyLevel.CRITICAL, 320,
                "Drinking water needed as ground wells submerged", RequestStatus.PENDING, now - 500000, now - 500000).toCsvRecord());
        records.add(new ReliefRequest("REQ-003", "LOC-003", "RES-004", 5, UrgencyLevel.HIGH, 280,
                "Antibiotics and trauma dressings needed for landslide injured", RequestStatus.PENDING, now - 400000, now - 400000).toCsvRecord());
        records.add(new ReliefRequest("REQ-004", "LOC-004", "RES-003", 20, UrgencyLevel.HIGH, 180,
                "Emergency food packs for evacuated families", RequestStatus.PENDING, now - 300000, now - 300000).toCsvRecord());
        records.add(new ReliefRequest("REQ-005", "LOC-006", "RES-005", 10, UrgencyLevel.MEDIUM, 110,
                "Thermal blankets for temporary shelter", RequestStatus.PENDING, now - 200000, now - 200000).toCsvRecord());
        FileManager.writeLines(AppConfig.REQUESTS_FILE, records);
        System.out.println("[DataSeeder] Seeded default relief requests.");
    }

    private static void seedDeliveries() {
        if (!isFileEmptyOrMissing(AppConfig.DELIVERIES_FILE)) return;
        List<String> records = new ArrayList<>();
        long now = System.currentTimeMillis();
        records.add(new Delivery("DEL-0001", "REQ-001", "DRN-001", "LOC-001", "LOC-002",
                Arrays.asList("LOC-001", "LOC-002"), 8.5, 320.0, DeliveryStatus.COMPLETED,
                now - 3600000, now - 2400000, now - 3600000).toCsvRecord());
        FileManager.writeLines(AppConfig.DELIVERIES_FILE, records);
        System.out.println("[DataSeeder] Seeded default deliveries.");
    }
}
