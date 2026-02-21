package com.ticketblitz.catalog.service;

import com.ticketblitz.catalog.config.CacheConfig;
import com.ticketblitz.catalog.dto.SeatDto;
import com.ticketblitz.catalog.entity.Seat;
import com.ticketblitz.catalog.mapper.SeatMapper;
import com.ticketblitz.catalog.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seat Service
 *
 * CACHING STRATEGY:
 * =================
 * Seats are HIGHLY DYNAMIC (change frequently during booking)
 * Short cache TTL: 30 seconds
 * Cache key includes event ID
 *
 * CONCURRENCY CONSIDERATIONS:
 * ===========================
 * - This service is READ-ONLY
 * - Booking Service handles WRITES
 * - Stale reads acceptable for 30 seconds
 * - Real availability checked during booking
 *
 * ====================
 * In high-concurrency scenarios, consider:
 * 1. Cache invalidation on booking
 * 2. WebSocket for real-time updates
 * 3. Eventual consistency trade-off
 *
 * @author Akhil
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    /**
     * Get all seats for an event
     *
     * CACHING: 30 seconds TTL (highly dynamic)
     */
    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId",
            cacheManager = "redisCacheManager"
    )
    public List<SeatDto> getSeatsByEvent(Long eventId) {
        log.debug("Fetching all seats for event: {}", eventId);

        List<Seat> seats = seatRepository.findByEventId(eventId);
        return seatMapper.toDtoList(seats);
    }

    /**
     * Get available seats only
     *
     * MOST FREQUENTLY CALLED ENDPOINT
     * This is called on every event detail page load
     */
    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':available'",
            cacheManager = "redisCacheManager"
    )
    public List<SeatDto> getAvailableSeats(Long eventId) {
        log.debug("Fetching available seats for event: {}", eventId);

        List<Seat> seats = seatRepository.findAvailableSeatsByEventId(eventId);
        return seatMapper.toDtoList(seats);
    }

    /**
     * Get seats by section
     */
    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':section:' + #section",
            cacheManager = "redisCacheManager"
    )
    public List<SeatDto> getSeatsBySection(Long eventId, String section) {
        log.debug("Fetching seats for event: {}, section: {}", eventId, section);

        List<Seat> seats = seatRepository.findByEventIdAndSection(eventId, section);
        return seatMapper.toDtoList(seats);
    }

    /**
     * Get seat map (grouped by section)
     *
     * Aggregate data for UI
     */
    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':seatmap'",
            cacheManager = "redisCacheManager"
    )
    public Map<String, List<SeatDto>> getSeatMap(Long eventId) {
        log.debug("Building seat map for event: {}", eventId);

        List<Seat> seats = seatRepository.findByEventId(eventId);
        List<SeatDto> seatDtos = seatMapper.toDtoList(seats);

        // Group by section
        return seatDtos.stream()
                .collect(Collectors.groupingBy(SeatDto::getSection));
    }

    /**
     * Get available seat count (for quick checks)
     */
    public int getAvailableSeatCount(Long eventId) {
        log.debug("Counting available seats for event: {}", eventId);

        return seatRepository.countAvailableSeats(eventId);
    }

    /**
     * Get sections for an event
     */
    @Cacheable(
            value = CacheConfig.SEAT_AVAILABILITY_CACHE,
            key = "'event:' + #eventId + ':sections'",
            cacheManager = "redisCacheManager"
    )
    public List<String> getSections(Long eventId) {
        log.debug("Fetching sections for event: {}", eventId);

        return seatRepository.findDistinctSectionsByEventId(eventId);
    }
}