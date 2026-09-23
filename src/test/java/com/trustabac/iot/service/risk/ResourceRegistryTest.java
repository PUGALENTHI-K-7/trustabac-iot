package com.trustabac.iot.service.risk;

import com.trustabac.iot.entity.ResourceSensitivity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceRegistryTest {

    private final ResourceRegistry registry = new ResourceRegistry();

    @Test
    @DisplayName("ResourceRegistry resolves default sensitivity tiers correctly")
    void testResolveSensitivity() {
        assertEquals(ResourceSensitivity.LOW, registry.resolveSensitivity("SMART_LIGHT"));
        assertEquals(ResourceSensitivity.LOW, registry.resolveSensitivity("AIR_CONDITIONER"));
        assertEquals(ResourceSensitivity.MEDIUM, registry.resolveSensitivity("GUEST_WIFI"));
        assertEquals(ResourceSensitivity.HIGH, registry.resolveSensitivity("SMART_DOOR_LOCK"));
        assertEquals(ResourceSensitivity.HIGH, registry.resolveSensitivity("SECURITY_CAMERA"));
        assertEquals(ResourceSensitivity.CRITICAL, registry.resolveSensitivity("ROUTER"));
        assertEquals(ResourceSensitivity.CRITICAL, registry.resolveSensitivity("OWNER_SETTINGS"));
    }

    @Test
    @DisplayName("ResourceRegistry returns MEDIUM for unmapped resources and null/blank input")
    void testFallbackForUnknown() {
        assertEquals(ResourceSensitivity.MEDIUM, registry.resolveSensitivity("UNKNOWN_RESOURCE_XYZ"));
        assertEquals(ResourceSensitivity.MEDIUM, registry.resolveSensitivity(null));
        assertEquals(ResourceSensitivity.MEDIUM, registry.resolveSensitivity("   "));
    }
}
