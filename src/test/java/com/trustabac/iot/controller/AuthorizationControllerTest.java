package com.trustabac.iot.controller;

import com.trustabac.iot.dto.BlockchainAuthorizationResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.service.DecisionCoordinator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DecisionCoordinator decisionCoordinator;

    @Test
    @DisplayName("POST /api/authorization/evaluate should return 200 with complete authorization decision and blockchain metadata")
    void shouldEvaluateAuthorizationSuccessfully() throws Exception {
        BlockchainAuthorizationResponse mockResponse = new BlockchainAuthorizationResponse(
                1L,
                "DEV-DOOR-001",
                "Guest-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "PASS",
                "All ABAC conditions satisfied",
                "Guest Smart Door Lock Access",
                85.0,
                "TRUSTED",
                15.0,
                "LOW",
                null,
                "Normal context",
                Decision.ALLOW,
                "Allowed by Smart Contract",
                "0x123abc456def",
                42L,
                "0xContractAddr",
                LocalDateTime.now()
        );

        when(decisionCoordinator.evaluateAuthorization(any())).thenReturn(mockResponse);

        String payload = """
                {
                    "deviceIdentifier": "DEV-DOOR-001",
                    "userId": "Guest-001",
                    "role": "GUEST",
                    "organization": "SmartRental",
                    "resource": "SMART_DOOR_LOCK",
                    "operation": "CONTROL",
                    "location": "Property-001",
                    "bookingId": "BOOK-001",
                    "networkContext": "LOCAL_WIFI",
                    "requestReference": "REQ-001"
                }
                """;

        mockMvc.perform(post("/api/authorization/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.abacResult").value("PASS"))
                .andExpect(jsonPath("$.finalDecision").value("ALLOW"))
                .andExpect(jsonPath("$.trustScore").value(85.0))
                .andExpect(jsonPath("$.riskScore").value(15.0))
                .andExpect(jsonPath("$.blockchainTransactionHash").value("0x123abc456def"))
                .andExpect(jsonPath("$.blockchainBlockNumber").value(42));
    }
}
