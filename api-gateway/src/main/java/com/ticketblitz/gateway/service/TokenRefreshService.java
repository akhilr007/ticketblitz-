package com.ticketblitz.gateway.service;

import com.ticketblitz.common.dto.ApiResponse;
import com.ticketblitz.common.exception.BusinessException;
import com.ticketblitz.gateway.exception.InvalidTokenException;
import com.ticketblitz.gateway.exception.TokenRevokedException;
import com.ticketblitz.gateway.model.RefreshTokenResponse;
import com.ticketblitz.gateway.model.TokenPair;
import com.ticketblitz.gateway.repository.RefreshTokenRepository;
import com.ticketblitz.gateway.util.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public Mono<RefreshTokenResponse> refreshAccessToken(String refreshToken) {
        // validate refresh token
        if (!tokenService.validateRefreshToken(refreshToken)) {
            return Mono.error(new InvalidTokenException("Invalid or expired refresh token"));
        }

        String tokenId = tokenService.extractTokenId(refreshToken);
        String email = tokenService.extractUsername(refreshToken);
        var roles = tokenService.extractRoles(refreshToken);

        // check if refresh token exists in redis
        return refreshTokenRepository.exists(tokenId)
                .flatMap(exists -> {
                    if (!exists) {
                        log.warn("Refresh token revoked or missing: {}", tokenId);
                        return Mono.error(new TokenRevokedException("Refresh token has been revoked"));
                    }

                    // revoke old refresh token
                    return refreshTokenRepository.revoke(tokenId, email)
                            .flatMap(revoked -> {

                                if (!revoked) {
                                    return Mono.error(new TokenRevokedException("Failed to revoke refresh token"));
                                }

                                // generate new token pair
                                TokenPair newTokenPair = tokenService.generateTokenPair(email, roles);
                                String newRefreshTokenId = tokenService.extractTokenId(newTokenPair.getRefreshToken());

                                // save new refresh token
                                return refreshTokenRepository.save(newRefreshTokenId, email)
                                        .flatMap(saved -> {
                                            if (!saved) {
                                                return Mono.error(new RuntimeException("Failed to save refresh token"));
                                            }

                                            log.info("Tokens refreshed for user: {}", email);

                                            RefreshTokenResponse refreshTokenResponse = RefreshTokenResponse
                                                    .builder()
                                                    .accessToken(newTokenPair.getAccessToken())
                                                    .accessTokenExpiresIn(newTokenPair.getAccessTokenExpiresIn())
                                                    .tokenType(newTokenPair.getTokenType())
                                                    .build();
                                            return Mono.just(refreshTokenResponse)
                                                    .contextWrite(
                                                            ctx -> ctx.put("refreshToken",
                                                                    newTokenPair.getRefreshToken()));

                                        });
                            });
                });
    }
}