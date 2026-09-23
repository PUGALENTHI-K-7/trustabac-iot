package com.trustabac.iot.dto;

/**
 * Response DTO returned when querying the current trust score of an IoT device.
 */
public class TrustScoreResponse {

    private String deviceIdentifier;
    private Double currentTrust;
    private String trustStatus;

    public TrustScoreResponse() {
    }

    public TrustScoreResponse(String deviceIdentifier, Double currentTrust, String trustStatus) {
        this.deviceIdentifier = deviceIdentifier;
        this.currentTrust = currentTrust;
        this.trustStatus = trustStatus;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public Double getCurrentTrust() {
        return currentTrust;
    }

    public void setCurrentTrust(Double currentTrust) {
        this.currentTrust = currentTrust;
    }

    public String getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(String trustStatus) {
        this.trustStatus = trustStatus;
    }
}
