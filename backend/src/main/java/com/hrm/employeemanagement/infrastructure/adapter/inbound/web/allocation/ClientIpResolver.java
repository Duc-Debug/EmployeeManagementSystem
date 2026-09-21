package com.hrm.employeemanagement.infrastructure.adapter.inbound.web.allocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolver trích xuất Client IP an toàn theo mô hình Trusted Reverse Proxy.
 * Quy tắc:
 * 1. Nếu request đến từ một Untrusted client (remoteAddr không nằm trong danh sách trusted proxy),
 *    tuyệt đối không tin cậy header X-Forwarded-For và lấy trực tiếp remoteAddr.
 * 2. Nếu request đến từ một Trusted Proxy (ví dụ 127.0.0.1 hoặc cấu hình qua app.security.trusted-proxies),
 *    trích xuất IP đầu tiên trong chuỗi X-Forwarded-For (client IP gốc).
 */
@Component
public class ClientIpResolver {

    private final Set<String> trustedProxies = new HashSet<>(Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "::1",
            "localhost"
    ));

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String configuredProxies) {
        if (configuredProxies != null && !configuredProxies.isBlank()) {
            for (String proxy : configuredProxies.split(",")) {
                if (!proxy.trim().isEmpty()) {
                    trustedProxies.add(proxy.trim());
                }
            }
        }
    }

    public boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        return trustedProxies.contains(remoteAddr.trim());
    }

    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String remoteAddr = request.getRemoteAddr();

        // Chỉ đọc forwarded headers nếu kết nối đến trực tiếp từ một trusted proxy
        if (isTrustedProxy(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                String[] parts = xForwardedFor.split(",");
                if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                    String clientIp = parts[0].trim();
                    if (clientIp.length() > 45) {
                        clientIp = clientIp.substring(0, 45);
                    }
                    return clientIp;
                }
            }
        }

        // Untrusted caller hoặc không có forwarded headers -> Fallback về remoteAddr
        if (remoteAddr != null && remoteAddr.length() > 45) {
            return remoteAddr.substring(0, 45);
        }
        return remoteAddr;
    }

    // Tiện ích hỗ trợ test đăng ký thêm trusted proxy
    public void addTrustedProxy(String proxyIp) {
        if (proxyIp != null && !proxyIp.isBlank()) {
            trustedProxies.add(proxyIp.trim());
        }
    }
}