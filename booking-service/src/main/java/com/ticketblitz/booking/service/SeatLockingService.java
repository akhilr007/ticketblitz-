package com.ticketblitz.booking.service;

import com.ticketblitz.booking.client.CatalogServiceClient;
import com.ticketblitz.common.constant.SeatStatus;
import com.ticketblitz.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates authoritative inventory transitions with catalog-service.
 *
 * These calls are intentionally synchronous because seat state is part of the
 * booking correctness boundary, not a best-effort side effect.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SeatLockingService {

    private final CatalogServiceClient catalogClient;

    public List<CatalogServiceClient.SeatInfo> getSeatsForBooking(Long eventId, List<Long> seatIds) {
        ApiResponse<List<CatalogServiceClient.SeatInfo>> response = catalogClient.getSeatsByIds(eventId, seatIds);
        List<CatalogServiceClient.SeatInfo> seats = extractData(response, "fetch seat information");

        if (seats.size() != seatIds.size()) {
            throw new IllegalArgumentException("One or more selected seats do not exist for this event.");
        }

        boolean unavailableSeatSelected = seats.stream()
                .anyMatch(seat -> seat.status() != SeatStatus.AVAILABLE);

        if (unavailableSeatSelected) {
            throw new IllegalStateException("One or more selected seats are no longer available.");
        }

        return seats;
    }

    public void lockSeatsInCatalog(Long eventId, List<Long> seatIds) {
        log.info("Locking {} seats in catalog for event {}", seatIds.size(), eventId);
        ApiResponse<List<CatalogServiceClient.SeatInfo>> response = catalogClient.lockSeats(
                eventId,
                new CatalogServiceClient.SeatOperationRequest(seatIds)
        );
        extractData(response, "lock seats in catalog");
    }

    public void bookSeatsInCatalog(Long eventId, List<Long> seatIds, Long bookingId) {
        log.info("Marking {} seats as BOOKED for booking {}", seatIds.size(), bookingId);
        ApiResponse<List<CatalogServiceClient.SeatInfo>> response = catalogClient.bookSeats(
                eventId,
                new CatalogServiceClient.SeatOperationRequest(seatIds)
        );
        extractData(response, "confirm seat booking in catalog");
    }

    public void releaseSeatsInCatalog(Long eventId, List<Long> seatIds) {
        log.info("Releasing {} seats for event {}", seatIds.size(), eventId);
        ApiResponse<List<CatalogServiceClient.SeatInfo>> response = catalogClient.releaseSeats(
                eventId,
                new CatalogServiceClient.SeatOperationRequest(seatIds)
        );
        extractData(response, "release seats in catalog");
    }

    private <T> T extractData(ApiResponse<T> response, String operation) {
        if (response == null || !"success".equalsIgnoreCase(response.getStatus()) || response.getData() == null) {
            String message = response != null && response.getError() != null
                    ? response.getError().getMessage()
                    : "Catalog service returned an invalid response";
            throw new IllegalStateException("Failed to " + operation + ": " + message);
        }

        return response.getData();
    }
}
