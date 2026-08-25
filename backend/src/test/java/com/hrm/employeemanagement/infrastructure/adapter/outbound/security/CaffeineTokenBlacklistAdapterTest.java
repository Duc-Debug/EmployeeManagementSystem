package com.hrm.employeemanagement.infrastructure.adapter.outbound.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CaffeineTokenBlacklistAdapterTest {

    private CaffeineTokenBlacklistAdapter blacklistAdapter;

    @BeforeEach
    void setUp() {
        blacklistAdapter = new CaffeineTokenBlacklistAdapter();
    }

    @Test
    @DisplayName("Token chưa blacklist trả về false")
    void testIsNotBlacklisted_ByDefault() {
        assertFalse(blacklistAdapter.isBlacklisted("any.token.value"));
        assertFalse(blacklistAdapter.isBlacklisted(null));
        assertFalse(blacklistAdapter.isBlacklisted(""));
    }

    @Test
    @DisplayName("Token sau khi blacklist với TTL hợp lệ trả về true")
    void testBlacklistToken_Success() {
        String token = "jwt.token.to.blacklist";
        blacklistAdapter.blacklist(token, 60_000L); // 1 minute TTL

        assertTrue(blacklistAdapter.isBlacklisted(token));
    }

    @Test
    @DisplayName("Blacklist với TTL <= 0 hoặc null token không gây lỗi")
    void testBlacklistToken_InvalidParams_HandledGracefully() {
        blacklistAdapter.blacklist(null, 60_000L);
        blacklistAdapter.blacklist("", 60_000L);
        blacklistAdapter.blacklist("token.with.negative.ttl", -100L);
        blacklistAdapter.blacklist("token.with.zero.ttl", 0L);

        assertFalse(blacklistAdapter.isBlacklisted("token.with.negative.ttl"));
        assertFalse(blacklistAdapter.isBlacklisted("token.with.zero.ttl"));
    }

    @Test
    @DisplayName("Xóa toàn bộ blacklist bằng clear()")
    void testClear_RemovesAllTokens() {
        blacklistAdapter.blacklist("token1", 60_000L);
        blacklistAdapter.blacklist("token2", 60_000L);

        assertTrue(blacklistAdapter.isBlacklisted("token1"));
        assertTrue(blacklistAdapter.isBlacklisted("token2"));

        blacklistAdapter.clear();

        assertFalse(blacklistAdapter.isBlacklisted("token1"));
        assertFalse(blacklistAdapter.isBlacklisted("token2"));
    }

    @Test
    @DisplayName("Thu hồi toàn bộ phiên của User: token sinh trước timestamp bị coi là revoked")
    void testBlacklistUser_Success() {
        String username = "admin";
        long now = System.currentTimeMillis();

        blacklistAdapter.blacklistUser(username, now);

        assertTrue(blacklistAdapter.isUserRevoked(username, now - 1000));
        assertTrue(blacklistAdapter.isUserRevoked(username, now));
        assertFalse(blacklistAdapter.isUserRevoked(username, now + 1000));
        assertFalse(blacklistAdapter.isUserRevoked("other_user", now - 1000));
    }

    @Test
    @DisplayName("Khởi tạo adapter với JwtProperties tùy chỉnh TTL cho user revocation")
    void testDynamicTtl_WithCustomJwtProperties() {
        com.hrm.employeemanagement.infrastructure.security.JwtProperties customProps =
                new com.hrm.employeemanagement.infrastructure.security.JwtProperties("a-very-secure-secret-key-that-is-at-least-32-chars-long!", 172_800_000L); // 48h
        CaffeineTokenBlacklistAdapter customAdapter = new CaffeineTokenBlacklistAdapter(customProps);

        String username = "custom_user";
        long now = System.currentTimeMillis();
        customAdapter.blacklistUser(username, now);

        assertTrue(customAdapter.isUserRevoked(username, now - 5000));
        assertFalse(customAdapter.isUserRevoked(username, now + 5000));
    }
}
