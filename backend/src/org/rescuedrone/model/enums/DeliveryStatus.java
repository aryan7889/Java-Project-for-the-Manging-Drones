package org.rescuedrone.model.enums;

/**
 * Status lifecycle of a drone delivery operation.
 */
public enum DeliveryStatus {
    PENDING("Pending"),
    IN_TRANSIT("In Transit"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    DeliveryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DeliveryStatus fromString(String text) {
        if (text == null) return PENDING;
        for (DeliveryStatus ds : DeliveryStatus.values()) {
            if (ds.name().equalsIgnoreCase(text.trim()) || ds.displayName.equalsIgnoreCase(text.trim())) {
                return ds;
            }
        }
        return PENDING;
    }
}
