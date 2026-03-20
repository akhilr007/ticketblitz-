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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    @Transactional
    public BookingDto createBooking(String userId, CreateBookingRequest request) {
        log.info("Creating booking for user: {}, event: {}, seats: {}",
                userId, request.getEventId(), request.getSeatIds());

        List<Long> requestedSeatIds = normalizeSeatIds(request.getSeatIds());
        return lockService.executeWithFairLock(
                buildBookingLockKey(request.getIdempotencyKey()),
                () -> createBookingWithinIdempotencyLock(userId, request, requestedSeatIds)
        );
    }

    @Transactional(readOnly = true)
    public BookingDto getBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return bookingMapper.toDto(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingListDto> getUserBookings(String userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable)
                .map(bookingMapper::toListDto);
    }

    @Transactional
    public BookingDto cancelBooking(Long bookingId, String userId) {
        log.info("Cancelling booking: {}, user: {}", bookingId, userId);

        return lockService.executeWithLock(
                buildBookingLockKey(bookingId.toString()),
                () -> {
                    Booking booking = bookingRepository.findByIdWithLock(bookingId)
                            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

                    if (!booking.getUserId().equals(userId)) {
                        throw new IllegalArgumentException("Unauthorized to cancel this booking");
                    }

                    if (!booking.canBeCancelled()) {
                        throw new IllegalStateException(
                                "Booking cannot be cancelled in current status: " + booking.getStatus()
                        );
                    }

                    booking.cancel();
                    bookingRepository.save(booking);

                    List<Long> seatIds = booking.getItems().stream()
                            .map(BookingItem::getSeatId)
                            .toList();

                    seatLockingService.releaseSeatsInCatalog(booking.getEventId(), seatIds);

                    log.info("Booking cancelled: {}", bookingId);
                    return bookingMapper.toDto(booking);
                }
        );
    }

    private BookingDto createBookingWithinIdempotencyLock(
            String userId,
            CreateBookingRequest request,
            List<Long> requestedSeatIds
    ) {
        Booking existingBooking = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey())
                .orElse(null);

        if (existingBooking != null) {
            log.info("Booking already exists for idempotency key {}: {}",
                    request.getIdempotencyKey(), existingBooking.getId());
            return bookingMapper.toDto(existingBooking);
        }

        CatalogServiceClient.EventInfo event = getActiveEvent(request.getEventId());
        if (event.availableSeats() != null && event.availableSeats() < requestedSeatIds.size()) {
            throw new IllegalStateException("Not enough seats are available for this event.");
        }

        List<CatalogServiceClient.SeatInfo> seats =
                seatLockingService.getSeatsForBooking(request.getEventId(), requestedSeatIds);

        BigDecimal totalAmount = seats.stream()
                .map(CatalogServiceClient.SeatInfo::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> lockKeys = requestedSeatIds.stream()
                .map(seatId -> buildSeatLockKey(request.getEventId(), seatId))
                .toList();

        return lockService.executeWithLocks(
                lockKeys,
                () -> doCreateBooking(userId, request, requestedSeatIds, event, seats, totalAmount)
        );
    }

    private BookingDto doCreateBooking(
            String userId,
            CreateBookingRequest request,
            List<Long> requestedSeatIds,
            CatalogServiceClient.EventInfo event,
            List<CatalogServiceClient.SeatInfo> seats,
            BigDecimal totalAmount
    ) {
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(reservationTimeoutMinutes);

        Booking booking = Booking.builder()
                .userId(userId)
                .eventId(request.getEventId())
                .eventName(event.name())
                .venueName(extractVenueName(event))
                .eventDate(event.eventDate())
                .status(BookingStatus.PENDING)
                .amount(totalAmount)
                .totalSeats(seats.size())
                .idempotencyKey(request.getIdempotencyKey())
                .expiresAt(expiresAt)
                .build();

        for (CatalogServiceClient.SeatInfo seat : seats) {
            booking.addItem(BookingItem.builder()
                    .seatId(seat.id())
                    .section(seat.section())
                    .rowLabel(seat.rowLabel())
                    .seatNumber(seat.seatNumber())
                    .price(seat.price())
                    .build());
        }

        booking = bookingRepository.save(booking);
        seatLockingService.lockSeatsInCatalog(request.getEventId(), requestedSeatIds);

        log.info("Booking created: {}, expires at: {}", booking.getId(), expiresAt);
        return bookingMapper.toDto(booking);
    }

    private CatalogServiceClient.EventInfo getActiveEvent(Long eventId) {
        ApiResponse<CatalogServiceClient.EventInfo> response = catalogClient.getEvent(eventId);
        if (response == null || !"success".equalsIgnoreCase(response.getStatus()) || response.getData() == null) {
            throw new IllegalArgumentException("Event not found: " + eventId);
        }

        CatalogServiceClient.EventInfo event = response.getData();
        if (!"ACTIVE".equalsIgnoreCase(event.status())) {
            throw new IllegalStateException("Event is not available for booking.");
        }

        return event;
    }

    private List<Long> normalizeSeatIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected.");
        }

        Set<Long> uniqueSeatIds = new LinkedHashSet<>(seatIds);
        if (uniqueSeatIds.size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seat IDs are not allowed.");
        }

        return List.copyOf(uniqueSeatIds);
    }

    private String buildSeatLockKey(Long eventId, Long seatId) {
        return String.format("booking:seat:%d:%d", eventId, seatId);
    }

    private String buildBookingLockKey(String identifier) {
        return String.format("booking:operation:%s", identifier);
    }

    private String extractVenueName(CatalogServiceClient.EventInfo event) {
        if (event.venue() == null || event.venue().name() == null || event.venue().name().isBlank()) {
            throw new IllegalStateException("Catalog service returned an event without venue information.");
        }

        return event.venue().name();
    }
}
