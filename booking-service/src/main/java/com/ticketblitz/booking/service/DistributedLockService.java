package com.ticketblitz.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Distributed lock facade over Redisson.
 *
 * Multi-lock acquisition is ordered deterministically to avoid deadlocks and
 * always releases every lock it successfully acquired.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    private static final long DEFAULT_WAIT_TIME = 10;
    private static final long DEFAULT_LEASE_TIME = 30;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    public RLock getFairLock(String lockKey) {
        log.debug("Creating fair lock: {}", lockKey);
        return redissonClient.getFairLock(lockKey);
    }

    public RLock getLock(String lockKey) {
        log.debug("Creating lock: {}", lockKey);
        return redissonClient.getLock(lockKey);
    }

    public <T> T executeWithFairLock(String lockKey, Supplier<T> action) {
        return executeWithLock(getFairLock(lockKey), lockKey, action);
    }

    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(getLock(lockKey), lockKey, action);
    }

    public <T> T executeWithLocks(List<String> lockKeys, Supplier<T> action) {
        List<String> normalizedLockKeys = lockKeys == null
                ? List.of()
                : lockKeys.stream().distinct().sorted(Comparator.naturalOrder()).toList();

        if (normalizedLockKeys.isEmpty()) {
            return action.get();
        }

        List<RLock> acquiredLocks = new ArrayList<>(normalizedLockKeys.size());
        try {
            for (String lockKey : normalizedLockKeys) {
                RLock lock = getLock(lockKey);
                acquireLock(lock, lockKey);
                acquiredLocks.add(lock);
            }

            return action.get();
        } finally {
            releaseLocks(acquiredLocks, normalizedLockKeys);
        }
    }

    private <T> T executeWithLock(RLock lock, String lockKey, Supplier<T> action) {
        boolean acquired = false;

        try {
            acquireLock(lock, lockKey);
            acquired = true;
            return action.get();
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {} by thread {}", lockKey, Thread.currentThread().getName());
            }
        }
    }

    private void acquireLock(RLock lock, String lockKey) {
        try {
            log.debug("Attempting to acquire lock: {}", lockKey);
            boolean acquired = lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TIME_UNIT);

            if (!acquired) {
                log.warn("Failed to acquire lock: {} (timeout after {}s)", lockKey, DEFAULT_WAIT_TIME);
                throw new LockAcquisitionException(
                        "Could not acquire lock: " + lockKey +
                                ". Resource is currently locked by another process."
                );
            }

            log.debug("Lock acquired: {} by thread {}", lockKey, Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            throw new LockAcquisitionException(
                    "Thread interrupted while acquiring lock: " + lockKey,
                    e
            );
        }
    }

    private void releaseLocks(List<RLock> acquiredLocks, List<String> normalizedLockKeys) {
        for (int index = acquiredLocks.size() - 1; index >= 0; index--) {
            RLock lock = acquiredLocks.get(index);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {} by thread {}",
                        normalizedLockKeys.get(index),
                        Thread.currentThread().getName());
            }
        }
    }

    public boolean isLocked(String lockKey) {
        return getLock(lockKey).isLocked();
    }

    public void forceUnlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isLocked()) {
            lock.forceUnlock();
            log.warn("Force unlocking {} - potential unsafe operation", lockKey);
        }
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
