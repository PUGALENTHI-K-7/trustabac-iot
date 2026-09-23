package com.trustabac.iot.repository;

import com.trustabac.iot.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Policy entity management.
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByName(String name);

    boolean existsByName(String name);

    List<Policy> findByActiveTrue();
}
