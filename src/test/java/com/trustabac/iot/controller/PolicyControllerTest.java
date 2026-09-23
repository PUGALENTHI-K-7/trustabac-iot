package com.trustabac.iot.controller;

import com.trustabac.iot.entity.AttributeCategory;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.PolicyCondition;
import com.trustabac.iot.entity.PolicyOperator;
import com.trustabac.iot.repository.PolicyRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PolicyRepository policyRepository;

    @BeforeEach
    void setUp() {
        policyRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/policies creates a new policy with conditions")
    void testCreatePolicySuccess() throws Exception {
        String payload = """
                {
                    "name": "Guest Door Control",
                    "description": "Allows guests to control smart door locks",
                    "targetResource": "SMART_DOOR_LOCK",
                    "targetOperation": "CONTROL",
                    "active": true,
                    "conditions": [
                        {
                            "attributeCategory": "SUBJECT",
                            "attributeKey": "subject.role",
                            "operator": "EQUALS",
                            "expectedValue": "GUEST"
                        },
                        {
                            "attributeCategory": "DEVICE",
                            "attributeKey": "device.active",
                            "operator": "EQUALS",
                            "expectedValue": "true"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Guest Door Control"))
                .andExpect(jsonPath("$.targetResource").value("SMART_DOOR_LOCK"))
                .andExpect(jsonPath("$.targetOperation").value("CONTROL"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.conditions", hasSize(2)));

        assertEquals(1, policyRepository.count());
    }

    @Test
    @DisplayName("POST /api/policies with duplicate name returns 409 Conflict")
    void testCreatePolicyDuplicateNameConflict() throws Exception {
        Policy existing = new Policy("Unique Policy", "Desc", "SMART_DOOR_LOCK", Operation.CONTROL, true);
        existing.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST"));
        policyRepository.save(existing);

        String payload = """
                {
                    "name": "Unique Policy",
                    "description": "Another desc",
                    "targetResource": "SMART_DOOR_LOCK",
                    "conditions": [
                        {
                            "attributeCategory": "SUBJECT",
                            "attributeKey": "subject.role",
                            "operator": "EQUALS",
                            "expectedValue": "GUEST"
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/policies with invalid body returns 400 Bad Request")
    void testCreatePolicyValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "name": "",
                    "targetResource": "",
                    "conditions": []
                }
                """;

        mockMvc.perform(post("/api/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/policies returns list of all policies")
    void testGetAllPolicies() throws Exception {
        Policy p = new Policy("Policy 1", "Desc", "SMART_LIGHT", Operation.CONTROL, true);
        policyRepository.save(p);

        mockMvc.perform(get("/api/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Policy 1"));
    }

    @Test
    @DisplayName("GET /api/policies/{id} returns policy details")
    void testGetPolicyById() throws Exception {
        Policy p = new Policy("Target Policy", "Desc", "SMART_TV", Operation.READ, true);
        Policy saved = policyRepository.save(p);

        mockMvc.perform(get("/api/policies/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Target Policy"));
    }

    @Test
    @DisplayName("PUT /api/policies/{id} updates existing policy")
    void testUpdatePolicy() throws Exception {
        Policy p = new Policy("Initial Policy", "Initial Desc", "SMART_LIGHT", Operation.READ, true);
        Policy saved = policyRepository.save(p);

        String updatePayload = """
                {
                    "description": "Updated Desc",
                    "targetResource": "GUEST_WIFI",
                    "targetOperation": "READ",
                    "active": false,
                    "conditions": [
                        {
                            "attributeCategory": "SUBJECT",
                            "attributeKey": "subject.role",
                            "operator": "EQUALS",
                            "expectedValue": "OWNER"
                        }
                    ]
                }
                """;

        mockMvc.perform(put("/api/policies/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated Desc"))
                .andExpect(jsonPath("$.targetResource").value("GUEST_WIFI"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.conditions", hasSize(1)))
                .andExpect(jsonPath("$.conditions[0].expectedValue").value("OWNER"));
    }

    @Test
    @DisplayName("DELETE /api/policies/{id} deletes policy")
    void testDeletePolicy() throws Exception {
        Policy p = new Policy("To Delete", "Desc", "SMART_LIGHT", Operation.CONTROL, true);
        Policy saved = policyRepository.save(p);

        mockMvc.perform(delete("/api/policies/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertEquals(0, policyRepository.count());
    }
}
