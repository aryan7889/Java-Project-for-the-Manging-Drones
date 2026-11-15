package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.Resource;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages relief resource inventory using ArrayList and HashMap with CSV persistence.
 */
public class ResourceManager extends BaseManager<Resource> {

    public static final int LOW_STOCK_THRESHOLD = 20;

    public ResourceManager() {
        super(AppConfig.RESOURCES_FILE);
    }

    public ResourceManager(String customFilePath) {
        super(customFilePath);
    }

    /**
     * Reserves requested stock for an active relief mission and saves.
     */
    public synchronized boolean allocateStock(String resourceId, int quantity) {
        Resource res = getById(resourceId);
        if (res != null && res.allocate(quantity)) {
            saveToFile();
            return true;
        }
        return false;
    }

    /**
     * Releases reserved stock back into available pool and saves.
     */
    public synchronized void releaseStock(String resourceId, int quantity) {
        Resource res = getById(resourceId);
        if (res != null) {
            res.release(quantity);
            saveToFile();
        }
    }

    /**
     * Permanently consumes reserved stock upon delivery completion and saves.
     */
    public synchronized void consumeStock(String resourceId, int quantity) {
        Resource res = getById(resourceId);
        if (res != null) {
            res.consume(quantity);
            saveToFile();
        }
    }

    /**
     * Returns items whose available stock is below the low-stock threshold.
     */
    public synchronized List<Resource> getLowStockResources() {
        List<Resource> low = new ArrayList<>();
        for (Resource r : list) {
            if (r.getAvailableStock() <= LOW_STOCK_THRESHOLD) {
                low.add(r);
            }
        }
        return low;
    }

    @Override
    protected Resource parseRecord(String[] tokens) {
        return Resource.fromCsvTokens(tokens);
    }
}
