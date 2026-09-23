package com.trustabac.iot.batch.repository;

import com.trustabac.iot.batch.entity.SecurityAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SecurityAnalyticsRepository extends JpaRepository<SecurityAnalytics, Long> {

    Optional<SecurityAnalytics> findByPeriodKey(String periodKey);

    Optional<SecurityAnalytics> findByBatchRunAudit_Id(Long batchRunId);
}
