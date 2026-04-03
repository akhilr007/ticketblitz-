-- ============================================================================
-- TICKETS TABLE
-- ============================================================================
-- One ticket per seat in a confirmed booking.
-- Generated asynchronously when BookingConfirmedEvent is consumed.
-- ============================================================================

CREATE TABLE IF NOT EXISTS tickets (
    id              BIGSERIAL PRIMARY KEY,

    -- Booking reference (cross-service)
    booking_id      BIGINT NOT NULL,
    user_id         VARCHAR(255) NOT NULL,

    -- Event info (denormalized from booking event)
    event_id        BIGINT NOT NULL,
    event_name      VARCHAR(255) NOT NULL,
    venue_name      VARCHAR(255) NOT NULL,
    event_date      TIMESTAMP NOT NULL,

    -- Seat info (denormalized from booking event)
    seat_id         BIGINT NOT NULL,
    section         VARCHAR(50) NOT NULL,
    row_label       VARCHAR(10) NOT NULL,
    seat_number     INTEGER NOT NULL,
    price           DECIMAL(10, 2) NOT NULL,

    -- Ticket identifiers
    ticket_number   VARCHAR(36) NOT NULL UNIQUE,  -- UUID
    qr_code         VARCHAR(512),                 -- QR payload (JSON or encoded string)

    -- Lifecycle
    status          VARCHAR(20) NOT NULL DEFAULT 'GENERATING',
    generated_at    TIMESTAMP,
    delivered_at    TIMESTAMP,

    -- Audit
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Optimistic locking
    version         INTEGER NOT NULL DEFAULT 0,

    -- Prevent duplicate tickets for same seat in same booking
    UNIQUE(booking_id, seat_id)
);

-- Performance indexes
CREATE INDEX idx_tickets_booking   ON tickets(booking_id);
CREATE INDEX idx_tickets_user      ON tickets(user_id);
CREATE INDEX idx_tickets_event     ON tickets(event_id);
CREATE INDEX idx_tickets_number    ON tickets(ticket_number);
CREATE INDEX idx_tickets_status    ON tickets(status);

COMMENT ON TABLE tickets IS 'E-tickets generated for confirmed bookings, one per seat';
