package com.ticketblitz.fulfillment.repository;

import com.ticketblitz.fulfillment.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Ticket Repository
 *
 * KEY QUERIES:
 * ============
 * 1. findByBookingId    → Retrieve all tickets for a booking (fulfillment check)
 * 2. findByTicketNumber → Single ticket lookup (QR code scan, download)
 * 3. findByUserId       → User's ticket history (paginated)
 * 4. existsByBookingId  → Idempotency check before ticket generation
 *
 * @author Akhil
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Find all tickets for a booking.
     * Used by the ticket retrieval API and idempotency checks.
     */
    List<Ticket> findByBookingId(Long bookingId);

    /**
     * Find ticket by its unique ticket number (UUID).
     * Used for QR code scanning and individual ticket lookup.
     */
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    /**
     * Check if tickets already exist for a booking.
     * IDEMPOTENCY: prevents duplicate ticket generation on message redelivery.
     */
    boolean existsByBookingId(Long bookingId);

    /**
     * Find all tickets for a user (paginated).
     * Used by the "My Tickets" API endpoint.
     */
    @Query("SELECT t FROM Ticket t WHERE t.userId = :userId ORDER BY t.eventDate DESC")
    Page<Ticket> findByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Count tickets for a booking.
     * Used for validation and stats.
     */
    long countByBookingId(Long bookingId);
}
