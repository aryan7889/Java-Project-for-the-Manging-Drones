package org.rescuedrone.model.enums;

/**
 * Urgency levels for relief requests used by PriorityQueue.
 * Higher weight represents higher dispatch precedence.
 */
public enum UrgencyLevel {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");

    private final int weight;
    private final String displayName;

    UrgencyLevel(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static UrgencyLevel fromString(String text) {
        if (text == null) return LOW;
        for (UrgencyLevel u : UrgencyLevel.values()) {
            if (u.name().equalsIgnoreCase(text.trim()) || u.displayName.equalsIgnoreCase(text.trim())) {
                return u;
            }
        }
        return LOW;
    }
}
