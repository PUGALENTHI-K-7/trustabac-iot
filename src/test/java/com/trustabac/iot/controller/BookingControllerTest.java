package com.trustabac.iot.controller;

import com.trustabac.iot.entity.Booking;
import com.trustabac.iot.entity.BookingStatus;
import com.trustabac.iot.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/bookings creates a new booking successfully")
    void testCreateBookingSuccess() throws Exception {
        String payload = """
                {
                    "bookingReference": "BOOK-101",
                    "propertyId": "Property-001",
                    "guestUserId": "Guest-001",
                    "validFrom": "2026-09-20T10:00:00",
                    "validUntil": "2026-09-25T10:00:00",
                    "bookingStatus": "CONFIRMED"
                }
                """;

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.bookingReference").value("BOOK-101"))
                .andExpect(jsonPath("$.propertyId").value("Property-001"))
                .andExpect(jsonPath("$.guestUserId").value("Guest-001"))
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"));

        assertEquals(1, bookingRepository.count());
    }

    @Test
    @DisplayName("POST /api/bookings with duplicate reference returns 409 Conflict")
    void testCreateDuplicateBookingConflict() throws Exception {
        Booking b = new Booking("BOOK-101", "Property-001", "Guest-001",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), BookingStatus.CONFIRMED);
        bookingRepository.save(b);

        String payload = """
                {
                    "bookingReference": "BOOK-101",
                    "propertyId": "Property-001",
                    "guestUserId": "Guest-002",
                    "validFrom": "2026-09-20T10:00:00",
                    "validUntil": "2026-09-25T10:00:00"
                }
                """;

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/bookings returns list of bookings")
    void testGetAllBookings() throws Exception {
        Booking b = new Booking("BOOK-102", "Property-001", "Guest-001",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), BookingStatus.CONFIRMED);
        bookingRepository.save(b);

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].bookingReference").value("BOOK-102"));
    }

    @Test
    @DisplayName("GET /api/bookings/{id} returns booking details")
    void testGetBookingById() throws Exception {
        Booking b = new Booking("BOOK-103", "Property-001", "Guest-001",
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(2), BookingStatus.ACTIVE);
        Booking saved = bookingRepository.save(b);

        mockMvc.perform(get("/api/bookings/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingReference").value("BOOK-103"))
                .andExpect(jsonPath("$.bookingStatus").value("ACTIVE"));
    }
}
