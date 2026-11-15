package org.rescuedrone.algorithm;

import org.rescuedrone.model.Drone;
import org.rescuedrone.model.Location;
import org.rescuedrone.model.ReliefRequest;
import org.rescuedrone.model.Resource;
import org.rescuedrone.model.enums.DroneStatus;
import org.rescuedrone.model.enums.RequestStatus;
import org.rescuedrone.service.LocationManager;
import org.rescuedrone.service.ReliefSystem;
import org.rescuedrone.service.RouteManager;

import java.util.*;

/**
 * Intelligent Drone Suitability and Allocation Engine.
 * Evaluates candidate drones against payload capacity, battery percentage,
 * and operational flight ranges using deterministic Least Excess Capacity ranking.
 */
public class DroneAllocator {

    public static class CandidateEvaluation {
        private final Drone drone;
        private final boolean eligible;
        private final String rejectionReason;
        private final double excessCapacityKg;
        private final int requiredBattery;

        public CandidateEvaluation(Drone drone, boolean eligible, String rejectionReason,
                                   double excessCapacityKg, int requiredBattery) {
            this.drone = drone;
            this.eligible = eligible;
            this.rejectionReason = rejectionReason;
            this.excessCapacityKg = excessCapacityKg;
            this.requiredBattery = requiredBattery;
        }

        public Drone getDrone() {
            return drone;
        }

        public boolean isEligible() {
            return eligible;
        }

        public String getRejectionReason() {
            return rejectionReason;
        }

        public double getExcessCapacityKg() {
            return excessCapacityKg;
        }

        public int getRequiredBattery() {
            return requiredBattery;
        }
    }

    public static class AllocationResult {
        private final boolean success;
        private final String message;
        private final ReliefRequest request;
        private final Drone selectedDrone;
        private final double payloadWeightKg;
        private final double flightDistanceKm;
        private final List<CandidateEvaluation> evaluations;

        public AllocationResult(boolean success, String message, ReliefRequest request,
                                Drone selectedDrone, double payloadWeightKg,
                                double flightDistanceKm, List<CandidateEvaluation> evaluations) {
            this.success = success;
            this.message = message;
            this.request = request;
            this.selectedDrone = selectedDrone;
            this.payloadWeightKg = payloadWeightKg;
            this.flightDistanceKm = flightDistanceKm;
            this.evaluations = evaluations != null ? evaluations : new ArrayList<>();
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public ReliefRequest getRequest() {
            return request;
        }

        public Drone getSelectedDrone() {
            return selectedDrone;
        }

        public double getPayloadWeightKg() {
            return payloadWeightKg;
        }

        public double getFlightDistanceKm() {
            return flightDistanceKm;
        }

        public List<CandidateEvaluation> getEvaluations() {
            return evaluations;
        }
    }

    /**
     * Evaluates all fleet drones and selects the optimal drone for a relief request.
     */
    public static AllocationResult evaluateAndAssign(String requestId, double flightDistanceKm) {
        ReliefSystem system = ReliefSystem.getInstance();
        ReliefRequest request = system.getRequestManager().getById(requestId);

        if (request == null) {
            return new AllocationResult(false, "Relief request not found with ID: " + requestId,
                    null, null, 0.0, flightDistanceKm, null);
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            return new AllocationResult(false, "Request " + requestId + " is not in PENDING state (Current: " + request.getStatus() + ")",
                    request, null, 0.0, flightDistanceKm, null);
        }

        Resource resource = system.getResourceManager().getById(request.getResourceId());
        double unitWeight = resource != null ? resource.getUnitWeightKg() : 1.0;
        double totalPayloadKg = request.getQuantity() * unitWeight;

        // Check inventory sufficiency first
        if (resource != null && !resource.hasSufficientStock(request.getQuantity())) {
            return new AllocationResult(false,
                    String.format("Insufficient inventory for %s: Requested %d, Available %d",
                            resource.getName(), request.getQuantity(), resource.getAvailableStock()),
                    request, null, totalPayloadKg, flightDistanceKm, null);
        }

        // If distance is not custom-specified, resolve real shortest path distance using Dijkstra
        org.rescuedrone.ds.Graph liveGraph = system.getRouteManager().buildGraph();
        if (flightDistanceKm <= 0 || flightDistanceKm == 10.0) {
            String defaultHubId = "LOC-001";
            DijkstraOptimizer.PathResult pathRes = DijkstraOptimizer.findShortestPath(
                    liveGraph, defaultHubId, request.getLocationId());
            if (pathRes != null && pathRes.isFound() && pathRes.getTotalDistanceKm() > 0) {
                flightDistanceKm = pathRes.getTotalDistanceKm();
            }
        }

        List<Drone> allDrones = system.getDroneManager().getAll();
        List<CandidateEvaluation> evaluations = new ArrayList<>();
        List<CandidateEvaluation> eligibleCandidates = new ArrayList<>();

        for (Drone drone : allDrones) {
            double effectiveDistanceKm = flightDistanceKm;
            if (drone.getCurrentLocationId() != null && !drone.getCurrentLocationId().isEmpty() &&
                    !drone.getCurrentLocationId().equalsIgnoreCase(request.getLocationId())) {
                DijkstraOptimizer.PathResult dronePath = DijkstraOptimizer.findShortestPath(
                        liveGraph, drone.getCurrentLocationId(), request.getLocationId());
                if (dronePath != null && dronePath.isFound() && dronePath.getTotalDistanceKm() > 0) {
                    effectiveDistanceKm = dronePath.getTotalDistanceKm();
                }
            }

            int requiredBattery = drone.calculateRequiredBattery(effectiveDistanceKm);
            double excessCapacity = drone.getMaxCapacityKg() - totalPayloadKg;

            if (drone.getStatus() != DroneStatus.AVAILABLE) {
                evaluations.add(new CandidateEvaluation(drone, false,
                        "Status is " + drone.getStatus() + " (not AVAILABLE)", excessCapacity, requiredBattery));
                continue;
            }

            if (totalPayloadKg > drone.getMaxCapacityKg()) {
                evaluations.add(new CandidateEvaluation(drone, false,
                        String.format("Payload (%.1f kg) exceeds max capacity (%.1f kg)", totalPayloadKg, drone.getMaxCapacityKg()),
                        excessCapacity, requiredBattery));
                continue;
            }

            if ((effectiveDistanceKm * 2.0) > drone.getMaxFlightRangeKm()) {
                evaluations.add(new CandidateEvaluation(drone, false,
                        String.format("Round trip distance (%.1f km) exceeds max range (%.1f km)", effectiveDistanceKm * 2.0, drone.getMaxFlightRangeKm()),
                        excessCapacity, requiredBattery));
                continue;
            }

            if (drone.getBatteryPercentage() < requiredBattery) {
                evaluations.add(new CandidateEvaluation(drone, false,
                        String.format("Current battery (%d%%) is below required flight buffer (%d%%)", drone.getBatteryPercentage(), requiredBattery),
                        excessCapacity, requiredBattery));
                continue;
            }

            // Drone passed all 4 constraints
            CandidateEvaluation validEval = new CandidateEvaluation(drone, true, "Eligible", excessCapacity, requiredBattery);
            evaluations.add(validEval);
            eligibleCandidates.add(validEval);
        }

        if (eligibleCandidates.isEmpty()) {
            return new AllocationResult(false,
                    "No suitable drone available satisfying capacity, battery, and flight range constraints.",
                    request, null, totalPayloadKg, flightDistanceKm, evaluations);
        }

        // Selection Strategy: Least Excess Capacity (Best Fit)
        // Tie-breaker 1: Higher battery percentage
        // Tie-breaker 2: Alphanumeric Drone ID
        eligibleCandidates.sort((c1, c2) -> {
            int capDiff = Double.compare(c1.getExcessCapacityKg(), c2.getExcessCapacityKg());
            if (capDiff != 0) return capDiff;

            int batDiff = Integer.compare(c2.getDrone().getBatteryPercentage(), c1.getDrone().getBatteryPercentage());
            if (batDiff != 0) return batDiff;

            return c1.getDrone().getId().compareTo(c2.getDrone().getId());
        });

        Drone optimalDrone = eligibleCandidates.get(0).getDrone();

        // Execute Assignment State Transition
        optimalDrone.setStatus(DroneStatus.ASSIGNED);
        system.getDroneManager().update(optimalDrone);

        request.setStatus(RequestStatus.ASSIGNED);
        system.getRequestManager().update(request);

        // Reserve inventory stock
        if (resource != null) {
            system.getResourceManager().allocateStock(resource.getId(), request.getQuantity());
        }

        String successMsg = String.format("Assigned drone %s (%s) with payload %.1f kg (Excess capacity: %.1f kg, Battery: %d%%)",
                optimalDrone.getId(), optimalDrone.getName(), totalPayloadKg,
                eligibleCandidates.get(0).getExcessCapacityKg(), optimalDrone.getBatteryPercentage());

        return new AllocationResult(true, successMsg, request, optimalDrone, totalPayloadKg, flightDistanceKm, evaluations);
    }
}
