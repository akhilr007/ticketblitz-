package com.ticketblitz.gateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Custom business metrics for the API Gateway.
 *
 * Exposes Micrometer counters for:
 * - Authentication success/failure (with reason tags)
 * - Rate-limit rejections
 *
 * All metrics are prefixed with "ticketblitz." for easy Grafana filtering.
 *
 * @author TicketBlitz Observability
 */
@Component
public class GatewayMetrics {

    // Counters
    private final Counter authSuccess;
    private final Counter authFailureMissingHeader;
    private final Counter authFailureInvalidFormat;
    private final Counter authFailureExpiredToken;
    private final Counter authFailureValidationError;
    private final Counter rateLimitRejected;

    public GatewayMetrics(MeterRegistry registry) {
        this.authSuccess = Counter.builder("ticketblitz.gateway.auth")
                .tag("result", "success")
                .description("Successful authentications")
                .register(registry);

        this.authFailureMissingHeader = Counter.builder("ticketblitz.gateway.auth")
                .tag("result", "failure")
                .tag("reason", "missing_header")
                .description("Auth failures due to missing Authorization header")
                .register(registry);

        this.authFailureInvalidFormat = Counter.builder("ticketblitz.gateway.auth")
                .tag("result", "failure")
                .tag("reason", "invalid_format")
                .description("Auth failures due to invalid header format")
                .register(registry);

        this.authFailureExpiredToken = Counter.builder("ticketblitz.gateway.auth")
                .tag("result", "failure")
                .tag("reason", "expired_token")
                .description("Auth failures due to expired/invalid token")
                .register(registry);

        this.authFailureValidationError = Counter.builder("ticketblitz.gateway.auth")
                .tag("result", "failure")
                .tag("reason", "validation_error")
                .description("Auth failures due to token validation errors")
                .register(registry);

        this.rateLimitRejected = Counter.builder("ticketblitz.gateway.ratelimit.rejected")
                .description("Requests rejected by rate limiter")
                .register(registry);
    }

    public void incrementAuthSuccess() {
        authSuccess.increment();
    }

    public void incrementAuthFailureMissingHeader() {
        authFailureMissingHeader.increment();
    }

    public void incrementAuthFailureInvalidFormat() {
        authFailureInvalidFormat.increment();
    }

    public void incrementAuthFailureExpiredToken() {
        authFailureExpiredToken.increment();
    }

    public void incrementAuthFailureValidationError() {
        authFailureValidationError.increment();
    }

    public void incrementRateLimitRejected() {
        rateLimitRejected.increment();
    }
}
