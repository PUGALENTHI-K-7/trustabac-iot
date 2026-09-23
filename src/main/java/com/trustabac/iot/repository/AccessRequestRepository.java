package com.trustabac.iot.repository;

import com.trustabac.iot.entity.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for AccessRequest persistent audit trail management.
 */
@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    List<AccessRequest> findByDeviceIdentifierOrderByCreatedAtDesc(String deviceIdentifier);

    List<AccessRequest> findByUserIdOrderByCreatedAtDesc(String userId);

    List<AccessRequest> findByRequestTimestampBetweenOrderByRequestTimestampAsc(java.time.LocalDateTime start, java.time.LocalDateTime end);

    List<AccessRequest> findAllByOrderByCreatedAtDesc();
}
