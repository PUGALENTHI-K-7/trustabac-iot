package com.trustabac.iot.service;

import com.trustabac.iot.dto.AccessEvaluationRequest;
import com.trustabac.iot.dto.AccessEvaluationResponse;
import com.trustabac.iot.dto.AccessRequestLogResponse;
import com.trustabac.iot.dto.RiskEvaluationResponse;
import com.trustabac.iot.dto.RiskFactorBreakdown;
import com.trustabac.iot.entity.AbacAttributeKeys;
import com.trustabac.iot.entity.AbacResult;
import com.trustabac.iot.entity.AccessRequest;
import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Booking;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.entity.ResourceSensitivity;
import com.trustabac.iot.entity.RiskStatus;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.PolicyRepository;
import com.trustabac.iot.service.risk.ResourceRegistry;
import com.trustabac.iot.service.risk.RiskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Core orchestrator for the ABAC eligibility evaluation gate and multi-tier access pipeline.
 * Evaluates ABAC eligibility (Gate 1).
 * If FAIL: returns immediately, leaves device trust unchanged, bypasses Risk evaluation.
 * If PASS: retrieves device Trust (Gate 2) and evaluates contextual Risk (Gate 3).
 * Note: Does NOT produce final ALLOW / RESTRICT / DENY decisions (reserved for Phase 6).
 */
@Service
@Transactional
public class AbacService {

    private static final Logger log = LoggerFactory.getLogger(AbacService.class);

    private final DeviceRepository deviceRepository;
    private final PolicyRepository policyRepository;
    private final BookingService bookingService;
    private final PolicyEvaluator policyEvaluator;
    private final AccessRequestRepository accessRequestRepository;
    private final ResourceRegistry resourceRegistry;
    private final RiskService riskService;
    private final Clock clock;

    public AbacService(DeviceRepository deviceRepository,
                       PolicyRepository policyRepository,
                       BookingService bookingService,
                       PolicyEvaluator policyEvaluator,
                       AccessRequestRepository accessRequestRepository,
                       ResourceRegistry resourceRegistry,
                       RiskService riskService,
                       Clock clock) {
        this.deviceRepository = deviceRepository;
        this.policyRepository = policyRepository;
        this.bookingService = bookingService;
        this.policyEvaluator = policyEvaluator;
        this.accessRequestRepository = accessRequestRepository;
        this.resourceRegistry = resourceRegistry;
        this.riskService = riskService;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    /**
     * Evaluates an incoming access request across the multi-tier pipeline.
     */
    public AccessEvaluationResponse evaluateAccess(AccessEvaluationRequest request) {
        // 1. Establish authoritative evaluation timestamp
        LocalDateTime evaluationTimestamp = request.getRequestTimestamp() != null
                ? request.getRequestTimestamp()
                : LocalDateTime.now(this.clock);

        // 2. Pre-check: Device resolution and validation
        Optional<Device> deviceOpt = deviceRepository.findByDeviceIdentifier(request.getDeviceIdentifier());
        if (deviceOpt.isEmpty()) {
            return recordAndRespond(request, evaluationTimestamp, AbacResult.FAIL,
                    "Device '" + request.getDeviceIdentifier() + "' is not registered in the gateway inventory",
                    null, null);
        }

        Device device = deviceOpt.get();
        if (!Boolean.TRUE.equals(device.getActive())) {
            return recordAndRespond(request, evaluationTimestamp, AbacResult.FAIL,
                    "Device '" + request.getDeviceIdentifier() + "' is inactive",
                    null, device);
        }

        if (device.getRegistrationStatus() != RegistrationStatus.REGISTERED) {
            return recordAndRespond(request, evaluationTimestamp, AbacResult.FAIL,
                    "Device '" + request.getDeviceIdentifier() + "' registration status is "
                            + device.getRegistrationStatus() + " and is ineligible for access",
                    null, device);
        }

        // 3. Context resolution: Booking validity check
        Booking booking = null;
        boolean isBookingValid = false;
        if (request.getBookingId() != null && !request.getBookingId().isBlank()) {
            Optional<Booking> bookingOpt = bookingService.findByBookingReference(request.getBookingId());
            if (bookingOpt.isPresent()) {
                booking = bookingOpt.get();
                isBookingValid = bookingService.isBookingValid(booking, evaluationTimestamp, request.getLocation());
            }
        }

        // 4. Assemble canonical 5-dimension attribute dictionary
        Map<String, Object> attributes = buildAttributeMap(request, device, booking, isBookingValid, evaluationTimestamp);

        // 5. Find matching active candidate policies
        List<Policy> activePolicies = policyRepository.findByActiveTrue();
        List<Policy> candidatePolicies = activePolicies.stream()
                .filter(p -> isPolicyApplicable(p, request))
                .toList();

        if (candidatePolicies.isEmpty()) {
            return recordAndRespond(request, evaluationTimestamp, AbacResult.FAIL,
                    String.format("No active policy applies to resource '%s' and operation '%s'",
                            request.getResource(), request.getOperation()),
                    null, device);
        }

        // 6. Evaluate candidate policies deterministically
        List<String> failureExplanations = new ArrayList<>();
        for (Policy candidate : candidatePolicies) {
            PolicyEvaluator.ConditionEvaluationResult evalResult = policyEvaluator.evaluatePolicy(candidate, attributes);
            if (evalResult.passed()) {
                String passReason = "All required ABAC conditions satisfied for policy '" + candidate.getName() + "'.";
                log.info("ABAC PASS: user='{}' device='{}' resource='{}' op='{}' policy='{}'",
                        request.getUserId(), request.getDeviceIdentifier(), request.getResource(),
                        request.getOperation(), candidate.getName());
                return recordAndRespond(request, evaluationTimestamp, AbacResult.PASS, passReason, candidate.getName(), device);
            } else {
                failureExplanations.add(String.format("Policy '%s': %s", candidate.getName(), evalResult.reason()));
            }
        }

        // 7. If all candidate policies failed, produce explainable failure
        String aggregatedReason = failureExplanations.size() == 1
                ? failureExplanations.getFirst()
                : "All applicable policies failed evaluation: [" + String.join("; ", failureExplanations) + "]";

        log.info("ABAC FAIL: user='{}' device='{}' resource='{}' op='{}' reason='{}'",
                request.getUserId(), request.getDeviceIdentifier(), request.getResource(),
                request.getOperation(), aggregatedReason);

        String evaluatedPolicyNames = candidatePolicies.stream()
                .map(Policy::getName)
                .collect(Collectors.joining(", "));

        return recordAndRespond(request, evaluationTimestamp, AbacResult.FAIL, aggregatedReason, evaluatedPolicyNames, device);
    }

    @Transactional(readOnly = true)
    public AccessRequestLogResponse getAccessRequestLogById(Long id) {
        AccessRequest entity = accessRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Access request log not found with id: " + id));
        return mapToLogResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<AccessRequestLogResponse> getAllAccessRequestLogs() {
        return accessRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToLogResponse)
                .collect(Collectors.toList());
    }

    private boolean isPolicyApplicable(Policy policy, AccessEvaluationRequest request) {
        String targetRes = policy.getTargetResource();
        boolean resourceMatch = "*".equals(targetRes)
                || "ALL".equalsIgnoreCase(targetRes)
                || targetRes.equalsIgnoreCase(request.getResource())
                || (request.getResourceType() != null && targetRes.equalsIgnoreCase(request.getResourceType()));

        boolean operationMatch = policy.getTargetOperation() == null
                || policy.getTargetOperation() == request.getOperation();

        return resourceMatch && operationMatch;
    }

    private Map<String, Object> buildAttributeMap(AccessEvaluationRequest request, Device device,
                                                  Booking booking, boolean isBookingValid,
                                                  LocalDateTime evaluationTimestamp) {
        Map<String, Object> attrs = new HashMap<>();

        // Subject Dimension
        attrs.put(AbacAttributeKeys.SUBJECT_USER_ID, request.getUserId());
        attrs.put(AbacAttributeKeys.SUBJECT_ROLE, request.getRole());
        attrs.put(AbacAttributeKeys.SUBJECT_ORGANIZATION, request.getOrganization());
        attrs.put(AbacAttributeKeys.SUBJECT_BOOKING_ID, request.getBookingId());

        // Device Dimension
        attrs.put(AbacAttributeKeys.DEVICE_IDENTIFIER, device.getDeviceIdentifier());
        attrs.put(AbacAttributeKeys.DEVICE_TYPE, device.getDeviceType() != null ? device.getDeviceType().name() : null);
        attrs.put(AbacAttributeKeys.DEVICE_CLASS, device.getDeviceClass() != null ? device.getDeviceClass().name() : null);
        attrs.put(AbacAttributeKeys.DEVICE_REGISTRATION_STATUS,
                device.getRegistrationStatus() != null ? device.getRegistrationStatus().name() : null);
        attrs.put(AbacAttributeKeys.DEVICE_ACTIVE, String.valueOf(device.getActive()));

        // Resource Dimension - Authoritatively resolved sensitivity
        ResourceSensitivity sensitivity = resourceRegistry.resolveSensitivity(request.getResource());
        attrs.put(AbacAttributeKeys.RESOURCE_IDENTIFIER, request.getResource());
        attrs.put(AbacAttributeKeys.RESOURCE_TYPE,
                request.getResourceType() != null && !request.getResourceType().isBlank()
                        ? request.getResourceType()
                        : request.getResource());
        attrs.put(AbacAttributeKeys.RESOURCE_SENSITIVITY, sensitivity.name());

        // Operation Dimension
        attrs.put(AbacAttributeKeys.OPERATION, request.getOperation().name());

        // Context & Booking Dimension
        attrs.put(AbacAttributeKeys.CONTEXT_LOCATION, request.getLocation());
        attrs.put(AbacAttributeKeys.CONTEXT_REQUEST_TIMESTAMP, evaluationTimestamp.toString());
        attrs.put(AbacAttributeKeys.CONTEXT_NETWORK_CONTEXT, request.getNetworkContext());
        attrs.put(AbacAttributeKeys.BOOKING_VALID, String.valueOf(isBookingValid));
        attrs.put(AbacAttributeKeys.BOOKING_STATUS, booking != null && booking.getBookingStatus() != null
                ? booking.getBookingStatus().name() : null);
        attrs.put(AbacAttributeKeys.BOOKING_PROPERTY_ID, booking != null ? booking.getPropertyId() : null);
        attrs.put(AbacAttributeKeys.BOOKING_GUEST_USER_ID, booking != null ? booking.getGuestUserId() : null);

        return attrs;
    }

    private AccessEvaluationResponse recordAndRespond(AccessEvaluationRequest request,
                                                      LocalDateTime evaluationTimestamp,
                                                      AbacResult result,
                                                      String reason,
                                                      String policyName,
                                                      Device device) {
        String truncatedReason = reason != null && reason.length() > 500
                ? reason.substring(0, 497) + "..."
                : reason;

        AccessRequest accessRequest = new AccessRequest(
                request.getDeviceIdentifier(),
                request.getUserId(),
                request.getRole(),
                request.getOrganization(),
                request.getResource(),
                request.getResourceType(),
                request.getOperation(),
                request.getLocation(),
                request.getBookingId(),
                request.getNetworkContext(),
                evaluationTimestamp,
                result,
                truncatedReason,
                policyName
        );

        AccessRequest saved = accessRequestRepository.save(accessRequest);

        // Gate 2: Trust Lookup (only on PASS; on FAIL trust is not evaluated/returned and remains untouched)
        Double trustScore = null;
        String trustStatus = null;
        Double riskScore = null;
        RiskStatus riskStatus = null;
        RiskFactorBreakdown riskFactors = null;
        String riskReason = null;

        if (result == AbacResult.PASS && device != null) {
            // Retrieve current authoritative device trust
            trustScore = device.getCurrentTrust() != null ? device.getCurrentTrust() : 80.0;
            trustStatus = trustScore >= 70.0 ? "TRUSTED" : (trustScore >= 40.0 ? "SUSPICIOUS" : "UNTRUSTED");

            // Gate 3: Contextual Risk Evaluation
            ResourceSensitivity resolvedSensitivity = resourceRegistry.resolveSensitivity(request.getResource());
            int requestCount = request.getRequestCountWindow() != null ? request.getRequestCountWindow() : 1;
            int violations = request.getRecentViolationCount() != null ? request.getRecentViolationCount() : 0;
            BehavioralIndicator behavior = request.getBehavioralIndicator() != null
                    ? request.getBehavioralIndicator()
                    : BehavioralIndicator.NORMAL;

            RiskContext riskContext = RiskContext.builder()
                    .deviceIdentifier(request.getDeviceIdentifier())
                    .userId(request.getUserId())
                    .resource(request.getResource())
                    .resourceSensitivity(resolvedSensitivity)
                    .operation(request.getOperation())
                    .location(request.getLocation())
                    .networkContext(request.getNetworkContext())
                    .evaluationTimestamp(evaluationTimestamp)
                    .requestCountWindow(requestCount)
                    .recentViolationCount(violations)
                    .behavioralIndicator(behavior)
                    .build();

            RiskEvaluationResponse riskEval = riskService.evaluateRisk(riskContext);
            riskScore = riskEval.riskScore();
            riskStatus = riskEval.riskStatus();
            riskFactors = riskEval.factors();
            riskReason = riskEval.reason();
        }

        return new AccessEvaluationResponse(
                saved.getId(),
                result,
                reason,
                policyName,
                evaluationTimestamp,
                request.getDeviceIdentifier(),
                request.getUserId(),
                request.getResource(),
                request.getOperation(),
                trustScore,
                trustStatus,
                riskScore,
                riskStatus,
                riskFactors,
                riskReason
        );
    }

    private AccessRequestLogResponse mapToLogResponse(AccessRequest entity) {
        return new AccessRequestLogResponse(
                entity.getId(),
                entity.getDeviceIdentifier(),
                entity.getUserId(),
                entity.getRole(),
                entity.getOrganization(),
                entity.getResource(),
                entity.getResourceType(),
                entity.getOperation(),
                entity.getLocation(),
                entity.getBookingId(),
                entity.getNetworkContext(),
                entity.getRequestTimestamp(),
                entity.getAbacResult(),
                entity.getAbacReason(),
                entity.getEvaluatedPolicyName(),
                entity.getCreatedAt()
        );
    }
}
