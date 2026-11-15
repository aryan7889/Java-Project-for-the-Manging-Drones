package org.rescuedrone.model;

/**
 * Abstract base class for all operational entities in RescueDrone.
 * Demonstrates Abstraction and Encapsulation.
 * Defines standard identification, audit timestamp, and validation contracts.
 */
public abstract class BaseEntity {
    protected String id;
    protected long createdAt;

    public BaseEntity() {
        this.createdAt = System.currentTimeMillis();
    }

    public BaseEntity(String id) {
        this.id = id;
        this.createdAt = System.currentTimeMillis();
    }

    public BaseEntity(String id, long createdAt) {
        this.id = id;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Validates domain constraints. Throws IllegalArgumentException on invalid state.
     */
    public abstract void validate();

    /**
     * Serializes this entity into a sanitised CSV record string.
     */
    public abstract String toCsvRecord();

    /**
     * Helper to safely escape CSV values containing commas or quotes.
     */
    protected String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
