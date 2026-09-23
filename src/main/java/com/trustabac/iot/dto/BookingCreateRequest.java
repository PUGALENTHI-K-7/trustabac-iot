package com.trustabac.iot.dto;

import com.trustabac.iot.entity.BookingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a smart-rental booking for ABAC context evaluation.
 */
public class BookingCreateRequest {

    @NotBlank(message = "Booking reference is required")
    @Size(max = 100, message = "Booking reference must not exceed 100 characters")
    private String bookingReference;

    @NotBlank(message = "Property ID is required")
    @Size(max = 100, message = "Property ID must not exceed 100 characters")
    private String propertyId;

    @NotBlank(message = "Guest user ID is required")
    @Size(max = 100, message = "Guest user ID must not exceed 100 characters")
    private String guestUserId;

    @NotNull(message = "Valid from timestamp is required")
    private LocalDateTime validFrom;

    @NotNull(message = "Valid until timestamp is required")
    private LocalDateTime validUntil;

    private BookingStatus bookingStatus;

    public BookingCreateRequest() {
    }

    public BookingCreateRequest(String bookingReference, String propertyId, String guestUserId,
                                LocalDateTime validFrom, LocalDateTime validUntil, BookingStatus bookingStatus) {
        this.bookingReference = bookingReference;
        this.propertyId = propertyId;
        this.guestUserId = guestUserId;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.bookingStatus = bookingStatus;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getGuestUserId() {
        return guestUserId;
    }

    public void setGuestUserId(String guestUserId) {
        this.guestUserId = guestUserId;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime validUntil) {
        this.validUntil = validUntil;
    }

    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }
}
