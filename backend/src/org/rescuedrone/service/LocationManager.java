package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages Location records (graph vertices) using ArrayList and HashMap with CSV persistence.
 */
public class LocationManager extends BaseManager<Location> {

    public LocationManager() {
        super(AppConfig.LOCATIONS_FILE);
    }

    public LocationManager(String customFilePath) {
        super(customFilePath);
    }

    public synchronized List<Location> getLocationsByDisaster(String disasterId) {
        List<Location> result = new ArrayList<>();
        if (disasterId == null) return result;
        for (Location loc : list) {
            if (disasterId.equalsIgnoreCase(loc.getDisasterId())) {
                result.add(loc);
            }
        }
        return result;
    }

    @Override
    protected Location parseRecord(String[] tokens) {
        return Location.fromCsvTokens(tokens);
    }
}
