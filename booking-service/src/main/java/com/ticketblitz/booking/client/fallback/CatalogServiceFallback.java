package com.ticketblitz.booking.client.fallback;

import com.ticketblitz.booking.client.CatalogServiceClient;
import com.ticketblitz.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class CatalogServiceFallback implements CatalogServiceClient {

    @Override
    public ApiResponse<EventInfo> getEvent(Long eventId) {
        log.error("Fallback triggered for getEvent: {} - catalog service unavailable", eventId);
        return ApiResponse.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Catalog service unavailable");
    }

    @Override
    public ApiResponse<List<SeatInfo>> getSeatsForEvent(Long eventId) {
        log.error("Fallback triggered for getSeatsForEvent: {}", eventId);
        return ApiResponse.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Seat info unavailable");
    }

    @Override
    public ApiResponse<List<SeatInfo>> getSeatsByIds(Long eventId, List<Long> seatIds) {
        log.error("Fallback triggered for getSeatsByIds: {} - {}", eventId, seatIds);
        return ApiResponse.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Seat info unavailable");
    }

    @Override
    public ApiResponse<List<SeatInfo>> lockSeats(Long eventId, SeatOperationRequest request) {
        log.error("Fallback triggered for lockSeats: {} - {}", eventId, request.seatIds());
        return ApiResponse.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Seat locking unavailable");
    }

    @Override
    public ApiResponse<List<SeatInfo>> bookSeats(Long eventId, SeatOperationRequest request) {
        log.error("Fallback triggered for bookSeats: {} - {}", eventId, request.seatIds());
        return ApiResponse.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Seat booking unavailable");
    }

    @Override
    public ApiResponse<List<SeatInfo>> releaseSeats(Long eventId, SeatOperationRequest request) {
        log.error("Fallback triggered for releaseSeats: {} - {}", eventId, request.seatIds());
        return ApiResponse.error(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "Seat release unavailable");
    }
}
