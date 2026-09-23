package com.trustabac.iot.service;

import com.trustabac.iot.dto.PolicyConditionDto;
import com.trustabac.iot.dto.PolicyConditionResponse;
import com.trustabac.iot.dto.PolicyCreateRequest;
import com.trustabac.iot.dto.PolicyResponse;
import com.trustabac.iot.dto.PolicyUpdateRequest;
import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.PolicyCondition;
import com.trustabac.iot.exception.DuplicateResourceException;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing ABAC Policy definitions and normalized conditions.
 */
@Service
@Transactional
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository policyRepository;

    public PolicyService(PolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public PolicyResponse createPolicy(PolicyCreateRequest request) {
        if (policyRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Policy with name '" + request.getName() + "' already exists");
        }

        Policy policy = new Policy(
                request.getName(),
                request.getDescription(),
                request.getTargetResource(),
                request.getTargetOperation(),
                request.getActive() != null ? request.getActive() : true
        );

        if (request.getConditions() != null) {
            for (PolicyConditionDto condDto : request.getConditions()) {
                PolicyCondition condition = new PolicyCondition(
                        condDto.getAttributeCategory(),
                        condDto.getAttributeKey(),
                        condDto.getOperator(),
                        condDto.getExpectedValue()
                );
                policy.addCondition(condition);
            }
        }

        Policy savedPolicy = policyRepository.save(policy);
        log.info("Created ABAC policy ID={} name='{}' with {} conditions",
                savedPolicy.getId(), savedPolicy.getName(), savedPolicy.getConditions().size());
        return mapToResponse(savedPolicy);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicyById(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + id));
        return mapToResponse(policy);
    }

    public PolicyResponse updatePolicy(Long id, PolicyUpdateRequest request) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + id));

        if (request.getDescription() != null) {
            policy.setDescription(request.getDescription());
        }
        if (request.getTargetResource() != null) {
            policy.setTargetResource(request.getTargetResource());
        }
        if (request.getTargetOperation() != null) {
            policy.setTargetOperation(request.getTargetOperation());
        }
        if (request.getActive() != null) {
            policy.setActive(request.getActive());
        }

        if (request.getConditions() != null) {
            policy.getConditions().clear();
            for (PolicyConditionDto condDto : request.getConditions()) {
                PolicyCondition condition = new PolicyCondition(
                        condDto.getAttributeCategory(),
                        condDto.getAttributeKey(),
                        condDto.getOperator(),
                        condDto.getExpectedValue()
                );
                policy.addCondition(condition);
            }
        }

        Policy updatedPolicy = policyRepository.save(policy);
        log.info("Updated ABAC policy ID={} name='{}'", updatedPolicy.getId(), updatedPolicy.getName());
        return mapToResponse(updatedPolicy);
    }

    public void deletePolicy(Long id) {
        Policy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found with id: " + id));
        policyRepository.delete(policy);
        log.info("Deleted ABAC policy ID={} name='{}'", id, policy.getName());
    }

    public PolicyResponse mapToResponse(Policy policy) {
        List<PolicyConditionResponse> conditionResponses = policy.getConditions() == null ? List.of() :
                policy.getConditions().stream()
                        .map(c -> new PolicyConditionResponse(
                                c.getId(),
                                c.getAttributeCategory(),
                                c.getAttributeKey(),
                                c.getOperator(),
                                c.getExpectedValue(),
                                c.getCreatedAt()
                        ))
                        .collect(Collectors.toList());

        return new PolicyResponse(
                policy.getId(),
                policy.getName(),
                policy.getDescription(),
                policy.getTargetResource(),
                policy.getTargetOperation(),
                policy.getActive(),
                conditionResponses,
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
