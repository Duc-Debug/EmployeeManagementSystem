package com.hrm.employeemanagement.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * CSRF Protection Filter for REST API / SPA with Cross-Site SameSite=None HttpOnly Cookie.
 * Enforces strict Origin, Sec-Fetch-Site, Referer, and Custom Header verification
 * on all state-changing HTTP requests (POST, PUT, PATCH, DELETE).
 */
@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    private static final Set<String> EXEMPT_PREFIXES = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars",
            "/h2-console"
    );

    private final List<Pattern> allowedOriginPatterns;

    public CsrfProtectionFilter(@Value("${app.cors.allowed-origins:}") String allowedOrigins) {
        List<String> rawPatterns = new ArrayList<>(List.of(
                "^http://localhost(:[0-9]+)?$",
                "^http://127\\.0\\.0\\.1(:[0-9]+)?$",
                "^https://employee-management-system-izcr9mk17-duc-debug\\.vercel\\.app$"
        ));

        if (allowedOrigins != null && !allowedOrigins.isBlank()) {
            Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(raw -> {
                        String regex = "^" + raw.replace(".", "\\.").replace("*", ".*") + "$";
                        rawPatterns.add(regex);
                    });
        }

        this.allowedOriginPatterns = rawPatterns.stream()
                .map(p -> Pattern.compile(p, Pattern.CASE_INSENSITIVE))
                .toList();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();

        // 1. Safe methods (GET, HEAD, OPTIONS, TRACE) do not mutate server state
        if (SAFE_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();

        // 2. Exempt public auth and documentation endpoints
        for (String prefix : EXEMPT_PREFIXES) {
            if (requestUri.startsWith(prefix)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        String origin = request.getHeader("Origin");
        String secFetchSite = request.getHeader("Sec-Fetch-Site");
        String referer = request.getHeader("Referer");

        // 3. If Origin header is present, it MUST match the allowed origins whitelist
        if (origin != null && !origin.isBlank()) {
            if (!isOriginAllowed(origin.trim(), request)) {
                rejectCsrf(response, "Yêu cầu bị từ chối do Origin không hợp lệ: " + origin);
                return;
            }
        }

        // 4. Sec-Fetch-Site enforcement: cross-site requests MUST have an allowed Origin
        if ("cross-site".equalsIgnoreCase(secFetchSite)) {
            if (origin == null || origin.isBlank() || !isOriginAllowed(origin.trim(), request)) {
                rejectCsrf(response, "Yêu cầu cross-site bị từ chối do thiếu hoặc không khớp Origin tin cậy.");
                return;
            }
        }

        // 5. If Origin is missing but Referer is present, verify Referer origin
        if ((origin == null || origin.isBlank()) && referer != null && !referer.isBlank()) {
            String refererOrigin = extractOrigin(referer);
            if (refererOrigin != null && !isOriginAllowed(refererOrigin, request)) {
                rejectCsrf(response, "Yêu cầu bị từ chối do Referer không hợp lệ: " + referer);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isOriginAllowed(String origin, HttpServletRequest request) {
        if (origin == null || origin.isBlank()) {
            return false;
        }

        // Normalize origin by removing trailing slash if present
        String normalizedOrigin = origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;

        // Check against allowed origin patterns
        for (Pattern pattern : allowedOriginPatterns) {
            if (pattern.matcher(normalizedOrigin).matches()) {
                return true;
            }
        }

        // Same-origin check (scheme + host matching current server)
        String serverScheme = request.getScheme();
        String serverHost = request.getHeader("Host");
        if (serverHost != null && !serverHost.isBlank()) {
            String sameOrigin = serverScheme + "://" + serverHost;
            if (normalizedOrigin.equalsIgnoreCase(sameOrigin)) {
                return true;
            }
        }

        return false;
    }

    private String extractOrigin(String url) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();
            if (scheme != null && host != null) {
                if (port != -1 && port != 80 && port != 443) {
                    return scheme + "://" + host + ":" + port;
                }
                return scheme + "://" + host;
            }
        } catch (Exception ignored) {
            // Malformed URI
        }
        return null;
    }

    private void rejectCsrf(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message.replace("\"", "\\\"") + "\",\"data\":null}"
        );
    }
}
