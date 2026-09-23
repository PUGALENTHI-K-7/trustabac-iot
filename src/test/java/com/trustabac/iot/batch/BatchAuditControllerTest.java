package com.trustabac.iot.batch;

import com.trustabac.iot.batch.controller.BatchAuditController;
import com.trustabac.iot.batch.dto.BatchAuditStatusResponse;
import com.trustabac.iot.batch.dto.BatchReportSummaryResponse;
import com.trustabac.iot.batch.dto.BatchRunResponse;
import com.trustabac.iot.batch.service.BatchAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
@org.springframework.test.context.ActiveProfiles("test")
class BatchAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BatchAuditService batchAuditService;

    @Test
    @DisplayName("POST /api/batch/audit/run should trigger batch job and return 200 OK")
    void testTriggerBatchRun() throws Exception {
        BatchRunResponse resp = new BatchRunResponse(10L, 100L, "PERIOD_TEST", "COMPLETED", "Success", false);
        when(batchAuditService.runAudit(any())).thenReturn(resp);

        mockMvc.perform(post("/api/batch/audit/run")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"periodKey\": \"PERIOD_TEST\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodKey").value("PERIOD_TEST"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.alreadyProcessed").value(false));
    }

    @Test
    @DisplayName("GET /api/batch/audit/status/{id} should return 200 OK with status details")
    void testGetStatus() throws Exception {
        BatchAuditStatusResponse resp = new BatchAuditStatusResponse();
        resp.setBatchRunId(10L);
        resp.setPeriodKey("PERIOD_TEST");
        resp.setStatus("COMPLETED");
        resp.setTotalRecordsRead(50L);

        when(batchAuditService.getStatus(10L)).thenReturn(Optional.of(resp));

        mockMvc.perform(get("/api/batch/audit/status/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchRunId").value(10))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalRecordsRead").value(50));
    }

    @Test
    @DisplayName("GET /api/batch/audit/reports/latest should return 200 OK with report details")
    void testGetLatestReport() throws Exception {
        BatchReportSummaryResponse report = new BatchReportSummaryResponse();
        report.setBatchRunId(10L);
        report.setPeriodKey("PERIOD_TEST");
        report.setStatus("COMPLETED");
        report.setAuthorization(Map.of("totalRequests", 20, "allowCount", 18));
        report.setReconciliation(Map.of("reconciledSuccessfully", true));

        when(batchAuditService.getLatestReport()).thenReturn(Optional.of(report));

        mockMvc.perform(get("/api/batch/audit/reports/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodKey").value("PERIOD_TEST"))
                .andExpect(jsonPath("$.authorization.totalRequests").value(20))
                .andExpect(jsonPath("$.reconciliation.reconciledSuccessfully").value(true));
    }

    @Test
    @DisplayName("GET /api/batch/audit/reports should list all runs")
    void testListAllRuns() throws Exception {
        when(batchAuditService.listAllRuns()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/batch/audit/reports"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
