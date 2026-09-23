package com.trustabac.iot.service;

import com.trustabac.iot.config.RiskProperties;
import com.trustabac.iot.dto.RiskEvaluationRequest;
import com.trustabac.iot.dto.RiskEvaluationResponse;
import com.trustabac.iot.dto.RiskFactorBreakdown;
import com.trustabac.iot.dto.RiskHistoryResponse;
import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.ResourceSensitivity;
import com.trustabac.iot.entity.RiskEvent;
import com.trustabac.iot.entity.RiskStatus;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.RiskEventRepository;
import com.trustabac.iot.service.risk.BehaviorRiskCalculator;
import com.trustabac.iot.service.risk.FrequencyRiskCalculator;
import com.trustabac.iot.service.risk.LocationRiskCalculator;
import com.trustabac.iot.service.risk.NetworkRiskCalculator;
import com.trustabac.iot.service.risk.ResourceRegistry;
import com.trustabac.iot.service.risk.RiskContext;
import com.trustabac.iot.service.risk.SensitivityRiskCalculator;
import com.trustabac.iot.service.risk.TimeRiskCalculator;
import com.trustabac.iot.service.risk.ViolationRiskCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Service orchestrating contextual risk evaluation, weighted composite score calculation,
 * status mapping, and append-only audit persistence.
 * Note: Risk represents current contextual danger and is strictly independent of long-term device Trust.
 */
@Service
@Transactional
public class RiskService {

    private static final Logger log = LoggerFactory.getLogger(RiskService.class);

    private final DeviceRepository deviceRepository;
    private final RiskEventRepository riskEventRepository;
    private final ResourceRegistry resourceRegistry;
    private final RiskProperties riskProperties;
    private final TimeRiskCalculator timeRiskCalculator;
    private final LocationRiskCalculator locationRiskCalculator;
    private final SensitivityRiskCalculator sensitivityRiskCalculator;
    private final FrequencyRiskCalculator frequencyRiskCalculator;
    private final NetworkRiskCalculator networkRiskCalculator;
    private final ViolationRiskCalculator violationRiskCalculator;
    private final BehaviorRiskCalculator behaviorRiskCalculator;
    private final Clock clock;

    @Autowired
    public RiskService(DeviceRepository deviceRepository,
                       RiskEventRepository riskEventRepository,
                       ResourceRegistry resourceRegistry,
                       RiskProperties riskProperties,
                       TimeRiskCalculator timeRiskCalculator,
                       LocationRiskCalculator locationRiskCalculator,
                       SensitivityRiskCalculator sensitivityRiskCalculator,
                       FrequencyRiskCalculator frequencyRiskCalculator,
                       NetworkRiskCalculator networkRiskCalculator,
                       ViolationRiskCalculator violationRiskCalculator,
                       BehaviorRiskCalculator behaviorRiskCalculator,
                       Clock clock) {
        this.deviceRepository = deviceRepository;
        this.riskEventRepository = riskEventRepository;
        this.resourceRegistry = resourceRegistry;
        this.riskProperties = riskProperties;
        this.timeRiskCalculator = timeRiskCalculator;
        this.locationRiskCalculator = locationRiskCalculator;
        this.sensitivityRiskCalculator = sensitivityRiskCalculator;
        this.frequencyRiskCalculator = frequencyRiskCalculator;
        this.networkRiskCalculator = networkRiskCalculator;
        this.violationRiskCalculator = violationRiskCalculator;
        this.behaviorRiskCalculator = behaviorRiskCalculator;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public RiskService(DeviceRepository deviceRepository,
                       RiskEventRepository riskEventRepository,
                       ResourceRegistry resourceRegistry,
                       RiskProperties riskProperties,
                       TimeRiskCalculator timeRiskCalculator,
                       LocationRiskCalculator locationRiskCalculator,
                       SensitivityRiskCalculator sensitivityRiskCalculator,
                       FrequencyRiskCalculator frequencyRiskCalculator,
                       NetworkRiskCalculator networkRiskCalculator,
                       ViolationRiskCalculator violationRiskCalculator,
                       BehaviorRiskCalculator behaviorRiskCalculator) {
        this(deviceRepository, riskEventRepository, resourceRegistry, riskProperties,
                timeRiskCalculator, locationRiskCalculator, sensitivityRiskCalculator,
                frequencyRiskCalculator, networkRiskCalculator, violationRiskCalculator,
                behaviorRiskCalculator, Clock.systemDefaultZone());
    }

    /**
     * Resolves authoritative server context and evaluates contextual risk for a public request.
     */
    public RiskEvaluationResponse resolveAndEvaluate(RiskEvaluationRequest request) {
        ResourceSensitivity resolvedSensitivity = resourceRegistry.resolveSensitivity(request.getResource());
        LocalDateTime evaluationTimestamp = LocalDateTime.now(this.clock);

        int requestCount = request.getRequestCountWindow() != null ? request.getRequestCountWindow() : 1;
        int violations = request.getRecentViolationCount() != null ? request.getRecentViolationCount() : 0;
        BehavioralIndicator behavior = request.getBehavioralIndicator() != null
                ? request.getBehavioralIndicator()
                : BehavioralIndicator.NORMAL;

        RiskContext context = RiskContext.builder()
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

        return evaluateRisk(context);
    }

    /**
     * Evaluates all 7 contextual risk factors against a resolved RiskContext, calculates
     * the weighted composite score, clamps to [0.0, 100.0], maps status, and persists an append-only RiskEvent.
     */
    public RiskEvaluationResponse evaluateRisk(RiskContext context) {
        Device device = deviceRepository.findByDeviceIdentifier(context.deviceIdentifier())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with identifier: " + context.deviceIdentifier()));

        // Calculate normalized factor contributions (0.0 to 1.0)
        double timeFactor = timeRiskCalculator.calculate(context);
        double locationFactor = locationRiskCalculator.calculate(context);
        double sensitivityFactor = sensitivityRiskCalculator.calculate(context);
        double frequencyFactor = frequencyRiskCalculator.calculate(context);
        double networkFactor = networkRiskCalculator.calculate(context);
        double violationFactor = violationRiskCalculator.calculate(context);
        double behaviorFactor = behaviorRiskCalculator.calculate(context);

        RiskFactorBreakdown breakdown = new RiskFactorBreakdown(
                roundTo2Decimals(timeFactor),
                roundTo2Decimals(locationFactor),
                roundTo2Decimals(sensitivityFactor),
                roundTo2Decimals(frequencyFactor),
                roundTo2Decimals(networkFactor),
                roundTo2Decimals(violationFactor),
                roundTo2Decimals(behaviorFactor)
        );

        // Weighted calculation
        RiskProperties.Weights w = riskProperties.getWeights();
        double rawRisk = (timeFactor * w.getTime())
                + (locationFactor * w.getLocation())
                + (sensitivityFactor * w.getSensitivity())
                + (frequencyFactor * w.getFrequency())
                + (networkFactor * w.getNetwork())
                + (violationFactor * w.getViolations())
                + (behaviorFactor * w.getBehavior());

        // Clamp to configured bounds [min, max]
        double clamped = Math.min(riskProperties.getMaximumScore(),
                Math.max(riskProperties.getMinimumScore(), rawRisk));
        double riskScore = Math.round(clamped * 10.0) / 10.0;

        RiskStatus status = mapRiskStatus(riskScore);
        String reason = generateRiskReason(breakdown, status, context);

        LocalDateTime timestamp = context.evaluationTimestamp() != null
                ? context.evaluationTimestamp()
                : LocalDateTime.now(this.clock);

        String factorSummary = String.format(Locale.US,
                "time=%.2f, location=%.2f, sensitivity=%.2f, frequency=%.2f, network=%.2f, violation=%.2f, behavior=%.2f",
                timeFactor, locationFactor, sensitivityFactor, frequencyFactor, networkFactor, violationFactor, behaviorFactor);

        RiskEvent riskEvent = new RiskEvent(
                device,
                context.deviceIdentifier(),
                riskScore,
                status,
                context.resource() != null ? context.resource() : "UNKNOWN",
                context.operation(),
                factorSummary,
                reason,
                timestamp
        );

        riskEventRepository.save(riskEvent);

        log.info("Risk evaluation for device '{}': score={}, status={}, reason='{}', timestamp='{}'",
                context.deviceIdentifier(), riskScore, status, reason, timestamp);

        return new RiskEvaluationResponse(
                context.deviceIdentifier(),
                riskScore,
                status,
                breakdown,
                reason,
                timestamp
        );
    }

    /**
     * Retrieves the latest evaluated risk score for a device.
     */
    @Transactional(readOnly = true)
    public RiskEvaluationResponse getCurrentRisk(String deviceIdentifier) {
        if (!deviceRepository.existsByDeviceIdentifier(deviceIdentifier)) {
            throw new ResourceNotFoundException("Device not found with identifier: " + deviceIdentifier);
        }

        return riskEventRepository.findFirstByDeviceIdentifierOrderByEvaluationTimestampDesc(deviceIdentifier)
                .map(this::mapToEvaluationResponse)
                .orElseGet(() -> new RiskEvaluationResponse(
                        deviceIdentifier,
                        0.0,
                        RiskStatus.LOW,
                        new RiskFactorBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
                        "No prior risk events recorded for device; baseline low risk.",
                        LocalDateTime.now(this.clock)
                ));
    }

    /**
     * Retrieves the complete append-only risk history for a device (newest first).
     */
    @Transactional(readOnly = true)
    public List<RiskHistoryResponse> getRiskHistory(String deviceIdentifier) {
        if (!deviceRepository.existsByDeviceIdentifier(deviceIdentifier)) {
            throw new ResourceNotFoundException("Device not found with identifier: " + deviceIdentifier);
        }

        return riskEventRepository.findByDeviceIdentifierOrderByEvaluationTimestampDesc(deviceIdentifier).stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps a composite risk score into monitoring categories (LOW, MEDIUM, HIGH).
     */
    public RiskStatus mapRiskStatus(Double score) {
        if (score == null || score <= riskProperties.getLowThreshold()) {
            return RiskStatus.LOW;
        }
        if (score <= riskProperties.getMediumThreshold()) {
            return RiskStatus.MEDIUM;
        }
        return RiskStatus.HIGH;
    }

    private String generateRiskReason(RiskFactorBreakdown factors, RiskStatus status, RiskContext context) {
        List<String> contributing = new ArrayList<>();
        if (factors.sensitivityRisk() >= 0.70) {
            contributing.add("high resource sensitivity (" + context.resourceSensitivity() + ")");
        }
        if (factors.frequencyRisk() >= 0.50) {
            contributing.add("elevated request frequency (" + context.requestCountWindow() + " reqs)");
        }
        if (factors.networkRisk() >= 0.50) {
            contributing.add("untrusted/remote network (" + context.networkContext() + ")");
        }
        if (factors.locationRisk() >= 0.50) {
            contributing.add("unexpected location (" + context.location() + ")");
        }
        if (factors.timeRisk() >= 0.50) {
            contributing.add("outside normal operational hours");
        }
        if (factors.violationRisk() >= 0.50) {
            contributing.add("recent security violations (" + context.recentViolationCount() + ")");
        }
        if (factors.behaviorRisk() >= 0.50) {
            contributing.add("abnormal behavioral indicator (" + context.behavioralIndicator() + ")");
        }

        if (contributing.isEmpty()) {
            return "Standard access context within normal operational and environmental parameters.";
        }
        return "Contextual risk elevated due to: " + String.join(", ", contributing) + ".";
    }

    private double roundTo2Decimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private RiskEvaluationResponse mapToEvaluationResponse(RiskEvent event) {
        // Parse summary or provide defaults
        return new RiskEvaluationResponse(
                event.getDeviceIdentifier(),
                event.getRiskScore(),
                event.getRiskStatus(),
                null,
                event.getReason(),
                event.getEvaluationTimestamp()
        );
    }

    private RiskHistoryResponse mapToHistoryResponse(RiskEvent event) {
        return new RiskHistoryResponse(
                event.getId(),
                event.getDeviceIdentifier(),
                event.getRiskScore(),
                event.getRiskStatus(),
                event.getResource(),
                event.getOperation(),
                event.getFactorSummary(),
                event.getReason(),
                event.getEvaluationTimestamp(),
                event.getCreatedAt()
        );
    }
}
