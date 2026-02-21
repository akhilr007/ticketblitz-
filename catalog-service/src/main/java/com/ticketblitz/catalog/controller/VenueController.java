package com.ticketblitz.catalog.controller;

import com.ticketblitz.catalog.dto.VenueDto;
import com.ticketblitz.catalog.service.VenueService;
import com.ticketblitz.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Venue Controller
 *
 * PUBLIC ENDPOINTS:
 * =================
 * Venues are static reference data
 * No authentication required
 *
 * @author Akhil
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Venues", description = "Venue management API")
public class VenueController {

    private final VenueService venueService;

    /**
     * Get venue by ID
     *
     * GET /api/v1/venues/{id}
     */
    @Operation(summary = "Get venue details", description = "Retrieve venue details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VenueDto>> getVenueById(
            @Parameter(description = "Venue ID")
            @PathVariable Long id) {

        log.info("GET /api/v1/venues/{}", id);

        VenueDto venue = venueService.getVenueById(id);

        return ResponseEntity.ok(
                ApiResponse.success(venue)
        );
    }

    /**
     * Get all venues
     *
     * GET /api/v1/venues
     */
    @Operation(summary = "Get all venues", description = "List all venues")
    @GetMapping
    public ResponseEntity<ApiResponse<List<VenueDto>>> getAllVenues() {
        log.info("GET /api/v1/venues");

        List<VenueDto> venues = venueService.getAllVenues();

        return ResponseEntity.ok(
                ApiResponse.success(venues)
        );
    }

    /**
     * Get venues by city
     *
     * GET /api/v1/venues/city/New York
     */
    @Operation(summary = "Get venues by city", description = "List venues in a specific city")
    @GetMapping("/city/{city}")
    public ResponseEntity<ApiResponse<List<VenueDto>>> getVenuesByCity(
            @Parameter(description = "City name")
            @PathVariable String city) {

        log.info("GET /api/v1/venues/city/{}", city);

        List<VenueDto> venues = venueService.getVenuesByCity(city);

        return ResponseEntity.ok(
                ApiResponse.success(venues)
        );
    }

    /**
     * Search venues by name
     *
     * GET /api/v1/venues/search?q=Madison
     */
    @Operation(summary = "Search venues", description = "Search venues by name")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<VenueDto>>> searchVenues(
            @Parameter(description = "Search query")
            @RequestParam(value = "q") String searchTerm) {

        log.info("GET /api/v1/venues/search?q={}", searchTerm);

        List<VenueDto> venues = venueService.searchVenuesByName(searchTerm);

        return ResponseEntity.ok(
                ApiResponse.success(venues)
        );
    }
}