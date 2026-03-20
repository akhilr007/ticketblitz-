package com.ticketblitz.catalog.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatOperationRequest implements Serializable {

    @NotEmpty(message = "At least one seat ID is required")
    private List<Long> seatIds;
}
