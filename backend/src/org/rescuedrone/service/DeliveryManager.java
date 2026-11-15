package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.Delivery;
import org.rescuedrone.model.enums.DeliveryStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages active and historical delivery missions with CSV persistence.
 */
public class DeliveryManager extends BaseManager<Delivery> {

    public DeliveryManager() {
        super(AppConfig.DELIVERIES_FILE);
    }

    public DeliveryManager(String customFilePath) {
        super(customFilePath);
    }

    public synchronized List<Delivery> getActiveDeliveries() {
        List<Delivery> active = new ArrayList<>();
        for (Delivery d : list) {
            if (d.getStatus() == DeliveryStatus.PENDING || d.getStatus() == DeliveryStatus.IN_TRANSIT) {
                active.add(d);
            }
        }
        return active;
    }

    public synchronized List<Delivery> getCompletedDeliveries() {
        List<Delivery> completed = new ArrayList<>();
        for (Delivery d : list) {
            if (d.getStatus() == DeliveryStatus.COMPLETED) {
                completed.add(d);
            }
        }
        return completed;
    }

    public synchronized List<Delivery> getDeliveriesByDrone(String droneId) {
        List<Delivery> result = new ArrayList<>();
        if (droneId == null) return result;
        for (Delivery d : list) {
            if (droneId.equalsIgnoreCase(d.getDroneId())) {
                result.add(d);
            }
        }
        return result;
    }

    @Override
    protected Delivery parseRecord(String[] tokens) {
        return Delivery.fromCsvTokens(tokens);
    }
}
