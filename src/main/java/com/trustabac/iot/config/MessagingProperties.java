package com.trustabac.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration properties for Spring AMQP / RabbitMQ event-driven pipeline.
 */
@Configuration
@ConfigurationProperties(prefix = "trustabac.messaging")
public class MessagingProperties {

    private boolean enabled = true;
    private String exchange = "trustabac.events";

    // Queues
    private String deviceEventQueue = "trustabac.device.events";
    private String trustEventQueue = "trustabac.trust.events";
    private String riskEventQueue = "trustabac.risk.events";
    private String authorizationEventQueue = "trustabac.authorization.events";
    private String dlqQueue = "trustabac.dlq";

    // Routing Keys
    private String routingKeyDeviceOperation = "device.operation";
    private String routingKeyTrustEvent = "trust.event";
    private String routingKeyRiskEvent = "risk.context";
    private String routingKeyAuthorizationResult = "authorization.result";
    private String routingKeyDlq = "dlq.events";

    // Consumer / Reliability Settings
    private int concurrentConsumers = 2;
    private int maxConcurrentConsumers = 5;
    private int maxRetryAttempts = 3;
    private long retryInitialIntervalMs = 1000L;
    private double retryMultiplier = 2.0;
    private long retryMaxIntervalMs = 5000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getDeviceEventQueue() {
        return deviceEventQueue;
    }

    public void setDeviceEventQueue(String deviceEventQueue) {
        this.deviceEventQueue = deviceEventQueue;
    }

    public String getTrustEventQueue() {
        return trustEventQueue;
    }

    public void setTrustEventQueue(String trustEventQueue) {
        this.trustEventQueue = trustEventQueue;
    }

    public String getRiskEventQueue() {
        return riskEventQueue;
    }

    public void setRiskEventQueue(String riskEventQueue) {
        this.riskEventQueue = riskEventQueue;
    }

    public String getAuthorizationEventQueue() {
        return authorizationEventQueue;
    }

    public void setAuthorizationEventQueue(String authorizationEventQueue) {
        this.authorizationEventQueue = authorizationEventQueue;
    }

    public String getDlqQueue() {
        return dlqQueue;
    }

    public void setDlqQueue(String dlqQueue) {
        this.dlqQueue = dlqQueue;
    }

    public String getRoutingKeyDeviceOperation() {
        return routingKeyDeviceOperation;
    }

    public void setRoutingKeyDeviceOperation(String routingKeyDeviceOperation) {
        this.routingKeyDeviceOperation = routingKeyDeviceOperation;
    }

    public String getRoutingKeyTrustEvent() {
        return routingKeyTrustEvent;
    }

    public void setRoutingKeyTrustEvent(String routingKeyTrustEvent) {
        this.routingKeyTrustEvent = routingKeyTrustEvent;
    }

    public String getRoutingKeyRiskEvent() {
        return routingKeyRiskEvent;
    }

    public void setRoutingKeyRiskEvent(String routingKeyRiskEvent) {
        this.routingKeyRiskEvent = routingKeyRiskEvent;
    }

    public String getRoutingKeyAuthorizationResult() {
        return routingKeyAuthorizationResult;
    }

    public void setRoutingKeyAuthorizationResult(String routingKeyAuthorizationResult) {
        this.routingKeyAuthorizationResult = routingKeyAuthorizationResult;
    }

    public String getRoutingKeyDlq() {
        return routingKeyDlq;
    }

    public void setRoutingKeyDlq(String routingKeyDlq) {
        this.routingKeyDlq = routingKeyDlq;
    }

    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    public int getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }

    public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public long getRetryInitialIntervalMs() {
        return retryInitialIntervalMs;
    }

    public void setRetryInitialIntervalMs(long retryInitialIntervalMs) {
        this.retryInitialIntervalMs = retryInitialIntervalMs;
    }

    public double getRetryMultiplier() {
        return retryMultiplier;
    }

    public void setRetryMultiplier(double retryMultiplier) {
        this.retryMultiplier = retryMultiplier;
    }

    public long getRetryMaxIntervalMs() {
        return retryMaxIntervalMs;
    }

    public void setRetryMaxIntervalMs(long retryMaxIntervalMs) {
        this.retryMaxIntervalMs = retryMaxIntervalMs;
    }
}
