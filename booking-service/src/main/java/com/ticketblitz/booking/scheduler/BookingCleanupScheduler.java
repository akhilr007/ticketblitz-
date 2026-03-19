package com.ticketblitz.booking.scheduler;

import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingItem;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.booking.service.SeatLockingService;
import com.ticketblitz.common.constant.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Booking cleanup scheduler
 *
 * Purpose
 * --------
 * Automatically cancel expired bookings
 * Releases seats back to available inventory
 * Prevents seat inventory leakage
 *
 * SCHEDULING
 * -----------
 * Runs every 5 minutes (configurable)
 * Fixed delay: Waits 5 minutes after previous execution completes
 * Initial delay: 1 minute after startup
 *
 * Design considerations
 * ---------------------
 * 1. Fixed delay vs Fixed rate
 *  - Fixed delay: safer, prevents overlap
 *  - Fixed rate: can cause overlap if cleanup takes > 5 min
 *
 * 2. Transaction boundaries
 *  - Separate transaction per booking
 *  - One failure doesn't rollback all
 *
 * 3. Batch size
 *  - process all expired bookings found
 *  - in prodcution, add pagination for large datasets
 *
 * Monitoring
 * ------------
 * - Log count of cancelled bookings
 * - Alert if consistently high cancellation rate
 * - Track cleanup duration for performance
 *
 * Alternative approaches
 * -----------------------
 * 1. Database trigger: Can't call external services
 * 2. Event-driven (RabbitMQ delayed exchange): More complex
 * 3. Separate cleanup service: Overkill for this use case
 *
 * PRODUCTION CONSIDERATIONS:
 * ==========================
 * - Add distributed lock to prevent multiple instances running cleanup
 * - Add metrics (cancelled count, duration)
 * - Add admin endpoint to trigger manual cleanup
 * - Consider ShedLock for distributed scheduling
 *
 * @author Akhil
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;
    private final SeatLockingService seatLockingService;

    /**
     * cleanup expired bookings
     *
     * schedule: every 5 minutes
     * initial delay: 60 seconds after startup
     */
    @Scheduled(
            fixedDelayString = "${booking.reservation.cleanup-interval-minutes:5}",
            initialDelay = 60000, // 1 minute
            timeUnit = TimeUnit.MINUTES
    )
    public void cleanupExpiredBookings() {
        log.info("Starting expired booking cleanup jobs");

        long startTime = System.currentTimeMillis();
        int cancelledCount = 0;

        try {
            // find all expired bookings
            List<Booking> expiredBookings = bookingRepository
                    .findExpiredBookings(LocalDateTime.now());

            log.info("Found {} expired bookings to cancel", expiredBookings.size());

            // process each booking
            for (Booking booking: expiredBookings) {
                try {
                    cancelExpiredBooking(booking);
                    cancelledCount++;
                }
                catch (Exception e) {
                    log.error("Failed to cancel expired booking: {}", booking.getId(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cleanup job completed: {} bookings cancelled in {}ms",
                    cancelledCount, duration);

            // Alert if high cancellation rate
            if (cancelledCount > 100) {
                log.warn("HIGH CANCELLATION RATE: {} bookings expired. " +
                        "Consider investigating timeout settings.", cancelledCount);
            }
        } catch (Exception e) {
            log.error("Cleanup job failed", e);
        }
    }

    /**
     * Cancel individual expired booking
     *
     * TRANSACTION: Separate transaction for each booking
     * Prevents one failure from rolling back all cancellations
     */
    @Transactional
    public void cancelExpiredBooking(Booking booking) {
        log.debug("Cancelling expired booking: {}, user: {}",
                booking.getId(), booking.getUserId());

        // Double-check it's still expired and PENDING
        if (!booking.isExpired() || booking.getStatus() != BookingStatus.PENDING) {
            log.debug("Booking {} is no longer expired or not PENDING, skipping",
                    booking.getId());
            return;
        }

        // Cancel booking
        booking.cancel();
        bookingRepository.save(booking);

        // Release seats back to available
        List<Long> seatIds = booking.getItems().stream()
                .map(BookingItem::getSeatId)
                .collect(Collectors.toList());

        seatLockingService.releaseSeatsInCatalog(
                booking.getEventId(),
                seatIds
        );

        log.info("Expired booking cancelled: {}, seats released: {}",
                booking.getId(), seatIds.size());

        // TODO: Send notification to user about expiration
    }

    /**
     * Send reminder notifications for bookings expiring soon
     *
     * SCHEDULE: Every 1 minute
     * Sends reminder 2 minutes before expiry
     */
    @Scheduled(fixedDelay = 1, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void sendExpiryReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.plusMinutes(2);  // 2 minutes from now

            List<Booking> expiringSoon = bookingRepository
                    .findBookingsExpiringSoon(now, threshold);

            for (Booking booking : expiringSoon) {
                log.info("Booking {} expiring soon. User should complete payment.",
                        booking.getId());

                // TODO: Send push notification or email
                // notificationService.sendExpiryReminder(booking);
            }

        } catch (Exception e) {
            log.error("Failed to send expiry reminders", e);
        }
    }
}