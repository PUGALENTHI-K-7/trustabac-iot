package com.trustabac.iot.dto;

import com.trustabac.iot.entity.TrustEventType;

import java.time.LocalDateTime;

/**
 * Response DTO returned after a trust-modifying behavioral or security event is processed.
 */
public class TrustUpdateResponse {

    private String deviceIdentifier;
    private Double oldTrust;
    private Double newTrust;
    private Double delta;
    private TrustEventType eventType;
    private String reason;
    private String source;
    private LocalDateTime eventTimestamp;
    private String message;

    public TrustUpdateResponse() {
    }

    public TrustUpdateResponse(String deviceIdentifier, Double oldTrust, Double newTrust,
                               Double delta, TrustEventType eventType, String reason,
                               String source, LocalDateTime eventTimestamp, String message) {
        this.deviceIdentifier = deviceIdentifier;
        this.oldTrust = oldTrust;
        this.newTrust = newTrust;
        this.delta = delta;
        this.eventType = eventType;
        this.reason = reason;
        this.source = source;
        this.eventTimestamp = eventTimestamp;
        this.message = message;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public Double getOldTrust() {
        return oldTrust;
    }

    public void setOldTrust(Double oldTrust) {
        this.oldTrust = oldTrust;
    }

    public Double getNewTrust() {
        return newTrust;
    }

    public void setNewTrust(Double newTrust) {
        this.newTrust = newTrust;
    }

    public Double getDelta() {
        return delta;
    }

    public void setDelta(Double delta) {
        this.delta = delta;
    }

    public TrustEventType getEventType() {
        return eventType;
    }

    public void setEventType(TrustEventType eventType) {
        this.eventType = eventType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
