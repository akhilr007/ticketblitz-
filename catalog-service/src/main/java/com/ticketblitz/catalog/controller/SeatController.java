package com.ticketblitz.catalog.controller;

import com.ticketblitz.catalog.dto.SeatDto;
import com.ticketblitz.catalog.service.SeatService;
import com.ticketblitz.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Seat Controller
 *
 * CONCURRENCY NOTES:
 * ==================
 * - This service is READ-ONLY
 * - Shows current seat availability
 * - Short cache TTL (30 seconds)
 * - Actual seat locking done by Booking Service
 *
 * @author Akhil
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@Tag(name = "Seats", description = "Seat availability API")
public class SeatController {

    private final SeatService seatService;

    /**
     * Get all seats for an event
     *
     * GET /api/v1/seats/event/{eventId}
     */
    @Operation(summary = "Get all seats", description = "List all seats for an event")
    @GetMapping("/event/{eventId}")
    public ResponseEntity<ApiResponse<List<SeatDto>>> getSeatsByEvent(
            @Parameter(description = "Event ID")
            @PathVariable Long eventId) {

        log.info("GET /api/v1/seats/event/{}", eventId);

        List<SeatDto> seats = seatService.getSeatsByEvent(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(seats)
        );
    }

    /**
     * Get available seats only
     *
     * GET /api/v1/seats/event/{eventId}/available
     *
     * MOST FREQUENTLY CALLED ENDPOINT
     */
    @Operation(summary = "Get available seats", description = "List only available seats")
    @GetMapping("/event/{eventId}/available")
    public ResponseEntity<ApiResponse<List<SeatDto>>> getAvailableSeats(
            @Parameter(description = "Event ID")
            @PathVariable Long eventId) {

        log.info("GET /api/v1/seats/event/{}/available", eventId);

        List<SeatDto> seats = seatService.getAvailableSeats(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(seats)
        );
    }

    /**
     * Get seats by section
     *
     * GET /api/v1/seats/event/{eventId}/section/VIP-A
     */
    @Operation(summary = "Get seats by section", description = "List seats in a specific section")
    @GetMapping("/event/{eventId}/section/{section}")
    public ResponseEntity<ApiResponse<List<SeatDto>>> getSeatsBySection(
            @Parameter(description = "Event ID")
            @PathVariable Long eventId,
            @Parameter(description = "Section name")
            @PathVariable String section) {

        log.info("GET /api/v1/seats/event/{}/section/{}", eventId, section);

        List<SeatDto> seats = seatService.getSeatsBySection(eventId, section);

        return ResponseEntity.ok(
                ApiResponse.success(seats)
        );
    }

    /**
     * Get seat map (grouped by section)
     *
     * GET /api/v1/seats/event/{eventId}/seatmap
     *
     * STAFF ENGINEER PATTERN:
     * Returns Map<String, List<SeatDto>> for UI rendering
     */
    @Operation(summary = "Get seat map", description = "Get seat map grouped by section")
    @GetMapping("/event/{eventId}/seatmap")
    public ResponseEntity<ApiResponse<Map<String, List<SeatDto>>>> getSeatMap(
            @Parameter(description = "Event ID")
            @PathVariable Long eventId) {

        log.info("GET /api/v1/seats/event/{}/seatmap", eventId);

        Map<String, List<SeatDto>> seatMap = seatService.getSeatMap(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(seatMap)
        );
    }

    /**
     * Get available seat count
     *
     * GET /api/v1/seats/event/{eventId}/count
     */
    @Operation(summary = "Get available seat count", description = "Get count of available seats")
    @GetMapping("/event/{eventId}/count")
    public ResponseEntity<ApiResponse<Integer>> getAvailableSeatCount(
            @Parameter(description = "Event ID")
            @PathVariable Long eventId) {

        log.info("GET /api/v1/seats/event/{}/count", eventId);

        int count = seatService.getAvailableSeatCount(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(count)
        );
    }

    /**
     * Get sections for an event
     *
     * GET /api/v1/seats/event/{eventId}/sections
     */
    @Operation(summary = "Get sections", description = "List all sections for an event")
    @GetMapping("/event/{eventId}/sections")
    public ResponseEntity<ApiResponse<List<String>>> getSections(
            @Parameter(description = "Event ID")
            @PathVariable Long eventId) {

        log.info("GET /api/v1/seats/event/{}/sections", eventId);

        List<String> sections = seatService.getSections(eventId);

        return ResponseEntity.ok(
                ApiResponse.success(sections)
        );
    }
}