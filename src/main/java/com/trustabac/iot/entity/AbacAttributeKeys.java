package com.trustabac.iot.entity;

/**
 * Centralized constant definitions for canonical attribute keys evaluated by the ABAC engine.
 */
public final class AbacAttributeKeys {

    private AbacAttributeKeys() {
        // Private constructor to prevent instantiation
    }

    // Subject Attributes
    public static final String SUBJECT_USER_ID = "subject.userId";
    public static final String SUBJECT_ROLE = "subject.role";
    public static final String SUBJECT_ORGANIZATION = "subject.organization";
    public static final String SUBJECT_BOOKING_ID = "subject.bookingId";

    // Device Attributes
    public static final String DEVICE_IDENTIFIER = "device.identifier";
    public static final String DEVICE_TYPE = "device.type";
    public static final String DEVICE_CLASS = "device.class";
    public static final String DEVICE_REGISTRATION_STATUS = "device.registrationStatus";
    public static final String DEVICE_ACTIVE = "device.active";

    // Resource Attributes
    public static final String RESOURCE_IDENTIFIER = "resource.identifier";
    public static final String RESOURCE_TYPE = "resource.type";
    public static final String RESOURCE_SENSITIVITY = "resource.sensitivity";

    // Operation Attributes
    public static final String OPERATION = "operation";

    // Context & Booking Attributes
    public static final String CONTEXT_LOCATION = "context.location";
    public static final String CONTEXT_REQUEST_TIMESTAMP = "context.requestTimestamp";
    public static final String CONTEXT_NETWORK_CONTEXT = "context.networkContext";
    public static final String BOOKING_VALID = "booking.valid";
    public static final String BOOKING_STATUS = "booking.status";
    public static final String BOOKING_PROPERTY_ID = "booking.propertyId";
    public static final String BOOKING_GUEST_USER_ID = "booking.guestUserId";
}
