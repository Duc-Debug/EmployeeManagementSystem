package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import com.hrm.employeemanagement.infrastructure.security.JwtProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * In-Memory Caffeine Token Blacklist Adapter with SHA-256 Hashing.
 * Stores token hashes with dynamic TTL matching the token's remaining lifetime,
 * reducing memory overhead by >75% compared to raw JWT strings.
 * Also supports user-level session revocation (all devices logout).
 *
 * <h3>SECURITY TRADE-OFF & PRODUCTION NOTICE:</h3>
 * <ul>
 *   <li><b>In-Memory Lifecycle:</b> This adapter maintains revocation state solely in the local JVM heap.
 *       State is cleared upon application restart/redeployment or pod eviction. Any unexpired JWTs
 *       logged out before restart could become valid until natural expiration if used against this node.</li>
 *   <li><b>Single-Instance Scope:</b> Does not sync across multiple instances/nodes in a clustered environment.</li>
 *   <li><b>Production Recommendation:</b> For high-availability, zero-downtime rolling deployments, and multi-instance
 *       production environments, a shared distributed TTL store (such as Redis) implementing {@link TokenBlacklistPort}
 *       must be configured to guarantee cross-node revocation consistency and restart survivability.</li>
 * </ul>
 */
@Component
public class CaffeineTokenBlacklistAdapter implements TokenBlacklistPort {

    private final Cache<String, Long> blacklistCache;
    private final Cache<String, Long> userRevocationCache;

    public CaffeineTokenBlacklistAdapter(JwtProperties jwtProperties) {
        this.blacklistCache = Caffeine.newBuilder()
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

        long userRevocationTtlMs = (jwtProperties != null && jwtProperties.expirationMs() > 0)
                ? jwtProperties.expirationMs()
                : 86_400_000L;

        this.userRevocationCache = Caffeine.newBuilder()
                .expireAfterWrite(userRevocationTtlMs, TimeUnit.MILLISECONDS)
                .build();
    }

    public CaffeineTokenBlacklistAdapter() {
        this(null);
    }

    @Override
    public void blacklist(String token, long remainingTtlMs) {
        if (token != null && !token.isBlank() && remainingTtlMs > 0) {
            String key = hashToken(token);
            blacklistCache.put(key, remainingTtlMs);
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = hashToken(token);
        return blacklistCache.getIfPresent(key) != null;
    }

    @Override
    public void blacklistUser(String username, long issuedBeforeTimestamp) {
        if (username != null && !username.isBlank()) {
            userRevocationCache.put(username, issuedBeforeTimestamp);
        }
    }

    @Override
    public boolean isUserRevoked(String username, long issuedAtTimestamp) {
        if (username == null || username.isBlank() || issuedAtTimestamp <= 0) {
            return false;
        }
        Long revokedBefore = userRevocationCache.getIfPresent(username);
        return revokedBefore != null && issuedAtTimestamp <= revokedBefore;
    }

    public void clear() {
        blacklistCache.invalidateAll();
        userRevocationCache.invalidateAll();
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
