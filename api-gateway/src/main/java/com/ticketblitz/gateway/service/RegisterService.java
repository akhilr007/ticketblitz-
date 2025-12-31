package com.ticketblitz.gateway.service;

import com.ticketblitz.common.exception.BusinessException;
import com.ticketblitz.gateway.model.RegisterRequest;
import com.ticketblitz.gateway.model.RegisterResponse;
import com.ticketblitz.gateway.model.User;
import com.ticketblitz.gateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterService {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserRepository userRepository;

    public Mono<RegisterResponse> register(RegisterRequest request) {

        log.info("Registering new user: {}", request.getEmail());

        return userRepository.existsByEmail(request.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        log.error("Account exists with this email: {}", request.getEmail());
                        return Mono.error(
                                new BusinessException(
                                       "EMAIL_ALREADY_EXITS",
                                        "An account with this email is already registered",
                                        HttpStatus.CONFLICT.value()));
                    }

                    User newUser = User.builder()
                            .email(request.getEmail())
                            .passwordHash(passwordEncoder.encode(request.getPassword()))
                            .firstName(request.getFirstName())
                            .lastName(request.getLastName())
                            .roles(List.of("USER"))
                            .enabled(true)
                            .build();
                    return userRepository.save(newUser)
                            .map(savedUser -> RegisterResponse.builder()
                                    .firstName(savedUser.getFirstName())
                                    .lastName(savedUser.getLastName())
                                    .email(savedUser.getEmail())
                                    .roles(savedUser.getRoles())
                                    .enabled(savedUser.isEnabled())
                                    .createdAt(savedUser.getCreatedAt())
                                    .build()
                            );
                })
                .doOnSuccess(user -> {
                    log.info("User successfully registered with email: {}", user.getEmail());
                }).doOnError(error -> {
                    log.error("Registration failed for user with email: {}", request.getEmail());
                });
    }

}