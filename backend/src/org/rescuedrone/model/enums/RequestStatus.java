package org.rescuedrone.model.enums;

/**
 * Status lifecycle of a relief request.
 */
public enum RequestStatus {
    PENDING("Pending"),
    ASSIGNED("Assigned"),
    IN_TRANSIT("In Transit"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String displayName;

    RequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static RequestStatus fromString(String text) {
        if (text == null) return PENDING;
        for (RequestStatus rs : RequestStatus.values()) {
            if (rs.name().equalsIgnoreCase(text.trim()) || rs.displayName.equalsIgnoreCase(text.trim())) {
                return rs;
            }
        }
        return PENDING;
    }
}
