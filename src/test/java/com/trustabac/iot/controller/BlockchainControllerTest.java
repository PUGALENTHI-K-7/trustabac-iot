package com.trustabac.iot.controller;

import com.trustabac.iot.dto.BlockchainStatusResponse;
import com.trustabac.iot.dto.ContractThresholdResponse;
import com.trustabac.iot.service.blockchain.BlockchainService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlockchainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BlockchainService blockchainService;

    @Test
    @DisplayName("GET /api/blockchain/status should return 200 with RPC and contract reachability")
    void shouldReturnBlockchainStatus() throws Exception {
        BlockchainStatusResponse mockStatus = new BlockchainStatusResponse(
                "http://127.0.0.1:8545",
                true,
                1337L,
                105L,
                "0x123ContractAddr",
                true,
                true
        );

        when(blockchainService.getStatus()).thenReturn(mockStatus);

        mockMvc.perform(get("/api/blockchain/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rpcUrl").value("http://127.0.0.1:8545"))
                .andExpect(jsonPath("$.rpcReachable").value(true))
                .andExpect(jsonPath("$.chainId").value(1337))
                .andExpect(jsonPath("$.latestBlock").value(105))
                .andExpect(jsonPath("$.contractReachable").value(true));
    }

    @Test
    @DisplayName("GET /api/blockchain/thresholds should return 200 with contract thresholds")
    void shouldReturnContractThresholds() throws Exception {
        ContractThresholdResponse mockThresholds = new ContractThresholdResponse(
                70,
                30,
                30,
                70,
                "0x123ContractAddr"
        );

        when(blockchainService.getThresholds()).thenReturn(mockThresholds);

        mockMvc.perform(get("/api/blockchain/thresholds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trustHigh").value(70))
                .andExpect(jsonPath("$.trustMedium").value(30))
                .andExpect(jsonPath("$.riskLow").value(30))
                .andExpect(jsonPath("$.riskMedium").value(70));
    }

    @Test
    @DisplayName("POST /api/blockchain/deploy should return 200 with deployed contract address")
    void shouldDeployContract() throws Exception {
        when(blockchainService.deployContract()).thenReturn("0xDeployedContractAddress123");

        mockMvc.perform(post("/api/blockchain/deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPLOYED"))
                .andExpect(jsonPath("$.contractAddress").value("0xDeployedContractAddress123"));
    }
}
