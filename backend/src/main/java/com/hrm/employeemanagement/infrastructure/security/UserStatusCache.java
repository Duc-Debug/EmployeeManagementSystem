package com.hrm.employeemanagement.infrastructure.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hrm.employeemanagement.domain.user.User;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * High-performance In-Memory Caffeine Cache for User Authentication & Status.
 * Caches active user lookup for 2 minutes to prevent DB congestion on high traffic.
 * Provides immediate eviction on account lock/unlock.
 */
@Component
public class UserStatusCache {

    private final Cache<String, User> cache = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    public Optional<User> get(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(cache.getIfPresent(username));
    }

    public void put(String username, User user) {
        if (username != null && user != null) {
            cache.put(username, user);
        }
    }

    public void evict(String username) {
        if (username != null) {
            cache.invalidate(username);
        }
    }

    public void clear() {
        cache.invalidateAll();
    }
}
