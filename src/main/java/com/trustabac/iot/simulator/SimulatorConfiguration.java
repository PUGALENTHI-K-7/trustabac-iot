package com.trustabac.iot.simulator;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for the IoT Traffic & Scenario Simulator.
 */
@Configuration
@ConfigurationProperties(prefix = "trustabac.simulator")
public class SimulatorConfiguration {

    private boolean enabled = true;
    private String mode = "MANUAL"; // MANUAL or AUTOMATIC
    private Long randomSeed = 42L;
    private long requestIntervalMs = 200;
    private int floodRequestCount = 15;
    private String guestUserId = "guest-user-001";
    private String propertyId = "Property-001";
    private String bookingId = "BOOKING-PHASE6B-001";
    private String organization = "SmartRental";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(Long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public long getRequestIntervalMs() {
        return requestIntervalMs;
    }

    public void setRequestIntervalMs(long requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    public int getFloodRequestCount() {
        return floodRequestCount;
    }

    public void setFloodRequestCount(int floodRequestCount) {
        this.floodRequestCount = floodRequestCount;
    }

    public String getGuestUserId() {
        return guestUserId;
    }

    public void setGuestUserId(String guestUserId) {
        this.guestUserId = guestUserId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}
