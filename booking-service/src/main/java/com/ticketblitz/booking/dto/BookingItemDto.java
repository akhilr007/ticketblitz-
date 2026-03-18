package com.ticketblitz.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookingItemDto implements Serializable {

    private Long id;
    private Long seatId;
    private String section;
    private String rowLabel;
    private Integer seatNumber;
    private BigDecimal price;
}