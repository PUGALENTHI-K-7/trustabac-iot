package com.trustabac.iot.dto;

import com.trustabac.iot.entity.TrustEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO submitted to report a behavioral or security event for an IoT device.
 * Note: Clients cannot submit arbitrary trust scores or authoritative timestamps.
 * The server computes the score delta and establishes the authoritative event timestamp.
 */
public class TrustEventRequest {

    @NotNull(message = "Trust event type is required")
    private TrustEventType eventType;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;

    @Size(max = 100, message = "Source must not exceed 100 characters")
    private String source;

    public TrustEventRequest() {
    }

    public TrustEventRequest(TrustEventType eventType, String reason, String source) {
        this.eventType = eventType;
        this.reason = reason;
        this.source = source;
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
}
