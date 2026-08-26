package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Production-Safe Redis Adapter for Token Blacklist & User Revocation.
 * Offloads token revocation state to Redis TTL keys, preventing JVM heap memory growth and OOM risk.
 */
@Component
@Profile("prod")
public class RedisTokenBlacklistAdapter implements TokenBlacklistPort {

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final String USER_REVOCATION_KEY_PREFIX = "user:revocation:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklistAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
    }

    @Override
    public void blacklist(String token, long remainingTtlMs) {
        if (token != null && remainingTtlMs > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_KEY_PREFIX + token, "revoked", remainingTtlMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + token));
    }

    @Override
    public void blacklistUser(String username, long issuedBeforeTimestamp) {
        if (username != null) {
            redisTemplate.opsForValue().set(USER_REVOCATION_KEY_PREFIX + username, String.valueOf(issuedBeforeTimestamp), 86400, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isUserRevoked(String username, long tokenIssuedAtTimestamp) {
        if (username == null) {
            return false;
        }
        String value = redisTemplate.opsForValue().get(USER_REVOCATION_KEY_PREFIX + username);
        if (value != null) {
            try {
                long revokedBefore = Long.parseLong(value);
                return tokenIssuedAtTimestamp <= revokedBefore;
            } catch (NumberFormatException ignored) {
            }
        }
        return false;
    }
}
