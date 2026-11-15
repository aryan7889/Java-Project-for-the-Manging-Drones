package org.rescuedrone.model.enums;

/**
 * Operational states of a drone in the fleet.
 */
public enum DroneStatus {
    AVAILABLE("Available"),
    ASSIGNED("Assigned"),
    IN_FLIGHT("In Flight"),
    MAINTENANCE("Maintenance");

    private final String displayName;

    DroneStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DroneStatus fromString(String text) {
        if (text == null) return AVAILABLE;
        for (DroneStatus ds : DroneStatus.values()) {
            if (ds.name().equalsIgnoreCase(text.trim()) || ds.displayName.equalsIgnoreCase(text.trim())) {
                return ds;
            }
        }
        return AVAILABLE;
    }
}
