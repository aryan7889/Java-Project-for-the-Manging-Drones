package org.rescuedrone.model;

import org.rescuedrone.model.enums.DisasterType;
import org.rescuedrone.model.enums.SeverityLevel;

/**
 * Represents a registered natural or man-made disaster event.
 * Inherits BaseEntity.
 */
public class Disaster extends BaseEntity {
    private String name;
    private DisasterType type;
    private String affectedRegion;
    private SeverityLevel severity;
    private String description;
    private String date;

    public Disaster() {
        super();
        this.type = DisasterType.OTHER;
        this.severity = SeverityLevel.MEDIUM;
    }

    public Disaster(String id, String name, DisasterType type, String affectedRegion,
                    SeverityLevel severity, String description, String date) {
        super(id);
        this.name = name;
        this.type = type != null ? type : DisasterType.OTHER;
        this.affectedRegion = affectedRegion;
        this.severity = severity != null ? severity : SeverityLevel.MEDIUM;
        this.description = description;
        this.date = date;
    }

    public Disaster(String id, String name, DisasterType type, String affectedRegion,
                    SeverityLevel severity, String description, String date, long createdAt) {
        super(id, createdAt);
        this.name = name;
        this.type = type != null ? type : DisasterType.OTHER;
        this.affectedRegion = affectedRegion;
        this.severity = severity != null ? severity : SeverityLevel.MEDIUM;
        this.description = description;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DisasterType getType() {
        return type;
    }

    public void setType(DisasterType type) {
        this.type = type;
    }

    public String getAffectedRegion() {
        return affectedRegion;
    }

    public void setAffectedRegion(String affectedRegion) {
        this.affectedRegion = affectedRegion;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public void setSeverity(SeverityLevel severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Disaster name cannot be empty");
        }
        if (affectedRegion == null || affectedRegion.trim().isEmpty()) {
            throw new IllegalArgumentException("Affected region cannot be empty");
        }
        if (type == null) {
            type = DisasterType.OTHER;
        }
        if (severity == null) {
            severity = SeverityLevel.MEDIUM;
        }
    }

    @Override
    public String toCsvRecord() {
        return String.join(",",
                escapeCsv(id),
                escapeCsv(name),
                type.name(),
                escapeCsv(affectedRegion),
                severity.name(),
                escapeCsv(description),
                escapeCsv(date),
                String.valueOf(createdAt)
        );
    }

    public static Disaster fromCsvTokens(String[] tokens) {
        if (tokens == null || tokens.length < 7) {
            throw new IllegalArgumentException("Malformed Disaster CSV record");
        }
        String id = tokens[0].trim();
        String name = tokens[1].trim();
        DisasterType type = DisasterType.fromString(tokens[2].trim());
        String region = tokens[3].trim();
        SeverityLevel severity = SeverityLevel.fromString(tokens[4].trim());
        String desc = tokens[5].trim();
        String date = tokens[6].trim();
        long createdAt = tokens.length > 7 ? Long.parseLong(tokens[7].trim()) : System.currentTimeMillis();

        return new Disaster(id, name, type, region, severity, desc, date, createdAt);
    }

    @Override
    public String toString() {
        return "Disaster{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", region='" + affectedRegion + '\'' +
                ", severity=" + severity +
                '}';
    }
}
