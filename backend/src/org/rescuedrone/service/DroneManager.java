package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.Drone;
import org.rescuedrone.model.enums.DroneStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the Drone fleet using ArrayList and HashMap with CSV persistence.
 */
public class DroneManager extends BaseManager<Drone> {

    public DroneManager() {
        super(AppConfig.DRONES_FILE);
    }

    public DroneManager(String customFilePath) {
        super(customFilePath);
    }

    /**
     * Returns all drones that are currently AVAILABLE.
     */
    public synchronized List<Drone> getAvailableDrones() {
        List<Drone> available = new ArrayList<>();
        for (Drone d : list) {
            if (d.getStatus() == DroneStatus.AVAILABLE) {
                available.add(d);
            }
        }
        return available;
    }

    /**
     * Updates the status of a drone and persists the change.
     */
    public synchronized boolean updateDroneStatus(String droneId, DroneStatus newStatus) {
        Drone drone = getById(droneId);
        if (drone != null) {
            drone.setStatus(newStatus);
            update(drone);
            return true;
        }
        return false;
    }

    /**
     * Recharges all drones in the fleet to 100% battery and restores status to AVAILABLE.
     */
    public synchronized int rechargeAll() {
        int count = 0;
        for (Drone d : list) {
            d.recharge();
            d.setStatus(DroneStatus.AVAILABLE);
            update(d);
            count++;
        }
        return count;
    }

    @Override
    protected Drone parseRecord(String[] tokens) {
        return Drone.fromCsvTokens(tokens);
    }
}
