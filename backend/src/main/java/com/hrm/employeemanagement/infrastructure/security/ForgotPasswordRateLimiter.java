package com.hrm.employeemanagement.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-Memory Sliding-Window Rate Limiter for Password Reset Requests (/forgot-password).
 * Protects against spam and token invalidation abuse by capping requests to 3 attempts per minute per IP+Identity.
 */
@Component
public class ForgotPasswordRateLimiter {

    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int WINDOW_DURATION_MINUTES = 1;

    private final Cache<String, AtomicInteger> requestsCache = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW_DURATION_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public boolean isRateLimited(String key) {
        if (key == null) return false;
        AtomicInteger count = requestsCache.getIfPresent(key);
        return count != null && count.get() >= MAX_REQUESTS_PER_WINDOW;
    }

    public void recordRequest(String key) {
        if (key == null) return;
        requestsCache.asMap().compute(key, (k, counter) -> {
            if (counter == null) {
                return new AtomicInteger(1);
            }
            counter.incrementAndGet();
            return counter;
        });
    }

    public void clear() {
        requestsCache.invalidateAll();
    }
}
