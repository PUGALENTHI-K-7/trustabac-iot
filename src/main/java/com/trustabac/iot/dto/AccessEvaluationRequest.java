package com.trustabac.iot.dto;

import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.ResourceSensitivity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request DTO submitted for evaluating an access attempt across the multi-tier pipeline.
 * Evaluates ABAC eligibility (Gate 1), attaches device Trust (Gate 2), and computes contextual Risk (Gate 3).
 * Note: Clients cannot provide the evaluation result or arbitrary risk scores.
 */
public class AccessEvaluationRequest {

    @NotBlank(message = "Device identifier is required")
    @Size(max = 100, message = "Device identifier must not exceed 100 characters")
    private String deviceIdentifier;

    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;

    @NotBlank(message = "Subject role is required")
    @Size(max = 50, message = "Role must not exceed 50 characters")
    private String role;

    @Size(max = 100, message = "Organization must not exceed 100 characters")
    private String organization;

    @NotBlank(message = "Resource identifier or target is required")
    @Size(max = 100, message = "Resource must not exceed 100 characters")
    private String resource;

    @Size(max = 100, message = "Resource type must not exceed 100 characters")
    private String resourceType;

    private ResourceSensitivity resourceSensitivity;

    @NotNull(message = "Operation is required")
    private Operation operation;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 100, message = "Booking ID must not exceed 100 characters")
    private String bookingId;

    @Size(max = 100, message = "Network context must not exceed 100 characters")
    private String networkContext;

    /**
     * Optional request timestamp for controlled test injection.
     * In normal runtime evaluation, the server establishes the authoritative evaluation timestamp.
     */
    private LocalDateTime requestTimestamp;

    /**
     * Optional contextual simulation / research test parameters.
     */
    private Integer requestCountWindow;
    private Integer recentViolationCount;
    private BehavioralIndicator behavioralIndicator;

    public AccessEvaluationRequest() {
    }

    public AccessEvaluationRequest(String deviceIdentifier, String userId, String role, String organization,
                                   String resource, String resourceType, ResourceSensitivity resourceSensitivity,
                                   Operation operation, String location, String bookingId, String networkContext,
                                   LocalDateTime requestTimestamp) {
        this(deviceIdentifier, userId, role, organization, resource, resourceType, resourceSensitivity,
                operation, location, bookingId, networkContext, requestTimestamp, null, null, null);
    }

    public AccessEvaluationRequest(String deviceIdentifier, String userId, String role, String organization,
                                   String resource, String resourceType, ResourceSensitivity resourceSensitivity,
                                   Operation operation, String location, String bookingId, String networkContext,
                                   LocalDateTime requestTimestamp, Integer requestCountWindow,
                                   Integer recentViolationCount, BehavioralIndicator behavioralIndicator) {
        this.deviceIdentifier = deviceIdentifier;
        this.userId = userId;
        this.role = role;
        this.organization = organization;
        this.resource = resource;
        this.resourceType = resourceType;
        this.resourceSensitivity = resourceSensitivity;
        this.operation = operation;
        this.location = location;
        this.bookingId = bookingId;
        this.networkContext = networkContext;
        this.requestTimestamp = requestTimestamp;
        this.requestCountWindow = requestCountWindow;
        this.recentViolationCount = recentViolationCount;
        this.behavioralIndicator = behavioralIndicator;
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

    public ResourceSensitivity getResourceSensitivity() {
        return resourceSensitivity;
    }

    public void setResourceSensitivity(ResourceSensitivity resourceSensitivity) {
        this.resourceSensitivity = resourceSensitivity;
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

    public Integer getRequestCountWindow() {
        return requestCountWindow;
    }

    public void setRequestCountWindow(Integer requestCountWindow) {
        this.requestCountWindow = requestCountWindow;
    }

    public Integer getRecentViolationCount() {
        return recentViolationCount;
    }

    public void setRecentViolationCount(Integer recentViolationCount) {
        this.recentViolationCount = recentViolationCount;
    }

    public BehavioralIndicator getBehavioralIndicator() {
        return behavioralIndicator;
    }

    public void setBehavioralIndicator(BehavioralIndicator behavioralIndicator) {
        this.behavioralIndicator = behavioralIndicator;
    }
}
