package com.trustabac.iot.experiment.controller;

import com.trustabac.iot.experiment.dto.ExperimentComparisonResponse;
import com.trustabac.iot.experiment.dto.ExperimentMetricsResponse;
import com.trustabac.iot.experiment.dto.ExperimentRunRequest;
import com.trustabac.iot.experiment.dto.ExperimentRunResponse;
import com.trustabac.iot.experiment.entity.ExperimentRun;
import com.trustabac.iot.experiment.service.ExperimentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for executing controlled research experiments and retrieving benchmark statistics.
 * Strictly observational/benchmarking: exercises the existing authoritative pipeline and never bypasses smart contracts.
 */
@RestController
@RequestMapping("/api/experiments")
public class ExperimentController {

    private final ExperimentService experimentService;

    public ExperimentController(ExperimentService experimentService) {
        this.experimentService = experimentService;
    }

    @PostMapping("/run")
    public ResponseEntity<ExperimentRunResponse> triggerExperiment(@RequestBody(required = false) ExperimentRunRequest request) {
        if (request == null) {
            request = new ExperimentRunRequest();
        }
        ExperimentRunResponse response = experimentService.runExperiment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{runId}/metrics")
    public ResponseEntity<ExperimentMetricsResponse> getExperimentMetrics(@PathVariable String runId) {
        return experimentService.getMetrics(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{runId}/comparison")
    public ResponseEntity<ExperimentComparisonResponse> getExperimentComparison(@PathVariable String runId) {
        return experimentService.getComparison(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{runId}/raw-samples")
    public ResponseEntity<List<com.trustabac.iot.experiment.dto.ExperimentRawSample>> getRawSamples(@PathVariable String runId) {
        List<com.trustabac.iot.experiment.dto.ExperimentRawSample> samples = experimentService.getRawSamples(runId);
        return ResponseEntity.ok(samples);
    }

    @GetMapping
    public ResponseEntity<List<ExperimentRun>> listAllRuns() {
        return ResponseEntity.ok(experimentService.listAllRuns());
    }

    @PostMapping("/{runId}/stop")
    public ResponseEntity<Map<String, Object>> stopRun(@PathVariable String runId) {
        boolean stopped = experimentService.stopRun(runId);
        return ResponseEntity.ok(Map.of(
                "runId", runId,
                "stopped", stopped,
                "message", stopped ? "Experiment run stopped successfully." : "Run not found or already completed."
        ));
    }
}
