package com.ticketblitz.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentDto implements Serializable {

    private Long id;
    private Long bookingId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String paymentGateway;
    private String gatewayTransactionId;
    private String status;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}