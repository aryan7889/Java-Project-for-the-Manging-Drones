package org.rescuedrone.model.enums;

/**
 * Types of disasters handled by RescueDrone.
 */
public enum DisasterType {
    FLOOD("Flood"),
    EARTHQUAKE("Earthquake"),
    CYCLONE("Cyclone"),
    LANDSLIDE("Landslide"),
    FIRE("Fire"),
    OTHER("Other");

    private final String displayName;

    DisasterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static DisasterType fromString(String text) {
        if (text == null) return OTHER;
        for (DisasterType dt : DisasterType.values()) {
            if (dt.name().equalsIgnoreCase(text.trim()) || dt.displayName.equalsIgnoreCase(text.trim())) {
                return dt;
            }
        }
        return OTHER;
    }
}
