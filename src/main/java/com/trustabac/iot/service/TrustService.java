package com.trustabac.iot.service;

import com.trustabac.iot.config.TrustProperties;
import com.trustabac.iot.dto.TrustEventRequest;
import com.trustabac.iot.dto.TrustHistoryResponse;
import com.trustabac.iot.dto.TrustScoreResponse;
import com.trustabac.iot.dto.TrustUpdateResponse;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.entity.TrustHistory;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.TrustHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service orchestrating dynamic behavioral trust scoring, delta updates, clamping,
 * and append-only audit history for IoT devices.
 * Uses an authoritative system Clock to enforce server-generated event timestamps.
 */
@Service
@Transactional
public class TrustService {

    private static final Logger log = LoggerFactory.getLogger(TrustService.class);

    private final DeviceRepository deviceRepository;
    private final TrustHistoryRepository trustHistoryRepository;
    private final TrustProperties trustProperties;
    private final Clock clock;

    @Autowired
    public TrustService(DeviceRepository deviceRepository,
                        TrustHistoryRepository trustHistoryRepository,
                        TrustProperties trustProperties,
                        Clock clock) {
        this.deviceRepository = deviceRepository;
        this.trustHistoryRepository = trustHistoryRepository;
        this.trustProperties = trustProperties;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }

    public TrustService(DeviceRepository deviceRepository,
                        TrustHistoryRepository trustHistoryRepository,
                        TrustProperties trustProperties) {
        this(deviceRepository, trustHistoryRepository, trustProperties, Clock.systemDefaultZone());
    }

    /**
     * Retrieves the current behavioral trust score and monitoring status for a device.
     */
    @Transactional(readOnly = true)
    public TrustScoreResponse getCurrentTrust(String deviceIdentifier) {
        Device device = deviceRepository.findByDeviceIdentifier(deviceIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with identifier: " + deviceIdentifier));

        Double currentTrust = device.getCurrentTrust() != null ? device.getCurrentTrust() : trustProperties.getInitialScore();
        String status = calculateTrustStatus(currentTrust);

        return new TrustScoreResponse(device.getDeviceIdentifier(), currentTrust, status);
    }

    /**
     * Processes an explicit behavioral or security event, calculates the score delta,
     * updates the device score with bounding [0.0, 100.0], and records an append-only audit entry.
     * The event timestamp is strictly established from the server's authoritative Clock.
     */
    public TrustUpdateResponse recordTrustEvent(String deviceIdentifier, TrustEventRequest request) {
        Device device = deviceRepository.findByDeviceIdentifier(deviceIdentifier)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with identifier: " + deviceIdentifier));

        Double oldTrust = device.getCurrentTrust() != null ? device.getCurrentTrust() : trustProperties.getInitialScore();
        double delta = trustProperties.getDeltaForEvent(request.getEventType());

        double calculated = oldTrust + delta;
        double clamped = Math.min(trustProperties.getMaximumScore(),
                Math.max(trustProperties.getMinimumScore(), calculated));

        // Round to 2 decimal places for clean representation
        double newTrust = Math.round(clamped * 100.0) / 100.0;

        device.setCurrentTrust(newTrust);
        Device savedDevice = deviceRepository.save(device);

        LocalDateTime eventTimestamp = LocalDateTime.now(this.clock);

        String source = request.getSource() != null ? request.getSource() : "GATEWAY_EVENT_PIPELINE";
        String reason = request.getReason() != null ? request.getReason() : "Behavioral event: " + request.getEventType();

        TrustHistory history = new TrustHistory(
                savedDevice,
                deviceIdentifier,
                oldTrust,
                newTrust,
                delta,
                request.getEventType(),
                reason,
                source,
                eventTimestamp
        );

        trustHistoryRepository.save(history);

        log.info("Trust update for device '{}': old={}, delta={}, new={}, event='{}', source='{}', timestamp='{}'",
                deviceIdentifier, oldTrust, delta, newTrust, request.getEventType(), source, eventTimestamp);

        String message = String.format("Trust score updated from %.1f to %.1f (delta %.1f) on event %s",
                oldTrust, newTrust, delta, request.getEventType());

        return new TrustUpdateResponse(
                deviceIdentifier,
                oldTrust,
                newTrust,
                delta,
                request.getEventType(),
                reason,
                source,
                eventTimestamp,
                message
        );
    }

    /**
     * Retrieves the complete append-only trust history for a device in newest-first order.
     */
    @Transactional(readOnly = true)
    public List<TrustHistoryResponse> getTrustHistory(String deviceIdentifier) {
        if (!deviceRepository.existsByDeviceIdentifier(deviceIdentifier)) {
            throw new ResourceNotFoundException("Device not found with identifier: " + deviceIdentifier);
        }

        return trustHistoryRepository.findByDeviceIdentifierOrderByCreatedAtDesc(deviceIdentifier).stream()
                .map(this::mapToHistoryResponse)
                .collect(Collectors.toList());
    }

    /**
     * Categorizes the score into descriptive monitoring status bands.
     * Note: Monitoring categories only; does NOT make final access decisions.
     */
    public String calculateTrustStatus(Double score) {
        if (score == null) {
            return "UNKNOWN";
        }
        if (score >= 70.0) {
            return "TRUSTED";
        } else if (score >= 40.0) {
            return "SUSPICIOUS";
        } else {
            return "UNTRUSTED";
        }
    }

    private TrustHistoryResponse mapToHistoryResponse(TrustHistory entity) {
        return new TrustHistoryResponse(
                entity.getId(),
                entity.getDeviceIdentifier(),
                entity.getOldTrust(),
                entity.getNewTrust(),
                entity.getDelta(),
                entity.getEventType(),
                entity.getReason(),
                entity.getSource(),
                entity.getEventTimestamp(),
                entity.getCreatedAt()
        );
    }
}
