package com.ticketblitz.fulfillment.dto;

import com.ticketblitz.common.constant.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ticket response DTO.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketDto {

    private Long id;
    private Long bookingId;
    private String userId;
    private Long eventId;
    private String eventName;
    private String venueName;
    private LocalDateTime eventDate;

    // Seat info
    private Long seatId;
    private String section;
    private String rowLabel;
    private Integer seatNumber;
    private BigDecimal price;

    // Ticket identifiers
    private String ticketNumber;
    private String qrCode;

    // Status
    private TicketStatus status;
    private LocalDateTime generatedAt;
    private LocalDateTime deliveredAt;
}
