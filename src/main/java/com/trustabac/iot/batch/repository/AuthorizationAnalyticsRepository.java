package com.trustabac.iot.batch.repository;

import com.trustabac.iot.batch.entity.AuthorizationAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorizationAnalyticsRepository extends JpaRepository<AuthorizationAnalytics, Long> {

    Optional<AuthorizationAnalytics> findByPeriodKey(String periodKey);

    Optional<AuthorizationAnalytics> findByBatchRunAudit_Id(Long batchRunId);
}
