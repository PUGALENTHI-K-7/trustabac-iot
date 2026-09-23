package com.trustabac.iot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity representing an append-only, immutable audit record of a trust score update event.
 */
@Entity
@Table(name = "trust_history")
public class TrustHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "device_identifier", nullable = false, length = 100)
    private String deviceIdentifier;

    @Column(name = "old_trust", nullable = false)
    private Double oldTrust;

    @Column(name = "new_trust", nullable = false)
    private Double newTrust;

    @Column(name = "delta", nullable = false)
    private Double delta;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private TrustEventType eventType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public TrustHistory() {
    }

    public TrustHistory(Device device, String deviceIdentifier, Double oldTrust, Double newTrust,
                        Double delta, TrustEventType eventType, String reason, String source,
                        LocalDateTime eventTimestamp) {
        this.device = device;
        this.deviceIdentifier = deviceIdentifier;
        this.oldTrust = oldTrust;
        this.newTrust = newTrust;
        this.delta = delta;
        this.eventType = eventType;
        this.reason = reason;
        this.source = source;
        this.eventTimestamp = eventTimestamp;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.eventTimestamp == null) {
            this.eventTimestamp = this.createdAt;
        }
    }

    // Getters

    public Long getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public Double getOldTrust() {
        return oldTrust;
    }

    public Double getNewTrust() {
        return newTrust;
    }

    public Double getDelta() {
        return delta;
    }

    public TrustEventType getEventType() {
        return eventType;
    }

    public String getReason() {
        return reason;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters

    public void setId(Long id) {
        this.id = id;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public void setOldTrust(Double oldTrust) {
        this.oldTrust = oldTrust;
    }

    public void setNewTrust(Double newTrust) {
        this.newTrust = newTrust;
    }

    public void setDelta(Double delta) {
        this.delta = delta;
    }

    public void setEventType(TrustEventType eventType) {
        this.eventType = eventType;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
