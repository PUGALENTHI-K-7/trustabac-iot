package com.trustabac.iot.service;

import com.trustabac.iot.dto.BlockchainAuthorizationRequest;
import com.trustabac.iot.dto.BlockchainAuthorizationResponse;
import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.ResourceSensitivity;
import com.trustabac.iot.service.enforcement.SimulatedDeviceStateStore;
import com.trustabac.iot.service.risk.ResourceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service orchestrating end-to-end resource operation enforcement.
 * Integrates the authoritative DecisionCoordinator pipeline (ABAC -> Trust -> Risk -> Smart Contract)
 * and enforces the resulting decision onto simulated IoT hardware state.
 */
@Service
public class ResourceOperationService {

    private static final Logger log = LoggerFactory.getLogger(ResourceOperationService.class);

    private final DecisionCoordinator decisionCoordinator;
    private final ResourceRegistry resourceRegistry;
    private final SimulatedDeviceStateStore stateStore;

    public ResourceOperationService(DecisionCoordinator decisionCoordinator,
                                    ResourceRegistry resourceRegistry,
                                    SimulatedDeviceStateStore stateStore) {
        this.decisionCoordinator = decisionCoordinator;
        this.resourceRegistry = resourceRegistry;
        this.stateStore = stateStore;
    }

    /**
     * Executes a protected resource operation through the authorization and enforcement pipeline.
     */
    @Transactional
    public ResourceOperationResponse executeOperation(ResourceOperationRequest request) {
        String operationId = UUID.randomUUID().toString();
        log.info("Processing resource operation [{}] on device='{}', resource='{}', op='{}', user='{}'",
                operationId, request.deviceIdentifier(), request.resource(), request.operation(), request.userId());

        // 1. Evaluate authorization through the authoritative DecisionCoordinator pipeline
        BlockchainAuthorizationRequest authReq = new BlockchainAuthorizationRequest(
                request.deviceIdentifier(),
                request.userId(),
                request.role(),
                request.organization(),
                request.resource(),
                request.operation(),
                request.location(),
                request.bookingId(),
                request.networkContext(),
                request.requestCountWindow(),
                request.recentViolationCount(),
                request.behavioralIndicator(),
                request.requestReference() != null ? request.requestReference() : "OP-" + operationId
        );

        BlockchainAuthorizationResponse authResp = decisionCoordinator.evaluateAuthorization(authReq);
        Decision decision = authResp.finalDecision();
        ResourceSensitivity sensitivity = resourceRegistry.resolveSensitivity(request.resource());

        // 2. Enforce decision with sensitivity-aware rules
        EnforcementOutcome outcome = enforceDecision(request, decision, sensitivity);

        // 3. Capture current simulated state for the target device/resource
        Map<String, Object> finalState = new HashMap<>(stateStore.getState(resolveStateKey(request)));

        String timestamp = authResp.evaluationTimestamp() != null
                ? authResp.evaluationTimestamp().toString()
                : LocalDateTime.now().toString();

        return new ResourceOperationResponse(
                operationId,
                request.deviceIdentifier(),
                request.resource(),
                request.operation(),
                outcome.status(),
                outcome.effectiveOperation(),
                outcome.message(),
                finalState,
                decision,
                authResp.decisionReason(),
                authResp.abacResult(),
                authResp.abacReason(),
                authResp.trustScore(),
                authResp.trustStatus(),
                authResp.riskScore(),
                authResp.riskStatus(),
                authResp.blockchainTransactionHash(),
                authResp.blockchainBlockNumber(),
                authResp.contractAddress(),
                timestamp
        );
    }

    private EnforcementOutcome enforceDecision(ResourceOperationRequest request, Decision decision, ResourceSensitivity sensitivity) {
        Operation op;
        try {
            op = Operation.valueOf(request.operation().toUpperCase());
        } catch (Exception e) {
            op = Operation.READ;
        }

        String stateKey = resolveStateKey(request);

        if (decision == Decision.ALLOW) {
            return executeAllowedOperation(stateKey, request, op);
        } else if (decision == Decision.RESTRICT) {
            return executeRestrictedOperation(stateKey, request, op, sensitivity);
        } else {
            return new EnforcementOutcome(
                    EnforcementStatus.BLOCKED,
                    "NONE",
                    "Operation blocked: Authorization DENIED by policy / smart contract."
            );
        }
    }

    private EnforcementOutcome executeAllowedOperation(String stateKey, ResourceOperationRequest req, Operation op) {
        if (op == Operation.CONTROL || op == Operation.WRITE || op == Operation.UPDATE) {
            applyStateChange(stateKey, req, false);
            return new EnforcementOutcome(
                    EnforcementStatus.EXECUTED,
                    op.name(),
                    "Operation successfully executed on resource."
            );
        } else {
            return new EnforcementOutcome(
                    EnforcementStatus.EXECUTED,
                    op.name(),
                    "Resource telemetry / status queried successfully."
            );
        }
    }

    private EnforcementOutcome executeRestrictedOperation(String stateKey, ResourceOperationRequest req, Operation op, ResourceSensitivity sensitivity) {
        // READ operations always execute under RESTRICT
        if (op == Operation.READ) {
            return new EnforcementOutcome(
                    EnforcementStatus.EXECUTED,
                    "READ",
                    "Restricted access: Telemetry / status read permitted."
            );
        }

        // For CONTROL / WRITE operations, behavior depends on ResourceSensitivity
        if (sensitivity == ResourceSensitivity.HIGH || sensitivity == ResourceSensitivity.CRITICAL) {
            // HIGH/CRITICAL: Suppress state mutation completely. Door remains locked.
            return new EnforcementOutcome(
                    EnforcementStatus.DOWNGRADED,
                    "READ",
                    "Restricted access: Physical control suppressed for high/critical resource. Status read permitted only."
            );
        } else {
            // LOW/MEDIUM: Execute with safe/clamped constraints
            applyStateChange(stateKey, req, true);
            return new EnforcementOutcome(
                    EnforcementStatus.DOWNGRADED,
                    op.name(),
                    "Restricted access: Operation executed with safe / clamped parameter bounds."
            );
        }
    }

    private void applyStateChange(String stateKey, ResourceOperationRequest req, boolean restrictedMode) {
        String normalizedResource = req.resource().toUpperCase();

        if (normalizedResource.contains("DOOR") || normalizedResource.contains("LOCK") || stateKey.contains("DOOR")) {
            if (!restrictedMode) {
                stateStore.updateState(stateKey, "lockState", "UNLOCKED");
                stateStore.updateState(stateKey, "lastUnlockMethod", "DIGITAL_KEY_" + req.userId());
            }
        } else if (normalizedResource.contains("THERMOSTAT")) {
            double targetTemp = 22.0;
            if (req.commandPayload() != null && req.commandPayload().containsKey("targetTempCelsius")) {
                try {
                    targetTemp = Double.parseDouble(req.commandPayload().get("targetTempCelsius").toString());
                } catch (Exception ignored) {
                }
            }
            if (restrictedMode) {
                // Eco-clamp between 20.0 and 24.0
                targetTemp = Math.max(20.0, Math.min(24.0, targetTemp));
                stateStore.updateState(stateKey, "ecoMode", true);
            }
            stateStore.updateState(stateKey, "targetTempCelsius", targetTemp);
        } else if (normalizedResource.contains("LIGHT")) {
            String power = "ON";
            if (req.commandPayload() != null && req.commandPayload().containsKey("power")) {
                power = req.commandPayload().get("power").toString().toUpperCase();
            }
            stateStore.updateState(stateKey, "power", power);
        } else if (normalizedResource.contains("WIFI")) {
            if (restrictedMode) {
                stateStore.updateState(stateKey, "bandwidthTier", "RESTRICTED_THROTTLED");
            } else {
                stateStore.updateState(stateKey, "bandwidthTier", "HIGH_SPEED");
            }
            stateStore.updateState(stateKey, "enabled", true);
        } else if (normalizedResource.contains("AIR_CONDITIONER") || normalizedResource.contains("AC")) {
            stateStore.updateState(stateKey, "power", "ON");
            if (restrictedMode) {
                stateStore.updateState(stateKey, "mode", "ECO");
            }
        } else if (normalizedResource.contains("TV")) {
            stateStore.updateState(stateKey, "power", "ON");
        } else {
            stateStore.updateState(stateKey, "lastOperation", req.operation());
        }
    }

    private String resolveStateKey(ResourceOperationRequest req) {
        if (req.deviceIdentifier() != null && !req.deviceIdentifier().isBlank()) {
            return req.deviceIdentifier().trim().toUpperCase();
        }
        return req.resource().trim().toUpperCase();
    }

    private record EnforcementOutcome(EnforcementStatus status, String effectiveOperation, String message) {
    }
}
