package com.ticketblitz.booking.controller;

import com.ticketblitz.booking.dto.*;
import com.ticketblitz.booking.service.BookingService;
import com.ticketblitz.booking.service.PaymentService;
import com.ticketblitz.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name="Bookings", description = "Booking management API")
@SecurityRequirement(name="Bearer Authentication")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;

    // default pagination
    private static final int  DEFAULT_PAGE = 0;
    private static final int  PAGE_SIZE = 20;

    /**
     * create new booking
     *
     * POST: /api/v1/bookings
     *
     * CONCURRENCY: uses distributed locking
     * IDEMPOTENCY: requires idempotency key
     */
    @Operation(
            summary = "Create booking",
            description = "Reserve seats for an event. Requires idempotency key (uuid)."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<BookingDto>> createBooking(
            @Parameter(description = "User ID from JWT", hidden = true)
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateBookingRequest request) {

        log.info("POST /api/v1/bookings - User: {}, Event: {}",
                userId, request.getEventId());

        try {
            BookingDto booking = bookingService.createBooking(userId, request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(booking));
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.BAD_REQUEST.value()),e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.CONFLICT.value()),e.getMessage()));
        }
    }

    /**
     * Get booking by ID
     *
     * GET /api/v1/bookings/{id}
     */
    @Operation(summary = "Get booking details", description = "Retrieve booking by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDto>> getBooking(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        log.info("GET /api/v1/bookings/{} - User: {}", id, userId);

        try {
            BookingDto booking = bookingService.getBooking(id);

            // Verify ownership
            if (!booking.getUserId().equals(userId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                                String.valueOf(HttpStatus.FORBIDDEN.value()),
                                "Access denied"));
            }

            return ResponseEntity.ok(
                    ApiResponse.success(booking)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.NOT_FOUND.value()),
                            e.getMessage()));
        }
    }

    /**
     * Get user's bookings (paginated)
     *
     * GET /api/v1/bookings?page=0&size=20
     */
    @Operation(summary = "Get user's bookings", description = "List all bookings for current user")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingListDto>>> getUserBookings(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        log.info("GET /api/v1/bookings - User: {}, Page: {}", userId, page);

        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<BookingListDto> bookings = bookingService.getUserBookings(userId, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(bookings)
        );
    }

    /**
     * Cancel booking
     *
     * DELETE /api/v1/bookings/{id}
     */
    @Operation(summary = "Cancel booking", description = "Cancel a pending or reserved booking")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingDto>> cancelBooking(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        log.info("DELETE /api/v1/bookings/{} - User: {}", id, userId);

        try {
            BookingDto booking = bookingService.cancelBooking(id, userId);

            return ResponseEntity.ok(
                    ApiResponse.success(booking)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.BAD_REQUEST.value()),e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.CONFLICT.value()),e.getMessage()));
        }
    }

    /**
     * Process payment for booking
     *
     * POST /api/v1/bookings/{id}/pay
     *
     * CONCURRENCY: Uses distributed locking
     * TRANSACTION: Atomic booking + payment update
     */
    @Operation(summary = "Process payment", description = "Pay for a pending booking")
    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<PaymentDto>> processPayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody PaymentRequest request) {

        log.info("POST /api/v1/bookings/{}/pay - User: {}", id, userId);

        try {
            // Verify booking ownership first
            BookingDto booking = bookingService.getBooking(id);
            if (!booking.getUserId().equals(userId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                                String.valueOf(HttpStatus.FORBIDDEN.value()),
                                "Access denied"));
            }

            PaymentDto payment = paymentService.processPayment(id, request);

            return ResponseEntity.ok(
                    ApiResponse.success(payment)
            );

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.BAD_REQUEST.value()),e.getMessage()));
        }
    }

    /**
     * Get payment information for booking
     *
     * GET /api/v1/bookings/{id}/payment
     */
    @Operation(summary = "Get payment info", description = "Retrieve payment details for a booking")
    @GetMapping("/{id}/payment")
    public ResponseEntity<ApiResponse<PaymentDto>> getPayment(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {

        log.info("GET /api/v1/bookings/{}/payment - User: {}", id, userId);

        try {
            // Verify ownership
            BookingDto booking = bookingService.getBooking(id);
            if (!booking.getUserId().equals(userId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error(
                                String.valueOf(HttpStatus.FORBIDDEN.value()),
                                "Access denied"));
            }

            PaymentDto payment = paymentService.getPaymentForBooking(id);

            return ResponseEntity.ok(
                    ApiResponse.success(payment)
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(
                            String.valueOf(HttpStatus.NOT_FOUND.value()),e.getMessage()));
        }
    }
}