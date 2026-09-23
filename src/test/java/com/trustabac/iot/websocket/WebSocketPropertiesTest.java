package com.trustabac.iot.websocket;

import com.trustabac.iot.config.WebSocketProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketPropertiesTest {

    @Test
    @DisplayName("Verify default configuration values for WebSocketProperties")
    void testDefaultValues() {
        WebSocketProperties props = new WebSocketProperties();
        assertTrue(props.isEnabled());
        assertEquals("/ws", props.getEndpoint());
        assertEquals("/topic", props.getTopicPrefix());
        assertEquals("/app", props.getAppPrefix());
        assertEquals("*", props.getAllowedOrigins());
    }

    @Test
    @DisplayName("Verify custom property setters and getters")
    void testCustomValues() {
        WebSocketProperties props = new WebSocketProperties();
        props.setEnabled(false);
        props.setEndpoint("/custom-ws");
        props.setTopicPrefix("/custom-topic");
        props.setAppPrefix("/custom-app");
        props.setAllowedOrigins("https://dashboard.trustabac.internal");

        assertFalse(props.isEnabled());
        assertEquals("/custom-ws", props.getEndpoint());
        assertEquals("/custom-topic", props.getTopicPrefix());
        assertEquals("/custom-app", props.getAppPrefix());
        assertEquals("https://dashboard.trustabac.internal", props.getAllowedOrigins());
    }
}
