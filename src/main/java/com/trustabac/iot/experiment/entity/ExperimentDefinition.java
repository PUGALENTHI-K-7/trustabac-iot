package com.trustabac.iot.experiment.entity;

import com.trustabac.iot.experiment.dto.ScenarioType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity describing a defined research experiment scenario and its baseline parameters.
 */
@Entity
@Table(name = "experiment_definitions")
public class ExperimentDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_key", unique = true, nullable = false, length = 100)
    private String experimentKey;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false, length = 50)
    private ScenarioType scenarioType;

    @Column(name = "default_operations", nullable = false)
    private int defaultOperations = 10;

    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ExperimentDefinition() {
    }

    public ExperimentDefinition(String experimentKey, String name, String description, ScenarioType scenarioType, int defaultOperations, String parametersJson) {
        this.experimentKey = experimentKey;
        this.name = name;
        this.description = description;
        this.scenarioType = scenarioType;
        this.defaultOperations = defaultOperations;
        this.parametersJson = parametersJson;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExperimentKey() {
        return experimentKey;
    }

    public void setExperimentKey(String experimentKey) {
        this.experimentKey = experimentKey;
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

    public ScenarioType getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(ScenarioType scenarioType) {
        this.scenarioType = scenarioType;
    }

    public int getDefaultOperations() {
        return defaultOperations;
    }

    public void setDefaultOperations(int defaultOperations) {
        this.defaultOperations = defaultOperations;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public void setParametersJson(String parametersJson) {
        this.parametersJson = parametersJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
