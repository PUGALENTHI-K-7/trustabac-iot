package com.trustabac.iot.batch.repository;

import com.trustabac.iot.batch.entity.DeviceAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceAnalyticsRepository extends JpaRepository<DeviceAnalytics, Long> {

    List<DeviceAnalytics> findAllByPeriodKey(String periodKey);

    List<DeviceAnalytics> findAllByBatchRunAudit_Id(Long batchRunId);
}
