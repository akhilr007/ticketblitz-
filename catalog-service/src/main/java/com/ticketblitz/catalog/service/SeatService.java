package com.ticketblitz.catalog.service;

import com.ticketblitz.catalog.config.CacheConfig;
import com.ticketblitz.catalog.dto.SeatDto;
import com.ticketblitz.catalog.entity.Event;
import com.ticketblitz.catalog.entity.Seat;
import com.ticketblitz.catalog.mapper.SeatMapper;
import com.ticketblitz.catalog.repository.EventRepository;
import com.ticketblitz.catalog.repository.SeatRepository;
import com.ticketblitz.common.constant.SeatStatus;
import com.ticketblitz.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seat Service
 *
 * Read paths remain cached for catalog traffic, but booking-related writes are
 * handled synchronously and transactionally so inventory stays authoritative.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final SeatMapper seatMapper;
    private final CacheManager caffeineCacheManager;
    private final CacheManager redisCacheManager;

    public SeatService(
            SeatRepository seatRepository,
            EventRepository eventRepository,
            SeatMapper seatMapper,
            @Qualifier("caffeineCacheManager") CacheManager caffeineCacheManager,
            @Qualifier("redisCacheManager") CacheManager redisCacheManager
    ) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
        this.seatMapper = seatMapper;
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
    }

    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId",
            cacheManager = "redisCacheManager"
    )
    public List<SeatDto> getSeatsByEvent(Long eventId) {
        log.debug("Fetching all seats for event: {}", eventId);
        return seatMapper.toDtoList(seatRepository.findByEventId(eventId));
    }

    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':available'",
            cacheManager = "redisCacheManager"
    )
    public List<SeatDto> getAvailableSeats(Long eventId) {
        log.debug("Fetching available seats for event: {}", eventId);
        return seatMapper.toDtoList(seatRepository.findAvailableSeatsByEventId(eventId));
    }

    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':section:' + #section",
            cacheManager = "redisCacheManager"
    )
    public List<SeatDto> getSeatsBySection(Long eventId, String section) {
        log.debug("Fetching seats for event: {}, section: {}", eventId, section);
        return seatMapper.toDtoList(seatRepository.findByEventIdAndSection(eventId, section));
    }

    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':seatmap'",
            cacheManager = "redisCacheManager"
    )
    public Map<String, List<SeatDto>> getSeatMap(Long eventId) {
        log.debug("Building seat map for event: {}", eventId);

        return seatMapper.toDtoList(seatRepository.findByEventId(eventId)).stream()
                .collect(Collectors.groupingBy(SeatDto::getSection));
    }

    public int getAvailableSeatCount(Long eventId) {
        log.debug("Counting available seats for event: {}", eventId);
        return seatRepository.countAvailableSeats(eventId);
    }

    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':sections'",
            cacheManager = "redisCacheManager"
    )
    public List<String> getSections(Long eventId) {
        log.debug("Fetching sections for event: {}", eventId);
        return seatRepository.findDistinctSectionsByEventId(eventId);
    }

    public List<SeatDto> getSeatsByEventAndIds(Long eventId, List<Long> seatIds) {
        List<Long> normalizedSeatIds = normalizeSeatIds(seatIds);
        List<Seat> seats = seatRepository.findByEventIdAndIdIn(eventId, normalizedSeatIds);

        validateAllRequestedSeatsFound(eventId, normalizedSeatIds, seats);
        return seatMapper.toDtoList(seats);
    }

    @Transactional
    public List<SeatDto> lockSeats(Long eventId, List<Long> seatIds) {
        List<Seat> seats = loadSeatsForUpdate(eventId, seatIds);
        ensureStatuses(seats, SeatStatus.AVAILABLE,
                "One or more seats are no longer available for booking.");

        seats.forEach(seat -> seat.setStatus(SeatStatus.LOCKED));
        refreshEventAvailability(eventId);
        evictInventoryCaches(eventId);

        return seatMapper.toDtoList(seats);
    }

    @Transactional
    public List<SeatDto> bookSeats(Long eventId, List<Long> seatIds) {
        List<Seat> seats = loadSeatsForUpdate(eventId, seatIds);
        boolean invalidTransition = seats.stream()
                .anyMatch(seat -> seat.getStatus() != SeatStatus.LOCKED
                        && seat.getStatus() != SeatStatus.BOOKED);

        if (invalidTransition) {
            throw new IllegalStateException("Only locked seats can be confirmed as booked.");
        }

        seats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.LOCKED)
                .forEach(seat -> seat.setStatus(SeatStatus.BOOKED));

        refreshEventAvailability(eventId);
        evictInventoryCaches(eventId);
        return seatMapper.toDtoList(seats);
    }

    @Transactional
    public List<SeatDto> releaseSeats(Long eventId, List<Long> seatIds) {
        List<Seat> seats = loadSeatsForUpdate(eventId, seatIds);
        boolean invalidTransition = seats.stream()
                .anyMatch(seat -> seat.getStatus() == SeatStatus.BOOKED);

        if (invalidTransition) {
            throw new IllegalStateException("Booked seats cannot be released back to inventory.");
        }

        long releasedSeats = seats.stream()
                .filter(seat -> seat.getStatus() == SeatStatus.LOCKED)
                .peek(seat -> seat.setStatus(SeatStatus.AVAILABLE))
                .count();

        if (releasedSeats > 0) {
            refreshEventAvailability(eventId);
        }

        evictInventoryCaches(eventId);
        return seatMapper.toDtoList(seats);
    }

    private List<Seat> loadSeatsForUpdate(Long eventId, List<Long> seatIds) {
        List<Long> normalizedSeatIds = normalizeSeatIds(seatIds);
        List<Seat> seats = seatRepository.findByEventIdAndIdInForUpdate(eventId, normalizedSeatIds);

        validateAllRequestedSeatsFound(eventId, normalizedSeatIds, seats);
        return seats;
    }

    private List<Long> normalizeSeatIds(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("At least one seat ID is required.");
        }

        Set<Long> uniqueSeatIds = new LinkedHashSet<>(seatIds);
        if (uniqueSeatIds.size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seat IDs are not allowed.");
        }

        return List.copyOf(uniqueSeatIds);
    }

    private void validateAllRequestedSeatsFound(Long eventId, List<Long> requestedSeatIds, List<Seat> seats) {
        if (seats.size() != requestedSeatIds.size()) {
            throw new ResourceNotFoundException(
                    "Seat",
                    "Some seats were not found for event " + eventId
            );
        }
    }

    private void ensureStatuses(List<Seat> seats, SeatStatus expectedStatus, String message) {
        boolean hasUnexpectedStatus = seats.stream()
                .anyMatch(seat -> seat.getStatus() != expectedStatus);

        if (hasUnexpectedStatus) {
            throw new IllegalStateException(message);
        }
    }

    private void refreshEventAvailability(Long eventId) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        int updatedAvailableSeats = seatRepository.countAvailableSeats(eventId);

        event.setAvailableSeats(updatedAvailableSeats);
        if (updatedAvailableSeats == 0 && event.getStatus() == Event.EventStatus.ACTIVE) {
            event.setStatus(Event.EventStatus.SOLD_OUT);
        } else if (updatedAvailableSeats > 0 && event.getStatus() == Event.EventStatus.SOLD_OUT) {
            event.setStatus(Event.EventStatus.ACTIVE);
        }
    }

    private void evictInventoryCaches(Long eventId) {
        evict(redisCacheManager.getCache(CacheConfig.SEAT_AVAILABILITY_CACHE), "event:" + eventId);
        evict(redisCacheManager.getCache(CacheConfig.SEAT_AVAILABILITY_CACHE), "event:" + eventId + ":available");
        evict(redisCacheManager.getCache(CacheConfig.SEAT_AVAILABILITY_CACHE), "event:" + eventId + ":seatmap");
        evict(redisCacheManager.getCache(CacheConfig.SEAT_AVAILABILITY_CACHE), "event:" + eventId + ":sections");
        evict(caffeineCacheManager.getCache(CacheConfig.EVENT_DETAILS_CACHE), eventId);

        Cache eventListCache = caffeineCacheManager.getCache(CacheConfig.EVENT_LIST_CACHE);
        if (eventListCache != null) {
            eventListCache.clear();
        }
    }

    private void evict(Cache cache, Object key) {
        if (cache != null) {
            cache.evict(key);
        }
    }
}
