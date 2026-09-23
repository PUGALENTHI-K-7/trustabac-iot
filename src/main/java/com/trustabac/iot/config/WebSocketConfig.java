package com.trustabac.iot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Spring WebSocket / STOMP configuration for real-time telemetry streaming.
 * Configures /ws endpoint with simple in-memory broker on /topic destinations.
 */
@Configuration
@EnableWebSocketMessageBroker
@ConditionalOnProperty(prefix = "trustabac.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketProperties properties;

    public WebSocketConfig(WebSocketProperties properties) {
        this.properties = properties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable in-memory broadcast broker for /topic/* subscriptions
        registry.enableSimpleBroker(properties.getTopicPrefix());
        // Application prefix for inbound messages (observational/read-only in Phase 7D)
        registry.setApplicationDestinationPrefixes(properties.getAppPrefix());
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Expose raw STOMP WebSocket endpoint with configured CORS origin patterns
        registry.addEndpoint(properties.getEndpoint())
                .setAllowedOriginPatterns(properties.getAllowedOrigins());
    }
}
