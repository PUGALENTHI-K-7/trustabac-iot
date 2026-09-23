package com.trustabac.iot.dto;

import com.trustabac.iot.entity.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for updating an existing ABAC Policy.
 */
public class PolicyUpdateRequest {

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Target resource must not exceed 100 characters")
    private String targetResource;

    private Operation targetOperation;

    private Boolean active;

    @Valid
    private List<PolicyConditionDto> conditions;

    public PolicyUpdateRequest() {
    }

    public PolicyUpdateRequest(String description, String targetResource, Operation targetOperation,
                               Boolean active, List<PolicyConditionDto> conditions) {
        this.description = description;
        this.targetResource = targetResource;
        this.targetOperation = targetOperation;
        this.active = active;
        this.conditions = conditions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetResource() {
        return targetResource;
    }

    public void setTargetResource(String targetResource) {
        this.targetResource = targetResource;
    }

    public Operation getTargetOperation() {
        return targetOperation;
    }

    public void setTargetOperation(Operation targetOperation) {
        this.targetOperation = targetOperation;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public List<PolicyConditionDto> getConditions() {
        return conditions;
    }

    public void setConditions(List<PolicyConditionDto> conditions) {
        this.conditions = conditions;
    }
}
