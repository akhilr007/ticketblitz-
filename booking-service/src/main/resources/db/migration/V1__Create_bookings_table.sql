CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL, --JWT token
    event_id BIGINT NOT NULL,
    event_name VARCHAR(255) NOT NULL,
    venue_name VARCHAR(255) NOT NULL,
    event_date TIMESTAMP NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', --PENDING, RESERVED, CONFIRMED, CANCELLED, FAILED

    total_amount DECIMAL(10, 2) NOT NULL,
    total_seats INTEGER NOT NULL,

    --idempotency
    idempotency_key VARCHAR(255) UNIQUE,

    -- timeout handling
    reserved_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,

    -- audit
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- optimistic locking
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_event ON bookings(event_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_expires ON bookings(expires_at) WHERE status = 'PENDING';

-- booking_items table (individual seats in a table)
CREATE TABLE IF NOT EXISTS booking_items (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,

    seat_id BIGINT NOT NULL, -- reference to catalog service seat
    section VARCHAR(50) NOT NULL,
    row_label VARCHAR(10) NOT NULL,
    seat_number INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(booking_id, seat_id) -- prevents duplicate seats in same booking
);

CREATE INDEX idx_booking_items_booking ON booking_items(booking_id);
CREATE INDEX idx_booking_items_seat ON booking_items(seat_id);

-- payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id),

    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',

    payment_method VARCHAR(50) NOT NULL, -- CREDIT_CARD, DEBIT_CARD, PAYPAL etc

    -- payment gateway fields (mock for now)
    payment_gateway VARCHAR(50) NOT NULL DEFAULT 'MOCK',
    gateway_transaction_id VARCHAR(255),

    status VARCHAR(20) NOT NULL, -- PENDING, SUCCESS, FAILED

    -- TIMESTAMPS
    initiated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,

    -- error handling
    error_message TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE(payment_gateway, gateway_transaction_id)
);

CREATE INDEX idx_payments_booking ON payments(booking_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway_tx ON payments(gateway_transaction_id);

COMMENT ON TABLE bookings IS 'Main booking records with user and event information';
COMMENT ON TABLE booking_items IS 'Individual seats within a booking';
COMMENT ON TABLE payments IS 'Payment records for bookings';