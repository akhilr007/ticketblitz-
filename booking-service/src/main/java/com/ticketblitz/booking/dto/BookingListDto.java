package com.ticketblitz.booking.dto;

import com.ticketblitz.common.constant.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingListDto implements Serializable {

    private Long id;
    private String eventName;
    private String venueName;
    private LocalDateTime eventDate;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private Integer totalSeats;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
}