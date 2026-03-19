package com.ticketblitz.booking.dto;

import java.math.BigDecimal;

public record BookingStatsDto(
        Long totalBookings,
        Long confirmedBookings,
        Long pendingBookings,
        Long cancelledBookings,
        BigDecimal totalRevenue
) {}