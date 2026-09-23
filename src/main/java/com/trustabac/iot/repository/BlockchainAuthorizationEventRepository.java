package com.trustabac.iot.repository;

import com.trustabac.iot.entity.BlockchainAuthorizationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockchainAuthorizationEventRepository extends JpaRepository<BlockchainAuthorizationEvent, Long> {
    List<BlockchainAuthorizationEvent> findByDeviceIdentifierOrderByEvaluationTimestampDesc(String deviceIdentifier);
    List<BlockchainAuthorizationEvent> findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<BlockchainAuthorizationEvent> findAllByOrderByIdDesc();
}
