package com.ticketblitz.booking.client;

import com.ticketblitz.booking.client.fallback.CatalogServiceFallback;
import com.ticketblitz.common.dto.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
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

    record EventInfo(
            Long id,
            String name,
            String venueName,
            String eventDate,
            Integer availableSeats,
            String status
    ) {}

    record SeatInfo(
            Long id,
            String section,
            String rowLabel,
            Integer seatNumber,
            BigDecimal price,
            String status
    ) {}
}