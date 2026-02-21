package com.ticketblitz.catalog.service;

import com.ticketblitz.catalog.config.CacheConfig;
import com.ticketblitz.catalog.dto.EventDto;
import com.ticketblitz.catalog.dto.EventListDto;
import com.ticketblitz.catalog.dto.PageResponse;
import com.ticketblitz.catalog.entity.Event;
import com.ticketblitz.catalog.mapper.EventMapper;
import com.ticketblitz.catalog.repository.EventRepository;
import com.ticketblitz.catalog.repository.specification.EventSpecification;
import com.ticketblitz.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event Service
 *
 * DESIGN PATTERNS:
 * 1. Service layer pattern - Business logic isolation
 * 2. Cache-Aside pattern - Explicit cache management
 * 3. DTO pattern - Never expose entities
 * 4. Transaction management - Read-only optimization
 *
 * CACHING STRATEGY:
 * 1. Event lists: 5 min TTL (frequent updates)
 * 2. Event details: 10 min TTL (rare updates)
 * 3. Cache keys include all filter params
 *
 * CONCURRENCY:
 * 1. Read only transactions (optimization)
 * 2. No locks needed (Read-only service)
 * 3. Writes handled by booking service
 *
 * TRANSACTION BOUNDERIES:
 * Servic layer = transaction boundary
 * @Transactional(readOnly=true) optimization:
 * - Disable dirty checking
 * - No flust to database
 * - Faster query execution
 *
 * @author Akhil
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    /**
     * Get event by ID (with venue)
     *
     * CACHING: 10 minutes TTL
     * KEY: "event_details::eventId"
     * @param eventId
     * @return EventDto
     */
    @Cacheable(
            value = CacheConfig.EVENT_DETAILS_CACHE,
            key = "#eventId",
            unless = "#result == null" // Dont cache null
    )
    public EventDto getEventById(Long eventId) {
        log.debug("Fetching event with ID: {} from database", eventId);

        Event event = eventRepository.findByIdWithVenue(eventId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Event", eventId
                ));
        return eventMapper.toDto(event);
    }

    /**
     * Get upcoming events (paginated)
     *
     * CACHING: 5 minutes TTL
     * KEY: "events::upcoming:page:size"
     */
    @Cacheable(
            value = CacheConfig.EVENT_LIST_CACHE,
            key = "'upcoming:' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<EventListDto> getUpcomingEvents(Pageable pageable) {
        log.debug("Fetching upcoming events: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Event> eventsPage = eventRepository.findUpcomingEvents(
                LocalDateTime.now(),
                pageable
        );

        return buildPageResponse(eventsPage);
    }

    /**
     * Search events with filters (specification pattern)
     */
    @Cacheable(
            value = CacheConfig.EVENT_LIST_CACHE,
            key = "'search:' + #category + ':' + #status + ':' + #city + ':' + " +
                    "#searchTerm + ':' + #minSeats + ':' + " +
                    "#pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<EventListDto> searchEvents(
            Event.EventCategory category,
            Event.EventStatus status,
            String city,
            String searchTerm,
            Integer minSeats,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        log.debug("Searching events: category={}, status={}, city={}, term={}",
                category, status, city, searchTerm);

        Specification<Event> spec = EventSpecification.buildSpecification(
                category, status, city, searchTerm, minSeats, startDate, endDate
        );

        Page<Event> eventsPage = eventRepository.findAll(spec, pageable);

        return buildPageResponse(eventsPage);
    }

    /**
     * Get events by category
     * @return
     */
    @Cacheable(
            value = CacheConfig.EVENT_LIST_CACHE,
            key = "'category:' + #category + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<EventListDto> getEventsByCategory(
            Event.EventCategory category,
            Pageable pageable) {

        log.debug("Fetching events by category: {}", category);

        Page<Event> eventsPage = eventRepository.findByCategory(category, pageable);
        return buildPageResponse(eventsPage);
    }

    /**
     * Get events by venue
     */
    @Cacheable(
            value = CacheConfig.EVENT_LIST_CACHE,
            key = "'venue:' + #venueId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<EventListDto> getEventsByVenue(
            Long venueId,
            Pageable pageable) {

        log.debug("Fetching events by venue: {}", venueId);

        Page<Event> eventsPage = eventRepository.findByVenueId(venueId, pageable);
        return buildPageResponse(eventsPage);
    }

    /**
     * Get events by city
     */
    @Cacheable(
            value = CacheConfig.EVENT_LIST_CACHE,
            key = "'city:' + #city + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<EventListDto> getEventsByCity(
            String city,
            Pageable pageable) {

        log.debug("Fetching events by city: {}", city);

        Page<Event> eventsPage = eventRepository.findByCity(city, pageable);
        return buildPageResponse(eventsPage);
    }

    /**
     * Get events by date range
     */
    @Cacheable(
            value = CacheConfig.EVENT_LIST_CACHE,
            key = "'daterange:' + #startDate + ':' + #endDate + ':' + " +
                    "#pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<EventListDto> getEventsByDateRange(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        log.debug("Fetching events by date range: {} to {}", startDate, endDate);

        Page<Event> eventsPage = eventRepository.findByDateRange(
                startDate, endDate, pageable
        );
        return buildPageResponse(eventsPage);
    }

    /**
     * Search events by name
     */
    @Cacheable(
            value = CacheConfig.EVENT_LIST_CACHE,
            key = "'search:' + #searchTerm + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public PageResponse<EventListDto> searchEventsByName(
            String searchTerm,
            Pageable pageable) {

        log.debug("Searching events by name: {}", searchTerm);

        Page<Event> eventsPage = eventRepository.searchByName(searchTerm, pageable);
        return buildPageResponse(eventsPage);
    }

    /**
     * Get active event count (for stats)
     */
    public long getActiveEventCount() {
        return eventRepository.countActiveEvents();
    }


    private PageResponse<EventListDto> buildPageResponse(Page<Event> page) {
        List<EventListDto> content = eventMapper.toListDtoList(page.getContent());

        return PageResponse.<EventListDto>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}