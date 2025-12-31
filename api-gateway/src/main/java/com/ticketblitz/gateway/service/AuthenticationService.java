package com.ticketblitz.gateway.service;

import com.ticketblitz.common.exception.AccountDisabledException;
import com.ticketblitz.common.exception.BusinessException;
import com.ticketblitz.common.exception.InvalidCredentialsException;
import com.ticketblitz.gateway.model.AuthenticationResult;
import com.ticketblitz.gateway.model.LoginRequest;
import com.ticketblitz.gateway.model.LoginResponse;
import com.ticketblitz.gateway.model.TokenPair;
import com.ticketblitz.gateway.repository.RefreshTokenRepository;
import com.ticketblitz.gateway.repository.UserRepository;
import com.ticketblitz.gateway.util.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private boolean validatePassword(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }

    public Mono<AuthenticationResult> authenticate(LoginRequest request) {
        log.info("Authentication attempt for: {}", request.getEmail());

        return userRepository.findByEmail(request.getEmail())
                .flatMap(user -> {
                    if (!validatePassword(
                            request.getPassword(),
                            user.getPasswordHash())
                    ) {
                        log.warn("Invalid password for user: {}", request.getEmail());
                        return Mono.error(new InvalidCredentialsException());
                    }

                    if (!user.isEnabled()) {
                        log.warn("Disabled account login attempt: {}", request.getEmail());
                        return Mono.error(new AccountDisabledException());
                    }

                    TokenPair tokenPair = tokenService.generateTokenPair(user.getEmail(), user.getRoles());
                    String refreshTokenId = tokenService.extractTokenId(tokenPair.getRefreshToken());

                    return refreshTokenRepository.save(refreshTokenId, user.getEmail())
                            .flatMap(saved -> {
                                if (!saved) {
                                    log.error("Failed to save refresh token for: {}", user.getEmail());
                                    return Mono.error(new BusinessException(
                                            "SESSION_CREATION_FAILED",
                                            "Failed to create session",
                                            HttpStatus.SC_INTERNAL_SERVER_ERROR));
                                }

                                log.info("User authenticated successfully: {}", user.getEmail());

                                LoginResponse loginResponse = LoginResponse.builder()
                                        .accessToken(tokenPair.getAccessToken())
                                        .accessTokenExpiresIn(tokenPair.getAccessTokenExpiresIn())
                                        .tokenType(tokenPair.getTokenType())
                                        .email(user.getEmail())
                                        .firstName(user.getFirstName())
                                        .lastName(user.getLastName())
                                        .roles(user.getRoles())
                                        .build();

                                AuthenticationResult result = AuthenticationResult.builder()
                                        .loginResponse(loginResponse)
                                        .refreshToken(tokenPair.getRefreshToken())
                                        .build();

                                return Mono.just(result);
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: {}", request.getEmail());
                    return Mono.error(new InvalidCredentialsException());
                }));
    }
}