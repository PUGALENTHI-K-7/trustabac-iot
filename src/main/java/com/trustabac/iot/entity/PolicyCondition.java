package com.trustabac.iot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a single condition predicate within an ABAC Policy.
 */
@Entity
@Table(name = "policy_conditions")
public class PolicyCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(name = "attribute_category", nullable = false, length = 30)
    private AttributeCategory attributeCategory;

    @Column(name = "attribute_key", nullable = false, length = 100)
    private String attributeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 30)
    private PolicyOperator operator;

    @Column(name = "expected_value", nullable = false, length = 255)
    private String expectedValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PolicyCondition() {
    }

    public PolicyCondition(AttributeCategory attributeCategory, String attributeKey,
                           PolicyOperator operator, String expectedValue) {
        this.attributeCategory = attributeCategory;
        this.attributeKey = attributeKey;
        this.operator = operator;
        this.expectedValue = expectedValue;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
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
