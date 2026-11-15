package org.rescuedrone.model;

/**
 * Represents a transportation route (weighted edge) between two locations in the relief graph.
 * Inherits BaseEntity.
 */
public class Route extends BaseEntity {
    private String sourceLocationId;
    private String destLocationId;
    private double distanceKm;

    public Route() {
        super();
    }

    public Route(String id, String sourceLocationId, String destLocationId, double distanceKm) {
        super(id);
        this.sourceLocationId = sourceLocationId;
        this.destLocationId = destLocationId;
        this.distanceKm = Math.max(0.1, distanceKm);
    }

    public Route(String id, String sourceLocationId, String destLocationId, double distanceKm, long createdAt) {
        super(id, createdAt);
        this.sourceLocationId = sourceLocationId;
        this.destLocationId = destLocationId;
        this.distanceKm = Math.max(0.1, distanceKm);
    }

    public String getSourceLocationId() {
        return sourceLocationId;
    }

    public void setSourceLocationId(String sourceLocationId) {
        this.sourceLocationId = sourceLocationId;
    }

    public String getDestLocationId() {
        return destLocationId;
    }

    public void setDestLocationId(String destLocationId) {
        this.destLocationId = destLocationId;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = Math.max(0.1, distanceKm);
    }

    @Override
    public void validate() {
        if (sourceLocationId == null || sourceLocationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Source location cannot be empty");
        }
        if (destLocationId == null || destLocationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination location cannot be empty");
        }
        if (sourceLocationId.trim().equalsIgnoreCase(destLocationId.trim())) {
            throw new IllegalArgumentException("Source and destination locations cannot be identical");
        }
        if (distanceKm <= 0.0) {
            throw new IllegalArgumentException("Route distance must be positive");
        }
    }

    @Override
    public String toCsvRecord() {
        return String.join(",",
                escapeCsv(id),
                escapeCsv(sourceLocationId),
                escapeCsv(destLocationId),
                String.valueOf(distanceKm),
                String.valueOf(createdAt)
        );
    }

    public static Route fromCsvTokens(String[] tokens) {
        if (tokens == null || tokens.length < 4) {
            throw new IllegalArgumentException("Malformed Route CSV record");
        }
        String id = tokens[0].trim();
        String src = tokens[1].trim();
        String dst = tokens[2].trim();
        double dist = Double.parseDouble(tokens[3].trim());
        long createdAt = tokens.length > 4 ? Long.parseLong(tokens[4].trim()) : System.currentTimeMillis();

        return new Route(id, src, dst, dist, createdAt);
    }

    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", " + sourceLocationId + " -> " + destLocationId +
                ", dist=" + distanceKm + "km" +
                '}';
    }
}
