package com.ticketblitz.booking.scheduler;

import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.booking.service.BookingExpirationService;
import com.ticketblitz.booking.service.DistributedLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private static final String EXPIRED_BOOKING_CLEANUP_LOCK = "booking:cleanup:expired";

    private final BookingRepository bookingRepository;
    private final BookingExpirationService bookingExpirationService;
    private final DistributedLockService lockService;

    @Scheduled(
            fixedDelayString = "${booking.reservation.cleanup-interval-minutes:5}",
            initialDelay = 1,
            timeUnit = TimeUnit.MINUTES
    )
    public void cleanupExpiredBookings() {
        try {
            lockService.executeWithLock(EXPIRED_BOOKING_CLEANUP_LOCK, () -> {
                runCleanup();
                return null;
            });
        } catch (DistributedLockService.LockAcquisitionException ex) {
            log.debug("Skipping expired booking cleanup because another instance is already running it.");
        }
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void sendExpiryReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime threshold = now.plusMinutes(2);

            List<Booking> expiringSoon = bookingRepository.findBookingsExpiringSoon(now, threshold);
            for (Booking booking : expiringSoon) {
                log.info("Booking {} expiring soon. User should complete payment.", booking.getId());
            }
        } catch (Exception e) {
            log.error("Failed to send expiry reminders", e);
        }
    }

    private void runCleanup() {
        log.info("Starting expired booking cleanup jobs");

        long startTime = System.currentTimeMillis();
        int cancelledCount = 0;

        try {
            List<Long> expiredBookingIds = bookingRepository.findExpiredBookingIds(LocalDateTime.now());
            log.info("Found {} expired bookings to cancel", expiredBookingIds.size());

            for (Long bookingId : expiredBookingIds) {
                try {
                    if (bookingExpirationService.cancelExpiredBooking(bookingId)) {
                        cancelledCount++;
                    }
                } catch (Exception e) {
                    log.error("Failed to cancel expired booking: {}", bookingId, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cleanup job completed: {} bookings cancelled in {}ms", cancelledCount, duration);

            if (cancelledCount > 100) {
                log.warn("HIGH CANCELLATION RATE: {} bookings expired. Consider investigating timeout settings.",
                        cancelledCount);
            }
        } catch (Exception e) {
            log.error("Cleanup job failed", e);
        }
    }
}
