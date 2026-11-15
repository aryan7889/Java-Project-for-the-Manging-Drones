package org.rescuedrone.service;

import org.rescuedrone.config.AppConfig;
import org.rescuedrone.model.Disaster;

/**
 * Manages Disaster records using ArrayList and HashMap with CSV persistence.
 */
public class DisasterManager extends BaseManager<Disaster> {

    public DisasterManager() {
        super(AppConfig.DISASTERS_FILE);
    }

    public DisasterManager(String customFilePath) {
        super(customFilePath);
    }

    @Override
    protected Disaster parseRecord(String[] tokens) {
        return Disaster.fromCsvTokens(tokens);
    }
}
