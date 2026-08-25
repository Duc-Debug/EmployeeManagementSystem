package com.hrm.employeemanagement.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-Memory Sliding-Window Rate Limiter for Authentication Endpoints.
 * Protects against Brute-Force attacks by blocking clients after 5 consecutive
 * failed attempts per minute.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int BLOCK_DURATION_MINUTES = 1;

    private final Cache<String, AtomicInteger> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(BLOCK_DURATION_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public boolean isBlocked(String key) {
        if (key == null)
            return false;
        AtomicInteger attempts = attemptsCache.getIfPresent(key);
        return attempts != null && attempts.get() >= MAX_FAILED_ATTEMPTS;
    }

    public void recordFailedAttempt(String key) {
        if (key == null)
            return;
        attemptsCache.asMap().compute(key, (k, counter) -> {
            if (counter == null) {
                return new AtomicInteger(1);
            }
            counter.incrementAndGet();
            return counter;
        });
    }

    public void recordSuccessfulLogin(String key) {
        if (key != null) {
            attemptsCache.invalidate(key);
        }
    }

    public void clear() {
        attemptsCache.invalidateAll();
    }
}
