package com.trustabac.iot.controller;

import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.RiskEventRepository;
import com.trustabac.iot.repository.TrustHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private TrustHistoryRepository trustHistoryRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private AccessRequestRepository accessRequestRepository;

    @BeforeEach
    void setUp() {
        riskEventRepository.deleteAll();
        trustHistoryRepository.deleteAll();
        accessRequestRepository.deleteAll();
        deviceRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/devices should create a new device and return 201 Created")
    void testRegisterDeviceSuccess() throws Exception {
        String payload = """
                {
                    "deviceIdentifier": "IOT-DOOR-001",
                    "deviceType": "SMART_DOOR_LOCK",
                    "deviceClass": "ACTUATOR"
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.deviceIdentifier").value("IOT-DOOR-001"))
                .andExpect(jsonPath("$.deviceType").value("SMART_DOOR_LOCK"))
                .andExpect(jsonPath("$.deviceClass").value("ACTUATOR"))
                .andExpect(jsonPath("$.registrationStatus").value("REGISTERED"))
                .andExpect(jsonPath("$.currentTrust").value(80.0))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        assertEquals(1, deviceRepository.count());
    }

    @Test
    @DisplayName("POST /api/devices with duplicate identifier should return 409 Conflict")
    void testRegisterDuplicateDeviceReturnsConflict() throws Exception {
        String payload = """
                {
                    "deviceIdentifier": "IOT-DOOR-001",
                    "deviceType": "SMART_DOOR_LOCK",
                    "deviceClass": "ACTUATOR"
                }
                """;

        // First registration succeeds
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());

        // Duplicate registration returns 409
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Device with identifier 'IOT-DOOR-001' already exists"));
    }

    @Test
    @DisplayName("POST /api/devices with invalid payload should return 400 Bad Request")
    void testRegisterDeviceValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "deviceIdentifier": "",
                    "deviceType": null,
                    "deviceClass": null
                }
                """;

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("GET /api/devices should return all registered devices")
    void testGetAllDevices() throws Exception {
        Device d1 = new Device("DEV-1", DeviceType.SMART_LIGHT, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        Device d2 = new Device("DEV-2", DeviceType.SMART_THERMOSTAT, DeviceClass.CONTROLLER, RegistrationStatus.REGISTERED, 80.0, true);
        deviceRepository.save(d1);
        deviceRepository.save(d2);

        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].deviceIdentifier").value("DEV-1"))
                .andExpect(jsonPath("$[1].deviceIdentifier").value("DEV-2"));
    }

    @Test
    @DisplayName("GET /api/devices/{id} should return device details when found")
    void testGetDeviceById() throws Exception {
        Device d = new Device("DEV-TARGET", DeviceType.SMART_TV, DeviceClass.ENDPOINT, RegistrationStatus.REGISTERED, 80.0, true);
        Device saved = deviceRepository.save(d);

        mockMvc.perform(get("/api/devices/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-TARGET"))
                .andExpect(jsonPath("$.deviceType").value("SMART_TV"));
    }

    @Test
    @DisplayName("GET /api/devices/{id} with nonexistent ID should return 404 Not Found")
    void testGetDeviceByNonexistentId() throws Exception {
        mockMvc.perform(get("/api/devices/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Device not found with ID: 99999"));
    }

    @Test
    @DisplayName("GET /api/devices/identifier/{deviceIdentifier} should return device details")
    void testGetDeviceByIdentifier() throws Exception {
        Device d = new Device("DEV-IDENT-01", DeviceType.GUEST_WIFI, DeviceClass.CONTROLLER, RegistrationStatus.REGISTERED, 80.0, true);
        deviceRepository.save(d);

        mockMvc.perform(get("/api/devices/identifier/DEV-IDENT-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-IDENT-01"))
                .andExpect(jsonPath("$.deviceType").value("GUEST_WIFI"));
    }

    @Test
    @DisplayName("PUT /api/devices/{id} should update device metadata")
    void testUpdateDevice() throws Exception {
        Device d = new Device("DEV-UPDATE", DeviceType.SMART_LIGHT, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        Device saved = deviceRepository.save(d);

        String updatePayload = """
                {
                    "deviceType": "AIR_CONDITIONER",
                    "deviceClass": "ENDPOINT",
                    "registrationStatus": "SUSPENDED",
                    "active": false
                }
                """;

        mockMvc.perform(put("/api/devices/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.deviceType").value("AIR_CONDITIONER"))
                .andExpect(jsonPath("$.deviceClass").value("ENDPOINT"))
                .andExpect(jsonPath("$.registrationStatus").value("SUSPENDED"))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("DELETE /api/devices/{id} should soft-deactivate and return REVOKED status")
    void testDeactivateDevice() throws Exception {
        Device d = new Device("DEV-REVOKE", DeviceType.SMART_LIGHT, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        Device saved = deviceRepository.save(d);

        mockMvc.perform(delete("/api/devices/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.registrationStatus").value("REVOKED"))
                .andExpect(jsonPath("$.active").value(false));

        // Verify device still exists in database (not physically deleted)
        mockMvc.perform(get("/api/devices/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationStatus").value("REVOKED"))
                .andExpect(jsonPath("$.active").value(false));

        assertFalse(deviceRepository.findById(saved.getId()).orElseThrow().getActive());
    }
}
