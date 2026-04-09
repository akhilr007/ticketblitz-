package com.ticketblitz.fulfillment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the Fulfillment Service.
 *
 * Exposes Micrometer counters and timers for:
 * - Tickets generated (count)
 * - Ticket generation duration
 * - Ticket generation errors
 * - Duplicate events skipped (idempotency)
 *
 * All metrics are prefixed with "ticketblitz." for easy Grafana filtering.
 *
 * @author TicketBlitz Observability
 */
@Component
public class FulfillmentMetrics {

    private final MeterRegistry registry;

    // Counters
    private final Counter ticketsGenerated;
    private final Counter ticketGenerationErrors;
    private final Counter duplicateEventsSkipped;

    // Timers
    private final Timer ticketGenerationDuration;

    public FulfillmentMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.ticketsGenerated = Counter.builder("ticketblitz.tickets.generated")
                .description("Total tickets generated")
                .register(registry);

        this.ticketGenerationErrors = Counter.builder("ticketblitz.tickets.generation.errors")
                .description("Ticket generation errors")
                .register(registry);

        this.duplicateEventsSkipped = Counter.builder("ticketblitz.tickets.duplicates.skipped")
                .description("Duplicate booking events skipped due to idempotency")
                .register(registry);

        this.ticketGenerationDuration = Timer.builder("ticketblitz.tickets.generation.duration")
                .description("Ticket generation duration per booking")
                .register(registry);
    }

    public void incrementTicketsGenerated(int count) {
        ticketsGenerated.increment(count);
    }

    public void incrementGenerationErrors() {
        ticketGenerationErrors.increment();
    }

    public void incrementDuplicatesSkipped() {
        duplicateEventsSkipped.increment();
    }

    public Timer.Sample startGenerationTimer() {
        return Timer.start(registry);
    }

    public void stopGenerationTimer(Timer.Sample sample) {
        sample.stop(ticketGenerationDuration);
    }
}
