package com.trustabac.iot.experiment.repository;

import com.trustabac.iot.experiment.dto.ScenarioType;
import com.trustabac.iot.experiment.entity.ExperimentDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExperimentDefinitionRepository extends JpaRepository<ExperimentDefinition, Long> {
    Optional<ExperimentDefinition> findByExperimentKey(String experimentKey);
    Optional<ExperimentDefinition> findByScenarioType(ScenarioType scenarioType);
}
