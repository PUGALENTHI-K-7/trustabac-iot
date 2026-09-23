package com.trustabac.iot.dto;

import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for contextual risk evaluation.
 * Note: Clients cannot submit arbitrary riskScore or resourceSensitivity values.
 * Sensitivity is authoritatively resolved server-side, and timestamps are established by server Clock.
 */
public class RiskEvaluationRequest {

    @NotBlank(message = "Device identifier is required")
    @Size(min = 3, max = 100, message = "Device identifier must be between 3 and 100 characters")
    private String deviceIdentifier;

    @NotBlank(message = "User ID is required")
    @Size(max = 100, message = "User ID must not exceed 100 characters")
    private String userId;

    @NotBlank(message = "Resource identifier is required")
    @Size(max = 100, message = "Resource must not exceed 100 characters")
    private String resource;

    @NotNull(message = "Operation is required")
    private Operation operation;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 50, message = "Network context must not exceed 50 characters")
    private String networkContext;

    /**
     * Optional contextual simulation / research test parameters.
     * In live production, these are derived from internal gateway velocity and IDS telemetry.
     */
    private Integer requestCountWindow;
    private Integer recentViolationCount;
    private BehavioralIndicator behavioralIndicator;

    public RiskEvaluationRequest() {
    }

    public RiskEvaluationRequest(String deviceIdentifier, String userId, String resource, Operation operation,
                                 String location, String networkContext) {
        this.deviceIdentifier = deviceIdentifier;
        this.userId = userId;
        this.resource = resource;
        this.operation = operation;
        this.location = location;
        this.networkContext = networkContext;
    }

    public RiskEvaluationRequest(String deviceIdentifier, String userId, String resource, Operation operation,
                                 String location, String networkContext, Integer requestCountWindow,
                                 Integer recentViolationCount, BehavioralIndicator behavioralIndicator) {
        this.deviceIdentifier = deviceIdentifier;
        this.userId = userId;
        this.resource = resource;
        this.operation = operation;
        this.location = location;
        this.networkContext = networkContext;
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

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
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

    public String getNetworkContext() {
        return networkContext;
    }

    public void setNetworkContext(String networkContext) {
        this.networkContext = networkContext;
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
