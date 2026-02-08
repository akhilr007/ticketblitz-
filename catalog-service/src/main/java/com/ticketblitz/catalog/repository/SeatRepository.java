package com.ticketblitz.catalog.repository;

import com.ticketblitz.catalog.entity.Seat;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Seat Repository
 *
 * HIGH CONCURRENCY CONSIDERATIONS:
 * ================================
 * Seats are the MOST frequently accessed/updated data
 * - Short cache TTL (30 seconds)
 * - Optimistic locking (@Version field)
 * - Indexed by event_id + status
 *
 * BOOKING SERVICE INTEGRATION
 * ===========================
 * This service only READS seat data
 * Booking service handles WRITES(status updates)
 *
 * @author Akhil
 */
@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Find available seats for an event
     *
     * OPTIMIZATION: Index on(event_id, status)
     * This query is executed very frequently
     */
    @Query("SELECT s FROM Seat s " +
            "WHERE s.eventId = :eventId " +
            "AND s.status = 'AVAILABLE' " +
            "ORDER BY s.section, s.rowLabel, s.seatNumber")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Seat> findAvailableSeatsByEventId(@Param("eventId") Long eventId);

    /**
     * Find all seats for an event (with status)
     */
    @Query("SELECT s FROM Seat s " +
            "WHERE s.eventId = :eventId " +
            "ORDER BY s.section, s.rowLabel, s.seatNumber")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Seat> findByEventId(@Param("eventId") Long eventId);

    /**
     * Find seats by section
     */
    @Query("SELECT s FROM Seat s " +
            "WHERE s.eventId = :eventId " +
            "AND s.section = :section " +
            "ORDER BY s.rowLabel, s.seatNumber")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    List<Seat> findByEventIdAndSection(
            @Param("eventId") Long eventId,
            @Param("section") String section
    );

    /**
     * Count available seats by event
     */
    @Query("SELECT COUNT(s) FROM Seat s " +
            "WHERE s.eventId = :eventId " +
            "AND s.status = 'AVAILABLE'")
    int countAvailableSeats(@Param("eventId") Long eventId);

    /**
     * Get distinct sections for an event
     */
    @Query("SELECT DISTINCT s.section FROM Seat s " +
            "WHERE s.eventId = :eventId " +
            "ORDER BY s.section")
    List<String> findDistinctSectionsByEventId(@Param("eventId") Long eventId);
}