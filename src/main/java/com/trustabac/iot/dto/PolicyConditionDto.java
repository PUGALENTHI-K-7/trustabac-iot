package com.trustabac.iot.dto;

import com.trustabac.iot.entity.AttributeCategory;
import com.trustabac.iot.entity.PolicyOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO representing an attribute condition specification when creating or updating a policy.
 */
public class PolicyConditionDto {

    @NotNull(message = "Attribute category is required")
    private AttributeCategory attributeCategory;

    @NotBlank(message = "Attribute key is required")
    private String attributeKey;

    @NotNull(message = "Operator is required")
    private PolicyOperator operator;

    @NotBlank(message = "Expected value is required")
    private String expectedValue;

    public PolicyConditionDto() {
    }

    public PolicyConditionDto(AttributeCategory attributeCategory, String attributeKey,
                              PolicyOperator operator, String expectedValue) {
        this.attributeCategory = attributeCategory;
        this.attributeKey = attributeKey;
        this.operator = operator;
        this.expectedValue = expectedValue;
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
}
