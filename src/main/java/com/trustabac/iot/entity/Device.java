package com.trustabac.iot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity representing an IoT device managed by the TrustABAC-IoT gateway.
 */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_identifier", nullable = false, unique = true, length = 100)
    private String deviceIdentifier;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 50)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_class", nullable = false, length = 50)
    private DeviceClass deviceClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 30)
    private RegistrationStatus registrationStatus;

    @Column(name = "current_trust", nullable = false)
    private Double currentTrust;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @jakarta.persistence.Version
    @Column(name = "version")
    private Long version;

    public Device() {
    }

    public Device(String deviceIdentifier, DeviceType deviceType, DeviceClass deviceClass,
                  RegistrationStatus registrationStatus, Double currentTrust, Boolean active) {
        this.deviceIdentifier = deviceIdentifier;
        this.deviceType = deviceType;
        this.deviceClass = deviceClass;
        this.registrationStatus = registrationStatus;
        this.currentTrust = currentTrust;
        this.active = active;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.currentTrust == null) {
            this.currentTrust = 80.0;
        }
        if (this.registrationStatus == null) {
            this.registrationStatus = RegistrationStatus.REGISTERED;
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

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

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    public DeviceClass getDeviceClass() {
        return deviceClass;
    }

    public void setDeviceClass(DeviceClass deviceClass) {
        this.deviceClass = deviceClass;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public Double getCurrentTrust() {
        return currentTrust;
    }

    public void setCurrentTrust(Double currentTrust) {
        this.currentTrust = currentTrust;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
