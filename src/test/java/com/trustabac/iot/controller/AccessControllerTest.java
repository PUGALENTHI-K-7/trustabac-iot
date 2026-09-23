package com.trustabac.iot.controller;

import com.trustabac.iot.entity.AbacAttributeKeys;
import com.trustabac.iot.entity.AttributeCategory;
import com.trustabac.iot.entity.Booking;
import com.trustabac.iot.entity.BookingStatus;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.PolicyCondition;
import com.trustabac.iot.entity.PolicyOperator;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.BookingRepository;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.PolicyRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AccessRequestRepository accessRequestRepository;

    @Autowired
    private TrustHistoryRepository trustHistoryRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @BeforeEach
    void setUp() {
        riskEventRepository.deleteAll();
        trustHistoryRepository.deleteAll();
        accessRequestRepository.deleteAll();
        policyRepository.deleteAll();
        bookingRepository.deleteAll();
        deviceRepository.deleteAll();

        // 1. Seed Device
        Device door = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED, 80.0, true);
        deviceRepository.save(door);

        // 2. Seed Booking
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking("BOOK-001", "Property-001", "Guest-001",
                now.minusDays(1), now.plusDays(2), BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // 3. Seed Policy
        Policy policy = new Policy("Guest Door Lock Access", "Allows guest to control door",
                "SMART_DOOR_LOCK", Operation.CONTROL, true);
        policy.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, AbacAttributeKeys.SUBJECT_ROLE, PolicyOperator.EQUALS, "GUEST"));
        policy.addCondition(new PolicyCondition(AttributeCategory.DEVICE, AbacAttributeKeys.DEVICE_ACTIVE, PolicyOperator.EQUALS, "true"));
        policy.addCondition(new PolicyCondition(AttributeCategory.DEVICE, AbacAttributeKeys.DEVICE_REGISTRATION_STATUS, PolicyOperator.EQUALS, "REGISTERED"));
        policy.addCondition(new PolicyCondition(AttributeCategory.CONTEXT, AbacAttributeKeys.BOOKING_VALID, PolicyOperator.EQUALS, "true"));
        policyRepository.save(policy);
    }

    @Test
    @DisplayName("POST /api/access/evaluate returns PASS with Trust & Risk for eligible guest access request")
    void testEvaluateAccessPass() throws Exception {
        String payload = """
                {
                    "deviceIdentifier": "DEV-DOOR-001",
                    "userId": "Guest-001",
                    "role": "GUEST",
                    "organization": "SmartRental",
                    "resource": "SMART_DOOR_LOCK",
                    "operation": "CONTROL",
                    "location": "Property-001",
                    "bookingId": "BOOK-001",
                    "networkContext": "LOCAL_WIFI"
                }
                """;

        mockMvc.perform(post("/api/access/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("PASS"))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.evaluatedPolicyName").value("Guest Door Lock Access"))
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-DOOR-001"))
                .andExpect(jsonPath("$.userId").value("Guest-001"))
                .andExpect(jsonPath("$.resource").value("SMART_DOOR_LOCK"))
                .andExpect(jsonPath("$.operation").value("CONTROL"))
                .andExpect(jsonPath("$.trustScore").value(80.0))
                .andExpect(jsonPath("$.trustStatus").value("TRUSTED"))
                .andExpect(jsonPath("$.riskScore").isNumber())
                .andExpect(jsonPath("$.riskStatus").value("LOW"));

        assertEquals(1, accessRequestRepository.count());
        assertEquals(1, riskEventRepository.count());
    }

    @Test
    @DisplayName("POST /api/access/evaluate returns FAIL when device is revoked (no Risk evaluation)")
    void testEvaluateAccessFailRevokedDevice() throws Exception {
        Device door = deviceRepository.findByDeviceIdentifier("DEV-DOOR-001").orElseThrow();
        door.setRegistrationStatus(RegistrationStatus.REVOKED);
        deviceRepository.save(door);

        String payload = """
                {
                    "deviceIdentifier": "DEV-DOOR-001",
                    "userId": "Guest-001",
                    "role": "GUEST",
                    "resource": "SMART_DOOR_LOCK",
                    "operation": "CONTROL",
                    "location": "Property-001",
                    "bookingId": "BOOK-001"
                }
                """;

        mockMvc.perform(post("/api/access/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("REVOKED")))
                .andExpect(jsonPath("$.riskScore").doesNotExist());

        assertEquals(1, accessRequestRepository.count());
        assertEquals(0, riskEventRepository.count());
    }

    @Test
    @DisplayName("POST /api/access/evaluate with invalid request body returns 400 Bad Request")
    void testEvaluateAccessValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "deviceIdentifier": "",
                    "userId": "",
                    "role": "",
                    "resource": "",
                    "operation": null
                }
                """;

        mockMvc.perform(post("/api/access/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/access/requests returns list of evaluated access requests")
    void testGetAllAccessRequestLogs() throws Exception {
        // Execute an evaluation first
        String payload = """
                {
                    "deviceIdentifier": "DEV-DOOR-001",
                    "userId": "Guest-001",
                    "role": "GUEST",
                    "resource": "SMART_DOOR_LOCK",
                    "operation": "CONTROL",
                    "location": "Property-001",
                    "bookingId": "BOOK-001"
                }
                """;

        mockMvc.perform(post("/api/access/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/access/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].deviceIdentifier").value("DEV-DOOR-001"))
                .andExpect(jsonPath("$[0].abacResult").value("PASS"));
    }

    @Test
    @DisplayName("GET /api/access/requests/{id} returns specific audit log details")
    void testGetAccessRequestLogById() throws Exception {
        String payload = """
                {
                    "deviceIdentifier": "DEV-DOOR-001",
                    "userId": "Guest-001",
                    "role": "GUEST",
                    "resource": "SMART_DOOR_LOCK",
                    "operation": "CONTROL",
                    "location": "Property-001",
                    "bookingId": "BOOK-001"
                }
                """;

        mockMvc.perform(post("/api/access/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Extract ID from response
        Long requestId = accessRequestRepository.findAll().getFirst().getId();

        mockMvc.perform(get("/api/access/requests/" + requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requestId))
                .andExpect(jsonPath("$.deviceIdentifier").value("DEV-DOOR-001"))
                .andExpect(jsonPath("$.abacResult").value("PASS"));
    }
}
