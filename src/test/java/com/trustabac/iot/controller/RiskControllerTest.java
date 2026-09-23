package com.trustabac.iot.controller;

import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.RiskEventRepository;
import com.trustabac.iot.repository.TrustHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RiskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private TrustHistoryRepository trustHistoryRepository;

    @Autowired
    private AccessRequestRepository accessRequestRepository;

    @BeforeEach
    void setUp() {
        riskEventRepository.deleteAll();
        trustHistoryRepository.deleteAll();
        accessRequestRepository.deleteAll();
        deviceRepository.deleteAll();

        Device device = new Device("DEV-TEST-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED, 80.0, true);
        deviceRepository.save(device);
    }

    @Test
    @DisplayName("POST /api/risk/evaluate successfully calculates contextual risk and persists RiskEvent")
    void testEvaluateRiskSuccess() throws Exception {
        String payload = """
                {
                    "deviceIdentifier": "DEV-TEST-001",
                    "userId": "Guest-001",
                    "resource": "SMART_DOOR_LOCK",
                    "operation": "CONTROL",
                    "location": "Property-001",
                    "networkContext": "LOCAL_WIFI"
                }
                """;

        mockMvc.perform(post("/api/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-TEST-001"))
                .andExpect(jsonPath("$.riskScore", notNullValue()))
                .andExpect(jsonPath("$.riskStatus").value("LOW"))
                .andExpect(jsonPath("$.factors.sensitivityRisk").value(0.70))
                .andExpect(jsonPath("$.factors.locationRisk").value(0.0))
                .andExpect(jsonPath("$.factors.networkRisk").value(0.0))
                .andExpect(jsonPath("$.evaluationTimestamp", notNullValue()));

        assertEquals(1, riskEventRepository.count());
    }

    @Test
    @DisplayName("POST /api/risk/evaluate with invalid payload returns 400 Bad Request")
    void testEvaluateRiskInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                    "deviceIdentifier": "",
                    "userId": null
                }
                """;

        mockMvc.perform(post("/api/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/risk/{deviceIdentifier} returns current risk assessment")
    void testGetCurrentRisk() throws Exception {
        String payload = """
                {
                    "deviceIdentifier": "DEV-TEST-001",
                    "userId": "Guest-001",
                    "resource": "SMART_LIGHT",
                    "operation": "READ",
                    "location": "Property-001",
                    "networkContext": "LOCAL_WIFI"
                }
                """;

        mockMvc.perform(post("/api/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/risk/DEV-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-TEST-001"))
                .andExpect(jsonPath("$.riskScore", notNullValue()))
                .andExpect(jsonPath("$.riskStatus").value("LOW"));
    }

    @Test
    @DisplayName("GET /api/risk/{deviceIdentifier} with unknown device returns 404 Not Found")
    void testGetCurrentRiskUnknownDevice() throws Exception {
        mockMvc.perform(get("/api/risk/DEV-UNKNOWN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/risk/{deviceIdentifier}/history returns risk audit records")
    void testGetRiskHistory() throws Exception {
        String event1 = """
                {
                    "deviceIdentifier": "DEV-TEST-001",
                    "userId": "Guest-001",
                    "resource": "SMART_LIGHT",
                    "operation": "READ",
                    "location": "Property-001",
                    "networkContext": "LOCAL_WIFI"
                }
                """;
        String event2 = """
                {
                    "deviceIdentifier": "DEV-TEST-001",
                    "userId": "Guest-001",
                    "resource": "ROUTER",
                    "operation": "WRITE",
                    "location": "Remote-Zone",
                    "networkContext": "UNKNOWN"
                }
                """;

        mockMvc.perform(post("/api/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/risk/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/risk/DEV-TEST-001/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].resource").value("ROUTER"))
                .andExpect(jsonPath("$[1].resource").value("SMART_LIGHT"));
    }
}
