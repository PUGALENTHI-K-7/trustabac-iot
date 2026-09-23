package com.trustabac.iot.simulator;

import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.dto.TrustEventRequest;
import com.trustabac.iot.dto.TrustUpdateResponse;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.service.ResourceOperationService;
import com.trustabac.iot.service.TrustService;
import com.trustabac.iot.service.enforcement.SimulatedDeviceStateStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core engine for generating smart-rental IoT traffic and executing deterministic or simulated scenarios
 * through the authoritative ResourceOperationService and TrustService pipelines.
 */
@Service
public class SimulatorService {

    private static final Logger log = LoggerFactory.getLogger(SimulatorService.class);

    private final ResourceOperationService resourceOperationService;
    private final TrustService trustService;
    private final SimulatedDeviceStateStore stateStore;
    private final SimulatorConfiguration config;
    private final DeviceRepository deviceRepository;
    private final com.trustabac.iot.messaging.publisher.IoTEventPublisher eventPublisher;
    private final com.trustabac.iot.websocket.WebSocketEventPublisher wsPublisher;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<SimulatorScenarioType> currentScenario = new AtomicReference<>(null);
    private final AtomicInteger totalScenariosExecuted = new AtomicInteger(0);
    private final AtomicInteger totalRequestsGenerated = new AtomicInteger(0);
    private final List<SimulatorScenarioResult> executionHistory = new CopyOnWriteArrayList<>();
    private final AtomicReference<SimulatorScenarioResult> lastScenarioResult = new AtomicReference<>(null);

    @org.springframework.beans.factory.annotation.Autowired
    public SimulatorService(ResourceOperationService resourceOperationService,
                            TrustService trustService,
                            SimulatedDeviceStateStore stateStore,
                            SimulatorConfiguration config,
                            DeviceRepository deviceRepository,
                            com.trustabac.iot.messaging.publisher.IoTEventPublisher eventPublisher,
                            java.util.Optional<com.trustabac.iot.websocket.WebSocketEventPublisher> wsPublisher) {
        this.resourceOperationService = resourceOperationService;
        this.trustService = trustService;
        this.stateStore = stateStore;
        this.config = config;
        this.deviceRepository = deviceRepository;
        this.eventPublisher = eventPublisher;
        this.wsPublisher = wsPublisher != null ? wsPublisher.orElse(null) : null;
    }

    public SimulatorService(ResourceOperationService resourceOperationService,
                            TrustService trustService,
                            SimulatedDeviceStateStore stateStore,
                            SimulatorConfiguration config,
                            DeviceRepository deviceRepository,
                            com.trustabac.iot.messaging.publisher.IoTEventPublisher eventPublisher) {
        this(resourceOperationService, trustService, stateStore, config, deviceRepository, eventPublisher, null);
    }

    public SimulatorService(ResourceOperationService resourceOperationService,
                            TrustService trustService,
                            SimulatedDeviceStateStore stateStore,
                            SimulatorConfiguration config,
                            DeviceRepository deviceRepository) {
        this(resourceOperationService, trustService, stateStore, config, deviceRepository, null, null);
    }

    public SimulatorStatusResponse start() {
        running.set(true);
        log.info("Simulator engine started. Mode: {}", config.getMode());
        return getStatus();
    }

    public SimulatorStatusResponse stop() {
        running.set(false);
        currentScenario.set(null);
        log.info("Simulator engine stopped.");
        return getStatus();
    }

    public SimulatorStatusResponse reset() {
        running.set(false);
        currentScenario.set(null);
        totalScenariosExecuted.set(0);
        totalRequestsGenerated.set(0);
        executionHistory.clear();
        lastScenarioResult.set(null);
        stateStore.reset();
        log.info("Simulator engine and simulated device states reset.");
        return getStatus();
    }

    public SimulatorStatusResponse getStatus() {
        Map<String, Object> statesSummary = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : stateStore.getAllStates().entrySet()) {
            statesSummary.put(entry.getKey(), entry.getValue());
        }

        return new SimulatorStatusResponse(
                running.get(),
                currentScenario.get() != null ? currentScenario.get().name() : null,
                config.getMode(),
                totalScenariosExecuted.get(),
                totalRequestsGenerated.get(),
                lastScenarioResult.get(),
                statesSummary,
                LocalDateTime.now().toString(),
                config.getRandomSeed()
        );
    }

    public List<Map<String, String>> getAvailableScenarios() {
        List<Map<String, String>> list = new ArrayList<>();
        for (SimulatorScenarioType type : SimulatorScenarioType.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("type", type.name());
            item.put("description", type.getDescription());
            list.add(item);
        }
        return list;
    }

    public SimulatorScenarioResult runScenarioByName(String scenarioName) {
        SimulatorScenarioType type = Arrays.stream(SimulatorScenarioType.values())
                .filter(t -> t.name().equalsIgnoreCase(scenarioName.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown simulation scenario: " + scenarioName));
        return runScenario(type);
    }

    public synchronized SimulatorScenarioResult runScenario(SimulatorScenarioType scenarioType) {
        String runId = "SIM-RUN-" + UUID.randomUUID().toString().substring(0, 8);
        long startMs = System.currentTimeMillis();
        String startTime = LocalDateTime.now().toString();

        currentScenario.set(scenarioType);
        log.info("Executing simulation scenario [{}] (ID: {})...", scenarioType.name(), runId);

        SimulatorScenarioResult result = new SimulatorScenarioResult(
                runId,
                scenarioType,
                scenarioType.name(),
                scenarioType.getDescription()
        );
        result.setStartTime(startTime);

        ensureFleetExistsInDatabase();

        try {
            switch (scenarioType) {
                case NORMAL_STAY -> executeNormalStay(result);
                case PRE_CHECKIN -> executePreCheckin(result);
                case SUSPICIOUS_ACTIVITY -> executeSuspiciousActivity(result);
                case REQUEST_FLOODING -> executeRequestFlooding(result);
                case HIGH_RISK_ATTACK -> executeHighRiskAttack(result);
                case LOW_TRUST_ATTACK -> executeLowTrustAttack(result);
                case UNAUTHORIZED_SENSITIVE_ACCESS -> executeUnauthorizedSensitiveAccess(result);
                case RESTRICT_ENFORCEMENT -> executeRestrictEnforcement(result);
                case POST_CHECKOUT -> executePostCheckout(result);
                case RECOVERY -> executeRecovery(result);
            }
            result.setSuccess(true);
            result.setSummary(String.format("Scenario %s executed successfully. %d requests, %d ALLOW, %d RESTRICT, %d DENY.",
                    scenarioType.name(), result.getTotalRequests(), result.getAllowCount(), result.getRestrictCount(), result.getDenyCount()));
        } catch (Exception e) {
            log.error("Scenario {} encountered error: {}", scenarioType.name(), e.getMessage(), e);
            result.setSuccess(false);
            result.setSummary("Scenario execution encountered error: " + e.getMessage());
        } finally {
            long endMs = System.currentTimeMillis();
            result.setEndTime(LocalDateTime.now().toString());
            result.setDurationMs(endMs - startMs);

            Map<String, Object> finalStates = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : stateStore.getAllStates().entrySet()) {
                finalStates.put(entry.getKey(), entry.getValue());
            }
            result.setFinalDeviceStates(finalStates);

            lastScenarioResult.set(result);
            executionHistory.add(result);
            totalScenariosExecuted.incrementAndGet();
            currentScenario.set(null);
        }

        return result;
    }

    public List<SimulatorScenarioResult> runAllScenarios() {
        List<SimulatorScenarioResult> results = new ArrayList<>();
        for (SimulatorScenarioType type : SimulatorScenarioType.values()) {
            results.add(runScenario(type));
        }
        return results;
    }

    // =========================================================================
    // SCENARIO IMPLEMENTATIONS
    // =========================================================================

    /**
     * 1. NORMAL STAY SCENARIO
     * Standard guest interactions across 9 canonical devices with an active booking.
     */
    private void executeNormalStay(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // Ensure baseline trust for normal stay
        try {
            deviceRepository.findByDeviceIdentifier("DOOR-SENSOR-001").ifPresent(d -> {
                if (d.getCurrentTrust() != null && d.getCurrentTrust() < 70.0) {
                    d.setCurrentTrust(80.0);
                    deviceRepository.save(d);
                }
            });
        } catch (Exception ignored) {
        }

        // 1. Door READ
        executeOp(result, 1, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "LOCAL_WIFI", 2, 0, "NORMAL", null);
        // 2. Door UNLOCK (CONTROL)
        executeOp(result, 2, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 3, 0, "NORMAL", null);
        // 3. Smart Light ON (CONTROL)
        Map<String, Object> lightCmd = Map.of("power", "ON");
        executeOp(result, 3, "LIGHT-001", "SMART_LIGHT", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 4, 0, "NORMAL", lightCmd);
        // 4. Smart TV ON (CONTROL)
        Map<String, Object> tvCmd = Map.of("power", "ON");
        executeOp(result, 4, "TV-001", "SMART_TV", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 5, 0, "NORMAL", tvCmd);
        // 5. Air Conditioner ON (CONTROL)
        Map<String, Object> acCmd = Map.of("power", "ON");
        executeOp(result, 5, "AC-001", "AIR_CONDITIONER", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 6, 0, "NORMAL", acCmd);
        // 6. Thermostat READ
        executeOp(result, 6, "THERMOSTAT-001", "SMART_THERMOSTAT", "READ", guest, booking, property, org, "LOCAL_WIFI", 7, 0, "NORMAL", null);
        // 7. Thermostat CONTROL (22.5C)
        Map<String, Object> thermoCmd = Map.of("targetTempCelsius", 22.5);
        executeOp(result, 7, "THERMOSTAT-001", "SMART_THERMOSTAT", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 8, 0, "NORMAL", thermoCmd);
        // 8. Guest Wi-Fi CONTROL
        executeOp(result, 8, "WIFI-001", "GUEST_WIFI", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 9, 0, "NORMAL", null);
        // 9. Door READ
        executeOp(result, 9, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "LOCAL_WIFI", 2, 0, "NORMAL", null);
        // 10. Door LOCK (CONTROL)
        executeOp(result, 10, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 3, 0, "NORMAL", null);
    }

    /**
     * 2. PRE-CHECKIN SCENARIO
     * Guest attempts to access resources prior to active booking check-in window.
     */
    private void executePreCheckin(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = "BOOKING-PRECHECKIN-FUTURE";
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // Guest attempts Door CONTROL, Door READ, and Light CONTROL before valid booking exists/activates
        executeOp(result, 1, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 1, 0, "NORMAL", null);
        executeOp(result, 2, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "LOCAL_WIFI", 2, 0, "NORMAL", null);
        executeOp(result, 3, "LIGHT-001", "SMART_LIGHT", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 3, 0, "NORMAL", Map.of("power", "ON"));
    }

    /**
     * 3. SUSPICIOUS ACTIVITY SCENARIO
     * Guest requests under anomalous contextual flags without ABAC automatically mutating trust;
     * an explicit behavioral security event is then recorded through the Trust API.
     */
    private void executeSuspiciousActivity(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // 1. Contextual suspicious access request
        executeOp(result, 1, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "REMOTE_UNKNOWN_PROXY", 8, 1, "ANOMALOUS_SEQUENCE", null);

        // 2. Explicit security event submission via Trust API + RabbitMQ audit broadcast
        recordAndPublishTrustEvent("DOOR-SENSOR-001", TrustEventType.SUSPICIOUS_ACTIVITY,
                "Simulated anomalous network location and abnormal access pattern",
                "SIMULATOR_SECURITY_AGENT", result.getScenarioId(), property, booking);

        // 3. Subsequent request to observe updated trust in authorization
        executeOp(result, 2, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "LOCAL_WIFI", 2, 0, "NORMAL", null);
    }

    /**
     * 4. REQUEST FLOODING SCENARIO
     * High-frequency burst of requests generating contextual frequency risk and an explicit trust penalty.
     */
    private void executeRequestFlooding(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();
        int burstCount = Math.min(config.getFloodRequestCount(), 15);

        for (int i = 1; i <= burstCount; i++) {
            executeOp(result, i, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "LOCAL_WIFI", 30 + i, 2, "BURST_FLOOD", null);
        }

        // Explicit REQUEST_FLOODING security event + RabbitMQ audit broadcast
        recordAndPublishTrustEvent("DOOR-SENSOR-001", TrustEventType.REQUEST_FLOODING,
                "Simulated high-frequency automated burst exceeding safety threshold",
                "SIMULATOR_BURST_MONITOR", result.getScenarioId(), property, booking);
    }

    /**
     * 5. HIGH-RISK ATTACK SCENARIO
     * Evaluates high risk authorization against critical administrative endpoints and extreme contextual risk.
     */
    private void executeHighRiskAttack(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // 1. Critical Router admin attempt -> ABAC rejects guest
        executeOp(result, 1, "ROUTER-001", "ROUTER", "CONTROL", guest, booking, property, org, "EXTERNAL_TOR_EXIT", 50, 5, "ATTACK_SIGNATURE", null);

        // 2. High-risk context on authorized resource (Door CONTROL under severe risk flags)
        executeOp(result, 2, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "EXTERNAL_PUBLIC_IP", 45, 4, "MALICIOUS_INDICATOR", null);
    }

    /**
     * 6. LOW-TRUST ATTACK SCENARIO
     * Degrades device trust via explicit security events, then executes an ABAC-valid request to verify Smart Contract DENY.
     */
    private void executeLowTrustAttack(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // 1. Degrade trust score explicitly via Trust API + RabbitMQ audit broadcast
        recordAndPublishTrustEvent("DOOR-SENSOR-001", TrustEventType.CONFIRMED_MALICIOUS,
                "Simulated confirmed malicious tamper payload",
                "SIMULATOR_IDS", result.getScenarioId(), property, booking);

        // 2. Attempt valid ABAC request -> Low trust triggers Smart Contract DENY / BLOCKED
        executeOp(result, 1, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 1, 0, "NORMAL", null);
    }

    /**
     * 7. UNAUTHORIZED SENSITIVE ACCESS SCENARIO
     * Guest attempts to access unauthorized sensitive infrastructure resources.
     */
    private void executeUnauthorizedSensitiveAccess(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // 1. Security Camera access
        executeOp(result, 1, "CAM-001", "SECURITY_CAMERA", "READ", guest, booking, property, org, "LOCAL_WIFI", 1, 0, "NORMAL", null);

        // 2. Router control
        executeOp(result, 2, "ROUTER-001", "ROUTER", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 1, 0, "NORMAL", null);

        // 3. Owner settings control
        executeOp(result, 3, "OWNER-001", "OWNER_SETTINGS", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 1, 0, "NORMAL", null);
    }

    /**
     * 8. RESTRICT ENFORCEMENT SCENARIO
     * Exercises sensitivity-aware RESTRICT verdicts (high sensitivity door control suppressed; status read permitted).
     */
    private void executeRestrictEnforcement(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // Reset door trust to a healthy level so trust is sufficient for RESTRICT
        try {
            deviceRepository.findByDeviceIdentifier("DOOR-SENSOR-001").ifPresent(d -> {
                if (d.getCurrentTrust() != null && d.getCurrentTrust() < 70.0) {
                    d.setCurrentTrust(80.0);
                    deviceRepository.save(d);
                }
            });
        } catch (Exception ignored) {
        }

        // 1. Moderate risk Door CONTROL -> RESTRICT -> DOWNGRADED (door remains LOCKED)
        executeOp(result, 1, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "REMOTE_CELLULAR", 12, 1, "NORMAL", null);

        // 2. Moderate risk Door READ -> RESTRICT -> EXECUTED (telemetry permitted)
        executeOp(result, 2, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "REMOTE_CELLULAR", 12, 1, "NORMAL", null);

        // 3. Moderate risk Thermostat CONTROL -> RESTRICT -> DOWNGRADED (eco clamp)
        executeOp(result, 3, "THERMOSTAT-001", "SMART_THERMOSTAT", "CONTROL", guest, booking, property, org, "REMOTE_CELLULAR", 12, 1, "NORMAL", Map.of("targetTempCelsius", 28.0));
    }

    /**
     * 9. POST-CHECKOUT SCENARIO
     * Guest access attempts following booking completion or checkout.
     */
    private void executePostCheckout(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = "BOOKING-EXPIRED-COMPLETED";
        String property = config.getPropertyId();
        String org = config.getOrganization();

        executeOp(result, 1, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 1, 0, "NORMAL", null);
        executeOp(result, 2, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "LOCAL_WIFI", 2, 0, "NORMAL", null);
        executeOp(result, 3, "LIGHT-001", "SMART_LIGHT", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 3, 0, "NORMAL", Map.of("power", "OFF"));
    }

    /**
     * 10. RECOVERY SCENARIO
     * Demonstrates device trust degradation, explicit administrative recovery events, and restored authorization.
     */
    private void executeRecovery(SimulatorScenarioResult result) {
        String guest = config.getGuestUserId();
        String booking = config.getBookingId();
        String property = config.getPropertyId();
        String org = config.getOrganization();

        // 1. Initial degraded state
        recordAndPublishTrustEvent("DOOR-SENSOR-001", TrustEventType.SUSPICIOUS_ACTIVITY,
                "Simulated pre-recovery incident", "SIMULATOR", result.getScenarioId(), property, booking);

        // 2. Recovery event submitted through Trust API + RabbitMQ audit broadcast
        recordAndPublishTrustEvent("DOOR-SENSOR-001", TrustEventType.RECOVERY,
                "Administrative trust verification and key refreshment", "ADMIN_CONSOLE", result.getScenarioId(), property, booking);

        // 3. Normal valid access following recovery -> ALLOW / EXECUTED
        executeOp(result, 1, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", guest, booking, property, org, "LOCAL_WIFI", 1, 0, "NORMAL", null);
        executeOp(result, 2, "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", guest, booking, property, org, "LOCAL_WIFI", 2, 0, "NORMAL", null);
    }

    private void recordAndPublishTrustEvent(String deviceId, TrustEventType type, String reason, String source,
                                            String scenarioId, String property, String booking) {
        try {
            TrustEventRequest req = new TrustEventRequest();
            req.setEventType(type);
            req.setReason(reason);
            req.setSource(source);
            TrustUpdateResponse updateResp = trustService.recordTrustEvent(deviceId, req);
            log.info("Explicit trust security event recorded: {} -> newTrust={}", type, updateResp.getNewTrust());

            if (eventPublisher != null) {
                String corrId = "CORR-" + scenarioId;
                com.trustabac.iot.messaging.event.TrustDomainEvent trustPayload =
                        new com.trustabac.iot.messaging.event.TrustDomainEvent(
                                deviceId,
                                type,
                                updateResp.getOldTrust(),
                                updateResp.getNewTrust(),
                                updateResp.getDelta(),
                                reason,
                                source,
                                LocalDateTime.now().toString(),
                                corrId
                        );
                com.trustabac.iot.messaging.event.EventEnvelope<com.trustabac.iot.messaging.event.TrustDomainEvent> trustEnv =
                        com.trustabac.iot.messaging.event.EventEnvelope.of(
                                "TRUST_UPDATE_AUDIT",
                                source,
                                deviceId,
                                property,
                                booking,
                                "TRUST-REF-" + System.currentTimeMillis(),
                                corrId,
                                trustPayload
                        );
                eventPublisher.publishTrustEvent(trustEnv);
            }
        } catch (Exception e) {
            log.warn("Unable to process trust event for device {}: {}", deviceId, e.getMessage());
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void executeOp(SimulatorScenarioResult result,
                           int stepNumber,
                           String deviceIdentifier,
                           String resource,
                           String operation,
                           String userId,
                           String bookingId,
                           String location,
                           String org,
                           String networkContext,
                           int requestCountWindow,
                           int recentViolationCount,
                           String behavioralIndicator,
                           Map<String, Object> commandPayload) {

        String eventId = "SIM-EVT-" + UUID.randomUUID().toString().substring(0, 8);
        String ref = "SIM-REF-" + System.currentTimeMillis();

        ResourceOperationRequest req = new ResourceOperationRequest(
                deviceIdentifier,
                userId,
                "GUEST",
                org,
                resource,
                operation,
                location,
                bookingId,
                networkContext,
                behavioralIndicator,
                requestCountWindow,
                recentViolationCount,
                ref,
                commandPayload
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        SimulatorEvent event = new SimulatorEvent(
                eventId,
                result.getScenarioType(),
                stepNumber,
                resp.timestamp() != null ? resp.timestamp() : LocalDateTime.now().toString(),
                deviceIdentifier,
                resource,
                operation,
                resp.authorizationDecision(),
                resp.enforcementStatus(),
                resp.effectiveOperation(),
                resp.trustScore(),
                resp.riskScore(),
                resp.abacResult(),
                resp.blockchainTxHash(),
                resp.blockchainBlockNumber(),
                resp.executionMessage()
        );

        result.addEvent(event);
        totalRequestsGenerated.incrementAndGet();

        // Asynchronously publish telemetry and authorization outcome to RabbitMQ
        if (eventPublisher != null) {
            String corrId = "CORR-" + result.getScenarioId();

            // 1. Device Operation Event
            com.trustabac.iot.messaging.event.DeviceOperationEvent devPayload =
                    new com.trustabac.iot.messaging.event.DeviceOperationEvent(
                            deviceIdentifier,
                            resource,
                            operation,
                            resp.effectiveOperation(),
                            resp.authorizationDecision(),
                            resp.enforcementStatus(),
                            resp.trustScore(),
                            resp.riskScore(),
                            resp.abacResult(),
                            resp.executionMessage(),
                            event.timestamp(),
                            corrId
                    );
            com.trustabac.iot.messaging.event.EventEnvelope<com.trustabac.iot.messaging.event.DeviceOperationEvent> devEnv =
                    com.trustabac.iot.messaging.event.EventEnvelope.of(
                            "DEVICE_OPERATION",
                            "IOT_SIMULATOR",
                            deviceIdentifier,
                            location,
                            bookingId,
                            ref,
                            corrId,
                            devPayload
                    );
            eventPublisher.publishDeviceOperation(devEnv);

            // 2. Authorization Result Event
            com.trustabac.iot.messaging.event.AuthorizationResultEvent authPayload =
                    new com.trustabac.iot.messaging.event.AuthorizationResultEvent(
                            ref,
                            corrId,
                            deviceIdentifier,
                            resource,
                            operation,
                            resp.abacResult(),
                            resp.trustScore(),
                            resp.riskScore(),
                            resp.authorizationDecision(),
                            resp.enforcementStatus(),
                            resp.effectiveOperation(),
                            resp.blockchainTxHash(),
                            resp.blockchainBlockNumber(),
                            event.timestamp()
                    );
            com.trustabac.iot.messaging.event.EventEnvelope<com.trustabac.iot.messaging.event.AuthorizationResultEvent> authEnv =
                    com.trustabac.iot.messaging.event.EventEnvelope.of(
                            "AUTHORIZATION_RESULT",
                            "AUTHORIZATION_ENGINE",
                            deviceIdentifier,
                            location,
                            bookingId,
                            ref,
                            corrId,
                            authPayload
                    );
            eventPublisher.publishAuthorizationResult(authEnv);

            // 3. Blockchain Result Event (if on-chain transaction executed)
            if (resp.blockchainTxHash() != null && !resp.blockchainTxHash().isBlank()) {
                com.trustabac.iot.messaging.event.BlockchainResultEvent bcPayload =
                        new com.trustabac.iot.messaging.event.BlockchainResultEvent(
                                resp.blockchainTxHash(),
                                resp.blockchainBlockNumber(),
                                resp.contractAddress(),
                                resp.authorizationDecision(),
                                deviceIdentifier,
                                resource,
                                operation,
                                event.timestamp(),
                                corrId
                        );
                com.trustabac.iot.messaging.event.EventEnvelope<com.trustabac.iot.messaging.event.BlockchainResultEvent> bcEnv =
                        com.trustabac.iot.messaging.event.EventEnvelope.of(
                                "BLOCKCHAIN_RESULT",
                                "SOLIDITY_SMART_CONTRACT",
                                deviceIdentifier,
                                location,
                                bookingId,
                                ref,
                                corrId,
                                bcPayload
                        );
                eventPublisher.publishBlockchainResult(bcEnv);
            }

            // 4. Risk Evaluation Audit Event
            com.trustabac.iot.messaging.event.RiskDomainEvent riskPayload =
                    new com.trustabac.iot.messaging.event.RiskDomainEvent(
                            deviceIdentifier,
                            resource,
                            operation,
                            location,
                            networkContext,
                            requestCountWindow,
                            recentViolationCount,
                            behavioralIndicator,
                            resp.riskScore(),
                            "EVALUATED",
                            "Contextual risk evaluation audit",
                            event.timestamp(),
                            corrId
                    );
            com.trustabac.iot.messaging.event.EventEnvelope<com.trustabac.iot.messaging.event.RiskDomainEvent> riskEnv =
                    com.trustabac.iot.messaging.event.EventEnvelope.of(
                            "RISK_EVALUATION_AUDIT",
                            "RISK_ENGINE",
                            deviceIdentifier,
                            location,
                            bookingId,
                            ref,
                            corrId,
                            riskPayload
                    );
            eventPublisher.publishRiskEvent(riskEnv);
        }

        // 5. WebSocket Simulator Step Broadcast (observational only)
        if (wsPublisher != null) {
            try {
                String corrId = "CORR-" + result.getScenarioId();
                com.trustabac.iot.messaging.event.SimulatorStepEvent simStep =
                        new com.trustabac.iot.messaging.event.SimulatorStepEvent(
                                eventId,
                                result.getScenarioType() != null ? result.getScenarioType().name() : "CUSTOM",
                                stepNumber,
                                deviceIdentifier,
                                resource,
                                operation,
                                resp.authorizationDecision() != null ? resp.authorizationDecision().name() : "UNKNOWN",
                                resp.enforcementStatus() != null ? resp.enforcementStatus().name() : "UNKNOWN",
                                resp.trustScore(),
                                resp.riskScore(),
                                resp.abacResult() != null ? resp.abacResult() : "UNKNOWN",
                                resp.blockchainTxHash(),
                                resp.blockchainBlockNumber(),
                                resp.executionMessage(),
                                event.timestamp(),
                                corrId
                        );
                com.trustabac.iot.messaging.event.EventEnvelope<com.trustabac.iot.messaging.event.SimulatorStepEvent> simEnv =
                        com.trustabac.iot.messaging.event.EventEnvelope.of(
                                "SIMULATOR_STEP",
                                "IOT_SIMULATOR",
                                deviceIdentifier,
                                location,
                                bookingId,
                                ref,
                                corrId,
                                simStep
                        );
                wsPublisher.publishSimulatorStep(simEnv);
            } catch (Exception e) {
                log.warn("Failed to broadcast simulator step event to WebSocket: {}", e.getMessage());
            }
        }
    }

    private void ensureFleetExistsInDatabase() {
        ensureDevice("DOOR-SENSOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR, 80.0);
        ensureDevice("WIFI-001", DeviceType.GUEST_WIFI, DeviceClass.CONTROLLER, 80.0);
        ensureDevice("TV-001", DeviceType.SMART_TV, DeviceClass.ENDPOINT, 80.0);
        ensureDevice("AC-001", DeviceType.AIR_CONDITIONER, DeviceClass.ACTUATOR, 80.0);
        ensureDevice("LIGHT-001", DeviceType.SMART_LIGHT, DeviceClass.ACTUATOR, 80.0);
        ensureDevice("THERMOSTAT-001", DeviceType.SMART_THERMOSTAT, DeviceClass.ACTUATOR, 80.0);
        ensureDevice("CAM-001", DeviceType.SECURITY_CAMERA, DeviceClass.SENSOR, 80.0);
        ensureDevice("ROUTER-001", DeviceType.ROUTER, DeviceClass.GATEWAY, 80.0);
        ensureDevice("OWNER-001", DeviceType.OWNER_SETTINGS, DeviceClass.CONTROLLER, 80.0);
    }

    private void ensureDevice(String deviceIdentifier, DeviceType type, DeviceClass deviceClass, double initialTrust) {
        try {
            if (deviceRepository.findByDeviceIdentifier(deviceIdentifier).isEmpty()) {
                Device d = new Device(deviceIdentifier, type, deviceClass, RegistrationStatus.REGISTERED, initialTrust, true);
                deviceRepository.save(d);
            }
        } catch (Exception e) {
            log.debug("Device {} already registered or database in-memory: {}", deviceIdentifier, e.getMessage());
        }
    }
}
