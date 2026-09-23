package com.trustabac.iot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity representing the persistent audit log of an evaluated access request.
 */
@Entity
@Table(name = "access_requests")
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_identifier", nullable = false, length = 100)
    private String deviceIdentifier;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "organization", length = 100)
    private String organization;

    @Column(name = "resource", nullable = false, length = 100)
    private String resource;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 30)
    private Operation operation;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "booking_id", length = 100)
    private String bookingId;

    @Column(name = "network_context", length = 100)
    private String networkContext;

    @Column(name = "request_timestamp", nullable = false)
    private LocalDateTime requestTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "abac_result", nullable = false, length = 20)
    private AbacResult abacResult;

    @Column(name = "abac_reason", nullable = false, length = 500)
    private String abacReason;

    @Column(name = "evaluated_policy_name", length = 150)
    private String evaluatedPolicyName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AccessRequest() {
    }

    public AccessRequest(String deviceIdentifier, String userId, String role, String organization,
                         String resource, String resourceType, Operation operation, String location,
                         String bookingId, String networkContext, LocalDateTime requestTimestamp,
                         AbacResult abacResult, String abacReason, String evaluatedPolicyName) {
        this.deviceIdentifier = deviceIdentifier;
        this.userId = userId;
        this.role = role;
        this.organization = organization;
        this.resource = resource;
        this.resourceType = resourceType;
        this.operation = operation;
        this.location = location;
        this.bookingId = bookingId;
        this.networkContext = networkContext;
        this.requestTimestamp = requestTimestamp;
        this.abacResult = abacResult;
        this.abacReason = abacReason;
        this.evaluatedPolicyName = evaluatedPolicyName;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getNetworkContext() {
        return networkContext;
    }

    public void setNetworkContext(String networkContext) {
        this.networkContext = networkContext;
    }

    public LocalDateTime getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(LocalDateTime requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public AbacResult getAbacResult() {
        return abacResult;
    }

    public void setAbacResult(AbacResult abacResult) {
        this.abacResult = abacResult;
    }

    public String getAbacReason() {
        return abacReason;
    }

    public void setAbacReason(String abacReason) {
        this.abacReason = abacReason;
    }

    public String getEvaluatedPolicyName() {
        return evaluatedPolicyName;
    }

    public void setEvaluatedPolicyName(String evaluatedPolicyName) {
        this.evaluatedPolicyName = evaluatedPolicyName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
