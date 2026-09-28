package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import com.hrm.employeemanagement.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistAdapterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtProperties jwtProperties;

    private RedisTokenBlacklistAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisTokenBlacklistAdapter(redisTemplate, jwtProperties);
    }

    @Test
    @DisplayName("blacklist should store hashed token key in Redis instead of raw JWT")
    void testBlacklist_UsesSha256HashedKey() throws Exception {
        String rawToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.testToken";
        long ttlMs = 3600000L;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.blacklist(rawToken, ttlMs);

        String expectedHash = hashSha256(rawToken);
        String expectedKey = "token:blacklist:" + expectedHash;

        verify(valueOperations).set(eq(expectedKey), eq("revoked"), eq(ttlMs), eq(TimeUnit.MILLISECONDS));
        assertFalse(expectedKey.contains(rawToken));
    }

    @Test
    @DisplayName("isBlacklisted should check Redis using hashed token key")
    void testIsBlacklisted_UsesSha256HashedKey() throws Exception {
        String rawToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.testToken";
        String expectedHash = hashSha256(rawToken);
        String expectedKey = "token:blacklist:" + expectedHash;

        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        boolean result = adapter.isBlacklisted(rawToken);

        assertTrue(result);
        verify(redisTemplate).hasKey(expectedKey);
    }

    @Test
    @DisplayName("blacklistUser should derive TTL from JwtProperties")
    void testBlacklistUser_UsesDynamicJwtPropertiesTtl() {
        String username = "testuser";
        long issuedBefore = 1700000000L;
        long expirationMs = 604800000L; // 7 days

        when(jwtProperties.expirationMs()).thenReturn(expirationMs);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.blacklistUser(username, issuedBefore);

        verify(valueOperations).set(
                eq("user:revocation:testuser"),
                eq(String.valueOf(issuedBefore)),
                eq(expirationMs),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    private String hashSha256(String token) throws Exception {
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
    }
}
