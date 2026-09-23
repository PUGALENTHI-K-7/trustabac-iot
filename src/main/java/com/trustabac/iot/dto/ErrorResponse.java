package com.trustabac.iot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

/**
 * Standard structured error response DTO for API exceptions.
 *
 * @param timestamp the timestamp when the error occurred
 * @param status the HTTP status code
 * @param error the HTTP error reason phrase
 * @param message detailed message describing the error
 * @param path the request URI path that produced the error
 */
public record ErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path);
    }
}
