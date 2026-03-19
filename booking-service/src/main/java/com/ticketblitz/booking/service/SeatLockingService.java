package com.ticketblitz.booking.service;

import com.ticketblitz.common.constant.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seat locking service: catalog service integration
 *
 * Purpose:
 * Update seat status in catalog service
 *  - AVAILABLE -> LOCKED (during booking)
 *  - LOCKED -> BOOKED (After payment)
 *  - LOCKED -> AVAILABLE (on cancel/timeout)
 *
 * Design pattern: Async Non blocking
 * -----------------------------------
 * - All operations are @Async to avoid blocking booking flow
 * - If catalog service is down, booking still succeeds
 * - Eventual consistency model
 *
 * Why async
 * ----------
 * - Don't block critical booking flow
 * - Catalog service failure shouldn't fail booking
 * - Can retry later if needed
 * - Better performance under high load
 *
 * Eventual consistency
 * ---------------------
 * - Booking database is source of truth
 * - Catalog service eventually reflects current state
 * - Reconcillation job can fix inconsistencies
 *
 * Alternative approaches
 * -----------------------
 * 1. Synchronous calls: Slower, booking fails if catalog down
 * 2. Event driven(RabbitMQ): More complex, adds latency
 * 3. Saga pattern: Overkill for this use case
 *
 * Tradeoffs
 * ----------
 * 1. Use @Async with best effort delivery
 * 2. Accept eventual consistency tradeoff
 * 3. Prioritize booking success over perfect consistency
 *
 * @author Akhil
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeatLockingService {

    private final RestTemplate restTemplate;

    private static final String CATALOG_BASE_URL = "http://localhost:8081/api/v1/seats";

    /**
     * Lock seats in catalog (async, non-blocking)
     *
     * called when: Booking created (PENDING status)
     * Updates: AVAILABLE -> LOCKED
     */
    @Async
    public void lockSeatsInCatalog(Long eventId, List<Long> seatIds, Long bookingId) {
        log.info("Locking {} seats for booking: {}", seatIds.size(), bookingId);

        try {
            updateSeatStatus(seatIds, SeatStatus.LOCKED);
            log.info("Seats locked successfully for booking: {}", bookingId);
        }
        catch (Exception e) {
            log.error("Failed to lock seats for booking: {}. " +
                    "Catalog may be inconsistent.", bookingId, e);
        }
    }

    /**
     * Book seats in catalog (async, non-blocking)
     *
     * Called when: Payment confirmed (CONFIRMED status)
     * Updates: LOCKED -> BOOKED
     */
    @Async
    public void bookSeatsInCatalog(Long eventId, List<Long> seatIds, Long bookingId) {
        log.info("Booking {} seats for confirmed booking: {}", seatIds.size(), bookingId);

        try {
            updateSeatStatus(seatIds, SeatStatus.BOOKED);
            log.info("Seats booked successfully for booking: {}", bookingId);
        }
        catch (Exception e) {
            log.error("Failed to book seats for booking: {}. Catalog may be inconsistent.",
                    bookingId, e);
        }
    }

    /**
     * Release seats in catalog
     *
     * Called when: booking cancelled or expired
     * updates -> LOCKED -> AVAILABLE
     */
    @Async
    public void releaseSeatsInCatalog(Long eventId, List<Long> seatIds) {
        log.info("Releasing {} seats back to available", seatIds.size());

        try {
            updateSeatStatus(seatIds, SeatStatus.AVAILABLE);
            log.info("Seats released successfully");
        }
        catch(Exception e) {
            log.error("Failed to release seats. Catalog may be inconsistent.", e);
        }
    }

    /**
     * Bulk update seat status (REST Call to catalog service)
     *
     * TODO: future implementation
     */
    private void updateSeatStatus(List<Long> seatIds, SeatStatus newStatus) {
        log.debug("Updating {} seats to status: {}", seatIds.size(), newStatus);

        // mock implementation

        Map<String, Object> request = new HashMap<>();
        request.put("seatIds", seatIds);
        request.put("status", newStatus.toString());

        log.debug("Seat status update mock: {} -> {}", seatIds, newStatus);

        try {
            restTemplate.postForEntity(
                    CATALOG_BASE_URL + "/bulk-update",
                    request,
                    Void.class
            );
        }
        catch (Exception e) {
            log.error("Failed to update seat status", e);
            throw e;
        }
    }

    /**
     * Verify seats are still available (synchronous check)
     *
     * Used before: Acquiring distributed lock
     * Prevents: Locking seats that are already booked
     */
    public boolean verifySeatAvailability(Long eventId, List<Long> seatIds) {
        log.debug("Verifying availability of {} seats", seatIds.size());

        // TODO: Implement actual check
        // For now, assume available

        return true;
    }
}