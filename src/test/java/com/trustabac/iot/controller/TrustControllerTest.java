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
class TrustControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TrustHistoryRepository trustHistoryRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

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
    @DisplayName("GET /api/trust/{deviceIdentifier} returns current trust score and status")
    void testGetCurrentTrust() throws Exception {
        mockMvc.perform(get("/api/trust/DEV-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-TEST-001"))
                .andExpect(jsonPath("$.currentTrust").value(80.0))
                .andExpect(jsonPath("$.trustStatus").value("TRUSTED"));
    }

    @Test
    @DisplayName("GET /api/trust/{deviceIdentifier} with unknown device returns 404 Not Found")
    void testGetCurrentTrustUnknownDevice() throws Exception {
        mockMvc.perform(get("/api/trust/DEV-UNKNOWN-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/trust/{deviceIdentifier}/events processes event, updates score, and returns server timestamp")
    void testRecordTrustEventSuccess() throws Exception {
        String payload = """
                {
                    "eventType": "SUSPICIOUS_ACTIVITY",
                    "reason": "Anomalous connection burst",
                    "source": "SECURITY_MONITOR"
                }
                """;

        mockMvc.perform(post("/api/trust/DEV-TEST-001/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-TEST-001"))
                .andExpect(jsonPath("$.oldTrust").value(80.0))
                .andExpect(jsonPath("$.newTrust").value(70.0))
                .andExpect(jsonPath("$.delta").value(-10.0))
                .andExpect(jsonPath("$.eventType").value("SUSPICIOUS_ACTIVITY"))
                .andExpect(jsonPath("$.reason").value("Anomalous connection burst"))
                .andExpect(jsonPath("$.source").value("SECURITY_MONITOR"))
                .andExpect(jsonPath("$.eventTimestamp", notNullValue()));

        assertEquals(1, trustHistoryRepository.count());
    }

    @Test
    @DisplayName("POST /api/trust/{deviceIdentifier}/events with client-supplied timestamp is ignored/overridden by server clock")
    void testRecordTrustEventIgnoresClientSuppliedTimestamp() throws Exception {
        String payload = """
                {
                    "eventType": "NORMAL_SUCCESS",
                    "reason": "Client heartbeat",
                    "source": "GATEWAY",
                    "eventTimestamp": "1999-01-01T00:00:00"
                }
                """;

        mockMvc.perform(post("/api/trust/DEV-TEST-001/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("NORMAL_SUCCESS"))
                .andExpect(jsonPath("$.eventTimestamp", notNullValue()));

        var historyList = trustHistoryRepository.findAll();
        assertEquals(1, historyList.size());
        assertEquals(true, historyList.getFirst().getEventTimestamp().getYear() >= 2026);
    }

    @Test
    @DisplayName("POST /api/trust/{deviceIdentifier}/events with invalid body returns 400 Bad Request")
    void testRecordTrustEventInvalidBody() throws Exception {
        String invalidPayload = """
                {
                    "eventType": null
                }
                """;

        mockMvc.perform(post("/api/trust/DEV-TEST-001/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/trust/{deviceIdentifier}/history returns trust audit records in order")
    void testGetTrustHistory() throws Exception {
        String event1 = """
                {
                    "eventType": "NORMAL_SUCCESS",
                    "reason": "Event 1"
                }
                """;
        String event2 = """
                {
                    "eventType": "SUSPICIOUS_ACTIVITY",
                    "reason": "Event 2"
                }
                """;

        mockMvc.perform(post("/api/trust/DEV-TEST-001/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event1))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/trust/DEV-TEST-001/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(event2))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/trust/DEV-TEST-001/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].eventType").value("SUSPICIOUS_ACTIVITY"))
                .andExpect(jsonPath("$[1].eventType").value("NORMAL_SUCCESS"));
    }
}
