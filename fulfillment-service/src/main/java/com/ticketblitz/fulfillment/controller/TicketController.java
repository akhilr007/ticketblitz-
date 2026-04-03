package com.ticketblitz.fulfillment.controller;

import com.ticketblitz.common.dto.ApiResponse;
import com.ticketblitz.fulfillment.dto.TicketDto;
import com.ticketblitz.fulfillment.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Ticket Controller — REST API for ticket retrieval.
 *
 * ENDPOINTS:
 * ==========
 * GET /api/v1/tickets                     → User's tickets (paginated)
 * GET /api/v1/tickets/booking/{bookingId} → Tickets for a booking
 * GET /api/v1/tickets/{ticketNumber}      → Single ticket by number
 *
 * AUTH:
 * =====
 * All endpoints require authenticated user (X-User-Id from API Gateway).
 * Users can only access their own tickets.
 *
 * @author Akhil
 */
@RestController
@Slf4j
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Ticket retrieval API")
@SecurityRequirement(name = "Bearer Authentication")
public class TicketController {

    private final TicketService ticketService;

    private static final int MAX_PAGE_SIZE = 100;

    /**
     * Get all tickets for the current user (paginated).
     *
     * GET /api/v1/tickets?page=0&size=20
     */
    @Operation(summary = "Get user's tickets", description = "List all tickets for the authenticated user")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TicketDto>>> getUserTickets(
            @Parameter(description = "User ID from JWT", hidden = true)
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("GET /api/v1/tickets - User: {}, Page: {}", userId, page);

        page = Math.max(0, page);
        size = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

        Pageable pageable = PageRequest.of(page, size, Sort.by("eventDate").descending());
        Page<TicketDto> tickets = ticketService.getUserTickets(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    /**
     * Get all tickets for a specific booking.
     *
     * GET /api/v1/tickets/booking/{bookingId}
     */
    @Operation(summary = "Get tickets for booking", description = "Retrieve all tickets for a confirmed booking")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getTicketsByBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") String userId) {

        log.info("GET /api/v1/tickets/booking/{} - User: {}", bookingId, userId);

        List<TicketDto> tickets = ticketService.getTicketsByBookingId(bookingId);

        // Verify ownership — tickets belong to a single user
        if (!tickets.isEmpty() && !tickets.get(0).getUserId().equals(userId)) {
            return ResponseEntity
                    .status(403)
                    .body(ApiResponse.error("403", "Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    /**
     * Get a single ticket by ticket number (UUID).
     *
     * GET /api/v1/tickets/{ticketNumber}
     */
    @Operation(summary = "Get ticket by number", description = "Retrieve a single ticket by its unique ticket number")
    @GetMapping("/{ticketNumber}")
    public ResponseEntity<ApiResponse<TicketDto>> getTicketByNumber(
            @PathVariable String ticketNumber,
            @RequestHeader("X-User-Id") String userId) {

        log.info("GET /api/v1/tickets/{} - User: {}", ticketNumber, userId);

        TicketDto ticket = ticketService.getTicketByTicketNumber(ticketNumber);

        // Verify ownership
        if (!ticket.getUserId().equals(userId)) {
            return ResponseEntity
                    .status(403)
                    .body(ApiResponse.error("403", "Access denied"));
        }

        return ResponseEntity.ok(ApiResponse.success(ticket));
    }
}
