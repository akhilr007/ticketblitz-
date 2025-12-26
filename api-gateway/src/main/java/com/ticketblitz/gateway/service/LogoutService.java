package com.ticketblitz.gateway.service;

import com.ticketblitz.gateway.exception.InvalidTokenException;
import com.ticketblitz.gateway.repository.RefreshTokenRepository;
import com.ticketblitz.gateway.util.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public Mono<Long> logout(String accessToken) {
        if (!tokenService.validateAccessToken(accessToken)) {
            return Mono.error(new InvalidTokenException("Invalid or expired access token"));
        }

        String email = tokenService.extractUsername(accessToken);

        return refreshTokenRepository.revokeAllUserTokens(email)
                .doOnSuccess(count -> log.info("User logged out, revoked {} tokens {}", count, email));
    }
}