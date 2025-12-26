package com.ticketblitz.gateway.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private boolean isProduction() {
        return "prod".equals(activeProfile);
    }

    public ResponseCookie createRefreshTokenCookie(String token, Duration maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(isProduction())
                .path("/auth")
                .maxAge(maxAge)
                .sameSite("Strict")
                .build();
    }

    public ResponseCookie deleteRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(isProduction())
                .path("/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
    }

    public String getRefreshTokenFromCookie(HttpCookie cookie) {
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    public static String getRefreshTokenCookieName() {
        return REFRESH_TOKEN_COOKIE;
    }
}