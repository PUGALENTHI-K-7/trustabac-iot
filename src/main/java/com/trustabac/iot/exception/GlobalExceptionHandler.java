package com.trustabac.iot.exception;

import com.trustabac.iot.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * Lightweight global exception handler providing structured JSON error
 * responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex, HttpServletRequest request) {
                log.warn("Resource not found on path {}: {}", request.getRequestURI(), ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ErrorResponse> handleDuplicateResource(
                        DuplicateResourceException ex, HttpServletRequest request) {
                log.warn("Duplicate resource on path {}: {}", request.getRequestURI(), ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.CONFLICT.value(),
                                HttpStatus.CONFLICT.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationException(
                        MethodArgumentNotValidException ex, HttpServletRequest request) {
                String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                                .collect(Collectors.joining(", "));

                log.warn("Validation error on path {}: {}", request.getRequestURI(), validationErrors);
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                validationErrors,
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleMessageNotReadable(
                        HttpMessageNotReadableException ex, HttpServletRequest request) {
                log.warn("Malformed JSON or unreadable request body on path {}: {}", request.getRequestURI(),
                                ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                "Malformed request body or invalid value provided",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
                log.warn("Parameter type mismatch on path {}: {}", request.getRequestURI(), ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                "Invalid parameter type for '" + ex.getName() + "'",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
                log.warn("Method not supported on path {}: {}", request.getRequestURI(), ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.METHOD_NOT_ALLOWED.value(),
                                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex, HttpServletRequest request) {
                log.warn("Illegal argument on path {}: {}", request.getRequestURI(), ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex, HttpServletRequest request) {
                log.error("Unhandled exception processing request to {}: ", request.getRequestURI(), ex);
                ErrorResponse errorResponse = ErrorResponse.of(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                "An unexpected internal error occurred",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
}
