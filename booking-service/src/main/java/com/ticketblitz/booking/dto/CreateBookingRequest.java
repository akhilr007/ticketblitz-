package com.ticketblitz.booking.dto;

import com.esotericsoftware.kryo.serializers.FieldSerializer;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateBookingRequest implements Serializable {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 10, message = "Maximum 10 seats per booking")
    private List<Long> seatIds;

    @NotNull(message = "Idempotency key is required")
    @Size(min = 36, max = 36, message = "Idempotency key must be UUID")
    private String idempotencyKey;
}