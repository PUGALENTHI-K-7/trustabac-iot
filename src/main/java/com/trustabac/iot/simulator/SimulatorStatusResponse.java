package com.trustabac.iot.simulator;

import java.util.Map;

/**
 * High-level status response reporting simulator health, execution metrics, and current device states.
 */
public record SimulatorStatusResponse(
        boolean running,
        String currentScenario,
        String mode,
        int totalScenariosExecuted,
        int totalRequestsGenerated,
        SimulatorScenarioResult lastScenarioResult,
        Map<String, Object> deviceStates,
        String systemTime,
        Long randomSeed
) {
}
