package com.ticketblitz.fulfillment.messaging;

import com.ticketblitz.common.event.BookingConfirmedEvent;
import com.ticketblitz.fulfillment.config.RabbitMQConfig;
import com.ticketblitz.fulfillment.service.TicketGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RabbitMQ Consumer — listens for BookingConfirmedEvent messages.
 *
 * CONSUMER GUARANTEES:
 * ====================
 * 1. IDEMPOTENT: TicketGenerationService checks for existing tickets
 * 2. RETRY: Broker-level retry with TTL queue (3 attempts)
 * 3. DLQ: After max retries, message goes to dead-letter queue
 *
 * ERROR HANDLING:
 * ===============
 * - Business exceptions (validation): logged, message rejected → DLQ
 * - Transient exceptions (DB down): Spring retry kicks in
 * - After max retries: message moves to DLQ for manual inspection
 *
 * CONCURRENCY:
 * ============
 * Multiple concurrent consumers (configurable in application.yml).
 * Each message is processed by exactly one consumer instance.
 *
 * @author Akhil
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmedListener {

    private final TicketGenerationService ticketGenerationService;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CONFIRMED_QUEUE)
    public void handleBookingConfirmed(BookingConfirmedEvent event,
                                       Message message) {

        // check retry count from x-death
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        List<Map<String, Object>> xDeath = (List<Map<String, Object>>) headers.get("x-death");
        long retryCount = 0;
        if (xDeath != null && !xDeath.isEmpty()) {
            retryCount = (long) xDeath.get(0).get("count");
        }

        if (retryCount >= 3) {
            log.warn("Max retries reached for bookingId {}. Sending to DLQ", event.getBookingId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DLX_EXCHANGE,
                    RabbitMQConfig.BOOKING_DLQ_ROUTING_KEY,
                    event
            );
            return;
        }

        log.info("Received BookingConfirmedEvent: bookingId={}, userId={}, seats={}",
                event.getBookingId(), event.getUserId(), event.getTotalSeats());
        try {
            ticketGenerationService.generateTickets(event);
            log.info("Ticket generation completed for booking: {}", event.getBookingId());

        } catch (Exception e) {
            log.error("Failed to generate tickets for booking: {}. Error: {}",
                    event.getBookingId(), e.getMessage(), e);

            // Re-throw to trigger Spring retry / DLQ routing
            throw e;
        }
    }
}