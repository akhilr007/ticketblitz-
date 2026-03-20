package com.ticketblitz.booking.client;

import com.ticketblitz.booking.client.fallback.CatalogServiceFallback;
import com.ticketblitz.common.constant.SeatStatus;
import com.ticketblitz.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@FeignClient(
        name = "catalog-service",
        path = "/api/v1",
        fallback = CatalogServiceFallback.class
)
public interface CatalogServiceClient {

    @GetMapping("/events/{eventId}")
    @CircuitBreaker(name = "catalogService")
    @Retry(name = "catalogService")
    ApiResponse<EventInfo> getEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/seats/event/{eventId}")
    @CircuitBreaker(name = "catalogService")
    @Retry(name = "catalogService")
    ApiResponse<List<SeatInfo>> getSeatsForEvent(@PathVariable("eventId") Long eventId);

    @GetMapping("/seats/event/{eventId}/selected")
    @CircuitBreaker(name = "catalogService")
    @Retry(name = "catalogService")
    ApiResponse<List<SeatInfo>> getSeatsByIds(
            @PathVariable("eventId") Long eventId,
            @RequestParam("seatIds") List<Long> seatIds
    );

    @PostMapping("/seats/event/{eventId}/lock")
    ApiResponse<List<SeatInfo>> lockSeats(
            @PathVariable("eventId") Long eventId,
            @RequestBody SeatOperationRequest request
    );

    @PostMapping("/seats/event/{eventId}/book")
    ApiResponse<List<SeatInfo>> bookSeats(
            @PathVariable("eventId") Long eventId,
            @RequestBody SeatOperationRequest request
    );

    @PostMapping("/seats/event/{eventId}/release")
    ApiResponse<List<SeatInfo>> releaseSeats(
            @PathVariable("eventId") Long eventId,
            @RequestBody SeatOperationRequest request
    );

    record EventInfo(
            Long id,
            String name,
            LocalDateTime eventDate,
            Integer availableSeats,
            String status,
            VenueInfo venue
    ) {}

    record SeatInfo(
            Long id,
            String section,
            String rowLabel,
            Integer seatNumber,
            BigDecimal price,
            SeatStatus status
    ) {}

    record VenueInfo(
            Long id,
            String name,
            String address,
            String city,
            String state,
            String country,
            String postalCode,
            Integer capacity,
            String description
    ) {}

    record SeatOperationRequest(List<Long> seatIds) implements Serializable {}
}
