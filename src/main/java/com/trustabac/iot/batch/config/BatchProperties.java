package com.trustabac.iot.batch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Spring Batch offline analytics.
 */
@Component
@ConfigurationProperties(prefix = "trustabac.batch")
public class BatchProperties {

    private boolean enabled = true;
    private int chunkSize = 100;
    private int defaultPeriodDays = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getDefaultPeriodDays() {
        return defaultPeriodDays;
    }

    public void setDefaultPeriodDays(int defaultPeriodDays) {
        this.defaultPeriodDays = defaultPeriodDays;
    }
}
