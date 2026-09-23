package com.trustabac.iot.service;

import com.trustabac.iot.dto.PolicyConditionDto;
import com.trustabac.iot.dto.PolicyCreateRequest;
import com.trustabac.iot.dto.PolicyResponse;
import com.trustabac.iot.dto.PolicyUpdateRequest;
import com.trustabac.iot.entity.AttributeCategory;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.PolicyCondition;
import com.trustabac.iot.entity.PolicyOperator;
import com.trustabac.iot.exception.DuplicateResourceException;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.PolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyRepository policyRepository;

    @InjectMocks
    private PolicyService policyService;

    private Policy policy;

    @BeforeEach
    void setUp() {
        policy = new Policy("Door Policy", "Description", "SMART_DOOR_LOCK", Operation.CONTROL, true);
        policy.setId(1L);
        policy.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST"));
    }

    @Test
    @DisplayName("Create policy: successfully creates and saves policy with conditions")
    void testCreatePolicySuccess() {
        PolicyCreateRequest request = new PolicyCreateRequest(
                "Door Policy", "Description", "SMART_DOOR_LOCK", Operation.CONTROL, true,
                List.of(new PolicyConditionDto(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST"))
        );

        when(policyRepository.existsByName("Door Policy")).thenReturn(false);
        when(policyRepository.save(any(Policy.class))).thenReturn(policy);

        PolicyResponse response = policyService.createPolicy(request);

        assertNotNull(response);
        assertEquals("Door Policy", response.getName());
        assertEquals("SMART_DOOR_LOCK", response.getTargetResource());
        assertEquals(1, response.getConditions().size());
    }

    @Test
    @DisplayName("Create policy: throws DuplicateResourceException on duplicate name")
    void testCreatePolicyDuplicateThrows() {
        PolicyCreateRequest request = new PolicyCreateRequest(
                "Door Policy", "Description", "SMART_DOOR_LOCK", Operation.CONTROL, true,
                List.of(new PolicyConditionDto(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST"))
        );

        when(policyRepository.existsByName("Door Policy")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> policyService.createPolicy(request));
    }

    @Test
    @DisplayName("getPolicyById: returns policy or throws ResourceNotFoundException")
    void testGetPolicyById() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        PolicyResponse response = policyService.getPolicyById(1L);
        assertEquals("Door Policy", response.getName());

        assertThrows(ResourceNotFoundException.class, () -> policyService.getPolicyById(99L));
    }

    @Test
    @DisplayName("getAllPolicies: returns list of policies")
    void testGetAllPolicies() {
        when(policyRepository.findAll()).thenReturn(List.of(policy));

        List<PolicyResponse> responses = policyService.getAllPolicies();
        assertEquals(1, responses.size());
    }

    @Test
    @DisplayName("updatePolicy: updates policy fields and conditions")
    void testUpdatePolicy() {
        PolicyUpdateRequest updateReq = new PolicyUpdateRequest(
                "New description", "GUEST_WIFI", Operation.READ, true,
                List.of(new PolicyConditionDto(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST"))
        );

        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.save(any(Policy.class))).thenReturn(policy);

        PolicyResponse response = policyService.updatePolicy(1L, updateReq);

        assertNotNull(response);
        verify(policyRepository).save(policy);
    }

    @Test
    @DisplayName("deletePolicy: deletes policy or throws ResourceNotFoundException")
    void testDeletePolicy() {
        when(policyRepository.findById(1L)).thenReturn(Optional.of(policy));
        when(policyRepository.findById(99L)).thenReturn(Optional.empty());

        policyService.deletePolicy(1L);
        verify(policyRepository).delete(policy);

        assertThrows(ResourceNotFoundException.class, () -> policyService.deletePolicy(99L));
    }
}
