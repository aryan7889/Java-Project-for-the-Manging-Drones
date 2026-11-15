package org.rescuedrone.model.enums;

/**
 * Categories of disaster relief inventory.
 */
public enum ResourceCategory {
    FOOD("Food"),
    WATER("Drinking Water"),
    MEDICINES("Medicines"),
    FIRST_AID("First Aid Kits"),
    EMERGENCY_SUPPLIES("Emergency Supplies");

    private final String displayName;

    ResourceCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ResourceCategory fromString(String text) {
        if (text == null) return EMERGENCY_SUPPLIES;
        for (ResourceCategory rc : ResourceCategory.values()) {
            if (rc.name().equalsIgnoreCase(text.trim()) || rc.displayName.equalsIgnoreCase(text.trim())) {
                return rc;
            }
        }
        return EMERGENCY_SUPPLIES;
    }
}
