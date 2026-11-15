package org.rescuedrone.model;

import org.rescuedrone.model.enums.ResourceCategory;

/**
 * Represents emergency relief supplies inventory (Food, Water, Medicines, Kits).
 * Inherits BaseEntity. Encapsulates inventory reservation and allocation logic.
 */
public class Resource extends BaseEntity {
    private String name;
    private ResourceCategory category;
    private int availableStock;
    private int reservedStock;
    private String unit;
    private double unitWeightKg;

    public Resource() {
        super();
        this.category = ResourceCategory.EMERGENCY_SUPPLIES;
        this.unitWeightKg = 1.0;
    }

    public Resource(String id, String name, ResourceCategory category,
                    int availableStock, int reservedStock, String unit, double unitWeightKg) {
        super(id);
        this.name = name;
        this.category = category != null ? category : ResourceCategory.EMERGENCY_SUPPLIES;
        this.availableStock = Math.max(0, availableStock);
        this.reservedStock = Math.max(0, reservedStock);
        this.unit = unit != null ? unit : "units";
        this.unitWeightKg = unitWeightKg > 0 ? unitWeightKg : 1.0;
    }

    public Resource(String id, String name, ResourceCategory category,
                    int availableStock, int reservedStock, String unit, double unitWeightKg, long createdAt) {
        super(id, createdAt);
        this.name = name;
        this.category = category != null ? category : ResourceCategory.EMERGENCY_SUPPLIES;
        this.availableStock = Math.max(0, availableStock);
        this.reservedStock = Math.max(0, reservedStock);
        this.unit = unit != null ? unit : "units";
        this.unitWeightKg = unitWeightKg > 0 ? unitWeightKg : 1.0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public void setCategory(ResourceCategory category) {
        this.category = category;
    }

    public int getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(int availableStock) {
        this.availableStock = Math.max(0, availableStock);
    }

    public int getReservedStock() {
        return reservedStock;
    }

    public void setReservedStock(int reservedStock) {
        this.reservedStock = Math.max(0, reservedStock);
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getUnitWeightKg() {
        return unitWeightKg;
    }

    public void setUnitWeightKg(double unitWeightKg) {
        this.unitWeightKg = unitWeightKg > 0 ? unitWeightKg : 1.0;
    }

    public int getTotalStock() {
        return availableStock + reservedStock;
    }

    public boolean hasSufficientStock(int quantity) {
        return quantity > 0 && this.availableStock >= quantity;
    }

    /**
     * Reserves requested stock for an active relief mission.
     */
    public synchronized boolean allocate(int quantity) {
        if (!hasSufficientStock(quantity)) {
            return false;
        }
        this.availableStock -= quantity;
        this.reservedStock += quantity;
        return true;
    }

    /**
     * Releases reserved stock back to available pool (e.g. if mission is cancelled).
     */
    public synchronized void release(int quantity) {
        int amountToRelease = Math.min(this.reservedStock, quantity);
        this.reservedStock -= amountToRelease;
        this.availableStock += amountToRelease;
    }

    /**
     * Permanently consumes reserved stock upon delivery completion.
     */
    public synchronized void consume(int quantity) {
        int amountToConsume = Math.min(this.reservedStock, quantity);
        this.reservedStock -= amountToConsume;
    }

    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name cannot be empty");
        }
        if (availableStock < 0) {
            throw new IllegalArgumentException("Available stock cannot be negative");
        }
        if (reservedStock < 0) {
            throw new IllegalArgumentException("Reserved stock cannot be negative");
        }
        if (unitWeightKg <= 0.0) {
            throw new IllegalArgumentException("Unit weight must be positive");
        }
    }

    @Override
    public String toCsvRecord() {
        return String.join(",",
                escapeCsv(id),
                escapeCsv(name),
                category.name(),
                String.valueOf(availableStock),
                String.valueOf(reservedStock),
                escapeCsv(unit),
                String.valueOf(unitWeightKg),
                String.valueOf(createdAt)
        );
    }

    public static Resource fromCsvTokens(String[] tokens) {
        if (tokens == null || tokens.length < 6) {
            throw new IllegalArgumentException("Malformed Resource CSV record");
        }
        String id = tokens[0].trim();
        String name = tokens[1].trim();
        ResourceCategory cat = ResourceCategory.fromString(tokens[2].trim());
        int avail = Integer.parseInt(tokens[3].trim());
        int res = Integer.parseInt(tokens[4].trim());
        String unit = tokens[5].trim();
        double weight = tokens.length > 6 ? Double.parseDouble(tokens[6].trim()) : 1.0;
        long createdAt = tokens.length > 7 ? Long.parseLong(tokens[7].trim()) : System.currentTimeMillis();

        return new Resource(id, name, cat, avail, res, unit, weight, createdAt);
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cat=" + category +
                ", avail=" + availableStock +
                ", reserved=" + reservedStock +
                '}';
    }
}
