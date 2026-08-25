package com.hrm.employeemanagement.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Custom AccessDeniedHandler for handling RBAC authorization failures (HTTP 403
 * Forbidden).
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // Ghi nhật ký từ chối truy cập (TC-04)
        String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous";
        log.warn("CẢNH BÁO BẢO MẬT: Người dùng [{}] bị từ chối truy cập URI [{}] từ IP [{}]",
                user, request.getRequestURI(), request.getRemoteAddr());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = "{\"success\":false,\"message\":\"Bạn không có quyền truy cập chức năng này\",\"data\":null}";
        response.getWriter().write(jsonResponse);
    }
}
