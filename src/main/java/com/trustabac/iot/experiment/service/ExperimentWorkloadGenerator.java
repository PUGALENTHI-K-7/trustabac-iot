package com.trustabac.iot.experiment.service;

import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.experiment.dto.ScenarioType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Deterministic workload generator for experimental scenarios.
 * Uses explicit pseudo-random seeds to guarantee exact reproducibility across runs.
 */
@Component
public class ExperimentWorkloadGenerator {

    private static final String BOOKING_REF = "BOOKING-PHASE6B-001";
    private static final String GUEST_USER = "guest-user-001";
    private static final String ORG = "SmartRental";
    private static final String PROPERTY = "Property-001";

    public List<ResourceOperationRequest> generateWorkload(ScenarioType scenarioType, int operations, long seed) {
        Random rand = new Random(seed);
        List<ResourceOperationRequest> list = new ArrayList<>(operations);

        for (int i = 0; i < operations; i++) {
            list.add(createRequestForScenario(scenarioType, i, rand));
        }
        return list;
    }

    private ResourceOperationRequest createRequestForScenario(ScenarioType scenarioType, int index, Random rand) {
        String reqRef = "EXP-" + scenarioType.name() + "-" + index;

        switch (scenarioType) {
            case NORMAL_ACCESS:
                // Valid booking, registered devices, allowed operation, normal context
                String[] normDevs = {"LIGHT-001", "THERMOSTAT-001", "TV-001", "AC-001", "DOOR-SENSOR-001"};
                String devNorm = normDevs[rand.nextInt(normDevs.length)];
                String resNorm = devNorm.startsWith("DOOR") ? "SMART_DOOR_LOCK"
                        : devNorm.startsWith("LIGHT") ? "SMART_LIGHT"
                        : devNorm.startsWith("THERM") ? "SMART_THERMOSTAT"
                        : devNorm.startsWith("TV") ? "SMART_TV" : "AIR_CONDITIONER";
                String opNorm = devNorm.startsWith("DOOR") ? "READ" : "CONTROL";

                return new ResourceOperationRequest(
                        devNorm, GUEST_USER, "GUEST", ORG,
                        resNorm, opNorm, PROPERTY, BOOKING_REF,
                        "LOCAL_WIFI", "NORMAL", 2, 0,
                        reqRef, Map.of("power", "ON", "targetTempCelsius", 22.0)
                );

            case RESTRICT_ACCESS:
                // Door lock control under moderate risk (frequency=12, violations=2) -> RESTRICT / DOWNGRADED
                return new ResourceOperationRequest(
                        "DOOR-SENSOR-001", GUEST_USER, "GUEST", ORG,
                        "SMART_DOOR_LOCK", "CONTROL", PROPERTY, BOOKING_REF,
                        "REMOTE_CELLULAR", "NORMAL", 12, 2,
                        reqRef, Map.of("lockState", "UNLOCKED")
                );

            case LOW_TRUST:
                // Device attempting door operation with degraded trust (< 50)
                return new ResourceOperationRequest(
                        "DOOR-SENSOR-001", GUEST_USER, "GUEST", ORG,
                        "SMART_DOOR_LOCK", "READ", PROPERTY, BOOKING_REF,
                        "LOCAL_WIFI", "NORMAL", 1, 0,
                        reqRef, Map.of()
                );

            case HIGH_RISK:
                // Extreme risk context on sensitive door control (mismatched location, anomalous behavior, burst frequency, violations) -> Risk > 70
                return new ResourceOperationRequest(
                        "DOOR-SENSOR-001", GUEST_USER, "GUEST", ORG,
                        "SMART_DOOR_LOCK", "CONTROL", "Unknown-Remote-Location", BOOKING_REF,
                        "PUBLIC_INTERNET", "ANOMALOUS", 35, 6,
                        reqRef, Map.of("lockState", "UNLOCKED")
                );

            case ABAC_FAILURE:
                // ABAC condition failure: Unauthorized access to SECURITY_CAMERA
                return new ResourceOperationRequest(
                        "CAM-001", GUEST_USER, "GUEST", ORG,
                        "SECURITY_CAMERA", "CONTROL", PROPERTY, BOOKING_REF,
                        "LOCAL_WIFI", "NORMAL", 1, 0,
                        reqRef, Map.of("record", true)
                );

            case MIXED_SECURITY_WORKLOAD:
                // Deterministic mix across all archetype requests
                int archetype = rand.nextInt(5);
                if (archetype == 0) return createRequestForScenario(ScenarioType.NORMAL_ACCESS, index, rand);
                if (archetype == 1) return createRequestForScenario(ScenarioType.RESTRICT_ACCESS, index, rand);
                if (archetype == 2) return createRequestForScenario(ScenarioType.LOW_TRUST, index, rand);
                if (archetype == 3) return createRequestForScenario(ScenarioType.HIGH_RISK, index, rand);
                return createRequestForScenario(ScenarioType.ABAC_FAILURE, index, rand);

            case BLOCKCHAIN_OUTAGE:
            case RECOVERY:
            default:
                return new ResourceOperationRequest(
                        "DOOR-SENSOR-001", GUEST_USER, "GUEST", ORG,
                        "SMART_DOOR_LOCK", "READ", PROPERTY, BOOKING_REF,
                        "LOCAL_WIFI", "NORMAL", 1, 0,
                        reqRef, Map.of()
                );
        }
    }
}
