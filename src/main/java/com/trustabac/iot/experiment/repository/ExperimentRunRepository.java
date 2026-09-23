package com.trustabac.iot.experiment.repository;

import com.trustabac.iot.experiment.dto.ScenarioType;
import com.trustabac.iot.experiment.entity.ExperimentRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {
    Optional<ExperimentRun> findByRunId(String runId);
    List<ExperimentRun> findByScenarioTypeOrderByStartTimeDesc(ScenarioType scenarioType);
    List<ExperimentRun> findAllByOrderByStartTimeDesc();
}
