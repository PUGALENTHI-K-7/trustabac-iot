package com.trustabac.iot.service;

import com.trustabac.iot.dto.BookingCreateRequest;
import com.trustabac.iot.dto.BookingResponse;
import com.trustabac.iot.entity.Booking;
import com.trustabac.iot.entity.BookingStatus;
import com.trustabac.iot.exception.DuplicateResourceException;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing smart-rental booking records and evaluation-time validity.
 */
@Service
@Transactional
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    public BookingResponse createBooking(BookingCreateRequest request) {
        if (bookingRepository.existsByBookingReference(request.getBookingReference())) {
            throw new DuplicateResourceException("Booking with reference '" + request.getBookingReference() + "' already exists");
        }

        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new IllegalArgumentException("Booking validFrom timestamp must be before validUntil timestamp");
        }

        Booking booking = new Booking(
                request.getBookingReference(),
                request.getPropertyId(),
                request.getGuestUserId(),
                request.getValidFrom(),
                request.getValidUntil(),
                request.getBookingStatus() != null ? request.getBookingStatus() : BookingStatus.CONFIRMED
        );

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Created booking ID={} ref='{}' for property='{}' guest='{}'",
                savedBooking.getId(), savedBooking.getBookingReference(),
                savedBooking.getPropertyId(), savedBooking.getGuestUserId());
        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public Optional<Booking> findByBookingReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference);
    }

    /**
     * Evaluates whether a given booking is active and valid at a given evaluation timestamp and location.
     */
    public boolean isBookingValid(Booking booking, LocalDateTime evaluationTime, String location) {
        if (booking == null) {
            return false;
        }

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED && booking.getBookingStatus() != BookingStatus.ACTIVE) {
            log.debug("Booking '{}' is invalid due to status '{}'", booking.getBookingReference(), booking.getBookingStatus());
            return false;
        }

        if (evaluationTime.isBefore(booking.getValidFrom()) || evaluationTime.isAfter(booking.getValidUntil())) {
            log.debug("Booking '{}' is invalid: evaluation time {} is outside [{}, {}]",
                    booking.getBookingReference(), evaluationTime, booking.getValidFrom(), booking.getValidUntil());
            return false;
        }

        if (location != null && !location.isBlank() && !booking.getPropertyId().equalsIgnoreCase(location.trim())) {
            log.debug("Booking '{}' property '{}' does not match request location '{}'",
                    booking.getBookingReference(), booking.getPropertyId(), location);
            return false;
        }

        return true;
    }

    public BookingResponse mapToResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingReference(),
                booking.getPropertyId(),
                booking.getGuestUserId(),
                booking.getValidFrom(),
                booking.getValidUntil(),
                booking.getBookingStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
