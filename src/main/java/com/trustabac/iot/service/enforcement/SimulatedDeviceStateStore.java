package com.trustabac.iot.service.enforcement;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe simulated state repository for IoT devices across the smart-rental property.
 * Maintains realistic telemetry and device operational states without claiming physical hardware control.
 */
@Component
public class SimulatedDeviceStateStore {

    private final Map<String, Map<String, Object>> deviceStates = new ConcurrentHashMap<>();

    public SimulatedDeviceStateStore() {
        initializeDefaultStates();
    }

    private void initializeDefaultStates() {
        // 1. Smart Door Lock
        Map<String, Object> doorLock = new ConcurrentHashMap<>();
        doorLock.put("lockState", "LOCKED");
        doorLock.put("batteryLevel", 95);
        doorLock.put("tamperAlarm", false);
        doorLock.put("lastUnlockMethod", "NONE");
        deviceStates.put("DOOR-SENSOR-001", doorLock);
        deviceStates.put("SMART_DOOR_LOCK", doorLock);

        // 2. Guest Wi-Fi Controller
        Map<String, Object> wifi = new ConcurrentHashMap<>();
        wifi.put("ssid", "SmartRental-Guest-WiFi");
        wifi.put("enabled", true);
        wifi.put("bandwidthTier", "HIGH_SPEED");
        wifi.put("clientCount", 1);
        deviceStates.put("GUEST_WIFI", wifi);

        // 3. Smart TV
        Map<String, Object> tv = new ConcurrentHashMap<>();
        tv.put("power", "OFF");
        tv.put("input", "HDMI-1");
        tv.put("volume", 18);
        deviceStates.put("SMART_TV", tv);

        // 4. Air Conditioner
        Map<String, Object> ac = new ConcurrentHashMap<>();
        ac.put("power", "OFF");
        ac.put("mode", "COOL");
        ac.put("targetTempCelsius", 22.0);
        ac.put("fanSpeed", "AUTO");
        deviceStates.put("AIR_CONDITIONER", ac);

        // 5. Smart Lights
        Map<String, Object> lights = new ConcurrentHashMap<>();
        lights.put("power", "OFF");
        lights.put("brightnessPct", 80);
        lights.put("colorTemp", "WARM_WHITE");
        deviceStates.put("SMART_LIGHT", lights);

        // 6. Smart Thermostat
        Map<String, Object> thermostat = new ConcurrentHashMap<>();
        thermostat.put("currentTempCelsius", 21.5);
        thermostat.put("targetTempCelsius", 22.0);
        thermostat.put("ecoMode", false);
        thermostat.put("hvacMode", "AUTO");
        deviceStates.put("SMART_THERMOSTAT", thermostat);

        // 7. Security Camera
        Map<String, Object> camera = new ConcurrentHashMap<>();
        camera.put("status", "ARMED");
        camera.put("recording", true);
        camera.put("privacyMode", false);
        deviceStates.put("SECURITY_CAMERA", camera);

        // 8. Router Administration
        Map<String, Object> router = new ConcurrentHashMap<>();
        router.put("status", "ONLINE");
        router.put("firewall", "ACTIVE");
        router.put("adminAccess", "RESTRICTED");
        deviceStates.put("ROUTER", router);

        // 9. Owner Settings
        Map<String, Object> ownerSettings = new ConcurrentHashMap<>();
        ownerSettings.put("propertyStatus", "OCCUPIED");
        ownerSettings.put("billingCycle", "ACTIVE");
        deviceStates.put("OWNER_SETTINGS", ownerSettings);
    }

    public Map<String, Object> getState(String key) {
        if (key == null) {
            return new HashMap<>();
        }
        return deviceStates.computeIfAbsent(key.trim().toUpperCase(), k -> {
            Map<String, Object> def = new ConcurrentHashMap<>();
            def.put("status", "ONLINE");
            def.put("operationalState", "NORMAL");
            return def;
        });
    }

    public void updateState(String key, String property, Object value) {
        if (key != null && property != null) {
            getState(key).put(property, value);
        }
    }

    public Map<String, Map<String, Object>> getAllStates() {
        Map<String, Map<String, Object>> copy = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : deviceStates.entrySet()) {
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return copy;
    }

    public void reset() {
        deviceStates.clear();
        initializeDefaultStates();
    }
}
