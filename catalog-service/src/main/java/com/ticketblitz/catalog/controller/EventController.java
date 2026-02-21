package com.ticketblitz.catalog.controller;

import com.ticketblitz.catalog.dto.EventDto;
import com.ticketblitz.catalog.dto.EventListDto;
import com.ticketblitz.catalog.dto.PageResponse;
import com.ticketblitz.catalog.entity.Event;
import com.ticketblitz.catalog.service.EventService;
import com.ticketblitz.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Event Controller -
 *
 * API VERSIONING:
 * ===============
 * Base path: /api/v1/events
 *
 * RATE LIMITING:
 * ==============
 * Handled by API Gateway (per-user rate limiting)
 *
 * AUTHENTICATION:
 * ===============
 * Public endpoints - No authentication required
 * Catalog service is read-only for browsing
 *
 * @author Akhil
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event browsing and search API")
public class EventController {

    private final EventService eventService;

    // Default pagination values
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "eventDate",
            "name",
            "price",
            "createdAt"
    );

    /**
     * Get event by ID
     *
     * GET /api/v1/events/{id}
     */
    @Operation(summary = "Get event details", description = "Retrieve full event details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDto>> getEventById(
            @Parameter(description = "Event ID")
            @PathVariable Long id) {

        log.info("GET /api/v1/events/{}", id);

        EventDto event = eventService.getEventById(id);

        return ResponseEntity.ok(
                ApiResponse.success(event)
        );
    }

    /**
     * Get upcoming events (paginated)
     *
     * GET /api/v1/events/upcoming?page=0&size=20
     */
    @Operation(summary = "Get upcoming events", description = "List all upcoming events with pagination")
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<PageResponse<EventListDto>>> getUpcomingEvents(
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field (e.g., eventDate, name)")
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @Parameter(description = "Sort direction (ASC or DESC)")
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.info("GET /api/v1/events/upcoming?page={}&size={}", page, size);

        // Validate and cap page size
        page = Math.max(0, page);
        size = Math.min(size, MAX_SIZE);

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            sortBy = "eventDate";
        }

        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponse<EventListDto> events = eventService.getUpcomingEvents(pageable);

        log.info("Upcoming events found: page={}, size={}, total={}",
                events.getPage(),
                events.getSize(),
                events.getTotalElements());

        return ResponseEntity.ok(
                ApiResponse.success(events)
        );
    }

    /**
     * Search events with multiple filters
     *
     * GET /api/v1/events/search?category=CONCERT&city=New York&searchTerm=Taylor
     */
    @Operation(summary = "Search events", description = "Search events with multiple filters")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<EventListDto>>> searchEvents(
            @Parameter(description = "Event category")
            @RequestParam(required = false) Event.EventCategory category,
            @Parameter(description = "Event status")
            @RequestParam(required = false) Event.EventStatus status,
            @Parameter(description = "City name")
            @RequestParam(required = false) String city,
            @Parameter(description = "Search term (name)")
            @RequestParam(required = false) String searchTerm,
            @Parameter(description = "Minimum available seats")
            @RequestParam(required = false) Integer minSeats,
            @Parameter(description = "Start date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "eventDate") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {

        log.info("GET /api/v1/events/search?category={}&city={}&term={}",
                category, city, searchTerm);

        size = Math.min(size, MAX_SIZE);
        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        PageResponse<EventListDto> events = eventService.searchEvents(
                category, status, city, searchTerm, minSeats, startDate, endDate, pageable
        );

        log.info("Search events found: {}", events);
        return ResponseEntity.ok(
                ApiResponse.success(events)
        );
    }

    /**
     * Get events by category
     *
     * GET /api/v1/events/category/CONCERT
     */
    @Operation(summary = "Get events by category", description = "List events filtered by category")
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<PageResponse<EventListDto>>> getEventsByCategory(
            @Parameter(description = "Event category")
            @PathVariable Event.EventCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/events/category/{}", category);

        size = Math.min(size, MAX_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());

        PageResponse<EventListDto> events = eventService.getEventsByCategory(category, pageable);

        log.info("Get events by category found: {}", events);
        return ResponseEntity.ok(
                ApiResponse.success(events)
        );
    }

    /**
     * Get events by venue
     *
     * GET /api/v1/events/venue/1
     */
    @Operation(summary = "Get events by venue", description = "List events for a specific venue")
    @GetMapping("/venue/{venueId}")
    public ResponseEntity<ApiResponse<PageResponse<EventListDto>>> getEventsByVenue(
            @Parameter(description = "Venue ID")
            @PathVariable Long venueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/events/venue/{}", venueId);

        size = Math.min(size, MAX_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());

        PageResponse<EventListDto> events = eventService.getEventsByVenue(venueId, pageable);

        log.info("Get events by venue found: {}", events);
        return ResponseEntity.ok(
                ApiResponse.success(events)
        );
    }

    /**
     * Get events by city
     *
     * GET /api/v1/events/city/New York
     */
    @Operation(summary = "Get events by city", description = "List events in a specific city")
    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<PageResponse<EventListDto>>> getEventsByCity(
            @Parameter(description = "City name")
            @PathVariable String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/events/city/{}", city);

        size = Math.min(size, MAX_SIZE);
        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").ascending());

        PageResponse<EventListDto> events = eventService.getEventsByCity(city, pageable);

        log.info("Get events by city found: {}", events);
        return ResponseEntity.ok(
                ApiResponse.success(events)
        );
    }

    /**
     * Get event count (for statistics)
     *
     * GET /api/v1/events/count
     */
    @Operation(summary = "Get active event count", description = "Get total number of active events")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getActiveEventCount() {
        log.info("GET /api/v1/events/count");

        long count = eventService.getActiveEventCount();

        log.info("Get active event count found: {}", count);
        return ResponseEntity.ok(
                ApiResponse.success(count)
        );
    }
}