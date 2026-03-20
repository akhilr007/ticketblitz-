package com.ticketblitz.booking.service;

import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingItem;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.common.constant.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingExpirationService {

    private final BookingRepository bookingRepository;
    private final SeatLockingService seatLockingService;

    @Transactional
    public boolean cancelExpiredBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.isExpired() || booking.getStatus() != BookingStatus.PENDING) {
            log.debug("Booking {} is no longer expired or not pending. Skipping cleanup.", bookingId);
            return false;
        }

        booking.cancel();
        bookingRepository.save(booking);

        List<Long> seatIds = booking.getItems().stream()
                .map(BookingItem::getSeatId)
                .toList();

        seatLockingService.releaseSeatsInCatalog(booking.getEventId(), seatIds);
        log.info("Expired booking cancelled: {}, seats released: {}", bookingId, seatIds.size());

        return true;
    }
}
