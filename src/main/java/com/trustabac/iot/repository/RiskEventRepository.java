package com.trustabac.iot.repository;

import com.trustabac.iot.entity.RiskEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for append-only RiskEvent audit records.
 */
@Repository
public interface RiskEventRepository extends JpaRepository<RiskEvent, Long> {

    List<RiskEvent> findByDeviceIdentifierOrderByEvaluationTimestampDesc(String deviceIdentifier);

    List<RiskEvent> findByDeviceIdentifierOrderByCreatedAtDesc(String deviceIdentifier);

    List<RiskEvent> findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(java.time.LocalDateTime start, java.time.LocalDateTime end);

    Optional<RiskEvent> findFirstByDeviceIdentifierOrderByEvaluationTimestampDesc(String deviceIdentifier);
}
