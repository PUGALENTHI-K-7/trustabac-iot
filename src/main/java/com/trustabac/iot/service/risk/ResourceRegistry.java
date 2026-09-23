package com.trustabac.iot.service.risk;

import com.trustabac.iot.entity.ResourceSensitivity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative server-side registry resolving resource sensitivity and metadata.
 * Prevents clients from spoofing or supplying arbitrary resource sensitivity values.
 */
@Component
public class ResourceRegistry {

    private final Map<String, ResourceSensitivity> sensitivityMap = new ConcurrentHashMap<>();

    public ResourceRegistry() {
        // Low Sensitivity: standard non-critical amenities
        sensitivityMap.put("SMART_LIGHT", ResourceSensitivity.LOW);
        sensitivityMap.put("AIR_CONDITIONER", ResourceSensitivity.LOW);
        sensitivityMap.put("SMART_THERMOSTAT", ResourceSensitivity.LOW);

        // Medium Sensitivity: shared network amenities
        sensitivityMap.put("GUEST_WIFI", ResourceSensitivity.MEDIUM);
        sensitivityMap.put("SMART_SPEAKER", ResourceSensitivity.MEDIUM);

        // High Sensitivity: perimeter physical access and surveillance
        sensitivityMap.put("SMART_DOOR_LOCK", ResourceSensitivity.HIGH);
        sensitivityMap.put("SECURITY_CAMERA", ResourceSensitivity.HIGH);
        sensitivityMap.put("GARAGE_DOOR", ResourceSensitivity.HIGH);

        // Critical Sensitivity: administrative, owner, network infrastructure
        sensitivityMap.put("ROUTER", ResourceSensitivity.CRITICAL);
        sensitivityMap.put("OWNER_SETTINGS", ResourceSensitivity.CRITICAL);
        sensitivityMap.put("SECURITY_CONFIGURATION", ResourceSensitivity.CRITICAL);
        sensitivityMap.put("GATEWAY_ADMIN", ResourceSensitivity.CRITICAL);
    }

    /**
     * Authoritatively resolves the sensitivity tier of a requested resource identifier or type.
     */
    public ResourceSensitivity resolveSensitivity(String resourceIdentifier) {
        if (resourceIdentifier == null || resourceIdentifier.isBlank()) {
            return ResourceSensitivity.MEDIUM;
        }
        String normalized = resourceIdentifier.trim().toUpperCase();
        return sensitivityMap.getOrDefault(normalized, ResourceSensitivity.MEDIUM);
    }

    /**
     * Allows dynamic registration of custom resource sensitivities for experimentation.
     */
    public void registerResource(String resourceIdentifier, ResourceSensitivity sensitivity) {
        if (resourceIdentifier != null && sensitivity != null) {
            sensitivityMap.put(resourceIdentifier.trim().toUpperCase(), sensitivity);
        }
    }
}
