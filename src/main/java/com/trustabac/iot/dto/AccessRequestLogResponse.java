package com.trustabac.iot.dto;

import com.trustabac.iot.entity.AbacResult;
import com.trustabac.iot.entity.Operation;

import java.time.LocalDateTime;

/**
 * Response DTO representing an audit log entry of an evaluated access request.
 */
public class AccessRequestLogResponse {

    private Long id;
    private String deviceIdentifier;
    private String userId;
    private String role;
    private String organization;
    private String resource;
    private String resourceType;
    private Operation operation;
    private String location;
    private String bookingId;
    private String networkContext;
    private LocalDateTime requestTimestamp;
    private AbacResult abacResult;
    private String abacReason;
    private String evaluatedPolicyName;
    private LocalDateTime createdAt;

    public AccessRequestLogResponse() {
    }

    public AccessRequestLogResponse(Long id, String deviceIdentifier, String userId, String role,
                                   String organization, String resource, String resourceType,
                                   Operation operation, String location, String bookingId,
                                   String networkContext, LocalDateTime requestTimestamp,
                                   AbacResult abacResult, String abacReason,
                                   String evaluatedPolicyName, LocalDateTime createdAt) {
        this.id = id;
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
