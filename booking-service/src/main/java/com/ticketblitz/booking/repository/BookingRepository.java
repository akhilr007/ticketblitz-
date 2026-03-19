package com.ticketblitz.booking.repository;

import com.ticketblitz.booking.dto.BookingStatsDto;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.common.constant.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Booking Repository
 *
 * CONCURRENCY CONTROL
 * -------------------
 * 1. Pessimistic Locking (SELECT FOR UPDATE)
 *  - used when updating booking status
 *  - prevents concurrent modifications
 *  - database-level locking
 *
 * 2. Optimistic locking (@Version)
 *  - fallback for non-critical reads
 *  - lighter than pessimistic
 *
 * LOCKING STRATEGIES
 * ---------------------
 * 1. PESSIMISTIC_WRITE: SELECT ... FOR UPDATE
 *  - used for payment processing, status updates
 *  - blocks other transactions until released
 *
 * 2. PESSIMISTIC_READ: SELECT ... FOR SHARE
 *  - used for reading during payment
 *  - allows concurrent reads
 *
 * IDEMPOTENCY
 * -------------
 * - findByIdempotencyKey() prevents duplicate bookings
 * - client sends UUID, we check if booking already exists
 *
 * @author Akhil
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * find booking by ID with pessimistic write lock
     *
     * USE CASE: payment processing, status updates
     * LOCKS: exclusive lock (blocks all other operations)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);


    /**
     * find booking by idempotency key (prevents duplicates)
     *
     * IDEMPOTENCY PATTERN
     * if client retries with same idempotency key, return existing booking
     */
    Optional<Booking> findByIdempotencyKey(String idempotencyKey);

    /**
     * find user's booking (paginated)
     */
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    Page<Booking> findByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * find expired bookings (for cleanup job)
     *
     * SCHEDULED TASK: runs every 5 minutes
     * finds bookings that expired but still pending
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.status = 'PENDING' "+
            "AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);


    /**
     * find bookings by event (analytics)
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.eventId = :eventId " +
            "AND b.status IN :statuses " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findByEventIdAndStatusIn(
            @Param("eventId") Long eventId,
            @Param("statuses") List<BookingStatus> statuses
    );

    /**
     * Count bookings by status
     */
    long countByStatus(BookingStatus status);

    /**
     * Count user's bookings
     */
    long countByUserId(String userId);

    /**
     * Find bookings expiring soon (for reminder notifications)
     */
    @Query("SELECT b FROM Booking b " +
            "WHERE b.status = 'PENDING' " +
            "AND b.expiresAt BETWEEN :now AND :threshold")
    List<Booking> findBookingsExpiringSoon(
            @Param("now") LocalDateTime now,
            @Param("threshold") LocalDateTime threshold
    );

    /**
     * Get booking statistics (for admin dashboard)
     */
    @Query("""
    SELECT new com.ticketblitz.booking.dto.BookingStatsDto(
           COUNT(b),
           COALESCE(SUM(CASE WHEN b.status = com.ticketblitz.common.constant.BookingStatus.CONFIRMED THEN 1 ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN b.status = com.ticketblitz.common.constant.BookingStatus.PENDING THEN 1 ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN b.status = com.ticketblitz.common.constant.BookingStatus.CANCELLED THEN 1 ELSE 0 END), 0),
           COALESCE(SUM(CASE WHEN b.status = com.ticketblitz.common.constant.BookingStatus.CONFIRMED THEN b.amount ELSE 0 END), 0)
    )
    FROM Booking b
    """)
    BookingStatsDto getBookingStatistics();
}