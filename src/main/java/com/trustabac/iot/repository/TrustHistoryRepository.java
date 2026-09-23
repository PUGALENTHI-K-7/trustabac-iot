package com.trustabac.iot.repository;

import com.trustabac.iot.entity.TrustHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for TrustHistory append-only audit records.
 */
@Repository
public interface TrustHistoryRepository extends JpaRepository<TrustHistory, Long> {

    List<TrustHistory> findByDeviceIdentifierOrderByCreatedAtDesc(String deviceIdentifier);

    List<TrustHistory> findByDeviceIdOrderByCreatedAtDesc(Long deviceId);

    List<TrustHistory> findByEventTimestampBetweenOrderByEventTimestampAsc(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
