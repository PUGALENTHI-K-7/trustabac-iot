package com.trustabac.iot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing an Attribute-Based Access Control (ABAC) Policy.
 */
@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "target_resource", nullable = false, length = 100)
    private String targetResource;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_operation", length = 30)
    private Operation targetOperation;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<PolicyCondition> conditions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Policy() {
    }

    public Policy(String name, String description, String targetResource, Operation targetOperation, Boolean active) {
        this.name = name;
        this.description = description;
        this.targetResource = targetResource;
        this.targetOperation = targetOperation;
        this.active = active;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addCondition(PolicyCondition condition) {
        conditions.add(condition);
        condition.setPolicy(this);
    }

    public void removeCondition(PolicyCondition condition) {
        conditions.remove(condition);
        condition.setPolicy(null);
    }

    // Getters and Setters

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

    public List<PolicyCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<PolicyCondition> conditions) {
        this.conditions.clear();
        if (conditions != null) {
            for (PolicyCondition condition : conditions) {
                addCondition(condition);
            }
        }
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
