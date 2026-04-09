package com.ticketblitz.booking.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Booking Service.
 *
 * Exposes Micrometer counters and timers for:
 * - Bookings created (by event)
 * - Bookings cancelled
 * - Payments processed (by status: success/failed)
 * - Payment processing duration
 * - Seats locked via distributed lock
 *
 * All metrics are prefixed with "ticketblitz." for easy Grafana filtering.
 *
 * @author TicketBlitz Observability
 */
@Component
public class BookingMetrics {

    private final MeterRegistry registry;

    // Counters
    private final Counter bookingsCreated;
    private final Counter bookingsCancelled;
    private final Counter paymentsSucceeded;
    private final Counter paymentsFailed;
    private final Counter seatsLocked;

    // Timers
    private final Timer paymentDuration;

    public BookingMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.bookingsCreated = Counter.builder("ticketblitz.bookings.created")
                .description("Total bookings created")
                .register(registry);

        this.bookingsCancelled = Counter.builder("ticketblitz.bookings.cancelled")
                .description("Total bookings cancelled")
                .register(registry);

        this.paymentsSucceeded = Counter.builder("ticketblitz.payments.processed")
                .tag("status", "success")
                .description("Payments processed successfully")
                .register(registry);

        this.paymentsFailed = Counter.builder("ticketblitz.payments.processed")
                .tag("status", "failed")
                .description("Payments that failed")
                .register(registry);

        this.seatsLocked = Counter.builder("ticketblitz.seats.locked")
                .description("Total seats locked for bookings")
                .register(registry);

        this.paymentDuration = Timer.builder("ticketblitz.payments.duration")
                .description("Payment processing duration")
                .register(registry);
    }

    public void incrementBookingsCreated() {
        bookingsCreated.increment();
    }

    public void incrementBookingsCancelled() {
        bookingsCancelled.increment();
    }

    public void incrementPaymentsSucceeded() {
        paymentsSucceeded.increment();
    }

    public void incrementPaymentsFailed() {
        paymentsFailed.increment();
    }

    public void incrementSeatsLocked(int count) {
        seatsLocked.increment(count);
    }

    public Timer.Sample startPaymentTimer() {
        return Timer.start(registry);
    }

    public void stopPaymentTimer(Timer.Sample sample) {
        sample.stop(paymentDuration);
    }
}
