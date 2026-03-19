package com.ticketblitz.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


/**
 * Distributed Lock Service - STAFF ENGINEER PATTERN
 *
 * DESIGN PATTERN: Facade Pattern
 * ===============================
 * Provides simple interface over complex Redisson API
 * Centralizes lock management and error handling
 *
 * LOCK TYPES:
 * ===========
 * 1. Fair Lock: FIFO ordering (first-come-first-served)
 * 2. Regular Lock: Non-fair (faster but no ordering)
 * 3. Try Lock: Timeout-based attempt
 *
 * USAGE PATTERNS:
 * ===============
 *
 * Pattern 1: Execute with Lock (auto-release)
 * -------------------------------------------
 * lockService.executeWithLock("seat:123", () -> {
 *     // Critical section
 *     bookSeat();
 *     return true;
 * });
 *
 * Pattern 2: Manual Lock/Unlock
 * ------------------------------
 * RLock lock = lockService.getLock("seat:123");
 * try {
 *     lock.lock(10, TimeUnit.SECONDS);
 *     // Critical section
 * } finally {
 *     lock.unlock();
 * }
 *
 * LOCK KEYS CONVENTION:
 * =====================
 * - "seat:{eventId}:{seatId}" - Individual seat
 * - "booking:{bookingId}" - Booking operation
 * - "event:{eventId}" - Event-level lock
 *
 * ERROR HANDLING:
 * ===============
 * - LockAcquisitionException: Failed to acquire lock
 * - InterruptedException: Thread interrupted while waiting
 * - Always unlock in finally block
 *
 * @author Akhil
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    // LOCK CONFIGURATION
    private static final long DEFAULT_WAIT_TIME = 10; // wait upto 10 seconds
    private static final long DEFAULT_LEASE_TIME = 30; // hold lock for max 30 seconds
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    /**
     * Get a fair lock (FIFO Ordering)
     *
     * USE CASE: Seat booking (ensure fairness)
     */
    public RLock getFairLock(String lockKey) {
        log.debug("Creating fair lock: {}", lockKey);
        return redissonClient.getFairLock(lockKey);
    }

    /**
     * Get a regular lock (faster, no ordering)
     *
     * USE CASE: General purpose locks
     */
    public RLock getLock(String lockKey) {
        log.debug("Creating lock: {}", lockKey);
        return  redissonClient.getLock(lockKey);
    }

    /**
     * execute code with fair lock (auto-release)
     *
     * pattern: template method
     * handles lock acquisition and release automatically
     *
     * @param lockKey Lock identifier
     * @param action code to execute within lock
     * @return result from action
     * @throws LockAcquisitionException if lock cannot be acquired
     */
    public <T> T executeWithFairLock(String lockKey, Supplier<T> action) {
        RLock lock = getFairLock(lockKey);
        return executeWithLock(lock, lockKey, action);
    }

    /*
        execute code with regular lock (auto release)
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        RLock lock = getLock(lockKey);
        return executeWithLock(lock, lockKey, action);
    }

    /*
        core lock execution logic
     */
    public <T> T executeWithLock(RLock lock, String lockKey, Supplier<T> action) {
        boolean acquired = false;

        try {
            // try to acquire lock with timeout
            log.debug("Attempting to acquire lock: {}", lockKey);
            acquired = lock.tryLock(DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TIME_UNIT);

            if (!acquired) {
                log.warn("Failed to acquire lock: {} (timeout after {}s)", lockKey, DEFAULT_WAIT_TIME);
                throw new LockAcquisitionException(
                        "Could not acquire lock: " + lockKey +
                                ". Resource is currently locked by another process."
                );
            }


            log.debug("Lock acquired: {} by thread {}", lockKey, Thread.currentThread().getName());

            // execute critical section
            return action.get();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while waiting for lock: {}", lockKey, e);
            throw new LockAcquisitionException(
                    "Thread interrupted while acquiring lock: " + lockKey, e
            );
        }
        finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {} by thread {}", lockKey, Thread.currentThread().getName());
            }
        }
    }

    /**
     *  Try to acquire lock without waiting
        @return true if lock acquired, false otherwise
     */
    public boolean tryLock(String lockKey) {
        RLock lock = getLock(lockKey);
        boolean acquired = lock.tryLock();

        if (acquired) {
            log.debug("Lock acquired immediately: {}", lockKey);
        }
        else {
            log.debug("Lock already held: {}", lockKey);
        }

        return acquired;
    }

    /**
     * check if lock is currently held
     */
    public boolean isLocked(String lockKey) {
        RLock lock = getLock(lockKey);
        return lock.isLocked();
    }

    /**
     * Force unlock, (use with caution)
     *
     * only used in admin/cleanup scenarios
     */
    public void forceUnlock(String lockKey) {
        RLock lock = getLock(lockKey);
        if (lock.isLocked()) {
            lock.forceUnlock();
            log.warn("Force unlocking {} - potential unsafe operation", lockKey);
        }
    }

    /**
     * Custom exception for lock acquisition failures
     */
    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }

        public LockAcquisitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}