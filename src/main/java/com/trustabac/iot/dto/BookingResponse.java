package com.trustabac.iot.dto;

import com.trustabac.iot.entity.BookingStatus;

import java.time.LocalDateTime;

/**
 * Response DTO representing a smart-rental booking.
 */
public class BookingResponse {

    private Long id;
    private String bookingReference;
    private String propertyId;
    private String guestUserId;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private BookingStatus bookingStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookingResponse() {
    }

    public BookingResponse(Long id, String bookingReference, String propertyId, String guestUserId,
                           LocalDateTime validFrom, LocalDateTime validUntil, BookingStatus bookingStatus,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.bookingReference = bookingReference;
        this.propertyId = propertyId;
        this.guestUserId = guestUserId;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.bookingStatus = bookingStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
