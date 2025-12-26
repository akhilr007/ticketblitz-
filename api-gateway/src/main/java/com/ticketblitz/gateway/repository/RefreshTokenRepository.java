package com.ticketblitz.gateway.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private static final String TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_PREFIX = "user_tokens:";
    private static final Duration TTL = Duration.ofDays(7);

    public Mono<Boolean> save(String tokenId, String email) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        String userTokensKey = USER_TOKENS_PREFIX + email;

        return redisTemplate.opsForValue()
                .set(tokenKey, email, TTL)
                .flatMap(success -> {
                    if (success) {
                        return redisTemplate.opsForSet()
                                .add(userTokensKey, tokenId)
                                .map(added -> added > 0);
                    }
                    return Mono.just(false);
                })
                .doOnSuccess(saved -> {
                    if (saved) {
                        log.debug("Saved refresh token for user: {}", email);
                    }
                    else {
                        log.warn("Failed to save refresh token for user: {}", email);
                    }
                })
                .onErrorResume(error -> {
                    log.error("Error saving refresh token", error);
                    return Mono.just(false);
                });
    }

    public Mono<Boolean> exists(String tokenId) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        return redisTemplate.hasKey(tokenKey);
    }

    public Mono<String> getUserEmail(String tokenId) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        return redisTemplate.opsForValue().get(tokenKey);
    }

    public Mono<Boolean> revoke(String tokenId, String email) {
        String tokenKey = TOKEN_PREFIX + tokenId;
        String userTokensKey = USER_TOKENS_PREFIX + email;

        return redisTemplate.delete(tokenKey)
                .flatMap(deleted -> {
                    if (deleted > 0) {
                        return redisTemplate.opsForSet()
                                .remove(userTokensKey, tokenId)
                                .map(removed -> removed > 0);
                    }
                    return Mono.just(false);
                })
                .doOnSuccess(revoked -> {
                    if (revoked) {
                        log.info("Revoked refresh token for user: {}", email);
                    }
                });
    }

    public Mono<Long> revokeAllUserTokens(String email) {
        String userTokensKey = USER_TOKENS_PREFIX + email;

        return redisTemplate.opsForSet()
                .members(userTokensKey)
                .flatMap(tokenId -> {
                    String tokenKey = TOKEN_PREFIX + tokenId;
                    return redisTemplate.delete(tokenKey);
                })
                .reduce(0L, Long::sum)
                .flatMap(deletedCount -> {
                    return redisTemplate.delete(userTokensKey)
                            .map(deleted -> deletedCount);
                })
                .doOnSuccess(count -> {
                    log.info("Revoked {} refresh tokens for user: {}", count, email);
                });
    }

    public Mono<Set<String>> getUserTokens(String email) {
        String userTokensKey = USER_TOKENS_PREFIX + email;
        return redisTemplate.opsForSet()
                .members(userTokensKey)
                .collect(Collectors.toSet());
    }

}