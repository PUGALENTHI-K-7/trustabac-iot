package com.trustabac.iot.service.risk;

import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.ResourceSensitivity;

import java.time.LocalDateTime;

/**
 * Server-resolved context encapsulating all authoritative inputs for risk calculation.
 * Distinguishes external request parameters from server-established risk indicators.
 */
public record RiskContext(
        String deviceIdentifier,
        String userId,
        String resource,
        ResourceSensitivity resourceSensitivity,
        Operation operation,
        String location,
        String networkContext,
        LocalDateTime evaluationTimestamp,
        int requestCountWindow,
        int recentViolationCount,
        BehavioralIndicator behavioralIndicator
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String deviceIdentifier;
        private String userId;
        private String resource;
        private ResourceSensitivity resourceSensitivity = ResourceSensitivity.MEDIUM;
        private Operation operation;
        private String location;
        private String networkContext;
        private LocalDateTime evaluationTimestamp;
        private int requestCountWindow = 1;
        private int recentViolationCount = 0;
        private BehavioralIndicator behavioralIndicator = BehavioralIndicator.NORMAL;

        public Builder deviceIdentifier(String deviceIdentifier) {
            this.deviceIdentifier = deviceIdentifier;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder resourceSensitivity(ResourceSensitivity resourceSensitivity) {
            this.resourceSensitivity = resourceSensitivity;
            return this;
        }

        public Builder operation(Operation operation) {
            this.operation = operation;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder networkContext(String networkContext) {
            this.networkContext = networkContext;
            return this;
        }

        public Builder evaluationTimestamp(LocalDateTime evaluationTimestamp) {
            this.evaluationTimestamp = evaluationTimestamp;
            return this;
        }

        public Builder requestCountWindow(int requestCountWindow) {
            this.requestCountWindow = requestCountWindow;
            return this;
        }

        public Builder recentViolationCount(int recentViolationCount) {
            this.recentViolationCount = recentViolationCount;
            return this;
        }

        public Builder behavioralIndicator(BehavioralIndicator behavioralIndicator) {
            this.behavioralIndicator = behavioralIndicator;
            return this;
        }

        public RiskContext build() {
            return new RiskContext(
                    deviceIdentifier,
                    userId,
                    resource,
                    resourceSensitivity,
                    operation,
                    location,
                    networkContext,
                    evaluationTimestamp,
                    requestCountWindow,
                    recentViolationCount,
                    behavioralIndicator
            );
        }
    }
}
