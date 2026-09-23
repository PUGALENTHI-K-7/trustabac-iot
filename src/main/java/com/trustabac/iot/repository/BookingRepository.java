package com.trustabac.iot.repository;

import com.trustabac.iot.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Booking entity management.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    boolean existsByBookingReference(String bookingReference);

    List<Booking> findByGuestUserId(String guestUserId);

    List<Booking> findByPropertyId(String propertyId);
}
