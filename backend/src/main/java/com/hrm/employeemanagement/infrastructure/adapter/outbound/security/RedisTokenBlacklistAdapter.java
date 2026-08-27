package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import com.hrm.employeemanagement.infrastructure.security.JwtProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final JwtProperties jwtProperties;

    public RedisTokenBlacklistAdapter(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.jwtProperties = jwtProperties;
    }

    public RedisTokenBlacklistAdapter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, null);
    }

    @Override
    public void blacklist(String token, long remainingTtlMs) {
        if (token != null && !token.isBlank() && remainingTtlMs > 0) {
            String key = BLACKLIST_KEY_PREFIX + hashToken(token);
            redisTemplate.opsForValue().set(key, "revoked", remainingTtlMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = BLACKLIST_KEY_PREFIX + hashToken(token);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public void blacklistUser(String username, long issuedBeforeTimestamp) {
        if (username != null && !username.isBlank()) {
            long userRevocationTtlMs = (jwtProperties != null && jwtProperties.expirationMs() > 0)
                    ? jwtProperties.expirationMs()
                    : 86_400_000L;
            redisTemplate.opsForValue().set(USER_REVOCATION_KEY_PREFIX + username, String.valueOf(issuedBeforeTimestamp), userRevocationTtlMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public boolean isUserRevoked(String username, long tokenIssuedAtTimestamp) {
        if (username == null || username.isBlank()) {
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

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            return token;
        }
    }
}

