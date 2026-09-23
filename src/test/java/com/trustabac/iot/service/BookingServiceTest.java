package com.trustabac.iot.service;

import com.trustabac.iot.dto.BookingCreateRequest;
import com.trustabac.iot.dto.BookingResponse;
import com.trustabac.iot.entity.Booking;
import com.trustabac.iot.entity.BookingStatus;
import com.trustabac.iot.exception.DuplicateResourceException;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private LocalDateTime now;
    private Booking booking;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.of(2026, 9, 20, 12, 0, 0);
        booking = new Booking("BOOK-001", "Property-001", "Guest-001",
                now.minusDays(1), now.plusDays(2), BookingStatus.CONFIRMED);
        booking.setId(1L);
    }

    @Test
    @DisplayName("Create booking: successfully persists valid booking")
    void testCreateBookingSuccess() {
        BookingCreateRequest request = new BookingCreateRequest(
                "BOOK-001", "Property-001", "Guest-001",
                now.minusDays(1), now.plusDays(2), BookingStatus.CONFIRMED
        );

        when(bookingRepository.existsByBookingReference("BOOK-001")).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals("BOOK-001", response.getBookingReference());
        assertEquals("Property-001", response.getPropertyId());
        assertEquals("Guest-001", response.getGuestUserId());
    }

    @Test
    @DisplayName("Create booking: throws DuplicateResourceException on duplicate reference")
    void testCreateBookingDuplicateThrows() {
        BookingCreateRequest request = new BookingCreateRequest(
                "BOOK-001", "Property-001", "Guest-001",
                now.minusDays(1), now.plusDays(2), BookingStatus.CONFIRMED
        );

        when(bookingRepository.existsByBookingReference("BOOK-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("Create booking: throws IllegalArgumentException when validFrom is after validUntil")
    void testCreateBookingInvalidTimeRangeThrows() {
        BookingCreateRequest request = new BookingCreateRequest(
                "BOOK-001", "Property-001", "Guest-001",
                now.plusDays(2), now.minusDays(1), BookingStatus.CONFIRMED
        );

        when(bookingRepository.existsByBookingReference("BOOK-001")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(request));
    }

    @Test
    @DisplayName("getBookingById: returns booking response or throws ResourceNotFoundException")
    void testGetBookingById() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        BookingResponse response = bookingService.getBookingById(1L);
        assertEquals("BOOK-001", response.getBookingReference());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.getBookingById(99L));
    }

    @Test
    @DisplayName("getAllBookings: returns list of bookings")
    void testGetAllBookings() {
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<BookingResponse> list = bookingService.getAllBookings();
        assertEquals(1, list.size());
    }

    @Test
    @DisplayName("isBookingValid: validates status, time window, and property location")
    void testIsBookingValid() {
        // Valid case
        assertTrue(bookingService.isBookingValid(booking, now, "Property-001"));
        assertTrue(bookingService.isBookingValid(booking, now, null)); // location omitted

        // Invalid time: before validFrom
        assertFalse(bookingService.isBookingValid(booking, now.minusDays(2), "Property-001"));

        // Invalid time: after validUntil
        assertFalse(bookingService.isBookingValid(booking, now.plusDays(3), "Property-001"));

        // Invalid property location
        assertFalse(bookingService.isBookingValid(booking, now, "Property-999"));

        // Invalid status: CANCELLED
        booking.setBookingStatus(BookingStatus.CANCELLED);
        assertFalse(bookingService.isBookingValid(booking, now, "Property-001"));
    }
}
