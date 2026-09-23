package com.trustabac.iot.dto;

/**
 * Data Transfer Object representing the health status of the application.
 *
 * @param status the current health status (e.g., "UP")
 */
public record HealthResponse(String status) {}
