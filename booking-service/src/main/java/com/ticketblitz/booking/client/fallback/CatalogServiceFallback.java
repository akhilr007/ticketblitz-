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
}