package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver("");

    @Test
    @DisplayName("TC-08a: Untrusted client gửi X-Forwarded-For giả mạo -> Bị từ chối, lưu remoteAddr thực tế")
    void testUntrustedClientSpoofingXff() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.50");
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1");

        String resolvedIp = resolver.resolveClientIp(request);

        assertThat(resolvedIp).isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("TC-08b: Trusted reverse proxy gửi X-Forwarded-For -> Lấy đúng client IP đầu tiên")
    void testTrustedProxyXff() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1"); // Trusted proxy
        when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1, 10.0.0.1");

        String resolvedIp = resolver.resolveClientIp(request);

        assertThat(resolvedIp).isEqualTo("198.51.100.1");
    }

    @Test
    @DisplayName("Trusted proxy không có header XFF -> Fallback về remoteAddr")
    void testTrustedProxyWithoutXff() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        String resolvedIp = resolver.resolveClientIp(request);

        assertThat(resolvedIp).isEqualTo("127.0.0.1");
    }
}