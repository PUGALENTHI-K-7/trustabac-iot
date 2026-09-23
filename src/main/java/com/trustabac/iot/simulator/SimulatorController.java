package com.trustabac.iot.simulator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API controller providing operational control, scenario execution, and status monitoring
 * for the IoT Traffic & Scenario Simulator.
 */
@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final SimulatorService simulatorService;

    public SimulatorController(SimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping("/start")
    public ResponseEntity<SimulatorStatusResponse> start() {
        return ResponseEntity.ok(simulatorService.start());
    }

    @PostMapping("/stop")
    public ResponseEntity<SimulatorStatusResponse> stop() {
        return ResponseEntity.ok(simulatorService.stop());
    }

    @PostMapping("/reset")
    public ResponseEntity<SimulatorStatusResponse> reset() {
        return ResponseEntity.ok(simulatorService.reset());
    }

    @GetMapping("/status")
    public ResponseEntity<SimulatorStatusResponse> getStatus() {
        return ResponseEntity.ok(simulatorService.getStatus());
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<Map<String, String>>> getScenarios() {
        return ResponseEntity.ok(simulatorService.getAvailableScenarios());
    }

    @PostMapping("/scenarios/{scenario}/run")
    public ResponseEntity<SimulatorScenarioResult> runScenario(@PathVariable("scenario") String scenario) {
        return ResponseEntity.ok(simulatorService.runScenarioByName(scenario));
    }

    @PostMapping("/run-all")
    public ResponseEntity<List<SimulatorScenarioResult>> runAllScenarios() {
        return ResponseEntity.ok(simulatorService.runAllScenarios());
    }
}
