package org.rescuedrone.model;

import org.rescuedrone.model.enums.RequestStatus;
import org.rescuedrone.model.enums.UrgencyLevel;

/**
 * Represents an emergency relief request submitted from an affected location.
 * Inherits BaseEntity and implements Comparable for PriorityQueue scheduling.
 */
public class ReliefRequest extends BaseEntity implements Comparable<ReliefRequest> {
    private String locationId;
    private String resourceId;
    private int quantity;
    private UrgencyLevel urgency;
    private int affectedPeople;
    private String description;
    private RequestStatus status;
    private long requestTimestamp;

    public ReliefRequest() {
        super();
        this.urgency = UrgencyLevel.LOW;
        this.status = RequestStatus.PENDING;
        this.requestTimestamp = System.currentTimeMillis();
    }

    public ReliefRequest(String id, String locationId, String resourceId, int quantity,
                         UrgencyLevel urgency, int affectedPeople, String description) {
        super(id);
        this.locationId = locationId;
        this.resourceId = resourceId;
        this.quantity = Math.max(1, quantity);
        this.urgency = urgency != null ? urgency : UrgencyLevel.LOW;
        this.affectedPeople = Math.max(0, affectedPeople);
        this.description = description;
        this.status = RequestStatus.PENDING;
        this.requestTimestamp = System.currentTimeMillis();
    }

    public ReliefRequest(String id, String locationId, String resourceId, int quantity,
                         UrgencyLevel urgency, int affectedPeople, String description,
                         RequestStatus status, long requestTimestamp, long createdAt) {
        super(id, createdAt);
        this.locationId = locationId;
        this.resourceId = resourceId;
        this.quantity = Math.max(1, quantity);
        this.urgency = urgency != null ? urgency : UrgencyLevel.LOW;
        this.affectedPeople = Math.max(0, affectedPeople);
        this.description = description;
        this.status = status != null ? status : RequestStatus.PENDING;
        this.requestTimestamp = requestTimestamp > 0 ? requestTimestamp : System.currentTimeMillis();
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
    }

    public UrgencyLevel getUrgency() {
        return urgency;
    }

    public void setUrgency(UrgencyLevel urgency) {
        this.urgency = urgency;
    }

    public int getAffectedPeople() {
        return affectedPeople;
    }

    public void setAffectedPeople(int affectedPeople) {
        this.affectedPeople = Math.max(0, affectedPeople);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public long getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(long requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    /**
     * Comparable implementation for Java PriorityQueue.
     * Rule:
     * 1. Higher urgency weight comes first (CRITICAL > HIGH > MEDIUM > LOW).
     * 2. If urgency is equal, higher affected people count comes first.
     * 3. If still equal, earlier request timestamp comes first (FIFO fairness).
     */
    @Override
    public int compareTo(ReliefRequest other) {
        if (other == null) return -1;

        // Primary: Urgency Level (descending weight)
        int urgencyDiff = Integer.compare(other.urgency.getWeight(), this.urgency.getWeight());
        if (urgencyDiff != 0) return urgencyDiff;

        // Secondary: Affected People (descending count)
        int popDiff = Integer.compare(other.affectedPeople, this.affectedPeople);
        if (popDiff != 0) return popDiff;

        // Tertiary: Timestamp (ascending - older requests first)
        return Long.compare(this.requestTimestamp, other.requestTimestamp);
    }

    @Override
    public void validate() {
        if (locationId == null || locationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Location ID cannot be empty");
        }
        if (resourceId == null || resourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource ID cannot be empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Requested quantity must be positive");
        }
        if (affectedPeople < 0) {
            throw new IllegalArgumentException("Affected people count cannot be negative");
        }
        if (urgency == null) {
            urgency = UrgencyLevel.LOW;
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }

    @Override
    public String toCsvRecord() {
        return String.join(",",
                escapeCsv(id),
                escapeCsv(locationId),
                escapeCsv(resourceId),
                String.valueOf(quantity),
                urgency.name(),
                String.valueOf(affectedPeople),
                escapeCsv(description),
                status.name(),
                String.valueOf(requestTimestamp),
                String.valueOf(createdAt)
        );
    }

    public static ReliefRequest fromCsvTokens(String[] tokens) {
        if (tokens == null || tokens.length < 8) {
            throw new IllegalArgumentException("Malformed ReliefRequest CSV record");
        }
        String id = tokens[0].trim();
        String locId = tokens[1].trim();
        String resId = tokens[2].trim();
        int qty = Integer.parseInt(tokens[3].trim());
        UrgencyLevel urgency = UrgencyLevel.fromString(tokens[4].trim());
        int people = Integer.parseInt(tokens[5].trim());
        String desc = tokens[6].trim();
        RequestStatus status = RequestStatus.fromString(tokens[7].trim());
        long ts = tokens.length > 8 ? Long.parseLong(tokens[8].trim()) : System.currentTimeMillis();
        long createdAt = tokens.length > 9 ? Long.parseLong(tokens[9].trim()) : ts;

        return new ReliefRequest(id, locId, resId, qty, urgency, people, desc, status, ts, createdAt);
    }

    @Override
    public String toString() {
        return "ReliefRequest{" +
                "id='" + id + '\'' +
                ", loc='" + locationId + '\'' +
                ", res='" + resourceId + '\'' +
                ", qty=" + quantity +
                ", urgency=" + urgency +
                ", people=" + affectedPeople +
                ", status=" + status +
                '}';
    }
}
