package com.trustabac.iot.batch.controller;

import com.trustabac.iot.batch.dto.BatchAuditStatusResponse;
import com.trustabac.iot.batch.dto.BatchReportSummaryResponse;
import com.trustabac.iot.batch.dto.BatchRunRequest;
import com.trustabac.iot.batch.dto.BatchRunResponse;
import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.service.BatchAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller providing manual trigger and observational status/report inspection
 * for Spring Batch offline analytics.
 * Strictly non-authoritative: triggers offline batch analytics only and never evaluates live access.
 */
@RestController
@RequestMapping("/api/batch/audit")
public class BatchAuditController {

    private final BatchAuditService batchAuditService;

    public BatchAuditController(BatchAuditService batchAuditService) {
        this.batchAuditService = batchAuditService;
    }

    @PostMapping("/run")
    public ResponseEntity<BatchRunResponse> triggerBatchRun(@RequestBody(required = false) BatchRunRequest request) {
        if (request == null) {
            request = new BatchRunRequest();
        }
        BatchRunResponse response = batchAuditService.runAudit(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{batchRunId}")
    public ResponseEntity<BatchAuditStatusResponse> getStatusById(@PathVariable Long batchRunId) {
        return batchAuditService.getStatus(batchRunId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/status")
    public ResponseEntity<BatchAuditStatusResponse> getStatusByPeriod(@RequestParam String periodKey) {
        return batchAuditService.getStatusByPeriodKey(periodKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reports/latest")
    public ResponseEntity<BatchReportSummaryResponse> getLatestReport() {
        return batchAuditService.getLatestReport()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/reports/{periodKey}")
    public ResponseEntity<BatchReportSummaryResponse> getReportByPeriod(@PathVariable String periodKey) {
        return batchAuditService.getReport(periodKey)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/reports")
    public ResponseEntity<List<BatchRunAudit>> listAllRuns() {
        return ResponseEntity.ok(batchAuditService.listAllRuns());
    }
}
