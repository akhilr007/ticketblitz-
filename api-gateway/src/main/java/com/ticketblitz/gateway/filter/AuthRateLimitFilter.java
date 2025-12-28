package com.ticketblitz.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketblitz.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AuthRateLimitFilter implements WebFilter {

    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_REQUESTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String path = exchange.getRequest().getPath().value();

        if (!path.equals("/auth/login")) {
            return chain.filter(exchange);
        }

        String ip = getClientIp(exchange);
        String key = "rl:auth:" + ip;

        return redis.opsForValue()
                .increment(key)
                .flatMap(count -> {

                    if (count == 1) {
                        redis.expire(key, WINDOW).subscribe();
                    }

                    if (count > MAX_REQUESTS) {
                        log.warn("auth rate limit exceeded for ip={}", ip);
                        return writeRateLimitResponse(exchange);
                    }

                    return chain.filter(exchange);
                });
    }

    private Mono<Void> writeRateLimitResponse(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().set("Retry-After", "60");

        ApiResponse<Void> body = ApiResponse.error(
                "RATE_LIMIT_EXCEEDED",
                "Too many login attempts. Please try again after 60 seconds."
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
            );
        } catch (Exception e) {
            log.error("failed to write rate limit response", e);
            return exchange.getResponse().setComplete();
        }
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0].trim();
        }
        return exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
    }
}