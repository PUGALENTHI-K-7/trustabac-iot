package com.trustabac.iot.repository;

import com.trustabac.iot.entity.PolicyCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for PolicyCondition entity management.
 */
@Repository
public interface PolicyConditionRepository extends JpaRepository<PolicyCondition, Long> {

    List<PolicyCondition> findByPolicyId(Long policyId);
}
