package org.rescuedrone.model;

import org.rescuedrone.model.enums.DeliveryStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a planned or completed drone relief delivery operation.
 * Inherits BaseEntity.
 */
public class Delivery extends BaseEntity {
    private String requestId;
    private String droneId;
    private String sourceLocationId;
    private String destLocationId;
    private List<String> pathNodeIds;
    private double totalDistanceKm;
    private double deliveryCost;
    private DeliveryStatus status;
    private long dispatchTime;
    private long completionTime;

    public Delivery() {
        super();
        this.pathNodeIds = new ArrayList<>();
        this.status = DeliveryStatus.PENDING;
    }

    public Delivery(String id, String requestId, String droneId, String sourceLocationId,
                    String destLocationId, List<String> pathNodeIds, double totalDistanceKm,
                    double deliveryCost) {
        super(id);
        this.requestId = requestId;
        this.droneId = droneId;
        this.sourceLocationId = sourceLocationId;
        this.destLocationId = destLocationId;
        this.pathNodeIds = pathNodeIds != null ? new ArrayList<>(pathNodeIds) : new ArrayList<>();
        this.totalDistanceKm = Math.max(0.0, totalDistanceKm);
        this.deliveryCost = Math.max(0.0, deliveryCost);
        this.status = DeliveryStatus.PENDING;
        this.dispatchTime = 0;
        this.completionTime = 0;
    }

    public Delivery(String id, String requestId, String droneId, String sourceLocationId,
                    String destLocationId, List<String> pathNodeIds, double totalDistanceKm,
                    double deliveryCost, DeliveryStatus status, long dispatchTime,
                    long completionTime, long createdAt) {
        super(id, createdAt);
        this.requestId = requestId;
        this.droneId = droneId;
        this.sourceLocationId = sourceLocationId;
        this.destLocationId = destLocationId;
        this.pathNodeIds = pathNodeIds != null ? new ArrayList<>(pathNodeIds) : new ArrayList<>();
        this.totalDistanceKm = Math.max(0.0, totalDistanceKm);
        this.deliveryCost = Math.max(0.0, deliveryCost);
        this.status = status != null ? status : DeliveryStatus.PENDING;
        this.dispatchTime = dispatchTime;
        this.completionTime = completionTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDroneId() {
        return droneId;
    }

    public void setDroneId(String droneId) {
        this.droneId = droneId;
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

    public List<String> getPathNodeIds() {
        return pathNodeIds;
    }

    public void setPathNodeIds(List<String> pathNodeIds) {
        this.pathNodeIds = pathNodeIds != null ? new ArrayList<>(pathNodeIds) : new ArrayList<>();
    }

    public double getTotalDistanceKm() {
        return totalDistanceKm;
    }

    public void setTotalDistanceKm(double totalDistanceKm) {
        this.totalDistanceKm = Math.max(0.0, totalDistanceKm);
    }

    public double getDeliveryCost() {
        return deliveryCost;
    }

    public void setDeliveryCost(double deliveryCost) {
        this.deliveryCost = Math.max(0.0, deliveryCost);
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public long getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(long dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    public void startMission() {
        this.status = DeliveryStatus.IN_TRANSIT;
        this.dispatchTime = System.currentTimeMillis();
    }

    public void completeMission() {
        this.status = DeliveryStatus.COMPLETED;
        this.completionTime = System.currentTimeMillis();
    }

    public void cancelMission() {
        this.status = DeliveryStatus.CANCELLED;
        this.completionTime = System.currentTimeMillis();
    }

    @Override
    public void validate() {
        if (requestId == null || requestId.trim().isEmpty()) {
            throw new IllegalArgumentException("Request ID cannot be empty");
        }
        if (droneId == null || droneId.trim().isEmpty()) {
            throw new IllegalArgumentException("Drone ID cannot be empty");
        }
        if (sourceLocationId == null || sourceLocationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Source location cannot be empty");
        }
        if (destLocationId == null || destLocationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination location cannot be empty");
        }
        if (totalDistanceKm < 0.0) {
            throw new IllegalArgumentException("Total distance cannot be negative");
        }
        if (deliveryCost < 0.0) {
            throw new IllegalArgumentException("Delivery cost cannot be negative");
        }
        if (status == null) {
            status = DeliveryStatus.PENDING;
        }
    }

    @Override
    public String toCsvRecord() {
        String pathJoined = pathNodeIds != null ? String.join("|", pathNodeIds) : "";
        return String.join(",",
                escapeCsv(id),
                escapeCsv(requestId),
                escapeCsv(droneId),
                escapeCsv(sourceLocationId),
                escapeCsv(destLocationId),
                escapeCsv(pathJoined),
                String.valueOf(totalDistanceKm),
                String.valueOf(deliveryCost),
                status.name(),
                String.valueOf(dispatchTime),
                String.valueOf(completionTime),
                String.valueOf(createdAt)
        );
    }

    public static Delivery fromCsvTokens(String[] tokens) {
        if (tokens == null || tokens.length < 9) {
            throw new IllegalArgumentException("Malformed Delivery CSV record");
        }
        String id = tokens[0].trim();
        String reqId = tokens[1].trim();
        String droneId = tokens[2].trim();
        String src = tokens[3].trim();
        String dst = tokens[4].trim();
        String pathStr = tokens[5].trim();
        List<String> path = pathStr.isEmpty() ? new ArrayList<>() : Arrays.asList(pathStr.split("\\|"));
        double dist = Double.parseDouble(tokens[6].trim());
        double cost = Double.parseDouble(tokens[7].trim());
        DeliveryStatus status = DeliveryStatus.fromString(tokens[8].trim());
        long dispatch = tokens.length > 9 ? Long.parseLong(tokens[9].trim()) : 0;
        long completion = tokens.length > 10 ? Long.parseLong(tokens[10].trim()) : 0;
        long createdAt = tokens.length > 11 ? Long.parseLong(tokens[11].trim()) : System.currentTimeMillis();

        return new Delivery(id, reqId, droneId, src, dst, path, dist, cost, status, dispatch, completion, createdAt);
    }

    @Override
    public String toString() {
        return "Delivery{" +
                "id='" + id + '\'' +
                ", req='" + requestId + '\'' +
                ", drone='" + droneId + '\'' +
                ", " + sourceLocationId + " -> " + destLocationId +
                ", dist=" + totalDistanceKm + "km" +
                ", cost=₹" + deliveryCost +
                ", status=" + status +
                '}';
    }
}
