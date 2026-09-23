package com.trustabac.iot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for DashboardController rendering and redirects.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /dashboard should return 200 OK and render dashboard view with context attributes")
    void testRenderDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("appName"))
                .andExpect(model().attribute("appName", "TrustABAC-IoT"))
                .andExpect(model().attributeExists("propertyId"))
                .andExpect(model().attribute("propertyId", "Property-001"))
                .andExpect(content().string(containsString("TrustABAC-IoT")))
                .andExpect(content().string(containsString("Smart-Rental Device Fleet")))
                .andExpect(content().string(containsString("DOOR-SENSOR-001")));
    }

    @Test
    @DisplayName("GET / should redirect 302 to /dashboard")
    void testRootRedirect() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
