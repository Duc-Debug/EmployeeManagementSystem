package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * In-Memory Caffeine Token Blacklist Adapter.
 * Stores invalid/logged-out JWT tokens with dynamic TTL matching the token's remaining lifetime.
 * Automatically evicts expired tokens to keep memory footprint minimal.
 */
@Component
public class CaffeineTokenBlacklistAdapter implements TokenBlacklistPort {

    private final Cache<String, Long> blacklistCache;

    public CaffeineTokenBlacklistAdapter() {
        this.blacklistCache = Caffeine.newBuilder()
                .maximumSize(50_000)
                .expireAfter(new Expiry<String, Long>() {
                    @Override
                    public long expireAfterCreate(String key, Long value, long currentTime) {
                        return TimeUnit.MILLISECONDS.toNanos(Math.max(value, 0));
                    }

                    @Override
                    public long expireAfterUpdate(String key, Long value, long currentTime, long currentDuration) {
                        return TimeUnit.MILLISECONDS.toNanos(Math.max(value, 0));
                    }

                    @Override
                    public long expireAfterRead(String key, Long value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    public void blacklist(String token, long remainingTtlMs) {
        if (token != null && !token.isBlank() && remainingTtlMs > 0) {
            blacklistCache.put(token, remainingTtlMs);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return blacklistCache.getIfPresent(token) != null;
    }

    public void clear() {
        blacklistCache.invalidateAll();
    }
}
