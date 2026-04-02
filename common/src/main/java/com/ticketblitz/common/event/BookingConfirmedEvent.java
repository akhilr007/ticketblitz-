package com.ticketblitz.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published by booking-service after successful payment.
 * Consumed by fulfillment-service to generate tickets.
 *
 * This is the choreography handoff in the booking saga:
 *   BookingService → (RabbitMQ) → FulfillmentService
 *
 * @author Akhil
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long bookingId;
    private String userId;
    private Long eventId;
    private String eventName;
    private String venueName;
    private LocalDateTime eventDate;
    private BigDecimal totalAmount;
    private Integer totalSeats;
    private LocalDateTime confirmedAt;

    private List<SeatInfo> seats;

    /**
     * Denormalized seat information so fulfillment-service
     * doesn't need to call back to catalog-service.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long seatId;
        private String section;
        private String rowLabel;
        private Integer seatNumber;
        private BigDecimal price;
    }
}