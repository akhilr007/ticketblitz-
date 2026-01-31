-- =============================================================================
-- INSERT VENUES
-- =============================================================================
INSERT INTO venues (name, address, city, state, country, postal_code, capacity, description) VALUES
('Madison Square Garden', '4 Pennsylvania Plaza', 'New York', 'NY', 'USA', '10001', 20000, 'World-famous arena in the heart of Manhattan'),
('Wembley Stadium', 'Wembley', 'London', NULL, 'UK', 'HA9 0WS', 90000, 'Iconic football stadium and concert venue'),
('Hollywood Bowl', '2301 Highland Ave', 'Los Angeles', 'CA', 'USA', '90068', 17500, 'Outdoor amphitheater known for its distinctive shell'),
('Red Rocks Amphitheatre', '18300 W Alameda Pkwy', 'Morrison', 'CO', 'USA', '80465', 9525, 'Natural rock formation concert venue'),
('Sydney Opera House', 'Bennelong Point', 'Sydney', 'NSW', 'Australia', '2000', 5738, 'Multi-venue performing arts center');

-- =============================================================================
-- INSERT EVENTS
-- =============================================================================
INSERT INTO events (venue_id, name, description, event_date, event_end_date, category, total_seats, available_seats, base_price, status, image_url) VALUES
-- Madison Square Garden Events
(1, 'Taylor Swift - The Eras Tour', 'Experience the spectacular Eras Tour live', '2025-03-15 20:00:00', '2025-03-15 23:00:00', 'CONCERT', 20000, 18500, 150.00, 'ACTIVE', 'https://example.com/taylor-swift.jpg'),
(1, 'New York Knicks vs Lakers', 'NBA basketball game', '2025-02-20 19:30:00', '2025-02-20 22:00:00', 'SPORTS', 20000, 15000, 85.00, 'ACTIVE', 'https://example.com/knicks.jpg'),

-- Wembley Stadium Events
(2, 'Champions League Final', 'UEFA Champions League Final', '2025-05-30 20:00:00', '2025-05-30 23:00:00', 'SPORTS', 90000, 75000, 200.00, 'ACTIVE', 'https://example.com/ucl.jpg'),
(2, 'Coldplay World Tour', 'Coldplay live in concert', '2025-06-15 19:00:00', '2025-06-15 22:30:00', 'CONCERT', 90000, 85000, 120.00, 'ACTIVE', 'https://example.com/coldplay.jpg'),

-- Hollywood Bowl Events
(3, 'LA Philharmonic - Summer Series', 'Classical music under the stars', '2025-07-10 20:00:00', '2025-07-10 22:30:00', 'CONCERT', 17500, 12000, 75.00, 'ACTIVE', 'https://example.com/philharmonic.jpg'),

-- Red Rocks Events
(4, 'The Lumineers Live', 'Indie folk concert', '2025-08-05 19:00:00', '2025-08-05 22:00:00', 'CONCERT', 9525, 8000, 95.00, 'ACTIVE', 'https://example.com/lumineers.jpg'),

-- Sydney Opera House Events
(5, 'The Phantom of the Opera', 'Classic musical theater', '2025-04-01 19:30:00', '2025-04-01 22:00:00', 'THEATER', 5738, 4500, 110.00, 'ACTIVE', 'https://example.com/phantom.jpg');

-- =============================================================================
-- INSERT SEATS (Sample for first event only - Madison Square Garden)
-- =============================================================================
-- Section A (VIP)
INSERT INTO seats (event_id, section, row_label, seat_number, price, status)
SELECT
    1,  -- Madison Square Garden - Taylor Swift
    'VIP-A',
    'A',
    generate_series(1, 50),
    250.00,
    'AVAILABLE';

-- Section B (Floor)
INSERT INTO seats (event_id, section, row_label, seat_number, price, status)
SELECT
    1,
    'FLOOR-B',
    chr(65 + (generate_series(1, 500) - 1) / 25),  -- A-T
    ((generate_series(1, 500) - 1) % 25) + 1,
    150.00,
    'AVAILABLE';

-- Upper levels
INSERT INTO seats (event_id, section, row_label, seat_number, price, status)
SELECT
    1,
    'UPPER-' || (generate_series(1, 100) / 20 + 1),
    chr(65 + (generate_series(1, 100) - 1) % 10),
    ((generate_series(1, 100) - 1) % 20) + 1,
    75.00,
    'AVAILABLE';

COMMENT ON TABLE seats IS 'For demo, only first event has seats. Other events would have seats added similarly.';