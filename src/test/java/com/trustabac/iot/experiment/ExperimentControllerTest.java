package com.trustabac.iot.experiment;

import com.trustabac.iot.experiment.dto.ExperimentComparisonResponse;
import com.trustabac.iot.experiment.dto.ExperimentMetricsResponse;
import com.trustabac.iot.experiment.dto.ExperimentRunResponse;
import com.trustabac.iot.experiment.dto.ScenarioType;
import com.trustabac.iot.experiment.dto.BenchmarkMode;
import com.trustabac.iot.experiment.service.ExperimentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExperimentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExperimentService experimentService;

    @Test
    @DisplayName("POST /api/experiments/run should trigger experiment and return 200 OK")
    void testTriggerExperiment() throws Exception {
        ExperimentRunResponse resp = new ExperimentRunResponse(
                1L, "EXP-TEST-001", ScenarioType.NORMAL_ACCESS, BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN,
                42L, 5, 5, "COMPLETED", "2026-09-22T10:00:00", "2026-09-22T10:00:01", 1000L, 5.0, "Success"
        );
        when(experimentService.runExperiment(any())).thenReturn(resp);

        mockMvc.perform(post("/api/experiments/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenarioType\": \"NORMAL_ACCESS\", \"operations\": 5, \"seed\": 42}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("EXP-TEST-001"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedOperations").value(5));
    }

    @Test
    @DisplayName("GET /api/experiments/{runId}/metrics should return metrics and statistical aggregates")
    void testGetMetrics() throws Exception {
        ExperimentMetricsResponse metrics = new ExperimentMetricsResponse();
        metrics.setRunId("EXP-TEST-001");
        metrics.setScenarioType(ScenarioType.NORMAL_ACCESS);
        metrics.setStatus("COMPLETED");
        metrics.setSampleSize(5);
        metrics.setAuthorizationLatency(Map.of("meanMs", 12.5, "medianMs", 12.0));
        metrics.setEnforcementLatency(Map.of("meanMs", 15.0, "medianMs", 14.5));
        metrics.setBehaviorMatchesExpectation(true);

        when(experimentService.getMetrics("EXP-TEST-001")).thenReturn(Optional.of(metrics));

        mockMvc.perform(get("/api/experiments/EXP-TEST-001/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("EXP-TEST-001"))
                .andExpect(jsonPath("$.sampleSize").value(5))
                .andExpect(jsonPath("$.behaviorMatchesExpectation").value(true));
    }

    @Test
    @DisplayName("GET /api/experiments/{runId}/comparison should return comparison against offline reference modes")
    void testGetComparison() throws Exception {
        ExperimentComparisonResponse comp = new ExperimentComparisonResponse();
        comp.setRunId("EXP-TEST-001");
        comp.setScenarioType(ScenarioType.NORMAL_ACCESS);
        comp.setModeCFullTrustabacBlockchain(Map.of("mode", "MODE_C_FULL_TRUSTABAC_BLOCKCHAIN"));
        comp.setModeAAbacOnlyAnalyticalReference(Map.of("mode", "MODE_A_ABAC_ONLY_ANALYTICAL_REFERENCE"));

        when(experimentService.getComparison("EXP-TEST-001")).thenReturn(Optional.of(comp));

        mockMvc.perform(get("/api/experiments/EXP-TEST-001/comparison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("EXP-TEST-001"))
                .andExpect(jsonPath("$.modeCFullTrustabacBlockchain.mode").value("MODE_C_FULL_TRUSTABAC_BLOCKCHAIN"));
    }

    @Test
    @DisplayName("GET /api/experiments should list all runs")
    void testListAllRuns() throws Exception {
        when(experimentService.listAllRuns()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/experiments"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
