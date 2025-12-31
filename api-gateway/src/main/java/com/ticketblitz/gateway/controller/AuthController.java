package com.ticketblitz.gateway.controller;

import com.ticketblitz.common.dto.ApiResponse;
import com.ticketblitz.gateway.model.*;
import com.ticketblitz.gateway.service.AuthenticationService;
import com.ticketblitz.gateway.service.LogoutService;
import com.ticketblitz.gateway.service.RegisterService;
import com.ticketblitz.gateway.service.TokenRefreshService;
import com.ticketblitz.gateway.util.CookieUtil;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthenticationService authenticationService;
    private final LogoutService logoutService;
    private final TokenRefreshService tokenRefreshService;
    private final CookieUtil cookieUtil;
    private final RegisterService registerService;

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<RegisterResponse>>> register(
            @Valid @RequestBody RegisterRequest request) {
        return registerService.register(request)
                .map(registerResponse -> {
                    return ResponseEntity.ok(ApiResponse.success(registerResponse));
                });
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<LoginResponse>>> login(
            @Valid @RequestBody LoginRequest request,
            ServerHttpResponse response) {

        log.info("Login request received for: {}", request.getEmail());

        return authenticationService.authenticate(request)
                .map(result -> {
                    ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
                            result.getRefreshToken(),
                            Duration.ofDays(7)
                    );
                    response.addCookie(cookie);
                    return ResponseEntity.ok(ApiResponse.success(result.getLoginResponse()));
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<RefreshTokenResponse>>> refresh(
            ServerHttpRequest request,
            ServerHttpResponse response) {

        HttpCookie cookie = request.getCookies().getFirst(CookieUtil.getRefreshTokenCookieName());

        if (cookie == null) {
            log.warn("Refresh token cookie missing");
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("NO_REFRESH_TOKEN", "Refresh token not found")));
        }

        String refreshToken = cookie.getValue();


        return tokenRefreshService.refreshAccessToken(refreshToken)
                .map(result -> {
                    ResponseCookie newCookie = cookieUtil.createRefreshTokenCookie(
                            result.getRefreshToken(),
                            Duration.ofDays(7)
                    );
                    response.addCookie(newCookie);
                    return ResponseEntity.ok(ApiResponse.success(result.getRefreshTokenResponse()));
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<ApiResponse<String>>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            ServerHttpResponse response) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_REQUEST", "Authorization header required")));
        }

        String accessToken = authHeader.substring(7);

        return logoutService.logout(accessToken)
                .map(count -> {
                    ResponseCookie deleteCookie = cookieUtil.deleteRefreshTokenCookie();
                    response.addCookie(deleteCookie);
                    return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
                });
    }
}