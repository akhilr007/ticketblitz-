package com.ticketblitz.gateway.controller;

import com.ticketblitz.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<Void>> catalogFallback() {
        log.warn("Catalog service fallback triggered");
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "SERVICE_UNAVAILABLE",
                        "Catalog service is temporary unavailable. Please try again later."
                ));
    }

    @GetMapping("/booking")
    public ResponseEntity<ApiResponse<Void>> bookingFallback() {
        log.warn("Booking service fallback triggered");
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "SERVICE_UNAVAILABLE",
                        "Booking service is temporary unavailable. Please try again later."
                ));
    }

    @GetMapping("/fulfillment")
    public ResponseEntity<ApiResponse<Void>> fulfillmentFallback() {
        log.warn("Fulfillment service fallback triggered");
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error(
                        "SERVICE_UNAVAILABLE",
                        "Fulfillment service is temporarily unavailable. Please try again later."
                ));
    }
}