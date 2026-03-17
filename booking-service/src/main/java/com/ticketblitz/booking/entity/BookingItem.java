package com.ticketblitz.booking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * BookingItem: Individual seat in a booking
 *
 * - BookingItem is owned by Booking (aggregate root)
 * - Cannot exist without a parent booking
 */
@Entity
@Table(name = "booking_items")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BookingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    // seat info (denormalized for catalog service)
    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Column(nullable = false, length = 50)
    private String section;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private Integer seatNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    @PositiveOrZero
    private BigDecimal price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}