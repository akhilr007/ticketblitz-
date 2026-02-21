package com.ticketblitz.catalog.repository;

import com.ticketblitz.catalog.entity.Event;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * DESIGN PATTERN:
 * ===============
 * 1. Repository Pattern - Data access abstraction
 * 2. Specification Pattern - Dynamic Query building (JpaSpecificationExecutor)
 * 3. Query Hints - Performance optimization
 *
 * QUERY OPTIMIZATION:
 * ===================
 * - DTO Projections: Don't fetch full entities when not needed
 * - JOIN FETCH: Solve N+1 problem for associations
 * - Query Hints: Control query plan and caching
 * - Read-Only: Optimization hint for queries
 *
 * SPECIFICATION PATTERN:
 * ======================
 * JpaSpecificationExecutor allows building dynamic queries:
 * - Filter by category, date range, venue, status
 * - Combine multiple criteria
 * - Type-safe query building
 *
 * @author Akhil
 */
@Repository
public interface EventRepository extends
        JpaRepository<Event, Long>,
        JpaSpecificationExecutor<Event> {

    /**
     * Find event with venue (JOIN FETCH prevents N+1)
     *
     * OPTIMIZATION: Single query instead of 2 queries
     * Without Join Fetch: 1 query for event + 1 query for venue
     * WITH JOIN FETCH : 1 query with JOIN
     */
    @Query("SELECT e FROM Event e " +
            "JOIN FETCH e.venue " +
            "WHERE e.id = :id")
    @QueryHints(@QueryHint(name="org.hibernate.readOnly", value = "true"))
    Optional<Event> findByIdWithVenue(@Param("id") Long id);

    /**
     * Find upcoming events (read only optimization)
     */
    @Query("SELECT e FROM Event e " +
            "WHERE e.eventDate >= :now " +
            "AND e.status = 'ACTIVE' " +
            "ORDER BY e.eventDate ASC ")
    @QueryHints(@QueryHint(name="org.hibernate.readOnly", value = "true"))
    Page<Event> findUpcomingEvents(
            @Param("now")LocalDateTime now,
            Pageable pageable
            );


    /**
     * Find events by category (with pagination)
     */
    @Query("SELECT e from Event e " +
            "WHERE e.eventCategory = :category " +
            "AND e.status = 'ACTIVE' " +
            "ORDER BY e.eventDate ASC")
    @QueryHints(@QueryHint(name="org.hibernate.readOnly", value = "true"))
    Page<Event> findByCategory(
            @Param("category") Event.EventCategory category,
            Pageable pageable
    );

    /**
     * Find events by venue
     */
    @Query("SELECT e FROM Event e " +
            "WHERE e.venue.id = :venueId " +
            "AND e.status = 'ACTIVE' " +
            "ORDER BY e.eventDate ASC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Page<Event> findByVenueId(
            @Param("venueId") Long venueId,
            Pageable pageable
    );

    /**
     * Search events by name (case-insensitive)
     */
    @Query("SELECT e from Event e " +
            "WHERE LOWER(e.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "AND e.status = 'ACTIVE' " +
            "ORDER BY e.eventDate ASC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Page<Event> searchByName(
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    /**
     * Find events by date range
     */
    @Query("SELECT e from Event e " +
            "WHERE e.eventDate BETWEEN :startDate AND :endDate " +
            "AND e.status = 'ACTIVE' " +
            "ORDER BY e.eventDate ASC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Page<Event> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find events by city (through venue)
     */
    @Query("SELECT e FROM Event e " +
            "JOIN e.venue v " +
            "WHERE v.city = :city " +
            "AND e.status = 'ACTIVE' " +
            "ORDER BY e.eventDate ASC")
    @QueryHints(@QueryHint(name = "org.hibernate.readOnly", value = "true"))
    Page<Event> findByCity(
            @Param("city") String city,
            Pageable pageable
    );

    /**
     * Check if event exists and is active
     */
    boolean existsByIdAndStatus(Long id, Event.EventStatus status);

    /**
     * Count active events
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = 'ACTIVE' ")
    long countActiveEvents();
}