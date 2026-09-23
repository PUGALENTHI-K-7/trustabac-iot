package com.trustabac.iot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WebSocketStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/websocket/status should return 200 OK with telemetry telemetry status")
    void testGetWebSocketStatus() throws Exception {
        mockMvc.perform(get("/api/websocket/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.endpoint").value("/ws"))
                .andExpect(jsonPath("$.topicPrefix").value("/topic"))
                .andExpect(jsonPath("$.supportedTopics").isArray());
    }
}
