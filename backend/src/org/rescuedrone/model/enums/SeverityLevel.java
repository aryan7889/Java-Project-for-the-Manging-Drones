package org.rescuedrone.model.enums;

/**
 * Severity levels for disasters and affected locations.
 * Includes numerical weight for ranking and comparisons.
 */
public enum SeverityLevel {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");

    private final int weight;
    private final String displayName;

    SeverityLevel(int weight, String displayName) {
        this.weight = weight;
        this.displayName = displayName;
    }

    public int getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static SeverityLevel fromString(String text) {
        if (text == null) return LOW;
        for (SeverityLevel s : SeverityLevel.values()) {
            if (s.name().equalsIgnoreCase(text.trim()) || s.displayName.equalsIgnoreCase(text.trim())) {
                return s;
            }
        }
        return LOW;
    }
}
