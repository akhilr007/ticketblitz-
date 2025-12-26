package com.ticketblitz.gateway.repository;

import com.ticketblitz.gateway.model.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class UserRepository {

    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void init() {
        log.info("Initializing in-memory user repository with test users");

        User user = User.builder()
                .id(1L)
                .email("user@test.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("John")
                .lastName("Doe")
                .roles(List.of("USER"))
                .enabled(true)
                .build();

        users.put(user.getEmail(), user);

        User admin = User.builder()
                .id(1L)
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .firstName("Admin")
                .lastName("User")
                .roles(List.of("USER", "ADMIN"))
                .enabled(true)
                .build();

        users.put(admin.getEmail(), admin);

        log.info("Test users created: {}", users.keySet());
        log.info("Use these credentials");
        log.info(" User:  user@test.com / password123");
        log.info(" Admin: admin@test.com / admin123");
    }

    public Mono<User> findByEmail(String email) {
        User user = users.get(email);
        if (user == null) {
            log.debug("User not found: {}", email);
            return Mono.empty();
        }
        log.debug("User found: {}", email);
        return Mono.just(user);
    }

    public boolean validatePassword(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    public Mono<Boolean> existsByEmail(String email) {
        return Mono.just(users.containsKey(email));
    }

    public Mono<User> save(User user) {
        if (!user.getPasswordHash().startsWith("$2a$")) {
            user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        }

        users.put(user.getEmail(), user);
        log.info("User saved: {}", user.getEmail());
        return Mono.just(user);
    }

    public Mono<List<User>> findAll() {
        return Mono.just(List.copyOf(users.values()));
    }
}