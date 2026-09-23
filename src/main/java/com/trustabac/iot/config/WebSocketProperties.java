package com.trustabac.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for WebSocket / STOMP real-time telemetry streaming.
 */
@Configuration
@ConfigurationProperties(prefix = "trustabac.websocket")
public class WebSocketProperties {

    private boolean enabled = true;
    private String endpoint = "/ws";
    private String allowedOrigins = "*";
    private String topicPrefix = "/topic";
    private String appPrefix = "/app";
    private long heartbeatServerInterval = 10000;
    private long heartbeatClientInterval = 10000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public String getAppPrefix() {
        return appPrefix;
    }

    public void setAppPrefix(String appPrefix) {
        this.appPrefix = appPrefix;
    }

    public long getHeartbeatServerInterval() {
        return heartbeatServerInterval;
    }

    public void setHeartbeatServerInterval(long heartbeatServerInterval) {
        this.heartbeatServerInterval = heartbeatServerInterval;
    }

    public long getHeartbeatClientInterval() {
        return heartbeatClientInterval;
    }

    public void setHeartbeatClientInterval(long heartbeatClientInterval) {
        this.heartbeatClientInterval = heartbeatClientInterval;
    }
}
