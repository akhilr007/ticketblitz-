package com.ticketblitz.booking.service;

import com.ticketblitz.booking.config.RabbitMQConfig;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingItem;
import com.ticketblitz.common.event.BookingConfirmedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Publishes domain events to RabbitMQ.
 *
 * This is the outbound side of the choreography-based saga:
 *   PaymentService confirms booking → this publisher fires
 *   → FulfillmentService picks up the event and generates tickets.
 *
 * DESIGN DECISION:
 * ================
 * Event is published AFTER the DB transaction commits (called at the
 * end of the transactional payment method). In a production system,
 * consider using the Transactional Outbox pattern to guarantee
 * exactly-once delivery.
 *
 * @author Akhil
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publish a booking-confirmed event with denormalized data
     * so the fulfillment service doesn't need to call back.
     */
    public void publishBookingConfirmed(Booking booking) {
        BookingConfirmedEvent event = buildEvent(booking);

        log.info("Publishing BookingConfirmedEvent for booking: {}", booking.getId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.BOOKING_CONFIRMED_ROUTING_KEY,
                event,
                message -> {
                    message.getMessageProperties()
                            .setCorrelationId(booking.getId().toString());

                    message.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                    return message;
                }
        );

        log.info("BookingConfirmedEvent published successfully for booking: {}", booking.getId());
    }

    private BookingConfirmedEvent buildEvent(Booking booking) {
        List<BookingConfirmedEvent.SeatInfo> seatInfos = booking.getItems().stream()
                .map(this::toSeatInfo)
                .toList();

        return BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .userId(booking.getUserId())
                .eventId(booking.getEventId())
                .eventName(booking.getEventName())
                .venueName(booking.getVenueName())
                .eventDate(booking.getEventDate())
                .totalAmount(booking.getAmount())
                .totalSeats(booking.getTotalSeats())
                .confirmedAt(booking.getConfirmedAt())
                .seats(seatInfos)
                .build();
    }

    private BookingConfirmedEvent.SeatInfo toSeatInfo(BookingItem item) {
        return BookingConfirmedEvent.SeatInfo.builder()
                .seatId(item.getSeatId())
                .section(item.getSection())
                .rowLabel(item.getRowLabel())
                .seatNumber(item.getSeatNumber())
                .price(item.getPrice())
                .build();
    }

    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(((correlationData, ack, cause) -> {
            if (ack) {
                log.info("Message delivered to exchange successfully");
            }
            else {
                log.error("Message delivery FAILED: {}", cause);
            }
        }));

        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Message returned: {}", returned.getMessage());
        });
    }
}