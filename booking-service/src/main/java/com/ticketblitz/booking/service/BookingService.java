package com.ticketblitz.booking.service;

import com.ticketblitz.booking.client.CatalogServiceClient;
import com.ticketblitz.booking.dto.BookingDto;
import com.ticketblitz.booking.dto.BookingListDto;
import com.ticketblitz.booking.dto.CreateBookingRequest;
import com.ticketblitz.booking.entity.Booking;
import com.ticketblitz.booking.entity.BookingItem;
import com.ticketblitz.booking.mapper.BookingMapper;
import com.ticketblitz.booking.repository.BookingRepository;
import com.ticketblitz.common.constant.BookingStatus;
import com.ticketblitz.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Architecture Patterns
 * ----------------------
 * 1. Distributed locking (Redisson) - cross instance coordination
 * 2. Pessimistic locking (JPA) - Database level locking
 * 3. Optimistic locking (@Version) - Concurrent update detection
 * 4. Idempotency pattern - Prevent duplicate bookings
 * 5. Circuit breaker (Feign) - Resilient inter service calls
 *
 * Concurrent Strategy - 3 Layers
 * ------------------------------
 * Layer 1: Distributed Lock (Redis)
 *  - coordinates b/w multiple service instances
 *  - Key: "booking:seat:{eventId}:{seatId}"
 *  - Timeout: 10 seconds wait, 30 seconds lease
 *
 * Layer 2: Pessimistic lock
 *  - SELECT ... FOR UPDATE on seat records
 *  - Prevent concurrent booking of same seat
 *  - Held within transaction boundary
 *
 * Layer 3: Optimistic lock (@Version)
 *  - Detects concurrent modifications
 *  - Fallback if distributed lock fails
 *  - Throws exception on version mismatch
 *
 * BOOKING FLOW
 * --------------
 * 1. Idempotency check
 *  - (if exists, return existing)
 *
 * 2. Validate event & seats (Catalog service)
 *
 * 3. Acquire distributed locks (all seats)
 *
 * 4. database transaction start
 *
 * 5. pessimistic lock seats (SELECT FOR UPDATE)
 *
 * 6. Create booking (PENDING status)
 *
 * 7. Mark seats as locked in catalog
 *
 * 8. commit transaction
 *
 * 9. release distributed locks
 *
 * 10. return booking (10 minute expiry)
 *
 *
 * Timeout handling
 * -----------------
 * - Booking expires after 10 minutes (configurable)
 * - Scheduled job runs every 5 minutes
 * - Auto cancels expired bookings
 * - Releases seats back to inventory
 *
 * Error Handling
 * ---------------
 * - LockAcquisitionException: another user booking same seats
 * - OptimisticLockException: concurrent modification detected
 * - FeignException: Catalog service unavailable
 * - All errors rollback transaction
 *
 * @author Akhil
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final DistributedLockService lockService;
    private final CatalogServiceClient catalogClient;
    private final SeatLockingService seatLockingService;
    private final BookingMapper bookingMapper;

    @Value("${booking.reservation.timeout-minutes:10}")
    private int reservationTimeoutMinutes;

    /**
     * create booking with distributed locking
     *
     * concurrency: multi layer locking strategy
     * idempotency: check idempotency key first
     * transaction: single database transaction
     */
    @Transactional
    public BookingDto createBooking(String userId, CreateBookingRequest request) {
        log.info("Creating booking for user: {}, event: {}, seats: {}",
                userId, request.getEventId(), request.getSeatIds());

        // idempotency check
        Booking existingBooking = bookingRepository
                .findByIdempotencyKey(request.getIdempotencyKey())
                .orElse(null);

        if (existingBooking != null) {
            log.info("Booking already exists (idempotency): {}", existingBooking.getId() );
            return bookingMapper.toDto(existingBooking);
        }

        // validate event exists
        ApiResponse<CatalogServiceClient.EventInfo> eventResponse =
                catalogClient.getEvent(request.getEventId());

        if (!eventResponse.getStatus().equals("success")) {
            throw new IllegalArgumentException("Event not found: " + request.getEventId());
        }

        CatalogServiceClient.EventInfo event = eventResponse.getData();

        // get seat information
        List<CatalogServiceClient.SeatInfo> seats = getSeatInfo(
                request.getEventId(),
                request.getSeatIds()
        );

        // calculate total amount
        BigDecimal totalAmount = seats.stream()
                .map(CatalogServiceClient.SeatInfo::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // lock seatIds with distributed lock
        return lockSeatsAndCreateBooking(userId, request, event, seats, totalAmount);
    }

    /**
     * lock seats and create booking (critical section)
     *
     * lock all seats before booking to prevent race conditions
     */
    private BookingDto lockSeatsAndCreateBooking(
            String userId,
            CreateBookingRequest request,
            CatalogServiceClient.EventInfo event,
            List<CatalogServiceClient.SeatInfo> seats,
            BigDecimal totalAmount
    ) {
        // build lock keys for all seats
        List<String> lockKeys = seats.stream()
                .map(seat -> buildSeatLockKey(request.getEventId(), seat.id()))
                .toList();

        log.debug("Attempting to acquire {} seat locks", lockKeys.size());

        // execute within distributed lock
        return lockService.executeWithFairLock(
                buildBookingLockKey(request.getIdempotencyKey()),
                () -> {
                    // lock individual seats
                    for (String lockKey: lockKeys) {
                        boolean acquired = lockService.tryLock(lockKey);
                        if (!acquired) {
                            throw new IllegalArgumentException(
                                    "Seat is currently being booked by another user. Please try again."
                            );
                        }
                    }

                    try {
                        // create booking within locks
                        return doCreateBooking(userId, request, event, seats, totalAmount);
                    } finally {
                        // locks auto released by lockService
                    }
                }
        );
    }

    /**
     * actually create the booking
     */
    private BookingDto doCreateBooking(
            String userId,
            CreateBookingRequest request,
            CatalogServiceClient.EventInfo event,
            List<CatalogServiceClient.SeatInfo> seats,
            BigDecimal totalAmount
    ) {
        // calculate expiry time
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusMinutes(reservationTimeoutMinutes);

        Booking booking = Booking.builder()
                .userId(userId)
                .eventId(request.getEventId())
                .eventName(event.name())
                .venueName(event.venueName())
                .eventDate(LocalDateTime.parse(event.eventDate()))
                .status(BookingStatus.PENDING)
                .amount(totalAmount)
                .totalSeats(seats.size())
                .idempotencyKey(request.getIdempotencyKey())
                .expiresAt(expiresAt)
                .build();

        // add booking items
        for (CatalogServiceClient.SeatInfo seat: seats) {
            BookingItem item = BookingItem.builder()
                    .seatId(seat.id())
                    .section(seat.section())
                    .rowLabel(seat.rowLabel())
                    .seatNumber(seat.seatNumber())
                    .price(seat.price())
                    .build();

            booking.addItem(item);
        }

        booking = bookingRepository.save(booking);

        log.info("Booking created: {}, expires at: {}", booking.getId(), expiresAt);

        // lock seats in catalog service
        seatLockingService.lockSeatsInCatalog(
                request.getEventId(),
                request.getSeatIds(),
                booking.getId()
        );

        return bookingMapper.toDto(booking);
    }

    /**
     * get booking by id
     */
    @Transactional(readOnly = true)
    public BookingDto getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Booking not found: " + bookingId
                ));
        return bookingMapper.toDto(booking);
    }

    /**
     * get user's booking
     */
    @Transactional(readOnly = true)
    public Page<BookingListDto> getUserBookings(String userId, Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findByUserId(userId, pageable);
        return bookings.map(bookingMapper::toListDto);
    }

    /**
     * cancel booking (with lock)
     */
    @Transactional
    public BookingDto cancelBooking(Long bookingId, String userId) {
        log.info("Cancelling booking: {}, user: {}", bookingId, userId);

        return lockService.executeWithLock(
                buildBookingLockKey(bookingId.toString()),
                () -> {
                    // lock booking for update
                    Booking booking = bookingRepository.findByIdWithLock(bookingId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Booking not found: " + bookingId
                            ));

                    // verify ownership
                    if (!booking.getUserId().equals(userId)) {
                        throw new IllegalArgumentException("Unauthorized to cancel this booking");
                    }

                    // check if cancelled
                    if (!booking.canBeCancelled()) {
                        throw new IllegalStateException(
                                "Booking cannot can be cancelled in current status: " + booking.getStatus());
                    }

                    // cancel booking
                    booking.cancel();
                    booking = bookingRepository.save(booking);

                    // release seats (async)
                    List<Long> seatIds = booking.getItems().stream()
                            .map(BookingItem::getSeatId)
                            .toList();

                    seatLockingService.releaseSeatsInCatalog(
                            booking.getEventId(),
                            seatIds
                    );

                    log.info("Booking cancelled: {}", bookingId);

                    return bookingMapper.toDto(booking);
                }
        );
    }

    // private helper methods
    private List<CatalogServiceClient.SeatInfo> getSeatInfo(
            Long eventId,
            List<Long> seatIds
    ) {
        ApiResponse<List<CatalogServiceClient.SeatInfo>> response =
                catalogClient.getSeatsForEvent(eventId);

        if (!response.getStatus().equals("success") || response.getData() == null) {
            throw new IllegalArgumentException("Failed to fetch seat information.");
        }

        // filter requested seats
        return response.getData().stream()
                .filter(seat -> seatIds.contains(seat.id()))
                .toList();
    }

    private String buildSeatLockKey(Long eventId, Long seatId) {
        return String.format("booking:seat:%d:%d", eventId, seatId);
    }

    private String buildBookingLockKey(String identifier) {
        return String.format("booking:operation:%s", identifier);
    }

}