package com.trustabac.iot.batch.repository;

import com.trustabac.iot.batch.entity.BatchRunAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRunAuditRepository extends JpaRepository<BatchRunAudit, Long> {

    Optional<BatchRunAudit> findByPeriodKey(String periodKey);

    Optional<BatchRunAudit> findTopByOrderByStartTimeDesc();

    List<BatchRunAudit> findAllByOrderByStartTimeDesc();

    boolean existsByPeriodKeyAndStatus(String periodKey, String status);
}
