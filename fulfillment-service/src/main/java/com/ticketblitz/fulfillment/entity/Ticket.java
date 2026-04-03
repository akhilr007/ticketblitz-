package com.ticketblitz.fulfillment.entity;

import com.ticketblitz.common.constant.TicketStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ticket Entity — one ticket per seat in a confirmed booking.
 *
 * LIFECYCLE:
 * ==========
 * GENERATING → ticket row created, processing in progress
 * GENERATED  → ticket number and QR code assigned, ready for delivery
 * DELIVERED  → ticket sent to user (email/download)
 * CANCELLED  → ticket voided (e.g., refund scenario)
 *
 * IDEMPOTENCY:
 * =============
 * Unique constraint on (booking_id, seat_id) prevents duplicate
 * ticket generation from RabbitMQ message redelivery.
 *
 * @author Akhil
 */
@Entity
@Table(name = "tickets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ticket_booking_seat",
                columnNames = {"booking_id", "seat_id"}
        ))
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Cross-service references ───────────────────────────────────

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    // ── Event info (denormalized) ──────────────────────────────────

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "venue_name", nullable = false)
    private String venueName;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    // ── Seat info (denormalized) ───────────────────────────────────

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

    // ── Ticket identifiers ─────────────────────────────────────────

    @Column(name = "ticket_number", nullable = false, unique = true, length = 36)
    private String ticketNumber;

    @Column(name = "qr_code", length = 512)
    private String qrCode;

    // ── Lifecycle ──────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    // ── Audit ──────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Integer version;

    // ── Lifecycle callbacks ────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = TicketStatus.GENERATING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Domain methods ─────────────────────────────────────────────

    public void markGenerated() {
        this.status = TicketStatus.GENERATED;
        this.generatedAt = LocalDateTime.now();
    }

    public void markDelivered() {
        this.status = TicketStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = TicketStatus.CANCELLED;
    }
}
