package org.rescuedrone.model;

import org.rescuedrone.model.enums.SeverityLevel;

/**
 * Represents an affected geographical location, relief camp, or medical base.
 * Acts as a vertex in the transportation Graph.
 * Inherits BaseEntity.
 */
public class Location extends BaseEntity {
    private String name;
    private double latitude;
    private double longitude;
    private String disasterId;
    private int affectedPopulation;
    private SeverityLevel severity;

    public Location() {
        super();
        this.severity = SeverityLevel.LOW;
    }

    public Location(String id, String name, double latitude, double longitude,
                    String disasterId, int affectedPopulation, SeverityLevel severity) {
        super(id);
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.disasterId = disasterId;
        this.affectedPopulation = Math.max(0, affectedPopulation);
        this.severity = severity != null ? severity : SeverityLevel.LOW;
    }

    public Location(String id, String name, double latitude, double longitude,
                    String disasterId, int affectedPopulation, SeverityLevel severity, long createdAt) {
        super(id, createdAt);
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.disasterId = disasterId;
        this.affectedPopulation = Math.max(0, affectedPopulation);
        this.severity = severity != null ? severity : SeverityLevel.LOW;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(String disasterId) {
        this.disasterId = disasterId;
    }

    public int getAffectedPopulation() {
        return affectedPopulation;
    }

    public void setAffectedPopulation(int affectedPopulation) {
        this.affectedPopulation = Math.max(0, affectedPopulation);
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public void setSeverity(SeverityLevel severity) {
        this.severity = severity;
    }

    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be empty");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
        if (affectedPopulation < 0) {
            throw new IllegalArgumentException("Affected population cannot be negative");
        }
        if (severity == null) {
            severity = SeverityLevel.LOW;
        }
    }

    @Override
    public String toCsvRecord() {
        return String.join(",",
                escapeCsv(id),
                escapeCsv(name),
                String.valueOf(latitude),
                String.valueOf(longitude),
                escapeCsv(disasterId),
                String.valueOf(affectedPopulation),
                severity.name(),
                String.valueOf(createdAt)
        );
    }

    public static Location fromCsvTokens(String[] tokens) {
        if (tokens == null || tokens.length < 7) {
            throw new IllegalArgumentException("Malformed Location CSV record");
        }
        String id = tokens[0].trim();
        String name = tokens[1].trim();
        double lat = Double.parseDouble(tokens[2].trim());
        double lon = Double.parseDouble(tokens[3].trim());
        String disasterId = tokens[4].trim();
        int pop = Integer.parseInt(tokens[5].trim());
        SeverityLevel severity = SeverityLevel.fromString(tokens[6].trim());
        long createdAt = tokens.length > 7 ? Long.parseLong(tokens[7].trim()) : System.currentTimeMillis();

        return new Location(id, name, lat, lon, disasterId, pop, severity, createdAt);
    }

    @Override
    public String toString() {
        return "Location{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", coords=(" + latitude + "," + longitude + ")" +
                ", pop=" + affectedPopulation +
                ", severity=" + severity +
                '}';
    }
}
