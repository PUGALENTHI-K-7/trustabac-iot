package com.trustabac.iot.experiment.repository;

import com.trustabac.iot.experiment.entity.ExperimentMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExperimentMeasurementRepository extends JpaRepository<ExperimentMeasurement, Long> {
    Optional<ExperimentMeasurement> findByRunId(String runId);
    Optional<ExperimentMeasurement> findByExperimentRun_Id(Long experimentRunId);
}
