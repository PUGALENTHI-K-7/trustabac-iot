package com.trustabac.iot.dto;

import com.trustabac.iot.entity.TrustEventType;

import java.time.LocalDateTime;

/**
 * Response DTO representing an immutable audit log entry in the device's trust score history.
 */
public class TrustHistoryResponse {

    private Long id;
    private String deviceIdentifier;
    private Double oldTrust;
    private Double newTrust;
    private Double delta;
    private TrustEventType eventType;
    private String reason;
    private String source;
    private LocalDateTime eventTimestamp;
    private LocalDateTime createdAt;

    public TrustHistoryResponse() {
    }

    public TrustHistoryResponse(Long id, String deviceIdentifier, Double oldTrust, Double newTrust,
                                Double delta, TrustEventType eventType, String reason,
                                String source, LocalDateTime eventTimestamp, LocalDateTime createdAt) {
        this.id = id;
        this.deviceIdentifier = deviceIdentifier;
        this.oldTrust = oldTrust;
        this.newTrust = newTrust;
        this.delta = delta;
        this.eventType = eventType;
        this.reason = reason;
        this.source = source;
        this.eventTimestamp = eventTimestamp;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
