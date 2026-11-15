package org.rescuedrone.config;

import java.io.File;

/**
 * Application-wide configuration parameters and paths.
 */
public class AppConfig {
    public static final int SERVER_PORT = 8080;
    public static final String SERVER_HOST = "0.0.0.0";
    public static final String APP_NAME = "RescueDrone";
    public static final String APP_VERSION = "1.0.0";

    // Data directory path relative to project root
    public static final String DATA_DIR = "backend/data";
    public static final String DISASTERS_FILE = DATA_DIR + File.separator + "disasters.csv";
    public static final String LOCATIONS_FILE = DATA_DIR + File.separator + "locations.csv";
    public static final String DRONES_FILE = DATA_DIR + File.separator + "drones.csv";
    public static final String RESOURCES_FILE = DATA_DIR + File.separator + "resources.csv";
    public static final String REQUESTS_FILE = DATA_DIR + File.separator + "relief_requests.csv";
    public static final String ROUTES_FILE = DATA_DIR + File.separator + "routes.csv";
    public static final String DELIVERIES_FILE = DATA_DIR + File.separator + "deliveries.csv";

    // Delivery Cost Parameters
    public static final double BASE_DELIVERY_CHARGE = 100.0; // ₹100
    public static final double DISTANCE_CHARGE_PER_KM = 20.0; // ₹20/km
    public static final double EMERGENCY_SURCHARGE = 50.0;    // ₹50
}
