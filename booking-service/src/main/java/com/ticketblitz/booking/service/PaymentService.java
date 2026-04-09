package com.ticketblitz.booking.service;

import com.ticketblitz.booking.config.BookingMetrics;
import com.ticketblitz.booking.dto.PaymentDto;
import com.ticketblitz.booking.dto.PaymentRequest;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingItem;
import com.ticketblitz.booking.entity.Payment;
import com.ticketblitz.booking.mapper.PaymentMapper;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.booking.repository.PaymentRepository;
import com.ticketblitz.common.constant.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
* Payment Service
 *
 * PURPOSE:
 * ========
 * Process payments for bookings
 * Integrates with payment gateway (mock for now)
 *
 * PAYMENT FLOW:
 * =============
 * 1. Validate booking exists and is PENDING
 * 2. Create payment record (PENDING)
 * 3. Call payment gateway (mock delay)
 * 4. Update payment status (SUCCESS/FAILED)
 * 5. Update booking status (CONFIRMED/FAILED)
 * 6. Update seats in catalog (LOCKED → BOOKED)
 * 7. Publish booking confirmed event (RabbitMQ)
 *
 * CONCURRENCY HANDLING:
 * =====================
 * - Uses pessimistic locking (findByIdWithLock)
 * - Prevents duplicate payment processing
 * - Transaction ensures atomic state changes
 *
 * IDEMPOTENCY:
 * ============
 * Payment is idempotent via booking's idempotency_key
 * Multiple payment attempts return same result
 *
 * MOCK PAYMENT GATEWAY:
 * =====================
 * Simulates:
 * - 2 second processing delay
 * - 95% success rate (5% random failures)
 * - Transaction ID generation
 *
 * PRODUCTION CONSIDERATIONS:
 * ==========================
 * 1. External API calls should be async
 * 2. Implement webhook for payment confirmation
 * 3. Add retry mechanism for failed payments
 * 4. Store encrypted payment details
 * 5. PCI compliance for card data
 *
 * STAFF ENGINEER PATTERN:
 * =======================
 * Separate payment logic from booking logic
 * Payment is a bounded context in DDD terms
 * Can be extracted to separate microservice later
 *
 * @author Akhil
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final SeatLockingService seatLockingService;
    private final PaymentMapper paymentMapper;
    private final DistributedLockService lockService;
    private final BookingEventPublisher eventPublisher;
    private final BookingMetrics metrics;

    @Value("${booking.payment.mock-enabled:true}")
    private boolean mockEnabled;

    @Value("${booking.payment.processing-delay-ms:2000}")
    private int processingDelayMs;

    /**
     * Process payment for booking
     *
     * TRANSACTION: Updates booking and payment atomically
     * LOCKING: Uses distributed lock to prevent duplicate processing
     */
    @Transactional
    public PaymentDto processPayment(Long bookingId, PaymentRequest request) {
        log.info("Processing payment for booking: {}", bookingId);

        // Use distributed lock to prevent duplicate payment processing
        return lockService.executeWithLock(
                buildPaymentLockKey(bookingId),
                () -> {
                    io.micrometer.core.instrument.Timer.Sample timerSample = metrics.startPaymentTimer();
                    try {
                        return doProcessPayment(bookingId, request);
                    } finally {
                        metrics.stopPaymentTimer(timerSample);
                    }
                }
        );
    }

    /**
     * Core payment processing logic
     */
    private PaymentDto doProcessPayment(Long bookingId, PaymentRequest request) {
        // Lock booking for update
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + bookingId
                ));

        // Validate booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException(
                    "Booking is not in PENDING status: " + booking.getStatus()
            );
        }

        // Check if payment already exists
        Payment existingPayment = paymentRepository.findByBookingId(bookingId)
                .orElse(null);

        if (existingPayment != null &&
                existingPayment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            log.info("Payment already processed for booking: {}", bookingId);
            return paymentMapper.toDto(existingPayment);
        }

        // Check if booking expired
        if (booking.isExpired()) {
            throw new IllegalStateException(
                    "Booking has expired. Cannot process payment."
            );
        }

        // Create payment record
        Payment payment = Payment.builder()
                .booking(booking)
                .amount(booking.getAmount())
                .currency("USD")
                .paymentMethod(request.getPaymentMethod())
                .paymentGateway("MOCK")
                .status(Payment.PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        // Update booking to RESERVED (payment processing)
        booking.setStatus(BookingStatus.RESERVED);
        bookingRepository.save(booking);

        // Process payment (mock gateway)
        boolean paymentSuccess = processPaymentGateway(payment, request);

        if (paymentSuccess) {
            // Payment succeeded
            String transactionId = UUID.randomUUID().toString();
            payment.markSuccess(transactionId);
            booking.confirm();

            log.info("Payment successful for booking: {}, transaction: {}",
                    bookingId, transactionId);

            metrics.incrementPaymentsSucceeded();

            // Update seats in catalog (LOCKED → BOOKED)
            List<Long> seatIds = booking.getItems().stream()
                    .map(BookingItem::getSeatId)
                    .collect(Collectors.toList());

            seatLockingService.bookSeatsInCatalog(
                    booking.getEventId(),
                    seatIds,
                    bookingId
            );

            // Publish booking confirmed event to RabbitMQ → fulfillment-service
            eventPublisher.publishBookingConfirmed(booking);

        } else {
            // Payment failed
            payment.markFailed("Payment declined by gateway");
            booking.fail();

            log.warn("Payment failed for booking: {}", bookingId);

            metrics.incrementPaymentsFailed();

            // Release seats
            List<Long> seatIds = booking.getItems().stream()
                    .map(BookingItem::getSeatId)
                    .collect(Collectors.toList());

            seatLockingService.releaseSeatsInCatalog(
                    booking.getEventId(),
                    seatIds
            );
        }

        // Save final state
        paymentRepository.save(payment);
        bookingRepository.save(booking);

        return paymentMapper.toDto(payment);
    }

    /**
     * Mock payment gateway processing
     *
     * SIMULATION:
     * - 2 second delay (configurable)
     * - 95% success rate
     * - Random transaction IDs
     *
     * PRODUCTION: Replace with real gateway API call
     */
    private boolean processPaymentGateway(Payment payment, PaymentRequest request) {
        if (!mockEnabled) {
            throw new UnsupportedOperationException(
                    "Real payment gateway not implemented yet"
            );
        }

        log.info("Calling mock payment gateway...");

        // Simulating processing delay
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted", e);
            return false;
        }

        // Simulate 95% success rate
        double random = Math.random();
        boolean success = random > 0.05;

        log.info("Mock payment gateway response: {}", success ? "SUCCESS" : "FAILED");

        return success;
    }

    /**
     * Get payment for booking
     */
    @Transactional(readOnly = true)
    public PaymentDto getPaymentForBooking(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No payment found for booking: " + bookingId
                ));

        return paymentMapper.toDto(payment);
    }


    // helper methods
    private String buildPaymentLockKey(Long bookingId) {
        return String.format("payment:booking:%d", bookingId);
    }
}