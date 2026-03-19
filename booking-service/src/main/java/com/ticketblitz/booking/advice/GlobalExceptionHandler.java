package com.ticketblitz.booking.advice;

import com.ticketblitz.booking.service.DistributedLockService;
import com.ticketblitz.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle lock acquisition failures (concurrent booking)
     */
    @ExceptionHandler(DistributedLockService.LockAcquisitionException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockAcquisitionException(
            DistributedLockService.LockAcquisitionException ex) {

        log.error("Lock acquisition failed: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.CONFLICT.value()),
                        "Seats are currently being booked. Please try again in a moment."
                ));
    }

    /**
     * Handle optimistic locking failures (concurrent updates)
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(
            ObjectOptimisticLockingFailureException ex) {

        log.error("Optimistic lock failure: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.CONFLICT.value()),
                        "This booking was modified by another transaction. Please try again."
                ));
    }

    /**
     * Handle illegal argument exceptions (validation)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.error("Invalid argument: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.BAD_REQUEST.value()),ex.getMessage()));
    }

    /**
     * Handle illegal state exceptions (business rules)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex) {

        log.error("Invalid state: {}", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(String.valueOf(HttpStatus.CONFLICT.value()),
                        ex.getMessage()));
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex) {

        log.error("Unexpected error occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                        "An unexpected error occurred. Please try again later."
                ));
    }
}