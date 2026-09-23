package com.trustabac.iot.dto;

import com.trustabac.iot.entity.AttributeCategory;
import com.trustabac.iot.entity.PolicyOperator;

import java.time.LocalDateTime;

/**
 * DTO representing an attribute condition within a policy response.
 */
public class PolicyConditionResponse {

    private Long id;
    private AttributeCategory attributeCategory;
    private String attributeKey;
    private PolicyOperator operator;
    private String expectedValue;
    private LocalDateTime createdAt;

    public PolicyConditionResponse() {
    }

    public PolicyConditionResponse(Long id, AttributeCategory attributeCategory, String attributeKey,
                                  PolicyOperator operator, String expectedValue, LocalDateTime createdAt) {
        this.id = id;
        this.attributeCategory = attributeCategory;
        this.attributeKey = attributeKey;
        this.operator = operator;
        this.expectedValue = expectedValue;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AttributeCategory getAttributeCategory() {
        return attributeCategory;
    }

    public void setAttributeCategory(AttributeCategory attributeCategory) {
        this.attributeCategory = attributeCategory;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public void setAttributeKey(String attributeKey) {
        this.attributeKey = attributeKey;
    }

    public PolicyOperator getOperator() {
        return operator;
    }

    public void setOperator(PolicyOperator operator) {
        this.operator = operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
