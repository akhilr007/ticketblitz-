package com.ticketblitz.booking.dto;

import com.ticketblitz.common.constant.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Booking response dto
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingDto {

    private Long id;
    private String userId;
    private Long eventId;
    private String eventName;
    private String venueName;
    private LocalDateTime eventDate;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private Integer totalSeats;
    private List<BookingItemDto> items;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private Long secondsUntilExpiry;
    private Boolean isExpired;
}