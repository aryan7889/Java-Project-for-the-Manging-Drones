package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.enums.UrgencyLevel;

/**
 * Service calculating delivery fee breakdowns according to operational rules.
 * Demonstrates Abstraction and Encapsulation.
 */
public class CostCalculator {

    public static class CostBreakdown {
        private final double baseCharge;
        private final double distanceKm;
        private final double distanceCharge;
        private final double emergencyCharge;
        private final double totalCost;

        public CostBreakdown(double baseCharge, double distanceKm, double distanceCharge,
                             double emergencyCharge, double totalCost) {
            this.baseCharge = baseCharge;
            this.distanceKm = distanceKm;
            this.distanceCharge = distanceCharge;
            this.emergencyCharge = emergencyCharge;
            this.totalCost = totalCost;
        }

        public double getBaseCharge() {
            return baseCharge;
        }

        public double getDistanceKm() {
            return distanceKm;
        }

        public double getDistanceCharge() {
            return distanceCharge;
        }

        public double getEmergencyCharge() {
            return emergencyCharge;
        }

        public double getTotalCost() {
            return totalCost;
        }

        @Override
        public String toString() {
            return String.format("Total: ₹%.2f (Base: ₹%.2f, Dist: %.1fkm @ ₹%.2f/km, Emergency: ₹%.2f)",
                    totalCost, baseCharge, distanceKm, distanceCharge, emergencyCharge);
        }
    }

    /**
     * Calculates delivery cost based on distance and request urgency.
     * Formula: Base (₹100) + (Distance * ₹20/km) + Emergency (₹50 if Critical/High).
     */
    public static CostBreakdown calculateCost(double distanceKm, UrgencyLevel urgency) {
        double base = AppConfig.BASE_DELIVERY_CHARGE;
        double distRate = AppConfig.DISTANCE_CHARGE_PER_KM;
        double distCharge = Math.max(0.0, distanceKm) * distRate;

        double emergencyCharge = 0.0;
        if (urgency == UrgencyLevel.CRITICAL || urgency == UrgencyLevel.HIGH) {
            emergencyCharge = AppConfig.EMERGENCY_SURCHARGE;
        }

        double total = base + distCharge + emergencyCharge;
        return new CostBreakdown(base, distanceKm, distCharge, emergencyCharge, total);
    }
}
