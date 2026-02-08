package com.ticketblitz.catalog.dto;

import com.ticketblitz.common.constant.SeatStatus;
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
public class SeatDto implements Serializable {

    private Long id;
    private String section;
    private String rowLabel;
    private Integer seatNumber;
    private BigDecimal price;
    private SeatStatus status;
}