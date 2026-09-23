package com.trustabac.iot.repository;

import com.trustabac.iot.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Device entity management.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceIdentifier(String deviceIdentifier);

    boolean existsByDeviceIdentifier(String deviceIdentifier);

    boolean existsByDeviceIdentifierAndIdNot(String deviceIdentifier, Long id);
}
