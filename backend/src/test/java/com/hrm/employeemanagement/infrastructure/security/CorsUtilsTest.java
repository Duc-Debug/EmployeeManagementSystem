package com.hrm.employeemanagement.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Unit Tests for CorsUtils Safe Origin Validation")
class CorsUtilsTest {

    @Test
    @DisplayName("Should accept valid exact origins")
    void shouldAcceptValidExactOrigins() {
        assertThat(CorsUtils.isSafeOriginPattern("https://employee-management-system-izcr9mk17-duc-debug.vercel.app")).isTrue();
        assertThat(CorsUtils.isSafeOriginPattern("https://hrm.company.com")).isTrue();
        assertThat(CorsUtils.isSafeOriginPattern("http://localhost:3000")).isTrue();
    }

    @Test
    @DisplayName("Should accept valid local development and private network wildcard patterns")
    void shouldAcceptValidLocalAndPrivateWildcards() {
        assertThat(CorsUtils.isSafeOriginPattern("http://localhost:*")).isTrue();
        assertThat(CorsUtils.isSafeOriginPattern("http://127.0.0.1:*")).isTrue();
        assertThat(CorsUtils.isSafeOriginPattern("http://192.168.*:*")).isTrue();
        assertThat(CorsUtils.isSafeOriginPattern("http://10.*:*")).isTrue();
        assertThat(CorsUtils.isSafeOriginPattern("http://172.16.*:*")).isTrue();
    }

    @Test
    @DisplayName("Should reject dangerous global and public cloud wildcard patterns")
    void shouldRejectDangerousWildcards() {
        assertThat(CorsUtils.isSafeOriginPattern("*")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("https://*.vercel.app")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("*.vercel.app")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("https://*.onrender.com")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("https://*.netlify.app")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("https://*.github.io")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("https://*.pages.dev")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("https://*")).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern(null)).isFalse();
        assertThat(CorsUtils.isSafeOriginPattern("   ")).isFalse();
    }
}
