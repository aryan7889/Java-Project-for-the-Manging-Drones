package org.rescuedrone.model;

import org.rescuedrone.model.enums.DroneStatus;

/**
 * Represents an unmanned aerial vehicle (drone) in the emergency relief fleet.
 * Inherits BaseEntity. Encapsulates payload capacity, battery state, and flight mechanics.
 */
public class Drone extends BaseEntity {
    private String name;
    private double maxCapacityKg;
    private int batteryPercentage;
    private double maxFlightRangeKm;
    private String currentLocationId;
    private DroneStatus status;
    private String droneType;

    public Drone() {
        super();
        this.batteryPercentage = 100;
        this.status = DroneStatus.AVAILABLE;
    }

    public Drone(String id, String name, double maxCapacityKg, int batteryPercentage,
                 double maxFlightRangeKm, String currentLocationId, DroneStatus status, String droneType) {
        super(id);
        this.name = name;
        this.maxCapacityKg = maxCapacityKg;
        this.batteryPercentage = Math.max(0, Math.min(100, batteryPercentage));
        this.maxFlightRangeKm = maxFlightRangeKm;
        this.currentLocationId = currentLocationId;
        this.status = status != null ? status : DroneStatus.AVAILABLE;
        this.droneType = droneType;
    }

    public Drone(String id, String name, double maxCapacityKg, int batteryPercentage,
                 double maxFlightRangeKm, String currentLocationId, DroneStatus status,
                 String droneType, long createdAt) {
        super(id, createdAt);
        this.name = name;
        this.maxCapacityKg = maxCapacityKg;
        this.batteryPercentage = Math.max(0, Math.min(100, batteryPercentage));
        this.maxFlightRangeKm = maxFlightRangeKm;
        this.currentLocationId = currentLocationId;
        this.status = status != null ? status : DroneStatus.AVAILABLE;
        this.droneType = droneType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMaxCapacityKg() {
        return maxCapacityKg;
    }

    public void setMaxCapacityKg(double maxCapacityKg) {
        this.maxCapacityKg = maxCapacityKg;
    }

    public int getBatteryPercentage() {
        return batteryPercentage;
    }

    public void setBatteryPercentage(int batteryPercentage) {
        this.batteryPercentage = Math.max(0, Math.min(100, batteryPercentage));
    }

    public double getMaxFlightRangeKm() {
        return maxFlightRangeKm;
    }

    public void setMaxFlightRangeKm(double maxFlightRangeKm) {
        this.maxFlightRangeKm = maxFlightRangeKm;
    }

    public String getCurrentLocationId() {
        return currentLocationId;
    }

    public void setCurrentLocationId(String currentLocationId) {
        this.currentLocationId = currentLocationId;
    }

    public DroneStatus getStatus() {
        return status;
    }

    public void setStatus(DroneStatus status) {
        this.status = status;
    }

    public String getDroneType() {
        return droneType;
    }

    public void setDroneType(String droneType) {
        this.droneType = droneType;
    }

    /**
     * Estimates battery percentage required for a given round-trip flight distance.
     * Rule: Consumes 2% battery per 1 km flown (round trip = 2x distance).
     * Plus 10% safety reserve buffer.
     */
    public int calculateRequiredBattery(double oneWayDistanceKm) {
        double roundTripKm = oneWayDistanceKm * 2.0;
        int required = (int) Math.ceil(roundTripKm * 1.5) + 10;
        return Math.min(100, required);
    }

    /**
     * Checks if this drone satisfies all capacity, battery, status, and range requirements.
     */
    public boolean isEligibleFor(double payloadWeightKg, double distanceKm) {
        if (this.status != DroneStatus.AVAILABLE) {
            return false;
        }
        if (payloadWeightKg > this.maxCapacityKg) {
            return false;
        }
        // Round trip must not exceed drone's maximum supported flight range
        if ((distanceKm * 2.0) > this.maxFlightRangeKm) {
            return false;
        }
        // Must have sufficient battery including safety reserve
        int requiredBattery = calculateRequiredBattery(distanceKm);
        return this.batteryPercentage >= requiredBattery;
    }

    /**
     * Deducts battery consumed after completing a flight mission.
     */
    public void consumeBattery(double distanceKm) {
        int consumed = (int) Math.ceil(distanceKm * 2.0 * 1.5);
        this.batteryPercentage = Math.max(0, this.batteryPercentage - consumed);
    }

    public void recharge() {
        this.batteryPercentage = 100;
        if (this.status == DroneStatus.MAINTENANCE) {
            this.status = DroneStatus.AVAILABLE;
        }
    }

    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Drone name cannot be empty");
        }
        if (maxCapacityKg <= 0.0) {
            throw new IllegalArgumentException("Max payload capacity must be positive");
        }
        if (batteryPercentage < 0 || batteryPercentage > 100) {
            throw new IllegalArgumentException("Battery percentage must be between 0 and 100");
        }
        if (maxFlightRangeKm <= 0.0) {
            throw new IllegalArgumentException("Max flight range must be positive");
        }
        if (status == null) {
            status = DroneStatus.AVAILABLE;
        }
    }

    @Override
    public String toCsvRecord() {
        return String.join(",",
                escapeCsv(id),
                escapeCsv(name),
                String.valueOf(maxCapacityKg),
                String.valueOf(batteryPercentage),
                String.valueOf(maxFlightRangeKm),
                escapeCsv(currentLocationId),
                status.name(),
                escapeCsv(droneType),
                String.valueOf(createdAt)
        );
    }

    public static Drone fromCsvTokens(String[] tokens) {
        if (tokens == null || tokens.length < 8) {
            throw new IllegalArgumentException("Malformed Drone CSV record");
        }
        String id = tokens[0].trim();
        String name = tokens[1].trim();
        double capacity = Double.parseDouble(tokens[2].trim());
        int battery = Integer.parseInt(tokens[3].trim());
        double range = Double.parseDouble(tokens[4].trim());
        String locId = tokens[5].trim();
        DroneStatus status = DroneStatus.fromString(tokens[6].trim());
        String type = tokens[7].trim();
        long createdAt = tokens.length > 8 ? Long.parseLong(tokens[8].trim()) : System.currentTimeMillis();

        return new Drone(id, name, capacity, battery, range, locId, status, type, createdAt);
    }

    @Override
    public String toString() {
        return "Drone{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cap=" + maxCapacityKg + "kg" +
                ", bat=" + batteryPercentage + "%" +
                ", range=" + maxFlightRangeKm + "km" +
                ", status=" + status +
                '}';
    }
}
