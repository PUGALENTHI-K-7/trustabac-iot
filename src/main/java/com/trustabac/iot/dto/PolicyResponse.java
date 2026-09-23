package com.trustabac.iot.dto;

import com.trustabac.iot.entity.Operation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO representing an ABAC Policy and its conditions.
 */
public class PolicyResponse {

    private Long id;
    private String name;
    private String description;
    private String targetResource;
    private Operation targetOperation;
    private Boolean active;
    private List<PolicyConditionResponse> conditions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PolicyResponse() {
    }

    public PolicyResponse(Long id, String name, String description, String targetResource,
                          Operation targetOperation, Boolean active, List<PolicyConditionResponse> conditions,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.targetResource = targetResource;
        this.targetOperation = targetOperation;
        this.active = active;
        this.conditions = conditions;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public List<PolicyConditionResponse> getConditions() {
        return conditions;
    }

    public void setConditions(List<PolicyConditionResponse> conditions) {
        this.conditions = conditions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
